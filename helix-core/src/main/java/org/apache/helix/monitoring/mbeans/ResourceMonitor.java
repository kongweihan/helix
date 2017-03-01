package org.apache.helix.monitoring.mbeans;

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

import java.util.Map;
import java.util.Set;

import org.apache.helix.HelixDefinedState;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.IdealState;
import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

public class ResourceMonitor implements ResourceMonitorMBean {
  private static final Logger LOG = Logger.getLogger(ResourceMonitor.class);
  private static final long RESET_TIME_RANGE = 1000 * 60 * 60; // 1 hour

  private int _numOfPartitions;
  private int _numOfPartitionsInExternalView;
  private int _numOfErrorPartitions;
  private int _numNonTopStatePartitions;
  private int _externalViewIdealStateDiff;
  private long _successfulTopStateHandoffDurationCounter;
  private long _successTopStateHandoffCounter;
  private long _failedTopStateHandoffCounter;
  private long _maxSinglePartitionTopStateHandoffDuration;
  private long _lastResetTime;
  private String _tag = ClusterStatusMonitor.DEFAULT_TAG;
  private String _resourceName;
  private String _clusterName;

  public enum MonitorState {
    TOP_STATE
  }

  public ResourceMonitor(String clusterName, String resourceName) {
    _clusterName = clusterName;
    _resourceName = resourceName;
    _successfulTopStateHandoffDurationCounter = 0L;
    _successTopStateHandoffCounter = 0L;
    _failedTopStateHandoffCounter = 0L;
    _lastResetTime = System.currentTimeMillis();
  }

  @Override
  public long getPartitionGauge() {
    return _numOfPartitions;
  }

  @Override
  public long getErrorPartitionGauge() {
    return _numOfErrorPartitions;
  }

  @Override
  public long getMissingTopStatePartitionGauge() {
    return _numNonTopStatePartitions;
  }

  @Override
  public long getDifferenceWithIdealStateGauge() {
    return _externalViewIdealStateDiff;
  }

  @Override
  public long getSuccessfulTopStateHandoffDurationCounter() {
    return _successfulTopStateHandoffDurationCounter;
  }

  @Override
  public long getSucceededTopStateHandoffCounter() {
    return _successTopStateHandoffCounter;
  }

  @Override
  public long getMaxSinglePartitionTopStateHandoffDurationGauge() {
    return _maxSinglePartitionTopStateHandoffDuration;
  }

  @Override
  public long getFailedTopStateHandoffCounter() {
    return _failedTopStateHandoffCounter;
  }

  @Override
  public String getSensorName() {
    return String.format("%s.%s.%s.%s", ClusterStatusMonitor.RESOURCE_STATUS_KEY, _clusterName,
        _tag, _resourceName);
  }

  public String getResourceName() {
    return _resourceName;
  }

  public void updateResource(ExternalView externalView, IdealState idealState, String topState) {
    if (externalView == null) {
      LOG.warn("external view is null");
      return;
    }
    String resourceName = externalView.getId();

    if (idealState == null) {
      LOG.warn("ideal state is null for " + resourceName);
      _numOfErrorPartitions = 0;
      _numNonTopStatePartitions = 0;
      _externalViewIdealStateDiff = 0;
      _numOfPartitionsInExternalView = 0;
      return;
    }

    assert (resourceName.equals(idealState.getId()));

    int numOfErrorPartitions = 0;
    int numOfDiff = 0;
    Set<String> topStatePartitions = Sets.newHashSet();

    if (_numOfPartitions == 0) {
      _numOfPartitions = idealState.getRecord().getMapFields().size();
    }

    // TODO fix this; IdealState shall have either map fields (CUSTOM mode)
    // or list fields (AUTO mode)
    for (String partitionName : idealState.getRecord().getMapFields().keySet()) {
      Map<String, String> idealRecord = idealState.getInstanceStateMap(partitionName);
      Map<String, String> externalViewRecord = externalView.getStateMap(partitionName);

      if (externalViewRecord == null) {
        numOfDiff += idealRecord.size();
        continue;
      }
      for (String host : idealRecord.keySet()) {
        if (!externalViewRecord.containsKey(host)
            || !externalViewRecord.get(host).equals(idealRecord.get(host))) {
          numOfDiff++;
        }
      }

      for (String host : externalViewRecord.keySet()) {
        if (externalViewRecord.get(host).equalsIgnoreCase(HelixDefinedState.ERROR.toString())) {
          numOfErrorPartitions++;
        }
        if (topState != null && externalViewRecord.get(host).equalsIgnoreCase(topState)) {
          topStatePartitions.add(partitionName);
        }
      }
    }
    _numOfErrorPartitions = numOfErrorPartitions;
    _externalViewIdealStateDiff = numOfDiff;
    _numOfPartitionsInExternalView = externalView.getPartitionSet().size();
    _numNonTopStatePartitions = _numOfPartitions - topStatePartitions.size();
    String tag = idealState.getInstanceGroupTag();
    if (tag == null || tag.equals("") || tag.equals("null")) {
      _tag = ClusterStatusMonitor.DEFAULT_TAG;
    } else {
      _tag = tag;
    }
  }

  public void updateStateHandoffStats(MonitorState monitorState, long duration, boolean succeeded) {
    switch (monitorState) {
    case TOP_STATE:
      if (succeeded) {
        _successTopStateHandoffCounter++;
        _successfulTopStateHandoffDurationCounter += duration;
        _maxSinglePartitionTopStateHandoffDuration =
            Math.max(_maxSinglePartitionTopStateHandoffDuration, duration);
      } else {
        _failedTopStateHandoffCounter++;
      }
      break;
    default:
      LOG.warn(
          String.format("Wrong monitor state \"%s\" that not supported ", monitorState.name()));
    }
  }

  @Override
  public long getExternalViewPartitionGauge() {
    return _numOfPartitionsInExternalView;
  }

  public String getBeanName() {
    return _clusterName + " " + _resourceName;
  }

  public void resetMaxTopStateHandoffGauge() {
    if (_lastResetTime + RESET_TIME_RANGE <= System.currentTimeMillis()) {
      _maxSinglePartitionTopStateHandoffDuration = 0L;
    }
  }
}
