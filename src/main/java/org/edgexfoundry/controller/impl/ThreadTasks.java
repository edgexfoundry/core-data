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

package org.edgexfoundry.controller.impl;

import java.util.Calendar;

import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.controller.DeviceServiceClient;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.messaging.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Separate class to allow Spring async methods to start a separate thread to complete.
 * 
 */
@Component
public class ThreadTasks {

  private static final org.edgexfoundry.support.logging.client.EdgeXLogger logger =
      org.edgexfoundry.support.logging.client.EdgeXLoggerFactory.getEdgeXLogger(ThreadTasks.class);

  @Value("${addto.event.queue}")
  private boolean addToEventQ;

  @Value("${device.update.lastconnected}")
  private boolean updateDeviceLastReported;

  @Value("${service.update.lastconnected}")
  private boolean updateServiceLastReported;

  @Autowired
  EventPublisher eventProducer;

  @Autowired
  DeviceClient deviceClient;

  @Autowired
  DeviceServiceClient serviceClient;

  @Async()
  public void updateDeviceLastReportedConnected(String deviceid) {
    if (!updateDeviceLastReported) {
      logger.debug("Skipping update of device connected/reported times for:  " + deviceid);
      return;
    }
    try {
      Device device = deviceClient.deviceForName(deviceid);
      if (device == null) {
        device = deviceClient.device(deviceid);
      }
      if (device != null) {
        Calendar calendar = Calendar.getInstance();
        long time = calendar.getTimeInMillis();
        deviceClient.updateLastConnected(device.getId(), time);
        deviceClient.updateLastReported(device.getId(), time);
      } else
        logger.error(
            "Error updating device connected/reported times.  Unknown device with identifier of:  "
                + deviceid);
    } catch (Exception e) {
      logger.error("Error updating device reported/connected times for: " + deviceid + "  "
          + e.getMessage());
    }
  }

  @Async
  public void updateDeviceServiceLastReportedConnected(String deviceid) {
    if (!updateServiceLastReported) {
      logger.debug("Skipping update of device service connected/reported times for:  " + deviceid);
      return;
    }
    try {
      Device device = deviceClient.deviceForName(deviceid);
      if (device == null) {
        device = deviceClient.device(deviceid);
      }
      if (device != null) {
        Calendar calendar = Calendar.getInstance();
        long time = calendar.getTimeInMillis();
        DeviceService service = device.getService();
        if (service != null) {
          serviceClient.updateLastConnected(service.getId(), time);
          serviceClient.updateLastReported(service.getId(), time);
        } else
          logger.error(
              "Error updating device service connected/reported times.  Unknown device service in device:  "
                  + device.getId());
      } else
        logger.error(
            "Error updating device connected/reported times.  Unknown device with identifier of:  "
                + deviceid);
    } catch (Exception e) {
      logger.error("Error updating device service reported/connected times for: " + deviceid + "  ("
          + e.getMessage() + ")");
    }

  }

  // put the new event on the message queue to be processed by the rules
  // engine
  @Async
  public void putEventOnQueue(Event event) {
    if (addToEventQ) {
      try {
        eventProducer.sendEventMessage(event);
      } catch (Exception e) {
        logger.error("Event not queued!!  Check message queue.  Problem queueing event:  " + event);
      }
    }
  }

}
