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

import java.util.Calendar;

import org.edgexfoundry.domain.core.Event;
import org.edgexfoundry.domain.core.Reading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.mongodb.WriteResult;

@Component
public class ScrubDao {

  @Autowired
  MongoTemplate template;

  /**
   * Remove all pushed events and their associated readings
   */
  public int scrubPushedEvents() {
    Query scrubQuery = new Query();
    scrubQuery.addCriteria(Criteria.where("pushed").gt(0));
    // remove readings
    template.remove(scrubQuery, Reading.class);
    // now remove events
    WriteResult result = template.remove(scrubQuery, Event.class);
    return result.getN();
  }

  /**
   * remove all old events (and associated reaadings) based on delimiting age.
   * 
   * @param age - minimum age in milliseconds (from created timestamp) an event should be in order
   *        to be removed
   * @return - number of events removed
   */
  public int scrubOldEvents(long age) {
    Query scrubQuery = new Query();
    scrubQuery
        .addCriteria(Criteria.where("created").lt(Calendar.getInstance().getTimeInMillis() - age));
    // remove readings
    template.remove(scrubQuery, Reading.class);
    // now remove events
    WriteResult result = template.remove(scrubQuery, Event.class);
    return result.getN();
  }

}
