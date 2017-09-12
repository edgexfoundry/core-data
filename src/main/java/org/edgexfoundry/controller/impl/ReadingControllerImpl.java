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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.controller.ReadingController;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.IoTType;
import org.edgexfoundry.domain.common.ValueDescriptor;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reading")
public class ReadingControllerImpl implements ReadingController {

  // TODO - someday check value against value desc, per name on adds/updates

  private static final String ERR_GETTING = "Error getting readings:  ";

  private static final String LIMIT_ON_READING = "Reading";

  private static final String SORT_CREATED = "created";

  private static final org.edgexfoundry.support.logging.client.EdgeXLogger logger =
      org.edgexfoundry.support.logging.client.EdgeXLoggerFactory
          .getEdgeXLogger(ReadingControllerImpl.class);

  @Autowired
  ReadingRepository readingRepos;

  @Autowired
  EventRepository eventRepos;

  @Autowired
  ValueDescriptorRepository valDescRepos;

  @Autowired
  MongoTemplate template;

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
   * Retrieve a reading by its database generated id.
   * 
   * @param id id (database generated id)
   * @return Reading
   * @throws ServiceException (HTTP 503) for unknown or unanticipated issues NotFoundException (HTTP
   *         404) if a reading cannot be found by the id provided.
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @Override
  public Reading reading(@PathVariable String id) {
    try {
      Reading reading = readingRepos.findOne(id);
      if (reading == null)
        throw new NotFoundException(Reading.class.toString(), id);
      return reading;
    } catch (NotFoundException nfE) {
      throw nfE;
    } catch (Exception e) {
      logger.error("Error getting reading:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.GET)
  @Override
  public List<Reading> readings() {
    if (readingRepos != null && readingRepos.count() > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      return readingRepos.findAll();
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/device/{deviceId}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readings(@PathVariable String deviceId, @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    if (metaCheck
        && (deviceClient.deviceForName(deviceId) == null && deviceClient.device(deviceId) == null))
      throw new NotFoundException(Device.class.toString(), deviceId);
    try {
      PageRequest request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, SORT_CREATED));
      List<Event> events = eventRepos.findByDevice(deviceId, request).getContent();
      if (events == null || events.isEmpty())
        return new ArrayList<>();
      return events.stream().map((Event e) -> e.getReadings()).flatMap(l -> l.stream()).limit(limit)
          .sorted().collect(Collectors.toList());
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/{start}/{end}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readings(@PathVariable long start, @PathVariable long end,
      @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      PageRequest request;
      request = new PageRequest(0, limit, new Sort(Sort.Direction.DESC, SORT_CREATED));
      return readingRepos.findByCreatedBetween(start, end, request).getContent();
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Return a count of the number of readings in core data
   * 
   * @return long - a count of total readings in core data
   */
  @RequestMapping(value = "/count", method = RequestMethod.GET)
  @Override
  public long readingCount() {
    try {
      return readingRepos.count();
    } catch (Exception e) {
      logger.error("Error getting reading count:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/name/{name:.+}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readingsByName(@PathVariable String name, @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      PageRequest request =
          new PageRequest(0, determineLimit(limit), new Sort(Sort.Direction.DESC, SORT_CREATED));
      return readingRepos.findByName(name, request).getContent();
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/name/{name:.+}/device/{device:.+}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readingsByNameAndDevice(@PathVariable String name,
      @PathVariable String device, @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      PageRequest request =
          new PageRequest(0, determineLimit(limit), new Sort(Sort.Direction.DESC, SORT_CREATED));
      return readingRepos.findByNameAndDevice(name, device, request).getContent();
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/uomlabel/{uomLabel:.+}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readingsByUomLabel(@PathVariable String uomLabel, @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      List<ValueDescriptor> valDescs = valDescRepos.findByUomLabel(uomLabel);
      if (valDescs.isEmpty())
        return new ArrayList<>();
      return filterReadings(valDescs, determineLimit(limit));
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/label/{label:.+}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readingsByLabel(@PathVariable String label, @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      List<ValueDescriptor> valDescs = valDescRepos.findByLabelsIn(label);
      if (valDescs.isEmpty())
        return new ArrayList<>();
      return filterReadings(valDescs, determineLimit(limit));
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/type/{type:.+}/{limit}", method = RequestMethod.GET)
  @Override
  public List<Reading> readingsByType(@PathVariable String type, @PathVariable int limit) {
    if (limit > maxLimit)
      throw new LimitExceededException(LIMIT_ON_READING);
    try {
      List<ValueDescriptor> valDescs = valDescRepos.findByType(IoTType.valueOf(type));
      if (valDescs.isEmpty())
        return new ArrayList<>();
      return filterReadings(valDescs, determineLimit(limit));
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.POST)
  @Override
  public String add(@RequestBody Reading reading) {
    if (valDescRepos.findByName(reading.getName()) == null)
      throw new DataValidationException("Non-existent value descriptor specified in reading");
    try {
      if (persistData) {
        readingRepos.save(reading);
      } else
        reading.setId("unsaved");
      return reading.getId();
    } catch (Exception e) {
      logger.error("Error adding reading:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.PUT)
  @Override
  public boolean update(@RequestBody Reading reading2) {
    try {
      Reading reading = readingRepos.findOne(reading2.getId());
      if (reading != null) {
        if (reading2.getValue() != null) {
          reading.setValue(reading2.getValue());
        }
        if (reading2.getName() != null) {
          if (valDescRepos.findByName(reading2.getName()) == null)
            throw new DataValidationException("Non-existent value descriptor specified in reading");
          reading.setName(reading2.getName());
        }
        if (reading2.getOrigin() != 0) {
          reading.setOrigin(reading2.getOrigin());
        }
        readingRepos.save(reading);
        return true;
      } else {
        logger.error("Request to update with non-existent reading:  " + reading2.getId());
        throw new NotFoundException(Reading.class.toString(), reading2.getId());
      }
    } catch (DataValidationException dE) {
      throw dE;
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error updating reading:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/id/{id}", method = RequestMethod.DELETE)
  @Override
  public boolean delete(@PathVariable String id) {
    try {
      Reading reading = readingRepos.findOne(id);
      if (reading != null) {
        readingRepos.delete(reading);
        return true;
      } else {
        logger.error("Request to delete with non-existent reading:  " + id);
        throw new NotFoundException(Reading.class.toString(), id);
      }
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error removing reading:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  private List<Reading> filterReadings(List<ValueDescriptor> valDescs, int aLimit) {
    List<String> matchingValDesc = ValueDescriptor.getNames(valDescs);
    Query readingsMatchingValDesName = new Query().limit(aLimit);
    readingsMatchingValDesName.addCriteria(Criteria.where("name").in(matchingValDesc));
    return template.find(readingsMatchingValDesName, Reading.class);
  }

  private int determineLimit(int limit) {
    if (limit <= maxLimit)
      return limit;
    else
      return maxLimit;
  }

}
