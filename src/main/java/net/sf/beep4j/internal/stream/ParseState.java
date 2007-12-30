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

/**
 * A state in the process of parsing an incoming stream of ByteBuffers.
 * 
 * @author Simon Raess
 */
interface ParseState {
	
	/**
	 * Processes the passed in ByteBuffer. The context object is used
	 * to report important events back to the context. If the buffer
	 * is not processed fully, the method returns true. Otherwise,
	 * the method returns false.
	 * 
	 * @param buffer the ByteBuffer to process
	 * @param context the context of the state
	 * @return true iff there are more bytes to be processed in the buffer
	 */
	boolean process(ByteBuffer buffer, ParseStateContext context);
	
}