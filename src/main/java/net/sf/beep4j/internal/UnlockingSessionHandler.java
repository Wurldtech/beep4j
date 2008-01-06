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

import java.util.concurrent.locks.Lock;

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.StartChannelRequest;
import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.internal.util.Assert;

class UnlockingSessionHandler implements SessionHandler {
	
	private final SessionHandler target;
	
	private final Lock lock;
	
	UnlockingSessionHandler(SessionHandler target, Lock lock) {
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
	
	public void connectionEstablished(StartSessionRequest s) {
		unlock();
		try {
			target.connectionEstablished(s);
		} finally {
			lock();
		}
	}
	
	public void sessionOpened(Session s) {
		unlock();
		try {
			target.sessionOpened(s);
		} finally {
			lock();
		}
	}
	
	public void sessionStartDeclined(int code, String message) {
		unlock();
		try {
			target.sessionStartDeclined(code, message);
		} finally {
			lock();
		}
	}
	
	public void channelStartFailed(String profileUri, ChannelHandler channelHandler, int code, String message) {
		unlock();
		try {
			target.channelStartFailed(profileUri, channelHandler, code, message);
		} finally {
			lock();
		}
	}
	
	public void channelStartRequested(StartChannelRequest request) {
		unlock();
		try {
			target.channelStartRequested(request);
		} finally {
			lock();
		}
	}
	
	public void sessionClosed() {
		unlock();
		try {
			target.sessionClosed();
		} finally {
			lock();
		}
	}
	
}