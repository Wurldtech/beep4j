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

import junit.framework.TestCase;

public class SlidingWindowTest extends TestCase {
	
	public void testSlide() throws Exception {
		SlidingWindow window = new SlidingWindow(50);
		window.moveBy(10);
		window.slide(10, 50);
		assertEquals(10, window.getPosition());
		assertEquals(50, window.getWindowSize());
		assertEquals(10, window.getStart());
		assertEquals(60, window.getEnd());
		assertEquals(50, window.remaining());
	}
	
	public void testMoveBy() throws Exception {
		SlidingWindow window = new SlidingWindow(50);
		assertEquals(0, window.getStart());
		assertEquals(0, window.getPosition());
		assertEquals(50, window.remaining());
		assertEquals(50, window.getEnd());
		window.moveBy(50);
		assertEquals(0, window.getStart());
		assertEquals(50, window.getPosition());
		assertEquals(0, window.remaining());
		assertEquals(50, window.getEnd());
	}
	
	public void testSlideAround() throws Exception {
		SlidingWindow window = new SlidingWindow(SlidingWindow.MAX - 10, 50);
		assertEquals(SlidingWindow.MAX - 10, window.getStart());
		assertEquals(SlidingWindow.MAX - 10, window.getPosition());
		assertEquals(50, window.remaining());
		assertEquals(39, window.getEnd());
		
		window.moveBy(10);
		assertEquals(SlidingWindow.MAX - 10, window.getStart());
		assertEquals(SlidingWindow.MAX, window.getPosition());
		assertEquals(40, window.remaining());
		assertEquals(39, window.getEnd());
		
		window.slide(SlidingWindow.MAX, 50);
		assertEquals(SlidingWindow.MAX, window.getStart());
		assertEquals(SlidingWindow.MAX, window.getPosition());
		assertEquals(50, window.remaining());
		assertEquals(49, window.getEnd());
	}
	
	public void testSlideOver() throws Exception {
		SlidingWindow window = new SlidingWindow(50);
		try {
			window.slide(10, 50);
			fail("sliding the window over the current position is an error");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testMoveRightWindowEdgeToTheLeft() throws Exception {
		SlidingWindow window = new SlidingWindow(50);
		try {
			window.slide(0, 49);
			fail("moving the right window edge to the left is not possible");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testMovePositionOverEnd() throws Exception {
		SlidingWindow window = new SlidingWindow(50);
		try {
			window.moveBy(51);
			fail("moving the position over the end is not possible");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testMovePositionOverEndWithWrapping() throws Exception {
		SlidingWindow window = new SlidingWindow(SlidingWindow.MAX - 10, 50);
		window.moveBy(30);
		assertEquals(19, window.getPosition());
		try {
			window.moveBy(21);
			fail("moving the position over the end is not possible");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

}
