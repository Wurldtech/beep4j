/*
 *  Copyright 2007 Simon Raess
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
package net.sf.beep4j.ext;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelFilter;
import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;

/**
 * Base class for ChannelFilter implementations. All methods are implemented to simply
 * delegate to the next filter in the chain. This is useful if you only want to filter
 * certain events.
 * 
 * @author Simon Raess
 */
public class ChannelFilterAdapter implements ChannelFilter {

	public void filterChannelOpened(NextFilter next, Channel channel) {
		next.filterChannelOpened(channel);
	}

	public void filterSendMessage(NextFilter next, Message message, ReplyHandler replyHandler) {
		next.filterSendMessage(message, replyHandler);
	}
	
	public void filterMessageReceived(NextFilter next, Message message, Reply reply) {
		next.filterMessageReceived(message, reply);
	}
	
	public void filterChannelClosed(NextFilter next) {
		next.filterChannelClosed();
	}
	
	public void filterReceivedRPY(NextFilter next, ReplyHandler replyHandler, Message message) {
		next.filterReceivedRPY(replyHandler, message);
	}
	
	public void filterReceivedERR(NextFilter next, Message message) {
		next.filterReceivedERR(message);
	}
	
	public void filterReceivedANS(NextFilter next, Message message) {
		next.filterReceivedANS(message);
	}
	
	public void filterReceivedNUL(NextFilter next) {
		next.filterReceivedNUL();
	}
	
	public void filterSendRPY(NextFilter next, Message message) {
		next.filterSendRPY(message);
	}
	
	public void filterSendERR(NextFilter next, Message message) {
		next.filterSendERR(message);
	}
	
	public void filterSendANS(NextFilter next, Message message) {
		next.filterSendANS(message);
	}
	
	public void filterSendNUL(NextFilter next) {
		next.filterSendNUL();
	}

}
