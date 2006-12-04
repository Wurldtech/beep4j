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
package net.sf.beep4j.internal.tcp;

import junit.framework.TestCase;
import net.sf.beep4j.Message;
import net.sf.beep4j.MessageStub;
import net.sf.beep4j.internal.TransportMapping;
import net.sf.beep4j.transport.Transport;

import org.easymock.MockControl;

public class TCPMappingTest extends TestCase {
	
	private MockControl transportCtrl;
	
	private Transport transport;
	
	private MockControl controllerCtrl;
	
	private ChannelController controller;
	
	private MockControl factoryCtrl;
	
	private ChannelControllerFactory factory;
	
	@Override
	protected void setUp() throws Exception {
		transportCtrl = MockControl.createControl(Transport.class);
		transport = (Transport) transportCtrl.getMock();
		factoryCtrl = MockControl.createControl(ChannelControllerFactory.class);
		factory = (ChannelControllerFactory) factoryCtrl.getMock();
		controllerCtrl = MockControl.createControl(ChannelController.class);
		controller = (ChannelController) controllerCtrl.getMock();
		
		factory.createChannelController(0, transport);
		factoryCtrl.setReturnValue(controller);
	}
	
	private void replay() {
		transportCtrl.replay();
		factoryCtrl.replay();
		controllerCtrl.replay();
	}

	private void verify() {
		transportCtrl.verify();
		factoryCtrl.verify();
		controllerCtrl.verify();
	}
	
	public void testReceiveFrame() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		
		// define expectations
		controller.checkFrame(0, 50);
		controller.frameReceived(0, 50);
		
		replay();
		
		mapping.channelStarted(0);
		mapping.checkFrame(0, 0, 50);
		mapping.frameReceived(0, 0, 50);
		
		verify();
	}
	
	public void testProcessMappingFrame() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory, 50);
		
		// define expectations
		controller.updateSendWindow(0, 4096);
		
		replay();
		
		mapping.channelStarted(0);
		mapping.processMappingFrame("SEQ 0 0 4096".split(" "));
		
		verify();
	}
	
	public void testStartCloseChannel() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		
		// define expectations
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.channelClosed(0);
		
		try {
			mapping.checkFrame(0, 0, 50);
			fail("channel 0 is closed, does not have a controller");
		} catch (Exception e) {
			// expected
			// TODO: catch proper exception
		}
		
		// verify
		verify();
	}
	
	public void testSendANS() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		Message message = new MessageStub();
		
		// define expectations
		controller.sendANS(0, 0, message);
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.sendANS(0, 0, 0, message);
		
		// verify
		verify();
	}

	public void testSendNUL() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		
		// define expectations
		controller.sendNUL(0);
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.sendNUL(0, 0);
		
		// verify
		verify();
	}
	
	public void testSendERR() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		Message message = new MessageStub();
		
		// define expectations
		controller.sendERR(0, message);
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.sendERR(0, 0, message);
		
		// verify
		verify();
	}
	
	public void testSendMSG() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		Message message = new MessageStub();
		
		// define expectations
		controller.sendMSG(0, message);
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.sendMSG(0, 0, message);
		
		// verify
		verify();
	}
	
	public void testSendRPY() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		Message message = new MessageStub();
		
		// define expectations
		controller.sendRPY(0, message);
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.sendRPY(0, 0, message);
		
		// verify
		verify();
	}
	
	public void testCloseTransport() throws Exception {
		TransportMapping mapping = new TCPMapping(transport, factory);
		
		// define expectations
		transport.closeTransport();
		replay();
		
		// test
		mapping.channelStarted(0);
		mapping.closeTransport();
		
		// verify
		verify();
	}
	
}
