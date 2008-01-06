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

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;

/**
 * @author Simon Raess
 */
final class FilterChannelHandler implements ChannelHandler {
	
	private final InternalChannelFilterChain filterChain;
	
	private final ChannelHandler target;
	
	FilterChannelHandler(InternalChannelFilterChain filterChain, ChannelHandler target) {
		this.filterChain = filterChain;
		this.target = target;
	}
	
	public void channelOpened(Channel c) {
		FilterChainTargetHolder.setChannelHandler(target);
		try {
			filterChain.fireFilterChannelOpened(c);
		} finally {
			FilterChainTargetHolder.setChannelHandler(null);
		}
	}

	public void channelStartFailed(int code, String message) {
		target.channelStartFailed(code, message);
	}

	public void messageReceived(Message message, Reply reply) {
		FilterChainTargetHolder.setChannelHandler(target);
		try {
			filterChain.fireFilterMessageReceived(message, reply);
		} finally {
			FilterChainTargetHolder.setChannelHandler(null);
		}
	}
	
	public void channelCloseRequested(CloseChannelRequest request) {
		FilterChainTargetHolder.setChannelHandler(target);
		try {
			filterChain.fireFilterChannelCloseRequested(request);
		} finally {
			FilterChainTargetHolder.setChannelHandler(null);
		}
	}
	
	public void channelClosed() {
		FilterChainTargetHolder.setChannelHandler(target);
		try {
			filterChain.fireFilterChannelClosed();
		} finally {
			FilterChainTargetHolder.setChannelHandler(null);
		}
	}

}
