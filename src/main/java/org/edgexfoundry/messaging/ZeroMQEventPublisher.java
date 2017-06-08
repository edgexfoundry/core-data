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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.edgexfoundry.domain.core.Event;
import org.zeromq.ZMQ;

/**
 * @author Jim
 *
 */
public class ZeroMQEventPublisher implements EventPublisher {

	// private static final Logger logger =
	// Logger.getLogger(ZeroMQEventPublisher.class);
	private final static org.edgexfoundry.support.logging.client.EdgeXLogger logger = 
			org.edgexfoundry.support.logging.client.EdgeXLoggerFactory.getEdgeXLogger(ZeroMQEventPublisher.class);

	private static final long PUB_UP_SLEEP = 1000;

	private String zeromqAddressPort;

	private ZMQ.Socket publisher;
	private ZMQ.Context context;

	{
		context = ZMQ.context(1);
	}

	// synchronized because zeroMQ sockets are not thread-safe
    @Override
	public synchronized void sendEventMessage(Event event) {
		try {
			if (publisher == null)
				getPublisher();
			if (publisher != null) {
				//logger.error("--->" + event.getId() + " sending@ " + System.currentTimeMillis());
				publisher.send(toByteArray(event));
				//logger.error("--->" + event.getId() + " sent@ " + System.currentTimeMillis());
				logger.debug("Sent event to export with device id:  " + event.getDevice());
			} else
				logger.error("Event not sent to export with id:" + event.getId());
		} catch (Exception e) {
			logger.error("Unable to send message via ZMQ");
		}
	}

	public String getZeromqAddressPort() {
		return zeromqAddressPort;
	}

	public void setZeromqAddressPort(String zeromqAddressPort) {
		this.zeromqAddressPort = zeromqAddressPort;
	}

	private void getPublisher() {
		//logger.error("--->Getting Publisher");
		try {
			if (publisher == null) {
				publisher = context.socket(ZMQ.PUB);
				publisher.bind(zeromqAddressPort);
				// TODO someday change this to make a call to check subscribers and only then release
				Thread.sleep(PUB_UP_SLEEP); // allow subscribers to connect
			}
		} catch (Exception e) {
			logger.error("Unable to get a publisher.  Error:  " + e);
			publisher = null;
		}
	}

	/**
	 * Serialize the event to a byte array
	 * 
	 * @param Event
	 * @return serialized byte array
	 * @throws IOException
	 */
	private byte[] toByteArray(Event event) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ObjectOutput out = new ObjectOutputStream(bos)) {
			out.writeObject(event);
			return bos.toByteArray();
		}
	}

}
