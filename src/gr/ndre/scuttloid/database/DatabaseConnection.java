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
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gr.ndre.scuttloid.BookmarkContent;

/**
 * Implements high level methods for database interaction
 */
public class DatabaseConnection {

    private DatabaseHelper h;
    private SQLiteDatabase db;

    /**
     * Constructor
     * @param context : a context for the database helper
     */
    public DatabaseConnection(Context context) {
        // initialize database connection
        h = new DatabaseHelper(context);
        db = h.getWritableDatabase();
    }

    /**
     * set bookmarks, overwrite existing storage
     * @param bookmarks : the shared instance of BookmarkContent
     */
    public void setBookmarks(BookmarkContent bookmarks) {
        // store tags, that have been added already

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
    public void getBookmarks(BookmarkContent bookmarks) {
        //TODO: clear bookmark content and fill with data from database
    }
}
