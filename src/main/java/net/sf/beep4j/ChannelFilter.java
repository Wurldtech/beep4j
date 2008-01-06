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
	
	// --> start of Channel filtering methods <--
	
	/**
	 * Filters {@link Channel#sendMessage(Message, ReplyHandler)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the message that is to be sent
	 * @param replyHandler the listener that processes the reply for this message
	 */
	void filterSendMessage(NextFilter next, Message message, ReplyHandler replyHandler);
	
	/**
	 * Filters {@link Channel#close(CloseChannelCallback)}.
	 * 
	 * @param next the next filter in the chain
	 * @param callback the close callback
	 */
	void filterClose(NextFilter next, CloseChannelCallback callback);
	
	// --> start of ChannelHandler filtering methods <--
	
	/**
	 * Filters {@link ChannelHandler#channelOpened(Channel)}.
	 * 
	 * @param next the next filter in the chain
	 * @param channel the channel that is opened
	 */
	void filterChannelOpened(NextFilter next, Channel channel);
	
	/**
	 * Filters {@link ChannelHandler#messageReceived(Message, Reply)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the received message
	 * @param reply the reply object used to send back a reply
	 */
	void filterMessageReceived(NextFilter next, Message message, Reply reply);
	
	/**
	 * Filters {@link ChannelHandler#channelCloseRequested(CloseChannelRequest)}.
	 * 
	 * @param next the next filter in the chain
	 * @param request the request object to close a channel
	 */
	void filterChannelCloseRequested(NextFilter next, CloseChannelRequest request);
	
	/**
	 * Filters {@link ChannelHandler#channelClosed()}.
	 * 
	 * @param next the next filter in the chain
	 */
	void filterChannelClosed(NextFilter next);
	
	// --> start of ReplyHandler filtering methods <--
	
	/**
	 * Filters {@link ReplyHandler#receivedRPY(Message)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the received RPY message
	 */
	void filterReceivedRPY(NextFilter next, Message message);
	
	/**
	 * Filters {@link ReplyHandler#receivedERR(Message)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the received ERR message
	 */
	void filterReceivedERR(NextFilter next, Message message);
	
	/**
	 * Filters {@link ReplyHandler#receivedANS(Message)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the received ANS message
	 */
	void filterReceivedANS(NextFilter next, Message message);
	
	/**
	 * Filters {@link ReplyHandler#receivedNUL()}.
	 * 
	 * @param next the next filter in the chain
	 */
	void filterReceivedNUL(NextFilter next);

	// --> start of Reply filtering methods <--
	
	/**
	 * Filters {@link Reply#sendRPY(Message)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the RPY message
	 */
	void filterSendRPY(NextFilter next, Message message);
	
	/**
	 * Filters {@link Reply#sendERR(Message)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the ERR message
	 */
	void filterSendERR(NextFilter next, Message message);
	
	/**
	 * Filters {@link Reply#sendANS(Message)}.
	 * 
	 * @param next the next filter in the chain
	 * @param message the ANS message
	 */
	void filterSendANS(NextFilter next, Message message);
	
	/**
	 * Filters {@link Reply#sendNUL()}.
	 * 
	 * @param next the next filter in the chain
	 */
	void filterSendNUL(NextFilter next);
	
	/**
	 * Interface of the next filter in the chain.
	 */
	interface NextFilter { 
		
		/**
		 * Invokes the method 
		 * {@link ChannelFilter#filterSendMessage(net.sf.beep4j.ChannelFilter.NextFilter, Message, ReplyHandler)} 
		 * on the next {@link ChannelFilter} in the chain.
		 * 
		 * @param message the message to be sent
		 * @param replyHandler the {@link ReplyHandler} that handles replies to this message
		 */
		void filterSendMessage(Message message, ReplyHandler replyHandler);
		
		/**
		 * Invokes the method 
		 * {@link ChannelFilter#filterClose(net.sf.beep4j.ChannelFilter.NextFilter, CloseChannelCallback)}
		 * on the next {@link ChannelFilter} in the chain.
		 * 
		 * @param callback the close channel callback
		 */
		void filterClose(CloseChannelCallback callback);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterChannelOpened(net.sf.beep4j.ChannelFilter.NextFilter, Channel)}
		 * on the next filter in the chain.
		 * 
		 * @param channel the opened channel
		 */
		void filterChannelOpened(Channel channel);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterMessageReceived(net.sf.beep4j.ChannelFilter.NextFilter, Message, Reply)}
		 * on the next filter in the chain.
		 * 
		 * @param message the received message
		 * @param reply the reply object used to send the reply
		 */
		void filterMessageReceived(Message message, Reply reply);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterChannelCloseRequested(net.sf.beep4j.ChannelFilter.NextFilter, CloseChannelRequest)}
		 * on the next filter in the chain.
		 * 
		 * @param request the channel close request
		 */
		void filterChannelCloseRequested(CloseChannelRequest request);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterChannelClosed(net.sf.beep4j.ChannelFilter.NextFilter)}
		 * on the next filter in the chain.
		 */
		void filterChannelClosed();
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterReceivedRPY(net.sf.beep4j.ChannelFilter.NextFilter, Message)}
		 * on the next filter in the chain.
		 * 
		 * @param message the received message
		 */
		void filterReceivedRPY(Message message);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterReceivedERR(net.sf.beep4j.ChannelFilter.NextFilter, Message)}
		 * on the next filter in the chain.
		 * 
		 * @param message the received message
		 */
		void filterReceivedERR(Message message);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterReceivedANS(net.sf.beep4j.ChannelFilter.NextFilter, Message)}
		 * on the next filter in the chain.
		 * 
		 * @param message the received message
		 */
		void filterReceivedANS(Message message);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterReceivedNUL(net.sf.beep4j.ChannelFilter.NextFilter)}
		 * on the next filter in the chain.
		 */
		void filterReceivedNUL();
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterSendRPY(net.sf.beep4j.ChannelFilter.NextFilter, Message)}
		 * on the next filter in the chain.
		 * 
		 * @param message the RPY message to be sent
		 */
		void filterSendRPY(Message message);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterSendERR(net.sf.beep4j.ChannelFilter.NextFilter, Message)}
		 * on the next filter in the chain.
		 * 
		 * @param message the ERR message to be sent
		 */
		void filterSendERR(Message message);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterSendANS(net.sf.beep4j.ChannelFilter.NextFilter, Message)}
		 * on the next filter in the chain.
		 * 
		 * @param message the ANS message to be sent
		 */
		void filterSendANS(Message message);
		
		/**
		 * Invokes the method
		 * {@link ChannelFilter#filterSendNUL(net.sf.beep4j.ChannelFilter.NextFilter)}
		 * on the next filter in the chain.
		 */
		void filterSendNUL();
		
	}
	
}
