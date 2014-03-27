/**
 * Scuttloid - Semantic Scuttle Android Client
 * Copyright (C) 2013 Alexandre Gravel-Raymond
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gr.ndre.scuttloid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
	protected static final int CREATE = 2;
	protected static final int DELETE = 3;
    protected static final int LAST_UPDATE = 4;

	protected static final String ADD_PATH = "api/posts_add.php";
	protected static final String GET_PATH = "api/posts_all.php";
	protected static final String DELETE_PATH = "api/posts_delete.php";
    protected static final String LAST_UPDATE_PATH = "api/posts_update.php?datemode=modified";

	protected String url;
	protected String username;
	protected String password;
	protected Integer handler;
	protected boolean accept_all_certs;
	
	protected Callback callback;
	
	/**
	 * Constructor injecting mandatory preferences
	 */
	public ScuttleAPI(SharedPreferences preferences, Callback api_callback) {
		this.url = preferences.getString("url", "");
		this.username = preferences.getString("username", "");
		this.password = preferences.getString("password", "");
		this.accept_all_certs = preferences.getBoolean("acceptallcerts", false);
		this.callback = api_callback;
	}

    /**
     * get date and time of last modification on server
     */
    public void getLastUpdate() {
        this.handler = LAST_UPDATE;
        APITask task = this.getAPITask(LAST_UPDATE_PATH);
        task.setHandler(new LastUpdateXMLHandler());
        task.execute();
    }

	public void getBookmarks() {
		this.handler = BOOKMARKS;
		APITask task = this.getAPITask(GET_PATH);
		task.setHandler(new BookmarksXMLHandler());
		task.execute();
	}
	
	public void updateBookmark(BookmarkContent.Item item) {
		this.handler = UPDATE;
		APITask task = this.getAPITask(ADD_PATH);
		task.setMethod(APITask.METHOD_POST);
		// Prepare post data
		List<NameValuePair> params = this.itemToParams(item);
		// Force bookmark replacement
		params.add(new BasicNameValuePair("replace", "yes"));
		task.setData(params);
		task.setHandler(new ResultXMLHandler());
		// accept 400 : missing field
		task.addAcceptableStatus(HttpStatus.SC_BAD_REQUEST); 
		// accept 409 : bookmark exists
		task.addAcceptableStatus(HttpStatus.SC_CONFLICT);
		task.execute();
	}
	
	public void createBookmark(BookmarkContent.Item item) {
		this.handler = CREATE;
		APITask task = this.getAPITask(ADD_PATH);
		task.setMethod(APITask.METHOD_POST);
		
		// Prepare post data
		List<NameValuePair> params = this.itemToParams(item);
		task.setData(params);
		task.setHandler(new ResultXMLHandler());
		// accept 400 : missing field
		task.addAcceptableStatus(HttpStatus.SC_BAD_REQUEST);
		// accept 409 : bookmark exists
		task.addAcceptableStatus(HttpStatus.SC_CONFLICT);
		task.execute();
	}
	
	public void deleteBookmark(BookmarkContent.Item item) {
		this.handler = DELETE;
		APITask task = this.getAPITask(DELETE_PATH);
		task.setMethod(APITask.METHOD_POST);
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("url", item.url));
		task.setData(params);
		task.setHandler(new ResultXMLHandler());
		// accept 404 : bookmark doesn't exist
		task.addAcceptableStatus(HttpStatus.SC_NOT_FOUND);
		task.execute();
	}
	
	@Override
	public void onDataReceived(DefaultHandler xml_handler, int status) {
		switch (this.handler) {
			case BOOKMARKS:
				BookmarkContent bookmarks = ((BookmarksXMLHandler) xml_handler).getBookmarks();
				((BookmarksCallback) this.callback).onBookmarksReceived(bookmarks);
				break;
			case UPDATE:
				if (status == HttpStatus.SC_OK) {
					((UpdateCallback) this.callback).onBookmarkUpdated();
				}
				else {
					this.sendResultError(xml_handler);
				}
				break;
			case CREATE:
				if (status == HttpStatus.SC_OK) {
					((CreateCallback) this.callback).onBookmarkCreated();
				}
				else if (status == HttpStatus.SC_CONFLICT) {
					((CreateCallback) this.callback).onBookmarkExists();
				}
				else {
					this.sendResultError(xml_handler);
				}
				break;
			case DELETE:
				if (status == HttpStatus.SC_OK) {
					((DeleteCallback) this.callback).onBookmarkDeleted();
				}
				else if (status == HttpStatus.SC_NOT_FOUND) {
					this.callback.onAPIError(this.callback.getContext().getString(R.string.error_bookmarkdelete_notfound));
				}
				else {
					this.sendResultError(xml_handler);
				}
				break;
            case LAST_UPDATE:
                long last_update = ((LastUpdateXMLHandler) xml_handler).last_update;
                ((LastUpdateCallback) this.callback).onLastUpdateReceived(last_update);
                break;
		}
	}

	protected void sendResultError(DefaultHandler xml_handler) {
		String result = ((ResultXMLHandler) xml_handler).code;
		result = result.substring(0, 1).toUpperCase(Locale.US) + result.substring(1);
		this.callback.onAPIError(result);
	}
	
	protected APITask getAPITask(String path) {
		APITask task = new APITask(this, this.username, this.password);
		String api_url = this.buildURL(path);
		task.setURL(api_url);
		task.acceptAllCerts(this.accept_all_certs);
		return task;
	}
	
	protected String getBaseURL() {
		return URLUtil.guessUrl(this.url);
	}
	
	protected String buildURL(String path) {
		return this.getBaseURL() + path;
	}
	
	protected List<NameValuePair> itemToParams(BookmarkContent.Item item) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("url", item.url));
		params.add(new BasicNameValuePair("description", item.title));
		params.add(new BasicNameValuePair("extended", item.description));
		params.add(new BasicNameValuePair("tags", item.tags));
		params.add(new BasicNameValuePair("status", item.status));
		return params;
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
			case APITask.TIMEOUT_ERROR:
				message = this.callback.getContext().getString(R.string.error_timeout);
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
	
	public interface Callback {
		void onAPIError(String message);
		Context getContext();
	}
	
	public interface ResultCallback extends Callback {
		void onDataReceived(String data);
	}
	
	public interface BookmarksCallback extends Callback {
		void onBookmarksReceived(BookmarkContent bookmarks);
	}
	
	public interface UpdateCallback extends Callback {
		void onBookmarkUpdated();
	}
	
	public interface CreateCallback extends Callback {
		void onBookmarkCreated();
		void onBookmarkExists();
	}
	
	public interface DeleteCallback extends Callback {
		void onBookmarkDeleted();
	}

    public interface LastUpdateCallback extends Callback {
        void onLastUpdateReceived( long last_update );
    }

}
