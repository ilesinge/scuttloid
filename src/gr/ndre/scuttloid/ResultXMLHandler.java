package gr.ndre.scuttloid;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class ResultXMLHandler extends DefaultHandler {
	
	public String code;
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		if ("result".equalsIgnoreCase(localName)) {
			this.code = attributes.getValue("code");
		}
	}
}
