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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;

public class DefaultMessageBuilder implements MessageBuilder {
	
	private final MessageHeader header;
	
	private final ByteArrayOutputStream target;
	
	private ByteBuffer buffer;
	
	private String charset;
	
	public DefaultMessageBuilder() {
		header = new MessageHeader();
		target = new ByteArrayOutputStream();
	}
	
	public void addHeader(String name, String value) {
		header.addHeader(name, value);
	}
	
	public void setCharsetName(String charset) {
		this.charset = charset;
		header.setCharset(charset);
	}
	
	public void setContentType(String type, String subtype) {
		header.setContentType(type, subtype);
	}

	public OutputStream getOutputStream() {
		return target;
	}

	public Writer getWriter() {
		Charset charset = Charset.forName(this.charset);
		Writer writer = new OutputStreamWriter(getOutputStream(), charset);
		return writer;
	}
	
	public ByteBuffer getContentBuffer(int size) {
		buffer = ByteBuffer.allocate(size);
		return buffer;
	}
	
	public Message getMessage() {
		if (buffer == null) {
			return new DefaultMessage(header, ByteBuffer.wrap(target.toByteArray()));
		} else {
			buffer.flip();
			return new DefaultMessage(header, buffer.asReadOnlyBuffer());
		}
	}

}
