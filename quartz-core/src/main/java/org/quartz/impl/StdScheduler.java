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

package org.quartz.impl;

import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.spi.JobFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that directly
 * proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code>
 * instance.
 * </p>
 * 
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzScheduler
 *
 * @author James House
 */
public class StdScheduler implements Scheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private QuartzScheduler sched;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Construct a <code>StdScheduler</code> instance to proxy the given
     * <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
     * </p>
     */
    public StdScheduler(QuartzScheduler sched) {
        this.sched = sched;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Returns the name of the <code>Scheduler</code>.
     * </p>
     */
    @Override
    public String getSchedulerName() {
        return sched.getSchedulerName();
    }

//    /**
//     * <p>
//     * Returns the instance Id of the <code>Scheduler</code>.
//     * </p>
//     */
//    @Override
//    public String getSchedulerInstanceId() {
//        return sched.getSchedulerInstanceId();
//    }
//    @Override
//    public SchedulerMetaData getMetaData() {
//        return new SchedulerMetaData(getSchedulerName(),
//                getSchedulerInstanceId(), getClass(), false, isStarted(),
//                isInStandbyMode(), isShutdown(), sched.runningSince(),
//                sched.numJobsExecuted(), sched.getJobStoreClass(),
//                sched.supportsPersistence(), sched.isClustered(), sched.getThreadPoolClass(),
//                sched.getThreadPoolSize(), sched.getVersion());
//
//    }
//
//    /**
//     * <p>
//     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
//     * </p>
//     */
//    @Override
//    public SchedulerContext getContext() throws SchedulerException {
//        return sched.getSchedulerContext();
//    }
//
    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Schedululer State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void start() throws SchedulerException {
        sched.start();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void startDelayed(int seconds) throws SchedulerException {
        sched.startDelayed(seconds);
    }


    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void standby() {
        sched.standby();
    }
    
    /**
     * Whether the scheduler has been started.  
     * 
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever
     * been called on this Scheduler, so it will return <code>true</code> even 
     * if the <code>Scheduler</code> is currently in standby mode or has been 
     * since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */
    @Override
    public boolean isStarted() {
        return (sched.runningSince() != null);
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean isInStandbyMode() {
        return sched.isInStandbyMode();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void shutdown() {
        sched.shutdown();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        sched.shutdown(waitForJobsToComplete);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean isShutdown() {
        return sched.isShutdown();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<JobExecutionContext> getCurrentlyExecutingJobs() {
        return sched.getCurrentlyExecutingJobs();
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void clear() throws SchedulerException {
//        sched.clear();
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
//        return sched.scheduleJob(jobDetail, trigger);
//    }
//    @Override
//    public Date scheduleJobAndExecute(JobCfg jobCfg, ExecuteCfg executeCfg) throws SchedulerException {
//        return sched.scheduleJob(jobCfg,executeCfg);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Date scheduleJob(Trigger trigger) throws SchedulerException {
//        return sched.scheduleJob(trigger);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void addJob(JobDetail jobDetail, boolean replace)throws SchedulerException {
//        sched.addJob(jobDetail, replace);
//    }
//    @Override
//    public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException {
//        sched.addJob(jobDetail, replace, storeNonDurableWhileAwaitingScheduling);
//    }
//
//    @Override
//    public boolean deleteJobs(List<Key> keys) throws SchedulerException {
//        return sched.deleteJobs(keys);
//    }
//    @Override
//    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {
//        sched.scheduleJobs(triggersAndJobs, replace);
//    }
//    @Override
//    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException {
//        sched.scheduleJob(jobDetail,  triggersForJob, replace);
//    }
//    @Override
//    public boolean unscheduleJobs(List<Key> keys) throws SchedulerException {
//        return sched.unscheduleJobs(keys);
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public boolean deleteJob(Key jobKey) throws SchedulerException {
//        return sched.deleteJob(jobKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public boolean unscheduleJob(Key key) throws SchedulerException {
//        return sched.unscheduleJob(key);
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     *  在“proxyed”QuartzScheduler上调用等效方法。 (重新安排作业)
//     * </p>
//     */
//    @Override
//    public Date rescheduleJob(Key triggerKey,Trigger newTrigger) throws SchedulerException {
//        return sched.rescheduleJob(triggerKey, newTrigger);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void triggerJob(Key jobKey) throws SchedulerException {
//        triggerJob(jobKey, null);
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void triggerJob(Key jobKey, JobDataMap data) throws SchedulerException {
//        sched.triggerJob(jobKey, data);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void pauseTrigger(Key triggerKey) throws SchedulerException {
//        sched.pauseTrigger(triggerKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
//        sched.pauseTriggers(matcher);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void pauseJob(Key key) throws SchedulerException {
//        sched.pauseJob(key);
//    }

//    /**
//     * @see org.quartz.Scheduler#getPausedTriggerGroups()
//     */
//    public Set<String> getPausedTriggerGroups() throws SchedulerException {
//        return sched.getPausedTriggerGroups();
//    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * triggerName allowed empty!
     * </p>
     */
    @Override
    public void pauseJobs(final String triggerName) throws SchedulerException {
        sched.pauseJobs(triggerName);
    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeTrigger(Key triggerKey) throws SchedulerException {
//        sched.resumeTrigger(triggerKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeTriggers(GroupMatcher<Key<?>> matcher) throws SchedulerException {
//        sched.resumeTriggers(matcher);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeJob(Key key) throws SchedulerException {
//        sched.resumeJob(key);
//    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void resumeJobs(final String triggerName) throws SchedulerException {
        sched.resumeJobs(triggerName);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Deprecated
    public void pauseAll() throws SchedulerException {
        sched.pauseAll();
    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeAll() throws SchedulerException {
//        sched.resumeAll();
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public List<String> getJobGroupNames() throws SchedulerException {
//        return sched.getJobGroupNames();
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public List<? extends Trigger> getTriggersOfJob(Key jobKey) throws SchedulerException {
//        return sched.getTriggersOfJob(jobKey);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public List<String> getTriggerGroupNames() throws SchedulerException {
//        return sched.getTriggerGroupNames();
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Set<Key> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
//        return sched.getTriggerKeys(matcher);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public JobDetail getJobDetail(Key jobKey)throws SchedulerException {
//        return sched.getJobDetail(jobKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Trigger getTrigger(Key triggerKey) throws SchedulerException {
//        return sched.getTrigger(triggerKey);
//    }
//    /**
//     * Reset the current state of the identified <code>{@link Trigger}</code>
//     * from {@link TriggerState#ERROR} to {@link TriggerState#NORMAL} or
//     * {@link TriggerState#PAUSED} as appropriate.
//     *
//     * <p>Only affects triggers that are in ERROR state - if identified trigger is not
//     * in that state then the result is a no-op.</p>
//     *
//     * <p>The result will be the trigger returning to the normal, waiting to
//     * be fired state, unless the trigger's group has been paused, in which
//     * case it will go into the PAUSED state.</p>
//     *
//     * @see Trigger.TriggerState
//     */
//    @Override
//    public void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException {
//        sched.resetTriggerFromErrorState(triggerKey);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException {
//        sched.addCalendar(calName, calendar, replace, updateTriggers);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public boolean deleteCalendar(String calName) throws SchedulerException {
//        return sched.deleteCalendar(calName);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Calendar getCalendar(String calName) throws SchedulerException {
//        return sched.getCalendar(calName);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public List<String> getCalendarNames() throws SchedulerException {
//        return sched.getCalendarNames();
//    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Other Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    

    /**
     * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
     */
    @Override
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        sched.setJobFactory(factory);
    }

//    /**
//     * @see org.quartz.Scheduler#getListenerManager()
//     */
//    @Override
//    public ListenerManager getListenerManager() throws SchedulerException {
//        return sched.getListenerManager();
//    }
//    @Override
//    public boolean interrupt(Key jobKey) throws UnableToInterruptJobException {
//        return sched.interrupt(jobKey);
//    }
//    @Override
//    public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
//        return sched.interrupt(fireInstanceId);
//    }

  
}
