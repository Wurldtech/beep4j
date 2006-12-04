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
 * Context used by ParseState implementations to send notifications
 * about important moments in the parse lifecycle.
 * 
 * @author Simon Raess
 */
public interface ParseStateContext {
	
	/**
	 * Callback method invoked when a header has been parsed.
	 * 
	 * @param tokens the header tokens
	 */
	void handleHeader(String[] tokens);
	
	/**
	 * Callback method invoked when the payload has been received.
	 * 
	 * @param payload the payload buffer
	 */
	void handlePayload(ByteBuffer buffer);
	
	/**
	 * Callback method invoked when the trailer has been parsed.
	 */
	void handleTrailer();
}
