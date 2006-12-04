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
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * InputStream implementation that reads from a ByteBuffer.
 * 
 * @author Simon Raess
 */
public class ByteBufferInputStream extends InputStream {
	
	/**
	 * The buffer from which the data is read.
	 */
	private ByteBuffer buffer;
	
	/**
	 * Creates a new ByteBufferInputStream that reads from the given
	 * ByteBuffer. The stream creates a read only view of the buffer.
	 * Changes to the underlying buffer will be visible through
	 * this stream.
	 * 
	 * @param buffer the buffer from which data is read
	 */
	public ByteBufferInputStream(ByteBuffer buffer) {
		Assert.notNull("buffer", buffer);
		this.buffer = buffer.asReadOnlyBuffer();
	}
	
	/*
	 * Ensures that the buffer has not been closed.
	 */
	private void checkClosed() throws IOException {
		if (buffer == null) {
			throw new IOException("stream is closed");
		}
	}
	
	@Override
	public boolean markSupported() {
		return true;
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		if (buffer != null) {
			buffer.mark();			
		}
	}
	
	@Override
	public synchronized void reset() throws IOException {
		checkClosed();
		buffer.reset();
	}
	
	@Override
	public int read() throws IOException {
		checkClosed();
		return buffer.hasRemaining() ? buffer.get() : -1;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		checkClosed();
		int length = Math.min(available(), len);
		if (length == 0) {
			return -1;
		} else {
			buffer.get(b, off, length);
			return length;
		}
	}
	
	@Override
	public int available() throws IOException {
		checkClosed();
		return buffer.remaining();
	}

	@Override
	public void close() throws IOException {
		buffer = null;
	}
	
}
