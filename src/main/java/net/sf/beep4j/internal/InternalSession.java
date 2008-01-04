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

import net.sf.beep4j.Message;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.internal.management.CloseCallback;

public interface InternalSession extends Session {
	
	/**
	 * Sends a message on the given channel with the given message number and message. The reply
	 * handler is to be notified when the corresponding reply is received.
	 * 
	 * @param channelNumber the channel on which to send the message
	 * @param messageNumber the message number of that message
	 * @param message the message to be sent
	 * @param replyHandler the handler for the reply
	 */
	void sendMSG(int channelNumber, int messageNumber, Message message, ReplyHandler replyHandler);
	
	void sendRPY(int channelNumber, int messageNumber, Message message);
	
	void sendERR(int channelNumber, int messageNumber, Message message);
	
	void sendANS(int channelNumber, int messageNumber, int answertNumber, Message message);
	
	void sendNUL(int channelNumber, int messageNumber);
	
	/**
	 * Requests to close the specified channel.
	 * 
	 * @param channelNumber the channel number
	 * @param callback the callback to be notified about the outcome
	 */
	void requestChannelClose(int channelNumber, final CloseCallback callback);
	
}
