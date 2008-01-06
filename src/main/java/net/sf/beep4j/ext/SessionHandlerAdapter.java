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

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.StartChannelRequest;
import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.internal.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for SessionHandler implementations. Some of the methods of
 * the SessionHandler interface are implemented by this abstract class.
 * Feel free to override the methods as necessary.
 * 
 * @author Simon Raess
 */
public abstract class SessionHandlerAdapter implements SessionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(SessionHandlerAdapter.class);
	
	/**
	 * The associated Session object.
	 */
	private Session session;
	
	/**
	 * Get the associated Session object. This method will return null
	 * as long as {@link #sessionOpened(Session)} has not been invoked
	 * and will return after {@link #sessionClosed()} has been invoked.
	 * 
	 * @return the associated Session object
	 */
	protected Session getSession() {
		return session;
	}
	
	/**
	 * Does not register a profile.
	 */
	public void connectionEstablished(StartSessionRequest s) {
		// do nothing
	}
	
	/**
	 * Notifies the handler that the session start was refused by the
	 * remote peer. This method does nothing. If you need to know
	 * about this type of event, override the method.
	 */
	public void sessionStartDeclined(int code, String message) {
		LOG.debug("sessionStartDeclined(" + code + ": " + message);
	}

	/**
	 * Notifies the handler that the Session was successfully established.
	 * This class keeps a reference to the Session object, which can be
	 * retrieved throught the {@link #getSession()} method.
	 * 
	 * @param session the Session object
	 */
	public void sessionOpened(Session session) {
		Assert.notNull("session", session);
		LOG.debug("sessionOpened");
		this.session = session;
	}
	
	/**
	 * Notifies the handler that the channel start request has been denied by the
	 * remote peer.
	 */
	public void channelStartFailed(String profileUri, ChannelHandler channelHandler, int code, String message) {
		// ignored
	}

	/**
	 * Notifies the handler that the remote peer wants to open a channel.
	 * The passed in request can be used to find out which profiles the
	 * other peer prefers. This method simply cancels the request. If 
	 * you want to support profiles, you should override this method
	 * and register some profiles. Do not call the super class method
	 * in that case.
	 * 
	 * @param request all the information about the request to start a channel
	 */
	public void channelStartRequested(StartChannelRequest request) {
		LOG.debug("start channel requested: cancelled");
	}
	
	/**
	 * Notifies the handler that the Session has been closed. This method
	 * simply sets the reference to the Session object to null.
	 */
	public void sessionClosed() {
		LOG.debug("session closed");
		this.session = null;
	}

}
