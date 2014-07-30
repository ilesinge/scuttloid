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
     * @param update_time : time in milliseconds to set as last update time (fixes unsynced server times)
     */
    public void setBookmarks(BookmarkContent bookmarks, long update_time) {
        // store tags, that have been added already

        // empty table
        truncateTable();

        // store all tag names
        Set<String> tags = extractTags(bookmarks);
        Map<String, Long> tagMap = setTags(tags);

        try {
            db.beginTransaction();

            // store all bookmarks
            for (BookmarkContent.Item item : bookmarks.getItems()) {
                // in table "bookmarks"
                ContentValues values = new ContentValues();
                values.put(h.BOOKMARKS_KEY_URL, item.url);
                values.put(h.BOOKMARKS_KEY_TITLE, item.title);
                values.put(h.BOOKMARKS_KEY_DESCRIPTION, item.description);
                values.put(h.BOOKMARKS_KEY_STATUS, item.status);
                values.put(h.BOOKMARKS_KEY_DATE, item.time);

                long insertedBookmarkdId = db.insert(h.TABLE_BOOKMARKS, null, values);

                // in table "tags"
                for (String tag : item.getTags().split(" ")) {
                    ContentValues tagRefValues = new ContentValues();
                    tagRefValues.put(h.TAGS_KEY_BOOKMARKID, insertedBookmarkdId);
                    tagRefValues.put(h.TAGS_KEY_TAGID, tagMap.get(tag));
                    db.insert(h.TABLE_TAGS, null, tagRefValues);
                }

            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // set last modification date/time
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREFS_LAST_UPDATE, update_time); //store last update time in milliseconds
        editor.apply();
    }

    /**
     * write tags to database. overwrite existing storage
     *
     * @param tags : set of unique tag names
     * @return a map of tag names to the corresponding tag ids
     */
    protected Map<String, Long> setTags(Set<String> tags) {
        Map<String, Long> tagMap = new HashMap<String, Long>();
        try {
            db.beginTransaction();
            for (String tagname : tags) {
                //add tagname to list
                ContentValues tagNameValues = new ContentValues();
                tagNameValues.put(h.TAGNAMES_KEY_TAGNAME, tagname);
                long tagid = db.insert(h.TABLE_TAG_NAMES, null, tagNameValues);
                tagMap.put(tagname, tagid);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
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
                new String[]{h.BOOKMARKS_KEY_URL, h.BOOKMARKS_KEY_TITLE, h.BOOKMARKS_KEY_DESCRIPTION, h.BOOKMARKS_KEY_STATUS, "group_concat(" + h.TAGNAMES_KEY_TAGNAME + ")"},
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
            bookmarks.addItem(bookmark);
            result.moveToNext();
        }

        return bookmarks;
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
