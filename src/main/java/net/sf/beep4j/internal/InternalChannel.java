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
import net.sf.beep4j.Message;
import net.sf.beep4j.internal.management.CloseCallback;

/**
 * Interface implemented by channels that contains methods only visible
 * to the internal implementation.
 * 
 * @author Simon Raess
 */
public interface InternalChannel extends Channel {
	
	void channelOpened(ChannelHandler channelHandler);
	
	void receiveMSG(int messageNumber, Message message);
	
	void receiveRPY(int messageNumber, Message message);
	
	void receiveERR(int messageNumber, Message message);
	
	void receiveANS(int messageNumber, int answerNumber, Message message);
	
	void receiveNUL(int messageNumber);
	
	/**
	 * Tests whether this channel is in the Alive state.
	 * 
	 * @return true iff the channel is in the Alive state
	 */
	boolean isAlive();
	
	/**
	 * Tests whether this channel is shutting down. This can be either
	 * caused by the application or the remote peer.
	 * 
	 * @return true iff the channel is nor Alive nor Dead
	 */
	boolean isShuttingDown();
	
	/**
	 * Tests whether this channel is in the Dead state.
	 * 
	 * @return true iff the channel is in the Dead state
	 */
	boolean isDead();

	void channelCloseRequested(CloseCallback callback);
	
}
