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

import static org.edgexfoundry.test.data.CommonData.TEST_DESCRIPTION;
import static org.edgexfoundry.test.data.CommonData.TEST_ORIGIN;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_DEF_VALUE;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_FORMATTING;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_LABELS;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_MAX;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_MIN;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_NAME;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_TYPE;
import static org.edgexfoundry.test.data.ValueDescriptorData.TEST_UOMLABEL;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.IoTType;
import org.edgexfoundry.domain.common.ValueDescriptor;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class})
public class ValueDescriptorRepositoryTest {

  @Autowired
  private ValueDescriptorRepository valDescRepos;
  private String testValDescId;

  /**
   * Create and save an instance of the ValueDescriptor before each test Note: the before method
   * tests the save operation of the Repository
   */
  @Before
  public void setup() {
    ValueDescriptor valDesc = new ValueDescriptor(TEST_NAME, TEST_MIN, TEST_MAX, TEST_TYPE,
        TEST_UOMLABEL, TEST_DEF_VALUE, TEST_FORMATTING, TEST_LABELS, TEST_DESCRIPTION);
    valDesc.setOrigin(TEST_ORIGIN);
    valDescRepos.save(valDesc);
    testValDescId = valDesc.getId();
    assertNotNull("Saved ValueDescriptor does not have an id", testValDescId);
  }

  /**
   * Clean up of the unit test Note: clean up also tests the delete operation of the repository
   */
  @After
  public void cleanup() {
    valDescRepos.deleteAll();
    assertNull("ValueDescriptor not deleted as part of cleanup",
        valDescRepos.findOne(testValDescId));
  }

  @Test(expected = DuplicateKeyException.class)
  public void testUniqueNameFail() {
    // create second value descriptor with all the same data to include
    // non-unique name
    ValueDescriptor valDesc2 = new ValueDescriptor(TEST_NAME, TEST_MIN, TEST_MAX, TEST_TYPE,
        TEST_UOMLABEL, TEST_DEF_VALUE, TEST_FORMATTING, TEST_LABELS, TEST_DESCRIPTION);
    valDescRepos.save(valDesc2);
  }

  @Test
  public void testUpdate() {
    ValueDescriptor valueDescriptor = valDescRepos.findOne(testValDescId);
    // check that create and modified timestamps are the same for a new
    // ValueDescriptor record
    assertEquals("Modified and created timestamps should be equal after creation",
        valueDescriptor.getModified(), valueDescriptor.getCreated());
    valueDescriptor.setMin(-200);
    valDescRepos.save(valueDescriptor);
    // reread value descriptor
    ValueDescriptor valueDescriptor2 = valDescRepos.findOne(testValDescId);
    assertEquals("min was not updated appropriately in ValueDescriptor update", -200,
        valueDescriptor2.getMin());
    assertNotEquals(
        "after modification, modified timestamp still the same as the value descriptor's create timestamp",
        valueDescriptor2.getModified(), valueDescriptor2.getCreated());
  }

  @Test
  public void testFindOne() {
    ValueDescriptor vD = valDescRepos.findOne(testValDescId);
    assertNotNull("Find one returns no value descriptors", vD);
    checkTestValueDescriptorData(vD);
  }

  @Test
  public void testFindAll() {
    List<ValueDescriptor> valDescriptors = valDescRepos.findAll();
    assertEquals("Find all not returning a list with one value descriptor", 1,
        valDescriptors.size());
    checkTestValueDescriptorData(valDescriptors.get(0));
  }

  @Test
  public void testFindByName() {
    ValueDescriptor valueDescriptor = valDescRepos.findByName(TEST_NAME);
    assertNotNull("find by name not returning any value descriptors", valueDescriptor);
    checkTestValueDescriptorData(valueDescriptor);
  }

  @Test
  public void testFindByNameWithNoMatching() {
    ValueDescriptor valueDescriptor = valDescRepos.findByName("noname");
    assertNull("find by name with bad name is returning value descriptor", valueDescriptor);
  }

  @Test
  public void testFindByUomLabel() {
    List<ValueDescriptor> valueDescriptors = valDescRepos.findByUomLabel(TEST_UOMLABEL);
    assertEquals("find by UOM label not returning any value descriptors", 1,
        valueDescriptors.size());
    checkTestValueDescriptorData(valueDescriptors.get(0));
  }

  @Test
  public void testFindByUomLabelWithNoMatching() {
    List<ValueDescriptor> valueDescriptors = valDescRepos.findByUomLabel("nolabel");
    assertTrue("find by uom label with bad uom label is returning value descriptors",
        valueDescriptors.isEmpty());
  }

  @Test
  public void testFindByLabel() {
    List<ValueDescriptor> valueDescriptors = valDescRepos.findByLabelsIn(TEST_LABELS[0]);
    assertEquals("find by label not returning any value descriptors", 1, valueDescriptors.size());
    checkTestValueDescriptorData(valueDescriptors.get(0));
  }

  @Test
  public void testFindByLabelWithNoMatching() {
    List<ValueDescriptor> valueDescriptors = valDescRepos.findByLabelsIn("foobar");
    assertTrue("find by label with bad label is returning value descriptors",
        valueDescriptors.isEmpty());
  }

  @Test
  public void testFindByType() {
    List<ValueDescriptor> valueDescriptors = valDescRepos.findByType(TEST_TYPE);
    assertEquals("find by type not returning any value descriptors", 1, valueDescriptors.size());
    checkTestValueDescriptorData(valueDescriptors.get(0));
  }

  @Test
  public void testFindByTypeWithNoMatching() {
    List<ValueDescriptor> valueDescriptors = valDescRepos.findByType(IoTType.F);
    assertTrue("find by type with bad type is returning value descriptors",
        valueDescriptors.isEmpty());
  }

  private void checkTestValueDescriptorData(ValueDescriptor valueDescriptor) {
    assertEquals("ValueDescriptor ID does not match saved id", testValDescId,
        valueDescriptor.getId());
    assertEquals("ValueDescriptor name does not match saved name", TEST_NAME,
        valueDescriptor.getName());
    assertEquals("ValueDescriptor min does not match saved min", TEST_MIN,
        valueDescriptor.getMin());
    assertEquals("ValueDescriptor max does not match saved max", TEST_MAX,
        valueDescriptor.getMax());
    assertEquals("ValueDescriptor type does not match saved type", TEST_TYPE,
        valueDescriptor.getType());
    assertEquals("ValueDescriptor label does not match saved label", TEST_UOMLABEL,
        valueDescriptor.getUomLabel());
    assertEquals("ValueDescriptor default value does not match saved default value", TEST_DEF_VALUE,
        valueDescriptor.getDefaultValue());
    assertEquals("ValueDescriptor formatting does not match saved formatting", TEST_FORMATTING,
        valueDescriptor.getFormatting());
    assertArrayEquals("ValueDescriptor labels does not match saved labels", TEST_LABELS,
        valueDescriptor.getLabels());
    assertEquals("ValueDescriptor origin does not match saved origin", TEST_ORIGIN,
        valueDescriptor.getOrigin());
    assertNotNull("ValueDescriptor modified date is null", valueDescriptor.getModified());
    assertNotNull("ValueDescriptor create date is null", valueDescriptor.getCreated());
  }

}
