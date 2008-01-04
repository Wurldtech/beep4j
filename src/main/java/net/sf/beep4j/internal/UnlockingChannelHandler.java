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
package net.sf.beep4j.internal;

import java.util.concurrent.locks.Lock;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;
import net.sf.beep4j.internal.util.Assert;

/**
 * {@link ChannelHandler} implementation that unlocks the given lock
 * before calling a target ChannelHandler. Further it guarantees that
 * the lock is again locked as soon as the target ChannelHandler
 * returns.
 * 
 * @author Simon Raess
 */
final class UnlockingChannelHandler implements ChannelHandler {
	
	private final ChannelHandler target;
	
	private final Lock lock;
	
	UnlockingChannelHandler(ChannelHandler target, Lock lock) {
		Assert.notNull("target", target);
		Assert.notNull("lock", lock);
		this.target = target;
		this.lock = lock;
	}
	
	private void lock() {
		lock.lock();
	}
	
	private void unlock() {
		lock.unlock();
	}
	
	public void channelOpened(Channel c) {
		unlock();
		try {
			target.channelOpened(c);
		} finally {
			lock();
		}
	}
	
	public void channelStartFailed(int code, String message) {
		unlock();
		try {
			target.channelStartFailed(code, message);
		} finally {
			lock();
		}
	}
	
	public void messageReceived(Message message, Reply reply) {
		unlock();
		try {
			target.messageReceived(message, reply);
		} finally {
			lock();
		}
	}
	
	public void channelCloseRequested(CloseChannelRequest request) {
		unlock();
		try {
			target.channelCloseRequested(request);
		} finally {
			lock();
		}
	}
	
	public void channelClosed() {
		unlock();
		try {
			target.channelClosed();
		} finally {
			lock();
		}
	}
	
}