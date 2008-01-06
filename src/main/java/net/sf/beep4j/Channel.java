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
 * Represents the outgoing view of a BEEP channel. It allows to send messages
 * to the remote peer and to close the channel.
 * 
 * @author Simon Raess
 */
public interface Channel {
	
	/**
	 * Gets the URI of the profile that is used by this channel.
	 * 
	 * @return the URI of the active profile
	 */
	String getProfile();
	
	/**
	 * Gets the session on which this channel runs.
	 * 
	 * @return the session
	 */
	Session getSession();
	
	/**
	 * Creates a new MessageBuilder object that can be used to create
	 * one Message.
	 * 
	 * @return a MessageBuilder implementation
	 */
	MessageBuilder createMessageBuilder();
	
	/**
	 * Sends a message on this channel to the remote peer. This
	 * method returns fairly quickly. That is, it does not wait for
	 * the answer to arrive. Instead, the reply is received through
	 * the reply listener.
	 * 
	 * @param message the message to be sent
	 * @param replyHandler the listener receiving the reply
	 */
	void sendMessage(Message message, ReplyHandler replyHandler);
	
	/**
	 * Closes the channel. The channel is 
	 * closed as soon as the conditions specified by section 2.3.1.3 of the
	 * BEEP specification are met. Note that the other peer can decline
	 * the close request. The outcome is communicated throught the 
	 * CloseChannelCallback supplied by the client.
	 * 
	 * @param callback the callback that gets notified about the outcome of
	 *                 the close request
	 */
	void close(CloseChannelCallback callback);
	
}
