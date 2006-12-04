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
package net.sf.beep4j.internal.profile;

import net.sf.beep4j.ProfileInfo;

final class StartChannelMessage implements ChannelManagementRequest {
	
	private final int channelNumber;
	
	private final ProfileInfo[] profiles;
	
	public StartChannelMessage(int channelNumber, ProfileInfo[] profiles) {
		this.channelNumber = channelNumber;
		this.profiles = profiles.clone();
	}
	
	public int getChannelNumber() {
		return channelNumber;
	}
	
	public ProfileInfo[] getProfiles() {
		return profiles.clone();
	}
	
}
