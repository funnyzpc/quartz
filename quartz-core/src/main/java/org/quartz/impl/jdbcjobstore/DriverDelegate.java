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

package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobPersistenceException;
import org.quartz.Trigger;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.utils.Key;
import org.slf4j.Logger;

/**
 * <p>
 * This is the base interface for all driver delegate classes.
 * 这是所有驱动程序委派类的基本接口。
 * </p>
 * 
 * <p>
 * This interface is very similar to the <code>{@link
 * org.quartz.spi.JobStore}</code>
 * interface except each method has an additional <code>{@link java.sql.Connection}</code>
 * parameter.
 * 这个接口与org.quartz.spi非常相似。除了每个方法外，JobStore接口都有一个额外的Connection参数。
 * </p>
 * 
 * <p>
 * Unless a database driver has some <strong>extremely-DB-specific</strong>
 * requirements, any DriverDelegate implementation classes should extend the
 * <code>{@link org.quartz.impl.jdbcjobstore.StdJDBCDelegate}</code> class.
 * 除非数据库驱动程序有一些非常特定于数据库的要求，否则任何DriverDelegate实现类都应该扩展StdJDBCDelegate类。
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 */
public interface DriverDelegate {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * @param initString of the format: settingName=settingValue|otherSettingName=otherSettingValue|...
     * @throws NoSuchDelegateException
     */
    void initialize(Logger logger, String tablePrefix, String schedName, String instanceId, ClassLoadHelper classLoadHelper, boolean useProperties, String initString) throws NoSuchDelegateException;


    //---------------------------------------------------------------------------
    // jobs
    //---------------------------------------------------------------------------

//    /**
//     * <p>
//     * Insert the job detail record.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param job
//     *          the job to insert
//     * @return number of rows inserted
//     * @throws IOException
//     *           if there were problems serializing the JobDataMap
//     */
//    int insertJobDetail(Connection conn, JobDetail job) throws IOException, SQLException;
//
//    /**
//     * <p>
//     * Update the job detail record.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param job
//     *          the job to update
//     * @return number of rows updated
//     * @throws IOException
//     *           if there were problems serializing the JobDataMap
//     */
//    int updateJobDetail(Connection conn, JobDetail job) throws IOException, SQLException;

//    /**
//     * <p>
//     * Check whether or not the given job disallows concurrent execution.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return true if the job exists and disallows concurrent execution, false otherwise
//     */
//    @Deprecated
//    boolean isJobNonConcurrent(Connection conn,Key jobKey) throws SQLException;

//    /**
//     * <p>
//     * Update the job data map for the given job.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param job
//     *          the job to update
//     * @return the number of rows updated
//     * @throws IOException
//     *           if there were problems serializing the JobDataMap
//     */
//    int updateJobData(Connection conn, JobDetail job) throws IOException, SQLException;
//
//    /**
//     * <p>
//     * Select the JobDetail object for a given job name / group name.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return the populated JobDetail object
//     * @throws ClassNotFoundException
//     *           if a class found during deserialization cannot be found or if
//     *           the job class could not be found
//     * @throws IOException
//     *           if deserialization causes an error
//     */
//    @Deprecated
//    JobDetail selectJobDetail(Connection conn,Key jobKey, ClassLoadHelper loadHelper) throws ClassNotFoundException, IOException, SQLException;
//    JobDetail selectJobCfg(Connection conn,Key jobKey, ClassLoadHelper loadHelper) throws ClassNotFoundException, IOException, SQLException;

//    /**
//     * <p>
//     * Select all of the job group names that are stored.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return an array of <code>String</code> group names
//     */
//    List<String> selectJobGroups(Connection conn) throws SQLException;
//
//    /**
//     * <p>
//     * Select all of the jobs contained in a given group.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param matcher
//     *          the group matcher to evaluate against the known jobs
//     * @return an array of <code>String</code> job names
//     */
//    Set<JobKey> selectJobsInGroup(Connection conn, GroupMatcher<JobKey> matcher) throws SQLException;

//
//    //---------------------------------------------------------------------------
//    // triggers
//    //---------------------------------------------------------------------------
//
//    /**
//     * <p>
//     * Insert the base trigger data.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param trigger
//     *          the trigger to insert
//     * @param state
//     *          the state that the trigger should be stored in
//     * @return the number of rows inserted
//     */
//    int insertTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException;
//
//    /**
//     * <p>
//     * Update the base trigger data.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param trigger
//     *          the trigger to insert
//     * @param state
//     *          the state that the trigger should be stored in
//     * @return the number of rows updated
//     */
////    int updateTrigger(Connection conn, OperableTrigger trigger, String state,JobDetail jobDetail) throws SQLException, IOException;
//    int updateJobCfg(Connection conn, OperableTrigger trigger, String state,JobDetail jobDetail) throws SQLException, IOException;

//    /**
//     * <p>
//     * Update all triggers in the given group to the given new state, if they
//     * are in one of the given old states.
//     * </p>
//     *
//     * @param conn
//     *          the DB connection
//     * @param matcher
//     *          the group matcher to evaluate against the known triggers
//     * @param newState
//     *          the new state for the trigger
//     * @param oldState1
//     *          one of the old state the trigger must be in
//     * @param oldState2
//     *          one of the old state the trigger must be in
//     * @param oldState3
//     *          one of the old state the trigger must be in
//     * @return int the number of rows updated
//     * @throws SQLException
//     */
//    int updateTriggerGroupStateFromOtherStates(Connection conn, GroupMatcher<Key<?>> matcher, String newState, String oldState1, String oldState2, String oldState3) throws SQLException;

//    /**
//     * <p>
//     * Update all of the triggers of the given group to the given new state, if
//     * they are in the given old state.
//     * </p>
//     *
//     * @param conn
//     *          the DB connection
//     * @param matcher
//     *          the matcher to evaluate against the known triggers
//     * @param newState
//     *          the new state for the trigger group
//     * @param oldState
//     *          the old state the triggers must be in
//     * @return int the number of rows updated
//     * @throws SQLException
//     */
//    int updateTriggerGroupStateFromOtherState(Connection conn, GroupMatcher<Key<?>> matcher, String newState, String oldState) throws SQLException;
//
//    /**
//     * <p>
//     * Update the states of all triggers associated with the given job.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @param state
//     *          the new state for the triggers
//     * @return the number of rows updated
//     */
//    int updateTriggerStatesForJob(Connection conn,Key jobKey,String state) throws SQLException;
//
//    /**
//     * <p>
//     * Update the states of any triggers associated with the given job, that
//     * are the given current state.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @param state
//     *          the new state for the triggers
//     * @param oldState
//     *          the old state of the triggers
//     * @return the number of rows updated
//     */
//    int updateTriggerStatesForJobFromOtherState(Connection conn,Key jobKey, String state, String oldState) throws SQLException;
//
//    /**
//     * <p>
//     * Delete the base trigger data for a trigger.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return the number of rows deleted
//     */
//    int deleteTrigger(Connection conn,Key key) throws SQLException;
//
//    /**
//     * <p>
//     * Select the number of triggers associated with a given job.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the number of triggers for the given job
//     */
//    int selectNumTriggersForJob(Connection conn,Key jobKey) throws SQLException;
//
//    /**
//     * <p>
//     * Select the job to which the trigger is associated.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return the <code>{@link org.quartz.JobDetail}</code> object
//     *         associated with the given trigger
//     */
//    JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper,Key triggerKey) throws ClassNotFoundException, SQLException;
//
//    /**
//     * <p>
//     * Select the job to which the trigger is associated. Allow option to load actual job class or not. When case of
//     * remove, we do not need to load the class, which in many cases, it's no longer exists.
//     * </p>
//     */
//    JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper,Key key, boolean loadJobClass) throws ClassNotFoundException, SQLException;
//
//    /**
//     * <p>
//     * Select the triggers for a job
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return an array of <code>(@link org.quartz.Trigger)</code> objects
//     *         associated with a given job.
//     * @throws SQLException
//     * @throws JobPersistenceException
//     */
//    List<OperableTrigger> selectTriggersForJob(Connection conn, Key jobKey) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException;
//
//    /**
//     * <p>
//     * Select the triggers for a calendar
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calName
//     *          the name of the calendar
//     * @return an array of <code>(@link org.quartz.Trigger)</code> objects
//     *         associated with the given calendar.
//     * @throws SQLException
//     * @throws JobPersistenceException
//     */
//    List<OperableTrigger> selectTriggersForCalendar(Connection conn, String calName) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException;
//
//    /**
//     * <p>
//     * Select a trigger.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return the <code>{@link org.quartz.Trigger}</code> object
//     * @throws JobPersistenceException
//     */
//    OperableTrigger selectTrigger(Connection conn,Key triggerKey) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException;
//    OperableTrigger selectJobCfgTrigger(Connection conn,Key triggerKey) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException;

    /**
     * <p>
     * Select a trigger's JobDataMap.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the <code>{@link org.quartz.JobDataMap}</code> of the Trigger,
     * never null, but possibly empty.
     */
//    JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName,String groupName) throws SQLException, ClassNotFoundException,IOException;
//    JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName ) throws SQLException, ClassNotFoundException,IOException;

//    /**
//     * <p>
//     * Select all of the trigger group names that are stored.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return an array of <code>String</code> group names
//     */
//    List<String> selectTriggerGroups(Connection conn) throws SQLException;

//    List<String> selectTriggerGroups(Connection conn, GroupMatcher<TriggerKey> matcher) throws SQLException;

//    /**
//     * <p>
//     * Select all of the triggers contained in a given group.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param matcher
//     *          to evaluate against known triggers
//     * @return a Set of <code>TriggerKey</code>s
//     */
//    @Deprecated
//    Set<Key> selectTriggersInGroup(Connection conn, GroupMatcher<Key<?>> matcher) throws SQLException;

//    int insertPausedTriggerGroup(Connection conn, String groupName) throws SQLException;

//    int deletePausedTriggerGroup(Connection conn, String groupName) throws SQLException;

//    int deletePausedTriggerGroup(Connection conn, GroupMatcher<TriggerKey> matcher) throws SQLException;

//    @Deprecated
//    int deleteAllPausedTriggerGroups(Connection conn) throws SQLException;

//    boolean isTriggerGroupPaused(Connection conn, String groupName) throws SQLException;

//    Set<String> selectPausedTriggerGroups(Connection conn) throws SQLException;
    
//    boolean isExistingTriggerGroup(Connection conn, String groupName)throws SQLException;

    //---------------------------------------------------------------------------
    // calendars
    //---------------------------------------------------------------------------

//    /**
//     * <p>
//     * Insert a new calendar.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name for the new calendar
//     * @param calendar
//     *          the calendar
//     * @return the number of rows inserted
//     * @throws IOException
//     *           if there were problems serializing the calendar
//     */
//    int insertCalendar(Connection conn, String calendarName,Calendar calendar) throws IOException, SQLException;

//    /**
//     * <p>
//     * Update a calendar.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name for the new calendar
//     * @param calendar
//     *          the calendar
//     * @return the number of rows updated
//     * @throws IOException
//     *           if there were problems serializing the calendar
//     */
//    int updateCalendar(Connection conn, String calendarName,Calendar calendar) throws IOException, SQLException;

//    /**
//     * <p>
//     * Check whether or not a calendar exists.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name of the calendar
//     * @return true if the trigger exists, false otherwise
//     */
//    boolean calendarExists(Connection conn, String calendarName) throws SQLException;

//    /**
//     * <p>
//     * Select a calendar.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name of the calendar
//     * @return the Calendar
//     * @throws ClassNotFoundException
//     *           if a class found during deserialization cannot be found be
//     *           found
//     * @throws IOException
//     *           if there were problems deserializing the calendar
//     */
//    Calendar selectCalendar(Connection conn, String calendarName) throws ClassNotFoundException, IOException, SQLException;

//    /**
//     * <p>
//     * Check whether or not a calendar is referenced by any triggers.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name of the calendar
//     * @return true if any triggers reference the calendar, false otherwise
//     */
//    boolean calendarIsReferenced(Connection conn, String calendarName) throws SQLException;

//    /**
//     * <p>
//     * Delete a calendar.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name of the trigger
//     * @return the number of rows deleted
//     */
//    int deleteCalendar(Connection conn, String calendarName) throws SQLException;

//    /**
//     * <p>
//     * Select the total number of calendars stored.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the total number of calendars stored
//     */
//    int selectNumCalendars(Connection conn) throws SQLException;

//    /**
//     * <p>
//     * Select all of the stored calendars.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return an array of <code>String</code> calendar names
//     */
//    List<String> selectCalendars(Connection conn) throws SQLException;

    //---------------------------------------------------------------------------
    // trigger firing
    //---------------------------------------------------------------------------

//    /**
//     * <p>
//     * Select the next time that a trigger will be fired.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the next fire time, or 0 if no trigger will be fired
//     *
//     * @deprecated Does not account for misfires.
//     */
//    long selectNextFireTime(Connection conn) throws SQLException;

//    /**
//     * <p>
//     * Insert a fired trigger.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param trigger
//     *          the trigger
//     * @param state
//     *          the state that the trigger should be stored in
//     * @return the number of rows inserted
//     */
//    int insertFiredTrigger(Connection conn, OperableTrigger trigger,String state, JobDetail jobDetail) throws SQLException;
//
//    /**
//     * <p>
//     * Update a fired trigger record.  Will update the fields
//     * "firing instance", "fire time", and "state".
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param trigger
//     *          the trigger
//     * @param state
//     *          the state that the trigger should be stored in
//     * @return the number of rows inserted
//     */
//    int updateFiredTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException;
//    /**
//     * <p>
//     * Get the number instances of the identified job currently executing.
//     * 获取当前正在执行的已标识作业的实例数。
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     *
//     * @return the number instances of the identified job currently executing.
//     */
//    int selectJobExecutionCount(Connection conn, JobKey jobKey) throws SQLException;
//    /**
//     * <p>
//     * A List of all current <code>SchedulerStateRecords</code>.
//     * </p>
//     *
//     * <p>
//     * If instanceId is not null, then only the record for the identified
//     * instance will be returned.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     */
//    List<SchedulerStateRecord> selectSchedulerStateRecords(Connection conn, String instanceId) throws SQLException;

//    int insertJobCfg(Connection conn, JobCfg jobCfg) throws SQLException, IOException;
//    int insertExecuteCfg(Connection conn, ExecuteCfg jobCfg) throws SQLException, IOException;

    // 写入 qrtz_app 应用信息
    int insertQrtzApp(Connection conn, QrtzApp app);

    // 写入 qrtz_node 节点信息
    int insertQrtzNode(Connection conn, QrtzNode node);
    QrtzApp findQrtzAppByApp(Connection conn,String application);

    // 清理历史数据
    void clearHistoryData(Connection conn,Long timeLimit);

    int updateQrtzAppByApp(Connection conn, QrtzApp app/*,long now,String wState*/);

    QrtzNode findQrtzNodeByAppHost(Connection conn,final String app, final String hostIP);

    void updateQrtzNodeOfState(Connection conn, QrtzNode node);

    void updateQrtzNodeOfTimeCheck(Connection conn, QrtzNode node);

    int clearAllExecuteData(Connection conn, long timeLimit);

    List<QrtzJob> findQrtzJobByAppForRecover(Connection conn, String applicaton);

    List<QrtzExecute> findQrtzExecuteForRecover(Connection conn, List<QrtzJob> jobs,long now);
    int updateRecoverExecute(Connection conn, QrtzExecute execute);

    int clearAllJobData(Connection conn, long timeLimit);

    int updateRecoverJob(Connection conn, QrtzJob job);

    List<QrtzExecute> findAllQrtzExecuteByPID(Connection conn, Long id);

    String findNodeStateByPK(Connection conn,String application, String hostIP);

    List<QrtzExecute> selectExecuteAndJobToAcquire(Connection conn, String application, long _tsw,long _tew,String state);

    int toLockAndUpdate(Connection conn, QrtzExecute newCe, String oldState, long oldPrevTime, long oldNextTime);

}

// EOF
