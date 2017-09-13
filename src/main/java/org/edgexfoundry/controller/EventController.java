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

import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface EventController {

  /**
   * Fetch a specific event by database specified id. Note: does not yet handle device managers.
   * NotFoundException (HTTP 404) if no events are found by the id.ServiceException (HTTP 503) for
   * unknown or unanticipated issues
   * 
   * @param id event identifier (database generated id)
   * @return event
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues NotFoundException (HTTP
   *         404) if an event cannot be found by the id
   */
  Event event(@PathVariable String id);

  /**
   * Fetch all events with their associated readings. LimitExceededException (HTTP 413) if the
   * number of events exceeds the current max limit. ServiceException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @return list of events
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of events exceeds the current max limit
   */
  List<Event> events();

  /**
   * Return all events between a given begin and end date/time (in the form of longs).
   * LimitExceededException (HTTP 413) if the number of events exceeds the current max limit.
   * ServiceException (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param start - start date in long form
   * @param end - end date in long form
   * @param limit - maximum number of events to fetch, must be < max limit
   * @return list of events between the specified dates
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of events exceeds the current max limit
   */
  List<Event> events(@PathVariable long start, @PathVariable long end, @PathVariable int limit);

  /**
   * Return a count of the number of events in core data for a given device (identified by database
   * or unique name).
   * 
   * @param deviceId - the id (database generated id) or name of the device associated to events
   * 
   * @return long - a count of total events in core data for the given device
   */
  long eventCountForDevice(@PathVariable String deviceId);

  /**
   * Return a count of the number of events in core data.
   * 
   * @return long - a count of total events in core data
   */
  long eventCount();

  /**
   * Return list of events with their associated readings for a given device, sort by event creation
   * date. Note: does not yet handle device managers. LimitExceededException (HTTP 413) if the
   * number of events exceeds the current max limit. ServiceException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param deviceId - the id (database generated id) or name of the device associated to events
   * @param limit - maximum number of events to fetch, must be < max limit
   * @return list of events associated to the matching device by id - limited in size by the limit
   *         parameter
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of events exceeds the current max limit
   */
  List<Event> eventsForDevice(@PathVariable String deviceId, @PathVariable int limit);

  List<Reading> readingsForDeviceAndValueDescriptor(@PathVariable String deviceId,
      @PathVariable String valuedescriptor, @PathVariable int limit);

  /**
   * Add a new event (with its associated readings). Prefers the event device is a device name but
   * can also be a device id (database generated). DataValidationException (HTTP 409) if the a
   * reading is associated to a non-existent value descriptor or device is null. ServiceException
   * (HTTP 503) for unknown or unanticipated issues.
   * 
   * @param event - event object with associated readings
   * @return new event database generated id
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws DataValidationException (HTTP 409) if one of the readings associated to the new event
   *         contains a non-existent value descriptor.
   */
  String add(@RequestBody Event event);

  /**
   * Delete an event and all its readings given its database generated id. NotFoundException (HTTP
   * 404) if the event cannot be found by id. ServiceException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param id database id for the event
   * @return boolean on success of deletion request
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws NotFoundException (HTTP 404) when the event cannot be found by the id
   */
  boolean delete(@PathVariable String id);

  /**
   * Delete all events (and their readings) associated to a device given the device's id (either
   * database generated id or name). ServiceException (HTTP 503) for unknown or unanticipated
   * issues.
   * 
   * @param deviceId - the id (database generated id) or name of the device associated to events
   * @return count of the number of events deleted
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   */
  int deleteByDevice(@PathVariable String deviceId);

  /**
   * Update the event data (not including updating the readings). NotFoundException (HTTP 404) if
   * the event cannot be found by id. ServiceException (HTTP 503) for unknown or unanticipated
   * issues.
   * 
   * @param event - event object containing the new event data and the database generated id
   * @return boolean on success of update request
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws NotFoundException (HTTP 404) when the event cannot be found by the id
   */
  boolean update(@RequestBody Event event2);

  /**
   * Update the event to be pushed by setting the pushed timestamp to the current time.
   * NotFoundException (HTTP 404) if the event cannot be found by id. ServiceException (HTTP 503)
   * for unknown or unanticipated issues.
   * 
   * @param id - database generated id for the event
   * @return boolean on success of update request
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws NotFoundException (HTTP 404) when the event cannot be found by the id
   */
  boolean markPushed(@PathVariable String id);

  /**
   * Remove all pushed events and their associated readings.ServiceException (HTTP 503) for unknown
   * or unanticipated issues.
   * 
   * @return count of the number of events scrubbed
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   */
  long scrubPushedEvents();

  /**
   * Undocumented feature to scrub all events/readings for dev/testing or in emergency situations
   * such as the DB filing quickly.
   * 
   * @return boolean indicating success of the operation.
   */
  boolean scrubAllEventsReadings();

  /**
   * Remove all old events (and associated readings) based on delimiting age.
   * 
   * @param age - minimum age in milliseconds (from created timestamp) an event should be in order
   *        to be removed
   * @return - count of the number of events removed
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * 
   */
  long scrubOldEvents(@PathVariable long age);


}
