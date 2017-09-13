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

package org.edgexfoundry.messaging.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import org.edgexfoundry.domain.core.Event;
import org.zeromq.ZMQ;

/**
 * This is not a test class!!
 * 
 * Use this class to hook up to and see events coming through the ZeroMQ topic(s) during development
 * or test.
 * 
 */
public class MessageTestClient {

  private static Event toEvent(byte[] eventBytes) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bis = new ByteArrayInputStream(eventBytes);
    ObjectInput in = new ObjectInputStream(bis);
    Event event = (Event) in.readObject();
    return event;
  }

  public static void main(String[] args)
      throws ClassNotFoundException, IOException, InterruptedException {
    ZMQ.Context context = ZMQ.context(1);
    System.out.println("Watching for events from event topic");
    ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
    subscriber.connect("tcp://localhost:5563");
    subscriber.subscribe("".getBytes());
    Event event = null;
    while (!Thread.currentThread().isInterrupted()) {
      event = toEvent(subscriber.recv());
      System.out.println("Event is: " + event);
    }
  }

}
