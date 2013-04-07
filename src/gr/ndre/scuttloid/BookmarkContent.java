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
	 * A bookmark item representing a piece of content.
	 */
	public static class Item implements Serializable {
		
		private static final long serialVersionUID = 4226037964405984432L;
		
		public String url;
		public String title;
		public String tags;
		
		// TODO maybe add URL as mandatory param in constructor
		public Item() {}

		@Override
		public String toString() {
			return title;
		}
	}
}
