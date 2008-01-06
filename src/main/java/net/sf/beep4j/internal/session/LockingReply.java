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

import java.util.concurrent.locks.ReentrantLock;

import net.sf.beep4j.Message;
import net.sf.beep4j.Reply;
import net.sf.beep4j.internal.util.Assert;

/**
 * Decorator reply adding locking around calls to the target
 * Reply.
 * 
 * @author Simon Raess
 */
final class LockingReply implements Reply {
	
	private final Reply target;
	
	private final ReentrantLock lock;
	
	LockingReply(Reply target, ReentrantLock lock) {
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
	
	public void sendANS(Message message) {
		lock();
		try {
			target.sendANS(message);
		} finally {
			unlock();
		}
	}
	
	public void sendNUL() {
		lock();
		try {
			target.sendNUL();
		} finally {
			unlock();
		}
	}
	
	public void sendERR(Message message) {
		lock();
		try {
			target.sendERR(message);
		} finally {
			unlock();
		}
	}
	
	public void sendRPY(Message message) {
		lock();
		try {
			target.sendRPY(message);
		} finally {
			unlock();
		}
	}
	
}
