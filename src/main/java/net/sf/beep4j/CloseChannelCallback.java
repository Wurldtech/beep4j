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
package net.sf.beep4j;

/**
 * Callback interface for channel close operation. When a BEEP peer
 * wants to close a channel it sends a close channel request to the
 * other peer. The other peer can either accept the request or
 * decline it. A declined request has the effect that the channel
 * is still alive. There is no way to close a channel for sure.
 * 
 * @author Simon Raess
 */
public interface CloseChannelCallback {
	
	/**
	 * Invoked by the framework if the channel close request has
	 * been accepted. Beside this method, the framework
	 * also invokes the {@link ChannelHandler#channelClosed(Channel)}
	 * method.
	 */
	void closeAccepted();
	
	/**
	 * Invoked by the framework if the channel close request has
	 * been declined. The code and message convey the reason
	 * why the request has been declined.
	 * 
	 * @param code the code, which is meaningful to a program
	 * @param message the message, which is meaningful to humans
	 */
	void closeDeclined(int code, String message);
	
}
