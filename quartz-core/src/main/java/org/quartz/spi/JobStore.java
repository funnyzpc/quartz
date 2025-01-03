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

package org.quartz.spi;

import java.util.List;

import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a <code>{@link org.quartz.Job}</code>
 * and <code>{@link org.quartz.Trigger}</code> storage mechanism for the
 * <code>{@link org.quartz.core.QuartzScheduler}</code>'s use.
 * </p>
 *
 * <p>
 * Storage of <code>Job</code> s and <code>Trigger</code> s should be keyed
 * on the combination of their name and group for uniqueness.
 * </p>
 *
 * @see org.quartz.core.QuartzScheduler
 * @see org.quartz.Trigger
 * @see org.quartz.Job
 * @see org.quartz.JobDetail
 * @see org.quartz.JobDataMap
 * @see org.quartz.Calendar
 *
 * @author James House
 * @author Eric Mueller
 */
public interface JobStore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Called by the QuartzScheduler before the <code>JobStore</code> is
     * used, in order to give the it a chance to initialize.
     */
    void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException;

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has started.
     */
    void schedulerStarted() throws SchedulerException ;

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has been paused.
     */
    void schedulerPaused();

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has resumed after being paused.
     */
    void schedulerResumed();

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * it should free up all of it's resources because the scheduler is
     * shutting down.
     */
    void shutdown();

    boolean supportsPersistence();
    
//    /**
//     * How long (in milliseconds) the <code>JobStore</code> implementation
//     * estimates that it will take to release a trigger and acquire a new one.
//     */
//    long getEstimatedTimeToReleaseAndAcquireTrigger();
    
    /**
     * Whether or not the <code>JobStore</code> implementation is clustered.
     */
    boolean isClustered();

    /////////////////////////////////////////////////////////////////////////////
    //
    // Job & Trigger Storage methods
    //
    /////////////////////////////////////////////////////////////////////////////

//    /**
//     * Store the given <code>{@link org.quartz.JobDetail}</code> and <code>{@link org.quartz.Trigger}</code>.
//     *
//     * @param newJob
//     *          The <code>JobDetail</code> to be stored.
//     * @param newTrigger
//     *          The <code>Trigger</code> to be stored.
//     * @throws ObjectAlreadyExistsException
//     *           if a <code>Job</code> with the same name/group already
//     *           exists.
//     */
//    void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws ObjectAlreadyExistsException, JobPersistenceException;
//    default void storeJobAndExecute(JobCfg jobCfg, ExecuteCfg executeCfg) throws ObjectAlreadyExistsException, JobPersistenceException{
//        throw new JobPersistenceException("Undefined logic");
//    }
//
//    /**
//     * Store the given <code>{@link org.quartz.JobDetail}</code>.
//     *
//     * @param newJob
//     *          The <code>JobDetail</code> to be stored.
//     * @param replaceExisting
//     *          If <code>true</code>, any <code>Job</code> existing in the
//     *          <code>JobStore</code> with the same name & group should be
//     *          over-written.
//     * @throws ObjectAlreadyExistsException
//     *           if a <code>Job</code> with the same name/group already
//     *           exists, and replaceExisting is set to false.
//     */
//    void storeJob(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException;

//    void storeJobsAndTriggers(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws ObjectAlreadyExistsException, JobPersistenceException;
//
//    /**
//     * Remove (delete) the <code>{@link org.quartz.Job}</code> with the given
//     * key, and any <code>{@link org.quartz.Trigger}</code> s that reference
//     * it.
//     *
//     * <p>
//     * If removal of the <code>Job</code> results in an empty group, the
//     * group should be removed from the <code>JobStore</code>'s list of
//     * known group names.
//     * </p>
//     *
//     * @return <code>true</code> if a <code>Job</code> with the given name &
//     *         group was found and removed from the store.
//     */
//    boolean removeJob(Key jobKey) throws JobPersistenceException;
    
//    boolean removeJobs(List<Key> keys) throws JobPersistenceException;
//
//    /**
//     * Retrieve the <code>{@link org.quartz.JobDetail}</code> for the given
//     * <code>{@link org.quartz.Job}</code>.
//     *
//     * @return The desired <code>Job</code>, or null if there is no match.
//     */
//    JobDetail retrieveJob(Key jobKey) throws JobPersistenceException;
//
//    /**
//     * Store the given <code>{@link org.quartz.Trigger}</code>.
//     *
//     * @param newTrigger
//     *          The <code>Trigger</code> to be stored.
//     * @param replaceExisting
//     *          If <code>true</code>, any <code>Trigger</code> existing in
//     *          the <code>JobStore</code> with the same name & group should
//     *          be over-written.
//     * @throws ObjectAlreadyExistsException
//     *           if a <code>Trigger</code> with the same name/group already
//     *           exists, and replaceExisting is set to false.
//     *
//     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
//     */
//    void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting)throws ObjectAlreadyExistsException, JobPersistenceException;
//
//    /**
//     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
//     * given key.
//     *
//     * <p>
//     * If removal of the <code>Trigger</code> results in an empty group, the
//     * group should be removed from the <code>JobStore</code>'s list of
//     * known group names.
//     * </p>
//     *
//     * <p>
//     * If removal of the <code>Trigger</code> results in an 'orphaned' <code>Job</code>
//     * that is not 'durable', then the <code>Job</code> should be deleted
//     * also.
//     * </p>
//     *
//     * @return <code>true</code> if a <code>Trigger</code> with the given
//     *         name & group was found and removed from the store.
//     */
//    boolean removeTrigger(Key key) throws JobPersistenceException;
//
//    boolean removeTriggers(List<Key> keys)throws JobPersistenceException;
//
//    /**
//     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
//     * given key, and store the new given one - which must be associated
//     * with the same job.
//     *
//     * @param newTrigger
//     *          The new <code>Trigger</code> to be stored.
//     *
//     * @return <code>true</code> if a <code>Trigger</code> with the given
//     *         name & group was found and removed from the store.
//     */
//    boolean replaceTrigger(Key triggerKey, OperableTrigger newTrigger) throws JobPersistenceException;
//
//    /**
//     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
//     *
//     * @return The desired <code>Trigger</code>, or null if there is no
//     *         match.
//     */
//    OperableTrigger retrieveTrigger(Key triggerKey) throws JobPersistenceException;
//
//    /**
//     * Determine whether a {@link Job} with the given identifier already
//     * exists within the scheduler.
//     *
//     * @param jobKey the identifier to check for
//     * @return true if a Job exists with the given identifier
//     * @throws SchedulerException
//     */
//    boolean checkDetailExists(Key jobKey) throws JobPersistenceException;
//    /**
//     * Clear (delete!) all scheduling data - all {@link Job}s, {@link Trigger}s
//     * {@link Calendar}s.
//     *
//     * @throws JobPersistenceException
//     */
//    void clearAllSchedulingData() throws JobPersistenceException;
    
//    /**
//     * Store the given <code>{@link org.quartz.Calendar}</code>.
//     *
//     * @param calendar
//     *          The <code>Calendar</code> to be stored.
//     * @param replaceExisting
//     *          If <code>true</code>, any <code>Calendar</code> existing
//     *          in the <code>JobStore</code> with the same name & group
//     *          should be over-written.
//     * @param updateTriggers
//     *          If <code>true</code>, any <code>Trigger</code>s existing
//     *          in the <code>JobStore</code> that reference an existing
//     *          Calendar with the same name with have their next fire time
//     *          re-computed with the new <code>Calendar</code>.
//     * @throws ObjectAlreadyExistsException
//     *           if a <code>Calendar</code> with the same name already
//     *           exists, and replaceExisting is set to false.
//     */
//    void storeCalendar(String name, Calendar calendar, boolean replaceExisting, boolean updateTriggers)throws ObjectAlreadyExistsException, JobPersistenceException;

//    /**
//     * Remove (delete) the <code>{@link org.quartz.Calendar}</code> with the
//     * given name.
//     *
//     * <p>
//     * If removal of the <code>Calendar</code> would result in
//     * <code>Trigger</code>s pointing to non-existent calendars, then a
//     * <code>JobPersistenceException</code> will be thrown.</p>
//     *       *
//     * @param calName The name of the <code>Calendar</code> to be removed.
//     * @return <code>true</code> if a <code>Calendar</code> with the given name
//     * was found and removed from the store.
//     */
//    boolean removeCalendar(String calName) throws JobPersistenceException;

//    /**
//     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
//     *
//     * @param calName
//     *          The name of the <code>Calendar</code> to be retrieved.
//     * @return The desired <code>Calendar</code>, or null if there is no
//     *         match.
//     */
//    Calendar retrieveCalendar(String calName) throws JobPersistenceException;

    /////////////////////////////////////////////////////////////////////////////
    //
    // Informational methods
    //
    /////////////////////////////////////////////////////////////////////////////

//    /**
//     * Get the number of <code>{@link org.quartz.Job}</code> s that are
//     * stored in the <code>JobsStore</code>.
//     */
//    int getNumberOfJobs() throws JobPersistenceException;
//
//    /**
//     * Get the number of <code>{@link org.quartz.Trigger}</code> s that are
//     * stored in the <code>JobsStore</code>.
//     */
//    int getNumberOfTriggers() throws JobPersistenceException;
//
//    /**
//     * Get the number of <code>{@link org.quartz.Calendar}</code> s that are
//     * stored in the <code>JobsStore</code>.
//     */
//    int getNumberOfCalendars()throws JobPersistenceException;

//    /**
//     * Get the keys of all of the <code>{@link org.quartz.Job}</code> s that
//     * have the given group name.
//     *
//     * <p>
//     * If there are no jobs in the given group name, the result should be
//     * an empty collection (not <code>null</code>).
//     * </p>
//     */
//    Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws JobPersistenceException;

//    /**
//     * Get the names of all of the <code>{@link org.quartz.Trigger}</code> s
//     * that have the given group name.
//     *
//     * <p>
//     * If there are no triggers in the given group name, the result should be a
//     * zero-length array (not <code>null</code>).
//     * </p>
//     */
//    Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException;

//    /**
//     * Get the names of all of the <code>{@link org.quartz.Job}</code>
//     * groups.
//     *
//     * <p>
//     * If there are no known group names, the result should be a zero-length
//     * array (not <code>null</code>).
//     * </p>
//     */
//    List<String> getJobGroupNames() throws JobPersistenceException;

//    /**
//     * Get the names of all of the <code>{@link org.quartz.Trigger}</code>
//     * groups.
//     *
//     * <p>
//     * If there are no known group names, the result should be a zero-length
//     * array (not <code>null</code>).
//     * </p>
//     */
//    List<String> getTriggerGroupNames() throws JobPersistenceException;

//    /**
//     * Get the names of all of the <code>{@link org.quartz.Calendar}</code> s
//     * in the <code>JobStore</code>.
//     *
//     * <p>
//     * If there are no Calendars in the given group name, the result should be
//     * a zero-length array (not <code>null</code>).
//     * </p>
//     */
//    List<String> getCalendarNames() throws JobPersistenceException;
//
//    /**
//     * Get all of the Triggers that are associated to the given Job.
//     *
//     * <p>
//     * If there are no matches, a zero-length array should be returned.
//     * </p>
//     */
//    List<OperableTrigger> getTriggersForJob(Key jobKey) throws JobPersistenceException;
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
//     */
//    void resetTriggerFromErrorState(TriggerKey triggerKey) throws JobPersistenceException;


    /////////////////////////////////////////////////////////////////////////////
    //
    // Trigger State manipulation methods
    //
    /////////////////////////////////////////////////////////////////////////////

//    /**
//     * Pause all of the <code>{@link org.quartz.Trigger}s</code> in the
//     * given group.
//     *
//     *
//     * <p>
//     * The JobStore should "remember" that the group is paused, and impose the
//     * pause on any new triggers that are added to the group while the group is
//     * paused.
//     * </p>
//     *
//     * @see #resumeTriggers(GroupMatcher)
//     */
//    Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) throws JobPersistenceException;

//    /**
//     * Pause the <code>{@link org.quartz.Job}</code> with the given name - by
//     * pausing all of its current <code>Trigger</code>s.
//     *
//     * @see #resumeJob(JobKey)
//     */
//    void pauseJob(Key key) throws JobPersistenceException;

//    /**
//     * Pause all of the <code>{@link org.quartz.Job}s</code> in the given
//     * group - by pausing all of their <code>Trigger</code>s.
//     *
//     * <p>
//     * The JobStore should "remember" that the group is paused, and impose the
//     * pause on any new jobs that are added to the group while the group is
//     * paused.
//     * </p>
//     *
//     * @see #resumeJobs(GroupMatcher)
//     */
//    Collection<String> pauseJobs(final String triggerName) throws JobPersistenceException;
//
//    /**
//     * Resume (un-pause) the <code>{@link org.quartz.Trigger}</code> with the
//     * given key.
//     *
//     * <p>
//     * If the <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     * </p>
//     *
//     * @see #pauseTrigger(TriggerKey)
//     */
//    void resumeTrigger(Key triggerKey) throws JobPersistenceException;

//    /**
//     * Resume (un-pause) all of the <code>{@link org.quartz.Trigger}s</code>
//     * in the given group.
//     *
//     * <p>
//     * If any <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     * </p>
//     *
//     * @see #pauseTriggers(GroupMatcher)
//     */
//    Collection<String> resumeTriggers(Key key) throws JobPersistenceException;
//
//    Set<String> getPausedTriggerGroups() throws JobPersistenceException;
//
//    /**
//     * Resume (un-pause) the <code>{@link org.quartz.Job}</code> with the
//     * given key.
//     *
//     * <p>
//     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
//     * or more fire-times, then the <code>Trigger</code>'s misfire
//     * instruction will be applied.
//     * </p>
//     *
//     * @see #pauseJob(JobKey)
//     */
//    void resumeJob(Key jobKey) throws JobPersistenceException;

//    /**
//     * Resume (un-pause) all of the <code>{@link org.quartz.Job}s</code> in
//     * the given group.
//     *
//     * <p>
//     * If any of the <code>Job</code> s had <code>Trigger</code> s that
//     * missed one or more fire-times, then the <code>Trigger</code>'s
//     * misfire instruction will be applied.
//     * </p>
//     *
//     * @see #pauseJobs(GroupMatcher)
//     */
//    Collection<String> resumeJobs(final String triggerName) throws JobPersistenceException;

//    /**
//     * Pause all triggers - equivalent of calling <code>pauseTriggerGroup(group)</code>
//     * on every group.
//     *
//     * <p>
//     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
//     * instructions WILL be applied.
//     * </p>
//     *
//     * @see #resumeAll()
//     * @see #pauseTriggers(GroupMatcher)
//     */
//    void pauseAll() throws JobPersistenceException;
//
//    /**
//     * Resume (un-pause) all triggers - equivalent of calling <code>resumeTriggerGroup(group)</code>
//     * on every group.
//     *
//     * <p>
//     * If any <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     * </p>
//     *
//     * @see #pauseAll()
//     */
//    void resumeAll() throws JobPersistenceException;

    /////////////////////////////////////////////////////////////////////////////
    //
    // Trigger-Firing methods
    //
    /////////////////////////////////////////////////////////////////////////////

//    /**
//     * Get a handle to the next trigger to be fired, and mark it as 'reserved'
//     * by the calling scheduler.
//     * 获取下一个要触发的触发器的句柄，并由调用调度程序将其标记为“保留”
//     *
//     * @param noLaterThan If > 0, the JobStore should only return a Trigger
//     * that will fire no later than the time represented in this value as
//     * milliseconds.
//     * @see #releaseAcquiredTrigger(OperableTrigger)
//     */
//    List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) throws JobPersistenceException;
    default List<QrtzExecute> acquireNextTriggers(String application,long _tsw,long _tew) throws JobPersistenceException{
        throw new JobPersistenceException("method is not defined ! "+application+" : "+_tsw+","+_tew);
    }

//    /**
//     * Inform the <code>JobStore</code> that the scheduler no longer plans to
//     * fire the given <code>Trigger</code>, that it had previously acquired
//     * (reserved).
//     */
//    void releaseAcquiredTrigger(OperableTrigger trigger);

//    /**
//     * Inform the <code>JobStore</code> that the scheduler is now firing the
//     * given <code>Trigger</code> (executing its associated <code>Job</code>),
//     * that it had previously acquired (reserved).
//     *
//     * @return may return null if all the triggers or their calendars no longer exist, or
//     *         if the trigger was not successfully put into the 'executing'
//     *         state.  Preference is to return an empty list if none of the triggers
//     *         could be fired.
//     */
//    List<TriggerFiredResult> triggersFired(List<OperableTrigger> triggers) throws JobPersistenceException;
//
//    /**
//     * Inform the <code>JobStore</code> that the scheduler has completed the
//     * firing of the given <code>Trigger</code> (and the execution of its
//     * associated <code>Job</code> completed, threw an exception, or was vetoed),
//     * and that the <code>{@link org.quartz.JobDataMap}</code>
//     * in the given <code>JobDetail</code> should be updated if the <code>Job</code>
//     * is stateful.
//     */
//    void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode);

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's Id,
     * prior to initialize being invoked.
     *  eg: QUARTZ-SPRINGBOOT#192.168.1.1
     * @since 1.7
     */
    void setInstanceId(String schedInstId);

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's name,
     * prior to initialize being invoked.
     *  eg: QUARTZ-SPRINGBOOT
     * @since 1.7
     */
//    void setInstanceName(String schedName);
    void setApplication(String application);
    String getInstanceName();

    /**
     * Tells the JobStore the pool size used to execute jobs
     * @param poolSize amount of threads allocated for job execution
     * @since 2.0
     */
    void setThreadPoolSize(int poolSize);

//    /**
//     * Get the amount of time (in ms) to wait when accessing this job store
//     * repeatedly fails.
//     *
//     * Called by the executor thread(s) when calls to
//     * {@link #acquireNextTriggers} fail more than once in succession, and the
//     * thread thus wants to wait a bit before trying again, to not consume
//     * 100% CPU, write huge amounts of errors into logs, etc. in cases like
//     * the DB being offline/restarting.
//     *
//     * The delay returned by implementations should be between 20 and
//     * 600000 milliseconds.
//     *
//     * @param failureCount the number of successive failures seen so far
//     * @return the time (in milliseconds) to wait before trying again
//     */
//    long getAcquireRetryDelay(int failureCount);

    default String findNodeStateByPK(String application, String hostIP){
//        throw new Exception("findNodeStateByPK method is not defined ! "+application+" : "+hostIP);
        return null;
    }

    default int toLockAndUpdate(QrtzExecute newCe, String oldState, long oldPrevTime, long oldNextTime){
//        throw new JobPersistenceException("toLockAndUpdate method is not defined ! "+newCe+" : "+oldState);
        return 0;
    }



    String[] getDBInfo()  ;
    List<QrtzApp> getAllApp();
    QrtzApp getAppByApplication(String application);

    List<QrtzNode> getNodeByApp(String application);
    // 根据job_id获取job信息
    QrtzJob getJobByJobId(String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzExecute getExecuteByExecuteId( String execute_id);
    // 根据job_id获取job下所有execute信息
    List<QrtzExecute> getExecuteByJobId(String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzJob getJobInAllByJobId(String job_id);
    // 根据execute_id获取execute及job信息
    QrtzExecute getExecuteInAllByExecuteId(String execute_id);
    // 添加应用
    int addApp(QrtzApp qrtzApp);
    // 删除应用
    int deleteApp(String application);
    // 暂停/启动应用
    int updateAppState(String application,String state);
    // 添加节点
    int addNode(QrtzNode qrtzNode);
    boolean containsNode(String application ,String hostIP);
    boolean containsNode(String application);
    // 删除节点
    int deleteNode(String application,String hostIP);
    // 暂停节点
    int updateNodeState(QrtzNode qrtzNode);
    int updateNode(QrtzNode qrtzNode);

    // 添加应用及节点
    int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode);

    int addJob(QrtzJob qrtzJob) ;
    int updateJob(QrtzJob qrtzJob) ;
    int deleteJob(String job_id) ;
    //    int findQrtzExecuteCountById(Long job_id);
    boolean containsExecute(String job_id);

    // 暂停指定job下的所有execute
    int updateJobState(String job_id, String state);
    // 暂停指定execute
    int updateExecuteState(String execute_id, String state);

    // 添加execute
    int addExecute(QrtzExecute qrtzExecute);
    // 删除execute
    int deleteExecute(String execute_id );

    int updateExecute(QrtzExecute qrtzExecute);

}
