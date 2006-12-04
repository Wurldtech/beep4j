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
 * Factory to create ChannelHandler objects. This factory is used by the
 * {@link Session#startChannel(ProfileInfo[], ChannelHandlerFactory)}
 * method to create a new ChannelHandler for the selected profile. Otherwise
 * the application would have to pass in n ChannelHandler objects for
 * n ProfileInfo objects passed into the method.
 * 
 * @author Simon Raess
 */
public interface ChannelHandlerFactory {
	
	/**
	 * Notifies the factory that the channel creation failed. 
	 * 
	 * @param code the error code
	 * @param message the error message
	 */
	void startChannelFailed(int code, String message);
	
	/**
	 * Requests the factory to create a ChannelHandler for the
	 * specified ProfileInfo.
	 * 
	 * @param info the ProfileInfo
	 * @return a new ChannelHandler for that profile
	 */
	ChannelHandler createChannelHandler(ProfileInfo info);
	
}
