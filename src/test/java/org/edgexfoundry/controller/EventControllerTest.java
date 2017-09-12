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

import static org.edgexfoundry.test.data.EventData.TEST_DEVICE_ID;
import static org.edgexfoundry.test.data.EventData.newTestInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.edgexfoundry.controller.impl.EventControllerImpl;
import org.edgexfoundry.controller.impl.ThreadTasks;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ScrubDao;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.EventData;
import org.edgexfoundry.test.data.ReadingData;
import org.edgexfoundry.test.data.ValueDescriptorData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

@Category(RequiresNone.class)
public class EventControllerTest {

  private static final String LIMIT_PROPERTY = "maxLimit";

  private static final String META_CHECK_PROPERTY = "metaCheck";

  private static final String PERSIST_DATA_PROPERTY = "persistData";

  private static final int MAX_LIMIT = 100;

  private static final String TEST_ID = "123";

  private static final String TEST_ERR_MSG = "test message";

  @InjectMocks
  private EventControllerImpl controller;

  @Mock
  private ReadingRepository readingRepos;

  @Mock
  private EventRepository eventRepos;

  @Mock
  private ValueDescriptorRepository valDescRepos;

  @Mock
  private ScrubDao scrubDao;

  @Mock
  private ThreadTasks tasker;

  @Mock
  private DeviceClient deviceClient;

  private Event event;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    event = newTestInstance();
    event.setId(TEST_ID);
    setControllerMaxLimit(MAX_LIMIT);
  }

  @Test
  public void testEvent() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    EventData.checkTestDataWithoutReadings(controller.event(TEST_ID), TEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testEventWithEventNotFound() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(null);
    controller.event(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testEventException() {
    when(eventRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.event(TEST_ID);
  }

  @Test
  public void testEvents() {
    List<Event> evts = new ArrayList<>();
    evts.add(event);
    when(eventRepos.findAll(any(Sort.class))).thenReturn(evts);
    List<Event> events = controller.events();
    assertEquals("Find all not returning a list with one event", 1, events.size());
    EventData.checkTestDataWithoutReadings(events.get(0), TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testEventsException() {
    when(eventRepos.findAll(any(Sort.class))).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.events();
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsMaxLimitExceeded() {
    when(eventRepos.count()).thenReturn(1000L);
    controller.events();
  }

  @Test
  public void testEventCountForDevice() {
    List<Event> evts = new ArrayList<>();
    evts.add(event);
    when(eventRepos.findByDevice(TEST_DEVICE_ID)).thenReturn(evts);
    assertEquals("Count of events does not match what is in the repository", 1L,
        controller.eventCountForDevice(TEST_DEVICE_ID));
  }

  @Test
  public void testEventCountForDeviceNotFound() {
    List<Event> evts = new ArrayList<>();
    when(eventRepos.findByDevice("baddeviceid")).thenReturn(evts);
    assertEquals("Count of events should be zero for unknown device", 0L,
        controller.eventCountForDevice("baddeviceid"));
  }

  @Test(expected = ServiceException.class)
  public void testEventCountForDeviceException() {
    when(eventRepos.findByDevice(TEST_DEVICE_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.eventCountForDevice(TEST_DEVICE_ID);
  }

  @Test
  public void testEventCount() {
    when(eventRepos.count()).thenReturn(1L);
    assertEquals("Count of events does not match what is in the repository", 1L,
        controller.eventCount());
  }

  @Test(expected = ServiceException.class)
  public void testEventCountException() {
    when(eventRepos.count()).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.eventCount();
  }

  /**
   * The Page<Event> object makes coverage testing of the eventsForX methods impossible. There is no
   * easy way to get back a PageRequest object from the mock object.
   */

  @Test(expected = ServiceException.class)
  public void testEventsForDeviceException() {
    when(eventRepos.findByDevice(anyObject(), anyObject()))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.eventsForDevice(TEST_DEVICE_ID, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsForDeviceMaxLimitExceeded() {
    controller.eventsForDevice(TEST_DEVICE_ID, 1000);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsForDeviceAndValueDescriptorException() {
    when(eventRepos.findByDevice(anyObject(), anyObject()))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readingsForDeviceAndValueDescriptor(TEST_DEVICE_ID, TEST_ID, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsForDeviceAndValueDescriptorMaxLimitExceeded() {
    controller.readingsForDeviceAndValueDescriptor(TEST_DEVICE_ID, TEST_ID, 1000);
  }

  @Test(expected = ServiceException.class)
  public void testEventsForTimeException() {
    when(eventRepos.findByCreatedBetween(anyLong(), anyLong(), anyObject()))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.events(0, Long.MAX_VALUE, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsForTimeMaxLimitExceeded() {
    controller.events(0, Long.MAX_VALUE, 1000);
  }

  @Test(expected = DataValidationException.class)
  public void testCheckDeviceThroughAddWithNullDeviceId() {
    event.setDevice(null);
    controller.add(event);
  }

  @Test(expected = NotFoundException.class)
  public void testCheckDeviceThroughAddWithDeviceNotFound() throws Exception {
    setMetaCheck(true);
    when(deviceClient.deviceForName(TEST_DEVICE_ID)).thenReturn(null);
    controller.add(event);
  }

  @Test(expected = ServiceException.class)
  public void testCheckDeviceThroughAddWithUnknownException() throws Exception {
    setMetaCheck(true);
    when(deviceClient.deviceForName(TEST_DEVICE_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.add(event);
  }

  @Test
  public void testAddNoReadings() {
    when(eventRepos.save(event)).thenReturn(event);
    assertEquals("Event id returned does not match expected", TEST_ID, controller.add(event));
  }

  @Test
  public void testAddWithReadings() {
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    event.setReadings(readings);
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(eventRepos.save(event)).thenReturn(event);
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(valueDescriptor);
    assertEquals("Event id returned does not match expected", TEST_ID, controller.add(event));
  }

  @Test(expected = DataValidationException.class)
  public void testAddWithReadingsBadValueDescriptor() {
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    event.setReadings(readings);
    when(eventRepos.save(event)).thenReturn(event);
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(null);
    controller.add(event);
  }

  @Test(expected = ServiceException.class)
  public void testAddNoReadingsException() {
    when(eventRepos.save(event)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.add(event);
  }

  @Test
  public void testAddWithPersistDataOff() throws Exception {
    setPersistData(false);
    assertEquals("Event id returned does not match expected", "unsaved", controller.add(event));
  }

  @Test
  public void testDelete() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    assertTrue("Event was not deleted by the controller", controller.delete(TEST_ID));
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteEventNotFound() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(null);
    controller.delete(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testDeleteEventException() {
    when(eventRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.delete(TEST_ID);
  }

  @Test
  public void testDeleteByDevice() {
    List<Event> evts = new ArrayList<>();
    evts.add(event);
    when(eventRepos.findByDevice(TEST_DEVICE_ID)).thenReturn(evts);
    assertEquals("Event was not deleted given a device id by the controller", 1,
        controller.deleteByDevice(TEST_DEVICE_ID));
  }

  @Test(expected = ServiceException.class)
  public void testDeleteByDeviceException() throws Exception {
    when(eventRepos.findByDevice(TEST_DEVICE_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.deleteByDevice(EventData.TEST_DEVICE_ID);
  }

  @Test
  public void testUpdate() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    when(eventRepos.save(event)).thenReturn(event);
    assertTrue("Event controller unable to update event", controller.update(event));
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateEventNotFound() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(null);
    controller.update(event);
  }

  @Test(expected = ServiceException.class)
  public void testUpdateException() {
    when(eventRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.update(event);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateDeviceNotFound() throws Exception {
    setMetaCheck(true);
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    when(deviceClient.deviceForName(EventData.TEST_DEVICE_ID)).thenReturn(null);
    controller.update(event);
  }

  @Test
  public void testMarkPushed() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    when(eventRepos.save(event)).thenReturn(event);
    assertTrue("Event not successfully marked pushed", controller.markPushed(TEST_ID));
  }

  @Test(expected = NotFoundException.class)
  public void testMarkPushedEventNotFound() {
    when(eventRepos.findOne(TEST_ID)).thenReturn(null);
    controller.markPushed(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testMarkedPushedException() {
    when(eventRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.markPushed(TEST_ID);
  }

  @Test
  public void testMarkPushedWithReadings() {
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    event.setReadings(readings);
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(valueDescriptor);
    assertTrue("Event not successfully marked pushed", controller.markPushed(TEST_ID));
  }

  @Test(expected = ServiceException.class)
  public void testMarkPushedWithReadingsBadValueDescriptor() {
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    event.setReadings(readings);
    when(eventRepos.findOne(TEST_ID)).thenReturn(event);
    when(valDescRepos.findByName(reading.getName())).thenReturn(null);
    controller.markPushed(TEST_ID);
  }

  @Test
  public void testScrubbedPushedEvents() {
    when(scrubDao.scrubPushedEvents()).thenReturn(1);
    assertEquals("Scrub of events did not scrub properly", 1, controller.scrubPushedEvents());
  }

  @Test(expected = ServiceException.class)
  public void testScrubbedPushedEventsException() {
    when(scrubDao.scrubPushedEvents()).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.scrubPushedEvents();
  }

  @Test
  public void testScrubAllEventsReadings() {
    assertTrue("Scrub of all event readings did not occur properly",
        controller.scrubAllEventsReadings());
  }

  @Test
  public void testScrubOldEvents() {
    when(scrubDao.scrubOldEvents(anyLong())).thenReturn(1);
    assertEquals("Old events not scrubbed properly", 1, controller.scrubOldEvents(1));
  }

  @Test(expected = ServiceException.class)
  public void testScrubOldEventsException() {
    when(scrubDao.scrubOldEvents(anyLong())).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.scrubOldEvents(1);
  }

  private void setControllerMaxLimit(int newLimit) throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(LIMIT_PROPERTY);
    temp.setAccessible(true);
    temp.set(controller, newLimit);
  }

  private void setMetaCheck(boolean newMetaCheck) throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(META_CHECK_PROPERTY);
    temp.setAccessible(true);
    temp.set(controller, newMetaCheck);
  }

  private void setPersistData(boolean newPersistData) throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(PERSIST_DATA_PROPERTY);
    temp.setAccessible(true);
    temp.set(controller, newPersistData);
  }
}
