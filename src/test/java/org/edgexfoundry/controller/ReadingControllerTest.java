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
import static org.edgexfoundry.test.data.ReadingData.TEST_NAME;
import static org.edgexfoundry.test.data.ReadingData.checkTestData;
import static org.edgexfoundry.test.data.ReadingData.newTestInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.edgexfoundry.controller.impl.ReadingControllerImpl;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.ReadingData;
import org.edgexfoundry.test.data.ValueDescriptorData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;

@Category(RequiresNone.class)
public class ReadingControllerTest {

  private static final String LIMIT_PROPERTY = "maxLimit";

  private static final String META_CHECK_PROPERTY = "metaCheck";

  private static final String PERSIST_DATA_PROPERTY = "persistData";

  private static final int MAX_LIMIT = 100;

  private static final String TEST_ID = "123";

  private static final String TEST_ERR_MSG = "test message";

  @InjectMocks
  private ReadingControllerImpl controller;

  @Mock
  ReadingRepository readingRepos;

  @Mock
  EventRepository eventRepos;

  @Mock
  ValueDescriptorRepository valDescRepos;

  @Mock
  MongoTemplate template;

  @Mock
  DeviceClient deviceClient;

  private Reading reading;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    reading = newTestInstance();
    reading.setId(TEST_ID);
    setControllerMAXLIMIT(MAX_LIMIT);
  }

  @Test
  public void testReading() {
    when(readingRepos.findOne(TEST_ID)).thenReturn(reading);
    checkTestData(controller.reading(TEST_ID), TEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testReadingWithUnknownId() {
    when(readingRepos.findOne(TEST_ID)).thenReturn(null);
    controller.reading(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testReadingException() throws Exception {
    when(readingRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.reading(TEST_ID);
  }

  @Test
  public void testReadings() {
    List<Reading> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(readingRepos.count()).thenReturn(10L);
    when(readingRepos.findAll()).thenReturn(rdgs);
    List<Reading> readings = controller.readings();
    assertEquals("Find all not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsException() throws Exception {
    when(readingRepos.count()).thenReturn(10L);
    when(readingRepos.findAll()).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readings();
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsMaxLimitExceed() throws Exception {
    when(readingRepos.count()).thenReturn(Long.MAX_VALUE);
    controller.readings();
  }

  @Test
  public void testReadingCount() {
    when(readingRepos.count()).thenReturn(1L);
    assertEquals("Count of readings does not match what is in the repository", 1L,
        controller.readingCount());
  }

  @Test(expected = ServiceException.class)
  public void testReadingCountException() {
    when(readingRepos.count()).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readingCount();
  }

  /**
   * The Page<Event> object makes coverage testing of the readingsByX methods impossible. There is
   * no easy way to get back a PageRequest object from the mock object.
   */

  @Test(expected = ServiceException.class)
  public void testReadingsByDeviceException() {
    when(eventRepos.findByDevice(anyObject(), anyObject()))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readings(TEST_DEVICE_ID, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByDeviceMaxLimitExceeded() {
    controller.readings(TEST_DEVICE_ID, 1000);
  }

  @Test(expected = NotFoundException.class)
  public void testReadingsByDeviceNotFound() throws Exception {
    setMetaCheck(true);
    when(deviceClient.deviceForName(TEST_DEVICE_ID)).thenReturn(null);
    when(deviceClient.device(TEST_DEVICE_ID)).thenReturn(null);
    controller.readings(TEST_DEVICE_ID, MAX_LIMIT);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByNameException() {
    controller.readingsByName(TEST_NAME, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByNameMaxLimitExceeded() {
    controller.readingsByName(TEST_NAME, 1000);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByNameAndDeviceException() {
    controller.readingsByNameAndDevice(TEST_NAME, TEST_DEVICE_ID, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByNameAndDeviceMaxLimitExceeded() {
    controller.readingsByNameAndDevice(TEST_NAME, TEST_DEVICE_ID, 1000);
  }

  @Test
  public void testReadingsByUoMLabel() {
    List<ValueDescriptor> valueDesps = new ArrayList<>();
    valueDesps.add(ValueDescriptorData.newTestInstance());
    List<Object> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(valDescRepos.findByUomLabel(ValueDescriptorData.TEST_UOMLABEL)).thenReturn(valueDesps);
    when(template.find(any(), any())).thenReturn(rdgs);
    List<Reading> readings =
        controller.readingsByUomLabel(ValueDescriptorData.TEST_UOMLABEL, MAX_LIMIT);
    assertEquals("Find by UoM label not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), TEST_ID);
  }

  @Test
  public void testReadingsByUoMLabelNoMatchingVD() {
    List<ValueDescriptor> valueDesps = new ArrayList<>();
    List<Object> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(valDescRepos.findByUomLabel(ValueDescriptorData.TEST_UOMLABEL)).thenReturn(valueDesps);
    List<Reading> readings =
        controller.readingsByUomLabel(ValueDescriptorData.TEST_UOMLABEL, MAX_LIMIT);
    assertEquals("Find by UoM label not returning a list with one reading", 0, readings.size());
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByUoMLabelException() {
    when(valDescRepos.findByUomLabel(ValueDescriptorData.TEST_UOMLABEL))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readingsByUomLabel(ValueDescriptorData.TEST_UOMLABEL, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByUoMLabelMaxLimitExceeded() {
    controller.readingsByUomLabel(ValueDescriptorData.TEST_UOMLABEL, 1000);
  }

  @Test
  public void testReadingsByLabel() {
    List<ValueDescriptor> valueDesps = new ArrayList<>();
    valueDesps.add(ValueDescriptorData.newTestInstance());
    List<Object> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(valDescRepos.findByLabelsIn(ValueDescriptorData.TEST_LABELS[0])).thenReturn(valueDesps);
    when(template.find(any(), any())).thenReturn(rdgs);
    List<Reading> readings =
        controller.readingsByLabel(ValueDescriptorData.TEST_LABELS[0], MAX_LIMIT);
    assertEquals("Find by label not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), TEST_ID);
  }

  @Test
  public void testReadingsByLabelNoMatchingVD() {
    List<ValueDescriptor> valueDesps = new ArrayList<>();
    List<Object> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(valDescRepos.findByLabelsIn(ValueDescriptorData.TEST_LABELS[0])).thenReturn(valueDesps);
    List<Reading> readings =
        controller.readingsByLabel(ValueDescriptorData.TEST_LABELS[0], MAX_LIMIT);
    assertEquals("Find by label not returning a list with one reading", 0, readings.size());
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByLabelException() {
    when(valDescRepos.findByLabelsIn(ValueDescriptorData.TEST_LABELS[0]))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readingsByLabel(ValueDescriptorData.TEST_LABELS[0], MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByLabelMaxLimitExceeded() {
    controller.readingsByLabel(ValueDescriptorData.TEST_LABELS[0], 1000);
  }

  @Test
  public void testReadingsByType() {
    List<ValueDescriptor> valueDesps = new ArrayList<>();
    valueDesps.add(ValueDescriptorData.newTestInstance());
    List<Object> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(valDescRepos.findByType(ValueDescriptorData.TEST_TYPE)).thenReturn(valueDesps);
    when(template.find(any(), any())).thenReturn(rdgs);
    List<Reading> readings =
        controller.readingsByType(ValueDescriptorData.TEST_TYPE.toString(), MAX_LIMIT);
    assertEquals("Find by type not returning a list with one reading", 1, readings.size());
    checkTestData(readings.get(0), TEST_ID);
  }

  @Test
  public void testReadingsByTypeNoMatchingVD() {
    List<ValueDescriptor> valueDesps = new ArrayList<>();
    List<Object> rdgs = new ArrayList<>();
    rdgs.add(reading);
    when(valDescRepos.findByType(ValueDescriptorData.TEST_TYPE)).thenReturn(valueDesps);
    List<Reading> readings =
        controller.readingsByType(ValueDescriptorData.TEST_TYPE.toString(), MAX_LIMIT);
    assertEquals("Find by type not returning a list with one reading", 0, readings.size());
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByTypeException() {
    when(valDescRepos.findByType(ValueDescriptorData.TEST_TYPE))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readingsByType(ValueDescriptorData.TEST_TYPE.toString(), MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByTypeMaxLimitExceeded() {
    controller.readingsByType(ValueDescriptorData.TEST_TYPE.toString(), 1000);
  }

  @Test(expected = ServiceException.class)
  public void testReadingsByTimeException() {
    when(readingRepos.findByCreatedBetween(anyLong(), anyLong(), anyObject()))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.readings(0, Long.MAX_VALUE, MAX_LIMIT);
  }

  @Test(expected = LimitExceededException.class)
  public void testReadingsByTimeMaxLimitExceeded() {
    controller.readings(0, Long.MAX_VALUE, 1000);
  }

  @Test
  public void testAdd() {
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(valueDescriptor);
    when(readingRepos.save(reading)).thenReturn(reading);
    assertEquals("Reading id returned on add does not match expected", TEST_ID,
        controller.add(reading));
  }

  @Test(expected = DataValidationException.class)
  public void testAddNoAssociatedVD() {
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(null);
    controller.add(reading);
  }

  @Test
  public void testAddNoPersist() throws Exception {
    setPersistData(false);
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(valueDescriptor);
    assertEquals("Reading id returned on add does not match expected", "unsaved",
        controller.add(reading));
  }

  @Test(expected = ServiceException.class)
  public void testAddException() throws Exception {
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(valDescRepos.findByName(ReadingData.TEST_NAME)).thenReturn(valueDescriptor);
    when(readingRepos.save(reading)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.add(reading);
  }

  @Test
  public void testUpdate() {
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(readingRepos.findOne(TEST_ID)).thenReturn(reading);
    when(valDescRepos.findByName(reading.getName())).thenReturn(valueDescriptor);
    when(readingRepos.save(reading)).thenReturn(reading);
    assertTrue("Update of reading did not return successfully", controller.update(reading));
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateReadingNotFound() {
    when(readingRepos.findOne(TEST_ID)).thenReturn(null);
    controller.update(reading);
  }

  @Test(expected = DataValidationException.class)
  public void testUpdateValueDescriptorNotFound() {
    when(readingRepos.findOne(TEST_ID)).thenReturn(reading);
    when(valDescRepos.findByName(reading.getName())).thenReturn(null);
    controller.update(reading);
  }

  @Test(expected = ServiceException.class)
  public void testUpdateException() {
    ValueDescriptor valueDescriptor = ValueDescriptorData.newTestInstance();
    when(readingRepos.findOne(TEST_ID)).thenReturn(reading);
    when(valDescRepos.findByName(reading.getName())).thenReturn(valueDescriptor);
    when(readingRepos.save(reading)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.update(reading);
  }

  @Test
  public void testDelete() {
    when(readingRepos.findOne(TEST_ID)).thenReturn(reading);
    assertTrue("Reading was not deleted by the controller", controller.delete(TEST_ID));
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteReadingNotFound() {
    when(readingRepos.findOne(TEST_ID)).thenReturn(null);
    controller.delete(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testDeleteException() {
    when(readingRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.delete(TEST_ID);
  }

  private void setControllerMAXLIMIT(int newLimit) throws Exception {
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
