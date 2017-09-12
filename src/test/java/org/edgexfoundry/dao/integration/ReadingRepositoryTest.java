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
import static org.edgexfoundry.test.data.ReadingData.TEST_NAME;
import static org.edgexfoundry.test.data.ReadingData.TEST_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.EventData;
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
public class ReadingRepositoryTest {

  @Autowired
  private ReadingRepository readingRepos;
  private String testReadingId;

  /**
   * Create and save an instance of the Reading before each test Note: the before method tests the
   * save operation of the Repository
   */
  @Before
  public void setup() {
    Reading reading = new Reading(TEST_NAME, TEST_VALUE);
    reading.setOrigin(TEST_ORIGIN);
    reading.setPushed(TEST_PUSHED);
    reading.setDevice(EventData.TEST_DEVICE_ID);
    readingRepos.save(reading);
    testReadingId = reading.getId();
    assertNotNull("Saved Reading does not have an id", testReadingId);
  }

  /**
   * Clean up of the unit test Note: clean up also tests the delete operation of the repository
   */
  @After
  public void cleanup() {
    readingRepos.delete(testReadingId);
    assertNull("Reading not deleted as part of cleanup", readingRepos.findOne(testReadingId));
  }

  @Test
  public void testUpdate() {
    Reading reading = readingRepos.findOne(testReadingId);
    // check that create and modified timestamps are the same for a new
    // Reading record
    assertEquals("Modified and created timestamps should be equal after creation",
        reading.getModified(), reading.getCreated());
    reading.setName("newname");
    readingRepos.save(reading);
    // reread reading
    Reading reading2 = readingRepos.findOne(testReadingId);
    assertEquals("name was not updated appropriately in reading update", "newname",
        reading2.getName());
    assertNotEquals(
        "after modification, modified timestamp still the same as the reading's create timestamp",
        reading2.getModified(), reading2.getCreated());
  }

  @Test
  public void testFindOne() {
    Reading reading = readingRepos.findOne(testReadingId);
    assertNotNull("Find one returns no", reading);
    checkReadingData(reading);
  }

  @Test
  public void testFindAll() {
    List<Reading> readings = readingRepos.findAll();
    assertEquals("Find all not returning a list with one reading", 1, readings.size());
    checkReadingData(readings.get(0));
  }

  @Test
  public void testFindByCreatedBetween() {
    long now = new Date().getTime();
    // between yesterday and tomorrow
    assertEquals("find by created date not returning any readings", 1,
        readingRepos.findByCreatedBetween(now - 86400000, now + 86400000).size());
  }

  @Test
  public void testFindByCreatedBetweenWithNoBetween() {
    long now = new Date().getTime();
    // between beginning of time and a year ago
    assertEquals("find by created date with bad dates is returning readings", 0,
        readingRepos.findByCreatedBetween(0, now - (86400000 * 365)).size());
  }

  @Test
  public void testFindByModifiedBetween() {
    long now = new Date().getTime();
    // between yesterday and tomorrow
    assertEquals("find by modified date not returning any readings", 1,
        readingRepos.findByModifiedBetween(now - 86400000, now + 86400000).size());
  }

  @Test
  public void testFindByModifiedBetweenWithNoBetween() {
    long now = new Date().getTime();
    // between beginning of time and a year ago
    assertEquals("find by modified date with bad dates is returning readings", 0,
        readingRepos.findByCreatedBetween(0, now - (86400000 * 365)).size());
  }

  @Test
  public void testFindByOriginBetween() {
    assertEquals("find by origin date not returning any readings", 1,
        readingRepos.findByOriginBetween(TEST_ORIGIN - 10, TEST_ORIGIN + 10).size());
  }

  @Test
  public void testFindByOriginBetweenWithNoBetween() {
    assertEquals("find by origin date with bad dates is returning readings", 0,
        readingRepos.findByCreatedBetween(0, TEST_ORIGIN - 100).size());
  }

  @Test
  public void testfindByPushedGreaterThan() {
    assertEquals("find by pushed date not returning any readings", 1,
        readingRepos.findByPushedGreaterThan(TEST_PUSHED - 10).size());
  }

  @Test
  public void testfindByPushedGreaterThanWithNoGreaterThan() {
    assertEquals("find by pushed date greater than with bad dates is returning readings", 0,
        readingRepos.findByPushedGreaterThan(TEST_PUSHED + 10).size());
  }

  @Test
  public void testFindByName() {
    List<Reading> readings = readingRepos.findByName(TEST_NAME);
    assertEquals("find by name not returning any readings", 1, readings.size());
    checkReadingData(readings.get(0));
  }

  @Test
  public void testFindByNameWithNoMatching() {
    List<Reading> readings = readingRepos.findByName("noname");
    assertTrue("find by name with bad name is returning readings", readings.isEmpty());
  }

  @Test
  public void testFindByValue() {
    List<Reading> readings = readingRepos.findByValueIn(TEST_VALUE);
    assertEquals("find by value not returning any readings", 1, readings.size());
    checkReadingData(readings.get(0));
  }

  @Test
  public void testFindByValueWithNoMatching() {
    List<Reading> readings = readingRepos.findByValueIn("noval");
    assertTrue("find by name with bad value is returning readings", readings.isEmpty());
  }

  @Test
  public void testFindByNameAndValue() {
    List<Reading> readings = readingRepos.findByNameAndValue(TEST_NAME, TEST_VALUE);
    assertEquals("find by name and value not returning any readings", 1, readings.size());
    checkReadingData(readings.get(0));
  }

  @Test
  public void testFindByNameAndValueWithNoMatching() {
    List<Reading> readings = readingRepos.findByNameAndValue("noname", "noval");
    assertTrue("find by name and value with bad name/value is returning readings",
        readings.isEmpty());
  }

  @Test
  public void testFindByNameAndDevice() {
    List<Reading> readings = readingRepos.findByNameAndDevice(TEST_NAME, EventData.TEST_DEVICE_ID);
    assertEquals("find by name and device not returning any readings", 1, readings.size());
    checkReadingData(readings.get(0));
  }

  @Test
  public void testFindByNameAndDeviceWithNoMatching() {
    List<Reading> readings = readingRepos.findByNameAndDevice("noname", "noval");
    assertTrue("find by name and device with bad name/device is returning readings",
        readings.isEmpty());
  }

  private void checkReadingData(Reading reading) {
    assertEquals("Name not the same as saved reading name", TEST_NAME, reading.getName());
    assertEquals("Value not the same as saved reading value", TEST_VALUE, reading.getValue());
    assertEquals("Reading origin does not match saved origin", TEST_ORIGIN, reading.getOrigin());
    assertEquals("Reading pushed does not match saved pushed date", TEST_PUSHED,
        reading.getPushed());
    assertEquals("Reading device does not match saved device", EventData.TEST_DEVICE_ID,
        reading.getDevice());
    assertNotNull("Reading modified date is null", reading.getModified());
    assertNotNull("Reading create date is null", reading.getCreated());
  }

}
