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
 * Represents a BEEP session with another peer. It allows to close the
 * session or start one or several channels. The Session interface
 * represents the outgoing view of a BEEP peer. The corresponding
 * incoming interface is the {@link SessionHandler}.
 * 
 * @author Simon Raess
 */
public interface Session {
	
	/**
	 * Gets the list of profiles supported by the remote peer. It may be
	 * that the peer supports additional profiles, which it did not advertise
	 * and which are thus not returned from this method.
	 * 
	 * @return an array of advertised profiles by the other peer
	 */
	String[] getProfiles();
		
	/**
	 * Tries to start a new channel using the profile identified by the given
	 * uri. The returned Future can be used to wait until the channel has
	 * been established. It returns only after the passed in
	 * {@link ChannelHandler#channelOpened(Channel)} method returns.
	 * 
	 * @param profileUri the uri of the profile to be used on the channel
	 * @param handler the channel handler for the new channel
	 * @return a Future that can be used to await the successful establishment
	 *         of the channel.
	 */
	void startChannel(String profileUri, ChannelHandler handler);
	
	/**
	 * Tries to start a new channel using the profile passed in. Use this
	 * method if you have to pass initialization data along the start
	 * channel request. See {@link #startChannel(String, ChannelHandler)}
	 * for the details.
	 * 
	 * @param profile the profile
	 * @param handler the channel handler for the new channel
	 * @return a Future that can be used to await the successfuly establishment
	 *         of the channel
	 */
	void startChannel(ProfileInfo profile, ChannelHandler handler);

	/**
	 * Tries to start a new channel using one of the profiles passed in. Use this
	 * method if you have to pass initialization data along the start
	 * channel request. See {@link #startChannel(String, ChannelHandler)}
	 * for the details.
	 * 
	 * @param profiles the profiles from which the other peer can choose
	 * @param factory the factory that creates new ChannelHandlers for the new channel
	 * @return a Future that can be used to await the successfuly establishment
	 *         of the channel
	 */
	void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory);
	
	/**
	 * Closes the session. Note that this method blocks until all outstanding
	 * requests have been sent and all requests received up to the moment
	 * this method is called have been properly replied to. It ignores negative
	 * replies to a close request from the other peer.
	 */
	void close();
	
}
