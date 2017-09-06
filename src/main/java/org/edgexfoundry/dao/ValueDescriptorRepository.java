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

package org.edgexfoundry.dao;

import java.util.List;

import org.edgexfoundry.domain.common.IoTType;
import org.edgexfoundry.domain.common.ValueDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ValueDescriptorRepository extends MongoRepository<ValueDescriptor, String> {

  ValueDescriptor findByName(String name);

  List<ValueDescriptor> findByUomLabel(String uomLabel);

  Page<ValueDescriptor> findByUomLabel(String uomLabel, Pageable pageable);

  List<ValueDescriptor> findByLabelsIn(String label);

  Page<ValueDescriptor> findByLabels(String label, Pageable pageable);

  List<ValueDescriptor> findByType(IoTType type);

  Page<ValueDescriptor> findByType(IoTType type, Pageable pageable);
}
