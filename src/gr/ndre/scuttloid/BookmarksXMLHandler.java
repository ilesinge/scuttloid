package gr.ndre.scuttloid;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class BookmarksXMLHandler extends DefaultHandler {
	//private List<HashMap<String, String>> bookmarks = new ArrayList<HashMap<String, String>>();
	public BookmarkContent bookmarks = new BookmarkContent();
	
	public BookmarkContent getBookmarks() {
		return this.bookmarks;
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if( localName.equalsIgnoreCase("post") ) {
			BookmarkContent.Item bookmark = new BookmarkContent.Item();
			bookmark.url = attributes.getValue("href");
			bookmark.title = attributes.getValue("description");
			/*
			TODO : Add all fields (including "extended") to bookmark item
			
			curBookmark.put("href", attributes.getValue("href"));
			curBookmark.put("description", attributes.getValue("description"));
			curBookmark.put("hash", attributes.getValue("hash"));
			curBookmark.put("tag", attributes.getValue("tag"));
			curBookmark.put("time", attributes.getValue("time"));
			*/
			this.bookmarks.addItem(bookmark);
		}
	}
}
