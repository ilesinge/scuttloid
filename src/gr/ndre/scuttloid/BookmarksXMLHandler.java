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
		if ("post".equalsIgnoreCase(localName)) {
			BookmarkContent.Item bookmark = new BookmarkContent.Item();
			bookmark.url = attributes.getValue("href");
			bookmark.title = attributes.getValue("description");
			bookmark.tags = attributes.getValue("tag");
			bookmark.description = attributes.getValue("extended");
			this.bookmarks.addItem(bookmark);
		}
	}
}
