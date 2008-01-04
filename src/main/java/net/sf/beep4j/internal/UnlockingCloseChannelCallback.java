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

import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.internal.util.Assert;

final class UnlockingCloseChannelCallback implements CloseChannelCallback {
	
	private final CloseChannelCallback target;
	
	private final Lock lock;
	
	public UnlockingCloseChannelCallback(CloseChannelCallback target, Lock lock) {
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
	
	public void closeAccepted() {
		unlock();
		try {
			target.closeAccepted();
		} finally {
			lock();
		}
	}

	public void closeDeclined(int code, String message) {
		unlock();
		try {
			target.closeDeclined(code, message);
		} finally {
			lock();
		}
	}

}
