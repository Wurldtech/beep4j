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
package net.sf.beep4j.transport.mina;

import net.sf.beep4j.ChannelFilterChainBuilder;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.internal.SessionImpl;
import net.sf.beep4j.internal.TransportMapping;
import net.sf.beep4j.internal.tcp.TCPMapping;
import net.sf.beep4j.internal.util.HexDump;
import net.sf.beep4j.transport.LoggingTransportContext;
import net.sf.beep4j.transport.Transport;
import net.sf.beep4j.transport.TransportContext;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport implementation based on Apache MINA. See {@link http://mina.apache.org}
 * for more information about MINA.
 * 
 * @author Simon Raess
 */
public class MinaTransport extends IoHandlerAdapter implements Transport {
	
	private static final Logger DATA_LOG = LoggerFactory.getLogger("net.sf.beep4j.transport.DATA");
	
	private static final Logger LOG = LoggerFactory.getLogger("net.sf.beep4j.transport");
	
	private IoSession session;
	
	private TransportContext context;
	
	public MinaTransport(boolean initiator, SessionHandler sessionHandler, ChannelFilterChainBuilder builder) {
		TransportMapping mapping = new TCPMapping(this);
		context = new LoggingTransportContext(new SessionImpl(initiator, sessionHandler, mapping));
	}
	
	public void sendBytes(java.nio.ByteBuffer buffer) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("sending " + buffer.remaining() + " bytes");
		}
		if (DATA_LOG.isDebugEnabled()) {
			DATA_LOG.debug(HexDump.dump(buffer));
		}
		session.write(ByteBuffer.wrap(buffer));
	}
	
	public void closeTransport() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("close transport");
		}
		session.close();
	}
	
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("transport session opened");
		}
		this.session = session;
		context.connectionEstablished(session.getRemoteAddress());
	}
	
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		LOG.debug("bytes sent on underlying transport");
	}
	
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		ByteBuffer buffer = (ByteBuffer) message;
		if (LOG.isDebugEnabled()) {
			LOG.debug("received " + buffer.remaining() + " bytes");
		}
		if (DATA_LOG.isDebugEnabled()) {
			DATA_LOG.debug(HexDump.dump(buffer.buf()));
		}
		context.messageReceived(buffer.buf());
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("caugth exception", cause);
		}
		context.exceptionCaught(cause);
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("transport session closed by remote peer");
		}
		context.connectionClosed();
	}
	
}
