package org.apache.helix.task;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.helix.task.beans.JobBean;
import org.apache.helix.task.beans.TaskBean;
import org.apache.helix.task.beans.WorkflowBean;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Houses a job dag and config set to fully describe a job workflow
 */
public class Workflow {
  /** Default workflow name, useful constant for single-node workflows */
  public static final String UNSPECIFIED = "UNSPECIFIED";

  /** Workflow name */
  protected String _name;

  /** Holds workflow-level configurations */
  protected WorkflowConfig _workflowConfig;

  /** Contains the per-job configurations for all jobs specified in the provided dag */
  protected Map<String, Map<String, String>> _jobConfigs;

  /** Containers the per-job configurations of all individually-specified tasks */
  protected Map<String, List<TaskConfig>> _taskConfigs;

  /** Constructs and validates a workflow against a provided dag and config set */
  protected Workflow(String name, WorkflowConfig workflowConfig,
      Map<String, Map<String, String>> jobConfigs, Map<String, List<TaskConfig>> taskConfigs) {
    _name = name;
    _workflowConfig = workflowConfig;
    _jobConfigs = jobConfigs;
    _taskConfigs = taskConfigs;
    validate();
  }

  public String getName() {
    return _name;
  }

  public Map<String, Map<String, String>> getJobConfigs() {
    return _jobConfigs;
  }

  public Map<String, List<TaskConfig>> getTaskConfigs() {
    return _taskConfigs;
  }

  public WorkflowConfig getWorkflowConfig() {
    return _workflowConfig;
  }

  /**
   * Parses the YAML description from a file into a {@link Workflow} object.
   * @param file An abstract path name to the file containing the workflow description.
   * @return A {@link Workflow} object.
   * @throws Exception
   */
  public static Workflow parse(File file) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(file));
    return parse(br);
  }

  /**
   * Parses a YAML description of the workflow into a {@link Workflow} object. The YAML string is of
   * the following
   * form:
   * <p/>
   *
   * <pre>
   * name: MyFlow
   * jobs:
   *   - name : JobA
   *     command : SomeTask
   *     ...
   *   - name : JobB
   *     parents : [JobA]
   *     command : SomeOtherTask
   *     ...
   *   - name : JobC
   *     command : AnotherTask
   *     ...
   *   - name : JobD
   *     parents : [JobB, JobC]
   *     command : AnotherTask
   *     ...
   * </pre>
   * @param yaml A YAML string of the above form
   * @return A {@link Workflow} object.
   */
  public static Workflow parse(String yaml) throws Exception {
    return parse(new StringReader(yaml));
  }

  /** Helper function to parse workflow from a generic {@link Reader} */
  private static Workflow parse(Reader reader) throws Exception {
    Yaml yaml = new Yaml(new Constructor(WorkflowBean.class));
    WorkflowBean wf = (WorkflowBean) yaml.load(reader);
    Builder builder = new Builder(wf.name);

    if (wf != null && wf.jobs != null) {
      for (JobBean job : wf.jobs) {
        if (job.name == null) {
          throw new IllegalArgumentException("A job must have a name.");
        }

        if (job.parents != null) {
          for (String parent : job.parents) {
            builder.addParentChildDependency(parent, job.name);
          }
        }

        builder.addConfig(job.name, JobConfig.WORKFLOW_ID, wf.name);
        builder.addConfig(job.name, JobConfig.COMMAND, job.command);
        if (job.jobConfigMap != null) {
          builder.addJobCommandConfigMap(job.name, job.jobConfigMap);
        }
        builder.addConfig(job.name, JobConfig.TARGET_RESOURCE, job.targetResource);
        if (job.targetPartitionStates != null) {
          builder.addConfig(job.name, JobConfig.TARGET_PARTITION_STATES,
              Joiner.on(",").join(job.targetPartitionStates));
        }
        if (job.targetPartitions != null) {
          builder.addConfig(job.name, JobConfig.TARGET_PARTITIONS,
              Joiner.on(",").join(job.targetPartitions));
        }
        builder.addConfig(job.name, JobConfig.MAX_ATTEMPTS_PER_TASK,
            String.valueOf(job.maxAttemptsPerTask));
        builder.addConfig(job.name, JobConfig.MAX_FORCED_REASSIGNMENTS_PER_TASK,
            String.valueOf(job.maxForcedReassignmentsPerTask));
        builder.addConfig(job.name, JobConfig.NUM_CONCURRENT_TASKS_PER_INSTANCE,
            String.valueOf(job.numConcurrentTasksPerInstance));
        builder.addConfig(job.name, JobConfig.TIMEOUT_PER_TASK,
            String.valueOf(job.timeoutPerPartition));
        builder.addConfig(job.name, JobConfig.FAILURE_THRESHOLD,
            String.valueOf(job.failureThreshold));
        if (job.tasks != null) {
          List<TaskConfig> taskConfigs = Lists.newArrayList();
          for (TaskBean task : job.tasks) {
            taskConfigs.add(TaskConfig.from(task));
          }
          builder.addTaskConfigs(job.name, taskConfigs);
        }
      }
    }

    if (wf.schedule != null) {
      builder.setScheduleConfig(ScheduleConfig.from(wf.schedule));
    }
    builder.setExpiry(wf.expiry);

    return builder.build();
  }

  /**
   * Verifies that all nodes in provided dag have accompanying config and vice-versa.
   * Also checks dag for cycles and unreachable nodes, and ensures configs are valid.
   */
  public void validate() {
    // validate dag and configs
    Set<String> jobNamesInConfig = new HashSet<String>(_jobConfigs.keySet());
    Set<String> jobNamesInDag = new HashSet<String>(_workflowConfig.getJobDag().getAllNodes());
    if (!jobNamesInConfig.equals(jobNamesInDag)) {
      Set<String> jobNamesInConfigButNotInDag = new HashSet<String>(jobNamesInConfig);
      jobNamesInConfigButNotInDag.removeAll(jobNamesInDag);
      Set<String> jobNamesInDagButNotInConfig = new HashSet<String>(jobNamesInDag);
      jobNamesInDagButNotInConfig.removeAll(jobNamesInConfig);

      throw new IllegalArgumentException(
          "Job Names dismatch. Names in config but not in dag: " + jobNamesInConfigButNotInDag +
          ", names in dag but not in config: " + jobNamesInDagButNotInConfig);
    }

    _workflowConfig.getJobDag().validate();

    for (String node : _jobConfigs.keySet()) {
      buildConfig(node);
    }
  }

  /** Builds a JobConfig from config map. Useful for validating configs */
  private JobConfig buildConfig(String job) {
    JobConfig.Builder b = JobConfig.Builder.fromMap(_jobConfigs.get(job));
    if (_taskConfigs != null && _taskConfigs.containsKey(job)) {
      b.addTaskConfigs(_taskConfigs.get(job));
    }
    return b.build();
  }

  /** Build a workflow incrementally from dependencies and single configs, validate at build time */
  public static class Builder {
    protected String _name;
    protected JobDag _dag;
    protected Map<String, Map<String, String>> _jobConfigs;
    protected Map<String, List<TaskConfig>> _taskConfigs;
    protected ScheduleConfig _scheduleConfig;
    protected long _expiry;
    protected Map<String, String> _cfgMap;
    protected int _parallelJobs = -1;

    public Builder(String name) {
      _name = name;
      _dag = new JobDag();
      _jobConfigs = new TreeMap<String, Map<String, String>>();
      _taskConfigs = new TreeMap<String, List<TaskConfig>>();
      _expiry = WorkflowConfig.DEFAULT_EXPIRY;
    }

    public Builder addConfig(String job, String key, String val) {
      job = namespacify(job);
      _dag.addNode(job);
      if (!_jobConfigs.containsKey(job)) {
        _jobConfigs.put(job, new TreeMap<String, String>());
      }
      _jobConfigs.get(job).put(key, val);
      return this;
    }

    public Builder addJobCommandConfigMap(String job, Map<String, String> jobConfigMap) {
      return addConfig(job, JobConfig.JOB_COMMAND_CONFIG_MAP,
          TaskUtil.serializeJobCommandConfigMap(jobConfigMap));
    }

    public Builder addJobConfig(String job, JobConfig.Builder jobConfigBuilder) {
      JobConfig jobConfig = jobConfigBuilder.setWorkflow(_name).build();
      for (Map.Entry<String, String> e : jobConfig.getResourceConfigMap().entrySet()) {
        String key = e.getKey();
        String val = e.getValue();
        addConfig(job, key, val);
      }
      addTaskConfigs(job, jobConfig.getTaskConfigMap().values());
      return this;
    }

    public Builder addTaskConfigs(String job, Collection<TaskConfig> taskConfigs) {
      job = namespacify(job);
      _dag.addNode(job);
      if (!_taskConfigs.containsKey(job)) {
        _taskConfigs.put(job, new ArrayList<TaskConfig>());
      }
      if (!_jobConfigs.containsKey(job)) {
        _jobConfigs.put(job, new TreeMap<String, String>());
      }
      _taskConfigs.get(job).addAll(taskConfigs);
      return this;
    }

    public Builder addParentChildDependency(String parent, String child) {
      parent = namespacify(parent);
      child = namespacify(child);
      _dag.addParentToChild(parent, child);

      return this;
    }

    public Builder fromMap(Map<String, String> cfg) {
      _cfgMap = cfg;
      return this;
    }

    public Builder setScheduleConfig(ScheduleConfig scheduleConfig) {
      _scheduleConfig = scheduleConfig;
      return this;
    }

    public Builder setExpiry(long expiry) {
      _expiry = expiry;
      return this;
    }

    public String namespacify(String job) {
      return TaskUtil.getNamespacedJobName(_name, job);
    }

    public Builder parallelJobs(int parallelJobs) {
      _parallelJobs = parallelJobs;
      return this;
    }

    public Workflow build() {
      WorkflowConfig.Builder builder = buildWorkflowConfig();
      // calls validate internally
      return new Workflow(_name, builder.build(), _jobConfigs, _taskConfigs);
    }

    protected WorkflowConfig.Builder buildWorkflowConfig() {
      for (String task : _jobConfigs.keySet()) {
        // addConfig(task, TaskConfig.WORKFLOW_ID, _name);
        _jobConfigs.get(task).put(JobConfig.WORKFLOW_ID, _name);
      }

      WorkflowConfig.Builder builder;
      if (_cfgMap != null) {
        builder = WorkflowConfig.Builder.fromMap(_cfgMap);
      } else {
        builder = new WorkflowConfig.Builder();
      }

      builder.setJobDag(_dag);
      builder.setTargetState(TargetState.START);
      if (_scheduleConfig != null) {
        builder.setScheduleConfig(_scheduleConfig);
      }
      if (_expiry > 0) {
        builder.setExpiry(_expiry);
      }
      if (_parallelJobs != -1) {
        builder.setParallelJobs(_parallelJobs);
      }

      return builder;
    }
  }
}
