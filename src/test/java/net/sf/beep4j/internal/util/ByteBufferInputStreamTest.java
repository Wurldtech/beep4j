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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import junit.framework.TestCase;

public class ByteBufferInputStreamTest extends TestCase {
	
	private ByteBuffer getTestBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(50);
		for (byte b = 25; b < 75; b++) {
			buffer.put(b);
		}
		buffer.flip();
		return buffer;
	}
	
	private void assertEndOfStream(InputStream stream) throws Exception {
		assertEquals(-1, stream.read());
	}
	
	private void assertArrayEquals(byte[] a1, byte[] a2) {
		assertTrue(Arrays.equals(a1, a2));
	}
	
	public void testReadEmpty() throws Exception {
		InputStream stream = new ByteBufferInputStream(ByteBuffer.allocate(0));
		assertEndOfStream(stream);
	}
	
	public void testRead() throws Exception {
		InputStream stream = new ByteBufferInputStream(getTestBuffer());
		
		for (int b = 25; b < 75; b++) {
			assertEquals(b, stream.read());
			assertEquals(74 - b, stream.available());
		}
		
		assertEndOfStream(stream);
	}
	
	public void testMarkReset() throws Exception {
		InputStream stream = new ByteBufferInputStream(getTestBuffer());
		
		for (byte b = 25; b < 30; b++) {
			assertEquals(b, stream.read());
		}
		
		stream.mark(Integer.MAX_VALUE);
		
		for (byte b = 30; b < 40; b++) {
			assertEquals(b, stream.read());
		}
		
		stream.reset();
		assertEquals(45, stream.available());

		for (byte b = 30; b < 40; b++) {
			assertEquals(b, stream.read());
		}
	}
	
	public void testBulkReadEmpty() throws Exception {
		InputStream stream = new ByteBufferInputStream(ByteBuffer.allocate(0));
		assertEquals(-1, stream.read(new byte[10]));
	}
	
	public void testBulkRead() throws Exception {
		InputStream stream = new ByteBufferInputStream(getTestBuffer());
		byte[] buf = new byte[10];
		
		assertEquals(10, stream.read(buf));
		assertArrayEquals(new byte[] { 25, 26, 27, 28, 29, 30, 31, 32, 33, 34 }, buf);
		
		assertEquals(7, stream.read(buf, 2, 7));
		assertArrayEquals(new byte[] { 25, 26, 35, 36, 37, 38, 39, 40, 41, 34 }, buf);
		
		assertEquals(8, stream.read(buf, 0, 8));
		assertArrayEquals(new byte[] { 42, 43, 44, 45, 46, 47, 48, 49, 41, 34 }, buf);
		
		assertEquals(25, stream.available());
		assertEquals(25, stream.read(new byte[30]));
		assertEndOfStream(stream);
	}

}
