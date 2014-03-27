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

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LastUpdateXMLHandler extends DefaultHandler {

    /**
     * time in milliseconds
      */
    public long last_update;

    protected static final String date_format = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if ("update".equalsIgnoreCase(localName)) {
            String date_str = attributes.getValue("time");

            SimpleDateFormat parser = new SimpleDateFormat( date_format );
            try {
                this.last_update = parser.parse( date_str ).getTime();
            } catch (ParseException e) {
                // write error to log
                Log.e("LastUpdateXMLHandler", e.getMessage());
                // use current time instead
                Date date = new Date();
                this.last_update = date.getTime();
            }
        }
    }
}
