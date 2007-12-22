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
 * A ResponseHandler is passed to the application whenever a message
 * of type MSG is received.
 * (see {@link ChannelHandler#receiveRequest(Channel, Message, ResponseHandler)})
 * It allows the application to reply to the received message.
 * According to section 2.1.1 of the BEEP specification (RFC 3080) the allowed
 * exchange styles are:
 * 
 * <ul>
 *   <li>MSG/RPY: positive reply, one-to-one exchange</li>
 *   <li>MSG/ERR: negative reply, one-to-one exchange</li>
 *   <li>MSG/ANS: zero or more ANS messages terminated with a NUL message, many-to-one exchange</li>
 * </ul>
 * 
 * @author Simon Raess
 */
public interface Reply {
	
	/**
	 * Creates a new MessageBuilder object that can be used to create
	 * one new message.
	 * 
	 * @return a new MessageBuilder object
	 */
	MessageBuilder createMessageBuilder();
	
	/**
	 * Sends a reply of type ANS. This method can be called zero or more
	 * times. To complete the response the method {@link #sendNUL()} has
	 * to be invoked.
	 *  
	 * @param message the response
	 * @throws IllegalArgumentException if the message is null
	 * @throws IllegalStateException if a response has already been sent
	 */
	void sendANS(Message message);
	
	/**
	 * Sends a reply of type NUL. This method must be called to complete a
	 * one-to-many exchange.
	 * 
	 * @throws IllegalStateException if a response has already been sent
	 */
	void sendNUL();

	/**
	 * Sends a negative reply of type ERR. This method can only be called exactly once.
	 * 
	 * @param message the response
	 * @throws IllegalArgumentException if the message is null
	 * @throws IllegalStateException if a response has already been sent
	 */
	void sendERR(Message message);
	
	/**
	 * Sends a positive reply of type RPY. This method can only be called exactly once.
	 * 
	 * @param message the response
	 * @throws IllegalArgumentException if the message is null
	 * @throws IllegalStateException if a response has already been sent
	 */
	void sendRPY(Message message);

}
