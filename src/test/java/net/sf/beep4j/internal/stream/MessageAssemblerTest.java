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
package net.sf.beep4j.internal.stream;

import java.nio.ByteBuffer;

import junit.framework.TestCase;
import net.sf.beep4j.internal.stream.DataHeader;
import net.sf.beep4j.internal.stream.Frame;
import net.sf.beep4j.internal.stream.MessageAssembler;
import net.sf.beep4j.internal.stream.MessageHandler;
import net.sf.beep4j.internal.stream.MessageType;
import net.sf.beep4j.internal.stream.DataHeader.ANSHeader;

import org.easymock.MockControl;

public class MessageAssemblerTest extends TestCase {
	
	private MockControl control;
	private MessageHandler handler;
	private MessageAssembler target;

	@Override
	protected void setUp() throws Exception {
		control = MockControl.createStrictControl(MessageHandler.class);
		control.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
		handler = (MessageHandler) control.getMock();
		target = new MessageAssembler(handler);
	}
	
	@Override
	protected void tearDown() throws Exception {
		control.verify();
	}
	
	private ByteBuffer getByteBuffer(int capacity) {
		return ByteBuffer.allocate(capacity);
	}
	
	public void testMSGNonFragmented() throws Exception {
		handler.receiveMSG(0, 0, null);
		handler.receiveMSG(0, 1, null);
		control.replay();
		
		// test		
		DataHeader header = new DataHeader(MessageType.MSG, 0, 0, false, 0, 10);
		Frame frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.MSG, 0, 1, false, 10, 10);
		frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
	}
	
	public void testMSGFragmented() throws Exception {
		handler.receiveMSG(0, 0, null);
		control.replay();
		
		// test
		DataHeader header = new DataHeader(MessageType.MSG, 0, 0, true, 0, 10);
		Frame frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.MSG, 0, 0, false, 10, 10);
		frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.MSG, 0, 0, true, 20, 10);
		frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
	}
	
	public void testMSGMessageNumberMismatch() throws Exception {
		control.replay();
		
		DataHeader header = new DataHeader(MessageType.MSG, 0, 0, true, 0, 10);
		// test
		Frame frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.MSG, 0, 1, false, 10, 10);
		frame = new Frame(header, getByteBuffer(10));
		try {
			target.handleFrame(frame);
			fail("message numbers are not equal");
		} catch (Exception e) {
			// expected
		}
	}
	
	public void testMSGInvalidSuccessor() throws Exception {
		control.replay();
		
		// test
		DataHeader header = new DataHeader(MessageType.MSG, 1, 0, true, 0, 20);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.RPY, 1, 0, false, 20, 20);
		frame = new Frame(header, getByteBuffer(20));
		try {
			target.handleFrame(frame);
			fail("invalid successor not detected");
		} catch (Exception e) {
			// expected
		}
	}
	
	public void testANSNonFragmented() throws Exception {
		handler.receiveANS(1, 0, 0, null);
		handler.receiveANS(1, 0, 1, null);
		handler.receiveNUL(1, 0);
		control.replay();
		
		// test
		DataHeader header = new ANSHeader(1, 0, false, 0, 20, 0);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 0, false, 20, 20, 1);
		frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.NUL, 1, 0, false, 40, 0);
		frame = new Frame(header, getByteBuffer(0));
		target.handleFrame(frame);
	}
	
	public void testANSFragmented() throws Exception {
		handler.receiveANS(1, 0, 0, null);
		handler.receiveNUL(1, 0);
		control.replay();
		
		// test
		DataHeader header = new ANSHeader(1, 0, true, 0, 20, 0);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 0, true, 20, 20, 0);
		frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 0, false, 40, 20, 0);
		frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.NUL, 1, 0, false, 60, 0);
		frame = new Frame(header, getByteBuffer(0));
		target.handleFrame(frame);
	}
	
	public void testANSInterleaved() throws Exception {
		handler.receiveANS(1, 0, 0, null);
		handler.receiveANS(1, 0, 1, null);
		handler.receiveNUL(1, 0);
		control.replay();
		
		// test
		DataHeader header = new ANSHeader(1, 0, true, 0, 20, 0);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 0, true, 20, 20, 1);
		frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 0, false, 40, 10, 0);
		frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 0, false, 50, 10, 1);
		frame = new Frame(header, getByteBuffer(10));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.NUL, 1, 0, false, 60, 0);
		frame = new Frame(header, getByteBuffer(0));
		target.handleFrame(frame);
	}

	public void testANSInvalidSuccessor() throws Exception {
		control.replay();
		
		// test
		DataHeader header = new ANSHeader(1, 0, true, 0, 20, 0);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.RPY, 1, 0, false, 20, 20);
		frame = new Frame(header, getByteBuffer(20));
		try {
			target.handleFrame(frame);
			fail("ANS or NUL expected");
		} catch (Exception e) {
			// expected
		}
	}
	
	public void testANSMessageNumberMismatch() throws Exception {
		handler.receiveANS(1, 0, 0, null);
		control.replay();
		
		// test
		DataHeader header = new ANSHeader(1, 0, false, 0, 20, 0);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new ANSHeader(1, 1, false, 20, 20, 1);
		frame = new Frame(header, getByteBuffer(20));
		try {
			target.handleFrame(frame);
			fail("message number mismatch");
		} catch (Exception e) {
			// expected
		}
	}
	
	public void testANSUnfinishedAnswers() throws Exception {
		control.replay();
		
		// test
		DataHeader header = new ANSHeader(1, 0, true, 0, 20, 0);
		Frame frame = new Frame(header, getByteBuffer(20));
		target.handleFrame(frame);
		
		header = new DataHeader(MessageType.NUL, 1, 0, false, 20, 0);
		frame = new Frame(header, getByteBuffer(20));
		try {
			target.handleFrame(frame);
			fail("response has unfinished ANS messages");
		} catch (Exception e) {
			// expected
		}
	}
	
	public void testNUL() throws Exception {
		handler.receiveNUL(1, 0);
		control.replay();
		
		// test
		DataHeader header = new DataHeader(MessageType.NUL, 1, 0, false, 0, 0);
		Frame frame = new Frame(header, getByteBuffer(0));
		target.handleFrame(frame);
		
		// verify
		control.verify();
	}
	
	public void testNULInvalid() throws Exception {
		control.replay();
		
		// test
		DataHeader header = new DataHeader(MessageType.NUL, 1, 0, true, 0, 0);
		Frame frame = new Frame(header, getByteBuffer(0));
		
		try {
			target.handleFrame(frame);
			fail("NUL frame cannot have continuation indicator set to true");
		} catch (Exception e) {
			// exected
			// TODO: correct exception type
		}
		
		header = new DataHeader(MessageType.NUL, 1, 0, false, 0, 20);
		frame = new Frame(header, getByteBuffer(20));
		
		try {
			target.handleFrame(frame);
			fail("NUL frame cannot have non-zero size");
		} catch (Exception e) {
			// exected
			// TODO: correct exception type
		}

		// verify
		control.verify();
	}
	
}
