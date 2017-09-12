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

import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_LABELS;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_NAME;
import static org.edgexfoundry.test.data.ValueDescriptorData.checkTestData;
import static org.edgexfoundry.test.data.ValueDescriptorData.newTestInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.edgexfoundry.controller.impl.ValueDescriptorControllerImpl;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.domain.meta.Command;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresNone;
import org.edgexfoundry.test.data.CommandData;
import org.edgexfoundry.test.data.DeviceData;
import org.edgexfoundry.test.data.ProfileData;
import org.edgexfoundry.test.data.ReadingData;
import org.edgexfoundry.test.data.ValueDescriptorData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

@Category(RequiresNone.class)
public class ValueDescriptorTest {

  private static final int MAX_LIMIT = 100;

  private static final String LIMIT_PROPERTY = "maxLimit";
  private static final String FORMAT_SPEC_PROPERTY = "formatSpecifier";

  private static final String TEST_ID = "123";

  private static final String TEST_ERR_MSG = "test message";

  @InjectMocks
  private ValueDescriptorControllerImpl controller;

  @Mock
  ValueDescriptorRepository valDescRepos;

  @Mock
  ReadingRepository readingRepos;

  @Mock
  DeviceClient deviceClient;

  @Mock
  MongoTemplate template;

  private ValueDescriptor valueDescriptor;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    valueDescriptor = newTestInstance();
    valueDescriptor.setId(TEST_ID);
    setControllerMAXLIMIT(MAX_LIMIT);
  }

  @Test
  public void testValueDescriptor() {
    when(valDescRepos.findOne(TEST_ID)).thenReturn(valueDescriptor);
    checkTestData(controller.valueDescriptor(TEST_ID), TEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testValueDescriptorWithUnknownId() {
    when(valDescRepos.findOne(TEST_ID)).thenReturn(null);
    controller.valueDescriptor(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorException() {
    when(valDescRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptor(TEST_ID);
  }

  @Test
  public void testValueDescriptors() {
    List<ValueDescriptor> valDes = new ArrayList<>();
    valDes.add(valueDescriptor);
    when(valDescRepos.count()).thenReturn(1L);
    when(valDescRepos.findAll()).thenReturn(valDes);
    List<ValueDescriptor> valueDescriptors = controller.valueDescriptors();
    assertEquals("Find all not returning a list with one value descriptor", 1,
        valueDescriptors.size());
    checkTestData(valueDescriptors.get(0), TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorsException() {
    when(valDescRepos.count()).thenReturn(1L);
    when(valDescRepos.findAll()).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptors();
  }

  @Test(expected = LimitExceededException.class)
  public void testValueDescriptorsMaxLimitExceed() {
    when(valDescRepos.count()).thenReturn(1000L);
    controller.valueDescriptors();
  }

  @Test
  public void testValueDescriptorByName() {
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(valueDescriptor);
    checkTestData(controller.valueDescriptorByName(TEST_NAME), TEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testValueDescriptorByNameNotFound() {
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(null);
    controller.valueDescriptorByName(TEST_NAME);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorByNameException() {
    when(valDescRepos.findByName(TEST_NAME)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptorByName(TEST_NAME);
  }

  @Test
  public void testValueDescriptorByUoMLabel() {
    List<ValueDescriptor> valDes = new ArrayList<>();
    valDes.add(valueDescriptor);
    when(valDescRepos.findByUomLabel(ValueDescriptorData.TEST_UOMLABEL)).thenReturn(valDes);
    List<ValueDescriptor> valueDescriptors =
        controller.valueDescriptorByUoMLabel(ValueDescriptorData.TEST_UOMLABEL);
    assertEquals("Find by UOM label not returning a list with one value descriptor", 1,
        valueDescriptors.size());
    checkTestData(valueDescriptors.get(0), TEST_ID);
  }

  @Test
  public void testValueDescriptorByUoMLabelNotFound() {
    List<ValueDescriptor> valDes = new ArrayList<>();
    when(valDescRepos.findByUomLabel(ValueDescriptorData.TEST_UOMLABEL)).thenReturn(valDes);
    assertEquals("Find by UOM label not returning a list with one value descriptor", 0,
        controller.valueDescriptorByUoMLabel(ValueDescriptorData.TEST_UOMLABEL).size());
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorByUoMLabelException() {
    when(valDescRepos.findByUomLabel(ValueDescriptorData.TEST_UOMLABEL))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptorByUoMLabel(ValueDescriptorData.TEST_UOMLABEL);
  }

  @Test
  public void testValueDescriptorByLabel() {
    List<Object> valDes = new ArrayList<>();
    valDes.add(valueDescriptor);
    when(template.find(anyObject(), anyObject())).thenReturn(valDes);
    List<ValueDescriptor> valueDescriptors = controller.valueDescriptorByLabel(TEST_LABELS[0]);
    assertEquals("Find by label not returning a list with one value descriptor", 1,
        valueDescriptors.size());
    checkTestData(valueDescriptors.get(0), TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorByLabelException() {
    when(template.find(anyObject(), anyObject())).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptorByLabel(ValueDescriptorData.TEST_LABELS[0]);
  }

  @Test
  public void testValueDescriptorsForDeviceByName() {
    Device device = DeviceData.newTestInstance();
    DeviceProfile profile = ProfileData.newTestInstance();
    Command command = CommandData.newTestInstance();
    profile.addCommand(command);
    device.setProfile(profile);
    List<ValueDescriptor> valDes = new ArrayList<>();
    valDes.add(valueDescriptor);
    when(deviceClient.deviceForName(DeviceData.TEST_NAME)).thenReturn(device);
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(valueDescriptor);
    List<ValueDescriptor> valueDescriptors =
        controller.valueDescriptorsForDeviceByName(DeviceData.TEST_NAME);
    checkTestData(valueDescriptors.get(0), TEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testValueDescriptorsForDeviceByNameNotFound() {
    when(deviceClient.deviceForName(DeviceData.TEST_NAME))
        .thenThrow(new javax.ws.rs.NotFoundException());
    controller.valueDescriptorsForDeviceByName(DeviceData.TEST_NAME);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorsForDeviceByNameException() {
    when(deviceClient.deviceForName(DeviceData.TEST_NAME))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptorsForDeviceByName(DeviceData.TEST_NAME);
  }

  @Test
  public void testValueDescriptorsForDeviceById() {
    Device device = DeviceData.newTestInstance();
    DeviceProfile profile = ProfileData.newTestInstance();
    Command command = CommandData.newTestInstance();
    profile.addCommand(command);
    device.setProfile(profile);
    List<ValueDescriptor> valDes = new ArrayList<>();
    valDes.add(valueDescriptor);
    when(deviceClient.device(TEST_ID)).thenReturn(device);
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(valueDescriptor);
    List<ValueDescriptor> valueDescriptors = controller.valueDescriptorsForDeviceById(TEST_ID);
    checkTestData(valueDescriptors.get(0), TEST_ID);
  }

  @Test(expected = NotFoundException.class)
  public void testValueDescriptorsForDeviceByIdNotFound() {
    when(deviceClient.device(TEST_ID)).thenThrow(new javax.ws.rs.NotFoundException());
    controller.valueDescriptorsForDeviceById(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorsForDeviceByIdException() {
    when(deviceClient.deviceForName(DeviceData.TEST_NAME))
        .thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.valueDescriptorsForDeviceById(TEST_ID);
  }

  @Test
  public void testAdd() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.save(valueDescriptor)).thenReturn(valueDescriptor);
    assertEquals("Value Descriptor id not retured as expected", TEST_ID,
        controller.add(valueDescriptor));
  }

  @Test(expected = DataValidationException.class)
  public void testAddWithBadFormattingString() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    valueDescriptor.setFormatting("junkformat");
    when(valDescRepos.save(valueDescriptor)).thenReturn(valueDescriptor);
    controller.add(valueDescriptor);
  }

  @Test(expected = ServiceException.class)
  public void testAddException() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.save(valueDescriptor)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.add(valueDescriptor);
  }

  @Test(expected = DataValidationException.class)
  public void testAddValueDescriptorWithSameName() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.save(valueDescriptor)).thenThrow(new DuplicateKeyException(TEST_ERR_MSG));
    controller.add(valueDescriptor);
  }

  @Test
  public void testUpdateById() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.findOne(TEST_ID)).thenReturn(valueDescriptor);
    assertTrue("Value descriptor controller unable to update value descriptor",
        controller.update(valueDescriptor));
  }

  @Test
  public void testUpdateByName() throws Exception {
    valueDescriptor.setId(null);
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.findByName(ValueDescriptorData.TEST_NAME)).thenReturn(valueDescriptor);
    assertTrue("Value descriptor controller unable to update value descriptor",
        controller.update(valueDescriptor));
  }

  @Test(expected = DataValidationException.class)
  public void testUpdateWithBadFormattingString() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    valueDescriptor.setFormatting("junkformat");
    when(valDescRepos.findOne(TEST_ID)).thenReturn(valueDescriptor);
    controller.update(valueDescriptor);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateValueDescriptorWithUnknownIdOrName() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.findOne(TEST_ID)).thenReturn(null);
    when(valDescRepos.findByName(ValueDescriptorData.TEST_NAME)).thenReturn(null);
    controller.update(valueDescriptor);
  }

  @Test(expected = ServiceException.class)
  public void testUpdateException() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    when(valDescRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.update(valueDescriptor);
  }

  @Test(expected = DataValidationException.class)
  public void testUpdateByIdChangeNameReferencedByReadings() throws Exception {
    setControllerFormatSpecifier(ValueDescriptorData.TEST_FORMATTING);
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    when(readingRepos.findByName(valueDescriptor.getName())).thenReturn(readings);
    when(valDescRepos.findOne(TEST_ID)).thenReturn(valueDescriptor);
    controller.update(valueDescriptor);
  }

  @Test
  public void testDelete() {
    when(valDescRepos.findOne(TEST_ID)).thenReturn(valueDescriptor);
    assertTrue("ValueDescriptor was not deleted by the controller", controller.delete(TEST_ID));
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteNotFound() {
    when(valDescRepos.findOne(TEST_ID)).thenReturn(null);
    controller.delete(TEST_ID);
  }

  @Test(expected = DataValidationException.class)
  public void testDeleteWithAssociatedReadings() {
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    when(readingRepos.findByName(valueDescriptor.getName())).thenReturn(readings);
    when(valDescRepos.findOne(TEST_ID)).thenReturn(valueDescriptor);
    controller.delete(TEST_ID);
  }

  @Test(expected = ServiceException.class)
  public void testDeleteException() {
    when(valDescRepos.findOne(TEST_ID)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.delete(TEST_ID);
  }

  @Test
  public void testDeleteByName() {
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(valueDescriptor);
    assertTrue("ValueDescriptor was not deleted by the controller",
        controller.deleteByName(TEST_NAME));
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteByNameNotFound() {
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(null);
    controller.deleteByName(TEST_NAME);
  }

  @Test(expected = DataValidationException.class)
  public void testDeleteByNameWithAssociatedReadings() {
    List<Reading> readings = new ArrayList<>();
    Reading reading = ReadingData.newTestInstance();
    readings.add(reading);
    when(readingRepos.findByName(valueDescriptor.getName())).thenReturn(readings);
    when(valDescRepos.findByName(TEST_NAME)).thenReturn(valueDescriptor);
    controller.deleteByName(TEST_NAME);
  }

  @Test(expected = ServiceException.class)
  public void testDeleteByNameException() {
    when(valDescRepos.findByName(TEST_NAME)).thenThrow(new RuntimeException(TEST_ERR_MSG));
    controller.deleteByName(TEST_NAME);
  }

  private void setControllerMAXLIMIT(int newLimit) throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(LIMIT_PROPERTY);
    temp.setAccessible(true);
    temp.set(controller, newLimit);
  }

  private void setControllerFormatSpecifier(String newFormatSpecifier) throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField(FORMAT_SPEC_PROPERTY);
    temp.setAccessible(true);
    temp.set(controller, newFormatSpecifier);
  }

}
