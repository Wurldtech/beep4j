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
package net.sf.beep4j.internal.tcp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import net.sf.beep4j.Message;
import net.sf.beep4j.internal.message.DefaultMessage;
import net.sf.beep4j.internal.message.MessageHeader;
import net.sf.beep4j.internal.stream.Constants;
import net.sf.beep4j.internal.stream.DataHeader;
import net.sf.beep4j.internal.stream.MessageType;
import net.sf.beep4j.transport.Transport;

import org.easymock.MockControl;

public class DefaultChannelControllerTest extends TestCase {
	
	private MockControl transportCtrl;
	
	private Transport transport;
	
	private ByteBuffer createFrame(int channel, boolean intermediate, 
			long seqno, int start, int size, MessageHeader messageHeader) {
		ByteBuffer headerBuffer = messageHeader.asByteBuffer();
		
		ByteBuffer header = new DataHeader(
				MessageType.MSG, channel, 1, intermediate, 
				seqno, size + headerBuffer.remaining()).asByteBuffer();
		
		ByteBuffer buffer = ByteBuffer.allocate(header.remaining() 
				+ headerBuffer.remaining() + size + 5);
		buffer.put(header);
		buffer.put(createPayload(headerBuffer, start, size));
		buffer.put(Constants.TRAILER_BYTES);
		buffer.flip();
		
		return buffer;
	}
	
	private ByteBuffer createFrame(int channel, boolean intermediate, long seqno, int start, int size) {
		ByteBuffer header = new DataHeader(
				MessageType.MSG, channel, 1, intermediate, 
				seqno, size).asByteBuffer();
		
		ByteBuffer buffer = ByteBuffer.allocate(header.remaining() + size + 5);
		buffer.put(header);
		buffer.put(createPayload(start, size));
		buffer.put(Constants.TRAILER_BYTES);
		buffer.flip();
		
		return buffer;
	}
	
	private Message createMessage(int start, int length) {
		MessageHeader header = new MessageHeader();
		header.addHeader("content-type", "application/beep+xml");
		return new DefaultMessage(header, createPayload(start, length));
	}

	private ByteBuffer createPayload(int start, int size) {
		ByteBuffer buffer = ByteBuffer.allocate(size);
		fill(buffer, start, size);
		buffer.flip();
		return buffer;
	}
	
	private ByteBuffer createPayload(ByteBuffer header, int start, int size) {
		ByteBuffer result = ByteBuffer.allocate(header.capacity() + size);
		result.put(header);
		fill(result, start, size);
		result.flip();
		return result;
	}
	
	private void fill(ByteBuffer buffer, int start, int size) {
		for (int i = start; i < start + size; i++) {
			buffer.put((byte) (i % 128));
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		transportCtrl = MockControl.createStrictControl(Transport.class);
		transportCtrl.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		transport = (Transport) transportCtrl.getMock();
	}
	
	/* 
	 * Tests that sending a message equal to the remaining window size
	 * is sent unfragmented.
	 */
	public void testSendNonFragmented() throws Exception {
		ChannelController target = new DefaultChannelController(transport, 0, 88);
		MessageHeader header = new MessageHeader();
		header.addHeader("content-type", "application/beep+xml");
		
		// define expectations
		transport.sendBytes(createFrame(0, false, 0, 0, 50, header));
		
		// replay
		transportCtrl.replay();
		
		// test
		Message message = createMessage(0, 50);
		target.sendMSG(1, message);
		
		// verify
		transportCtrl.verify();
	}
	
	/*
	 * Tests that a message is fragmented and that the first fragment
	 * is as large as the remaining window size.
	 */
	public void testSendFragmented() throws Exception {
		MessageHeader header = new MessageHeader();
		header.addHeader("content-type", "application/beep+xml");
		
		// define expectations
		transport.sendBytes(createFrame(0, true, 0, 0, 50, header));
		
		// replay
		transportCtrl.replay();
		
		// test
		ChannelController target = new DefaultChannelController(transport, 0, 88);
		target.sendMSG(1, createMessage(0, 60));
		
		// verify
		transportCtrl.verify();
	}
	
	/*
	 * Tests that the updateSendWindow method does what its supposed to do.
	 * The test scenario includes an initial message of length 65 and a
	 * window size of 50. 
	 * 
	 * - sendMessage of size 65
	 *   -> first frame (fragment): first 50 bytes of the original message
	 * - updateSendWindow(10, 50)
	 *   -> second frame (fragment): bytes 51 - 60 of original message
	 * - updateSendWindow(15, 50)
	 *   -> third frame: bytes 61 - 65 of original message
	 */
	public void testUpdateSendWindow() throws Exception {
		ChannelController target = new DefaultChannelController(transport, 0, 88);
		MessageHeader header = new MessageHeader();
		header.addHeader("content-type", "application/beep+xml");
		
		// define expectations
		transport.sendBytes(createFrame(0, true, 0, 0, 50, header));
		transport.sendBytes(createFrame(0, true, 88, 50, 10));
		transport.sendBytes(createFrame(0, false, 98, 60, 5));
		
		// replay
		transportCtrl.replay();
		
		// test
		target.sendMSG(1, createMessage(0, 65));
		target.updateSendWindow(10, 88);
		target.updateSendWindow(15, 88);
		
		// verify
		transportCtrl.verify();
	}
	
	public void testFrameReceived() throws Exception {
		// define expectations
		transportCtrl.replay();
		
		// test
		ChannelController controller = new DefaultChannelController(transport, 0, 4096);
		controller.frameReceived(0, 1024);
		
		// verify
		transportCtrl.verify();
	}
	
	public void testSendUpdateWindow() throws Exception {
		// define expectations
		transport.sendBytes(createSEQFrame(0, 4096, 4096));
		transportCtrl.replay();
		
		// test
		ChannelController controller = new DefaultChannelController(transport, 0, 4096);
		controller.frameReceived(0, 4096);
		
		// verify
		transportCtrl.verify();
	}
	
	public void testSendMultipleUpdateWindow() throws Exception {
		// define expectations
		transport.sendBytes(createSEQFrame(0, 4096, 4096));
		transport.sendBytes(createSEQFrame(0, 8192, 4096));
		transport.sendBytes(createSEQFrame(0, 10240, 4096));
		transportCtrl.replay();
		
		// test
		ChannelController controller = new DefaultChannelController(transport, 0, 4096);
		controller.frameReceived(0, 4096);
		controller.frameReceived(4096, 4096);
		controller.frameReceived(8192, 2048);
		
		// verify
		transportCtrl.verify();
	}
	
	private ByteBuffer createSEQFrame(int channel, long ackno, int window) {
		StringBuilder buf = new StringBuilder(SEQHeader.TYPE);
		buf.append(" ");
		buf.append(channel);
		buf.append(" ");
		buf.append(ackno);
		buf.append(" ");
		buf.append(window);
		buf.append("\r\n");
		return Charset.forName("US-ASCII").encode(buf.toString());
	}

}
