/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.mapred;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.TaskType;
/**
 * A {@link LoadManager} for use by the {@link FairScheduler} that allocates
 * tasks evenly across nodes up to their per-node maximum, using the default
 * load management algorithm in Hadoop.
 */
public class CapBasedLoadManager extends LoadManager {
  /**
   * Determine how many tasks of a given type we want to run on a TaskTracker. 
   * This cap is chosen based on how many tasks of that type are outstanding in
   * total, so that when the cluster is used below capacity, tasks are spread
   * out uniformly across the nodes rather than being clumped up on whichever
   * machines sent out heartbeats earliest.
   */
  // The allowed uneven percentage of tasks on the TaskTrackers.
  public final static String OVERSHOOT =
    "mapred.fairscheduler.capbasedloadmanager.overshootpercentage";
  int allowedOverShoot = 0;
  
  @Override
  public void setConf(Configuration conf) {
    super.setConf(conf);
    allowedOverShoot =
      conf.getInt(OVERSHOOT, 0);
    if (allowedOverShoot < 0) {
      allowedOverShoot = 0;
    }
    if (allowedOverShoot > 100) {
      allowedOverShoot = 100;
    }
  }
  
  int getCap(int totalRunnableTasks, int localMaxTasks, int totalSlots) {
    double load = ((double)totalRunnableTasks) / totalSlots;
    return (int) Math.ceil((localMaxTasks * Math.min(1.0, load) +
                           (double)allowedOverShoot / 100.0 * localMaxTasks));
  }

  @Override
  public boolean canAssignMap(TaskTrackerStatus tracker,
      int totalRunnableMaps, int totalMapSlots) {
    return tracker.countMapTasks() < getCap(totalRunnableMaps,
        tracker.getMaxMapSlots(), totalMapSlots);
  }

  @Override
  public boolean canAssignReduce(TaskTrackerStatus tracker,
      int totalRunnableReduces, int totalReduceSlots) {
    return tracker.countReduceTasks() < getCap(totalRunnableReduces,
        tracker.getMaxReduceSlots(), totalReduceSlots);
  }

  @Override
  public boolean canLaunchTask(TaskTrackerStatus tracker,
      JobInProgress job, TaskType type) {
    return true;
  }
}
