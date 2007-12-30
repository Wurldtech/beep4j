/*
 *  Copyright 2007 Simon Raess
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

import net.sf.beep4j.Message;
import net.sf.beep4j.internal.SessionListener;

/**
 * Represents the interface of the stream layer. The stream layer is responsible
 * to receive and send BEEP messages as well as to provide a way to close the
 * underlying transport. The messages in the stream layer are not yet correlated
 * or assigned to a particular channel.
 * 
 * @author Simon Raess
 */
public interface BeepStream extends SessionListener {
	
	/**
	 * Sends a message of type MSG.
	 * 
	 * @param channel the channel number
	 * @param messageNumber the message number
	 * @param message the message
	 */
	void sendMSG(int channel, int messageNumber, Message message);

	/**
	 * Sends a message of type RPY.
	 * 
	 * @param channel the channel number
	 * @param messageNumber the message number
	 * @param message the message
	 */
	void sendRPY(int channel, int messageNumber, Message message);
	
	/**
	 * Sends a message of type ERR.
	 * 
	 * @param channel the channel number
	 * @param messageNumber the message number
	 * @param message the message
	 */
	void sendERR(int channel, int messageNumber, Message message);
	
	/**
	 * Sends a message of type ANS.
	 * 
	 * @param channel the channel number
	 * @param messageNumber the message number
	 * @param message the message
	 */
	void sendANS(int channel, int messageNumber, int answerNumber, Message message);
	
	/**
	 * Sends a message of type NUL.
	 * 
	 * @param channel the channel number
	 * @param messageNumber the message number
	 * @param message the message
	 */
	void sendNUL(int channel, int messageNumber);
	
	/**
	 * Instructs the mapping to close the underlying Transport object.
	 */
	void closeTransport();
	
}
