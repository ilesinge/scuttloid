package gr.ndre.scuttloid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	 * An array of bookmark items.
	 */
	protected ArrayList<Item> items = new ArrayList<Item>();
	
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
		this.item_map.remove(url);
		this.items.remove(position);
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
		this.items.remove(item);
		this.items.add(item);
		this.item_map.put(item.url, item);
	}
	
	/**
	 * Add a bookmark to the top of the collection
	 */
	public void addItemToTop(Item item) {
		this.items.remove(item);
		this.items.add(0, item);
		this.item_map.put(item.url, item);
	}
	
	public int getPosition(String url) {
		Item item = this.item_map.get(url);
		if (item != null) {
			return this.items.indexOf(item);
		}
		return -1;
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
	}
}
