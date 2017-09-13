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

package org.edgexfoundry.integration.spring;

import static org.junit.Assert.assertNotNull;

import org.edgexfoundry.Application;
import org.edgexfoundry.HeartBeat;
import org.edgexfoundry.dao.EventRepository;
import org.edgexfoundry.dao.ReadingRepository;
import org.edgexfoundry.dao.ScrubDao;
import org.edgexfoundry.messaging.EventPublisher;
import org.edgexfoundry.messaging.impl.ZeroMQEventPublisherImpl;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class})
public class SpringConfigurationTest {

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  HeartBeat heartBeat;

  @Test
  public void testHeartBeatBeanExists() {
    assertNotNull("HeartBeat bean not available", heartBeat);
  }

  @Test
  public void testReposBeansExist() {
    EventRepository eventRepos = ctx.getBean(EventRepository.class);
    assertNotNull("Event Repos bean not available", eventRepos);
    ReadingRepository readingRepos = ctx.getBean(ReadingRepository.class);
    assertNotNull("Reading Repos bean not available", readingRepos);
  }

  @Test
  public void testEventProducterBeanExists() {
    ZeroMQEventPublisherImpl producer = (ZeroMQEventPublisherImpl) ctx.getBean(EventPublisher.class);
    assertNotNull("ZeroMQ Event Producer bean not available", producer);
  }

  @Test
  public void testScrubberDaoBeanExists() {
    ScrubDao dao = ctx.getBean(ScrubDao.class);
    MongoTemplate template = ctx.getBean(org.springframework.data.mongodb.core.MongoTemplate.class);
    assertNotNull("Scrubber DAO bean not available", dao);
    assertNotNull("Mongo DB Template bean not available", template);
  }

}
