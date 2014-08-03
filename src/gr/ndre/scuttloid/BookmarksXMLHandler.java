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

import java.security.MessageDigest;

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
            bookmark.time = attributes.getValue("time");
            bookmark.hash = attributes.getValue("hash");
            if( bookmark.hash == null ) {
                // generate an md5 hash from the url
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] array = md.digest( bookmark.url.getBytes() );
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < array.length; ++i) {
                        sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
                    }
                    bookmark.hash = sb.toString();
                } catch (java.security.NoSuchAlgorithmException e) {
                }
            }
            bookmark.meta = attributes.getValue("meta");
			this.bookmarks.addItem(bookmark);
		}
	}
}
