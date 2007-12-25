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

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.StartChannelRequest;
import net.sf.beep4j.internal.util.Assert;

final class DefaultStartChannelRequest implements StartChannelRequest {
	
	private final ProfileInfo[] profiles;
	
	private StartChannelResponse response;
	
	public DefaultStartChannelRequest(ProfileInfo[] profiles) {
		Assert.notNull("profiles", profiles);
		this.profiles = profiles.clone();
	}
	
	public StartChannelResponse getResponse() {
		if (response == null) {
			response = StartChannelResponse.createCancelledResponse(
					550, "none of the requested profiles are supported");
		}
		return response;
	}
	
	public ProfileInfo[] getProfiles() {
		return profiles.clone();
	}
	
	public boolean hasProfile(String profileUri) {
		for (int i = 0; i < profiles.length; i++) {
			if (profileUri.equals(profiles[i].getUri())) {
				return true;
			}
		}
		return false;
	}
	
	public ProfileInfo getProfile(String profileUri) {
		for (int i = 0; i < profiles.length; i++) {
			if (profileUri.equals(profiles[i].getUri())) {
				return profiles[i];
			}
		}
		throw new IllegalArgumentException("there is no ProfileInfo object matching "
				+ "the passed in profile uri: '" + profileUri + "'");
	}
	
	public void cancel(int code, String message) {
		if (response != null) {
			throw new IllegalStateException("StartChannelRequest is already either "
					+ "cancelled or accepted");
		}
		response = StartChannelResponse.createCancelledResponse(
					code, message);
	}

	public void selectProfile(ProfileInfo profile, ChannelHandler handler) {
		Assert.notNull("profile", profile);
		Assert.notNull("handler", handler);
		if (response != null) {
			throw new IllegalStateException("StartChannelRequest is already either "
					+ "cancelled or accepted");
		}
		checkProfile(profile.getUri());
		response = StartChannelResponse.createSuccessResponse(profile, handler);
	}
	
	private void checkProfile(String profile) {
		for (int i = 0; i < profiles.length; i++) {
			ProfileInfo initial = profiles[i];
			if (profile.equals(initial.getUri())) {
				return;
			}
		}
		throw new IllegalArgumentException("cannot select profile: " + profile
				+ " (not found in list of acceptable profiles)");
	}
	
}
