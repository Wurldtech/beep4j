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
package net.sf.beep4j.internal.profile;

import java.net.SocketAddress;

import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.ReplyListener;
import net.sf.beep4j.ResponseHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.CloseCallback;
import net.sf.beep4j.internal.DefaultStartSessionRequest;
import net.sf.beep4j.internal.SessionManager;
import net.sf.beep4j.internal.StartChannelResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ChannelManagementProfile interface.
 * 
 * @author Simon Raess
 */
public class ChannelManagementProfileImpl implements ChannelHandler, ChannelManagementProfile {
	
	private static final Logger LOG = LoggerFactory.getLogger(ChannelManagementProfile.class);
	
	private SessionManager manager;
	
	private Channel channel;
	
	private final boolean initiator;
	
	private final ChannelManagementMessageBuilder builder;
	
	private final ChannelManagementMessageParser parser; 
	
	public ChannelManagementProfileImpl(boolean initiator) {
		this.initiator = initiator;
		this.builder = createChannelManagementMessageBuilder();
		this.parser = createChannelManagementMessageParser();
	}

	protected ChannelManagementMessageBuilder createChannelManagementMessageBuilder() {
		return new SaxMessageBuilder();
	}
	
	protected ChannelManagementMessageParser createChannelManagementMessageParser() {
		return new SaxMessageParser();
	}
	
	protected MessageBuilder createMessageBuilder() {
		MessageBuilder builder = channel.createMessageBuilder();
		builder.setContentType("application", "beep+xml");
		builder.setCharsetName("UTF-8");
		return builder;
	}
	
	
	// --> start of ChannelHandler methods <--
	
	/**
	 * This method is only called on channels created through the
	 * startChannel methods of the Session. Thus, this method can
	 * safely throw an UnsupportedOperationException because it
	 * is never called. The channel management profile is created
	 * by the session itself when it starts up.
	 * 
	 * @throws UnsupportedOperationException unconditionally
	 */
	public void channelStartFailed(int code, String message) {
		throw new UnsupportedOperationException();
	}
	
	public void channelOpened(Channel c) {
		this.channel = c;
	}
	
	public void messageReceived(Message message, final ResponseHandler handler) {
		ChannelManagementRequest r = parser.parseRequest(message);
		LOG.debug("received request of type " + r.getClass().getSimpleName());
		
		if (r instanceof StartChannelMessage) {
			StartChannelMessage request = (StartChannelMessage) r; 
			int channelNumber = request.getChannelNumber();
			
			// validate start channel request
			if (initiator && channelNumber % 2 != 0) {
				LOG.info("received invalid start channel request: number attribute in <start> element must be " 
						+ "odd valued (was=" + channelNumber + ")");
				handler.sendERR(builder.createError(
						createMessageBuilder(), 501, "number attribute in <start> element must be odd valued"));
			} else if (!initiator && channelNumber % 2 != 1) {
				LOG.info("received invalid start channel request: number attribute in <start> element must be "
						+ "even valued (was=" + channelNumber + ")");
				handler.sendERR(builder.createError(
						createMessageBuilder(), 501, "number attribute in <start> element must be even valued"));
			} else {
				handleStartChannelRequest(request, handler);
			}
			
		} else if (r instanceof CloseChannelMessage) {
			final CloseChannelMessage request = (CloseChannelMessage) r;
			
			// TODO: do we really need different behavior for channel 0?
			if (request.getChannelNumber() == 0) {
				LOG.info("session close requested");
				manager.sessionCloseRequested(new CloseCallback() {
					public void closeDeclined(int code, String message) {
						LOG.info("close of session "
								+ " declined by framework: "
								+ code + ",'" + message + "'");
						handler.sendERR(
								builder.createError(createMessageBuilder(), code, message));
					}
				
					public void closeAccepted() {
						LOG.info("close of session "
								+ " accepted by framework");
						handler.sendRPY(
								builder.createOk(createMessageBuilder()));
					}
				});
				
			} else {
				LOG.info("close of channel " + request.getChannelNumber() + " requested");
				manager.channelCloseRequested(request.getChannelNumber(), new CloseChannelRequest() {
					public void reject() {
						LOG.info("close of channel " + request.getChannelNumber()
								+ " declined by application");
						handler.sendERR(builder.createError(
								createMessageBuilder(), 550, "still working"));
					}
					public void accept() {
						LOG.info("close of channel " + request.getChannelNumber()
								+ " accepted by application");
						handler.sendRPY(builder.createOk(createMessageBuilder()));
					}
				});
			}
			
		} else {
			throw new RuntimeException("unexpected code path");
		}
	}

	private void handleStartChannelRequest(StartChannelMessage request, ResponseHandler handler) {
		StartChannelResponse response = manager.channelStartRequested(
				request.getChannelNumber(), request.getProfiles());			
		
		if (response.isCancelled()) {
			LOG.info("start channel request is cancelled by application: "
					+ response.getCode() + "," + response.getMessage());
			handler.sendERR(builder.createError(
					createMessageBuilder(), response.getCode(), response.getMessage()));
			
		} else {
			LOG.info("start channel request is accepted by application: "
					+ response.getProfile().getUri());
			handler.sendRPY(builder.createProfile(
					createMessageBuilder(), response.getProfile()));
		}
	}
	
	public void closeRequested(CloseChannelRequest request) {
		throw new UnsupportedOperationException("unexpected code path");
	}
	
	public void channelClosed() {
		this.channel = null;
	}
	
	// --> end of ChannelHandler methods <--
	
	
	// --> start of ChannelManagementProfile methods <--
	
	public ChannelHandler createChannelHandler(SessionManager manager) {
		this.manager = manager;
		return this;
	}
	
	public boolean connectionEstablished(
			SocketAddress address, 
			SessionHandler sessionHandler, 
			ResponseHandler response) {
		DefaultStartSessionRequest request = new DefaultStartSessionRequest(!initiator);
		sessionHandler.connectionEstablished(request);
		
		if (request.isCancelled()) {
			response.sendERR(createError(request.getReplyCode(), request.getMessage()));
		} else {
			response.sendRPY(createGreeting(request.getProfiles()));
		}
		
		return !request.isCancelled();
	}
	
	public Message createGreeting(String[] profiles) {
		return builder.createGreeting(createMessageBuilder(), profiles);
	}
	
	public Message createError(int code, String diagnostics) {
		return builder.createError(createMessageBuilder(), code, diagnostics);
	}

	public Greeting receivedGreeting(Message message) {
		return parser.parseGreeting(message);
	}

	public BEEPError receivedError(Message message) {
		return parser.parseError(message);
	}
	
	public void startChannel(int channelNumber, ProfileInfo[] infos, final StartChannelCallback callback) {
		Message message = builder.createStart(createMessageBuilder(), channelNumber, infos);
		channel.sendMessage(message, new ReplyListener() {
		
			public void receiveRPY(Message message) {
				ProfileInfo profile = parser.parseProfile(message);
				callback.channelCreated(profile);
			}
		
			public void receiveERR(Message message) {
				BEEPError error = parser.parseError(message);
				callback.channelFailed(error.getCode(), error.getMessage());
			}
		
			public void receiveNUL() {
				throw new ProtocolException("message type NUL is not a valid response");		
			}
			
			public void receiveANS(Message message) {
				throw new ProtocolException("message type ANS is not a valid response");		
			}
		});
	}
	
	public void closeChannel(int channelNumber, final CloseChannelCallback callback) {
		Message message = builder.createClose(createMessageBuilder(), channelNumber, 200);
		channel.sendMessage(message, new ReplyListener() {
		
			public void receiveRPY(Message message) {
				parser.parseOk(message);
				callback.closeAccepted();
			}
		
			public void receiveERR(Message message) {
				BEEPError error = parser.parseError(message);
				callback.closeDeclined(error.getCode(), error.getMessage());
			}
		
			public void receiveANS(Message message) {
				throw new UnsupportedOperationException();		
			}
			
			public void receiveNUL() {
				throw new UnsupportedOperationException();		
			}
		
		});
	}
	
	public void closeSession(final CloseCallback callback) {
		Message message = builder.createClose(createMessageBuilder(), 0, 200);
		channel.sendMessage(message, new ReplyListener() {
		
			public void receiveRPY(Message message) {
				parser.parseOk(message);
				callback.closeAccepted();
			}
		
			public void receiveERR(Message message) {
				BEEPError error = parser.parseError(message);
				callback.closeDeclined(error.getCode(), error.getMessage());
			}
		
			public void receiveANS(Message message) {
				throw new ProtocolException("ANS message not valid response for close request");
			}
			
			public void receiveNUL() {
				throw new ProtocolException("NUL message not valid response for close request");
			}
		
		});
	}
	
	// --> end of ChannelManagementProfile methods <--
		
}
