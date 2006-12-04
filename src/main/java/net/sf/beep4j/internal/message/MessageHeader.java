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
package net.sf.beep4j.internal.message;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.beep4j.Message;
import net.sf.beep4j.internal.message.contenttype.ContentTypeParser;
import net.sf.beep4j.internal.message.contenttype.ParseException;

public class MessageHeader {
	
	private static final String EOL = "\r\n";

	private final Map<String,String> header;
	
	private ByteBuffer buffer;
	
	private String type = "application";
	
	private String subtype = "octet-stream";
	
	private String charset = "UTF-8";
	
	private String transferEncoding = "binary";
	
	public MessageHeader() {
		this.header = new HashMap<String,String>();
	}
	
	public void addHeader(String name, String value) {
		name = name.toLowerCase();
		if (Message.CONTENT_TYPE.equals(name)) {
			parseContentType(value.trim());
		} else if (Message.CONTENT_TRANSFER_ENCODING.equals(name)) {
			parseContentTransferEncoding(value);
		} else {
			header.put(name, value.trim());
		}
	}
	
	public void setContentType(String type, String subtype) {
		this.type = type.toLowerCase();
		this.subtype = subtype.toLowerCase();
	}
	
	public String getContentType() {
		return type + "/" + subtype;
	}
	
	public void setCharset(String name) {
		this.charset = name;
	}
	
	public String getCharset() {
		return charset;
	}
	
	public void setTransferEncoding(String transferEncoding) {
		this.transferEncoding = transferEncoding.toLowerCase();
	}
	
	public String getTransferEncoding() {
		return transferEncoding;
	}
	
	private void parseContentType(String value) {
		ContentTypeParser parser = new ContentTypeParser(new StringReader(value));
		
		try {
			parser.parseAll();
			this.type = parser.getType().toLowerCase();
			this.subtype = parser.getSubType().toLowerCase();
			this.charset = (String) parser.getParameters().get("charset");
		
			if (this.charset == null && "application/beep+xml".equals(getContentType())) {
				this.charset = "UTF-8";
			} else if (this.charset == null && "text".equals(type)) {
				this.charset = "US-ASCII";
			}
		} catch (ParseException e) {
			// TODO: proper exception type
			throw new IllegalArgumentException(e);
		}
	}
	
	private void parseContentTransferEncoding(String value) {
		value = value.toLowerCase();
		if (Message.BINARY_TRANSFER_ENCODING.equals(value)) {
			transferEncoding = value;
		} else if (Message.BASE64_TRANSFER_ENCODING.equals(value)) {
			transferEncoding = value;
		} else {
			throw new IllegalArgumentException("unknown or unsupported transfer encoding: '" 
					+ value + "'");
		}
	}
	
	public Iterator<String> getHeaderNames() {
		return Collections.unmodifiableCollection(header.keySet()).iterator();
	}
	
	public String getHeader(String name) {
		return header.get(name.toLowerCase());
	}
	
	public synchronized ByteBuffer asByteBuffer() {
		if (buffer == null) {
			StringBuilder builder = new StringBuilder();
			
			builder.append("Content-Type: ");
			builder.append(getContentType());
			if (!"UTF-8".equals(charset)) {
				builder.append("; charset=");
				builder.append(charset);
			}
			builder.append(EOL);
			
			if (!Message.BINARY_TRANSFER_ENCODING.equals(getTransferEncoding())) {
				builder.append("Content-Transfer-Encoding: ");
				builder.append(getTransferEncoding());
				builder.append(EOL);
			}
			
			Iterator<String> names = getHeaderNames();
			while (names.hasNext()) {
				String name = names.next();
				String value = header.get(name);
				builder.append(name);
				builder.append("=");
				builder.append(value);
				builder.append(EOL);
			}
			builder.append(EOL);
			
			CharBuffer chars = CharBuffer.wrap(builder);
			buffer = Charset.forName("US-ASCII").encode(chars);
		}
		
		return buffer.asReadOnlyBuffer();
	}
		
}
