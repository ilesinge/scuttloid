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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Handles database creation and connection
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database information
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ScuttloidBookmarks.db";

    // Table information
    static final String TABLE_BOOKMARKS = "bookmark";
    static final String BOOKMARKS_KEY_ID = "id";
    static final String BOOKMARKS_KEY_URL = "url";
    static final String BOOKMARKS_KEY_TITLE = "title";
    static final String BOOKMARKS_KEY_DESCRIPTION = "description";
    static final String BOOKMARKS_KEY_STATUS = "status";
    static final String BOOKMARKS_KEY_HASH = "hash";

    static final String TABLE_TAGS = "tag";
    static final String TAGS_KEY_TAGID = "tagid";
    static final String TAGS_KEY_BOOKMARKID = "bookmarkid";

    static final String TABLE_TAG_NAMES = "tag_name";
    static final String TAGNAMES_KEY_ID = "id";
    static final String TAGNAMES_KEY_TAGNAME = "tagname";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        //note: using separate table for tags, to simplify tag filtering
        // table for bookmarks
        database.execSQL(
                "CREATE TABLE " + TABLE_BOOKMARKS + " ( " +
                BOOKMARKS_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BOOKMARKS_KEY_URL + " TEXT NOT NULL, " +
                BOOKMARKS_KEY_TITLE + " TEXT NOT NULL, " +
                BOOKMARKS_KEY_DESCRIPTION + " TEXT, " +
                BOOKMARKS_KEY_STATUS + " INTEGER, " +
                BOOKMARKS_KEY_HASH + " BLOB UNIQUE )"
        );

        // table for tag-bookmark relations
        database.execSQL(
                "CREATE TABLE " + TABLE_TAGS + " ( " +
                TAGS_KEY_TAGID + " INTEGER, " +
                TAGS_KEY_BOOKMARKID + " INTEGER)"
        );
        // create unique index
        database.execSQL("CREATE UNIQUE INDEX utag ON " + TABLE_TAGS + "( " + TAGS_KEY_TAGID + ", " + TAGS_KEY_BOOKMARKID + " )");

        //table for tag names
        final String CREATE_TABLE_TAG_NAMES = "CREATE TABLE " + TABLE_TAG_NAMES + " ( " +
                TAGNAMES_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TAGNAMES_KEY_TAGNAME + " INTEGER NOT NULL UNIQUE)";
        database.execSQL(CREATE_TABLE_TAG_NAMES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGS);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_TAG_NAMES);
        onCreate(database);
    }
}
