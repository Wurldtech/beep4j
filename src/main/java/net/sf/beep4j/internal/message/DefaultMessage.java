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

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;

import net.sf.beep4j.Message;
import net.sf.beep4j.internal.util.ByteBufferInputStream;
import net.sf.beep4j.internal.util.CharSequenceReader;

public class DefaultMessage implements Message {
	
	private final ByteBuffer content;
	
	private final MessageHeader header;
	
	private ByteBuffer buffer;
	
	public DefaultMessage(MessageHeader header, ByteBuffer content) {
		this.header = header;
		this.content = content;
	}
		
	public String getContentType() {
		return header.getContentType();
	}
	
	protected String getCharsetName() {
		return header.getCharset();
	}
	
	protected String getTransferEncoding() {
		return header.getTransferEncoding();
	}
	
	public Iterator<String> getHeaderNames() {
		return header.getHeaderNames();
	}
	
	public String getHeader(String name) {
		return header.getHeader(name);
	}
	
	public InputStream getInputStream() {
		ByteBuffer buffer = content.asReadOnlyBuffer();
		return new ByteBufferInputStream(buffer);
	}
	
	public Reader getReader() {
		if (getCharsetName() == null) {
			throw new IllegalStateException("no charset has been defined, "
					+ "use method with charset parameter");
		}
		Charset charset = Charset.forName(getCharsetName());
		return getReader(charset);
	}
	
	public Reader getReader(String charsetName) {
		Charset charset = Charset.forName(charsetName);
		return getReader(charset);
	}
	
	private Reader getReader(Charset charset) {
		CharBuffer buffer = charset.decode(content.asReadOnlyBuffer());
		return new CharSequenceReader(buffer);
	}
	
	public ByteBuffer getContentBuffer() {
		return content.asReadOnlyBuffer();
	}
	
	public synchronized ByteBuffer asByteBuffer() {
		if (buffer == null) {
			ByteBuffer header = this.header.asByteBuffer();
			ByteBuffer content = this.content.asReadOnlyBuffer();
			buffer = ByteBuffer.allocate(header.remaining() + content.remaining());
			buffer.put(header);
			buffer.put(content);
			buffer.flip();
		}
		return buffer.asReadOnlyBuffer();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (obj instanceof Message) {
			Message m = (Message) obj;
			return asByteBuffer().equals(m.asByteBuffer());
		} else {
			return false;
		}
	}
	
}
