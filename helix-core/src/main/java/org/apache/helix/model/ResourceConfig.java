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

import org.apache.helix.HelixProperty;
import org.apache.helix.ZNRecord;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * Resource configurations
 */
public class ResourceConfig extends HelixProperty {
  /**
   * Configurable characteristics of a resource
   */
  public enum ResourceConfigProperty {
    MONITORING_DISABLED, // Resource-level config, do not create Mbean and report any status for the resource.
  }

  private static final Logger _logger = Logger.getLogger(ResourceConfig.class.getName());

  /**
   * Instantiate for a specific instance
   *
   * @param resourceId the instance identifier
   */
  public ResourceConfig(String resourceId) {
    super(resourceId);
  }

  /**
   * Instantiate with a pre-populated record
   *
   * @param record a ZNRecord corresponding to an instance configuration
   */
  public ResourceConfig(ZNRecord record) {
    super(record);
  }

  /**
   * Get the value of DisableMonitoring set.
   *
   * @return the MonitoringDisabled is true or false
   */
  public Boolean isMonitoringDisabled() {
    return _record.getBooleanField(ResourceConfigProperty.MONITORING_DISABLED.toString(), false);
  }

  /**
   * Set whether to disable monitoring for this resource.
   *
   * @param monitoringDisabled whether to disable monitoring for this resource.
   */
  public void setMonitoringDisabled(boolean monitoringDisabled) {
    _record
        .setBooleanField(ResourceConfigProperty.MONITORING_DISABLED.toString(), monitoringDisabled);
  }

  /**
   * Put a set of simple configs.
   *
   * @param configsMap
   */
  public void putSimpleConfigs(Map<String, String> configsMap) {
    getRecord().getSimpleFields().putAll(configsMap);
  }

  /**
   * Get all simple configurations.
   *
   * @return all simple configurations.
   */
  public Map<String, String> getSimpleConfigs() {
    return Collections.unmodifiableMap(getRecord().getSimpleFields());
  }

  /**
   * Put a single simple config value.
   *
   * @param configKey
   * @param configVal
   */
  public void putSimpleConfig(String configKey, String configVal) {
    getRecord().getSimpleFields().put(configKey, configVal);
  }

  /**
   * Get a single simple config value.
   *
   * @param configKey
   * @return configuration value, or NULL if not exist.
   */
  public String getSimpleConfig(String configKey) {
    return getRecord().getSimpleFields().get(configKey);
  }

  /**
   * Put a single map config.
   *
   * @param configKey
   * @param configValMap
   */
  public void putMapConfig(String configKey, Map<String, String> configValMap) {
    getRecord().setMapField(configKey, configValMap);
  }

  /**
   * Get a single map config.
   *
   * @param configKey
   * @return configuration value map, or NULL if not exist.
   */
  public Map<String, String> getMapConfig(String configKey) {
    return getRecord().getMapField(configKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ResourceConfig) {
      ResourceConfig that = (ResourceConfig) obj;

      if (this.getId().equals(that.getId())) {
        return true;
      }
    }
    return false;
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
  public String getResourceName() {
    return _record.getId();
  }

  @Override
  public boolean isValid() {
    return true;
  }
}

