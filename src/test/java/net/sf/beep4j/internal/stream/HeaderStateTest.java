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
import java.nio.charset.Charset;

import junit.framework.TestCase;

import net.sf.beep4j.internal.stream.HeaderState;
import net.sf.beep4j.internal.stream.ParseState;
import net.sf.beep4j.internal.stream.ParseStateContext;

import org.easymock.MockControl;

public class HeaderStateTest extends TestCase {
	
	public void testProcessMappingFrame() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleHeader(new String[] { "SEQ", "0", "0", "4096" });
		control.replay();
		
		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer = charset.encode("SEQ 0 0 4096\r\n");
				
		ParseState state = new HeaderState();
		assertFalse(state.process(buffer, context));
		
		assertEquals(14, buffer.position());
		
		control.verify();
	}
	
	public void testProcessDataFrame() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleHeader(new String[] { "MSG", "0", "0", ".", "0", "100" });
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer = charset.encode("MSG 0 0 . 0 100\r\nbluberi");
		
		ParseState state = new HeaderState();
		assertTrue(state.process(buffer, context));
		
		assertEquals(17, buffer.position());
		
		control.verify();
	}
	
	public void testProcessMultiPass() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleHeader(new String[] { "MSG", "0", "0", ".", "0", "100" });
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer = charset.encode("MSG 0 0 .");
		
		ParseState state = new HeaderState();
		assertFalse(state.process(buffer, context));
		assertEquals(9, buffer.position());
		
		buffer = charset.encode(" 0 100\r\nbluberi");
		assertTrue(state.process(buffer, context));
		assertEquals(8, buffer.position());
		
		control.verify();
	}
	
	public void testProcessComplex() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleHeader(new String[] { "MSG", "0", "0", ".", "0", "100" });
		context.handleHeader(new String[] { "MSG", "2", "1", ".", "0", "100" });
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ParseState state = new HeaderState();
		
		ByteBuffer buffer = charset.encode("MSG 0 0 . 0 100\r\n");
		assertFalse(state.process(buffer, context));
		assertEquals(17, buffer.position());
		
		buffer = charset.encode("MSG 2 1 . 0 100\r\n");
		assertFalse(state.process(buffer, context));
		assertEquals(17, buffer.position());
		
		control.verify();
	}
	
	public void testProcessNonZeroPosition() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleHeader(new String[] { "MSG", "0", "0", ".", "0", "100" });
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ParseState state = new HeaderState();
		
		ByteBuffer buffer = charset.encode("abcdefgMSG 0 0 . 0 100\r\n");
		buffer.position(7);
		assertFalse(state.process(buffer, context));
		assertEquals(24, buffer.position());
		
		control.verify();
	}
	
	public void testProcessSplitHeader() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();

		context.handleHeader(new String[] { "MSG", "0", "0", ".", "0", "100" });
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer1 = charset.encode("MSG 0 0 . 0 100\r");

		ParseState state = new HeaderState();
		assertFalse(state.process(buffer1, context));
		assertEquals(16, buffer1.position());

		ByteBuffer buffer2 = charset.encode("\nbluberi");

		assertTrue(state.process(buffer2, context));
		assertEquals(1, buffer2.position());

		control.verify();
	}

	public void testProcessFullySplitHeader() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();

		context.handleHeader(new String[] { "MSG", "0", "0", ".", "0", "100" });
		control.replay();

		Charset charset = Charset.forName("US-ASCII");

		char[] toSplit = "MSG 0 0 . 0 100\r\n".toCharArray();

		ParseState state = new HeaderState();

		for (char ch : toSplit) {
			ByteBuffer buffer = charset.encode(String.valueOf(ch));

			assertFalse(state.process(buffer, context));
			assertEquals(1, buffer.position());
		}

		control.verify();
	}
	
}
