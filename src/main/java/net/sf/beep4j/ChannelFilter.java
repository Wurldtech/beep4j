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
 * A ChannelFilter is meant to be used to implement cross-cutting concerns, such
 * as logging, message transformations, and security. Each channel can have
 * 0 or more such filters that are arranged in a chain.
 * 
 * @author Simon Raess
 */
public interface ChannelFilter {
	
	/**
	 * Filters {@link Channel#sendMessage(Message, ReplyListener)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the message that is to be sent
	 * @param listener the listener that processes the reply for this message
	 */
	void filterSendMessage(NextFilter next, Object message, ReplyListener listener);
	
	/**
	 * Filters {@link Channel#close(CloseChannelCallback)}.
	 * 
	 * @param next the next filter in the chain
	 */
	void filterClose(NextFilter next);
	
	/**
	 * Filters {@link ChannelHandler#channelOpened(Channel)}.
	 * 
	 * @param next the next filter in the chain
	 * @param channel the channel that was opened
	 */
	void filterChannelOpened(NextFilter next, Channel channel);
	
	/**
	 * Filters {@link ChannelHandler#messageReceived(Message, ResponseHandler)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the message that is received
	 * @param responseHandler to be used to create the response for the received message
	 */
	void filterMessageReceived(NextFilter next, Message message, ResponseHandler responseHandler);
	
	/**
	 * Filters {@link ChannelHandler#closeRequested(CloseChannelRequest)}.
	 * 
	 * @param next the next filter in the chain
	 * @param request the close channel request
	 */
	void filterChannelCloseRequested(NextFilter next, CloseChannelRequest request);
	
	/**
	 * Filters {@link ChannelHandler#channelClosed()}.
	 * 
	 * @param next the next filter in the chain
	 */
	void filterChannelClosed(NextFilter next);
	
	interface NextFilter { 
		
		void filterSendMessage(Message message, ReplyListener listener);
		
		void filterClose(CloseChannelCallback callback);
		
	}
	
}
