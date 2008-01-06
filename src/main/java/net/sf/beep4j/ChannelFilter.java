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
	
	void filterClose(NextFilter next, CloseChannelCallback callback);
	
	// --> start of ChannelHandler filtering methods <--
	
	void filterChannelOpened(NextFilter next, Channel channel);
	
	void filterMessageReceived(NextFilter next, Message message, Reply reply);
	
	void filterChannelCloseRequested(NextFilter next, CloseChannelRequest request);
	
	void filterChannelClosed(NextFilter next);
	
	// --> start of ReplyHandler filtering methods <--
	
	void filterReceivedRPY(NextFilter next, Message message);
	
	void filterReceivedERR(NextFilter next, Message message);
	
	void filterReceivedANS(NextFilter next, Message message);
	
	void filterReceivedNUL(NextFilter next);

	// --> start of Reply filtering methods <--
	
	void filterSendRPY(NextFilter next, Message message);
	
	void filterSendERR(NextFilter next, Message message);
	
	void filterSendANS(NextFilter next, Message message);
	
	void filterSendNUL(NextFilter next);
	
	/**
	 * Interface of the next filter in the chain.
	 */
	interface NextFilter { 
		
		void filterSendMessage(Message message, ReplyHandler replyHandler);
		
		void filterClose(CloseChannelCallback callback);
		
		void filterChannelOpened(Channel channel);
		
		void filterMessageReceived(Message message, Reply reply);
		
		void filterChannelCloseRequested(CloseChannelRequest request);
		
		void filterChannelClosed();
		
		void filterReceivedRPY(Message message);
		
		void filterReceivedERR(Message message);
		
		void filterReceivedANS(Message message);
		
		void filterReceivedNUL();
		
		void filterSendRPY(Message message);
		
		void filterSendERR(Message message);
		
		void filterSendANS(Message message);
		
		void filterSendNUL();
		
	}
	
}
