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

import java.lang.reflect.Method;

import junit.framework.TestCase;
import net.sf.beep4j.Channel;
import net.sf.beep4j.ChannelFilter;
import net.sf.beep4j.CloseChannelCallback;
import net.sf.beep4j.CloseChannelRequest;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageStub;
import net.sf.beep4j.Reply;
import net.sf.beep4j.ReplyHandler;
import net.sf.beep4j.ChannelFilter.NextFilter;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

/**
 * @author Simon Raess
 */
public class ChannelFilterChainTest extends TestCase {
	
	private Mockery context;
	
	private Sequence sequence;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = new Mockery();
		sequence = context.sequence("filter-sequence");
	}
	
	private <T> T mock(Class<T> typeToMock) {
		return context.mock(typeToMock);
	}
	
	private <T> T mock(Class<T> typeToMock, String name) {
		return context.mock(typeToMock, name);
	}
	
	private void assertIsSatisfied() {
		context.assertIsSatisfied();
	}
	
	private void checking(Expectations expectations) {
		context.checking(expectations);
	}
	
	public void testFilterSendMessage() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final Message message = new MessageStub();
		final ReplyHandler replyHandler = mock(ReplyHandler.class);
		final ChannelFilter[] filters = new ChannelFilter[3];
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterSendMessage(with(any(NextFilter.class)), with(same(message)), with(same(replyHandler)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addFirst(filter);
		}
		
		target.fireFilterSendMessage(message, replyHandler);
		
		assertIsSatisfied();
	}
	
	public void testFilterClose() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final CloseChannelCallback callback = mock(CloseChannelCallback.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterClose(with(any(NextFilter.class)), with(same(callback)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addFirst(filter);
		}
		
		target.fireFilterClose(callback);
		
		assertIsSatisfied();
	}
	
	public void testFilterChannelOpened() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Channel channel = mock(Channel.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterChannelOpened(with(any(NextFilter.class)), with(same(channel)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterChannelOpened(channel);
		
		assertIsSatisfied();
	}
	
	public void testFilterMessageReceived() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = mock(Message.class);
		final Reply reply = mock(Reply.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterMessageReceived(with(any(NextFilter.class)), with(same(message)), with(same(reply)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterMessageReceived(message, reply);
		
		assertIsSatisfied();
	}
	
	public void testFilterChannelCloseRequested() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final CloseChannelRequest request = mock(CloseChannelRequest.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterChannelCloseRequested(with(any(NextFilter.class)), with(same(request)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterChannelCloseRequested(request);
		
		assertIsSatisfied();
	}
	
	public void testFilterChannelClosed() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterChannelClosed(with(any(NextFilter.class)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterChannelClosed();
		
		assertIsSatisfied();
	}
	
	public void testFilterSendRPY() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = new MessageStub();
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterSendRPY(with(any(NextFilter.class)), with(same(message)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addFirst(filter);
		}
		
		target.fireFilterSendRPY(message);
		
		assertIsSatisfied();
	}

	
	public void testFilterSendERR() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = new MessageStub();
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterSendERR(with(any(NextFilter.class)), with(same(message)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addFirst(filter);
		}
		
		target.fireFilterSendERR(message);
		
		assertIsSatisfied();
	}
	
	public void testFilterSendANS() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = new MessageStub();
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterSendANS(with(any(NextFilter.class)), with(same(message)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addFirst(filter);
		}
		
		target.fireFilterSendANS(message);
		
		assertIsSatisfied();
	}
	
	public void testFilterSendNUL() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterSendNUL(with(any(NextFilter.class)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addFirst(filter);
		}
		
		target.fireFilterSendNUL();
		
		assertIsSatisfied();
	}
	
	public void testFilterReceivedRPY() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = mock(Message.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterReceivedRPY(with(any(NextFilter.class)), with(same(message)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterReceivedRPY(message);
		
		assertIsSatisfied();
	}
	
	public void testFilterReceivedERR() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = mock(Message.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterReceivedERR(with(any(NextFilter.class)), with(same(message)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterReceivedERR(message);
		
		assertIsSatisfied();
	}
	
	public void testFilterReceivedANS() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		final Message message = mock(Message.class);
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterReceivedANS(with(any(NextFilter.class)), with(same(message)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterReceivedANS(message);
		
		assertIsSatisfied();
	}
	
	public void testFilterReceivedNUL() throws Exception {
		DefaultChannelFilterChain target = new DefaultChannelFilterChain(new HeadFilter(), new TailFilter());
		
		final ChannelFilter[] filters = new ChannelFilter[3];
		
		for (int i = 0; i < filters.length; i++) {
			final ChannelFilter filter = mock(ChannelFilter.class, "filter-" + i);
			filters[i] = filter;
			
			checking(new Expectations() {{
				one(filter).filterReceivedNUL(with(any(NextFilter.class)));
				will(proceed());
				inSequence(sequence);
			}});
			
			target.addLast(filter);
		}
		
		target.fireFilterReceivedNUL();
		
		assertIsSatisfied();
	}
	
	private static Action proceed() {
		return new Action() {
			
			public void describeTo(Description description) {
				description.appendText("invoking next filter");
			}
			
			public Object invoke(Invocation invocation) throws Throwable {
				Method invokedMethod = invocation.getInvokedMethod();
				NextFilter next = (NextFilter) invocation.getParameter(0);
				
				Object[] parameters = new Object[invocation.getParameterCount() - 1];
				for (int i = 0; i < parameters.length; i++) {
					parameters[i] = invocation.getParameter(i + 1);
				}
				
				Class<?>[] parameterTypes = new Class[parameters.length];
				for (int i = 0; i < parameters.length; i++) {
					parameterTypes[i] = invokedMethod.getParameterTypes()[i + 1];
				}
				
				Method method = next.getClass().getMethod(invokedMethod.getName(), parameterTypes);
				return method.invoke(next, parameters);
			}
		};
	}
	
	private static class HeadFilter implements ChannelFilter {

		public void filterChannelOpened(NextFilter next, Channel channel) {
			next.filterChannelOpened(channel);
		}

		public void filterMessageReceived(NextFilter next, Message message, Reply reply) {
			next.filterMessageReceived(message, reply);
		}
		
		public void filterChannelCloseRequested(NextFilter next, CloseChannelRequest request) {
			next.filterChannelCloseRequested(request);
		}
		
		public void filterChannelClosed(NextFilter next) {
			next.filterChannelClosed();
		}

		public void filterReceivedRPY(NextFilter next, Message message) {
			next.filterReceivedRPY(message);
		}
		
		public void filterReceivedERR(NextFilter next, Message message) {
			next.filterReceivedERR(message);
		}
		
		public void filterReceivedANS(NextFilter next, Message message) {
			next.filterReceivedANS(message);
		}
		
		public void filterReceivedNUL(NextFilter next) {
			next.filterReceivedNUL();
		}

		public void filterSendMessage(NextFilter next, Message message, ReplyHandler replyHandler) {
			// terminate
		}
		
		public void filterClose(NextFilter next, CloseChannelCallback callback) {
			// terminate
		}
		
		public void filterSendRPY(NextFilter next, Message message) {
			// terminate
		}
		
		public void filterSendERR(NextFilter next, Message message) {
			// terminate
		}
		
		public void filterSendANS(NextFilter next, Message message) {
			// terminate
		}
		
		public void filterSendNUL(NextFilter next) {
			// terminate
		}
		
	}
	
	private static class TailFilter implements ChannelFilter {
		
		public void filterChannelOpened(NextFilter next, Channel channel) {
			// terminate
		}
		
		public void filterMessageReceived(NextFilter next, Message message, Reply reply) {
			// terminate
		}
		
		public void filterChannelCloseRequested(NextFilter next, CloseChannelRequest request) {
			// terminate
		}
		
		public void filterChannelClosed(NextFilter next) {
			// terminate
		}
		
		public void filterReceivedRPY(NextFilter next, Message message) {
			// terminate
		}
		
		public void filterReceivedERR(NextFilter next, Message message) {
			// terminate
		}
		
		public void filterReceivedANS(NextFilter next, Message message) {
			// terminate
		}
		
		public void filterReceivedNUL(NextFilter next) {
			// terminate
		}
		
		public void filterSendMessage(NextFilter next, Message message, ReplyHandler replyHandler) {
			next.filterSendMessage(message, replyHandler);			
		}
		
		public void filterClose(NextFilter next, CloseChannelCallback callback) {
			next.filterClose(callback);
		}
		
		public void filterSendRPY(NextFilter next, Message message) {
			next.filterSendRPY(message);
		}
		
		public void filterSendERR(NextFilter next, Message message) {
			next.filterSendERR(message);
		}
		
		public void filterSendANS(NextFilter next, Message message) {
			next.filterSendANS(message);
		}
		
		public void filterSendNUL(NextFilter next) {
			next.filterSendNUL();
		}
		
	}
	
}
