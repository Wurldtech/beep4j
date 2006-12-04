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
package net.sf.beep4j.internal.util;

/**
 * A Sequence returns positive sequential numbers. After reaching the maximum 
 * value, the sequence number should cycle back to zero and start again.
 * 
 * @author Simon Raess
 */
public interface Sequence<T extends Number> {
	
	/**
	 * Returns the next value from this sequence.
	 * 
	 * @return the next number
	 */
	T next();
	
}
