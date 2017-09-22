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

package org.edgexfoundry.controller.impl;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.controller.EventController;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ScrubDao;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.core.Reading;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/event")
public class EventControllerImpl implements EventController {
  // TODO - someday, check each reading values match value descriptor per
  // reading's name on add

  private static final String ERR_GETTING = "Error getting events:  ";

  private static final String LIMIT_ON_EVENT = "Event";

  private static final String SORT_CREATED = "created";

  private static final org.edgexfoundry.support.logging.client.EdgeXLogger logger =
      org.edgexfoundry.support.logging.client.EdgeXLoggerFactory
          .getEdgeXLogger(EventControllerImpl.class);

  @Autowired
  ReadingRepository readingRepos;

  @Autowired
  EventRepository eventRepos;

  @Autowired
  ValueDescriptorRepository valDescRepos;

  @Autowired
  ScrubDao scrubDao;

  @Autowired
  ThreadTasks tasker;

  @Autowired
  DeviceClient deviceClient;

  @Value("${read.max.limit}")
  private int maxLimit;

  @Value("${metadata.check}")
  private boolean metaCheck;

  // persist data to the database - alternate is to just stream it to the
  // event queue
  @Value("${persist.data}")
  private boolean persistData = true;

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
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @Override
  public Event event(@PathVariable String id) {
    try {
      Event e = eventRepos.findOne(id);
      if (e == null)
        throw new NotFoundException(Event.class.toString(), id);
      return e;
    } catch (NotFoundException nfE) {
      throw nfE;
    } catch (Exception e) {
      logger.error("Error getting event:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Fetch all events with their associated readings. LimitExceededException (HTTP 413) if the
   * number of events exceeds the current max limit. ServiceException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @return list of events
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * @throws LimitExceededException (HTTP 413) if the number of events exceeds the current max limit
   */
  @RequestMapping(method = RequestMethod.GET)
  @Override
  public List<Event> events() {
    if (eventRepos != null && eventRepos.count() > maxLimit)
      throw new LimitExceededException(LIMIT_ON_EVENT);
    try {
      Sort sort = new Sort(Sort.Direction.DESC, "_id");
      return eventRepos.findAll(sort);
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/{start}/{end}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Event> events(@PathVariable long start, @PathVariable long end,
      @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_EVENT);
    try {
      PageRequest request;
      request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, SORT_CREATED));
      return eventRepos.findByCreatedBetween(start, end, request).getContent();
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Return a count of the number of events in core data for a given device (identified by database
   * or unique name).
   * 
   * @param deviceId - the id (database generated id) or name of the device associated to events
   * 
   * @return long - a count of total events in core data for the given device
   */
  @RequestMapping(value = "/count/{deviceId:.+}", method = RequestMethod.GET)
  @Override
  public long eventCountForDevice(@PathVariable String deviceId) {
    try {
      return eventRepos.findByDevice(deviceId).size();
    } catch (Exception e) {
      logger.error("Error getting event count:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Return a count of the number of events in core data.
   * 
   * @return long - a count of total events in core data
   */
  @RequestMapping(value = "/count", method = RequestMethod.GET)
  @Override
  public long eventCount() {
    try {
      return eventRepos.count();
    } catch (Exception e) {
      logger.error("Error getting event count:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/device/{deviceId:.+}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Event> eventsForDevice(@PathVariable String deviceId, @PathVariable int limit) {
    checkDevice(deviceId);
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_EVENT);
    try {
      PageRequest request;
      request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, SORT_CREATED));
      return eventRepos.findByDevice(deviceId, request).getContent();
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

  @RequestMapping(value = "/device/{deviceId:.+}/valuedescriptor/{valuedescriptor:.+}/{limit}")
  @Override
  public List<Reading> readingsForDeviceAndValueDescriptor(@PathVariable String deviceId,
      @PathVariable String valuedescriptor, @PathVariable int limit) {
    checkDevice(deviceId);
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_EVENT);
    try {
      PageRequest request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, SORT_CREATED));
      List<Event> events = eventRepos.findByDevice(deviceId, request).getContent();
      return events.stream().flatMap(e -> e.getReadings().stream())
          .filter(r -> r.getName().equals(valuedescriptor)).collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Error getting readings for device and value descriptor:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.POST)
  @Override
  public String add(@RequestBody Event event) {
    checkDevice(event.getDevice());
    try {
      if (persistData) {
        if (event.getReadings() != null) {
          for (Reading reading : event.getReadings()) {
            if (valDescRepos.findByName(reading.getName()) == null)
              throw new DataValidationException(
                  "Non-existent value descriptor specified in reading");
            readingRepos.save(reading);
          }
        }
        eventRepos.save(event);
      } else {
        event.setId("unsaved");
      }
      tasker.putEventOnQueue(event);
      tasker.updateDeviceLastReportedConnected(event.getDevice());
      tasker.updateDeviceServiceLastReportedConnected(event.getDevice());
      return event.getId();
    } catch (DataValidationException dE) {
      throw dE;
    } catch (Exception e) {
      logger.error("Error adding event:  " + e.getMessage());
      throw new ServiceException(e);
    }

  }

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
  @RequestMapping(value = "/id/{id}", method = RequestMethod.DELETE)
  @Override
  public boolean delete(@PathVariable String id) {
    try {
      Event event = eventRepos.findOne(id);
      if (event != null) {
        deleteEvent(event);
        return true;
      } else {
        logger.error("Request to delete with non-existent event:  " + id);
        throw new NotFoundException(Event.class.toString(), id);
      }
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error removing an event:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Delete all events (and their readings) associated to a device given the device's id (either
   * database generated id or name). ServiceException (HTTP 503) for unknown or unanticipated
   * issues.
   * 
   * @param deviceId - the id (database generated id) or name of the device associated to events
   * @return count of the number of events deleted
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   */
  @RequestMapping(value = "/device/{deviceId:.+}", method = RequestMethod.DELETE)
  @Override
  public int deleteByDevice(@PathVariable String deviceId) {
    checkDevice(deviceId);
    try {
      List<Event> events = eventRepos.findByDevice(deviceId);
      events.stream().parallel().forEach(e -> deleteEvent(e));
      return events.size();
    } catch (Exception e) {
      logger.error("Error removing an event by device identifier:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.PUT)
  @Override
  public boolean update(@RequestBody Event event2) {
    try {
      Event event = eventRepos.findOne(event2.getId());
      if (event != null) {
        if (event2.getDevice() != null) {
          if (metaCheck && (deviceClient.deviceForName(event2.getDevice()) == null
              && deviceClient.device(event2.getDevice()) == null))
            throw new NotFoundException(Device.class.toString(), event2.getDevice());
          event.setDevice(event2.getDevice());
        }
        if (event2.getPushed() != 0)
          event.setPushed(event2.getPushed());
        if (event2.getOrigin() != 0)
          event.setOrigin(event2.getOrigin());
        eventRepos.save(event);
        return true;
      } else {
        logger.error("Request to update with non-existent event:  " + event2.getId());
        throw new NotFoundException(Event.class.toString(), event2.getId());
      }
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error updating an event:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/id/{id}", method = RequestMethod.PUT)
  @Override
  public boolean markPushed(@PathVariable String id) {
    try {
      Event event = eventRepos.findOne(id);
      if (event != null) {
        long now = Calendar.getInstance().getTimeInMillis();
        if (event.getReadings() != null) {
          event.markPushed(now);
          for (Reading reading : event.getReadings()) {
            if (valDescRepos.findByName(reading.getName()) == null)
              throw new DataValidationException(
                  "Non-existent value descriptor specified in reading");
            readingRepos.save(reading);
          }
        }
        eventRepos.save(event);
        return true;
      } else {
        logger.error("Request to update with non-existent event:  " + id);
        throw new NotFoundException(Event.class.toString(), id);
      }
    } catch (NotFoundException nE) {
      throw nE;

    } catch (Exception e) {
      logger.error("Error marking an event pushed:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Remove all pushed events and their associated readings.ServiceException (HTTP 503) for unknown
   * or unanticipated issues.
   * 
   * @return count of the number of events scrubbed
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   */
  @RequestMapping(value = "/scrub", method = RequestMethod.DELETE)
  @Override
  public long scrubPushedEvents() {
    try {
      return scrubDao.scrubPushedEvents();
    } catch (Exception e) {
      logger.error("Error scrubbing pushed events:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Undocumented feature to scrub all events/readings for dev/testing or in emergency situations
   * such as the DB filing quickly.
   * 
   * @return boolean indicating success of the operation.
   */
  @RequestMapping(value = "/scruball", method = RequestMethod.DELETE)
  @Override
  public boolean scrubAllEventsReadings() {
    try {
      readingRepos.deleteAll();
      eventRepos.deleteAll();
      return true;
    } catch (Exception e) {
      logger.error("Error scrubbing all events/readings:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Remove all old events (and associated readings) based on delimiting age.
   * 
   * @param age - minimum age in milliseconds (from created timestamp) an event should be in order
   *        to be removed
   * @return - count of the number of events removed
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues
   * 
   */
  @RequestMapping(value = "/removeold/age/{age}", method = RequestMethod.DELETE)
  @Override
  public long scrubOldEvents(@PathVariable long age) {
    try {
      return scrubDao.scrubOldEvents(age);
    } catch (Exception e) {
      logger.error("Error scrubbing old events:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  private void deleteEvent(Event event) {
    deleteAssociatedReadings(event);
    eventRepos.delete(event);
  }

  private void deleteAssociatedReadings(Event event) {
    List<Reading> readings = event.getReadings();
    if (readings != null) {
      readings.stream().parallel().forEach(r -> readingRepos.delete(r));
    }
  }

  private void checkDevice(String deviceId) {
    if (deviceId == null) {
      logger.error("Event must be associated to a device");
      throw new DataValidationException("Event must be associated to a device");
    }
    try {
      if (metaCheck && (deviceClient.deviceForName(deviceId) == null
          && deviceClient.device(deviceId) == null)) {
        logger.error("No device found for associated device id");
        throw new NotFoundException(Device.class.toString(), deviceId);
      }
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error(
          "Unknown issue when trying to find associated device for: " + deviceId + e.getMessage());
      throw new ServiceException(e);
    }
  }

}
