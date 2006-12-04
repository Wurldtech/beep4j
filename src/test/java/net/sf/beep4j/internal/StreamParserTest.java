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
package net.sf.beep4j.internal;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import org.easymock.MockControl;

public class StreamParserTest extends TestCase {
	
	private MockControl handlerCtrl;

	private FrameHandler handler;
	
	private MockControl mappingCtrl;
	
	private TransportMapping mapping;

	private ByteBuffer getMessage(String name) throws Exception {
		File file = new File("data/" + name);
		FileInputStream stream = new FileInputStream(file);
		FileChannel channel = stream.getChannel();
		return channel.map(MapMode.READ_ONLY, 0, channel.size());
	}
	
	protected void setUp() {
		handlerCtrl = MockControl.createStrictControl(FrameHandler.class);
		handler = (FrameHandler) handlerCtrl.getMock();
		mappingCtrl = MockControl.createStrictControl(TransportMapping.class);
		mapping = (TransportMapping) mappingCtrl.getMock();
	}
	
	public void testOneFrame() throws Exception {
		// setup
		Charset charset = Charset.forName("UTF-8");
		DataHeader header = new DataHeader(MessageType.RPY, 0, 0, false, 0, 52);
		Frame frame = new Frame(
				header,
				charset.encode("Content-Type: application/beep+xml\r\n\r\n<greeting />\r\n"));
		
		// define expectations
		handler.handleFrame(frame);		
		mapping.checkFrame(0, 0, 52);
		mapping.frameReceived(0, 0, 52);
		
		// replay
		mappingCtrl.replay();
		handlerCtrl.replay();
		
		// test
		StreamParser parser = new DefaultStreamParser(handler, mapping);
		ByteBuffer buffer = getMessage("greeting/i_greeting.txt");		
		parser.process(buffer);
		
		// verify
		mappingCtrl.verify();
		handlerCtrl.verify();
	}
	
	public void testTwoFrames() throws Exception {
		// setup
		Charset charset = Charset.forName("UTF-8");
		DataHeader header = new DataHeader(MessageType.RPY, 0, 0, false, 0, 52);
		Frame frame = new Frame(
				header,
				charset.encode("Content-Type: application/beep+xml\r\n\r\n<greeting />\r\n"));
		
		// define expectations
		handler.handleFrame(frame);		
		mapping.checkFrame(0, 0, 52);
		mapping.frameReceived(0, 0, 52);
		handler.handleFrame(frame);		
		mapping.checkFrame(0, 0, 52);
		mapping.frameReceived(0, 0, 52);

		// replay
		mappingCtrl.replay();
		handlerCtrl.replay();

		// test
		StreamParser parser = new DefaultStreamParser(handler, mapping);
		parser.process(getMessage("greeting/i_greeting.txt"));
		parser.process(getMessage("greeting/i_greeting.txt"));
		
		// verify
		mappingCtrl.verify();
		handlerCtrl.verify();
	}
	
	public void testMappingFrame() throws Exception {
		// define expectations
		mapping.processMappingFrame(new String[] { "SEQ", "0", "0", "4096" });
		mappingCtrl.setMatcher(MockControl.ARRAY_MATCHER);
		
		// replay
		mappingCtrl.replay();
		handlerCtrl.replay();
		
		// test
		StreamParser parser = new DefaultStreamParser(handler, mapping);
		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer = charset.encode("SEQ 0 0 4096\r\n");
		parser.process(buffer);
		
		// verify
		mappingCtrl.verify();
		handlerCtrl.verify();
	}
	
	public void testMappingFollowedByDataFrame() throws Exception {
		// setup
		Charset charset = Charset.forName("US-ASCII");
		DataHeader header = new DataHeader(MessageType.RPY, 0, 0, false, 0, 52);
		Frame frame = new Frame(
				header,
				charset.encode("Content-Type: application/beep+xml\r\n\r\n<greeting />\r\n"));
		
		// define expectations
		mapping.processMappingFrame(new String[] { "SEQ", "0", "0", "4096" });
		mappingCtrl.setMatcher(MockControl.ARRAY_MATCHER);
		handler.handleFrame(frame);		
		mapping.checkFrame(0, 0, 52);
		mapping.frameReceived(0, 0, 52);
		
		// replay
		mappingCtrl.replay();
		handlerCtrl.replay();
		
		// test
		StreamParser parser = new DefaultStreamParser(handler, mapping);
		ByteBuffer buffer = charset.encode("SEQ 0 0 4096\r\n");
		parser.process(buffer);
		parser.process(getMessage("greeting/i_greeting.txt"));
		
		// verify
		mappingCtrl.verify();
		handlerCtrl.verify();
	}
		
}
