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
		return items.get(location);
	}
	
	/**
	 * Get item list
	 */
	public ArrayList<Item> getItems() {
		return items;
	}
	
	/**
	 * A map of bookmark items, by URL.
	 */
	protected Map<String, Item> item_map = new HashMap<String, Item>();
	
	/**
	 * An array of bookmark items.
	 */
	protected ArrayList<Item> items = new ArrayList<Item>();

	/**
	 * Add a bookmark to the collection.
	 */
	public void addItem(Item item) {
		items.add(item);
		item_map.put(item.url, item);
	}
	
	/**
	 * Add a bookmark to the top of the collection
	 */
	public void addItemToTop(Item item) {
		items.add(0, item);
		item_map.put(item.url, item);
	}
	
	public int getPosition(String url) {
		Item item = item_map.get(url);
		if (item != null) {
			return items.indexOf(item);
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
		protected String tags;
		public String description;
		public String status;
		
		// TODO maybe add URL as mandatory param in constructor
		public Item() {}

		@Override
		public String toString() {
			return title;
		}
		
		public String getTags() {
			String tags = "";
			if (!this.tags.equals("system:unfiled")) {
				tags = this.tags;
			}
			return tags;
		}
		
		public String getCSVTags() {
			String output = "";
			String[] atags = this.getTags().split(" ");
			if (atags.length > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append(atags[0]);
				for (int i = 1; i < atags.length; i++) {
					sb.append(", ");
					sb.append(atags[i]);
				}
				output = sb.toString();
			}
			return output;
		}
	}
}
