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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import junit.framework.TestCase;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;

public class DefaultMessageBuilderTest extends TestCase {
	
	private ByteBuffer getMessage(String name) throws IOException {
		FileInputStream fis = new FileInputStream("data/plain/" + name);
		FileChannel channel = fis.getChannel();
		ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
		return buffer;
	}
	
	public void testBuild() throws Exception {
		MessageBuilder builder = new DefaultMessageBuilder();
		builder.setContentType("application", "beep+xml");
		builder.setCharsetName("UTF-8");
		
		PrintWriter writer = new PrintWriter(builder.getWriter());
		writer.print("<greeting />\r\n");
		writer.close();
		
		Message message = builder.getMessage();
		ByteBuffer buffer = message.asByteBuffer();
		assertEquals(getMessage("greeting/i_greeting.txt"), buffer);
	}
	
}
