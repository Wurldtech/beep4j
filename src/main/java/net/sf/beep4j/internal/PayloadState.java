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

/**
 * ParseState responsible to read the payload of a frame.
 * 
 * @author Simon Raess
 */
final class PayloadState implements ParseState {
	
	/**
	 * The number of bytes to read.
	 */
	private int size;
	
	/**
	 * The current position. This number specifies how many bytes
	 * have already been read.
	 */
	private int position;
	
	/**
	 * Holds the read payload.
	 */
	private ByteBuffer payload;
	
	/**
	 * Creates a new PayloadState that reads exactly <var>payloadSize</var> 
	 * bytes from the incoming buffers.
	 * 
	 * @param payloadSize the number of bytes in the payload
	 */
	PayloadState(int payloadSize) {
		this.position = 0;
		this.size = payloadSize;
		this.payload = ByteBuffer.allocate(payloadSize);
	}
	
	public final boolean process(ByteBuffer buffer, ParseStateContext context) {
		// calculate the number of bytes to be read
		int available = buffer.remaining();
		int remaining = size - position;
		int actual = Math.min(available, remaining);
		
		// write actual number of bytes to the payload buffer
		int limit = buffer.limit();
		buffer.limit(buffer.position() + actual);
		payload.put(buffer);
		buffer.limit(limit);
		
		// update the position
		position += actual;
		
		// have we read enough?
		if (position == size) {
			payload.flip();			
			context.handlePayload(payload);
			return buffer.hasRemaining();
		}
		
		return false;
	}
	
}
