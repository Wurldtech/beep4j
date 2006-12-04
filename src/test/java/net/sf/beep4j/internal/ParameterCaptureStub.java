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

import org.jmock.core.Invocation;
import org.jmock.core.Stub;

public class ParameterCaptureStub<T> implements Stub {
	private final int index;
	private final Class<? extends T> type;
	private final Stub target;
	private T parameter;
	public ParameterCaptureStub(int index, Class<? extends T> type, Stub target) {
		this.index = index;
		this.type = type;
		this.target = target;
	}
	public T getParameter() {
		return parameter;
	}
	public StringBuffer describeTo(StringBuffer buf) {
		buf.append("stub[capture parameter " + index + "]");
		return buf;
	}
	public Object invoke(Invocation invocation) throws Throwable {
		parameter = type.cast(invocation.parameterValues.get(index));
		if (target != null) {
			return target.invoke(invocation);
		} else {
			return null;
		}
	}
}