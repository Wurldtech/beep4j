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
import net.sf.beep4j.Reply;
import net.sf.beep4j.internal.FilterChainTargetHolder;
import net.sf.beep4j.internal.util.Assert;

/**
 * @author Simon Raess
 */
class FilterReply implements Reply {
	
	private final InternalChannelFilterChain filterChain;
	
	private final Reply target;
	
	FilterReply(InternalChannelFilterChain filterChain, Reply target) {
		Assert.notNull("filterChain", filterChain);
		Assert.notNull("target", target);
		this.filterChain = filterChain;
		this.target = target;
	}

	public void sendANS(Message message) {
		FilterChainTargetHolder.setReply(target);
		try {
			filterChain.fireFilterSendANS(message);
		} finally {
			FilterChainTargetHolder.setReply(null);
		}
	}

	public void sendERR(Message message) {
		FilterChainTargetHolder.setReply(target);
		try {
			filterChain.fireFilterSendERR(message);
		} finally {
			FilterChainTargetHolder.setReply(null);
		}
	}

	public void sendNUL() {
		FilterChainTargetHolder.setReply(target);
		try {
			filterChain.fireFilterSendNUL();
		} finally {
			FilterChainTargetHolder.setReply(null);
		}
	}

	public void sendRPY(Message message) {
		FilterChainTargetHolder.setReply(target);
		try {
			filterChain.fireFilterSendRPY(message);
		} finally {
			FilterChainTargetHolder.setReply(null);
		}
	}

}
