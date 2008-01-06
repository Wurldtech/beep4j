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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.beep4j.ChannelFilterChainBuilder;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.ChannelHandlerFactory;
import net.sf.beep4j.Message;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.NullChannelFilterChainBuilder;
import net.sf.beep4j.internal.SessionListener;
import net.sf.beep4j.internal.SessionManager;
import net.sf.beep4j.internal.StartChannelResponse;
import net.sf.beep4j.internal.TransportHandler;
import net.sf.beep4j.internal.management.BEEPError;
import net.sf.beep4j.internal.management.CloseCallback;
import net.sf.beep4j.internal.management.Greeting;
import net.sf.beep4j.internal.management.ManagementProfile;
import net.sf.beep4j.internal.management.ManagementProfileImpl;
import net.sf.beep4j.internal.management.StartChannelCallback;
import net.sf.beep4j.internal.stream.BeepStream;
import net.sf.beep4j.internal.stream.MessageHandler;
import net.sf.beep4j.internal.util.Assert;
import net.sf.beep4j.internal.util.IntegerSequence;
import net.sf.beep4j.internal.util.Sequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the Session interface. Objects of this class are
 * the central object in a BEEP session.
 * 
 * <ul>
 *  <li>dispatch incoming messages</li>
 *  <li>send outgoing messages</li>
 *  <li>manage channel start and close</li>
 * </ul>
 * 
 * @author Simon Raess
 */
public class SessionImpl 
		implements MessageHandler, SessionManager, InternalSession, TransportHandler {
	
	private static final int MANAGEMENT_CHANNEL = 0;

	private final Logger LOG = LoggerFactory.getLogger(SessionImpl.class);
	
	private final boolean initiator;
	
	private final Map<Integer,InternalChannel> channels = new HashMap<Integer,InternalChannel>();
	
	private final ManagementProfile channelManagementProfile;
	
	private final BeepStream beepStream;
	
	private final SessionHandler sessionHandler;
	
	private final Sequence<Integer> channelNumberSequence;
	
	private final List<SessionListener> listeners = Collections.synchronizedList(new ArrayList<SessionListener>());
	
	private ChannelFilterChainBuilder filterChainBuilder = new NullChannelFilterChainBuilder();
	
	private final ReentrantLock sessionLock = new ReentrantLock();
	
	private SessionState currentState;
	
	private SessionState initialState;
	
	private SessionState aliveState;
	
	private SessionState closeInitiatedState;
	
	private SessionState deadState;

	/**
	 * The greeting received from the other peer.
	 */
	private Greeting greeting;
	
	public SessionImpl(boolean initiator, SessionHandler sessionHandler, BeepStream beepStream) {
		Assert.notNull("sessionHandler", sessionHandler);
		Assert.notNull("beepStream", beepStream);
		
		this.initiator = initiator;
		this.sessionHandler = new UnlockingSessionHandler(sessionHandler, sessionLock);
		this.beepStream = beepStream;
		
		addSessionListener(beepStream);
				
		this.channelManagementProfile = createChannelManagementProfile(initiator);
		initChannelManagementProfile();
		
		this.channelNumberSequence = new IntegerSequence(initiator ? 1 : 2, 2);
		
		initialState = createInitialState();
		aliveState = createAliveState();
		closeInitiatedState = createCloseInitiatedState();
		deadState = createDeadState();
		currentState = initialState;
	}
	
	public void setChannelFilterChainBuilder(ChannelFilterChainBuilder filterChainBuilder) {
		this.filterChainBuilder = filterChainBuilder == null ? new NullChannelFilterChainBuilder() : filterChainBuilder;
	}

	protected DeadState createDeadState() {
		return new DeadState();
	}

	protected SessionState createCloseInitiatedState() {
		return new CloseInitiatedState();
	}

	protected SessionState createAliveState() {
		return new AliveState();
	}

	protected SessionState createInitialState() {
		return new InitialState();
	}
	
	protected ManagementProfile createChannelManagementProfile(boolean initiator) {
		return new ManagementProfileImpl(initiator);
	}

	protected void initChannelManagementProfile() {
		InternalChannel channel = new ChannelImpl(this, null, MANAGEMENT_CHANNEL, filterChainBuilder, null);
		ChannelHandler channelHandler = channelManagementProfile.createChannelHandler(this, channel);
		registerChannel(MANAGEMENT_CHANNEL, channel);
		channel.channelOpened(channelHandler);
	}
		
	protected InternalChannel createChannel(InternalSession session, String profileUri, int channelNumber) {
		return new ChannelImpl(session, profileUri, channelNumber, filterChainBuilder, sessionLock);
	}
	
	private ChannelHandler initChannel(InternalChannel channel, ChannelHandler handler) {
		return new UnlockingChannelHandler(handler, sessionLock);
	}
	
	protected void lock() {
		sessionLock.lock();
	}
	
	protected void unlock() {
		sessionLock.unlock();
	}
	
	private String traceInfo() {
		StringBuilder builder = new StringBuilder();
		builder.append("[").append(System.identityHashCode(this));
		builder.append("|").append(currentState);
		builder.append("|").append(initiator).append("] ");
		return builder.toString();
	}
	
	private void debug(Object... messages) {
		if (LOG.isDebugEnabled()) {
			StringBuffer buffer = new StringBuffer();
			for (Object message : messages) {
				buffer.append(message);
			}
			LOG.debug(buffer.toString());
		}
	}
	
	private void info(String message) {
		if (LOG.isInfoEnabled()) {
			LOG.info(traceInfo() + message);
		}
	}
	
	private void warn(String message, Exception e) {
		LOG.warn(traceInfo() + message, e);
	}
	
	private void setCurrentState(SessionState currentState) {
		debug("setting session state from ", this.currentState, " to ", currentState);
		this.currentState = currentState;
	}

	private SessionState getCurrentState() {
		return currentState;
	}

	public void addSessionListener(SessionListener l) {
		listeners.add(l);
	}
	
	protected void fireChannelStarted(int channel) {
		SessionListener[] list = listeners.toArray(new SessionListener[listeners.size()]);
		for (int i = 0; i < list.length; i++) {
			SessionListener listener = list[i];
			listener.channelStarted(channel);
		}	
	}

	protected void fireChannelClosed(int channel) {
		SessionListener[] list = listeners.toArray(new SessionListener[listeners.size()]);
		for (int i = 0; i < list.length; i++) {
			SessionListener listener = list[i];
			listener.channelClosed(channel);
		}
		
	}
	
	private int getNextChannelNumber() {
		return channelNumberSequence.next();
	}
	
	protected boolean hasOpenChannels() {
		return channels.size() > 1;
	}

	protected void registerChannel(int channelNumber, InternalChannel channel) {
		channels.put(channelNumber, channel);
		fireChannelStarted(channelNumber);
	}
	
	protected InternalChannel getChannel(int channelNumber) {
		InternalChannel channel = channels.get(channelNumber);
		if (channel == null) {
			throw new ProtocolException("channel " + channelNumber + " is not known by session");
		}
		return channel;
	}

	protected void removeChannel(int channelNumber) {
		channels.remove(channelNumber);
		fireChannelClosed(channelNumber);
	}
	
	protected void checkInitialAliveTransition() {
		if (greeting != null) {
			setCurrentState(aliveState);
			sessionHandler.sessionOpened(SessionImpl.this);
		}
	}
	
	// --> start of Session methods <--
	
	public String[] getProfiles() {
		if (greeting == null) {
			throw new IllegalStateException("greeting has not yet been received");
		}
		return greeting.getProfiles();
	}
	
	public void startChannel(String profileUri, ChannelHandler handler) {
		startChannel(new ProfileInfo(profileUri), handler);
	}
	
	public void startChannel(final ProfileInfo profile, final ChannelHandler channelHandler) {
		startChannel(new ProfileInfo[] { profile }, new ChannelHandlerFactory() {
			public ChannelHandler createChannelHandler(ProfileInfo info) {
				if (!profile.getUri().equals(info.getUri())) {
					throw new IllegalArgumentException("profile URIs do not match: "
							+ profile.getUri() + " | " + info.getUri());
				}
				return channelHandler;
			}
			public void startChannelFailed(int code, String message) {
				unlock();
				try {
					sessionHandler.channelStartFailed(profile.getUri(), channelHandler, code, message);
				} finally {
					lock();
				}
			}
		});
	}
	
	public void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory) {
		lock();
		try {
			getCurrentState().startChannel(profiles, factory);
		} finally {
			unlock();
		}
	}
	
	public void close() {
		lock();
		try {
			getCurrentState().closeSession();
		} finally {
			unlock();
		}
	}
	
	// --> end of Session methods <--
	
	
	// --> start of InternalSession methods <--
	
	/*
	 * This method is called by the channel implementation to send a message on
	 * a particular channel to the other peer.
	 */	
	public void sendMSG(int channelNumber, int messageNumber, Message message, ReplyHandler replyHandler) {
		lock();
		try {
			getCurrentState().sendMessage(channelNumber, messageNumber, message, replyHandler);
		} finally {
			unlock();
		}
	}
	
	public void sendANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
		lock();
		try {
			getCurrentState().sendANS(channelNumber, messageNumber, answerNumber, message);
		} finally {
			unlock();
		}
	}
	
	public void sendERR(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().sendERR(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}
	
	public void sendNUL(int channelNumber, int messageNumber) {
		lock();
		try {
			getCurrentState().sendNUL(channelNumber, messageNumber);
		} finally {
			unlock();
		}
	}
	
	public void sendRPY(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().sendRPY(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}

	/*
	 * This method is called by the channel implementation to send a close channel
	 * request to the other peer.
	 */
	public void requestChannelClose(final int channelNumber, final CloseCallback callback) {
		Assert.notNull("callback", callback);
		lock();
		try {
			channelManagementProfile.closeChannel(channelNumber, new CloseCallback() {
				public void closeDeclined(int code, String message) {
					callback.closeDeclined(code, message);
				}
				public void closeAccepted() {
					callback.closeAccepted();
					removeChannel(channelNumber);
				}
			});
		} finally {
			unlock();
		}
	}
	
	// --> end of InternalSession methods <--
		
	
	// --> start of SessionManager methods <--
	//
	//   - these methods are invoked while holding the session lock
	//   - they are called by the ChannelManagementProfile
	//   - thus, it is not necessary to lock / unlock the session lock here
	
	/*
	 * This method is invoked by the ChannelManagementProfile when the other
	 * peer requests creating a new channel.
	 */
	public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
		Assert.holdsLock("session", sessionLock);
		return getCurrentState().channelStartRequested(channelNumber, profiles);
	}
	
	/*
	 * This method is invoked by the ChannelManagement profile when a channel
	 * close request is received. This request is passed on to the ChannelHandler,
	 * that is the application, which decides what to do with the request to
	 * close the channel.
	 */
	public void channelCloseRequested(final int channelNumber, final CloseCallback callback) {
		Assert.holdsLock("session", sessionLock);
		getCurrentState().channelCloseRequested(channelNumber, callback);
	}
	
	/*
	 * This method is invoked by the ChannelManagement profile when a session
	 * close request is received.
	 */
	public void sessionCloseRequested(CloseCallback callback) {
		Assert.holdsLock("session", sessionLock);
		getCurrentState().sessionCloseRequested(callback);
	}
	
	// --> end of SessionManager methods <--
	
	
	// --> start of MessageHandler methods <-- 

	public final void receiveMSG(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveMSG(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}

	public final void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveANS(channelNumber, messageNumber, answerNumber, message);
		} finally {
			unlock();
		}
	}
	
	public final void receiveNUL(int channelNumber, int messageNumber) {
		lock();
		try {
			getCurrentState().receiveNUL(channelNumber, messageNumber);
		} finally {
			unlock();
		}
	}

	public final void receiveERR(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveERR(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}
		
	public final void receiveRPY(int channelNumber, int messageNumber, Message message) {
		lock();
		try {
			getCurrentState().receiveRPY(channelNumber, messageNumber, message);
		} finally {
			unlock();
		}
	}
	
	// --> end of MessageHandler methods <--
	
	/*
	 * Notifies the ChannelManagementProfile about this event. The
	 * ChannelManagementProfile then asks the application (SessionHandler)
	 * whether to accept the connection and sends the appropriate response.
	 */
	public void connectionEstablished(SocketAddress address) {
		lock();
		try {
			getCurrentState().connectionEstablished(address);
		} finally {
			unlock();
		}
	}
	
	public void exceptionCaught(Throwable cause) {
		lock();
		try {
			getCurrentState().exceptionCaught(cause);
		} finally {
			unlock();
		}
	}
	
	public void connectionClosed() {
		lock();
		try {
			getCurrentState().connectionClosed();
		} finally {
			unlock();
		}
	}
	
	// --> end of TransportHandler methods <--
	
	/**
	 * Interface of session states. The whole implementation of the SessionImpl
	 * class is based on the state pattern. All the important methods are
	 * delegated to an implementation of SessionState. A BEEP session is
	 * inherently state dependent. Some actions are not supported in
	 * certain states.
	 */
	protected static interface SessionState extends MessageHandler {
		
		void connectionEstablished(SocketAddress address);
		
		void sendRPY(int channelNumber, int messageNumber, Message message);

		void sendNUL(int channelNumber, int messageNumber);

		void sendERR(int channelNumber, int messageNumber, Message message);

		void sendANS(int channelNumber, int messageNumber, int answerNumber, Message message);

		void exceptionCaught(Throwable cause);

		void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory);
		
		void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler listener);
		
		void closeSession();
		
		StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles);
		
		void channelCloseRequested(int channelNumber, CloseCallback callback);
		
		void sessionCloseRequested(CloseCallback callback);
		
		void connectionClosed();
		
	}
	
	protected abstract class AbstractSessionState implements SessionState {
		
		public abstract String getName();
		
		public void exceptionCaught(Throwable cause) {
			// TODO: how to handle other exceptions?
			if (cause instanceof ProtocolException) {
				handleProtocolException((ProtocolException) cause);
			}			
		}

		private void handleProtocolException(ProtocolException cause) {
			warn("dropping connection because of a protocol exception", cause);
			try {
				sessionHandler.sessionClosed();
			} finally {
				setCurrentState(deadState);
				beepStream.closeTransport();
			}
		}
		
		public void connectionEstablished(SocketAddress address) {
			throw new IllegalStateException("connection already established, state=<" 
					+ getName() + ">");
		}
		
		public void startChannel(ProfileInfo[] profiles, ChannelHandlerFactory factory) {
			throw new IllegalStateException("" +
					"cannot start channel in state <" + getName() + ">");
		}
		
		public void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler listener) {
			throw new IllegalStateException(
					"cannot send messages in state <" + getName() + ">: channel="
					+ channelNumber);
		}
		
		public void sendANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			throw new IllegalStateException(
					"cannot send ANS message in state <" + getName() + ">: channel="
					+ channelNumber);
		}
		
		public void sendERR(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"cannot send ERR message in state <" + getName() + ">: channel="
					+ channelNumber);
		}
		
		public void sendNUL(int channelNumber, int messageNumber) {
			throw new IllegalStateException(
					"cannot send NUL message in state <" + getName() + ">: channel="
					+ channelNumber);			
		}
		
		public void sendRPY(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"cannot send RPY message in state <" + getName() + ">: channel="
					+ channelNumber);
		}
		
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
			return StartChannelResponse.createCancelledResponse(550, "cannot start channel");
		}

		public void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message ANS, channel=" + channelNumber 
					+ ",message=" + messageNumber
					+ ",answerNumber=" + answerNumber);
		}

		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message ERR, channel=" + channelNumber + ",message=" + messageNumber);
		}

		public void receiveMSG(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message MSG, channel=" + channelNumber + ",message=" + messageNumber);
		}

		public void receiveNUL(int channelNumber, int messageNumber) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message NUL, channel=" + channelNumber + ",message=" + messageNumber);
		}

		public void receiveRPY(int channelNumber, int messageNumber, Message message) {
			throw new IllegalStateException(
					"internal error: unexpected method invocation in state <" + getName() + ">: "
					+ "message RPY, channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void closeSession() {
			throw new IllegalStateException("cannot close session");
		}
		
		public void channelCloseRequested(int channelNumber, CloseCallback callback) {
			throw new IllegalStateException("cannot close channel");
		}
		
		public void sessionCloseRequested(CloseCallback callback) {
			throw new IllegalStateException("cannot close session");
		}
		
		public void connectionClosed() {
			try {
				sessionHandler.sessionClosed();
			} finally {
				setCurrentState(deadState);
			}
		}

	}

	protected class InitialState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "initial";
		}
		
		public void connectionEstablished(SocketAddress address) {			
			DefaultStartSessionRequest request = new DefaultStartSessionRequest(!initiator);
			sessionHandler.connectionEstablished(request);
			
			if (request.isCancelled()) {
				beepStream.sendERR(MANAGEMENT_CHANNEL, 0, channelManagementProfile.createSessionStartDeclined(request.getReplyCode(), request.getMessage()));
				setCurrentState(deadState);
				beepStream.closeTransport();
			} else {
				beepStream.sendRPY(MANAGEMENT_CHANNEL, 0, channelManagementProfile.createGreeting(request.getProfiles()));
			}
		}
		
		public void receiveMSG(int channelNumber, int messageNumber, Message message) {
			throw new ProtocolException(
					"first message in a session must be RPY or ERR on channel 0: "
					+ "was MSG channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			throw new ProtocolException(
					"first message in a session must be RPY or ERR on channel 0: "
					+ "was ANS channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void receiveNUL(int channelNumber, int messageNumber) {
			throw new ProtocolException(
					"first message in a session must be RPY or ERR on channel 0: "
					+ "was NUL channel=" + channelNumber + ",message=" + messageNumber);
		}
		
		public void receiveRPY(final int channelNumber, final int messageNumber, final Message message) {
			validateMessage(channelNumber, messageNumber);
			greeting = channelManagementProfile.receivedGreeting(message);
			checkInitialAliveTransition();
		}
		
		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			validateMessage(channelNumber, messageNumber);
			BEEPError error = channelManagementProfile.receivedError(message);
			
			info("received error, session start failed: " + error.getCode() + ":" + error.getMessage());
			
			sessionHandler.sessionStartDeclined(error.getCode(), error.getMessage());
			setCurrentState(deadState);
			beepStream.closeTransport();
		}

		private void validateMessage(int channelNumber, int messageNumber) {
			if (channelNumber != MANAGEMENT_CHANNEL || messageNumber != 0) {
				throw new ProtocolException("first message in session must be sent on "
						+ "channel 0 with message number 0: was channel " + channelNumber
						+ ", message=" + messageNumber);
			}
		}
		
		@Override
		public String toString() {
			return "<initial>";
		}

	}
	
	protected class AliveState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "alive";
		}
		
		@Override
		public void startChannel(final ProfileInfo[] profiles, final ChannelHandlerFactory factory) {
			final int channelNumber = getNextChannelNumber();
			channelManagementProfile.startChannel(channelNumber, profiles, new StartChannelCallback() {
				public void channelCreated(ProfileInfo info) {
					lock();
					try {
						ChannelHandler handler = factory.createChannelHandler(info);
						InternalChannel channel = createChannel(
								SessionImpl.this, info.getUri(), channelNumber);
						ChannelHandler channelHandler = initChannel(channel, handler);
						registerChannel(channelNumber, channel);
						channel.channelOpened(channelHandler);
					} finally {
						unlock();
					}
				}
				public void channelFailed(int code, String message) {
					lock();
					try {
						factory.startChannelFailed(code, message);
					} finally {
						unlock();
					}
				}
			});
		}
		
		@Override
		public void sendMessage(int channelNumber, int messageNumber, Message message, ReplyHandler listener) {
			debug("send message: channel=", channelNumber, ",message=", messageNumber);
			beepStream.sendMSG(channelNumber, messageNumber, message);
		}
		
		@Override
		public void sendANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			debug("send ANS: channel=", channelNumber, ",message=", messageNumber, ",answer=", answerNumber);
			beepStream.sendANS(channelNumber, messageNumber, answerNumber, message);
		}
		
		@Override
		public void sendERR(int channelNumber, int messageNumber, Message message) {
			debug("send ERR: channel=", channelNumber, ",message=", messageNumber);
			beepStream.sendERR(channelNumber, messageNumber, message);
		}
		
		@Override
		public void sendNUL(int channelNumber, int messageNumber) {
			debug("send NUL: channel=", channelNumber, ",message=", messageNumber);
			beepStream.sendNUL(channelNumber, messageNumber);
		}
		
		@Override
		public void sendRPY(int channelNumber, int messageNumber, Message message) {
			debug("send RPY: channel=", channelNumber, ",message=", messageNumber);
			beepStream.sendRPY(channelNumber, messageNumber, message);
		}
		
		@Override
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
			debug("start of channel ", channelNumber, " requested by remote peer: ", Arrays.toString(profiles));
			DefaultStartChannelRequest request = new DefaultStartChannelRequest(profiles);
			sessionHandler.channelStartRequested(request);
			
			StartChannelResponse response = request.getResponse();
			
			if (response.isCancelled()) {
				debug("start of channel ", channelNumber, " is cancelled by application: ", response.getCode(), 
						",'", response.getMessage(), "'");
				return response;
			}
			
			ProfileInfo info = response.getProfile();
			if (info == null) {
				throw new ProtocolException("StartChannelRequest must be either cancelled or a profile must be selected");
			}
			
			debug("start of channel ", channelNumber, " is accepted by application: ", info.getUri());
			
			InternalChannel channel = createChannel(SessionImpl.this, info.getUri(), channelNumber);
			ChannelHandler handler = initChannel(channel, response.getChannelHandler());
			registerChannel(channelNumber, channel);
			channel.channelOpened(handler);
			
			return response;
		}
		
		@Override
		public void receiveMSG(int channelNumber, int messageNumber, Message message) {			
			InternalChannel channel = getChannel(channelNumber);
			channel.receiveMSG(messageNumber, message);
		}

		@Override
		public void receiveANS(int channelNumber, int messageNumber, int answerNumber, Message message) {
			InternalChannel channel = getChannel(channelNumber);
			channel.receiveANS(messageNumber, answerNumber, message);
		}
		
		@Override
		public void receiveNUL(int channelNumber, int messageNumber) {
			InternalChannel channel = getChannel(channelNumber);
			channel.receiveNUL(messageNumber);
		}

		@Override
		public void receiveERR(int channelNumber, int messageNumber, Message message) {
			InternalChannel channel = getChannel(channelNumber);
			channel.receiveERR(messageNumber, message);
		}

		@Override
		public void receiveRPY(int channelNumber, int messageNumber, Message message) {
			InternalChannel channel = getChannel(channelNumber);
			channel.receiveRPY(messageNumber, message);
		}
		
		@Override
		public void closeSession() {
			// TODO: do not allow session close if there are still open channels
			setCurrentState(closeInitiatedState);
			channelManagementProfile.closeSession(new CloseCallback() {
				public void closeDeclined(int code, String message) {
					Assert.holdsLock("session", sessionLock);
					performClose();
				}
			
				public void closeAccepted() {
					Assert.holdsLock("session", sessionLock);
					performClose();
				}
				
				private void performClose() {
					try {
						sessionHandler.sessionClosed();
					} finally {
						setCurrentState(deadState);
						beepStream.closeTransport();
					}
				}
			});
		}
		
		@Override
		public void channelCloseRequested(final int channelNumber, final CloseCallback callback) {
			InternalChannel channel = getChannel(channelNumber);
			channel.channelCloseRequested(new CloseCallback() {
				public void closeDeclined(final int code, final String message) {
					callback.closeDeclined(code, message);			
				}
			
				public void closeAccepted() {
					callback.closeAccepted();
					removeChannel(channelNumber);
				}
			});
		}
		
		@Override
		public void sessionCloseRequested(CloseCallback callback) {
			if (hasOpenChannels()) {
				callback.closeDeclined(550, "still working");
			} else {
				callback.closeAccepted();
				try {
					sessionHandler.sessionClosed();
				} finally {
					setCurrentState(deadState);
					beepStream.closeTransport();
				}
			}
		}
		
		@Override
		public String toString() {
			return "<alive>";
		}
		
	}
	
	protected class CloseInitiatedState extends AliveState {
		
		@Override
		public String getName() {
			return "close-initiated";
		}
		
		@Override
		public void sessionCloseRequested(CloseCallback callback) {
			debug("received session close request while close is already in progress");
			try {
				sessionHandler.sessionClosed();
			} finally {
				callback.closeAccepted();
				beepStream.closeTransport();
				setCurrentState(deadState);
			}
		}
		
		@Override
		public StartChannelResponse channelStartRequested(int channelNumber, ProfileInfo[] profiles) {
			return StartChannelResponse.createCancelledResponse(550, "session release in progress");
		}
		
		@Override
		public String toString() {
			return "<wait-for-response>";
		}
		
	}

	protected class DeadState extends AbstractSessionState {
		
		@Override
		public String getName() {
			return "dead";
		}
		
		public void connectionClosed() {
			// ignore this one
		}
		
		@Override
		public String toString() {
			return "<dead>";
		}
				
	}
	
}
