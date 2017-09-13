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

import static org.edgexfoundry.test.data.CommonData.TEST_DESCRIPTION;
import static org.edgexfoundry.test.data.CommonData.TEST_ORIGIN;
import static org.edgexfoundry.test.data.CommonData.TEST_PUSHED;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_DEF_VALUE;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_FORMATTING;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_LABELS;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_MAX;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_MIN;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_NAME;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_TYPE;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_UOMLABEL;
import static org.edgexfoundry.test.data.ValueDescriptorData.checkTestData;
import static org.edgexfoundry.test.data.ValueDescriptorData.newTestInstance;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.impl.ValueDescriptorControllerImpl;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.IoTType;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
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
public class ValueDescriptorControllerTest {

  private static final int UPDATE_INT = 99;
  private static final String UPDATE_STRING = "foo";
  private static final String UPDATE_FMT_STRING = "%s";
  private static final String[] UPDATE_STRING_ARRAY = {"foo", "bar"};
  private static final String MAX_LIMIT = "maxLimit";

  @Autowired
  ValueDescriptorControllerImpl controller;

  @Autowired
  ValueDescriptorRepository repos;

  @Autowired
  ReadingRepository readingRepos;

  @Autowired
  MongoTemplate template;

  private String testValDescId;

  @Before
  public void setup() {

    ValueDescriptor valDesc = newTestInstance();
    repos.save(valDesc);
    testValDescId = valDesc.getId();
    assertNotNull("Saved ValueDescriptor does not have an id", testValDescId);
  }

  @After
  public void cleanup() throws Exception {
    resetControllerRepos();
    resetControllerMongoTemplate();
    resetControllerMaxLimit();
    repos.deleteAll();
    readingRepos.deleteAll();
    assertNull("ValueDescriptor not deleted as part of cleanup", repos.findOne(testValDescId));
  }

  @Test
  public void testValueDescriptor() {
    ValueDescriptor valueDescriptor = controller.valueDescriptor(testValDescId);
    checkTestData(valueDescriptor, testValDescId);
  }

  @Test(expected = NotFoundException.class)
  public void testValueDescriptorWithUnknownId() {
    controller.valueDescriptor("nosuchid");
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorException() throws Exception {
    unsetControllerRepos();
    controller.valueDescriptor(testValDescId);
  }

  @Test
  public void testValueDescriptors() {
    List<ValueDescriptor> valueDescriptors = controller.valueDescriptors();
    assertEquals("Find all not returning a list with one value descriptor", 1,
        valueDescriptors.size());
    checkTestData(valueDescriptors.get(0), testValDescId);
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorsException() throws Exception {
    unsetControllerRepos();
    controller.valueDescriptors();
  }

  @Test(expected = LimitExceededException.class)
  public void testValueDescriptorsMaxLimitExceed() throws Exception {
    unsetControllerMaxLimit();
    controller.valueDescriptors();
  }

  @Test
  public void testValueDescriptorByName() {
    ValueDescriptor valueDescriptor = controller.valueDescriptorByName(TEST_NAME);
    checkTestData(valueDescriptor, testValDescId);
  }

  @Test(expected = NotFoundException.class)
  public void testValueDescriptorByNameWithBadName() {
    controller.valueDescriptorByName("noknownname");
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorByNameException() throws Exception {
    unsetControllerRepos();
    controller.valueDescriptorByName(TEST_NAME);
  }

  @Test
  public void testValueDescriptorByUOMLabel() {
    List<ValueDescriptor> valueDescriptors = controller.valueDescriptorByUoMLabel(TEST_UOMLABEL);
    assertEquals("Find by UOM label not returning a list with one value descriptor", 1,
        valueDescriptors.size());
    checkTestData(valueDescriptors.get(0), testValDescId);
  }

  @Test
  public void testValueDescriptorByUOMLabelWithUnknownUOM() {
    assertEquals(
        "Controller is returning something other than empty list with an unknown uom label", 0,
        controller.valueDescriptorByUoMLabel("unknownuomlabel").size());
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorByUOMLabelException() throws Exception {
    unsetControllerRepos();
    controller.valueDescriptorByUoMLabel(TEST_UOMLABEL);
  }

  @Test
  public void testValueDescriptorByLabel() {
    List<ValueDescriptor> valueDescriptors = controller.valueDescriptorByLabel(TEST_LABELS[0]);
    assertEquals("Find by label not returning a list with one value descriptor", 1,
        valueDescriptors.size());
    checkTestData(valueDescriptors.get(0), testValDescId);
  }

  @Test
  public void testValueDescriptorByUOMLabelWithUnknownLabel() {
    assertEquals("Controller is returning something other than empty list with an unknown label", 0,
        controller.valueDescriptorByUoMLabel("unknownlabel").size());
  }

  @Test(expected = ServiceException.class)
  public void testValueDescriptorByLabelException() throws Exception {
    unsetControllerMongoTemplate();
    controller.valueDescriptorByLabel(TEST_LABELS[0]);
  }

  // TODO - someday create more extensive tests to check value descriptor for
  // device. Requires setup of metadata first and requires metadata running.
  @Test(expected = NotFoundException.class)
  public void testValueDescriptorsForDeviceByName() {
    controller.valueDescriptorsForDeviceByName("unknowndevice");
  }

  // this test requires meta data to be running in order to pass
  @Test(expected = NotFoundException.class)
  public void testValueDescriptorsForDeviceById() {
    controller.valueDescriptorsForDeviceById("unknownid");
  }

  @Test
  public void testAddValueDescriptor() {
    ValueDescriptor valDesc = new ValueDescriptor(TEST_NAME + "2nd", TEST_MIN, TEST_MAX, TEST_TYPE,
        TEST_UOMLABEL, TEST_DEF_VALUE, TEST_FORMATTING, TEST_LABELS, TEST_DESCRIPTION);
    assertNotNull("New value descriptor id is null", controller.add(valDesc));
    assertNotNull("ValueDescriptor ID is not present", valDesc.getId());
    assertNotNull("ValueDescriptor modified date is null", valDesc.getModified());
    assertNotNull("ValueDescriptor create date is null", valDesc.getCreated());
  }

  @Test(expected = DataValidationException.class)
  public void testAddValueDescriptorWithBadFormattingString() {
    ValueDescriptor valDesc = new ValueDescriptor(TEST_NAME + "2nd", TEST_MIN, TEST_MAX, TEST_TYPE,
        TEST_UOMLABEL, TEST_DEF_VALUE, "junkformat", TEST_LABELS, TEST_DESCRIPTION);
    controller.add(valDesc);
  }

  @Test(expected = ServiceException.class)
  public void testAddValueDescriptorException() throws Exception {
    unsetControllerRepos();
    ValueDescriptor valDesc = new ValueDescriptor(TEST_NAME + "2nd", TEST_MIN, TEST_MAX, TEST_TYPE,
        TEST_UOMLABEL, TEST_DEF_VALUE, TEST_FORMATTING, TEST_LABELS, TEST_DESCRIPTION);
    controller.add(valDesc);
  }

  @Test(expected = DataValidationException.class)
  public void testAddValueDescriptorWithSameName() {
    ValueDescriptor valDesc = new ValueDescriptor(TEST_NAME, TEST_MIN, TEST_MAX, TEST_TYPE,
        TEST_UOMLABEL, TEST_DEF_VALUE, TEST_FORMATTING, TEST_LABELS, TEST_DESCRIPTION);
    controller.add(valDesc);
  }

  @Test
  public void testDeleteValueDescriptor() {
    assertTrue("ValueDescriptor was not deleted by the controller",
        controller.delete(testValDescId));
  }

  @Test
  public void testDeleteByNameValueDescriptor() {
    assertTrue("ValueDescriptor was not deleted by the controller",
        controller.deleteByName(TEST_NAME));
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteValueDescriptorWithUnknownId() {
    controller.delete("unknownid");
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteValueDescriptorWithUnknownName() {
    controller.deleteByName("unknownname");
  }

  @Test(expected = ServiceException.class)
  public void testDeleteValueDescriptorException() throws Exception {
    unsetControllerRepos();
    controller.delete(testValDescId);
  }

  @Test(expected = ServiceException.class)
  public void testDeleteByNameValueDescriptorException() throws Exception {
    unsetControllerRepos();
    controller.deleteByName(TEST_NAME);
  }

  @Test(expected = DataValidationException.class)
  public void testDeleteValueDescriptorWithAssociatedReading() {
    Reading reading = new Reading(TEST_NAME, "somevalue");
    reading.setOrigin(TEST_ORIGIN);
    reading.setPushed(TEST_PUSHED);
    readingRepos.save(reading);
    controller.delete(testValDescId);
  }

  @Test
  public void testUpdateValueDescriptorById() {
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setId(testValDescId);
    valueDescriptor2.setFormatting(UPDATE_FMT_STRING);
    valueDescriptor2.setMin(UPDATE_INT);
    valueDescriptor2.setMax(UPDATE_INT);
    valueDescriptor2.setType(IoTType.I);
    valueDescriptor2.setUomLabel(UPDATE_STRING);
    valueDescriptor2.setDefaultValue(UPDATE_INT);
    valueDescriptor2.setLabels(UPDATE_STRING_ARRAY);
    valueDescriptor2.setOrigin(UPDATE_INT);
    assertTrue("Value descriptor controller unable to update value descriptor",
        controller.update(valueDescriptor2));
    ValueDescriptor valueDescriptor = repos.findOne(testValDescId);
    checkUpdatedValueDescriptor(valueDescriptor);
  }

  @Test
  public void testUpdateValueDescriptorByName() {
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setName(TEST_NAME);
    valueDescriptor2.setFormatting(UPDATE_FMT_STRING);
    valueDescriptor2.setMin(UPDATE_INT);
    valueDescriptor2.setMax(UPDATE_INT);
    valueDescriptor2.setType(IoTType.I);
    valueDescriptor2.setUomLabel(UPDATE_STRING);
    valueDescriptor2.setDefaultValue(UPDATE_INT);
    valueDescriptor2.setLabels(UPDATE_STRING_ARRAY);
    valueDescriptor2.setOrigin(UPDATE_INT);
    assertTrue("Value descriptor controller unable to update value descriptor",
        controller.update(valueDescriptor2));
    ValueDescriptor valueDescriptor = repos.findOne(testValDescId);
    checkUpdatedValueDescriptor(valueDescriptor);
  }

  @Test(expected = DataValidationException.class)
  public void testUpdateValueDescriptorWithBadFormattingString() {
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setName(TEST_NAME);
    valueDescriptor2.setFormatting("foobar");
    valueDescriptor2.setMin(UPDATE_INT);
    valueDescriptor2.setMax(UPDATE_INT);
    valueDescriptor2.setType(IoTType.I);
    valueDescriptor2.setUomLabel(UPDATE_STRING);
    valueDescriptor2.setDefaultValue(UPDATE_INT);
    valueDescriptor2.setLabels(UPDATE_STRING_ARRAY);
    valueDescriptor2.setOrigin(UPDATE_INT);
    controller.update(valueDescriptor2);
  }

  private void checkUpdatedValueDescriptor(ValueDescriptor valueDescriptor) {
    assertEquals("ValueDescriptor ID does not match saved id", testValDescId,
        valueDescriptor.getId());
    assertEquals("ValueDescriptor name does not match saved name", TEST_NAME,
        valueDescriptor.getName());
    assertEquals("ValueDescriptor min does not match saved min", UPDATE_INT,
        valueDescriptor.getMin());
    assertEquals("ValueDescriptor max does not match saved max", UPDATE_INT,
        valueDescriptor.getMax());
    assertEquals("ValueDescriptor type does not match saved type", IoTType.I,
        valueDescriptor.getType());
    assertEquals("ValueDescriptor label does not match saved label", UPDATE_STRING,
        valueDescriptor.getUomLabel());
    assertEquals("ValueDescriptor default value does not match saved default value", UPDATE_INT,
        valueDescriptor.getDefaultValue());
    assertEquals("ValueDescriptor formatting does not match saved formatting", UPDATE_FMT_STRING,
        valueDescriptor.getFormatting());
    assertArrayEquals("ValueDescriptor labels does not match saved labels", UPDATE_STRING_ARRAY,
        valueDescriptor.getLabels());
    assertEquals("ValueDescriptor origin does not match saved origin", UPDATE_INT,
        valueDescriptor.getOrigin());
    assertNotNull("ValueDescriptor modified date is null", valueDescriptor.getModified());
    assertNotNull("ValueDescriptor create date is null", valueDescriptor.getCreated());
    assertTrue(valueDescriptor.getModified() != valueDescriptor.getCreated());
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateValueDescriptorWithUnknownId() {
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setId("unknownId");
    controller.update(valueDescriptor2);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateValueDescriptorWithUnknownName() {
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setName("unknownId");
    controller.update(valueDescriptor2);
  }

  @Test(expected = ServiceException.class)
  public void testUpdateValueDescriptorException() throws Exception {
    unsetControllerRepos();
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setId(testValDescId);
    controller.update(valueDescriptor2);
  }

  @Test(expected = DataValidationException.class)
  public void testUpdateValueDescriptorWithAssociatedReading() {
    Reading reading = new Reading(TEST_NAME, "somevalue");
    reading.setOrigin(TEST_ORIGIN);
    reading.setPushed(TEST_PUSHED);
    readingRepos.save(reading);
    ValueDescriptor valueDescriptor2 = new ValueDescriptor();
    valueDescriptor2.setName("foo");
    valueDescriptor2.setId(testValDescId);
    controller.update(valueDescriptor2);
  }

  // use Java reflection to unset controller's repos
  private void unsetControllerRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("valDescRepos");
    temp.setAccessible(true);
    temp.set(controller, null);
  }

  // use Java reflection to reset controller's repos
  private void resetControllerRepos() throws Exception {
    Class<?> controllerClass = controller.getClass();
    Field temp = controllerClass.getDeclaredField("valDescRepos");
    temp.setAccessible(true);
    temp.set(controller, repos);
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
