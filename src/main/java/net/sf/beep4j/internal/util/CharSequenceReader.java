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

/**
 * Reader implementation that reads from a CharSequence. It's odd that
 * this kind of reader does not exist in the standard Java library. It
 * is similar to a StringReader but more generally applicable.
 * 
 * @author Simon Raess
 */
public class CharSequenceReader extends Reader {

	private final CharSequence sequence;

	private int position;
	
	private boolean closed;
	
	public CharSequenceReader(CharSequence sequence) {
		Assert.notNull("sequence", sequence);
		this.sequence = sequence;
	}
	
	private int remaining() {
		return sequence.length() - position;
	}
	
	private void checkClosed() throws IOException {
		if (closed) {
			throw new IOException("stream is closed");
		}
	}
	
	@Override
	public int read() throws IOException {
		checkClosed();
		if (position >= sequence.length()) {
			return -1;
		} else {
			return sequence.charAt(position++);
		}
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		checkClosed();
		int length = Math.min(len, remaining());
		for (int i = 0; i < length; i++) {
			cbuf[off + i] = sequence.charAt(position++);
		}
		return length > 0 ? length : -1;
	}

	@Override
	public void close() throws IOException {
		closed = true;
	}
		
}
