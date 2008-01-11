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

import net.sf.beep4j.internal.stream.ParseState;
import net.sf.beep4j.internal.stream.ParseStateContext;
import net.sf.beep4j.internal.stream.TrailerState;

import org.easymock.MockControl;

public class TrailerStateTest extends TestCase {
	
	public void testProcess() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleTrailer();
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer = charset.encode("END\r\n");
		
		ParseState state = new TrailerState();
		assertFalse(state.process(buffer, context));
		
		assertEquals(5, buffer.position());
		
		control.verify();
	}
	
	public void testProcessMultiPass() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleTrailer();
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ParseState state = new TrailerState();

		ByteBuffer buffer = charset.encode("E");
		assertFalse(state.process(buffer, context));
		assertEquals(1, buffer.position());
		
		buffer = charset.encode("ND");
		assertFalse(state.process(buffer, context));
		assertEquals(2, buffer.position());
		
		buffer = charset.encode("\r\nMSG");
		assertTrue(state.process(buffer, context));
		assertEquals(2, buffer.position());
		
		control.verify();
	}
	
	public void testProcessZeroLength() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleTrailer();
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ParseState state = new TrailerState();

		ByteBuffer buffer = charset.encode("E");
		assertFalse(state.process(buffer, context));
		assertEquals(1, buffer.position());
		
		buffer = charset.encode("");
		assertFalse(state.process(buffer, context));
		assertEquals(0, buffer.position());
		
		buffer = charset.encode("ND\r\nMSG");
		assertTrue(state.process(buffer, context));
		assertEquals(4, buffer.position());
		
		control.verify();
	}
	
	public void testProcessComplex() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleTrailer();
		context.handleTrailer();
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ParseState state = new TrailerState();
		
		ByteBuffer buffer = charset.encode("END\r\n");
		assertFalse(state.process(buffer, context));		
		assertEquals(5, buffer.position());
		
		buffer = charset.encode("END\r\nxy");
		assertTrue(state.process(buffer, context));		
		assertEquals(5, buffer.position());
		
		control.verify();
	}
	
	public void testProcessNonZeroPosition() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		control.setDefaultMatcher(MockControl.ARRAY_MATCHER);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handleTrailer();
		control.replay();

		Charset charset = Charset.forName("US-ASCII");
		ParseState state = new TrailerState();
		
		ByteBuffer buffer = charset.encode("abcdefgEND\r\n");
		buffer.position(7);
		assertFalse(state.process(buffer, context));
		assertEquals(12, buffer.position());
		
		control.verify();
	}

}
