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

/**
 * The StartSessionRequest is used when establishing a session to
 * register profiles or to cancel the session.
 * 
 * @author Simon Raess 
 */
public interface StartSessionRequest {
	
	/**
	 * Registers a profile URI, which will be advertised as part
	 * of the greeting element. This method may be called zero or
	 * more times, depending on how many profiles the peer wants to
	 * advertise.
	 * 
	 * @param profileUri the profile URI to be advertised
	 */
	void registerProfile(String profileUri);
	
	/**
	 * Cancels the Session by sending an error element in a negative reply.
	 * The error code is 421. The meaning of this code is 'service not available'.
	 */
	void cancel();
	
}
