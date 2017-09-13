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

package org.edgexfoundry.controller;

import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.edgexfoundry.controller.impl.ThreadTasks;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.messaging.EventPublisher;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.DeviceData;
import org.edgexfoundry.test.data.EventData;
import org.edgexfoundry.test.data.ServiceData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Category(RequiresNone.class)
public class ThreadTasksTest {

  private static final String TEST_ID = "123";

  @InjectMocks
  private ThreadTasks tasks;

  @Mock
  EventPublisher eventProducer;

  @Mock
  DeviceClient deviceClient;

  @Mock
  DeviceServiceClient serviceClient;

  private Device device;
  private DeviceService service;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    device = DeviceData.newTestInstance();
    service = ServiceData.newTestInstance();
    device.setService(service);
    setUpdateDeviceLastReported(true);
    setUpdateServiceLastReported(true);
  }

  @Test
  public void testUpdateDeviceServiceLastReportedConnected() {
    when(deviceClient.deviceForName(DeviceData.TEST_NAME)).thenReturn(device);
    tasks.updateDeviceServiceLastReportedConnected(DeviceData.TEST_NAME);
  }

  @Test
  public void testUpdateDeviceServiceLastReportedConnectedWithNoService() {
    device.setService(null);
    when(deviceClient.deviceForName(DeviceData.TEST_NAME)).thenReturn(device);
    tasks.updateDeviceServiceLastReportedConnected(DeviceData.TEST_NAME);
  }

  @Test
  public void testUpdateDeviceServiceLastReportedConnectedById() {
    when(deviceClient.device(TEST_ID)).thenReturn(device);
    tasks.updateDeviceServiceLastReportedConnected(TEST_ID);
  }

  @Test
  public void testUpdateDeviceServiceLastReportedConnectedUnknownDevice() {
    when(deviceClient.device(TEST_ID)).thenReturn(null);
    when(deviceClient.device(TEST_ID)).thenReturn(null);
    tasks.updateDeviceServiceLastReportedConnected(TEST_ID);
  }

  @Test
  public void testUpdateDeviceServiceLastReportedConnectedException() {
    when(deviceClient.deviceForName(TEST_ID)).thenThrow(new RuntimeException());
    tasks.updateDeviceServiceLastReportedConnected(TEST_ID);
  }

  @Test
  public void testUpdateDeviceLastReportedConnected() {
    when(deviceClient.deviceForName(DeviceData.TEST_NAME)).thenReturn(device);
    tasks.updateDeviceLastReportedConnected(DeviceData.TEST_NAME);
  }

  @Test
  public void testUpdateDeviceLastReportedConnectedById() {
    when(deviceClient.device(TEST_ID)).thenReturn(device);
    tasks.updateDeviceLastReportedConnected(TEST_ID);
  }

  @Test
  public void testUpdateDeviceLastReportedConnectedUnknownDevice() {
    when(deviceClient.device(TEST_ID)).thenReturn(null);
    when(deviceClient.device(TEST_ID)).thenReturn(null);
    tasks.updateDeviceLastReportedConnected(TEST_ID);
  }

  @Test
  public void testUpdateDeviceLastReportedConnectedException() {
    when(deviceClient.deviceForName(TEST_ID)).thenThrow(new RuntimeException());
    tasks.updateDeviceLastReportedConnected(TEST_ID);
  }

  @Test
  public void testPutEventOnQueue() throws Exception {
    setAddToEventQ(true);
    Event event = EventData.newTestInstance();
    tasks.putEventOnQueue(event);
  }

  private void setUpdateDeviceLastReported(boolean newUpdDevLastRpt) throws Exception {
    Class<?> clazz = tasks.getClass();
    Field temp = clazz.getDeclaredField("updateDeviceLastReported");
    temp.setAccessible(true);
    temp.set(tasks, newUpdDevLastRpt);
  }

  private void setUpdateServiceLastReported(boolean newUpdSrvLastRpt) throws Exception {
    Class<?> clazz = tasks.getClass();
    Field temp = clazz.getDeclaredField("updateServiceLastReported");
    temp.setAccessible(true);
    temp.set(tasks, newUpdSrvLastRpt);
  }

  private void setAddToEventQ(boolean newAddToEventQ) throws Exception {
    Class<?> clazz = tasks.getClass();
    Field temp = clazz.getDeclaredField("addToEventQ");
    temp.setAccessible(true);
    temp.set(tasks, newAddToEventQ);
  }


}
