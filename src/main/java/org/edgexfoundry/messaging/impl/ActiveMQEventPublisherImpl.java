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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.messaging.EventPublisher;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * Note - this message publisher is not used by default configuration. To use this publisher, you
 * need to change the spring-config.xml file to use this publisher versus the ZeroMQ publisher.
 * 
 */
public class ActiveMQEventPublisherImpl implements EventPublisher {

  private static final org.edgexfoundry.support.logging.client.EdgeXLogger logger =
      org.edgexfoundry.support.logging.client.EdgeXLoggerFactory
          .getEdgeXLogger(ActiveMQEventPublisherImpl.class);

  private JmsTemplate template;

  /**
   * Send a message containing the serialized Event (and Readings) into an Active MQ queue to allow
   * a rules engine or other service to act on new sensor/device readings.
   * 
   * @param event - the Event object (with embedded Readings) to be placed in the queue.
   */
  public void sendEventMessage(final Event event) {
    template.send(new MessageCreator() {
      public Message createMessage(Session session) throws JMSException {
        ObjectMessage message = session.createObjectMessage(event);
        logger.info("Sent event/readings with id:  " + event.getId());
        return message;
      }
    });
  }

  /**
   * Get the Spring JMS Template object used to communicate with the ActiveMQ queue broker
   * 
   * @return - the Spring JMS Template
   */
  public JmsTemplate getTemplate() {
    return template;
  }

  /**
   * Set the Spring JMS Template object used to communicate with the ActiveMQ queue broker. Used by
   * the Spring DI engine.
   * 
   * @param template - the Spring JMS Template
   */
  public void setTemplate(JmsTemplate template) {
    this.template = template;
  }

}
