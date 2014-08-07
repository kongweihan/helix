package org.apache.helix.participant.statemachine;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.helix.api.StateTransitionHandlerFactory;
import org.apache.helix.api.id.PartitionId;
import org.apache.helix.messaging.handling.HelixTaskExecutor;
import org.apache.log4j.Logger;

public class ScheduledTaskStateModelFactory extends StateTransitionHandlerFactory<ScheduledTaskStateModel> {
  Logger logger = Logger.getLogger(ScheduledTaskStateModelFactory.class);

  HelixTaskExecutor _executor;

  public ScheduledTaskStateModelFactory(HelixTaskExecutor executor) {
    _executor = executor;
  }

  @Override
  public ScheduledTaskStateModel createStateTransitionHandler(PartitionId partition) {
    logger.info("Create state model for ScheduledTask " + partition);
    return new ScheduledTaskStateModel(this, _executor, partition);
  }
}
