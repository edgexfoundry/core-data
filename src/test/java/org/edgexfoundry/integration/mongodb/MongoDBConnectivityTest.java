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

package org.edgexfoundry.integration.mongodb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.edgexfoundry.test.category.RequiresMongoDB;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoTimeoutException;

@Category(RequiresMongoDB.class)
public class MongoDBConnectivityTest {

  private static final String MONGO_URI = "mongodb://core:password@localhost/coredata";
  private static final String DB_NAME = "coredata";
  private static final String EVENT_COLLECTION_NAME = "event";
  private static final String READING_COLLECTION_NAME = "reading";

  @Test
  public void testMongoDBConnect() throws UnknownHostException {
    MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGO_URI));
    DB database = mongoClient.getDB(DB_NAME);
    DBCollection events = database.getCollection(EVENT_COLLECTION_NAME);
    DBCollection readings = database.getCollection(READING_COLLECTION_NAME);
    try {
      assertFalse("MongoDB Events collection not accessible", events.isCapped());
      assertFalse("MongoDB Readings collection not accessible", readings.isCapped());
    } catch (MongoTimeoutException ex) {
      fail("Mongo DB not available.  Check that Mongo DB has been started");
    }
  }

}
