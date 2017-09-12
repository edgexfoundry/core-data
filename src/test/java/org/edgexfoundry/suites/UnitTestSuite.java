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
 * @microservice: support-notifications
 * @author: Jim White, Dell
 * @version: 1.0.0
 *******************************************************************************/

package org.edgexfoundry.suites;

import org.edgexfoundry.controller.EventControllerTest;
import org.edgexfoundry.controller.PingControllerTest;
import org.edgexfoundry.controller.ReadingControllerTest;
import org.edgexfoundry.controller.ThreadTasksTest;
import org.edgexfoundry.controller.ValueDescriptorTest;
import org.edgexfoundry.messaging.ZeroMQEventPublishingImplTest;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Used in development only. Remove @Ignore to run just the unit tests (not integration tests).
 * These tests do require other resources to run.
 * 
 * @author Jim White
 *
 */
@Ignore
@RunWith(Suite.class)
@Suite.SuiteClasses({EventControllerTest.class, PingControllerTest.class,
    ReadingControllerTest.class, ThreadTasksTest.class, ValueDescriptorTest.class,
    ZeroMQEventPublishingImplTest.class})
public class UnitTestSuite {

}
