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
package net.sf.beep4j.internal.profile;

/**
 * Object representation of a BEEP error element. A BEEP error has
 * a code and a diagnostic message.
 * 
 * @author Simon Raess
 */
public final class BEEPError {
	
	/**
	 * The three digit error code that is significant for machines.
	 */
	private final int code;
	
	/**
	 * The diagnostic message significant for humans.
	 */
	private final String message;
	
	/**
	 * Creates a new BEEPError object with the given code and
	 * diagnostic message. The message can be null.
	 * 
	 * @param code the status code
	 * @param message the diagnostic message
	 */
	public BEEPError(int code, String message) {
		this.code = code;
		this.message = message;
	}
	
	/**
	 * Gets the three digit status code. See section 8 of RFC 3080.
	 * 
	 * @return the status code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Gets the diagnostic message. 
	 * 
	 * @return the diagnostic message
	 */
	public String getMessage() {
		return message;
	}
	
	@Override
	public String toString() {
		return "<error code='" + code + "'>" + message + "</code>";
	}
	
}
