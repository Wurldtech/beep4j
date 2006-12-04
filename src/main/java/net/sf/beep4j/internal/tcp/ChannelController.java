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
package net.sf.beep4j.internal.tcp;

import net.sf.beep4j.Message;

/**
 * Controller object that knows about the channel's send and receive
 * buffers. It's main purpose is sending SEQ frames when the controller
 * thinks that's necessary as well as to fragment / queue outgoing
 * messages to respect the window of the other peer.
 * 
 * @author Simon Raess
 */
public interface ChannelController {
	
	/**
	 * To be invoked whenever a SEQ frame is received to update the sender
	 * window. The window start will be placed at <var>ackno</var> and
	 * the size will be updated to <var>size</var>. Note that the position
	 * will remain unaffected. If ackno is larger than the window size
	 * the communication with the other peer should be stopped as the
	 * peers lost the synchronization.
	 * 
	 * @param ackno the acknowledged sequence number
	 * @param size the size of the window
	 */
	void updateSendWindow(long ackno, int size);
	
	/**
	 * Send an ANS message with the given messageNumber and answerNumber on
	 * the channel of this controller.
	 * 
	 * @param messageNumber the message number of the message
	 * @param answerNumber the answer number of the message
	 * @param message the Message to be sent
	 */
	void sendANS(int messageNumber, int answerNumber, Message message);
	
	/**
	 * Sends an NUL message with the given messageNumber on the channel
	 * of this controller.
	 * 
	 * @param messageNumber the message number of the Message
	 */
	void sendNUL(int messageNumber);
	
	/**
	 * Sends an ERR message with the given messageNumber on the channel
	 * of this controller.
	 * 
	 * @param messageNumber the message number of the Message
	 * @param message the Message to be sent
	 */
	void sendERR(int messageNumber, Message message);

	/**
	 * Sends an MSG message with the given messageNumber on the channel
	 * of this controller.
	 * 
	 * @param messageNumber the message number of the Message
	 * @param message the Message to be sent
	 */
	void sendMSG(int messageNumber, Message message);

	/**
	 * Sends an RPY message with the given messageNumber on the channel
	 * of this controller.
	 * 
	 * @param messageNumber the message number of the Message
	 * @param message the Message to be sent
	 */
	void sendRPY(int messageNumber, Message message);

	/**
	 * <p>Validation of the sequence number according to the BEEP specification section
	 * 2.2.1.1.</p>
	 * 
	 * <p>A frame is poorly formed if the value of the sequence number doesn't 
	 * correspond to the expected value for the associated channel.</p>
	 * 
	 * <p>Further, this method checks that the payload size is not greater than
	 * the remaining buffer space.</p>
	 */
	void checkFrame(long seqno, int payloadSize);
	
	/**
	 * Notifies the controller that the frame with sequence number <var>seqno</var>
	 * and size <var>size</var> has been completely received.
	 * 
	 * @param seqno the sequence number of the frame
	 * @param size the size of the frame
	 */
	void frameReceived(long seqno, int size);

}
