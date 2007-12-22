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
package net.sf.beep4j.ext;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.internal.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for ChannelHandler implementors. Implement the remaining
 * unimplemented methods and override the methods you deem necessary.
 * 
 * @author Simon Raess
 */
public abstract class ChannelHandlerAdapter implements ChannelHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(ChannelHandlerAdapter.class);
	
	/**
	 * The associated Channel object.
	 */
	private Channel channel;
	
	/**
	 * Gets the channel object set by the {@link #channelOpened(Channel)} method.
	 * 
	 * @return the Channel object
	 */
	protected Channel getChannel() {
		return channel;
	}

	/**
	 * This method ignores this event. If you want to react to it
	 * you must override this method.
	 * 
	 * @param code the reply code
	 * @param message the diagnostic message
	 */
	public void channelStartFailed(int code, String message) {
		LOG.debug("starting channel failed: " + code + ",'" + message + "'");
	}
	
	/**
	 * Notifies this handler that the channel has been successfully opened.
	 * This method keeps a reference to the Channel object, which can be
	 * retrieved through the {@link #getChannel()} method.
	 * 
	 * @param channel the Channel object
	 */
	public void channelOpened(Channel channel) {
		Assert.notNull("channel", channel);
		LOG.debug("channel opened");
		this.channel = channel;
	}
	
	/**
	 * Notifies this handler that the remote peer requested to close
	 * the channel associated with this handler. This method accepts
	 * the request. If you want to have the oppurtunity to cancel
	 * a close request, you must override this method.
	 * 
	 * @param request the request to be declined or accepted
	 */
	public void channelCloseRequested(CloseChannelRequest request) {
		LOG.debug("close of channel requested: accepted");
		request.accept();
	}
	
	/**
	 * Notifies this handler that the channel has been closed. This
	 * method sets the channel reference to null.
	 */
	public void channelClosed() {
		LOG.debug("channel closed");
		this.channel = null;
	}

}
