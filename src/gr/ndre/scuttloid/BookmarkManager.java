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

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

import gr.ndre.scuttloid.database.DatabaseConnection;

/**
 * Manages loading of bookmarks, locally and remote
 */
public class BookmarkManager implements ScuttleAPI.Callback, ScuttleAPI.CreateCallback, ScuttleAPI.BookmarksCallback, ScuttleAPI.LastUpdateCallback, ScuttleAPI.DeleteCallback, ScuttleAPI.UpdateCallback {

    private ScuttleAPI scuttleAPI;
    private DatabaseConnection database;

    private long remote_update_time;

    protected String url;
    protected String password;

    protected Callback callback;

    /**
     * Constructor injecting mandatory preferences
     */
    public BookmarkManager(SharedPreferences preferences, Callback manager_callback) {
        this.callback = manager_callback;
        this.scuttleAPI = new ScuttleAPI(preferences, this);
        this.database = new DatabaseConnection( callback.getContext() );
    }

    /**
     * Get bookmarks
     * Returns locally stored bookmarks
     */
    public void getBookmarks() {
        // load bookmarks from database
        BookmarkContent bookmarks = database.getBookmarks();
        //callback
        ( (BookmarksCallback)callback ).onBookmarksReceived(bookmarks);
    }

    /**
     * refresh bookmarks
     * Will load changes from the server, store them locally and return the updated list.
     */
    public void refreshBookmarks() {
        //get time of last update on server
        if( scuttleAPI.hasDeletionDetectionBug() ) {
            scuttleAPI.getLastUpdate( database.getLastSync(), database.getDates() );
        } else {
            scuttleAPI.getLastUpdate( database.getLastSync() );
        }
    }

    /**
     * Last update time received
     */
    @Override
    public void onLastUpdateReceived(boolean needs_update, long remote_update_time) {
        // store, to pass to database later
        this.remote_update_time = remote_update_time;
        // if remote data is newer
        if( needs_update ) {
            //get bookmarks
            scuttleAPI.getBookmarks();
        } else {
            ( (BookmarksCallback)callback ).onBookmarksReceived(null);
        }

    }

    /**
     * Received Bookmarks
     * @param bookmarks: all received bookmarks. Final is needed to allow usage in a Thread
     */
    @Override
    public void onBookmarksReceived(final BookmarkContent bookmarks) {
        // return to callback
        ( (BookmarksCallback)callback ).onBookmarksReceived(bookmarks);

        // store bookmarks locally
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        database.setBookmarks(bookmarks, remote_update_time);
                    }
                }
        ).start();
    }

    /**
     * Create bookmark
     */
    public void createBookmark(BookmarkContent.Item item) {
        scuttleAPI.createBookmark(item);
    }

    /**
     * bookmark created
     * TODO: update local storage after creating bookmark
     */
    @Override
    public void onBookmarkCreated() {
        ( (CreateCallback)callback ).onBookmarkCreated();
    }

    /**
     * bookmark already exists, let the callback handle it
     */
    @Override
    public void onBookmarkExists() {
        ( (CreateCallback)callback ).onBookmarkExists();
    }

    /**
     * Update bookmark
     */
    public void updateBookmark(BookmarkContent.Item item) {
        scuttleAPI.updateBookmark(item);
    }

    /**
     *  Bookmark updated
     *  TODO: update local storage after updating bookmark
     */
    @Override
    public void onBookmarkUpdated() {
        ( (UpdateCallback)callback ).onBookmarkUpdated();
    }

    /**
     * Delete bookmark
     */
    public void deleteBookmark(BookmarkContent.Item item) {
        scuttleAPI.deleteBookmark(item);
    }

    /**
     * Bookmark deleted
     * TODO: update local storage after deleting bookmark
     * TODO: deletions on server are not detected yet
     */
    @Override
    public void onBookmarkDeleted() {
        ( (DeleteCallback)callback ).onBookmarkDeleted();
        // clear cache, to refetch remote bookmarks on reload
        database.clearCache();
    }

    /**
     * Forward Errors to callback
     * TODO: maybe some errors should be handled here.
     */
    @Override
    public void onAPIError(String message) {
        this.callback.onManagerError(message);
    }

    /**
     * get the context from the callback
     */
    @Override
    public Context getContext() {
        return callback.getContext();
    }

    // Callback Interfaces

    public interface Callback {
        void onManagerError(String message);
        Context getContext();
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

}
