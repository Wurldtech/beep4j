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

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import junit.framework.TestCase;

public class CharSequenceReaderTest extends TestCase {
	
	private static final String INPUT = "abcdefghijklmnopqrstuvwxyz";

	private void assertEndOfStream(Reader reader) throws IOException {
		assertEquals(-1, reader.read());
	}
	
	private void assertArrayEquals(char[] a1, char[] a2) {
		assertTrue(Arrays.equals(a1, a2));
	}
	
	public void testReadEmpty() throws Exception {
		Reader reader = new CharSequenceReader("");
		assertEndOfStream(reader);
	}
	
	public void testRead() throws Exception {
		Reader reader = new CharSequenceReader(INPUT);
		
		for (char c = 'a'; c <= 'z'; c++) {
			assertEquals(c, reader.read());
		}
		
		assertEndOfStream(reader);
	}
	
	public void testBulkReadEmpty() throws Exception {
		Reader reader = new CharSequenceReader("");
		assertEquals(-1, reader.read(new char[10]));
	}
	
	public void testBuldRead() throws Exception {
		Reader reader = new CharSequenceReader(INPUT);
		char[] buf = new char[10];
		
		assertEquals(10, reader.read(buf));
		assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' }, buf);
		
		assertEquals(7, reader.read(buf, 2, 7));
		assertArrayEquals(new char[] { 'a', 'b', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'j' }, buf);
		
		assertEquals(9, reader.read(buf));
		assertArrayEquals(new char[] { 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'j' }, buf);
		
		assertEndOfStream(reader);
	}
	
}
