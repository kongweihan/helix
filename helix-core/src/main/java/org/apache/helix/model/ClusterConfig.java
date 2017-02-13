package org.apache.helix.model;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.helix.HelixProperty;
import org.apache.helix.ZNRecord;
import org.apache.helix.api.config.StateTransitionThrottleConfig;

/**
 * Cluster configurations
 */
public class ClusterConfig extends HelixProperty {
  /**
   * Configurable characteristics of a cluster
   */
  public enum ClusterConfigProperty {
    HELIX_DISABLE_PIPELINE_TRIGGERS,
    PERSIST_BEST_POSSIBLE_ASSIGNMENT,
    PERSIST_INTERMEDIATE_ASSIGNMENT,
    TOPOLOGY,  // cluster topology definition, for example, "/zone/rack/host/instance"
    FAULT_ZONE_TYPE, // the type in which isolation should be applied on when Helix places the replicas from same partition.
    DELAY_REBALANCE_DISABLED,  // enabled the delayed rebalaning in case node goes offline.
    DELAY_REBALANCE_TIME,    // delayed time in ms that the delay time Helix should hold until rebalancing.
    STATE_TRANSITION_THROTTLE_CONFIGS,
    STATE_TRANSITION_CANCELLATION_ENABLED
  }

  /**
   * Instantiate for a specific cluster
   *
   * @param cluster the cluster identifier
   */
  public ClusterConfig(String cluster) {
    super(cluster);
  }

  /**
   * Instantiate with a pre-populated record
   *
   * @param record a ZNRecord corresponding to a cluster configuration
   */
  public ClusterConfig(ZNRecord record) {
    super(record);
  }

  /**
   * Whether to persist best possible assignment in a resource's idealstate.
   *
   * @return
   */
  public Boolean isPersistBestPossibleAssignment() {
    return _record
        .getBooleanField(ClusterConfigProperty.PERSIST_BEST_POSSIBLE_ASSIGNMENT.toString(), false);
  }

  /**
   * Enable/Disable persist best possible assignment in a resource's idealstate.
   *
   * @return
   */
  public void setPersistBestPossibleAssignment(Boolean enable) {
    if (enable == null) {
      _record.getSimpleFields().remove(ClusterConfigProperty.PERSIST_BEST_POSSIBLE_ASSIGNMENT.toString());
    } else {
      _record.setBooleanField(ClusterConfigProperty.PERSIST_BEST_POSSIBLE_ASSIGNMENT.toString(), enable);
    }
  }

  /**
   * Whether to persist IntermediateAssignment in a resource's idealstate.
   *
   * @return
   */
  public Boolean isPersistIntermediateAssignment() {
    return _record
        .getBooleanField(ClusterConfigProperty.PERSIST_INTERMEDIATE_ASSIGNMENT.toString(), false);
  }

  /**
   * Enable/Disable persist IntermediateAssignment in a resource's idealstate.
   *
   * @return
   */
  public void setPersistIntermediateAssignment(Boolean enable) {
    if (enable == null) {
      _record.getSimpleFields().remove(ClusterConfigProperty.PERSIST_INTERMEDIATE_ASSIGNMENT.toString());
    } else {
      _record.setBooleanField(ClusterConfigProperty.PERSIST_INTERMEDIATE_ASSIGNMENT.toString(), enable);
    }
  }

  public Boolean isPipelineTriggersDisabled() {
    return _record
        .getBooleanField(ClusterConfigProperty.HELIX_DISABLE_PIPELINE_TRIGGERS.toString(), false);
  }

  public long getRebalanceDelayTime() {
    return _record.getLongField(ClusterConfigProperty.DELAY_REBALANCE_TIME.name(), -1);
  }

  public boolean isDelayRebalaceDisabled() {
    return _record.getBooleanField(ClusterConfigProperty.DELAY_REBALANCE_DISABLED.name(), false);
  }

  /**
   * Enable/Disable state transition cancellation for the cluster
   * @param enable
   */
  public void stateTransitionCancelEnabled(Boolean enable) {
    if (enable == null) {
      _record.getSimpleFields()
          .remove(ClusterConfigProperty.STATE_TRANSITION_CANCELLATION_ENABLED.name());
    } else {
      _record.setBooleanField(ClusterConfigProperty.STATE_TRANSITION_CANCELLATION_ENABLED.name(),
          enable);
    }
  }

  public boolean isStateTransitionCancelEnabled() {
    return _record
        .getBooleanField(ClusterConfigProperty.STATE_TRANSITION_CANCELLATION_ENABLED.name(), false);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClusterConfig) {
      ClusterConfig that = (ClusterConfig) obj;

      if (this.getId().equals(that.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get a list StateTransitionThrottleConfig set for this cluster.
   *
   * @return
   */
  public List<StateTransitionThrottleConfig> getStateTransitionThrottleConfigs() {
    List<String> configs =
        _record.getListField(ClusterConfigProperty.STATE_TRANSITION_THROTTLE_CONFIGS.name());
    if (configs == null || configs.isEmpty()) {
      return Collections.emptyList();
    }
    List<StateTransitionThrottleConfig> throttleConfigs =
        new ArrayList<StateTransitionThrottleConfig>();
    for (String configstr : configs) {
      StateTransitionThrottleConfig throttleConfig =
          StateTransitionThrottleConfig.fromJSON(configstr);
      if (throttleConfig != null) {
        throttleConfigs.add(throttleConfig);
      }
    }

    return throttleConfigs;
  }

  /**
   * Set StateTransitionThrottleConfig for this cluster.
   *
   * @param throttleConfigs
   */
  public void setStateTransitionThrottleConfigs(
      List<StateTransitionThrottleConfig> throttleConfigs) {
    List<String> configStrs = new ArrayList<String>();

    for (StateTransitionThrottleConfig throttleConfig : throttleConfigs) {
      String configStr = throttleConfig.toJSON();
      if (configStr != null) {
        configStrs.add(configStr);
      }
    }

    if (!configStrs.isEmpty()) {
      _record
          .setListField(ClusterConfigProperty.STATE_TRANSITION_THROTTLE_CONFIGS.name(), configStrs);
    }
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  /**
   * Get the name of this resource
   *
   * @return the instance name
   */
  public String getClusterName() {
    return _record.getId();
  }
}

