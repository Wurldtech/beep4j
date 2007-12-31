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

import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.transport.Transport;

/**
 * Represents a BEEP frame, which consists of a header ({@link DataHeader})
 * and a {@link ByteBuffer} content.
 * 
 * @author Simon Raess
 */
public final class Frame {
	
	/**
	 * The header of this frame.
	 */
	private final DataHeader header;
	
	/**
	 * The payload data of this frame.
	 */
	private final ByteBuffer payload;
	
	/**
	 * Creates a new frame that has the given header and payload.
	 * 
	 * @param header the header of the frame
	 * @param payload the payload of the frame
	 */
	public Frame(DataHeader header, ByteBuffer payload) {
		Assert.notNull("header", header);
		Assert.notNull("payload", payload);
		this.header = header;
		this.payload = payload;
	}
	
	public DataHeader getHeader() {
		return header;
	}
	
	public MessageType getType() {
		return header.getType();
	}
	
	public int getChannelNumber() {
		return header.getChannel();
	}
	
	public boolean isIntermediate() {
		return header.isIntermediate();
	}
	
	public int getMessageNumber() {
		return header.getMessageNumber();
	}
	
	public long getSequenceNumber() {
		return header.getSequenceNumber();
	}
	
	public int getSize() {
		return header.getPayloadSize();
	}
	
	public ByteBuffer getPayload() {
		return payload;
	}
	
	/**
	 * Splits this frame into one frame of the given <var>size</var>
	 * and another frame with the rest of data.
	 * 
	 * @param size the size of the first frame's payload
	 * @return an array of two frames replacing this frame
	 */
	public Frame[] split(int size) {
		Frame[] result = new Frame[2];
		
		DataHeader[] headers = header.split(size);
		ByteBuffer[] buffers = splitPayload(getPayload(), size);
		
		result[0] = new Frame(headers[0], buffers[0]);		
		result[1] = new Frame(headers[1], buffers[1]);
		
		return result;
	}
		
	private ByteBuffer[] splitPayload(ByteBuffer payload, int size) {
		ByteBuffer[] result = new ByteBuffer[2];
		ByteBuffer tmp = payload.asReadOnlyBuffer();
		
		tmp.position(0).limit(size);
		result[0] = tmp.slice();
		
		tmp.position(size);
		tmp.limit(payload.capacity());
		result[1] = tmp.slice();
		
		return result;
	}

	/**
	 * Sends this frame to the given transport.
	 * 
	 * @param transport the transport where the bytes of this frame are
	 *        sent to
	 */
	public final void send(Transport transport) {
		ByteBuffer headerBuffer = header.asByteBuffer();
		
		ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.remaining() + getSize() + 5);
		buffer.put(headerBuffer);
		buffer.put(getPayload());
		buffer.put(Constants.TRAILER_BYTES);
		buffer.flip();
		
		transport.sendBytes(buffer);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (obj.getClass().equals(getClass())) {
			Frame frame = (Frame) obj;
			return header.equals(frame.header)
			    && payload.equals(frame.payload);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = result * 13 + header.hashCode();
		result = result * 13 + payload.hashCode();
		return result;
	}
	
	@Override
	public String toString() {
		return "Frame[header=" + getHeader() + "]";
	}

}
