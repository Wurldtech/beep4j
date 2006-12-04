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

public class IntegerSequence implements Sequence<Integer> {
	
	private final int increment;
	
	private int value = 0;
	
	public IntegerSequence() {
		this(1);
	}
	
	public IntegerSequence(int increment) {
		this(0, increment);
	}
	
	public IntegerSequence(int start, int increment) {
		this.value = start;
		this.increment = increment;
	}
	
	public synchronized Integer next() {
		int result = value;
		value += increment;
		value  = value < 0 ? 0 : value;
		return result;
	}

}
