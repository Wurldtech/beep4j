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
package net.sf.beep4j.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.beep4j.Channel;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.Initiator;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageBuilder;
import net.sf.beep4j.ProfileInfo;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.Reply;
import net.sf.beep4j.Session;
import net.sf.beep4j.SessionHandler;
import net.sf.beep4j.SessionHandlerFactory;
import net.sf.beep4j.StartChannelRequest;
import net.sf.beep4j.StartSessionRequest;
import net.sf.beep4j.ext.ChannelHandlerAdapter;
import net.sf.beep4j.ext.SessionHandlerAdapter;
import net.sf.beep4j.transport.mina.MinaInitiator;
import net.sf.beep4j.transport.mina.MinaListener;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;

public class EchoIntegrationTest extends TestCase {
	
	public void testOneToManyEcho() throws Exception {
		ProfileInfo profile = new ProfileInfo(OneToManyEchoProfileHandler.PROFILE, "8192");
		String text = loadMessage("rfc3080.txt");
		doTest(profile, 1, text, 8001);
	}
	
	public void testEcho() throws Exception {
		ProfileInfo profile = new ProfileInfo(EchoProfileHandler.PROFILE);
		String text = loadMessage("rfc3080.txt");
		doTest(profile, 1, text, 8001);
	}
	
	public void testSimultanousEcho() throws Exception {
		ProfileInfo profile = new ProfileInfo(EchoProfileHandler.PROFILE);
		String text = loadMessage("rfc3080.txt");
		doTest(profile, 3, text, 8001);
	}
	
	public void testOneToManySimultanousEcho() throws Exception {
		ProfileInfo profile = new ProfileInfo(OneToManyEchoProfileHandler.PROFILE, "8192");
		String text = loadMessage("rfc3080.txt");
		doTest(profile, 3, text, 8001);
	}
	
	protected void doTest(ProfileInfo profile, int channels, String text, int port) throws Exception {
		Semaphore sem = new Semaphore(-channels);
		
		IoAcceptor acceptor = new VmPipeAcceptor();
		SocketAddress address = new VmPipeAddress(port);

		MinaListener listener = new MinaListener(acceptor);
		listener.bind(address, new EchoSessionHandlerFactory(sem));
		
		IoConnector connector = new VmPipeConnector();
		EchoClientHandler client = new EchoClientHandler(profile, channels, text, sem);
		
		Initiator initiator = new MinaInitiator(connector);
		initiator.connect(address, client);
		
		sem.acquire();
		listener.unbind(address);
		
		client.assertEquals(text);
	}
	
	private String loadMessage(String resource) throws IOException {
		InputStream stream = getClass().getResourceAsStream(resource);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "US-ASCII"));
		StringBuilder builder = new StringBuilder();
		
		char[] buf = new char[1024];
		int count;
		
		while ((count = reader.read(buf)) != -1) {
			builder.append(new String(buf, 0, count));
		}
		
		reader.close();
		return builder.toString();
	}
	
	private static class EchoSessionHandlerFactory implements SessionHandlerFactory {
		private final Semaphore semaphore;
		private EchoSessionHandlerFactory(Semaphore semaphore) {
			this.semaphore = semaphore;
		}
		public SessionHandler createSessionHandler() {
			return new EchoSessionHandler(semaphore);
		}
	}
	
	protected static class EchoSessionHandler extends SessionHandlerAdapter {
		private final Semaphore semaphore;
		
		private EchoSessionHandler(Semaphore semaphore) {
			this.semaphore = semaphore;
		}
		
		public void connectionEstablished(StartSessionRequest s) {
			s.registerProfile(EchoProfileHandler.PROFILE);
			s.registerProfile(OneToManyEchoProfileHandler.PROFILE);
		}
		
		@Override
		public void channelStartRequested(StartChannelRequest startup) {
			if (startup.hasProfile(EchoProfileHandler.PROFILE)) {
				startup.selectProfile(
						startup.getProfile(EchoProfileHandler.PROFILE), 
						new EchoProfileHandler());				
			} else if (startup.hasProfile(OneToManyEchoProfileHandler.PROFILE)) {
				ProfileInfo profile = startup.getProfile(OneToManyEchoProfileHandler.PROFILE);
				int size = getSize(profile.getContent());
				startup.selectProfile(profile, new OneToManyEchoProfileHandler(size));
			}
		}
		
		private int getSize(String content) {
			try {
				return Integer.parseInt(content.trim());
			} catch (Exception e) {
				return 8192;
			}
		}
		
		@Override
		public void sessionClosed() {
			semaphore.release();
		}
	}
	
	private class EchoClientHandler extends SessionHandlerAdapter {
		private final ProfileInfo profile;
		private final String text;
		private final Semaphore semaphore;
		private Talker[] talkers;
		
		private EchoClientHandler(ProfileInfo profile, int channels, String text, Semaphore semaphore) {
			this.profile = profile;
			this.talkers = new Talker[channels];
			this.text = text;
			this.semaphore = semaphore;
		}
		
		public void assertEquals(String text) {
			for (int i = 0; i < talkers.length; i++) {
				Talker talker = talkers[i];
				talker.assertEquals(text);
			}
		}
		
		@Override
		public void sessionOpened(Session session) {
			for (int i = 0; i < talkers.length; i++) {
				talkers[i] = new Talker(text, semaphore);
				session.startChannel(profile, talkers[i]);
			}
		}
	}
	
	protected class Talker extends ChannelHandlerAdapter {
		
		private final String expected;
		
		private final Semaphore semaphore;
		
		private EchoListener listener;
		
		protected Talker(String expected, Semaphore semaphore) {
			this.expected = expected;
			this.semaphore = semaphore;
		}
		
		public void assertEquals(String text) {
			Assert.assertEquals(text, listener.getReceivedText());
		}
		
		public void channelOpened(Channel c) {
			MessageBuilder builder = c.createMessageBuilder();
			builder.setCharsetName("US-ASCII");
			try {
				Writer writer = builder.getWriter();
				writer.write(expected);
				writer.close();
				listener = new EchoListener(c, semaphore);
				c.sendMessage(builder.getMessage(), listener);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public void messageReceived(Message message, Reply handler) {
			throw new UnsupportedOperationException();
		}
		
	}
	
	protected class EchoListener implements ReplyHandler {
		private final Channel channel;
		private final Semaphore semaphore;
		private final StringBuilder builder;
		private String actual;
		
		protected EchoListener(Channel channel, Semaphore semaphore) {
			this.channel = channel;
			this.semaphore = semaphore;
			this.builder = new StringBuilder();
		}
		
		protected String getReceivedText() {
			return actual;
		}
		
		public void receivedANS(Message message) {
			String str = toString(message);
			builder.append(str);
		}
		
		public void receivedERR(Message message) {
			throw new UnsupportedOperationException();
		}
		
		public void receivedNUL() {
			verify();
			channel.close(new PrintingCloseCallback(channel.getSession(), semaphore));
		}
		
		public void receivedRPY(Message message) {
			builder.append(toString(message));
			verify();
			channel.close(new PrintingCloseCallback(channel.getSession(), semaphore));
		}
		
		private String toString(Message message) {
			BufferedReader reader = new BufferedReader(message.getReader("UTF-8"));
			StringBuilder builder = new StringBuilder();
			int i;
			
			try {
				while ((i = reader.read()) != -1) {
					builder.append((char) i);
				}				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			return builder.toString();
		}
		
		private void verify() {
			actual = builder.toString();
		}
	}
	
	private class PrintingCloseCallback implements CloseChannelCallback {
		private final Session session;
		private final Semaphore semaphore;
		
		private PrintingCloseCallback(Session session, Semaphore semaphore) {
			this.session = session;
			this.semaphore = semaphore;
		}
		
		public void closeAccepted() {
			if (semaphore.availablePermits() >= -1) {
				session.close();
			}
			semaphore.release();
		}
		
		public void closeDeclined(int code, String message) { }
	}	

}
