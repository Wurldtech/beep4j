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
package net.sf.beep4j.internal.profile;

import static net.sf.beep4j.internal.profile.XMLConstants.A_CODE;
import static net.sf.beep4j.internal.profile.XMLConstants.A_ENCODING;
import static net.sf.beep4j.internal.profile.XMLConstants.A_NUMBER;
import static net.sf.beep4j.internal.profile.XMLConstants.A_URI;
import static net.sf.beep4j.internal.profile.XMLConstants.ENCODING_BASE64;
import static net.sf.beep4j.internal.profile.XMLConstants.E_CLOSE;
import static net.sf.beep4j.internal.profile.XMLConstants.E_ERROR;
import static net.sf.beep4j.internal.profile.XMLConstants.E_GREETING;
import static net.sf.beep4j.internal.profile.XMLConstants.E_OK;
import static net.sf.beep4j.internal.profile.XMLConstants.E_PROFILE;
import static net.sf.beep4j.internal.profile.XMLConstants.E_START;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;

public final class SaxMessageBuilder implements ChannelManagementMessageBuilder {
		
	public Message createClose(MessageBuilder builder, int channelNumber, int code) {
		WriterHandler handler = new WriterHandler(builder.getWriter());
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.put(A_NUMBER, Integer.toString(channelNumber));
		attributes.put(A_CODE, Integer.toString(200));
		handler.startElement(E_CLOSE, attributes);
		handler.endElement();
		handler.close();
		return builder.getMessage();
	}

	public Message createError(MessageBuilder builder, int code, String message) {
		WriterHandler handler = new WriterHandler(builder.getWriter());
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.put(A_CODE, Integer.toString(code));		
		handler.startElement(E_ERROR, attributes);
		handler.characters(message);
		handler.endElement();
		handler.close();
		return builder.getMessage();
	}
	
	public Message createGreeting(MessageBuilder builder, String[] profiles) {
		WriterHandler handler = new WriterHandler(builder.getWriter());
		Map<String,String> attributes = Collections.emptyMap();
		handler.startElement(E_GREETING, attributes);
		for (int i = 0; i < profiles.length; i++) {
			String profile = profiles[i];
			handler.startElement(E_PROFILE, Collections.singletonMap(A_URI, profile));
			handler.endElement();
		}
		handler.endElement();
		handler.close();
		return builder.getMessage();
	}

	public Message createOk(MessageBuilder builder) {
		WriterHandler handler = new WriterHandler(builder.getWriter());
		Map<String,String> attributes = Collections.emptyMap();
		handler.startElement(E_OK, attributes);
		handler.endElement();
		handler.close();
		return builder.getMessage();
	}

	public Message createProfile(MessageBuilder builder, ProfileInfo profile) {
		WriterHandler handler = new WriterHandler(builder.getWriter());
		handler.startElement(E_PROFILE, Collections.singletonMap(A_URI, profile.getUri()));
		if (profile.hasContent()) {
			StringBuilder str = new StringBuilder();
			profile.appendTo(str);
			handler.characters(str.toString());
		}		
		handler.endElement();
		handler.close();
		return builder.getMessage();
	}

	public Message createStart(MessageBuilder builder, int channelNumber,
			ProfileInfo[] infos) {
		WriterHandler handler = new WriterHandler(builder.getWriter());
		handler.startElement(E_START, Collections.singletonMap(A_NUMBER, Integer.toString(channelNumber)));
		for (int i = 0; i < infos.length; i++) {
			ProfileInfo info = infos[i];
			Map<String,String> attributes = new HashMap<String,String>();
			attributes.put(A_URI, info.getUri());
			if (info.isBase64Encoded()) {
				attributes.put(A_ENCODING, ENCODING_BASE64);
			}
			handler.startElement(E_PROFILE, attributes);
			if (info.hasContent()) {
				StringBuilder appendable = new StringBuilder();
				info.appendTo(appendable);
				handler.characters(appendable.toString());
			}
			handler.endElement();
		}
		handler.endElement();
		handler.close();
		return builder.getMessage();
	}
	
	private static class WriterHandler implements ElementHandler {
		
		private final PrintWriter writer;
		
		private final LinkedList<String> path = new LinkedList<String>();
		
		private boolean afterStartTag;
		
		private WriterHandler(Writer writer) {
			this.writer = new PrintWriter(writer);
		}
		
		public void startElement(String name, Map<String, String> attributes) {
			if (afterStartTag) {
				writer.write(">");
			}
			writer.write("<" + name);
			Iterator it = attributes.keySet().iterator();
			while (it.hasNext()) {
				String attributeName = (String) it.next();
				writer.write(" " + attributeName);
				writer.write("=\"" + attributes.get(attributeName) + "\"");
			}
			afterStartTag = true;
			path.addLast(name);
		}
		
		public void characters(String content) {
			if (afterStartTag) {
				writer.write(">");
				afterStartTag = false;
			}
			writer.write(content);			
		}
		
		public void endElement() {
			String top = path.removeLast();
			if (afterStartTag) {
				writer.write(" />");
			} else {
				writer.write("</" + top + ">");
			}
			afterStartTag = false;
		}
		
		public void close() {
			writer.close();
		}
		
	}

}
