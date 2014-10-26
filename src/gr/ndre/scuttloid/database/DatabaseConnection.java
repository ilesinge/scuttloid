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

package gr.ndre.scuttloid.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gr.ndre.scuttloid.BookmarkContent;

/**
 * Implements high level methods for database interaction
 */
public class DatabaseConnection {

    private static final String PREFS_NAME = "database_prefs";
    private static final String PREFS_LAST_UPDATE = "last_update";

    private SharedPreferences preferences;
    private DatabaseHelper h;
    private SQLiteDatabase db;

    /**
     * Constructor
     * @param context : a context for the database helper
     */
    public DatabaseConnection(Context context) {
        // get preference instance
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        // initialize database connection
        h = DatabaseHelper.getInstance(context);
        db = h.getWritableDatabase();
    }

    /**
     * set bookmarks, overwrite existing storage
     * @param bookmarks : the shared instance of BookmarkContent
     */
    public void setBookmarks(BookmarkContent bookmarks) {
        // empty table
        truncateTable();
        this.addBookmarks(bookmarks);
    }

    /**
     * Add Bookmarks. Replace if bookmark (with same hash) already exists)
     * @param bookmarks list of bookmarks to add
     */
    public void addBookmarks(BookmarkContent bookmarks) {
        // store all tag names
        Set<String> tags = extractTags(bookmarks);
        Map<String, Long> tagMap = setTags(tags);

        // prepare current date
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = new Date();
        String currDate = dateFormat.format(date);

        try {
            db.beginTransaction();

            // store all bookmarks
            for (BookmarkContent.Item item : bookmarks.getItems()) {
                // check for null values
                if( item.hash == null ) {
                    item.hash = item.url;
                }
                if( item.time == null ) {
                    item.time = currDate;
                }

                // remove existing tag connections first
                int num = db.delete(DatabaseHelper.TABLE_TAGS, DatabaseHelper.TAGS_KEY_BOOKMARKID +
                        " IN ( SELECT " + DatabaseHelper.BOOKMARKS_KEY_ID + " FROM " + DatabaseHelper.TABLE_BOOKMARKS +
                        " WHERE " + DatabaseHelper.BOOKMARKS_KEY_HASH + " = ? LIMIT 1 )",
                        new String[]{item.hash});

                // in table "bookmarks"
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.BOOKMARKS_KEY_URL, item.url);
                values.put(DatabaseHelper.BOOKMARKS_KEY_TITLE, item.title);
                values.put(DatabaseHelper.BOOKMARKS_KEY_DESCRIPTION, item.description);
                values.put(DatabaseHelper.BOOKMARKS_KEY_STATUS, item.status);
                values.put(DatabaseHelper.BOOKMARKS_KEY_DATE, item.time);
                values.put(DatabaseHelper.BOOKMARKS_KEY_HASH, item.hash);
                values.put(DatabaseHelper.BOOKMARKS_KEY_META, item.meta);

                long insertedBookmarkdId = db.insertWithOnConflict(DatabaseHelper.TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                // in table "tags": insert new tag connections
                for (String tag : item.getTags().split(" ")) {
                    ContentValues tagRefValues = new ContentValues();
                    tagRefValues.put(DatabaseHelper.TAGS_KEY_BOOKMARKID, insertedBookmarkdId);
                    tagRefValues.put(DatabaseHelper.TAGS_KEY_TAGID, tagMap.get(tag));
                    db.insert(DatabaseHelper.TABLE_TAGS, null, tagRefValues);
                }

            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        this.removeUnusedTags();
    }

    /**
     * Add Bookmark. Replace if bookmark (with same hash) already exists
     * @param item: bookmark item
     */
    public void addBookmark( BookmarkContent.Item item ) {
        // wrap in BookmarkContent and forward to addBookmarks.
        BookmarkContent bookmarkContent = new BookmarkContent();
        bookmarkContent.addItem( item );
        addBookmarks( bookmarkContent );
    }

    /**
     * Remove bookmarks with specified hashes
     * @param hashes: set of hashes of bookmarks to be removed
     */
    public void removeBookmarks( Set<String> hashes ) {
        if( hashes != null && hashes.size() != 0 ) {
            // build where clause
            StringBuilder where = new StringBuilder();
            where.append( DatabaseHelper.BOOKMARKS_KEY_HASH + " IN ( " );
            for( int i = hashes.size(); i>0; --i ) {
                where.append( "?" );
                if( i > 1 ) {
                    where.append(",");
                }
            }
            where.append(" )");

            // delete tag connections
            int num = db.delete(DatabaseHelper.TABLE_TAGS, DatabaseHelper.TAGS_KEY_BOOKMARKID +
                    " IN ( SELECT " + DatabaseHelper.BOOKMARKS_KEY_ID + " FROM " + DatabaseHelper.TABLE_BOOKMARKS +
                    " WHERE " + where + " )",
                    hashes.toArray(new String[hashes.size()]));
            // delete bookmarks
            db.delete(DatabaseHelper.TABLE_BOOKMARKS, where.toString(), hashes.toArray(new String[hashes.size()]) );
            // delete unused tags
            this.removeUnusedTags();
        }
    }

    /**
     * Remove bookmark
     * @param item: bookmark item
     */
    public void removeBookmark( BookmarkContent.Item item ) {
        // wrap in set of hashes and forward to removeBookmarks
        Set<String> hash = new HashSet<String>();
        hash.add( item.hash );
        removeBookmarks( hash );
    }

    /**
     * Clean up unused tags from database
     */
    protected void removeUnusedTags() {
        // build where clause
        String where = "NOT EXISTS ( SELECT " + DatabaseHelper.TAGS_KEY_TAGID +
                        " FROM " + DatabaseHelper.TABLE_TAGS +
                        " WHERE " + DatabaseHelper.TAGS_KEY_TAGID + " = " + DatabaseHelper.TAGNAMES_KEY_ID +
                        " LIMIT 1 )";
        int num = db.delete( DatabaseHelper.TABLE_TAG_NAMES, where, null);
    }

    /**
     * Set the time of last sync
     * @param update_time : time in milliseconds to set as last update time (fixes unsynced server times)
     */
    public void setLastSync(long update_time) {
        // set last modification date/time
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putLong(PREFS_LAST_UPDATE, update_time); //store last update time in milliseconds
        editor.apply();
    }

    /**
     * write tags to database if they do not exist already
     * @param tags : set of unique tag names
     * @return a map of tag names to the corresponding tag ids, or null if no tags where added
     */
    protected Map<String, Long> setTags(Set<String> tags) {
        if( tags.isEmpty() ) {
            return null;
        }

        try {
            db.beginTransaction();
            for (String tagname : tags) {
                //add tagname to list
                ContentValues tagNameValues = new ContentValues();
                tagNameValues.put(h.TAGNAMES_KEY_TAGNAME, tagname);
                //Note: getting the existing tagid using CONFLICT_IGNORE does not work due to this bug https://code.google.com/p/android/issues/detail?id=13045
                db.insertWithOnConflict(h.TABLE_TAG_NAMES, null, tagNameValues, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // get ids of inserted tags
        StringBuilder where = new StringBuilder();
        for( int i = tags.size(); i>0; --i ) {
            where.append( "?" );
            if( i > 1 ) {
                where.append(",");
            }
        }
        Cursor result = db.query(
                DatabaseHelper.TABLE_TAG_NAMES,
                new String[]{DatabaseHelper.TAGNAMES_KEY_ID, DatabaseHelper.TAGNAMES_KEY_TAGNAME},
                h.TAGNAMES_KEY_TAGNAME + " IN ( " + where.toString() + " )",
                tags.toArray( new String[tags.size()] ),
                null,null, null
        );

        HashMap<String, Long> tagMap = new HashMap<String, Long>();

        // fill tagMap
        result.moveToFirst();
        while( !result.isAfterLast() ) {
            tagMap.put(
                    result.getString(result.getColumnIndexOrThrow(DatabaseHelper.TAGNAMES_KEY_TAGNAME)),
                    result.getLong(result.getColumnIndexOrThrow(DatabaseHelper.TAGNAMES_KEY_ID))
            );
            result.moveToNext();
        }

        return tagMap;

    }

    /**
     * extract unique list of Tags from
     *
     * @param bookmarks : bookmark container
     * @return A set of unique tag names
     */
    protected Set<String> extractTags(BookmarkContent bookmarks) {
        // stores unique tags
        Set<String> tags = new HashSet<String>();

        // walk through bookmarks
        for (BookmarkContent.Item item : bookmarks.getItems()) {
            for (String tag : item.getTags().split(" ")) {
                tags.add(tag);
            }
        }

        return tags;
    }

    /**
     * get bookmarks, loads database into BookmarkContent
     */
    public BookmarkContent getBookmarks() {
        // query database
        Cursor result = db.query(
                h.TABLE_BOOKMARKS +
                " LEFT JOIN " + h.TABLE_TAGS + " ON (" + h.TABLE_BOOKMARKS + "." + h.BOOKMARKS_KEY_ID + " = " + h.TABLE_TAGS + "." + h.TAGS_KEY_BOOKMARKID + ")" +
                " LEFT JOIN " + h.TABLE_TAG_NAMES + " ON (" + h.TABLE_TAGS + "." + h.TAGS_KEY_TAGID + " = " + h.TABLE_TAG_NAMES + "." + h.TAGNAMES_KEY_ID + ")",
                new String[]{h.BOOKMARKS_KEY_URL, h.BOOKMARKS_KEY_TITLE, h.BOOKMARKS_KEY_DESCRIPTION, h.BOOKMARKS_KEY_STATUS,
                        h.BOOKMARKS_KEY_DATE, h.BOOKMARKS_KEY_HASH, h.BOOKMARKS_KEY_META, "group_concat(" + h.TAGNAMES_KEY_TAGNAME + ")"},
                null,
                null,
                h.BOOKMARKS_KEY_URL,
                null,
                h.TABLE_BOOKMARKS + "." + h.BOOKMARKS_KEY_ID
        );

        // fill BookmarkContent
        BookmarkContent bookmarks = new BookmarkContent();
        result.moveToFirst();
        while( !result.isAfterLast() ) {
            BookmarkContent.Item bookmark = new BookmarkContent.Item();
            bookmark.url = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_URL));
            bookmark.title = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_TITLE));
            String tags = result.getString(result.getColumnIndexOrThrow("group_concat(" + h.TAGNAMES_KEY_TAGNAME + ")"));
            //TODO: currently Tags are split here, to be joined in setTags, may change in the future?
            if( tags != null ) {
                bookmark.setTags( TextUtils.split(tags, ",") );
            } else {
                //TODO: is this the best way? If not set, results in NullPointerException
                bookmark.setTags( new String[]{"system:unfiled"} );
            }
            bookmark.description = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_DESCRIPTION));
            bookmark.status = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_STATUS));
            bookmark.time = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_DATE));
            bookmark.hash = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_HASH));
            bookmark.meta = result.getString(result.getColumnIndexOrThrow(h.BOOKMARKS_KEY_META));
            bookmarks.addItem(bookmark);
            result.moveToNext();
        }

        return bookmarks;
    }

    /**
     * Get hashes and meta of a bookmark
     * @return returns a HashMap containing hash->meta
     */
    public HashMap<String, String> getHashes() {
        // query database
        Cursor result = db.query(
                DatabaseHelper.TABLE_BOOKMARKS,
                new String[]{DatabaseHelper.BOOKMARKS_KEY_HASH, DatabaseHelper.BOOKMARKS_KEY_META},
                null, null, null, null, null
        );

        HashMap<String, String> hashes = new HashMap<String, String>();

        // fill hashes
        result.moveToFirst();
        while( !result.isAfterLast() ) {
            hashes.put(
                    result.getString(result.getColumnIndexOrThrow(DatabaseHelper.BOOKMARKS_KEY_HASH)),
                    result.getString(result.getColumnIndexOrThrow(DatabaseHelper.BOOKMARKS_KEY_META))
            );
            result.moveToNext();
        }

        // return null if database is empty
        if( hashes.isEmpty() ) {
            return null;
        }

        return hashes;
    }

    /**
     * count bookmarks for each day and return map: date->count
     */
    public HashMap<String, Integer> getDates() {
        // query database
        Cursor result = db.query(
                h.TABLE_BOOKMARKS,
                new String[]{"date(" + h.BOOKMARKS_KEY_DATE + ")", "count(*)"},
                null,
                null,
                "date(" + h.BOOKMARKS_KEY_DATE + ")",
                null,
                "date(" + h.TABLE_BOOKMARKS + "." + h.BOOKMARKS_KEY_DATE + ")"
        );

        HashMap dates = new HashMap<String, Integer>();

        // fill dates
        result.moveToFirst();
        while( !result.isAfterLast() ) {
            dates.put(
                    result.getString(result.getColumnIndexOrThrow("date(" + h.BOOKMARKS_KEY_DATE + ")")),
                    result.getInt(result.getColumnIndexOrThrow("count(*)"))
            );
            result.moveToNext();
        }

        return dates;
    }

    /**
     * delete all local data and force remote download next time
     */
    public void clearCache() {
        // empty table
        truncateTable();
        // reset last update time
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREFS_LAST_UPDATE, 0L);
        editor.apply();
    }

    public long getLastSync() {
        return preferences.getLong(PREFS_LAST_UPDATE, 0L);
    }

    protected void truncateTable() {
        try {
            db.beginTransaction();

            // empty all tables
            db.delete(h.TABLE_BOOKMARKS, null, null);
            db.delete(h.TABLE_TAGS, null, null);
            db.delete(h.TABLE_TAG_NAMES, null, null);

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }
}
