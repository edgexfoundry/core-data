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
import static org.edgexfoundry.test.data.ReadingData.TEST_NAME;
import static org.edgexfoundry.test.data.ReadingData.checkTestData;
import static org.edgexfoundry.test.data.ReadingData.newTestInstance;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_LABELS;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_TYPE;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_UOMLABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.impl.ReadingControllerImpl;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
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
@Category({RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class})
public class ReadingControllerTest {

  private static final String MAX_LIMIT = "maxLimit";

  @Autowired
  ReadingControllerImpl controller;

  @Autowired
  ValueDescriptorRepository valDescRepos;

  @Autowired
  ReadingRepository repos;

  @Autowired
  EventRepository eventRepos;

  @Autowired
  MongoTemplate template;

  private String testReadingId;

  // TODO - need tests to test device client calls

  @Before
  public void setup() {
    ValueDescriptor valDesc = ValueDescriptorData.newTestInstance();
    valDesc.setOrigin(TEST_ORIGIN);
    valDescRepos.save(valDesc);
    Reading reading = newTestInstance();
    reading.setOrigin(TEST_ORIGIN);
    repos.save(reading);
    testReadingId = reading.getId();
    assertNotNull("Saved Reading does not have an id", testReadingId);
  }

  @After
  public void cleanup() throws Exception {
    resetControllerEventRepos();
    resetControllerRepos();
    resetControllerMongoTemplate();
    resetControllerMaxLimit();
    repos.deleteAll();
    valDescRepos.deleteAll();
    eventRepos.deleteAll();
    assertNull("Reading not deleted as part of cleanup", repos.findOne(testReadingId));
  }

  @Test
  public void testReading() {
    Reading reading = controller.reading(testReadingId);
    checkTestData(reading, testReadingId);
  }

  @Test
  public void testReadingCount() {
    assertEquals("Count of readings does not match what is in the repository", 1L,
        controller.readingCount());
  }

  @Test(expected = NotFoundException.class)
  public void testReadingWithUnknownId() {
    controller.reading("nosuchid");
  }

  @Test(expected = ServiceException.class)
  public void testReadingException() throws Exception {
    unsetControllerRepos();
    controller.reading(testReadingId);
  }

  @Test
  public void testReadings() {
    List<Reading> readings = controller.readings();
    assertEquals("Find all not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsException() throws Exception {
    unsetControllerRepos();
    controller.readings();
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.readings();
  }

  @Test
  public void testReadingsByDeviceId() {
    Event event = EventData.newTestInstance();
    event.setReadings(repos.findAll());
    eventRepos.save(event);

    List<Reading> deviceReadings = controller.readings(EventData.TEST_DEVICE_ID, 10);
    assertEquals("Find by device id not returning a list with one reading", 1,
        deviceReadings.size());
    checkTestData(deviceReadings.get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByDeviceIdException() throws Exception {
    Event event = EventData.newTestInstance();
    event.setReadings(repos.findAll());
    eventRepos.save(event);

    unsetControllerEventRepos();
    controller.readings(EventData.TEST_DEVICE_ID, 10);

  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByDeviceIdMaxLimitExceed() throws Exception {
    Event event = EventData.newTestInstance();
    event.setReadings(repos.findAll());

    unsetControllerMaxLimit();
    controller.readings(EventData.TEST_DEVICE_ID, 10);
  }

  @Test
  public void testReadingsByName() {
    List<Reading> readings = controller.readingsByName(ReadingData.TEST_NAME, 10);
    assertEquals("Find by name not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByNameException() throws Exception {
    unsetControllerRepos();
    controller.readingsByName(ReadingData.TEST_NAME, 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByNameMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.readingsByName(ReadingData.TEST_NAME, 10);
  }

  @Test
  public void testReadingsByNameAndDevice() {
    List<Reading> readings =
        controller.readingsByNameAndDevice(ReadingData.TEST_NAME, EventData.TEST_DEVICE_ID, 10);
    assertEquals("Find by name and device not returning a list with one reading", 1,
        readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByNameAndDeviceException() throws Exception {
    unsetControllerRepos();
    controller.readingsByNameAndDevice(ReadingData.TEST_NAME, EventData.TEST_DEVICE_ID, 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByNameAndDeviceMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.readingsByNameAndDevice(ReadingData.TEST_NAME, EventData.TEST_DEVICE_ID, 10);
  }

  @Test
  public void testReadingsByUomLabel() {
    List<Reading> readings = controller.readingsByUomLabel(TEST_UOMLABEL, 10);
    assertEquals("Find by UoM label not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test
  public void testReadingsByUomLabelWithUnknownUomLabel() {
    List<Reading> readings = controller.readingsByUomLabel("unknownuomlabel", 10);
    assertEquals("Find by UoM label with unknown label is still returning a list with one reading",
        0, readings.size());
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByUomLabelException() throws Exception {
    unsetControllerMongoTemplate();
    controller.readingsByUomLabel(TEST_UOMLABEL, 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByUomLabelMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.readingsByUomLabel(TEST_UOMLABEL, 10);
  }

  @Test
  public void testReadingsByLabel() {
    List<Reading> readings = controller.readingsByLabel(TEST_LABELS[0], 10);
    assertEquals("Find by label not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test
  public void testReadingsByLabelWithUnknownLabel() {
    List<Reading> readings = controller.readingsByLabel("unknownlabel", 10);
    assertEquals("Find by label with unknown label is still returning a list with one reading", 0,
        readings.size());
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByLabelException() throws Exception {
    unsetControllerMongoTemplate();
    controller.readingsByLabel(TEST_LABELS[0], 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByLabelMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.readingsByUomLabel(TEST_LABELS[0], 10);
  }

  @Test
  public void testReadingsByType() {
    List<Reading> readings = controller.readingsByType(TEST_TYPE.toString(), 10);
    assertEquals("Find by type not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByTypeWithUnknownType() {
    List<Reading> readings = controller.readingsByType("z", 10);
    assertNull("Find by type with unknown type is still returning a list with one reading",
        readings);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByTypeException() throws Exception {
    unsetControllerMongoTemplate();
    controller.readingsByType(TEST_TYPE.toString(), 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByTypeMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.readingsByType(TEST_TYPE.toString(), 10);
  }

  @Test
  public void testReadingsByTime() {
    long now = new Date().getTime();
    // between yesterday and tomorrow
    List<Reading> readings = controller.readings(now - 86400000, now + 86400000, 10);
    assertEquals("Find by start and end time not returning a list with one reading", 1,
        readings.size());
    checkTestData(readings.get(0), testReadingId);
  }

  @Test
  public void testReadingsByTimeWithNoneMatching() {
    long now = new Date().getTime();
    // between beginning of time and yesterday
    List<Reading> readings = controller.readings(0, now - 86400000, 10);
    assertEquals(
        "Find by start and end time with none in that period is still returning a list with one reading",
        0, readings.size());
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByTimeException() throws Exception {
    unsetControllerRepos();
    long now = new Date().getTime();
    controller.readings(now - 86400000, now + 86400000, 10);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByTimeMaxLimitExceed() throws Exception {
    long now = new Date().getTime();
    unsetControllerMaxLimit();
    controller.readings(now - 86400000, now + 86400000, 10);
  }

  @Test
  public void testAdd() {
    Reading reading2 = new Reading(ReadingData.TEST_NAME, ReadingData.TEST_VALUE);
    assertNotNull("New reading id is null", controller.add(reading2));
    assertNotNull("Reading ID is not present", reading2.getId());
    assertNotNull("Reading modified date is null", reading2.getModified());
    assertNotNull("Reading create date is null", reading2.getCreated());
  }

  @Test(expected = DataValidationException.class)
  public void testAddWithBadName() {
    Reading reading2 = new Reading("unknownid", ReadingData.TEST_VALUE);
    controller.add(reading2);
  }

  @Test(expected = ServiceException.class)
  public void testAddException() throws Exception {
    unsetControllerRepos();
    Reading reading2 = new Reading(ReadingData.TEST_NAME, ReadingData.TEST_VALUE);
    controller.add(reading2);
  }

  @Test
  public void testDelete() {
    assertTrue("Reading was not deleted by the controller", controller.delete(testReadingId));
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteWithUnknownId() {
    controller.delete("unknownid");
  }

  @Test(expected = ServiceException.class)
  public void testDeleteException() throws Exception {
    unsetControllerRepos();
    controller.delete(testReadingId);
  }

  @Test
  public void testUpdate() {
    Reading reading2 = new Reading(TEST_NAME, "newvalue");
    reading2.setOrigin(1234);
    reading2.setId(testReadingId);
    assertTrue("Reading controller unable to update reading", controller.update(reading2));

    Reading reading = repos.findOne(testReadingId);

    assertEquals("Reading ID does not match saved id", testReadingId, reading.getId());
    assertEquals("Reading name does not match saved name", TEST_NAME, reading.getName());
    assertEquals("Reading value does not match saved name", "newvalue", reading.getValue());
    assertEquals("Reading origin does not match saved origin", 1234, reading.getOrigin());
    assertNotNull("Reading modified date is null", reading.getModified());
    assertNotNull("Reading create date is null", reading.getCreated());
    assertTrue(reading.getModified() != reading.getCreated());
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateWithUnknownId() {
    Reading reading = new Reading();
    reading.setId("unknownId");
    controller.update(reading);
  }

  @Test(expected = ServiceException.class)
  public void testUpdateException() throws Exception {
    unsetControllerRepos();
    Reading reading = new Reading();
    reading.setId(testReadingId);
    controller.update(reading);
  }

  @Test(expected = DataValidationException.class)
  public void testUpdateWithBadName() {
    Reading reading = new Reading("unknownname", "somevalue");
    reading.setId(testReadingId);
    reading.setOrigin(TEST_ORIGIN);
    controller.update(reading);
  }

  // use Java reflection to unset controller's repos
  private void unsetControllerRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("readingRepos");
    temp.setAccessible(true);
    temp.set(controller, null);
  }

  // use Java reflection to reset controller's repos
  private void resetControllerRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("readingRepos");
    temp.setAccessible(true);
    temp.set(controller, repos);
  }

  // use Java reflection to unset controller's repos
  private void unsetControllerEventRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("eventRepos");
    temp.setAccessible(true);
    temp.set(controller, null);
  }

  // use Java reflection to reset controller's repos
  private void resetControllerEventRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("eventRepos");
    temp.setAccessible(true);
    temp.set(controller, eventRepos);
  }

  // use Java reflection to unset controller's tempalte
  private void unsetControllerMongoTemplate() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("template");
    temp.setAccessible(true);
    temp.set(controller, null);
  }

  // use Java reflection to reset controller's template
  private void resetControllerMongoTemplate() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("template");
    temp.setAccessible(true);
    temp.set(controller, template);
  }

  // use Java reflection to unset controller's tempalte
  private void unsetControllerMaxLimit() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(MAX_LIMIT);
    temp.setAccessible(true);
    temp.set(controller, 0);
  }

  // use Java reflection to reset controller's template
  private void resetControllerMaxLimit() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(MAX_LIMIT);
    temp.setAccessible(true);
    temp.set(controller, 1000);
  }

}
