package gr.ndre.scuttloid;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class BookmarksXMLHandler extends DefaultHandler {
	
	public BookmarkContent bookmarks = new BookmarkContent();
	
	public BookmarkContent getBookmarks() {
		return this.bookmarks;
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if (localName.equalsIgnoreCase("post")) {
			BookmarkContent.Item bookmark = new BookmarkContent.Item();
			bookmark.url = attributes.getValue("href");
			bookmark.title = attributes.getValue("description");
			bookmark.tags = attributes.getValue("tag");
			bookmark.description = attributes.getValue("extended");
			this.bookmarks.addItem(bookmark);
		}
	}
}
