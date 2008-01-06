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
 * The SessionHandler represents the incoming view of a BEEP session. It has
 * several call back methods that correspond to certain events in the lifecycle
 * of the session. The {@link #connectionEstablished(StartSessionRequest)} method
 * is invoked when the connection to the other peer has been established.
 * Its purpose is to register zero or more profiles, which will be advertised
 * in the greeting message. The {@link #sessionOpened(Session)} method
 * is invoked when the greeting from the other peer has been received and the
 * connectionEstablished method has returned. From that point on until the
 * session is closed (as indicated by {@link #sessionClosed(Session)}) the
 * session is in an usable state. When the other peer wants to start a 
 * channel the method {@link #startChannel(Session, StartChannelRequest)}
 * is invoked.
 * 
 * <p>The order of calls to this method is guaranteed to be as follows:</p>
 * 
 * <ul>
 *  <li>{@link #connectionEstablished(StartSessionRequest)}</li>
 *  <li>{@link #sessionOpened(Session)}</li>
 *  <li>zero or more {@link #startChannel(Session, StartChannelRequest)}</li>
 *  <li>{@link #sessionClosed(Session)}</li>
 * </ul>
 * 
 * <p>It is possible that instead of the last three methods only the
 * {@link #sessionStartDeclined(int, String)} method is called.</p>
 *
 * @author Simon Raess
 * 
 * @see ch.iserver.beepj.Session
 */
public interface SessionHandler {
	
	/**
	 * The connectionEstablished method is called by the framework when the
	 * connection was established to the other peer. The purpose of
	 * this method is to send the BEEP greeting message (see 2.3.1.1
	 * of RFC 3080).
	 * 
	 * <p>A server typically registers some profiles in this method.</p>
	 * <pre>
	 *   public void connectionEstablished(StartSessionRequest s) {
	 *     s.registerProfile("http://iana.org/beep/TLS");
	 *   }
	 * </pre>
	 * 
	 * <p>To send a negative reply, a SessionHandler can cancel the
	 * session startup.</p>
	 * <pre>
	 *   public void connectionEstablished(StartSessionRequest s) {
	 *     s.cancelSession();
	 *   }
	 * </pre>
	 * 
	 * @param s the session startup object
	 */
	void connectionEstablished(StartSessionRequest s);
	
	/**
	 * Invoked by the framework if the remote peer declines the session
	 * start by sending an error in a negative reply.
	 * 
	 * @param code the reason code
	 * @param message the human readable message
	 */
	void sessionStartDeclined(int code, String message);
	
	/**
	 * This method is invoked when the session has been established.
	 * 
	 * @param s the opened Session
	 */
	void sessionOpened(Session s);
	
	/**
	 * Notifies the SessionHandler that starting the specified channel has failed.
	 * 
	 * @param profileUri the URI of the requested profile
	 * @param channelHandler the channel handler passed to one of the Session startChannel methods
	 * @param code the error code from the remote peer
	 * @param message the error message from the remote peer
	 */
	void channelStartFailed(String profileUri, ChannelHandler channelHandler, int code, String message);
	
	/**
	 * This method is invoked when the other peer wants to start
	 * a new channel. The passed in ChannelStartup method should
	 * be used to select a suitable channel and to install a
	 * ChannelHandler.
	 * 
	 * <pre>
	 *   public void channelStartRequested(StartChannelRequest request) {
	 *     request.selectProfile(startup.getProfiles()[0]);
	 *     request.setChannelHandler(new MyChannelHandler());
	 *   }
	 * </pre>
	 * 
	 * Beside accepting the creation of a channel you can also decline
	 * it.
	 * 
	 * <pre>
	 *   public void channelStartRequested(Session s, StartChannelRequest request) {
	 *     request.cancel(550, "still working");
	 *   }
	 * </pre>
	 * 
	 * Both methods are mutually exclusive. Either you select a profile and
	 * a set a channel handler or you cancel the request.
	 * 
	 * @param request all the information about the request to start a channel
	 */
	void channelStartRequested(StartChannelRequest request);
	
	/**
	 * This method is invoked when the session is closed.
	 */
	void sessionClosed();
		
}
