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

import java.util.Set;

import gr.ndre.scuttloid.database.DatabaseConnection;

/**
 * Manages loading of bookmarks, locally and remote
 */
public class BookmarkManager implements ScuttleAPI.Callback, ScuttleAPI.CreateCallback, ScuttleAPI.BookmarksCallback, ScuttleAPI.LastUpdateCallback, ScuttleAPI.DeleteCallback, ScuttleAPI.UpdateCallback {

    private ScuttleAPI scuttleAPI; //TODO: using a single instance of ScuttleAPI is dangerous, because the handler is changed internally depending on how it is used.
    private DatabaseConnection database;

    private long remote_update_time;

    private BookmarkContent.Item createItem, editItem, deleteItem;

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
        if( scuttleAPI.knowsBookmarkUpdates() ) {
            scuttleAPI.getLastUpdate( database.getLastSync() );
        } else {
            // no need to check for updates. Just download the bookmarks:
            onLastUpdateReceived( true, 0L );
        }
        //TODO: DeletionDetectionBug: remove this if + content
        // DeletectionDetectionBug workaround has been disabled:
        // if( scuttleAPI.hasDeletionDetectionBug() ) {
        //    scuttleAPI.getLastUpdate( database.getLastSync(), database.getDates() );
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
            if( scuttleAPI.knowsBookmarkHashes() ) {
                scuttleAPI.getBookmarks( database.getHashes() );
            } else {
                scuttleAPI.getBookmarks( null );
            }
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
                        database.setBookmarks(bookmarks);
                        database.setLastSync( remote_update_time );
                    }
                }
        ).start();
    }

    /**
     * Received Bookmarks Patch
     * @param bookmarks: bookmarks that have changed/created since last sync
     */
    @Override
    public void onBookmarksDiffReceived(final BookmarkContent bookmarks) {
        // apply patch to current set of bookmarks
        BookmarkContent old_bookmarks = BookmarkContent.getShared();
        // remove changed bookmarks
        old_bookmarks.removeItems( bookmarks );
        // add all new/changed bookmarks
        old_bookmarks.addItems( bookmarks );
        // return to callback
        ( (BookmarksCallback)callback ).onBookmarksReceived(old_bookmarks);

        // store bookmarks locally
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        database.addBookmarks(bookmarks);
                        database.setLastSync( remote_update_time );
                    }
                }
        ).start();
    }

    /**
     * Remove local bookmarks that have been deleted on the server
     * @param hashes: hashes of deleted bookmarks
     */
    @Override
    public void onBookmarksDeletedReceived(final Set<String> hashes) {
        BookmarkContent old_bookmarks = BookmarkContent.getShared();
        // remove deleted bookmarks
        for( String hash: hashes ) {
            old_bookmarks.removeByHash( hash );
        }
        BookmarkContent.setShared(old_bookmarks);

        // remove bookmarks from local database
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        database.removeBookmarks(hashes);
                    }
                }
        ).start();
    }

    /**
     * Create bookmark
     */
    public void createBookmark(BookmarkContent.Item item) {
        this.createItem = item;
        scuttleAPI.createBookmark(item);
    }

    /**
     * bookmark created
     */
    @Override
    public void onBookmarkCreated() {
        database.addBookmark( this.createItem );
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
        this.editItem = item;
        scuttleAPI.updateBookmark(item);
    }

    /**
     *  Bookmark updated
     */
    @Override
    public void onBookmarkUpdated() {
        database.addBookmark( this.editItem );
        ( (UpdateCallback)callback ).onBookmarkUpdated();
    }

    /**
     * Delete bookmark
     */
    public void deleteBookmark(BookmarkContent.Item item) {
        this.deleteItem = item;
        scuttleAPI.deleteBookmark(item);
    }

    /**
     * Bookmark deleted
     */
    @Override
    public void onBookmarkDeleted() {
        database.removeBookmark( this.deleteItem );
        ( (DeleteCallback)callback ).onBookmarkDeleted();
        // clear cache, to refetch remote bookmarks on reload
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
