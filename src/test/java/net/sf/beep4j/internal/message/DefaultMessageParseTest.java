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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import junit.framework.TestCase;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;

public class DefaultMessageParseTest extends TestCase {
	
	private ByteBuffer readMessage(String name) throws IOException {
		FileInputStream is = new FileInputStream("data/plain/" + name);
		FileChannel channel = is.getChannel();
		return channel.map(MapMode.READ_ONLY, 0, channel.size());
	}
	
	private String getContent(Message message) throws IOException {
		BufferedReader reader = new BufferedReader(message.getReader());
		StringBuilder builder = new StringBuilder();
		String line;
		
		while ((line = reader.readLine()) != null) {
			builder.append(line).append("\r\n");
		}
		
		return builder.toString();
	}
	
	public void testParse1() throws Exception {
		MessageParser parser = new DefaultMessageParser();
		Message message = parser.parse(readMessage("greeting/i_greeting.txt"));
		assertEquals("application/beep+xml", message.getContentType());
		String content = getContent(message);
		assertEquals(MESSAGE_1, content);
	}

	public void testParse2() throws Exception {
		MessageParser parser = new DefaultMessageParser();
		Message message = parser.parse(readMessage("greeting/l_greeting.txt"));
		assertEquals("application/beep+xml", message.getContentType());
		assertEquals("bar", message.getHeader("Foo"));
		String content = getContent(message);
		assertEquals(MESSAGE_2, content);
	}
	
	public void testParse3() throws Exception {
		MessageParser parser = new DefaultMessageParser();
		Message message = parser.parse(readMessage("greeting/i_greeting_no_headers.txt"));
		assertEquals("application/octet-stream", message.getContentType());
		String content = getContent(message);
		assertEquals(MESSAGE_3, content);
	}
	
	public void testParseRoundTrip() throws Exception {
		MessageBuilder messageBuilder = new DefaultMessageBuilder();
		messageBuilder.addHeader("Foo", "  Bar  ");
		messageBuilder.setCharsetName("UTF-8");
		messageBuilder.setContentType("application", "beep+xml");

		PrintWriter writer = new PrintWriter(messageBuilder.getWriter());
		writer.print(MESSAGE_2);
		writer.close();
		
		Message outMessage = messageBuilder.getMessage();
		MessageParser parser = new DefaultMessageParser();
		Message inMessage = parser.parse(outMessage.asByteBuffer());
		assertEquals("application/beep+xml", inMessage.getContentType());
		assertEquals("Bar", inMessage.getHeader("Foo"));
		String content = getContent(inMessage);
		assertEquals(MESSAGE_2, content);
	}

	private static final String MESSAGE_1 = "<greeting />\r\n";

	private static final String MESSAGE_2 = "<greeting>\r\n"
		+ "   <profile uri='http://iana.org/beep/TLS' />\r\n"
		+ "</greeting>\r\n";
	
	private static final String MESSAGE_3 = "<greeting />\r\n";

	
}
