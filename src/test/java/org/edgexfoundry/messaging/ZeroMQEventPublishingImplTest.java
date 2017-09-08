/*******************************************************************************
 * Copyright 2016-2017 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @microservice:  core-data
 * @author: Jim White, Dell
 * @version: 1.0.0
 *******************************************************************************/

package org.edgexfoundry.messaging;

import java.io.IOException;

import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.EventData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeromq.ZMQ;

@Category(RequiresNone.class)
public class ZeroMQEventPublishingTest {

	private ZeroMQEventPublisher publisher;

	private Event event;

	@Before
	public void setup() {
		publisher = new ZeroMQEventPublisher();
		event = new Event(EventData.TEST_DEVICE_ID);
		publisher.setZeromqAddressPort("tcp://*:5563");
	}

	@Test
	public void testSendEvent() throws InterruptedException, ClassNotFoundException, IOException {
		publisher.sendEventMessage(event);
	}

	@Test
	public void testSendEventPortOccupied() throws InterruptedException {
		ZMQ.Context context2 = ZMQ.context(1);
		ZMQ.Socket publisher2 = context2.socket(ZMQ.PUB);
		publisher2.bind("tcp://*:5563");
		publisher.sendEventMessage(event);
		Thread.sleep(3000);
		publisher2.close();
		context2.term();
	}

}
