package gr.ndre.scuttloid;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.URLUtil;

/**
 * Enclose all API calls to Semantic Scuttle server
 */
public class ScuttleAPI implements APITask.Callback {
	
	protected static final int BOOKMARKS = 0;
	protected static final int UPDATE = 1;

	protected String url;
	protected String username;
	protected String password;
	protected Integer handler;
	
	public static interface Callback {
		public void onAPIError(String message);
		public Context getContext();
	}
	
	public static interface ResultCallback extends Callback {
		public void onDataReceived(String data);
	}
	
	public static interface BookmarksCallback extends Callback {
		public void onBookmarksReceived(BookmarkContent bookmarks);
	}
	
	public static interface UpdateCallback extends Callback {
		public void onBookmarkUpdated();
	}
	
	public static interface CreateCallback extends Callback {
		
	}
	
	protected Callback callback;
	
	/**
	 * Constructor injecting mandatory preferences
	 */
	public ScuttleAPI(SharedPreferences preferences, Callback callback) {
		this.url = preferences.getString("url", "");
		this.username = preferences.getString("username", "");
		this.password = preferences.getString("password", "");
		this.callback = callback;
	}
	
	public void getBookmarks() {
		this.handler = BOOKMARKS;
		APITask task = this.getAPITask();
		String url = this.getBaseURL();
		url += "api/posts_all.php";
		task.setURL(url);
		task.setHandler(new BookmarksXMLHandler());
		task.execute();
	}
	
	public void updateBookmark(BookmarkContent.Item item) {
		this.handler = UPDATE;
		APITask task = this.getAPITask();
		String url = this.getBaseURL();
		url += "api/posts_add.php";
		task.setURL(url);
		task.setMethod(APITask.METHOD_POST);
		
		// Prepare post data
		List<NameValuePair> params = new ArrayList<NameValuePair>(6);
		params.add(new BasicNameValuePair("url", item.url));
		params.add(new BasicNameValuePair("description", item.title));
		params.add(new BasicNameValuePair("extended", item.description));
		params.add(new BasicNameValuePair("tags", item.tags));
		params.add(new BasicNameValuePair("status", item.status));
		// Force bookmark replacement
		params.add(new BasicNameValuePair("replace", "yes"));
		task.setData(params);
		task.setHandler(new ResultXMLHandler());
		task.execute();
	}
	
	@Override
	public void onDataReceived(DefaultHandler handler) {
		switch (this.handler) {
			case BOOKMARKS:
				BookmarkContent bookmarks = ((BookmarksXMLHandler)handler).getBookmarks();
				((BookmarksCallback) this.callback).onBookmarksReceived(bookmarks);
				break;
			case UPDATE:
				String code = ((ResultXMLHandler)handler).code;
				if (code.equals("done")) {
					((UpdateCallback) this.callback).onBookmarkUpdated();
				}
				else {
					this.callback.onAPIError(code);
				}
			default:
				
		}
	}
	
	protected APITask getAPITask() {
		APITask task = new APITask(this, this.username, this.password);
		return task;
	}
	
	protected String getBaseURL() {
		return URLUtil.guessUrl(this.url);
	}

	@Override
	public void onError(int status) {
		String message = "";
		switch (status) {
			case APITask.UNKNOWN_HOST:
				message = this.callback.getContext().getString(R.string.error_unknownhost);
				break;
			case APITask.PARSE_ERROR:
				message = this.callback.getContext().getString(R.string.error_xmlparse);
				break;
			case APITask.SSL_ERROR:
				message = this.callback.getContext().getString(R.string.error_sslconnection);
				break;
			case HttpStatus.SC_UNAUTHORIZED:
				message = this.callback.getContext().getString(R.string.error_authentication);
				break;
			case HttpStatus.SC_NOT_FOUND:
				// TODO : in some cases, 404 could mean "item not found" when deleting
				message = this.callback.getContext().getString(R.string.error_notfound);
				break;
			default:
				message = this.callback.getContext().getString(R.string.error_apigeneric);
				//System.out.println(String.valueOf(status));
				break;
		}
		if (!message.isEmpty()) {
			this.callback.onAPIError(message);
		}
	}

}
