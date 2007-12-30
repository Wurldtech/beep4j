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

import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.internal.profile.CloseCallback;

/**
 * The SessionManager is used to start / close channels and to close
 * a session.
 * 
 * @author Simon Raess
 */
public interface SessionManager {
	
	/**
	 * Requests to start a new channel or cancels the request.
	 * 
	 * @param channelNumber the channel number
	 * @param profiles the list of acceptable profiles
	 * @return the response from the application
	 */
	StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles);
	
	/**
	 * Requests to close the channel identified by the given channel number.
	 * The request can either be accepted (resulting in a close channel) or
	 * declined (in which case the channel stays open).
	 * 
	 * @param channelNumber the channel number
	 * @param request the close request
	 */
	void channelCloseRequested(int channelNumber, CloseChannelRequest request);
	
	/**
	 * Closes the session.
	 * 
	 * @param callback the callback that gets notified about the outcome
	 */
	void sessionCloseRequested(CloseCallback callback);
	
	/**
	 * Sends a message through the session on channel 0.
	 * 
	 * @param message the message to be sent
	 * @param reply the handler for the reply
	 */
	void sendMessage(Message message, ReplyHandler reply);
	
}
