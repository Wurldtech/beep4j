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
package net.sf.beep4j.internal.session;

import java.util.concurrent.locks.ReentrantLock;

import net.sf.beep4j.Message;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.internal.util.Assert;

/**
 * ReplyHandler that unlocks a lock before calling a target handler. After
 * returning from the target handler, the lock is again acquired.
 * 
 * @author Simon Raess
 */
final class UnlockingReplyHandler implements ReplyHandler {
	
	private final ReplyHandler target;
	
	private final ReentrantLock lock;
	
	public UnlockingReplyHandler(ReplyHandler target, ReentrantLock lock) {
		Assert.notNull("target", target);
		this.target = target;
		this.lock = lock;
	}
	
	private void unlock() {
		if (lock != null) {
			lock.unlock();
		}
	}
	
	private void lock() {
		if (lock != null) {
			lock.lock();
		}
	}
	
	public void receivedANS(Message message) {
		unlock();
		try {
			target.receivedANS(message);
		} finally {
			lock();
		}
	}

	public void receivedERR(Message message) {
		unlock();
		try {
			target.receivedERR(message);
		} finally {
			lock();
		}
	}

	public void receivedNUL() {
		unlock();
		try {
			target.receivedNUL();
		} finally {
			lock();
		}
	}

	public void receivedRPY(Message message) {
		unlock();
		try {
			target.receivedRPY(message);
		} finally {
			lock();
		}
	}

}

