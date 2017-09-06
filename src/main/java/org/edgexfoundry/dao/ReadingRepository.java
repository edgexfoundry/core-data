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

import java.io.Serializable;
import java.util.List;

import org.edgexfoundry.domain.core.Reading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReadingRepository extends MongoRepository<Reading, String> {

  List<Reading> findByCreatedBetween(long start, long end);

  Page<Reading> findByCreatedBetween(long start, long end, Pageable pageable);

  List<Reading> findByModifiedBetween(long start, long end);

  Page<Reading> findByModifiedBetween(long start, long end, Pageable pageable);

  List<Reading> findByOriginBetween(long start, long end);

  Page<Reading> findByOriginBetween(long start, long end, Pageable pageable);

  List<Reading> findByPushedGreaterThan(long zero);

  Page<Reading> findByPushedGreaterThan(long zero, Pageable pageable);

  List<Reading> findByName(String name);

  Page<Reading> findByName(String name, Pageable pageable);

  List<Reading> findByValueIn(Serializable value);

  Page<Reading> findByValue(Serializable value, Pageable pageable);

  List<Reading> findByNameAndValue(String name, String value);

  Page<Reading> findByNameAndValue(String name, String value, Pageable pageable);

  List<Reading> findByNameAndDevice(String name, String device);

  Page<Reading> findByNameAndDevice(String name, String device, Pageable pageable);

}
