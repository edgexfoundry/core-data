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

package org.edgexfoundry.controller.integration;

import static org.edgexfoundry.test.data.CommonData.TEST_ORIGIN;
import static org.edgexfoundry.test.data.EventData.TEST_DEVICE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.impl.EventControllerImpl;
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
import org.edgexfoundry.test.category.RequiresMetaDataRunning;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.CommonData;
import org.edgexfoundry.test.data.EventData;
import org.edgexfoundry.test.data.ReadingData;
import org.edgexfoundry.test.data.ValueDescriptorData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class,
    RequiresMetaDataRunning.class})
public class EventControllerTest {

  private static final String MAXLIMIT = "maxLimit";

  @Autowired
  EventControllerImpl controller;

  @Autowired
  EventRepository repos;

  @Autowired
  ReadingRepository readingRepos;

  @Autowired
  ValueDescriptorRepository valDescRepos;

  @Autowired
  MongoTemplate template;

  @Autowired
  ScrubDao scrubDao;

  private String testEventId;
  private String testReadingId;

  // TODO - need tests to test device client calls

  @Before
  public void setup() {
    ValueDescriptor valDesc = ValueDescriptorData.newTestInstance();
    valDescRepos.save(valDesc);
    Reading reading = ReadingData.newTestInstance();
    readingRepos.save(reading);
    testReadingId = reading.getId();
    assertNotNull("Saved Reading does not have an id", testReadingId);
    List<Reading> readings = new ArrayList<Reading>();
    readings.add(reading);
    Event event = EventData.newTestInstance();
    event.setReadings(readings);
    event.setOrigin(TEST_ORIGIN);
    assertTrue("Reading device not the same as Event device or not set at all",
        reading.getDevice().equals(event.getDevice()));
    repos.save(event);
    testEventId = event.getId();
    assertNotNull("Saved Event does not have an id", testEventId);
  }

  @After
  public void cleanup() throws Exception {
    resetControllerScrubberDao();
    resetControllerRepos();
    resetControllerMAXLIMIT();
    repos.deleteAll();
    readingRepos.deleteAll();
    valDescRepos.deleteAll();
    assertNull("Event not deleted as part of cleanup", repos.findOne(testEventId));
  }

  @Test
  public void testEvent() {
    Event event = controller.event(testEventId);
    EventData.checkTestData(event, testEventId);
    ReadingData.checkTestData(event.getReadings().get(0), testReadingId);
  }

  @Test(expected = NotFoundException.class)
  public void testEventWithUnknownnId() {
    controller.event("nosuchid");
  }

  @Test(expected = ServiceException.class)
  public void testEventException() throws Exception {
    unsetControllerRepos();
    controller.event(testEventId);
  }

  @Test
  public void testEvents() {
    List<Event> events = controller.events();
    assertEquals("Find all not returning a list with one event", 1, events.size());
    EventData.checkTestData(events.get(0), testEventId);
    ReadingData.checkTestData(events.get(0).getReadings().get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testEventsException() throws Exception {
    unsetControllerRepos();
    controller.events();
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsMaxLimitExceeded() throws Exception {
    unsetControllerMaxLimit();
    controller.events();
  }

  @Test
  public void testEventCount() {
    assertEquals("Count of events does not match what is in the repository", 1L,
        controller.eventCount());
  }

  @Test
  public void testEventCountForDevice() {
    assertEquals("Count of events does not match what is in the repository", 1L,
        controller.eventCountForDevice(TEST_DEVICE_ID));
  }

  @Test
  public void testEventCountForDeviceNotFound() {
    assertEquals("Count of events should be zero for unknown device", 0L,
        controller.eventCountForDevice("baddeviceid"));
  }

  @Test
  public void testEventsByDevice() {
    List<Event> events = controller.eventsForDevice(TEST_DEVICE_ID, 10);
    assertEquals("Find by device not returning a list with one event", 1, events.size());
    EventData.checkTestData(events.get(0), testEventId);
    ReadingData.checkTestData(events.get(0).getReadings().get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testEventsByDeviceException() throws Exception {
    unsetControllerRepos();
    controller.eventsForDevice(TEST_DEVICE_ID, 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsByDeviceMaxLimitExceeded() throws Exception {
    unsetControllerMaxLimit();
    controller.eventsForDevice(TEST_DEVICE_ID, 10);
  }

  @Test
  public void testEventsByTime() {
    long now = new Date().getTime();
    // between yesterday and tomorrow
    List<Event> events = controller.events(now - 86400000, now + 86400000, 10);
    assertEquals("Find by start and end time not returning a list with one event", 1,
        events.size());
    EventData.checkTestData(events.get(0), testEventId);
    ReadingData.checkTestData(events.get(0).getReadings().get(0), testReadingId);
  }

  @Test
  public void testEventsByTimeWithNoneMatching() {
    long now = new Date().getTime();
    // between beginning of time and yesterday
    List<Event> events = controller.events(0, now - 86400000, 10);
    assertEquals(
        "Find by start and end time with none in that period is still returning a list with one event",
        0, events.size());
  }

  @Test(expected = ServiceException.class)
  public void testEventsByTimeException() throws Exception {
    unsetControllerRepos();
    long now = new Date().getTime();
    // between yesterday and tomorrow
    controller.events(now - 86400000, now + 86400000, 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testEventsByTimeMaxLimitExceeded() throws Exception {
    unsetControllerMaxLimit();
    long now = new Date().getTime();
    // between yesterday and tomorrow
    controller.events(now - 86400000, now + 86400000, 10);
  }

  @Test
  public void testReadingsForDeviceAndValueDescriptor() {
    List<Reading> readings =
        controller.readingsForDeviceAndValueDescriptor(TEST_DEVICE_ID, ReadingData.TEST_NAME, 10);
    assertEquals(
        "Find readings by device id/name and value descriptor - through event - is not returning the correct results",
        1, readings.size());
    ReadingData.checkTestData(readings.get(0), testReadingId);
  }

  @Test
  public void testAdd() {
    Reading reading2 = new Reading(ReadingData.TEST_NAME, ReadingData.TEST_VALUE);
    List<Reading> readings = new ArrayList<Reading>();
    readings.add(reading2);
    Event event2 = new Event(EventData.TEST_DEVICE_ID, readings);
    event2.setOrigin(TEST_ORIGIN);
    assertNotNull("New event id is null", controller.add(event2));
    assertNotNull("Event ID is not present", event2.getId());
    assertNotNull("Event modified date is null", event2.getModified());
    assertNotNull("Event create date is null", event2.getCreated());
  }

  @Test(expected = DataValidationException.class)
  public void testAddWithBadReadingName() {
    Reading reading2 = new Reading("unknownid", ReadingData.TEST_VALUE);
    List<Reading> readings = new ArrayList<Reading>();
    readings.add(reading2);
    Event event2 = new Event(EventData.TEST_DEVICE_ID, readings);
    event2.setOrigin(TEST_ORIGIN);
    controller.add(event2);
  }

  @Test(expected = DataValidationException.class)
  public void testAddWithNoDevice() {
    Reading reading2 = new Reading(ReadingData.TEST_NAME, ReadingData.TEST_VALUE);
    List<Reading> readings = new ArrayList<Reading>();
    readings.add(reading2);
    Event event2 = new Event(null, readings);
    event2.setOrigin(TEST_ORIGIN);
    controller.add(event2);

  }

  @Test(expected = ServiceException.class)
  public void testAddException() throws Exception {
    unsetControllerRepos();
    Reading reading2 = new Reading(ReadingData.TEST_NAME, ReadingData.TEST_VALUE);
    List<Reading> readings = new ArrayList<Reading>();
    readings.add(reading2);
    Event event2 = new Event(EventData.TEST_DEVICE_ID, readings);
    controller.add(event2);
  }

  @Test
  public void testDelete() {
    assertTrue("Event was not deleted by the controller", controller.delete(testEventId));
  }

  @Test(expected = ServiceException.class)
  public void testDeleteException() throws Exception {
    unsetControllerRepos();
    controller.delete(testEventId);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteWithUnknownId() {
    controller.delete("unknownid");
  }

  @Test
  public void testDeleteByDevice() {
    assertEquals("Event was not deleted given a device id by the controller", 1,
        controller.deleteByDevice(EventData.TEST_DEVICE_ID));
  }

  @Test(expected = ServiceException.class)
  public void testDeleteByDeviceException() throws Exception {
    unsetControllerRepos();
    controller.deleteByDevice(EventData.TEST_DEVICE_ID);
  }

  @Test
  public void testUpdate() {
    Event event2 = new Event(EventData.TEST_DEVICE_ID);
    event2.setId(testEventId);
    event2.setOrigin(1234);
    assertTrue("Event controller unable to update event", controller.update(event2));

    Event event = repos.findOne(testEventId);
    assertEquals("ID doesn't match expected event id", testEventId, event.getId());
    assertEquals("Event device does not match saved device", EventData.TEST_DEVICE_ID,
        event.getDevice());
    assertEquals("Event pushed does not match saved", 0, event.getPushed());
    assertEquals("Event readings list size is bigger than one which does not match saved list", 1,
        event.getReadings().size());
    assertEquals("Event origin does not match saved origin", 1234, event.getOrigin());
    assertNotNull("Event modified date is null", event.getModified());
    assertNotNull("Event create date is null", event.getCreated());
    assertTrue(event.getModified() != event.getCreated());
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateWithUnknownId() {
    Event event = new Event(EventData.TEST_DEVICE_ID);
    event.setId("unknownid");
    controller.update(event);
  }

  @Test(expected = ServiceException.class)
  public void testUpdateException() throws Exception {
    unsetControllerRepos();
    Event event = new Event(EventData.TEST_DEVICE_ID);
    event.setId(testEventId);
    controller.update(event);
  }

  @Test
  public void testMarkPushed() {
    assertTrue("Event not successfully marked pushed", controller.markPushed(testEventId));
    Event event = controller.event(testEventId);
    // does the event pushed match the reading pushed (from the database)
    ReadingData.checkTestData(event.getReadings().get(0), testReadingId, ReadingData.TEST_NAME,
        ReadingData.TEST_VALUE, event.getPushed(), event.getDevice(), CommonData.TEST_ORIGIN,
        !CommonData.MATCHING_TIMESTAMPS);
  }

  @Test(expected = NotFoundException.class)
  public void testMarkPushedWithUnknownId() {
    controller.markPushed("unknowneventid");
  }

  @Test(expected = ServiceException.class)
  public void testMarkPushedException() throws Exception {
    unsetControllerRepos();
    controller.markPushed(testEventId);
  }

  @Test
  public void testScrubPushedEvents() {
    Event event = controller.event(testEventId);
    event.setPushed(123);
    assertTrue("Event pushed not set for test", controller.update(event));

    assertEquals("Scrub of events did not scrub properly", 1, controller.scrubPushedEvents());
  }

  @Test
  public void testScrubPushedEventsWithNoEventsToScrub() {
    assertEquals("Scrubbed events when none were supposed to be found", 0,
        controller.scrubPushedEvents());
  }

  @Test(expected = ServiceException.class)
  public void testScrubPushedEventsException() throws Exception {
    unsetControllerScrubberDao();
    controller.scrubPushedEvents();
  }

  @Test
  public void testScrubOldEvents() throws InterruptedException {
    Thread.sleep(2000); // allow record to age so it can be removed
    assertEquals("Old events not scrubbed properly", 1, controller.scrubOldEvents(1));
  }

  @Test
  public void testScrubOldEventsWithNoEventsToScrub() {
    long now = new Date().getTime();
    assertEquals("Found old events to scrub when none were supposed to be found", 0,
        controller.scrubOldEvents(now + 2000));
  }

  @Test(expected = ServiceException.class)
  public void testScrubOldEventsException() throws Exception {
    unsetControllerScrubberDao();
    long now = new Date().getTime();
    // scrub events older than now + 2 seconds
    controller.scrubOldEvents(now + 2000);
  }

  // use Java reflection to unset controller's repos
  private void unsetControllerRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("eventRepos");
    temp.setAccessible(true);
    temp.set(controller, null);
  }

  // use Java reflection to reset controller's repos
  private void resetControllerRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("eventRepos");
    temp.setAccessible(true);
    temp.set(controller, repos);
  }

  // use Java reflection to unset controller's tempalte
  private void unsetControllerMaxLimit() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(MAXLIMIT);
    temp.setAccessible(true);
    temp.set(controller, 0);
  }

  // use Java reflection to reset controller's template
  private void resetControllerMAXLIMIT() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(MAXLIMIT);
    temp.setAccessible(true);
    temp.set(controller, 1000);
  }

  // use Java reflection to unset controller's tempalte
  private void unsetControllerScrubberDao() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("scrubDao");
    temp.setAccessible(true);
    temp.set(controller, null);
  }

  // use Java reflection to reset controller's template
  private void resetControllerScrubberDao() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("scrubDao");
    temp.setAccessible(true);
    temp.set(controller, scrubDao);
  }

}
