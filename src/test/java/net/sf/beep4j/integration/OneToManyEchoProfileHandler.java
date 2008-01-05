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
package net.sf.beep4j.integration;

import java.nio.ByteBuffer;

import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ext.ChannelHandlerAdapter;

public class OneToManyEchoProfileHandler extends ChannelHandlerAdapter {
	
	public static final String PROFILE = "http://www.iserver.ch/profiles/echo-2";
	
	private final int blockSize;
	
	public OneToManyEchoProfileHandler(int blockSize) {
		this.blockSize = blockSize;
	}
	
	public void messageReceived(Message message, Reply handler) {
		ByteBuffer buffer = message.getContentBuffer();
		int remaining = buffer.remaining();
		
		while (remaining > 0) {
			MessageBuilder builder = createMessageBuilder();
			int size = Math.min(blockSize, remaining);
			buffer.limit(buffer.position() + size);
			builder.getContentBuffer(size).put(buffer);
			handler.sendANS(builder.getMessage());
			remaining -= size;
		}
		
		handler.sendNUL();
	}

}
