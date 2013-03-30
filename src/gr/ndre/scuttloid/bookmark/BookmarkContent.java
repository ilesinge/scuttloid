package gr.ndre.scuttloid.bookmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class BookmarkContent {

	/**
	 * An array of bookmark items.
	 */
	public static List<BookmarkItem> ITEMS = new ArrayList<BookmarkItem>();

	/**
	 * A map of bookmark items, by ID.
	 */
	public static Map<String, BookmarkItem> ITEM_MAP = new HashMap<String, BookmarkItem>();

	static {
		// Add 3 sample items.
		addItem(new BookmarkItem("1", "Item 1"));
		addItem(new BookmarkItem("2", "Item 2"));
		addItem(new BookmarkItem("3", "Item 3"));
	}

	private static void addItem(BookmarkItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}

	/**
	 * A bookmark item representing a piece of content.
	 */
	public static class BookmarkItem {
		public String id;
		public String content;

		public BookmarkItem(String id, String content) {
			this.id = id;
			this.content = content;
		}

		@Override
		public String toString() {
			return content;
		}
	}
}
