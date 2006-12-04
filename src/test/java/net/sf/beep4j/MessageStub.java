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
package net.sf.beep4j;

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class MessageStub implements Message {

	public Iterator<String> getHeaderNames() {
		return null;
	}
	
	public String getContentType() {
		return null;
	}
	
	public String getHeader(String name) {
		return null;
	}
	
	public InputStream getInputStream() {
		return null;
	}
	
	public Reader getReader() {
		return null;
	}
	
	public Reader getReader(String charset) {
		return null;
	}
	
	public ByteBuffer getContentBuffer() {
		return null;
	}
	
	public ByteBuffer asByteBuffer() {
		return ByteBuffer.allocate(0);
	}

}
