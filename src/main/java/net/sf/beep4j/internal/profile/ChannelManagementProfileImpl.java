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

import net.sf.beep4j.ChannelHandler;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ProtocolException;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.CloseCallback;
import net.sf.beep4j.internal.DefaultCloseChannelRequest;
import net.sf.beep4j.internal.DefaultStartSessionRequest;
import net.sf.beep4j.internal.SessionManager;
import net.sf.beep4j.internal.StartChannelResponse;
import net.sf.beep4j.internal.message.DefaultMessageBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ChannelManagementProfile interface.
 * 
 * @author Simon Raess
 */
public class ChannelManagementProfileImpl implements ChannelManagementProfile {
	
	private static final Logger LOG = LoggerFactory.getLogger(ChannelManagementProfile.class);
	
	private SessionManager manager;
	
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
		MessageBuilder builder = new DefaultMessageBuilder();
		builder.setContentType("application", "beep+xml");
		builder.setCharsetName("UTF-8");
		return builder;
	}
		
	public ChannelHandler createChannelHandler(SessionManager manager) {
		this.manager = manager;
		return new ManagementChannelHandler(this, parser);
	}
	
	public boolean connectionEstablished(
			SocketAddress address, 
			SessionHandler sessionHandler, 
			Reply reply) {
		DefaultStartSessionRequest request = new DefaultStartSessionRequest(!initiator);
		sessionHandler.connectionEstablished(request);
		
		if (request.isCancelled()) {
			reply.sendERR(createError(request.getReplyCode(), request.getMessage()));
		} else {
			reply.sendRPY(createGreeting(request.getProfiles()));
		}
		
		return !request.isCancelled();
	}
	
	protected Message createGreeting(String[] profiles) {
		return builder.createGreeting(createMessageBuilder(), profiles);
	}
	
	protected Message createError(int code, String diagnostics) {
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
		manager.sendMessage(message, new ReplyHandler() {
		
			public void receivedRPY(Message message) {
				ProfileInfo profile = parser.parseProfile(message);
				callback.channelCreated(profile);
			}
		
			public void receivedERR(Message message) {
				BEEPError error = parser.parseError(message);
				callback.channelFailed(error.getCode(), error.getMessage());
			}
		
			public void receivedNUL() {
				throw new ProtocolException("NUL is not a valid response to a start channel request");		
			}
			
			public void receivedANS(Message message) {
				throw new ProtocolException("ANS is not a valid response to a start channel request");
			}
		});
	}
	
	public void closeChannel(int channelNumber, final CloseChannelCallback callback) {
		Message message = builder.createClose(createMessageBuilder(), channelNumber, 200);
		manager.sendMessage(message, new ReplyHandler() {
		
			public void receivedRPY(Message message) {
				parser.parseOk(message);
				callback.closeAccepted();
			}
		
			public void receivedERR(Message message) {
				BEEPError error = parser.parseError(message);
				callback.closeDeclined(error.getCode(), error.getMessage());
			}
		
			public void receivedANS(Message message) {
				throw new ProtocolException("ANS is not a valid response to a close channel request");		
			}
			
			public void receivedNUL() {
				throw new ProtocolException("NUL is not a valid response to a close channel request");		
			}
		
		});
	}
	
	public void closeSession(final CloseCallback callback) {
		Message message = builder.createClose(createMessageBuilder(), 0, 200);
		manager.sendMessage(message, new ReplyHandler() {
		
			public void receivedRPY(Message message) {
				parser.parseOk(message);
				callback.closeAccepted();
			}
		
			public void receivedERR(Message message) {
				BEEPError error = parser.parseError(message);
				callback.closeDeclined(error.getCode(), error.getMessage());
			}
		
			public void receivedANS(Message message) {
				throw new ProtocolException("ANS message not valid response for close session request");
			}
			
			public void receivedNUL() {
				throw new ProtocolException("NUL message not valid response for close session request");
			}
		
		});
	}
	
	public void startChannelRequested(final int channelNumber, final ProfileInfo[] profiles, final Reply reply) {
		// first validate the request, throw a ProtocolException if it fails
		validateStartChannelRequest(channelNumber, reply); 

		StartChannelResponse response = manager.channelStartRequested(channelNumber, profiles);			
		
		if (response.isCancelled()) {
			LOG.debug("start channel request is cancelled by application: "
					+ response.getCode() + "," + response.getMessage());
			reply.sendERR(builder.createError(
					createMessageBuilder(), response.getCode(), response.getMessage()));
			
		} else {
			LOG.debug("start channel request is accepted by application: "
					+ response.getProfile().getUri());
			reply.sendRPY(builder.createProfile(
					createMessageBuilder(), response.getProfile()));
		}
	}

	private void validateStartChannelRequest(final int channelNumber,
			final Reply reply) {
		// validate start channel request
		if (initiator && channelNumber % 2 != 0) {
			LOG.warn("received invalid start channel request: number attribute in <start> element must be " 
					+ "odd valued (was=" + channelNumber + ")");
			reply.sendERR(builder.createError(
					createMessageBuilder(), 501, "number attribute in <start> element must be odd valued"));
		} else if (!initiator && channelNumber % 2 != 1) {
			LOG.warn("received invalid start channel request: number attribute in <start> element must be "
					+ "even valued (was=" + channelNumber + ")");
			reply.sendERR(builder.createError(
					createMessageBuilder(), 501, "number attribute in <start> element must be even valued"));
		}
	}

	public void closeChannelRequested(final int channelNumber, final Reply reply) {
		DefaultCloseChannelRequest request = new DefaultCloseChannelRequest();
		manager.channelCloseRequested(channelNumber, request);
		if (request.isAccepted()) {
			reply.sendRPY(builder.createOk(createMessageBuilder()));
		} else {
			reply.sendERR(builder.createError(createMessageBuilder(), 550, "still working"));
		}
	}

	public void closeSessionRequested(final Reply reply) {
		LOG.debug("session close requested");
		manager.sessionCloseRequested(new CloseCallback() {
			public void closeDeclined(int code, String message) {
				LOG.debug("close of session declined by framework: "
						+ code + ",'" + message + "'");
				reply.sendERR(
						builder.createError(createMessageBuilder(), code, message));
			}
		
			public void closeAccepted() {
				LOG.debug("close of session accepted by framework");
				reply.sendRPY(builder.createOk(createMessageBuilder()));
			}
		});
	}
	
	// --> end of ChannelManagementProfile methods <--
		
}
