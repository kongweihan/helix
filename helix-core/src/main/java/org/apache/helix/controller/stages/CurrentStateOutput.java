package org.apache.helix.controller.stages;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.helix.model.CurrentState;
import org.apache.helix.model.Message;
import org.apache.helix.model.Partition;

import com.google.common.collect.Sets;

/**
 * The current state includes both current state and pending messages
 * For pending messages, we consider both toState and fromState
 * Pending message prevents controller sending transitions that may potentially violate state
 * constraints @see HELIX-541
 */
public class CurrentStateOutput {
  private final Map<String, Map<Partition, Map<String, String>>> _currentStateMap;
  private final Map<String, Map<Partition, Map<String, Message>>> _pendingStateMap;
  // Contains per-resource maps of partition -> (instance, requested_state). This corresponds to the
  // REQUESTED_STATE
  // field in the CURRENTSTATES node.
  private final Map<String, Map<Partition, Map<String, String>>> _requestedStateMap;
  // Contains per-resource maps of partition -> (instance, info). This corresponds to the INFO field
  // in the
  // CURRENTSTATES node. This is information returned by state transition methods on the
  // participants. It may be used
  // by the rebalancer.
  private final Map<String, Map<Partition, Map<String, String>>> _infoMap;
  private final Map<String, String> _resourceStateModelMap;
  private final Map<String, CurrentState> _curStateMetaMap;

  public CurrentStateOutput() {
    _currentStateMap = new HashMap<String, Map<Partition, Map<String, String>>>();
    _pendingStateMap = new HashMap<String, Map<Partition, Map<String, Message>>>();
    _resourceStateModelMap = new HashMap<String, String>();
    _curStateMetaMap = new HashMap<String, CurrentState>();
    _requestedStateMap = new HashMap<String, Map<Partition, Map<String, String>>>();
    _infoMap = new HashMap<String, Map<Partition, Map<String, String>>>();
  }

  public void setResourceStateModelDef(String resourceName, String stateModelDefName) {
    _resourceStateModelMap.put(resourceName, stateModelDefName);
  }

  public String getResourceStateModelDef(String resourceName) {
    return _resourceStateModelMap.get(resourceName);
  }

  public void setBucketSize(String resource, int bucketSize) {
    CurrentState curStateMeta = _curStateMetaMap.get(resource);
    if (curStateMeta == null) {
      curStateMeta = new CurrentState(resource);
      _curStateMetaMap.put(resource, curStateMeta);
    }
    curStateMeta.setBucketSize(bucketSize);
  }

  public int getBucketSize(String resource) {
    int bucketSize = 0;
    CurrentState curStateMeta = _curStateMetaMap.get(resource);
    if (curStateMeta != null) {
      bucketSize = curStateMeta.getBucketSize();
    }

    return bucketSize;
  }

  public void setCurrentState(String resourceName, Partition partition, String instanceName,
      String state) {
    if (!_currentStateMap.containsKey(resourceName)) {
      _currentStateMap.put(resourceName, new HashMap<Partition, Map<String, String>>());
    }
    if (!_currentStateMap.get(resourceName).containsKey(partition)) {
      _currentStateMap.get(resourceName).put(partition, new HashMap<String, String>());
    }
    _currentStateMap.get(resourceName).get(partition).put(instanceName, state);
  }

  public void setRequestedState(String resourceName, Partition partition, String instanceName,
      String state) {
    if (!_requestedStateMap.containsKey(resourceName)) {
      _requestedStateMap.put(resourceName, new HashMap<Partition, Map<String, String>>());
    }
    if (!_requestedStateMap.get(resourceName).containsKey(partition)) {
      _requestedStateMap.get(resourceName).put(partition, new HashMap<String, String>());
    }
    _requestedStateMap.get(resourceName).get(partition).put(instanceName, state);
  }

  public void setInfo(String resourceName, Partition partition, String instanceName, String state) {
    if (!_infoMap.containsKey(resourceName)) {
      _infoMap.put(resourceName, new HashMap<Partition, Map<String, String>>());
    }
    if (!_infoMap.get(resourceName).containsKey(partition)) {
      _infoMap.get(resourceName).put(partition, new HashMap<String, String>());
    }
    _infoMap.get(resourceName).get(partition).put(instanceName, state);
  }

  public void setPendingState(String resourceName, Partition partition, String instanceName,
      Message message) {
    if (!_pendingStateMap.containsKey(resourceName)) {
      _pendingStateMap.put(resourceName, new HashMap<Partition, Map<String, Message>>());
    }
    if (!_pendingStateMap.get(resourceName).containsKey(partition)) {
      _pendingStateMap.get(resourceName).put(partition, new HashMap<String, Message>());
    }
    _pendingStateMap.get(resourceName).get(partition).put(instanceName, message);
  }

  /**
   * given (resource, partition, instance), returns currentState
   * @param resourceName
   * @param partition
   * @param instanceName
   * @return
   */
  public String getCurrentState(String resourceName, Partition partition, String instanceName) {
    Map<Partition, Map<String, String>> map = _currentStateMap.get(resourceName);
    if (map != null) {
      Map<String, String> instanceStateMap = map.get(partition);
      if (instanceStateMap != null) {
        return instanceStateMap.get(instanceName);
      }
    }
    return null;
  }

  public String getRequestedState(String resourceName, Partition partition, String instanceName) {
    Map<Partition, Map<String, String>> map = _requestedStateMap.get(resourceName);
    if (map != null) {
      Map<String, String> instanceStateMap = map.get(partition);
      if (instanceStateMap != null) {
        return instanceStateMap.get(instanceName);
      }
    }
    return null;
  }

  public String getInfo(String resourceName, Partition partition, String instanceName) {
    Map<Partition, Map<String, String>> map = _infoMap.get(resourceName);
    if (map != null) {
      Map<String, String> instanceStateMap = map.get(partition);
      if (instanceStateMap != null) {
        return instanceStateMap.get(instanceName);
      }
    }
    return null;
  }

  /**
   * given (resource, partition, instance), returns toState
   * @param resourceName
   * @param partition
   * @param instanceName
   * @return pending message
   */
  public Message getPendingState(String resourceName, Partition partition, String instanceName) {
    Map<Partition, Map<String, Message>> map = _pendingStateMap.get(resourceName);
    if (map != null) {
      Map<String, Message> instanceStateMap = map.get(partition);
      if (instanceStateMap != null) {
        return instanceStateMap.get(instanceName);
      }
    }
    return null;
  }

  /**
   * given (resource, partition), returns (instance->currentState) map
   * @param resourceName
   * @param partition
   * @return
   */
  public Map<String, String> getCurrentStateMap(String resourceName, Partition partition) {
    if (_currentStateMap.containsKey(resourceName)) {
      Map<Partition, Map<String, String>> map = _currentStateMap.get(resourceName);
      if (map.containsKey(partition)) {
        return map.get(partition);
      }
    }
    return Collections.emptyMap();
  }

  /**
   * Given (resource, partition), returns (instance->toState) map
   * @param resourceName
   * @param partition
   * @return pending target state map
   */
  public Map<String, String> getPendingStateMap(String resourceName, Partition partition) {
    if (_pendingStateMap.containsKey(resourceName)) {
      Map<Partition, Map<String, Message>> map = _pendingStateMap.get(resourceName);
      if (map.containsKey(partition)) {
        Map<String, Message> pendingMsgMap = map.get(partition);
        Map<String, String> pendingStateMap = new HashMap<String, String>();
        for (String instance : pendingMsgMap.keySet()) {
          pendingStateMap.put(instance, pendingMsgMap.get(instance).getToState());
        }
        return pendingStateMap;
      }
    }
    return Collections.emptyMap();
  }

  /**
   * Given (resource, partition), returns (instance->pendingMessage) map
   * @param resourceName
   * @param partition
   * @return pending messages map
   */
  public Map<String, Message> getPendingMessageMap(String resourceName, Partition partition) {
    if (_pendingStateMap.containsKey(resourceName)) {
      Map<Partition, Map<String, Message>> map = _pendingStateMap.get(resourceName);
      if (map.containsKey(partition)) {
        return map.get(partition);
      }
    }
    return Collections.emptyMap();
  }

  /**
   * Get the partitions mapped in the current state
   * @param resourceId resource to look up
   * @return set of mapped partitions, or empty set if there are none
   */
  public Set<Partition> getCurrentStateMappedPartitions(String resourceId) {
    Map<Partition, Map<String, String>> currentStateMap = _currentStateMap.get(resourceId);
    Map<Partition, Map<String, Message>> pendingStateMap = _pendingStateMap.get(resourceId);
    Set<Partition> partitionSet = Sets.newHashSet();
    if (currentStateMap != null) {
      partitionSet.addAll(currentStateMap.keySet());
    }
    if (pendingStateMap != null) {
      partitionSet.addAll(pendingStateMap.keySet());
    }
    return partitionSet;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("current state= ").append(_currentStateMap);
    sb.append(", pending state= ").append(_pendingStateMap);
    return sb.toString();

  }

}
