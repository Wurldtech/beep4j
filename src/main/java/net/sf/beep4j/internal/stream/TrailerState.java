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

import net.sf.beep4j.ProtocolException;

/**
 * ParseState that expects a valid trailer. A BEEP trailer consists of
 * the following character sequence:
 * 
 * <pre>
 *   END&lt;CR>&lt;LF>
 * </pre>
 * 
 * If the characters at the current position do not consist of that sequence
 * then the BEEP peers have lost synchronization.
 * 
 * @author Simon Raess
 */
final class TrailerState implements ParseState {
	
	private static final String TRAILER = "END\r\n";

	/**
	 * The trailer consists of 5 bytes. We can allocate and reuse a 
	 * single buffer. 
	 */
	private ByteBuffer tmp = ByteBuffer.allocate(5);
	
	public final String getName() {
		return "trailer";
	}
	
	public final boolean process(ByteBuffer buffer, ParseStateContext context) {
		if (tmp.capacity() - tmp.remaining() + buffer.remaining() >= 5) {
			int remaining = tmp.remaining();
			
			// get the next five characters encoded in US-ASCII
			Charset charset = Charset.forName("US-ASCII");
			ByteBuffer copy = buffer.asReadOnlyBuffer();
			copy.limit(copy.position() + remaining);
			tmp.put(copy);
			tmp.flip();
			
			String trailer = charset.decode(tmp).toString();
				
			if (!TRAILER.equals(trailer)) {
				throw new ProtocolException("expected 'END<CR><LF>' (was '" + trailer + "'");
			}
			
			// move the position past the header
			buffer.position(buffer.position() + remaining);
			
			// and clear the temporary buffer
			tmp.clear();
			
			context.handleTrailer();
			return buffer.hasRemaining();
			
		} else {
			tmp.put(buffer);
		}

		return false;
	}
}
