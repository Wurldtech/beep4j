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

import net.sf.beep4j.Message;


/**
 * Interface to be implemented by processors of reassembled messages.
 * 
 * @author Simon Raess
 */
public interface MessageHandler {
	
	/**
	 * Receive a MSG message.
	 * 
	 * @param channelNumber the channel number
	 * @param messageNumber the message number
	 * @param message the Message itself
	 */
	void receiveMSG(int channelNumber, int messageNumber, Message message);
	
	/**
	 * Receive a RPY message.
	 * 
	 * @param channelNumber the channel number
	 * @param messageNumber the message number
	 * @param message the Message itself
	 */
	void receiveRPY(int channelNumber, int messageNumber, Message message);

	/**
	 * Receive a ERR message.
	 * 
	 * @param channelNumber the channel number
	 * @param messageNumber the message number
	 * @param message the Message itself
	 */
	void receiveERR(int channelNumber, int messageNumber, Message message);

	/**
	 * Receive a ANS message.
	 * 
	 * @param channelNumber the channel number
	 * @param messageNumber the message number
	 * @param answerNumber the answer number
	 * @param message the Message itself
	 */
	void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message);

	/**
	 * Receive a NUL message.
	 * 
	 * @param channelNumber the channel number
	 * @param messageNumber the message number
	 */
	void receiveNUL(int channelNumber, int messageNumber);
	
}
