/*******************************************************************************
 * Copyright 2016-2017 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * @microservice: core-data
 * @author: Jim White, Dell
 * @version: 1.0.0
 *******************************************************************************/

package org.edgexfoundry.messaging.impl;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;

import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.messaging.EventPublisher;
import org.zeromq.ZMQ;

public class ZeroMQEventPublisherImpl implements EventPublisher {

  private static final org.edgexfoundry.support.logging.client.EdgeXLogger logger =
      org.edgexfoundry.support.logging.client.EdgeXLoggerFactory
          .getEdgeXLogger(ZeroMQEventPublisherImpl.class);

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
        publisher.send(toByteArray(event));
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
   * Encode the event as JSON and send to a byte array
   * 
   * @param Event
   * @return JSON encoded byte array
   * @throws IOException
   */
  private byte[] toByteArray(Event event) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bos)) {
      Gson gson = new Gson();
      String eventString = gson.toJson(event);
      out.write(eventString.getBytes());
      return bos.toByteArray();
    }
  }

}
