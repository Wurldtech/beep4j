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
package net.sf.beep4j.internal.session;

import net.sf.beep4j.Message;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.internal.FilterChainTargetHolder;
import net.sf.beep4j.internal.util.Assert;

/**
 * ReplyHandler implementation that intercepts all method invocations and 
 * passes them through a filter chain. 
 *
 * @see InternalChannelFilterChain
 * @author Simon Raess
 */
final class FilterReplyHandler implements ReplyHandler {
	
	private final InternalChannelFilterChain filterChain;
	
	private final ReplyHandler target;
	
	FilterReplyHandler(InternalChannelFilterChain filterChain, ReplyHandler target) {
		Assert.notNull("filterChain", filterChain);
		Assert.notNull("target", target);
		this.filterChain = filterChain;
		this.target = target;
	}

	public void receivedRPY(Message message) {
		FilterChainTargetHolder.setReplyHandler(target);
		try {
			filterChain.fireFilterReceivedRPY(message);
		} finally {
			FilterChainTargetHolder.setReplyHandler(null);
		}
	}
	
	public void receivedERR(Message message) {
		FilterChainTargetHolder.setReplyHandler(target);
		try {
			filterChain.fireFilterReceivedERR(message);
		} finally {
			FilterChainTargetHolder.setReplyHandler(null);
		}
	}
	
	public void receivedANS(Message message) {
		FilterChainTargetHolder.setReplyHandler(target);
		try {
			filterChain.fireFilterReceivedANS(message);
		} finally {
			FilterChainTargetHolder.setReplyHandler(null);
		}
	}
	
	public void receivedNUL() {
		FilterChainTargetHolder.setReplyHandler(target);
		try {
			filterChain.fireFilterReceivedNUL();
		} finally {
			FilterChainTargetHolder.setReplyHandler(null);
		}
	}

}
