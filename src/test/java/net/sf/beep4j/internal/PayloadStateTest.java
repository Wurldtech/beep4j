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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import org.easymock.MockControl;

public class PayloadStateTest extends TestCase {
	
	private Charset charset = Charset.forName("US-ASCII");
	
	public void testProcess() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handlePayload(charset.encode("123456789"));
		control.replay();
		
		Charset charset = Charset.forName("US-ASCII");
		ByteBuffer buffer = charset.encode("1234567890");
		
		ParseState state = new PayloadState(9);
		assertTrue(state.process(buffer, context));
		
		assertEquals(9, buffer.position());
		
		control.verify();
	}
	
	public void testProcessTwice() throws Exception {
		MockControl control = MockControl.createControl(ParseStateContext.class);
		ParseStateContext context = (ParseStateContext) control.getMock();
		
		context.handlePayload(charset.encode("123456789"));
		control.replay();
		
		Charset charset = Charset.forName("US-ASCII");
		
		ParseState state = new PayloadState(9);
		
		ByteBuffer buffer = charset.encode("12345");
		assertFalse(state.process(buffer, context));
		assertEquals(5, buffer.position());
		
		buffer = charset.encode("67890");
		assertTrue(state.process(buffer, context));
		assertEquals(4, buffer.position());
		
		control.verify();
	}
	
}
