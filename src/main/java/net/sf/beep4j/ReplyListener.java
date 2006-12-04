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
 * Callback interface used to notify the application about a
 * reply to a message. Whenever the application sends a message
 * ({@link Channel#sendMessage(Message, ReplyListener)}) it
 * specifies a ReplyListener.
 * 
 * @author Simon Raess
 */
public interface ReplyListener {
	
	/**
	 * Invoked when an ANS response is received. For a one-to-many
	 * exchange style this method can be invoked 0 or more times.
	 * 
	 * @param message the received message
	 */
	void receiveANS(Message message);
	
	/**
	 * Invoked when a NUL response is received. For a one-to-many
	 * exchange style this method marks the end of such an exchange.
	 * No further methods must be invoked on the listener after 
	 * this method has been invoked.
	 */
	void receiveNUL();
	
	/**
	 * Invoked when an ERR response is received. This type of response
	 * is also termed negativ reply.
	 * 
	 * @param message the received message
	 */
	void receiveERR(Message message);
	
	/**
	 * Invoked when a RPY response is received. This type of response
	 * is also termed positiv reply.
	 * 
	 * @param message the received message
	 */
	void receiveRPY(Message message);
	
}
