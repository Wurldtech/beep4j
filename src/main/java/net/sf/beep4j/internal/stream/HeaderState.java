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
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import net.sf.beep4j.ProtocolException;

/**
 * ParseState to read the header of a BEEP message. If a valid header
 * is found, the method {@link ParseStateContext#handleHeader(String[])}
 * is invoked and the tokenized header is passed in.
 * 
 * @author Simon Raess
 */
final class HeaderState implements ParseState {
	
	private static final int NOT_FOUND = -1;

	private static final byte CR = '\r';
	
	private static final byte LF = '\n';
	
	private static final int MAX_HEADER_LENGTH = 61;

	private final ByteBuffer tmp = ByteBuffer.allocate(MAX_HEADER_LENGTH);
	
	public boolean process(ByteBuffer buffer, ParseStateContext context) {
		int position = buffer.position();
		int index = findLF(buffer);
		
		if (index != NOT_FOUND) {
			checkHeaderLength(tmp.position() + index - 1);
			
			int limit = buffer.limit();
			buffer.limit(index + position);			
			tmp.put(buffer);
			buffer.limit(limit);
			tmp.flip();
			
			// Verify that the CR is present
			tmp.position(tmp.limit() - 1);
			if (tmp.get() != CR) {
				throw new ProtocolException("found LF but missing CR in header");
			}
			tmp.rewind();
			
			// Rewind to before the CR
			tmp.limit(tmp.limit() - 1);
			
			// Step past the LF
			buffer.position(buffer.position() + 1);
			
			String[] tokens = tokenize(tmp);	
			context.handleHeader(tokens);
			
			tmp.clear();
			
			return buffer.hasRemaining();
			
		} else {
			checkHeaderLength(tmp.position() + buffer.remaining() - 1);
			tmp.put(buffer);
			return false;
		}
	}

	private void checkHeaderLength(int length) {
		if (length > MAX_HEADER_LENGTH) {
			throw new ProtocolException("header longer than maximum: " 
					+ length + " > " + MAX_HEADER_LENGTH);
		}
	}
		
	private String[] tokenize(ByteBuffer header) {
		boolean space = false;
		
		ByteBuffer copy = header.asReadOnlyBuffer();
		int remaining = copy.remaining();
		for (int i = 0; i < remaining; i++) {
			byte current = copy.get();
			if (space && current == (byte) ' ') {
				throw new ProtocolException("two consecutive spaces in header");
			}
			space = current == (byte) ' ';
		}
		
		Charset charset = Charset.forName("US-ASCII");
		CharBuffer buffer = charset.decode(header);
			
		String[] tmp = buffer.toString().split(" ");			
		return tmp;
	}
	
	private int findLF(ByteBuffer buf) {
		if (buf.hasRemaining()) {
			buf.mark();
			int remaining = buf.remaining();

			for (int i = 0; i < remaining; i++) {
				if (buf.get() == LF) {
					buf.reset();
					return i;
				}
			}

			buf.reset();
		}

		return NOT_FOUND;
	}

}
