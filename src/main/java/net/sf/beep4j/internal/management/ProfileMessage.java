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
package net.sf.beep4j.internal.management;

import net.sf.beep4j.ProfileInfo;

final class ProfileMessage {
	
	private final ProfileInfo profile;
	
	public ProfileMessage(ProfileInfo profile) {
		this.profile = profile;
	}
	
	public ProfileInfo getProfile() {
		return profile;
	}
	
	@Override
	public String toString() {
		return "<profile uri='" + profile.getUri() + "'>...</profile>";
	}
	
}
