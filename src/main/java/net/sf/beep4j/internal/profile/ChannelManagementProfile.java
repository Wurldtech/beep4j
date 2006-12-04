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

import java.net.SocketAddress;

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.Message;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ResponseHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.CloseCallback;
import net.sf.beep4j.internal.SessionManager;

/**
 * Interface of the channel management profile, which is used on channel 0
 * of every BEEP session. Channel 0 is a bit special because it exists
 * in every BEEP session as soon as the session is established.
 * 
 * @author Simon Raess
 */
public interface ChannelManagementProfile {
	
	/**
	 * Invoked by the framework to initialize the channel and to get
	 * the ChannelHandler for the profile.
	 * 
	 * @param manager the SessionManager to be used by the profile
	 * @return the ChannelHandler for the profile
	 */
	ChannelHandler createChannelHandler(SessionManager manager);
	
	/**
	 * Invoked by the session when the connection has been established.
	 * The profile must pass this event to the SessionHandler. Depending
	 * on the response of the application, either a greeting or an
	 * error is sent to the ResponseHandler.
	 * @param address address of remote peer
	 * @param handler the SessionHandler of the session
	 * @param response the ResponseHandler to be used to generate a response
	 * 
	 * @return true iff the connection is established. Returning false from
	 *              this method will drop the connection.
	 */
	boolean connectionEstablished(SocketAddress address, SessionHandler handler, 
			ResponseHandler response);

	/**
	 * Invoked by the session when a greeting has been received during
	 * the session startup. Parses the given message into a Greeting object.
	 * 
	 * @param message the message to be parsed
	 * @return the parsed Greeting object
	 */
	Greeting receivedGreeting(Message message);
	
	/**
	 * Invoked by the session when an error has been received during
	 * the session startup.
	 * Parses the given message into a BEEPError object.
	 * 
	 * @param message the message to be parsed
	 * @return the parsed BEEPError object
	 */
	BEEPError receivedError(Message message);
	
	/**
	 * Sends a start channel message to the other peer.
	 * 
	 * @param channelNumber the channel number of the new peer
	 * @param infos the ProfileInfos to be passed inside the profile element in
	 *        the request
	 * @param callback the callback that is invoked when the response is received
	 */
	void startChannel(int channelNumber, ProfileInfo[] infos, StartChannelCallback callback);
	
	/**
	 * Send a close channel message. The corresponding method is invoked on
	 * the callback to notify this peer about the outcome of the close
	 * request.
	 * 
	 * @param channelNumber the channel to be closed
	 * @param callback the callback that is invoked when the response is received
	 */
	void closeChannel(int channelNumber, CloseChannelCallback callback);
	
	/**
	 * Closes the session. The BEEP specification notes that it is possible
	 * for a BEEP peer to decline session closing. However, this does not
	 * seem to make a lot of sense. So, if this method is invoked, the session
	 * is always closed.
	 * 
	 * @param callback the callback used to notify about the response
	 */
	void closeSession(CloseCallback callback);

}
