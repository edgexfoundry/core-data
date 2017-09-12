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

package org.edgexfoundry;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Configures Mongo DB. This configuration allows the MongoClientOptions to be used - thereby
 * allowing timeouts and wait times to override defaults.
 * 
 */
@Configuration
@EnableMongoAuditing
public class AppConfig extends AbstractMongoConfiguration {

  private @Value("${spring.data.mongodb.username}") String username;
  private @Value("${spring.data.mongodb.password}") String password;
  private @Value("${spring.data.mongodb.database}") String database;
  private @Value("${spring.data.mongodb.host}") String host;
  private @Value("${spring.data.mongodb.port}") int port;
  private @Value("${spring.data.mongodb.connectTimeout}") int connectTimeout;
  private @Value("${spring.data.mongodb.socketTimeout}") int socketTimeout;
  private @Value("${spring.data.mongodb.maxWaitTime}") int maxWaitTime;
  private @Value("${spring.data.mongodb.socketKeepAlive}") boolean socketKeepAlive;

  private MongoClient client;

  @Override
  protected String getDatabaseName() {
    return database;
  }

  public Mongo mongo() throws UnknownHostException {
    return getClient();
  }

  public @Bean MongoDbFactory mongoDbFactory() throws Exception {
    return new SimpleMongoDbFactory(getClient(), getDatabaseName());
  }

  public @Bean MongoTemplate mongoTemplate() throws Exception {
    return new MongoTemplate(mongoDbFactory());
  }

  private MongoClient getClient() throws UnknownHostException {
    if (client != null)
      return client;
    client = new MongoClient(getServerAddress(), getMongoCredentials(), getMongoClientOptions());
    return client;
  }

  private ServerAddress getServerAddress() throws UnknownHostException {
    return new ServerAddress(host, port);
  }

  private List<MongoCredential> getMongoCredentials() {
    MongoCredential credential =
        MongoCredential.createCredential(username, getDatabaseName(), password.toCharArray());
    return Arrays.asList(credential);
  }

  private MongoClientOptions getMongoClientOptions() {
    return MongoClientOptions.builder().connectTimeout(connectTimeout).socketTimeout(socketTimeout)
        .maxWaitTime(maxWaitTime).socketKeepAlive(socketKeepAlive).build();
  }

}
