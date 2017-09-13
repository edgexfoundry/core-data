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

import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReadingController {

  /**
   * Retrieve a reading by its database generated id.
   * 
   * @param id id (database generated id)
   * @return Reading
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues NotFoundException (HTTP
   *         404) if a reading cannot be found by the id provided.
   */
  Reading reading(@PathVariable String id);

  /**
   * Return list of all readings. Sorts by reading id. LimitExceededException (HTTP 413) if the
   * number of readings exceeds the current max limit. ServiceException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @return list of all readings
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readings();

  /**
   * Return list of all readings for a given device, sort by reading creation date. Note: does not
   * yet handle device managers. LimitExceededException (HTTP 413) if the number of readings exceeds
   * the current max limit. ServiceException (HTTP 503) for unknown or unanticipated issues.
   * NotFoundException (HTTP 404) if meta checks are in place and if the device id or name does not
   * match any existing devices.
   * 
   * @param device database generated identifier or device name
   * @param limit - maximum number of readings to fetch, must be < MAX_LIMIT
   * @return List of readings for a device, could be an empty list if none match
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readings(@PathVariable String deviceId, @PathVariable int limit);

  /**
   * Return a list of readings between two timestamps - limited by the number specified in the limit
   * parameter. LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   * limit. ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param start - millisecond (long) timestamp of the beginning of the time range
   * @param end - millisecond (long) timestamp of the end of the time rage
   * @param limit - maximum number of readings to be allowed to be returned
   * @return - list of matching readings in this range (limited by the limit parameter)
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readings(@PathVariable long start, @PathVariable long end, @PathVariable int limit);

  /**
   * Return a count of the number of readings in core data
   * 
   * @return long - a count of total readings in core data
   */
  long readingCount();

  /**
   * Return a list of readings that are associated to a ValueDescripter by name.
   * LimitExceededException (HTTP 413) if the number of readings exceeds the current max limit.
   * ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param valueDescriptorName - name of the matching ValueDescriptor
   * @param limit - maximum number of readings to return (must not exceed max limit)
   * @return - list of readings matching on the value descriptor name
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readingsByName(@PathVariable String name, @PathVariable int limit);

  /**
   * Return a list of readings that are associated to a ValueDescripter and Device by name.
   * LimitExceededException (HTTP 413) if the number of readings exceeds the current max limit.
   * ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param valueDescriptorName - name of the matching ValueDescriptor
   * @param device name - name or id of the matching device associated to the event/reading
   * @param limit - maximum number of readings to return (must not exceed max limit)
   * @return - list of readings matching on the value descriptor and device name
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readingsByNameAndDevice(@PathVariable String name, @PathVariable String device,
      @PathVariable int limit);

  /**
   * Return a list of readings with an associated value descriptor of the UoM label specified.
   * LimitExceededException (HTTP 413) if the number of readings exceeds the current max limit.
   * ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param uomLabel - Unit of Measure label (UoMLabel) matching the Value Descriptor associated to
   *        the reading
   * @param limit - maximum number of readings to be allowed to be returned
   * @return - list of matching readings having value descriptor with UoM label specified
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readingsByUomLabel(@PathVariable String uomLabel, @PathVariable int limit);

  /**
   * Return a list of readings with an associated value descriptor of the label specified.
   * LimitExceededException (HTTP 413) if the number of readings exceeds the current max limit.
   * ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param label - String label that should be in matching Value Descriptor's label array
   * @param limit - maximum number of readings to be allowed to be returned
   * @return - list of matching readings having value descriptor with the associated label. Could be
   *         an empty list if none match.
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readingsByLabel(@PathVariable String label, @PathVariable int limit);

  /**
   * Return a list of readings with an associated value descriptor of the type (IoTType) specified.
   * LimitExceededException (HTTP 413) if the number of readings exceeds the current max limit.
   * ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param type - an IoTType in string form (one of I, B, F, S for integer, Boolean, Floating point
   *        or String)
   * @param limit - maximum number of readings to be allowed to be returned
   * @return - list of matching readings having value descriptor of the types specified. Could be an
   *         empty list if none match.
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of readings exceeds the current max
   *         limit
   */
  List<Reading> readingsByType(@PathVariable String type, @PathVariable int limit);

  /**
   * Add a new reading. ServiceException (HTTP 503) for unknown or unanticipated issues.
   * DataValidationException if the associated value descriptor is non-existent.
   * 
   * @param reading - Reading object
   * @return String id (database id) of the new Reading
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if one of the readings associated to the new event
   *         contains a non-existent value descriptor.
   */
  String add(@RequestBody Reading reading);

  /**
   * Update the reading. NotFoundException (HTTP 404) if the reading cannot be found by id.
   * ServiceException (HTTP 503) for unknown or unanticipated issues. DataValidationException if the
   * associated value descriptor is non-existent.
   * 
   * @param reading2 - Reading object containing update data and the id of the reading to be updated
   * @return boolean indicating success of the update operation
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if one of the readings associated to the new event
   *         contains a non-existent value descriptor.
   * @throws NotFoundException (HTTP 404) if the reading cannot be located by the provided id in the
   *         reading.
   */
  boolean update(@RequestBody Reading reading2);

  /**
   * Delete the reading from persistent storage. NotFoundException (HTTP 404) if the reading cannot
   * be found by id. ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param id - database generated id of the reading to be deleted
   * @return boolean indicating success of the delete operation
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws NotFoundException (HTTP 404) if the reading cannot be located by the provided id in the
   *         reading.
   */
  boolean delete(@PathVariable String id);

}
