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

import java.util.concurrent.locks.ReentrantLock;

import net.sf.beep4j.Message;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.internal.util.Assert;

class ReplyHandlerHolder {
	private final int messageNumber;
	private final ReplyHandler replyHandler;
	private final ReentrantLock lock;

	ReplyHandlerHolder(int messageNumber, ReplyHandler replyHandler) {
		this(messageNumber, replyHandler, null);
	}

	ReplyHandlerHolder(int messageNumber, ReplyHandler replyHandler, ReentrantLock lock) {
		Assert.notNull("replyHandler", replyHandler);
		this.messageNumber = messageNumber;
		this.replyHandler = replyHandler;
		this.lock = lock;
	}
	
	private void lock() {
		if (lock != null) {
			lock.lock();
		}
	}
	
	private void unlock() {
		if (lock != null) {
			lock.unlock();
		}
	}
	
	protected void receivedANS(int channelNumber, int messageNumber, Message message) {
		validateMessageNumber(channelNumber, messageNumber);
		unlock();
		try {
			replyHandler.receivedANS(message);
		} finally {
			lock();
		}
	}
	
	protected void receivedNUL(int channelNumber, int messageNumber) {
		validateMessageNumber(channelNumber, messageNumber);
		unlock();
		try {
			replyHandler.receivedNUL();
		} finally {
			lock();
		}
	}
	
	protected void receivedERR(int channelNumber, int messageNumber, Message message) {
		validateMessageNumber(channelNumber, messageNumber);
		unlock();
		try {
			replyHandler.receivedERR(message);
		} finally {
			lock();
		}
	}
	
	protected void receivedRPY(int channelNumber, int messageNumber, Message message) {
		validateMessageNumber(channelNumber, messageNumber);
		unlock();
		try {
			replyHandler.receivedRPY(message);
		} finally {
			lock();
		}
	}
	
	private void validateMessageNumber(int channelNumber, int messageNumber) {
		if (this.messageNumber != messageNumber) {
			throw new ProtocolException("next expected reply on channel "
					+ channelNumber + " must have message number "
					+ this.messageNumber + " but was "
					+ messageNumber);
		}
	}
	
}
