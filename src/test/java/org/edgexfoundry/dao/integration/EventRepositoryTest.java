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

package org.edgexfoundry.dao.integration;

import static org.edgexfoundry.test.data.CommonData.TEST_ORIGIN;
import static org.edgexfoundry.test.data.CommonData.TEST_PUSHED;
import static org.edgexfoundry.test.data.EventData.TEST_DEVICE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class})
public class EventRepositoryTest {

  @Autowired
  private EventRepository eventRepos;
  private String testEventId;

  /**
   * Create and save an instance of the Event before each test Note: the before method tests the
   * save operation of the Repository
   */
  @Before
  public void creatTestData() {
    Event event = new Event(TEST_DEVICE_ID, null);
    event.setOrigin(TEST_ORIGIN);
    event.setPushed(TEST_PUSHED);
    eventRepos.save(event);
    testEventId = event.getId();
    assertNotNull("new test event has no identifier", testEventId);
  }

  /**
   * Clean up of the unit test Note: clean up also tests the delete operation of the repository
   */
  @After
  public void destroyTestData() {
    eventRepos.delete(testEventId);
    assertNull("deleted test event still exists in the database", eventRepos.findOne(testEventId));
  }

  @Test
  public void testUpdate() {
    Event event = eventRepos.findOne(testEventId);
    // check that create and modified timestamps are the same for a new
    // Event record
    assertEquals("Modified and created timestamps should be equal after creation",
        event.getModified(), event.getCreated());
    event.setDevice("newdeviceid");
    eventRepos.save(event);
    // reread event
    Event event2 = eventRepos.findOne(testEventId);
    assertEquals("device id was not updated appropriately in event update", "newdeviceid",
        event2.getDevice());
    assertNotEquals(
        "after modification, modified timestamp still the same as the event's create timestamp",
        event2.getModified(), event2.getCreated());
  }

  @Test
  public void testFindOne() {
    Event event = eventRepos.findOne(testEventId);
    assertNotNull("Find one returns no events", event);
    checkTestEventData(event);
  }

  @Test
  public void testFindAll() {
    List<Event> events = eventRepos.findAll();
    assertEquals("Find all not returning a list with one event", 1, events.size());
    checkTestEventData(events.get(0));
  }

  @Test
  public void testFindByCreatedBetween() {
    long now = new Date().getTime();
    // between yesterday and tomorrow
    assertEquals("find by created date not returning any events", 1,
        eventRepos.findByCreatedBetween(now - 86400000, now + 86400000).size());
  }

  @Test
  public void testFindByCreatedBetweenWithNoBetween() {
    long now = new Date().getTime();
    // between beginning of time and a year ago
    assertEquals("find by created date with bad dates is returning events", 0,
        eventRepos.findByCreatedBetween(0, now - (86400000 * 365)).size());
  }

  @Test
  public void testFindByModifiedBetween() {
    long now = new Date().getTime();
    // between yesterday and tomorrow
    assertEquals("find by modified date not returning any events", 1,
        eventRepos.findByModifiedBetween(now - 86400000, now + 86400000).size());
  }

  @Test
  public void testFindByModifiedBetweenWithNoBetween() {
    long now = new Date().getTime();
    // between beginning of time and a year ago
    assertEquals("find by modified date with bad dates is returning events", 0,
        eventRepos.findByCreatedBetween(0, now - (86400000 * 365)).size());
  }

  @Test
  public void testFindByOriginBetween() {
    assertEquals("find by origin date not returning any events", 1,
        eventRepos.findByOriginBetween(TEST_ORIGIN - 10, TEST_ORIGIN + 10).size());
  }

  @Test
  public void testFindByOriginBetweenWithNoBetween() {
    assertEquals("find by origin date with bad dates is returning events", 0,
        eventRepos.findByCreatedBetween(0, TEST_ORIGIN - 100).size());
  }

  @Test
  public void testfindByPushedGreaterThan() {
    assertEquals("find by pushed date not returning any events", 1,
        eventRepos.findByPushedGreaterThan(TEST_PUSHED - 10).size());
  }

  @Test
  public void testfindByPushedGreaterThanWithNoGreaterThan() {
    assertEquals("find by pushed date greater than with bad dates is returning events", 0,
        eventRepos.findByPushedGreaterThan(TEST_PUSHED + 10).size());
  }

  @Test
  public void testFindByDevice() {
    List<Event> events = eventRepos.findByDevice(TEST_DEVICE_ID);
    assertEquals("find by device not returning any events", 1, events.size());
    checkTestEventData(events.get(0));
  }

  @Test
  public void testFindByDeviceWithNoMatching() {
    assertEquals("find by device with bad device is returning events", 0,
        eventRepos.findByDevice("nosuchdeviceid").size());
  }

  private void checkTestEventData(Event event) {
    assertEquals("Event ID does not match saved id", testEventId, event.getId());
    assertEquals("Event device does not match saved device", TEST_DEVICE_ID, event.getDevice());
    assertEquals("Event origin does not match saved origin", TEST_ORIGIN, event.getOrigin());
    assertEquals("Event pushed does not match saved pushed date", TEST_PUSHED, event.getPushed());
    assertNotNull("Event modified date is null", event.getModified());
    assertNotNull("Event created date is null", event.getCreated());
  }

}
