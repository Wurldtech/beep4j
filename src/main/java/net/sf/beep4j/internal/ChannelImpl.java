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

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Session;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;
import net.sf.beep4j.internal.util.Assert;

class ChannelImpl implements Channel, ChannelHandler, InternalChannel {
	
	private final InternalSession session;
	
	private final String profile;
	
	private final int channelNumber;
	
	private ChannelHandler channelHandler;
	
	private State state = new Alive();
	
	/**
	 * Counter that counts how many messages we have sent but to which we
	 * have not received a reply.
	 */
	private int outstandingReplyCount;
	
	/**
	 * Counter that counts how many messages we have received but to which
	 * we have not sent a response.
	 */
	private int outstandingResponseCount;
	
	public ChannelImpl(
			InternalSession session, 
			String profile, 
			int channelNumber) {
		this.session = session;
		this.profile = profile;
		this.channelNumber = channelNumber;
	}
	
	public ChannelHandler initChannel(ChannelHandler channelHandler) {
		Assert.notNull("channelHandler", channelHandler);
		this.channelHandler = channelHandler;
		return this;
	}
	
	public boolean isAlive() {
		return state instanceof Alive;
	}
	
	public boolean isDead() {
		return state instanceof Dead;
	}
	
	public boolean isShuttingDown() {
		return !isAlive() && !isDead();
	}
	
	public String getProfile() {
		return profile;
	}

	public Session getSession() {
		return session;
	}
	
	public MessageBuilder createMessageBuilder() {
		return new DefaultMessageBuilder();
	}
	
	protected void setState(State state) {
		this.state = state;
		this.state.checkCondition();
	}
	
	public void sendMessage(final Message message, final ReplyHandler listener) {
		Assert.notNull("message", message);
		Assert.notNull("listener", listener);
		state.sendMessage(message, new ReplyListenerWrapper(listener));
	}
	
	public void close(CloseChannelCallback callback) {
		Assert.notNull("callback", callback);
		state.closeInitiated(callback);
	}
	
	public void channelClosed() {
		channelHandler.channelClosed();
	}
	
	public void channelStartFailed(int code, String message) {
		channelHandler.channelStartFailed(code, message);
	}
	
	public void channelOpened(Channel c) {
		channelHandler.channelOpened(this);		
	}
	
	public void messageReceived(Message message, Reply handler) {
		state.messageReceived(message, new ReplyWrapper(handler));		
	}
	
	public void channelCloseRequested(CloseChannelRequest request) {
		state.closeRequested(request);
	}
	
	private synchronized void incrementOutstandingReplyCount() {
		outstandingReplyCount++;
	}
	
	private synchronized void decrementOutstandingReplyCount() {
		outstandingReplyCount--;
		state.checkCondition();
	}
	
	private synchronized boolean hasOutstandingReplies() {
		return outstandingReplyCount > 0;
	}
	
	private synchronized void incrementOutstandingResponseCount() {
		outstandingResponseCount++;
	}
	
	private synchronized void decrementOutstandingResponseCount() {
		outstandingResponseCount--;
		state.checkCondition();
	}
	
	private synchronized boolean hasOutstandingResponses() {
		return outstandingResponseCount > 0;
	}
	
	private synchronized boolean isReadyToShutdown() {
		return !hasOutstandingReplies() && !hasOutstandingResponses();
	}

	/*
	 * Wrapper for ReplyListener that decrements a counter whenever
	 * a complete message has been received.
	 */
	private class ReplyListenerWrapper implements ReplyHandler {

		private final ReplyHandler target;
		
		private ReplyListenerWrapper(ReplyHandler target) {
			this.target = target;
			incrementOutstandingReplyCount();
		}
		
		public void receivedANS(Message message) {
			target.receivedANS(message);			
		}
		
		public void receivedNUL() {
			decrementOutstandingReplyCount();
			target.receivedNUL();
		}
		
		public void receivedERR(Message message) {
			decrementOutstandingReplyCount();
			target.receivedERR(message);
		}
		
		public void receivedRPY(Message message) {
			decrementOutstandingReplyCount();
			target.receivedRPY(message);
		}
	}
	
	private class ReplyWrapper implements Reply {
		
		private final Reply target;
		
		private ReplyWrapper(Reply target) {
			this.target = target;
			incrementOutstandingResponseCount();
		}
		
		public MessageBuilder createMessageBuilder() {
			return target.createMessageBuilder();
		}
		
		public void sendANS(Message message) {
			target.sendANS(message);			
		}
		
		public void sendNUL() {
			decrementOutstandingResponseCount();
			target.sendNUL();
		}
		
		public void sendERR(Message message) {
			decrementOutstandingResponseCount();
			target.sendERR(message);
		}
		
		public void sendRPY(Message message) {
			decrementOutstandingResponseCount();
			target.sendRPY(message);
		}
	}
	
	private static interface State {
		
		void checkCondition();
		
		void sendMessage(Message message, ReplyHandler listener);
		
		void closeInitiated(CloseChannelCallback callback);
		
		void closeRequested(CloseChannelRequest request);
		
		void messageReceived(Message message, Reply handler);
		
	}
	
	private static abstract class AbstractState implements State {
		
		public void checkCondition() {
			// nothing to check
		}
		
		public void sendMessage(Message message, ReplyHandler listener) {
			throw new IllegalStateException();
		}
		
		public void closeInitiated(CloseChannelCallback callback) {
			throw new IllegalStateException();
		}
		
		public void closeRequested(CloseChannelRequest request) {
			throw new IllegalStateException();
		}
		
		public void messageReceived(Message message, Reply handler) {
			throw new IllegalStateException();
		}
	}
	
	private class Alive extends AbstractState {
		
		@Override
		public void sendMessage(final Message message, final ReplyHandler listener) {
			session.sendMessage(channelNumber, message, listener);
		}
		
		@Override
		public void messageReceived(Message message, Reply handler) {
			channelHandler.messageReceived(message, handler);
		}
		
		@Override
		public void closeInitiated(CloseChannelCallback callback) {
			setState(new CloseInitiated(callback));
		}
		
		@Override
		public void closeRequested(CloseChannelRequest request) {
			// TODO: notify application (=ChannelHandler) about this event
			setState(new CloseRequested(request));
		}
		
	}
	
	private class CloseInitiated extends AbstractState {
		
		private final CloseChannelCallback callback;
		
		private CloseInitiated(CloseChannelCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void messageReceived(Message message, Reply handler) {
			channelHandler.messageReceived(message, handler);
		}
		
		@Override
		public void checkCondition() {
			if (isReadyToShutdown()) {
				session.requestChannelClose(channelNumber, new CloseChannelCallback() {
					public void closeDeclined(int code, String message) {
						// TODO how do we handle exceptions from the callback?
						callback.closeDeclined(code, message);
						setState(new Alive());
					}
					public void closeAccepted() {
						// TODO how do we handle exceptions from the callback?
						channelClosed();
						callback.closeAccepted();
						setState(new Dead());
					}
				});
			}
		}
		
		/*
		 * If we receive a close request in this state, we accept the close
		 * request immediately without consulting the application. The
		 * reasoning is that the application already requested to close
		 * the channel, so it makes no sense to let it change that 
		 * decision.
		 */
		@Override
		public void closeRequested(CloseChannelRequest request) {
			callback.closeAccepted();
			channelClosed();
			request.accept();
			setState(new Dead());
		}
		
	}
	
	private class CloseRequested extends AbstractState {
		
		private final CloseChannelRequest request;
		
		private CloseRequested(CloseChannelRequest request) {
			this.request = request;
		}
		
		@Override
		public void messageReceived(Message message, Reply handler) {
			channelHandler.messageReceived(message, handler);
		}
		
		@Override
		public void checkCondition() {
			if (isReadyToShutdown()) {
				channelHandler.channelCloseRequested(new CloseChannelRequest() {
					public void reject() {
						setState(new Alive());
						request.reject();
					}
					public void accept() {
						setState(new Dead());
						request.accept();
						// TODO: handle exceptions
						channelHandler.channelClosed();
					}
				});
			}
		}
	}
	
	private class Dead extends AbstractState {
		// dead is dead ;)
	}
	
}
