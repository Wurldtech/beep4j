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
package net.sf.beep4j.internal.session;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

public class ParameterCaptureAction<T> implements Action {
	private final int index;
	private final Class<? extends T> type;
	private final Action target;
	private T parameter;
	public ParameterCaptureAction(int index, Class<? extends T> type, Action target) {
		this.index = index;
		this.type = type;
		this.target = target;
	}
	public T getParameter() {
		return parameter;
	}
	public void describeTo(Description description) {
		description.appendText("stub[capture parameter " + index + "]");
	}
	public Object invoke(Invocation invocation) throws Throwable {
		parameter = type.cast(invocation.getParameter(index));
		if (target != null) {
			return target.invoke(invocation);
		} else {
			return null;
		}
	}	
}