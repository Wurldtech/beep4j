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
 * Represents the incoming view of a channel. The interface contains 
 * callback methods which are invoked when the channel is established
 * ({@link #channelOpened(Channel)}), when a request from the
 * remote peer is received 
 * ({@link #receiveRequest(Channel, Message, Reply)}), or 
 * when the channel has been closed
 * ({@link #channelClosed(Channel)}).
 * 
 * @author Simon Raess
 */
public interface ChannelHandler {
	
	/**
	 * Invoked by the framework when the channel has been successfully
	 * started.
	 * 
	 * @param c the channel that was opened
	 */
	void channelOpened(Channel c);
	
	/**
	 * Invoked by the framework when the channel could not be started.
	 * 
	 * @param code the error code
	 * @param message the human readable error message
	 */
	void channelStartFailed(int code, String message);
	
	/**
	 * Invoked by the framework when the other peer sent a message
	 * to this peer on this channel. 
	 * 
	 * @param c the channel on which the message was received
	 * @param message the received message
	 * @param handler the handler used to return a response.
	 */
	void messageReceived(Message message, Reply handler);
	
	/**
	 * Invoked by the framework when the other peer requested to
	 * close the channel. 
	 * 
	 * @param request the request
	 */
	void channelCloseRequested(CloseChannelRequest request);
	
	/**
	 * Invoked by the framework when the other peer decided to close
	 * this channel.
	 * 
	 * @param c the Channel that was closed
	 */
	void channelClosed();
	
}
