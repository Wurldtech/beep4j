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
package net.sf.beep4j.internal.util;

import junit.framework.TestCase;

public class IntegerSequenceTest extends TestCase {
	
	public void testDefaultSequence() throws Exception {
		IntegerSequence sequence = new IntegerSequence();
		for (int i = 0; i < 50; i++) {
			assertEquals(i, sequence.next().intValue());
		}
	}
	
	public void testStartAt10() throws Exception {
		IntegerSequence sequence = new IntegerSequence(10, 1);
		for (int i = 10; i < 60; i++) {
			assertEquals(i, sequence.next().intValue());
		}
	}
	
	public void testIncrementBy2() throws Exception {
		IntegerSequence sequence = new IntegerSequence(1, 2);
		for (int i = 1; i < 20; i += 2) {
			assertEquals(i, sequence.next().intValue());
		}
	}
	
	public void testWrapAround() throws Exception {
		IntegerSequence sequence = new IntegerSequence(Integer.MAX_VALUE - 2, 1);
		assertEquals(Integer.MAX_VALUE - 2, sequence.next().intValue());
		assertEquals(Integer.MAX_VALUE - 1, sequence.next().intValue());
		assertEquals(Integer.MAX_VALUE - 0, sequence.next().intValue());
		assertEquals(0, sequence.next().intValue());
	}
	
}
