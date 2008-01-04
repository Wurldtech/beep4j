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

public class Pair<S,T> {
	
	public final S first;
	
	public final T second;
	
	public Pair(S first, T second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() == obj.getClass()) {
			Pair<?,?> p = (Pair<?,?>) obj;
			return equals(first, p.first)
			    && equals(second, p.second);
		} else {
			return false;
		}
	}
	
	private boolean equals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		} else if (o1 == null || o2 == null) {
			return false;
		} else {
			return o1.equals(o2);
		}
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = result * 13 + hashCode(first);
		result = result * 13 + hashCode(second);
		return result;
	}
	
	private int hashCode(Object o) {
		return o == null ? 0 : o.hashCode();
	}
	
}
