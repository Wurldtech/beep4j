/*
 *  Copyright 2006 Simon Raess
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.beep4j.internal.management;

import static net.sf.beep4j.internal.management.XMLConstants.A_CODE;
import static net.sf.beep4j.internal.management.XMLConstants.A_ENCODING;
import static net.sf.beep4j.internal.management.XMLConstants.A_NUMBER;
import static net.sf.beep4j.internal.management.XMLConstants.A_URI;
import static net.sf.beep4j.internal.management.XMLConstants.ENCODING_BASE64;
import static net.sf.beep4j.internal.management.XMLConstants.E_CLOSE;
import static net.sf.beep4j.internal.management.XMLConstants.E_ERROR;
import static net.sf.beep4j.internal.management.XMLConstants.E_GREETING;
import static net.sf.beep4j.internal.management.XMLConstants.E_OK;
import static net.sf.beep4j.internal.management.XMLConstants.E_PROFILE;
import static net.sf.beep4j.internal.management.XMLConstants.E_START;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.beep4j.Message;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.internal.InternalException;
import net.sf.beep4j.internal.util.Base64Encoder;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxMessageParser implements ManagementMessageParser {
	
	private void processMessage(Message message, ElementHandlerContentHandler handler) {
		try {
			SAXParser parser = createParser();
			parser.parse(new InputSource(message.getReader()), handler);
		} catch (SAXException e) {
			throw new ProtocolException("received XML is not well-formed", e);
		} catch (IOException e) {
			throw new InternalException("failed to read from input message", e);
		}
	}

	private SAXParser createParser() {
		try {
			return SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new InternalException("failed to create SAX parser", e);
		} catch (SAXException e) {
			throw new InternalException("failed to create SAX parser", e);
		}
	}

	public ManagementRequest parseRequest(Message message) {
		ElementHandlerContentHandler handler = new ElementHandlerContentHandler("start | close");
		handler.registerHandler("/start", new StartElementHandler(handler));
		handler.registerHandler("/start/profile", new ProfileElementHandler(handler));
		handler.registerHandler("/close", new CloseElementHandler(handler));
			
		processMessage(message, handler);

		return (ManagementRequest) handler.peekObject();
	}
	
	public BEEPError parseError(Message message) {
		ElementHandlerContentHandler handler = new ElementHandlerContentHandler("error");
		handler.registerHandler("/error", new ErrorElementHandler(handler));

		processMessage(message, handler);
		
		return (BEEPError) handler.peekObject();
	}
	
	public void parseOk(Message message) {
		ElementHandlerContentHandler handler = new ElementHandlerContentHandler("ok");
		handler.registerHandler("/ok", new OkElementHandler());

		processMessage(message, handler);
	}
	
	public Greeting parseGreeting(Message message) {
		ElementHandlerContentHandler handler = new ElementHandlerContentHandler("greeting");
		handler.registerHandler("/greeting", new GreetingElementHandler(handler));
		handler.registerHandler("/greeting/profile", new SimpleProfileElementHandler(handler));

		processMessage(message, handler);
		
		return (Greeting) handler.peekObject();
	}
	
	@SuppressWarnings("unchecked")
	public ProfileInfo parseProfile(Message message) {
		ElementHandlerContentHandler handler = new ElementHandlerContentHandler("profile");
		handler.registerHandler("/profile", new ProfileElementHandler(handler));
		handler.pushObject(new LinkedList<ProfileInfo>());

		processMessage(message, handler);
		
		List<ProfileInfo> result = (List) handler.peekObject();
		return result.get(0);
	}
	
	protected static class ElementHandlerContentHandler extends DefaultHandler implements ElementHandlerContext {
		
		private final LinkedList<String> path = new LinkedList<String>();
		
		private final String rootElement;
		
		private LinkedList<Object> objectStack = new LinkedList<Object>();
		
		private Map<String,ElementHandler> handlers = new HashMap<String,ElementHandler>();
		
		protected ElementHandlerContentHandler(String rootElement) {
			this.rootElement = rootElement;
		}
		
		public Object peekObject() {
			return objectStack.getLast();
		}
		
		public Object popObject() {
			return objectStack.removeLast();
		}
		
		public void pushObject(Object o) {
			objectStack.addLast(o);
		}
		
		public void registerHandler(String path, ElementHandler handler) {
			handlers.put(path, handler);
		}
		
		private ElementHandler getElementHandler(List<String> path) {
			LinkedList<String> copy = new LinkedList<String>(path);
			while (copy.size() > 0) {
				String key = toString(copy);
				ElementHandler handler = handlers.get(key);
				if (handler != null) {
					return handler;
				}
				copy.removeLast();
			}
			
			// throw a ProtocolException, which will terminate the session silently
			String xpath = path.toString();
			xpath = xpath.length() > 0 ? xpath.substring(1, xpath.length() - 1) : xpath;
			throw new ProtocolException("invalid channel management message (xpath = '" 
					+ xpath + "'" + ",expected '" + rootElement + "')");
		}
		
		private String toString(List<String> path) {
			StringBuilder result = new StringBuilder();
			Iterator<String> it = path.iterator();
			while (it.hasNext()) {
				String fragment = (String) it.next();
				result.append("/").append(fragment);
			}
			return result.toString();
		}
		
		private Map<String,String> toMap(Attributes attributes) {
			Map<String,String> result = new HashMap<String,String>();
			for (int i = 0; i < attributes.getLength(); i++) {
				result.put(attributes.getQName(i), attributes.getValue(i));
			}
			return result;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			path.addLast(qName);
			ElementHandler handler = getElementHandler(path);
			handler.startElement(qName, toMap(attributes));
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			ElementHandler handler = getElementHandler(path);
			handler.characters(new String(ch, start, length));
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			ElementHandler handler = getElementHandler(path);
			handler.endElement();
			path.removeLast();
		}
	}
	
	protected static class StartElementHandler implements ElementHandler {
		
		private final ElementHandlerContext context;
		
		private int channelNumber;
		
		public StartElementHandler(ElementHandlerContext context) {
			this.context = context;
		}
		
		public void startElement(String name, Map<String,String> attributes) {
			expectName(E_START, name);
			channelNumber = expectIntegerAttribute(A_NUMBER, attributes);
			context.pushObject(new LinkedList<ProfileInfo>());
		}

		public void characters(String content) {
			// ignored			
		}
		
		@SuppressWarnings("unchecked")
		public void endElement() {
			List<ProfileInfo> profiles = (List) context.popObject(); 
			context.pushObject(new StartChannelMessage(
					channelNumber, 
					profiles.toArray(new ProfileInfo[profiles.size()])));
		}
		
	}
	
	protected static class CloseElementHandler implements ElementHandler {
		
		private final ElementHandlerContext context;
		
		private int code;
		
		private int number;
		
		private StringBuilder content;
		
		public CloseElementHandler(ElementHandlerContext context) {
			this.context = context;
		}
		
		public void startElement(String name, Map<String, String> attributes) {
			expectName(E_CLOSE, name);
			this.code = expectIntegerAttribute(A_CODE, attributes);
			this.number = getIntegerAttribute(A_NUMBER, 0, attributes);
			this.content = new StringBuilder();
		}

		public void characters(String content) {
			this.content.append(content);		
		}

		public void endElement() {
			context.pushObject(new CloseChannelMessage(number, code, content.toString()));			
		}
		
	}
		
	protected static class GreetingElementHandler implements ElementHandler {
		
		private final ElementHandlerContext context;
		
		private String[] localize;
		
		private String[] features;
		
		public GreetingElementHandler(ElementHandlerContext context) {
			this.context = context;
		}
		
		public void startElement(String name, Map<String, String> attributes) {
			if (!E_GREETING.equals(name)) {
				throw new ProtocolException("expected greeting element, was '" + name + "'");
			}
			String value = attributes.get("localize");
			this.localize = value == null ? new String[0] : value.split(" ");
			
			value = attributes.get("features");
			this.features = value == null ? new String[0] : value.split(" ");
			
			context.pushObject(new LinkedHashSet<String>());
		}

		public void characters(String content) {
			// ignored			
		}
		
		@SuppressWarnings("unchecked")
		public void endElement() {
			Set<String> profiles = (Set<String>) context.popObject();
			context.pushObject(new Greeting(localize, features, profiles.toArray(new String[0])));
		}
		
	}
	
	protected static class ProfileElementHandler implements ElementHandler {
		
		private final ElementHandlerContext context;
		
		private String uri;
		
		private boolean base64Encoded;
		
		private StringBuilder content;
		
		public ProfileElementHandler(ElementHandlerContext context) {
			this.context = context;
		}
		
		public void startElement(String name, Map<String, String> attributes) {
			expectName(E_PROFILE, name);
			this.uri = expectAttribute(A_URI, attributes);
			this.base64Encoded = getBooleanAttribute(A_ENCODING, ENCODING_BASE64, attributes);
			this.content = new StringBuilder();
		}
		
		public void characters(String content) {
			this.content.append(content);			
		}
		
		@SuppressWarnings("unchecked")
		public void endElement() {
			ProfileInfo profile = null;
			String content = this.content.toString().trim();
			if (content.length() == 0) {
				profile = new ProfileInfo(uri);
			} else if (base64Encoded) {
				byte[] bytes = new Base64Encoder().decode(content);
				profile = new ProfileInfo(uri, bytes);
			} else {
				profile = new ProfileInfo(uri, content);
			}
			((Collection) context.peekObject()).add(profile);			
		}
		
	}
	
	protected static class SimpleProfileElementHandler implements ElementHandler {
		
		private final ElementHandlerContext context;
		
		public SimpleProfileElementHandler(ElementHandlerContext context) {
			this.context = context;
		}
		
		@SuppressWarnings("unchecked")
		public void startElement(String name, Map<String, String> attributes) {
			expectName(E_PROFILE, name);
			String uri = expectAttribute(A_URI, attributes);
			((Set) context.peekObject()).add(uri);
		}

		public void characters(String content) {
			throw new ProtocolException("profile element inside greeting must not contain content");
		}

		public void endElement() {
			// ignored			
		}
		
	}
	
	protected static class OkElementHandler implements ElementHandler {
		
		public void startElement(String name, Map<String, String> attributes) {
			expectName(E_OK, name);
		}

		public void characters(String content) {
			throw new ProtocolException("ok element must no contain content");
		}

		public void endElement() {
			// ignored
		}
		
	}
	
	protected static class ErrorElementHandler implements ElementHandler {
		
		private final ElementHandlerContext context;
		
		private int code;
		
		private StringBuilder content;
		
		public ErrorElementHandler(ElementHandlerContext context) {
			this.context = context;
		}
		
		public void startElement(String name, Map<String, String> attributes) {
			expectName(E_ERROR, name);
			this.code = expectIntegerAttribute(A_CODE, attributes);
			this.content = new StringBuilder();
		}
		
		public void characters(String content) {
			this.content.append(content);
		}
		
		public void endElement() {
			context.pushObject(new BEEPError(code, content.toString().trim()));
		}
		
	}

	
	private static void expectName(String expected, String actual) {
		if (!expected.equals(actual)) {
			throw new ProtocolException("expected element '" + expected + "' "
					+ "but was '" + actual + "'");
		}
	}

	private static int getIntegerAttribute(String name, int defaultValue, Map<String,String> attributes) {
		String value = attributes.get(name);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	private static boolean getBooleanAttribute(String name, String trueValue, Map<String,String> attributes) {
		String value = attributes.get(name);
		return trueValue.equals(value);
	}
	
	private static int expectIntegerAttribute(String name, Map<String,String> attributes) {
		String value = attributes.get(name);
		if (value == null) {
			throw new ProtocolException("expected mandatory attribute: " + name);
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new ProtocolException("expected attribute is not a number: " + name
					+ " (value=" + value + ")");
		}
	}
	
	private static String expectAttribute(String name, Map<String,String> attributes) {
		String value = attributes.get(name);
		if (value == null) {
			throw new ProtocolException("expected mandatory attribute '" + name + "'");
		}
		return value;
	}

}
