
/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.core;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.ExecuteCfg;
import org.quartz.JobCfg;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.quartz.Trigger.TriggerState;
import org.quartz.utils.Key;

/**
 * @author James House
 */
public interface RemotableQuartzScheduler extends Remote {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    String getSchedulerName() throws RemoteException;

    String getSchedulerInstanceId() throws RemoteException;

    SchedulerContext getSchedulerContext() throws SchedulerException, RemoteException;

    void start() throws SchedulerException, RemoteException;

    void startDelayed(int seconds) throws SchedulerException, RemoteException;
    
    void standby() throws RemoteException;

    boolean isInStandbyMode() throws RemoteException;

    void shutdown() throws RemoteException;

    void shutdown(boolean waitForJobsToComplete) throws RemoteException;

    boolean isShutdown() throws RemoteException;

    Date runningSince() throws RemoteException;

    String getVersion() throws RemoteException;

    int numJobsExecuted() throws RemoteException;

    Class<?> getJobStoreClass() throws RemoteException;

    boolean supportsPersistence() throws RemoteException;

    boolean isClustered() throws RemoteException;

    Class<?> getThreadPoolClass() throws RemoteException;

    int getThreadPoolSize() throws RemoteException;

    void clear() throws SchedulerException, RemoteException;
    
    List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException, RemoteException;

    Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException, RemoteException;
    default Date scheduleJob(JobCfg jobCfg, ExecuteCfg executeCfg) throws SchedulerException, RemoteException{
        throw new SchedulerException("Undefined logic!");
    }

    Date scheduleJob(Trigger trigger) throws SchedulerException, RemoteException;

    void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException, RemoteException;

    void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException, RemoteException;

    boolean deleteJob(Key jobKey) throws SchedulerException, RemoteException;

    boolean unscheduleJob(Key triggerKey) throws SchedulerException, RemoteException;

    Date rescheduleJob(Key triggerKey, Trigger newTrigger) throws SchedulerException, RemoteException;
        
//    void triggerJob(Key jobKey, JobDataMap data) throws SchedulerException, RemoteException;

//    void triggerJob(OperableTrigger trig) throws SchedulerException, RemoteException;
    
    void pauseTrigger(Key triggerKey) throws SchedulerException, RemoteException;

//    void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException, RemoteException;

    void pauseJob(Key key) throws SchedulerException, RemoteException;

    void pauseJobs(final String triggerName) throws SchedulerException, RemoteException;

    void resumeTrigger(Key triggerKey) throws SchedulerException, RemoteException;

//    void resumeTriggers(Key key) throws SchedulerException, RemoteException;

//    Set<String> getPausedTriggerGroups() throws SchedulerException, RemoteException;
    
    void resumeJob(Key key) throws SchedulerException, RemoteException;

    void resumeJobs(final String triggerName) throws SchedulerException, RemoteException;

//    void pauseAll() throws SchedulerException, RemoteException;

    void resumeAll() throws SchedulerException, RemoteException;

//    List<String> getJobGroupNames() throws SchedulerException, RemoteException;

//    Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException, RemoteException;
    Set<Key> getAllJobKeysInSched(final String triggerName) throws SchedulerException, RemoteException;

    List<? extends Trigger> getTriggersOfJob(Key jobKey) throws SchedulerException, RemoteException;

//    List<String> getTriggerGroupNames() throws SchedulerException, RemoteException;

//    Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException, RemoteException;

    JobDetail getJobDetail(Key jobKey) throws SchedulerException, RemoteException;

    Trigger getTrigger(Key triggerKey) throws SchedulerException, RemoteException;

    TriggerState getTriggerState(Key triggerKey) throws SchedulerException, RemoteException;

//    void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException, RemoteException;

//    void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException, RemoteException;

//    boolean deleteCalendar(String calName) throws SchedulerException, RemoteException;

//    Calendar getCalendar(String calName) throws SchedulerException, RemoteException;

//    List<String> getCalendarNames() throws SchedulerException, RemoteException;

    boolean interrupt(Key jobKey) throws UnableToInterruptJobException,RemoteException;

    boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException,RemoteException;
    
    boolean checkExists(Key jobKey) throws SchedulerException,RemoteException;
   
    boolean checkTriggerExists(Key triggerKey) throws SchedulerException,RemoteException;
 
    boolean deleteJobs(List<Key> jobKeys) throws SchedulerException,RemoteException;

    void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException,RemoteException;

    void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException,RemoteException;

    boolean unscheduleJobs(List<Key> keys) throws SchedulerException,RemoteException;
    
}
