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
package net.sf.beep4j.internal.stream;

import java.util.HashMap;
import java.util.Map;

import net.sf.beep4j.internal.SessionListener;
import net.sf.beep4j.internal.util.Assert;

/**
 * FrameHandler implementation that delegates to a unique {@link FrameHandler}
 * per channel. It uses
 * 
 * @author Simon Raess
 */
public class DelegatingFrameHandler implements FrameHandler, SessionListener {
	
	private final FrameHandlerFactory factory;
	
	private final Map<Integer, FrameHandler> handlers = new HashMap<Integer, FrameHandler>();
	
	public DelegatingFrameHandler(FrameHandlerFactory factory) {
		Assert.notNull("factory", factory);
		this.factory = factory;
		this.channelStarted(0);
	}
	
	public void handleFrame(Frame frame) {
		FrameHandler handler = handlers.get(frame.getChannelNumber());
		if (handler == null) {
			throw new IllegalStateException("there must be a FrameHandler for channel "
					+ frame.getChannelNumber() + "; channelStarted was not called");
		}
		handler.handleFrame(frame);
	}
	
	public void channelStarted(int channelNumber) {
		FrameHandler handler = factory.createFrameHandler();
		handlers.put(channelNumber, handler);
	}
	
	public void channelClosed(int channelNumber) {
		handlers.remove(channelNumber);
	}
	
}
