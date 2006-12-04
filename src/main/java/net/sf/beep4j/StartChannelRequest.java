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
 * Represents a request to start a new channel by the other peer.
 * This request may be cancelled or accepted by selecting a profile
 * from a list of profiles. If you take no action, the request
 * will be cancelled, the channel not created.
 * 
 * @author Simon Raess
 */
public interface StartChannelRequest {
	
	/**
	 * Gets the list of acceptable profiles sent as part of the start
	 * channel request.
	 * 
	 * @return the array of acceptable profiles
	 */
	ProfileInfo[] getProfiles();
	
	/**
	 * Determines whether there is a profile element with the
	 * given profile uri.
	 * 
	 * @param profileUri the profile uri
	 * @return true iff a profile info object with the given uri is available
	 */
	boolean hasProfile(String profileUri);
	
	/**
	 * Gets the ProfileInfo for the given profile uri. If there is no
	 * ProfileInfo object matching the uri, an IllegalArgumentException
	 * is thrown. This method does not return null.
	 * 
	 * @param profileUri the profile uri
	 * @return the ProfileInfo object matching the given profile uri
	 * @throws IllegalArgumentException if the profile info element is missing
	 */
	ProfileInfo getProfile(String profileUri);
	
	/**
	 * Selects one particular profile from the list of profiles.
	 * The acceptable list of profiles can be retrieved from the
	 * {@link #getProfiles()} method. If you pass in a profile
	 * which is not in the list of profiles, this method will
	 * throw an exception.
	 * 
	 * @param profile the selected profile for the channel
	 * @param handler the channel handler for the new channel
	 */
	void selectProfile(ProfileInfo profile, ChannelHandler handler);
	
	/**
	 * Cancels the request to start a new channel. An error element in a negative
	 * reply is sent to the other peer and the channel won't
	 * be created.
	 * 
	 * @param code the reply code
	 * @param message the diagnostic message
	 */
	void cancel(int code, String message);
	
}
