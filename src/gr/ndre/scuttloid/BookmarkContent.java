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

import android.text.TextUtils;
import android.util.Log;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for providing Bookmark content for user interfaces.
 */
public class BookmarkContent {
	
	/**
	 * Shared instance
	 */
	protected static BookmarkContent shared_content;
	
	/**
	 * A map of bookmark items, by URL.
	 */
	protected Map<String, Item> item_map = new HashMap<String, Item>();

    /**
     * A map of bookmark items, by hash.
     */
    protected Map<String, Item> item_hash_map = new HashMap<String, Item>();

	/**
	 * An array of bookmark items.
	 */
	protected ArrayList<Item> items = new ArrayList<Item>();

    /**
     * A set of temporary bookmark items. These will be removed when syncing with the server
     */
    protected Set<Item> temp_items = new HashSet<Item>();

	/**
	 * Get shared instance
	 */
	public static BookmarkContent getShared() {
		return shared_content;
	}
	
	/**
	 * Set shared instance
	 */
	public static void setShared(BookmarkContent content) {
		shared_content = content;
	}

	/**
	 * Get item by location
	 */
	public Item getItem(int location) {
		return this.items.get(location);
	}
	
	/**
	 * Remove item by URL
	 */
	public void removeItem(String url) {
		int position = this.getPosition(url);
        if( position != -1 ) {
            this.item_map.remove(url);
            this.item_hash_map.remove( this.items.get(position).hash );
            this.items.remove(position);
        }
	}

    /**
     * Remove items contained in another BookmarkContent object
     */
    public void removeItems(BookmarkContent remove_bookmarks) {
        for( Item item : remove_bookmarks.getItems() ) {
            this.removeByHash( item.hash );
        }
    }

    public void removeByHash( String hash ) {
        Item item = this.item_hash_map.get(hash);
        if( item != null ) {
            this.item_map.remove(item.url);
            this.items.remove(item);
            this.item_hash_map.remove(hash);
        }
    }

	/**
	 * Get item list
	 */
	public ArrayList<Item> getItems() {
		return new ArrayList<Item>(this.items);
	}

	/**
	 * Add a bookmark to the collection.
	 */
	public void addItem(Item item) {
        this.addItemAt(item, -1);
	}
	
	/**
	 * Add a bookmark to the top of the collection
	 */
	public void addItemToTop(Item item) {
		this.addItemAt(item, 0);
	}

    /**
     * Add a bookmark at selected position
     * @param item: bookmark item
     * @param position: position where item is added. -1 for default position.
     */
    protected void addItemAt(Item item, int position) {
        this.items.remove(item);
        if( position == -1 ) {
            this.items.add(item);
        } else {
            this.items.add( position, item);
        }
        this.item_map.put(item.url, item);
        if( item.hash == null ) {
            item.hash = item.url;
            this.item_hash_map.put(item.url, item);
            this.temp_items.add(item);
        } else {
            this.item_hash_map.put(item.hash, item);
        }
    }

    /**
     * Add items contained in another BookmarkContent object
     */
    public void addItems(BookmarkContent add_bookmarks) {
        for( Item item : add_bookmarks.getItems() ) {
            this.addItem( item );
        }
    }

    /**
     * Update a bookmark in the collection.
     */
    public void updateItem( Item item ) {
        int position = getPosition( item.url );
        //replace at same position
        addItemAt( item, position );
    }
	
	public int getPosition(String url) {
		Item item = this.item_map.get(url);
		if (item != null) {
			return this.items.indexOf(item);
		}
		return -1;
	}

    /**
     * Clear temporary bookmarks
     * These bookmarks have been added directly by scuttloid and will be replaced when synced
     */
    public void clearTemp() {
        for( Item item : this.temp_items ) {
            this.removeByHash( item.hash );
        }
        this.temp_items.clear();
    }

    /**
     * Sort items by creation time (newest first)
     */
    public void sort() {
        Collections.sort( this.items, Item.DateComparator );
    }

	/**
	 * A bookmark item representing a piece of content.
	 */
	public static class Item implements Serializable {
		
		private static final long serialVersionUID = 4226037964405984432L;
		
		public String url;
		public String title;
		public String description;
		public String status;
        public String time;
        public String hash;
        public String meta;
		
		protected String tags;
		
		@Override
		public String toString() {
			return this.title;
		}
		
		public String getTags() {
			String real_tags = "";
			if (!this.tags.equals("system:unfiled")) {
				real_tags = this.tags;
			}
			return real_tags;
		}
		
		public String getCSVTags() {
			String output = "";
			String[] atags = this.getTags().split(" ");
			if (atags.length > 0) {
				StringBuilder string_builder = new StringBuilder();
				string_builder.append(atags[0]);
				for (int i = 1; i < atags.length; i++) {
					string_builder.append(", ");
					string_builder.append(atags[i]);
				}
				output = string_builder.toString();
			}
			return output;
		}

        public void setTags(String[] tag_array) {
            tags = TextUtils.join(" ", tag_array);
        }

        /**
         * Compare create date of two bookmarks. Used for sorting
         */
        public static Comparator<Item> DateComparator = new Comparator<Item>() {
            @Override
            public int compare(Item item, Item item2) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                boolean diff = false;
                try {
                   diff = df.parse(item.time).getTime() < df.parse(item2.time).getTime();
                } catch (ParseException e) {
                    Log.e("Scuttloid", e.getMessage());
                } finally {
                    if( diff ) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        };

	}
}
