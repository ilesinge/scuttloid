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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

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
    protected static final int BOOKMARKS_HASHES = 6;
    protected static final int BOOKMARKS_DIFF = 7;
	protected static final int UPDATE = 1;
	protected static final int CREATE = 2;
	protected static final int DELETE = 3;
    protected static final int LAST_UPDATE = 4;
    protected static final int DATES = 5;

    protected int serverAPIVersion;

    protected static final String ADD_PATH = "/posts/add";
	protected static final String ALL_PATH = "/posts/all";
    protected static final String HASHES_PATH = "/posts/all?hashes";
    protected static final String GET_PATH = "/posts/get";
    protected static final String DELETE_PATH = "/posts/delete";
    protected static final String LAST_UPDATE_PATH = "/posts/update?datemode=modified";
    protected static final String DATES_PATH = "/posts/dates";

	protected String url;
	protected String username;
	protected String password;
	protected Integer handler;
	protected boolean accept_all_certs;

    // temporary information used by 'needsUpdate' and 'getBookmarksDiff'
    protected HashMap<String, Integer> dates = null, local_dates;
    protected long last_update, last_sync;
    protected HashMap<String, String> local_bookmark_hashes = null;
	
	protected Callback callback;

	/**
	 * Constructor injecting mandatory preferences
	 */
	public ScuttleAPI(SharedPreferences preferences, Callback api_callback) {
		this.url = preferences.getString("url", "");

        // append "/api" to url if necessary
        Pattern url_pattern = Pattern.compile( ".*/(api|v1)/?" ); //v1 is for delicious api
        Matcher url_matcher = url_pattern.matcher( this.url );
        if( !url_matcher.matches() ) {
            this.url += "/api";
        }

		this.username = preferences.getString("username", "");
		this.password = preferences.getString("password", "");
		this.accept_all_certs = preferences.getBoolean("acceptallcerts", false);
		this.callback = api_callback;

        // detect api version
        if( this.url.indexOf( "delicious.com" ) != -1 ) {
            this.serverAPIVersion = 0; // 0 means it's delicious.com
        } else {
            this.serverAPIVersion = 1; // 1 is the current semantic scuttle api
        }
	}

    /**
     * get date and time of last modification on server
     * Accept local_dates to fix deletion detection bug.
     */
    public void getLastUpdate( long last_sync, HashMap<String, Integer> local_dates ) {
        this.local_dates = local_dates;
        this.last_sync = last_sync;

        this.handler = LAST_UPDATE;
        APITask task = this.getAPITask(LAST_UPDATE_PATH);
        task.setHandler(new LastUpdateXMLHandler());
        task.execute();
    }

    /**
     * get date and time of last modification on server
     * without parameters
     */
    public void getLastUpdate( long last_sync ) {
        this.getLastUpdate( last_sync, new HashMap<String, Integer>() );
    }

    /**
     * get (changed) bookmarks
     * @param local_bookmark_hashes: HashMap containing all the hash-meta pairs of the local bookmarks. If null, all bookmarks are resynced
     */
	public void getBookmarks( HashMap<String, String> local_bookmark_hashes ) {
        // for modern APIs only get modification hashes of bookmarks
        if( local_bookmark_hashes != null && this.knowsBookmarkHashes() ) {
            this.handler = BOOKMARKS_HASHES;
            this.local_bookmark_hashes = local_bookmark_hashes;
            APITask task = this.getAPITask(HASHES_PATH);
            task.setHandler(new BookmarksHashesXMLHandler());
            task.execute();
        } else {
            this.handler = BOOKMARKS;
            APITask task = this.getAPITask(ALL_PATH);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("meta", "yes"));
            task.setData( params );
            task.setHandler(new BookmarksXMLHandler());
            task.execute();
        }
	}

    protected void getBookmarksDiff( HashMap<String, String> bookmark_hashes ) {
        // Get a set of changed and deleted bookmarks.
        // New or changed bookmarks correspond to those hash-meta pairs that are unique to the remote bookmark hashes
        StringBuilder changed = new StringBuilder();
        // Deleted bookmarks correspond to the hashes unique to the local bookmarks.
        Set<String> deleted = new HashSet<String>( this.local_bookmark_hashes.keySet() );
        // The key
        for( String bookmark_hash : bookmark_hashes.keySet() ) {
            if( this.local_bookmark_hashes.containsKey(bookmark_hash) ) {
                // Hashes that appear in both Maps have not been deleted
                deleted.remove(bookmark_hash);
                if( !bookmark_hashes.get(bookmark_hash).equals( this.local_bookmark_hashes.get(bookmark_hash) ) ) {
                    // if the meta of local and remote hashes are not equal, bookmark has changed.
                    changed.append(bookmark_hash).append(' ');
                }
            } else {
                // Hashes that appear only in the remote bookmarks are new
                changed.append(bookmark_hash).append(' ');
            }
        }
        if( changed.length() > 0 ) {
            changed.deleteCharAt( changed.length() - 1 );
        }

        // Delete bookmarks locally, that have been deleted on the server
        ((BookmarksCallback) this.callback).onBookmarksDeletedReceived( deleted );

        // get changed/new bookmarks for the server
        this.handler = BOOKMARKS_DIFF;
        APITask task = this.getAPITask(GET_PATH);
        task.setMethod(APITask.METHOD_POST);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("hashes", changed.toString()));
        params.add(new BasicNameValuePair("meta", "yes"));
        task.setData( params );
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

    // get posts/dates
    public void getDates() {
        this.handler = DATES;
        APITask task = this.getAPITask(DATES_PATH);
        task.setHandler(new DatesXMLHandler());
        task.execute();
    }
	
	@Override
	public void onDataReceived(DefaultHandler xml_handler, int status) {
		switch (this.handler) {
			case BOOKMARKS:
				BookmarkContent bookmarks = ((BookmarksXMLHandler) xml_handler).getBookmarks();
				((BookmarksCallback) this.callback).onBookmarksReceived(bookmarks);
				break;
            case BOOKMARKS_HASHES:
                this.getBookmarksDiff(((BookmarksHashesXMLHandler) xml_handler).bookmark_hashes);
                break;
            case BOOKMARKS_DIFF:
                BookmarkContent bookmarks_diff = ((BookmarksXMLHandler) xml_handler).getBookmarks();
                ((BookmarksCallback) this.callback).onBookmarksDiffReceived( bookmarks_diff );
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
                this.last_update = ((LastUpdateXMLHandler) xml_handler).last_update;
                this.needsUpdate();
                break;
            case DATES:
                this.dates = ((DatesXMLHandler) xml_handler).getDates();
                this.needsUpdate();
                break;
		}
	}

    /**
     * Waits for information (posts/update and posts/dates) to determine if an update of the database is needed.
     * Calls the Update Callback with the parameter 'needs_update' set to true if an update is needed.
     */
    protected void needsUpdate() {
        // if change has been detected: no need to check for deletions
        if( this.last_update != this.last_sync ) {
            ((LastUpdateCallback) this.callback).onLastUpdateReceived(true, this.last_update);
        } else {
            // check for deletions if the used API has the deletion detection bug
            if( this.hasDeletionDetectionBug() ) {
                if( this.dates == null ) {
                    // load remote dates (count of bookmarks per date returned by posts/dates)
                    getDates();
                } else {
                    //TODO: it would be simpler to just compare the total number of bookmarks.
                    if( !this.local_dates.equals( this.dates ) ) {
                        // some bookmarks were deleted. force a refresh
                        ((LastUpdateCallback) this.callback).onLastUpdateReceived(true,  this.last_update);
                    } else {
                        // no bookmarks where deleted*. refresh depending on date returned by posts/update
                        // *NOTE: it is possible, that the same number of bookmarks have been deleted as there have been new bookmarks
                        //        However, the creation of new bookmarks is detected by posts/update in the previous step.
                        ((LastUpdateCallback) this.callback).onLastUpdateReceived(this.last_update != this.last_sync, this.last_update);
                    }
                }
            } else {
                ((LastUpdateCallback) this.callback).onLastUpdateReceived(this.last_update != this.last_sync, this.last_update);
            }
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

    /**
     * Returns true if deletion detection bug exists in used API
     */
    public boolean hasDeletionDetectionBug() {
        return this.serverAPIVersion == 1;
    }

    /**
     * Returns true if API can be used to get changed bookmarks only
     */
    public boolean knowsBookmarkHashes() {
        return this.serverAPIVersion == 0;
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
        void onBookmarksDiffReceived(BookmarkContent bookmarks);
        void onBookmarksDeletedReceived( Set<String> hashes );
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
        void onLastUpdateReceived( boolean needs_update, long last_update );
    }

}
