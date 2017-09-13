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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.edgexfoundry.controller.DeviceClient;
import org.edgexfoundry.controller.ValueDescriptorController;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ValueDescriptorRepository;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.edgexfoundry.domain.meta.Command;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/valuedescriptor")
public class ValueDescriptorControllerImpl implements ValueDescriptorController {
  // TODO - someday, on adds and updates, check min, max values for validity.
  // Check default is within min/max.
  // TODO - someday, check meta data to make sure device report does not
  // reference value descriptor on delete or change of name

  private static final String ERR_GETTING = "Error getting value descriptor:  ";

  private static final org.edgexfoundry.support.logging.client.EdgeXLogger logger =
      org.edgexfoundry.support.logging.client.EdgeXLoggerFactory
          .getEdgeXLogger(ValueDescriptorControllerImpl.class);

  @Autowired
  ValueDescriptorRepository valDescRepos;

  @Autowired
  ReadingRepository readingRepos;

  @Autowired
  DeviceClient deviceClient;

  @Autowired
  MongoTemplate template;

  @Value("${read.max.limit}")
  private int maxLimit;

  @Value("${formatSpecifier}")
  private String formatSpecifier;

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
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @Override
  public ValueDescriptor valueDescriptor(@PathVariable String id) {
    try {
      ValueDescriptor valuDes = valDescRepos.findOne(id);
      if (valuDes == null)
        throw new NotFoundException(ValueDescriptor.class.toString(), id);
      return valuDes;
    } catch (NotFoundException nfE) {
      throw nfE;
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.GET)
  @Override
  public List<ValueDescriptor> valueDescriptors() {
    try {
      if (valDescRepos.count() > maxLimit)
        throw new LimitExceededException("Value Descriptor");
      else
        return valDescRepos.findAll();
    } catch (LimitExceededException lE) {
      throw lE;
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Return ValueDescriptor object with given name. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param name of the value descriptor to locate and return
   * @return ValueDescriptor having the provided name, could be null if none are found.
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   */
  @RequestMapping(value = "/name/{name:.+}", method = RequestMethod.GET)
  @Override
  public ValueDescriptor valueDescriptorByName(@PathVariable String name) {
    try {
      ValueDescriptor valuDes = valDescRepos.findByName(name);
      if (valuDes == null)
        throw new NotFoundException(ValueDescriptor.class.toString(), name);
      return valuDes;
    } catch (NotFoundException nfE) {
      throw nfE;
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Return ValueDescriptor objects with given UoM label. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param uomLabel
   * @return list of ValueDescriptor matching UoM Label
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   */
  @RequestMapping(value = "/uomlabel/{uomLabel:.+}", method = RequestMethod.GET)
  @Override
  public List<ValueDescriptor> valueDescriptorByUoMLabel(@PathVariable String uomLabel) {
    try {
      return valDescRepos.findByUomLabel(uomLabel);
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

  /**
   * Return ValueDescriptor objects with given label. ServcieException (HTTP 503) for unknown or
   * unanticipated issues.
   * 
   * @param label
   * @return list of ValueDescriptor matching UoM Label
   * @throws ServcieException (HTTP 503) for unknown or unanticipated issues
   */
  @RequestMapping(value = "/label/{label:.+}", method = RequestMethod.GET)
  @Override
  public List<ValueDescriptor> valueDescriptorByLabel(@PathVariable String label) {
    try {
      Query query = new Query(Criteria.where("labels").all(label));
      return template.find(query, ValueDescriptor.class);
    } catch (Exception e) {
      logger.error(ERR_GETTING + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/devicename/{name:.+}", method = RequestMethod.GET)
  @Override
  public List<ValueDescriptor> valueDescriptorsForDeviceByName(@PathVariable String name) {
    try {
      Device device = deviceClient.deviceForName(name);
      Set<String> vdNames = device.getProfile().getCommands().stream()
          .map((Command cmd) -> cmd.associatedValueDescriptors()).flatMap(l -> l.stream())
          .collect(Collectors.toSet());
      return vdNames.stream().map(s -> this.valDescRepos.findByName(s))
          .collect(Collectors.toList());
    } catch (javax.ws.rs.NotFoundException nfE) {
      throw new NotFoundException(Device.class.toString(), name);
    } catch (Exception e) {
      logger.error("Error getting value descriptor by device name:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/deviceid/{id}", method = RequestMethod.GET)
  @Override
  public List<ValueDescriptor> valueDescriptorsForDeviceById(@PathVariable String id) {
    try {
      Device device = deviceClient.device(id);
      Set<String> vdNames = device.getProfile().getCommands().stream()
          .map((Command cmd) -> cmd.associatedValueDescriptors()).flatMap(l -> l.stream())
          .collect(Collectors.toSet());
      return vdNames.stream().map(s -> this.valDescRepos.findByName(s))
          .collect(Collectors.toList());
    } catch (javax.ws.rs.NotFoundException nfE) {
      throw new NotFoundException(Device.class.toString(), id);
    } catch (Exception e) {
      logger.error("Error getting value descriptor by device name:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.POST)
  @Override
  public String add(@RequestBody ValueDescriptor valueDescriptor) {
    if (!validateFormatString(valueDescriptor))
      throw new DataValidationException(
          "Value descriptor's format string doesn't fit the required pattern: " + formatSpecifier);
    try {
      valDescRepos.save(valueDescriptor);
      return valueDescriptor.getId();
    } catch (DuplicateKeyException dE) {
      throw new DataValidationException(
          "Value descriptor's name is not unique: " + valueDescriptor.getName());
    } catch (Exception e) {
      logger.error("Error adding value descriptor:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(method = RequestMethod.PUT)
  @Override
  public boolean update(@RequestBody ValueDescriptor valueDescriptor2) {
    try {
      ValueDescriptor valueDescriptor =
          getValueDescriptorByIdOrName(valueDescriptor2.getId(), valueDescriptor2.getName());
      if (valueDescriptor != null) {
        updateValueDescriptor(valueDescriptor2, valueDescriptor);
        return true;
      } else {
        logger.error(
            "Request to update with non-existent or unidentified value descriptor (id/name):  "
                + valueDescriptor2.getId() + "/" + valueDescriptor2.getName());
        throw new NotFoundException(ValueDescriptor.class.toString(), valueDescriptor2.getId());
      }
    } catch (DataValidationException dE) {
      throw dE;
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error updating value descriptor:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/id/{id}", method = RequestMethod.DELETE)
  @Override
  public boolean delete(@PathVariable String id) {
    try {
      ValueDescriptor valueDescriptor = valDescRepos.findOne(id);
      if (valueDescriptor != null)
        return deleteValueDescriptor(valueDescriptor);
      else {
        logger.error("Request to delete with non-existent value descriptor id:  " + id);
        throw new NotFoundException(ValueDescriptor.class.toString(), id);
      }
    } catch (DataValidationException dE) {
      throw dE;
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error removing value descriptor:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

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
  @RequestMapping(value = "/name/{name:.+}", method = RequestMethod.DELETE)
  @Override
  public boolean deleteByName(@PathVariable String name) {
    try {
      ValueDescriptor valueDescriptor = valDescRepos.findByName(name);
      if (valueDescriptor != null)
        return deleteValueDescriptor(valueDescriptor);
      else {
        logger.error("Request to delete with unknown value descriptor name:  " + name);
        throw new NotFoundException(ValueDescriptor.class.toString(), name);
      }
    } catch (DataValidationException dE) {
      throw dE;
    } catch (NotFoundException nE) {
      throw nE;
    } catch (Exception e) {
      logger.error("Error removing value descriptor:  " + e.getMessage());
      throw new ServiceException(e);
    }
  }

  private boolean deleteValueDescriptor(ValueDescriptor valueDescriptor) {
    if (readingRepos.findByName(valueDescriptor.getName()).isEmpty()) {
      valDescRepos.delete(valueDescriptor);
      return true;
    } else {
      logger.error("Data integrity issue.  Value Descriptor with id:  " + valueDescriptor.getId()
          + "is still referenced by existing readings.");
      throw new DataValidationException("Data integrity issue.  Value Descriptor with id:  "
          + valueDescriptor.getId() + "is still referenced by existing readings.");
    }
  }

  private void updateValueDescriptor(ValueDescriptor from, ValueDescriptor to) {
    if (from.getDefaultValue() != null) {
      to.setDefaultValue(from.getDefaultValue());
    }
    if (from.getFormatting() != null) {
      if (!validateFormatString(from))
        throw new DataValidationException(
            "Value descriptor's format string doesn't fit the required pattern: "
                + formatSpecifier);
      to.setFormatting(from.getFormatting());
    }
    if (from.getLabels() != null) {
      to.setLabels(from.getLabels());
    }
    if (from.getMax() != null) {
      to.setMax(from.getMax());
    }
    if (from.getMin() != null) {
      to.setMin(from.getMin());
    }
    if (from.getName() != null) {
      if (readingRepos.findByName(to.getName()).isEmpty()) {
        to.setName(from.getName());
      } else {
        logger.error("Data integrity issue.  Value Descriptor with name:  " + from.getName()
            + " is still referenced by existing readings.");
        throw new DataValidationException("Data integrity issue.  Value Descriptor with name:  "
            + from.getName() + " is still referenced by existing readings.");
      }
    }
    if (from.getOrigin() != 0) {
      to.setOrigin(from.getOrigin());
    }
    if (from.getType() != null) {
      to.setType(from.getType());
    }
    if (from.getUomLabel() != null) {
      to.setUomLabel(from.getUomLabel());
    }
    valDescRepos.save(to);
  }

  private boolean validateFormatString(ValueDescriptor valueDescriptor) {
    if ("".equals(valueDescriptor.getFormatting()) || (valueDescriptor.getFormatting() == null))
      return true;
    else
      return valueDescriptor.getFormatting().matches(formatSpecifier);
  }

  private ValueDescriptor getValueDescriptorByIdOrName(String id, String name) {
    if (id != null)
      return valDescRepos.findOne(id);
    return valDescRepos.findByName(name);
  }
}
