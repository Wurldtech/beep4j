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

import java.util.Arrays;

import net.sf.beep4j.internal.util.Assert;

/**
 * Object representation of a BEEP greeting element.
 * 
 * @author Simon Raess
 */
public final class Greeting {
	
	/**
	 * The tokenized localize attribute of the greeting element.
	 */
	private final String[] localize;
	
	/**
	 * The tokenized features attribute of the greeting element.
	 */
	private final String[] features;
	
	/**
	 * The URIs of all the child profile elements.
	 */
	private final String[] profiles;
	
	/**
	 * Creates a new Greeting object representation.
	 * 
	 * @param localize the localize tokens
	 * @param features the feature tokens
	 * @param profiles an array of profile URIs
	 */
	public Greeting(String[] localize, String[] features, String[] profiles) {
		Assert.notNull("localize", localize);
		Assert.notNull("features", features);
		Assert.notNull("profiles", profiles);
		this.localize = localize;
		this.features = features;
		this.profiles = profiles;
	}
	
	/**
	 * Gets an array of feature tokens.
	 * 
	 * @return feature token array
	 */
	public String[] getFeatures() {
		return features;
	}
	
	/**
	 * Gets an array of localize tokens.
	 * 
	 * @return localize token array
	 */
	public String[] getLocalize() {
		return localize;
	}
	
	/**
	 * Gets an array of profile URIs.
	 * 
	 * @return profile URI array
	 */
	public String[] getProfiles() {
		return profiles;
	}
	
	@Override
	public String toString() {
		return "profiles=" + Arrays.toString(profiles);
	}
		
}
