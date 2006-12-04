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

import java.io.UnsupportedEncodingException;


public abstract class Constants {
	
	public static final String TRAILER = "END\r\n";
	
	public static final byte[] TRAILER_BYTES;
	
	static {
		try {
			TRAILER_BYTES = TRAILER.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new ExceptionInInitializerError(e);
		}		
	}
	
	public static final int TRAILER_LENGTH = TRAILER.length();

}
