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

import java.util.List;

import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ValueDescriptorController {

  /**
   * Fetch a specific ValueDescriptor by its database generated id. NotFoundException (HTTP 404) if
   * no value descriptor can be found by the id provided. ServcieException (HTTP 503) for unknown or
   * unanticipated issues
   * 
   * @param String ValueDescriptor id (ObjectId)
   * 
   * @return ValueDescriptor
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues NotFoundException (HTTP
   *         404) if not found by id.
   */
  ValueDescriptor valueDescriptor(@PathVariable String id);

  /**
   * Return all ValueDescriptor objects. LimitExceededException (HTTP 413) if the number of value
   * descriptors exceeds the current max limit. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @return list of ValueDescriptors
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of value descriptors exceeds the
   *         current max limit
   */
  List<ValueDescriptor> valueDescriptors();

  /**
   * Return ValueDescriptor object with given name. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param name of the value descriptor to locate and return
   * @return ValueDescriptor having the provided name, could be null if none are found.
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   */
  ValueDescriptor valueDescriptorByName(@PathVariable String name);

  /**
   * Return ValueDescriptor objects with given UoM label. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param uomLabel
   * @return list of ValueDescriptor matching UoM Label
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   */
  List<ValueDescriptor> valueDescriptorByUoMLabel(@PathVariable String uomLabel);

  /**
   * Return ValueDescriptor objects with given label. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param label
   * @return list of ValueDescriptor matching UoM Label
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   */
  List<ValueDescriptor> valueDescriptorByLabel(@PathVariable String label);

  /**
   * Retrieve value descriptors associated to a device where the device is identified by name - that
   * is value descriptors that are listed as part of a devices parameter names on puts or expected
   * values on get or put commands. Throws a ServcieException (HTTP 503) for unknown or
   * unanticipated issues. Throws NotFoundExcetption (HTTP 404) if a device cannot be found for the
   * name provided.
   * 
   * @param name - name of the device
   * @return list of value descriptors associated to the device.
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws NotFoundException (HTTP 404) for device not found by name
   */
  List<ValueDescriptor> valueDescriptorsForDeviceByName(@PathVariable String name);

  /**
   * Retrieve value descriptors associated to a device where the device is identified by id - that
   * is value descriptors that are listed as part of a devices parameter names on puts or expected
   * values on get or put commands. Throws a ServcieException (HTTP 503) for unknown or
   * unanticipated issues. Throws NotFoundExcetption (HTTP 404) if a device cannot be found for the
   * id provided.
   * 
   * @param name - name of the device
   * @return list of value descriptors associated to the device.
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws NotFoundException (HTTP 404) for device not found by id
   */
  List<ValueDescriptor> valueDescriptorsForDeviceById(@PathVariable String id);

  /**
   * Add a new ValueDescriptor whose name must be unique. ServcieException (HTTP 503) for unknown or
   * unanticipated issues. DataValidationException (HTTP 409) if the a formatting string of the
   * value descriptor is not a valid printf format or if the name is determined to not be unique
   * with regard to other value descriptors.
   * 
   * @param valueDescriptor object
   * @return id of the new ValueDescriptor
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if the format string is not valid or name not unique
   */
  String add(@RequestBody ValueDescriptor valueDescriptor);

  /**
   * Update the ValueDescriptor identified by the id or name in the object provided. Id is used
   * first, name is used second for identification purposes. ServcieException (HTTP 503) for unknown
   * or unanticipated issues. DataValidationException (HTTP 409) if the a formatting string of the
   * value descriptor is not a valid printf format. NotFoundException (404) if the value descriptor
   * cannot be located by the identifier.
   * 
   * @param valueDescriptor2 - object holding the identifier and new values for the ValueDescriptor
   * @return boolean indicating success of the update
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if the format string is not valid
   * @throws NotFoundException (HTTP 404) when the value descriptor cannot be found by the id
   */
  boolean update(@RequestBody ValueDescriptor valueDescriptor2);

  /**
   * Remove the ValueDescriptor designated by database generated identifier. ServcieException (HTTP
   * 503) for unknown or unanticipated issues. DataValidationException (HTTP 409) if the value
   * descriptor is still referenced in Readings. NotFoundException (404) if the value descriptor
   * cannot be located by the identifier.
   * 
   * @param identifier (database generated) of the value descriptor to be deleted
   * @return boolean indicating success of the remove operation
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if the value descriptor is still referenced by
   *         readings
   * @throws NotFoundException (HTTP 404) when the value descriptor cannot be found by the id
   */
  boolean delete(@PathVariable String id);

  /**
   * Remove the ValueDescriptor designated by name. ServcieException (HTTP 503) for unknown or
   * unanticipated issues. DataValidationException (HTTP 409) if the value descriptor is still
   * referenced in Readings. NotFoundException (404) if the value descriptor cannot be located by
   * the identifier.
   * 
   * @param name of the value descriptor to be removed.
   * @return boolean indicating success of the remove operation
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if the value descriptor is still referenced by
   *         readings
   * @throws NotFoundException (HTTP 404) when the value descriptor cannot be found by the id
   */
  boolean deleteByName(@PathVariable String name);

}
