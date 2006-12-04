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
package net.sf.beep4j.internal.tcp;

import net.sf.beep4j.transport.Transport;

/**
 * Factory to create ChannelController objects.
 * 
 * @author Simon Raess
 */
public interface ChannelControllerFactory {
	
	/**
	 * Creates a new ChannelController for the channel identified by the
	 * <var>channelNumber</var>. The transport is the interface to the
	 * implementation of the Transport layer.
	 * 
	 * @param channelNumber the channel number
	 * @param transport the Transport implementation
	 * @return a fully initialized ChannelController
	 */
	ChannelController createChannelController(int channelNumber, Transport transport);
	
}
