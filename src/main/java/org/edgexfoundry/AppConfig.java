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

  @Value("${spring.data.mongodb.username}")
  private String username;

  @Value("${spring.data.mongodb.password}")
  private String password;

  @Value("${spring.data.mongodb.database}")
  private String database;

  @Value("${spring.data.mongodb.host}")
  private String host;

  @Value("${spring.data.mongodb.port}")
  private int port;

  @Value("${spring.data.mongodb.connectTimeout}")
  private int connectTimeout;

  @Value("${spring.data.mongodb.socketTimeout}")
  private int socketTimeout;

  @Value("${spring.data.mongodb.maxWaitTime}")
  private int maxWaitTime;

  @Value("${spring.data.mongodb.socketKeepAlive}")
  private boolean socketKeepAlive;

  private MongoClient client;

  @Override
  protected String getDatabaseName() {
    return database;
  }

  public Mongo mongo() throws UnknownHostException {
    return getClient();
  }

  @Override
  public @Bean MongoDbFactory mongoDbFactory() throws Exception {
    return new SimpleMongoDbFactory(getClient(), getDatabaseName());
  }

  @Override
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
