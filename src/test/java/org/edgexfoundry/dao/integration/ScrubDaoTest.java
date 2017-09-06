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
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_NAME1;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_NAME2;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_VALUE1;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_VALUE2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ScrubDao;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.core.Reading;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class})
public class ScrubDaoTest {

  @Autowired
  private ScrubDao dao;

  @Autowired
  private EventRepository eventRepos;

  @Autowired
  private ReadingRepository readingRepos;

  @Autowired
  MongoTemplate template;

  private String testEventId;
  private String testReadingId1, testReadingId2;

  @Before
  public void setup() throws Exception {
    injectDAOwithTemplate();
    Event event = new Event(TEST_DEVICE_ID, null);
    event.setOrigin(TEST_ORIGIN);
    Reading reading1 = new Reading(TEST_NAME1, TEST_VALUE1);
    reading1.setOrigin(TEST_ORIGIN);
    event.addReading(reading1);
    Reading reading2 = new Reading(TEST_NAME2, TEST_VALUE2);
    reading2.setOrigin(TEST_ORIGIN);
    event.addReading(reading2);
    event.markPushed(TEST_PUSHED);
    readingRepos.save(reading1);
    testReadingId1 = reading1.getId();
    readingRepos.save(reading2);
    testReadingId2 = reading2.getId();
    eventRepos.save(event);
    testEventId = event.getId();
    assertNotNull("new test event has no identifier", testEventId);
  }

  // use Java reflection to inject the template into the dao since Autowiring
  // isn't handled by Spring in the test
  private void injectDAOwithTemplate() throws Exception {
    Class<?> daoClass = dao.getClass();
    Field temp = daoClass.getDeclaredField("template");
    temp.setAccessible(true);
    temp.set(dao, template);
  }

  @After
  public void cleanup() {
    readingRepos.delete(testReadingId1);
    assertNull("deleted test reading1 still exists in the database",
        readingRepos.findOne(testReadingId1));
    readingRepos.delete(testReadingId2);
    assertNull("deleted test reading2 still exists in the database",
        readingRepos.findOne(testReadingId2));
    eventRepos.delete(testEventId);
    assertNull("deleted test event still exists in the database", eventRepos.findOne(testEventId));
  }

  @Test
  public void testScrubPushedEvents() {
    assertEquals(1, dao.scrubPushedEvents());
    Event event = eventRepos.findOne(testEventId);
    assertNull("Event was not scrubbed with scrub pushed call", event);
    Reading reading1 = readingRepos.findOne(testReadingId1);
    Reading reading2 = readingRepos.findOne(testReadingId2);
    assertNull("Reading1 was not scrubbed with scrub pushed call", reading1);
    assertNull("Reading2 was not scrubbed with scrub pushed call", reading2);
  }

  @Test
  public void testScrubOldEvents() throws InterruptedException {
    Thread.sleep(2000); // allow record to age so it can be removed
    assertEquals(1, dao.scrubOldEvents(1));
    Event event = eventRepos.findOne(testEventId);
    assertNull("Event was not scrubbed with scrub old call", event);
    Reading reading1 = readingRepos.findOne(testReadingId1);
    Reading reading2 = readingRepos.findOne(testReadingId2);
    assertNull("Reading1 was not scrubbed with scrub old call", reading1);
    assertNull("Reading2 was not scrubbed with scrub old call", reading2);
  }

}
