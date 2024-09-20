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

import static org.quartz.utils.Key.key;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

import org.quartz.Job;
import org.quartz.JobPersistenceException;
import org.quartz.Trigger;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.impl.jdbcjobstore.TriggerPersistenceDelegate.TriggerPropertyBundle;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.Key;
import org.slf4j.Logger;

/**
 * <p>
 * This is meant to be an abstract base class for most, if not all, <code>{@link org.quartz.impl.jdbcjobstore.DriverDelegate}</code>
 * implementations. Subclasses should override only those methods that need
 * special handling for the DBMS driver in question.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 * @author Eric Mueller
 */
public class StdJDBCDelegate implements DriverDelegate, StdJDBCConstants {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected Logger logger = null;

    protected String tablePrefix = "QRTZ_";

    protected String instanceId;

    protected String schedName;

    protected boolean useProperties;
    
    protected ClassLoadHelper classLoadHelper;

    protected List<TriggerPersistenceDelegate> triggerPersistenceDelegates = new LinkedList<TriggerPersistenceDelegate>();

    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create new StdJDBCDelegate instance.
     * </p>
     */
    public StdJDBCDelegate() {
    }

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
    @Override
    public void initialize(Logger logger, String tablePrefix, String schedName, String instanceId, ClassLoadHelper classLoadHelper, boolean useProperties, String initString) throws NoSuchDelegateException {
        this.logger = logger;
        this.tablePrefix = tablePrefix;
        this.schedName = schedName;
        this.instanceId = instanceId;
        this.useProperties = useProperties;
        this.classLoadHelper = classLoadHelper;
        addDefaultTriggerPersistenceDelegates();

        if(initString == null){
            return;
        }
        String[] settings = initString.split("\\|");
        for(String setting: settings) {
            String[] parts = setting.split("=");
            String name = parts[0];
            if(parts.length == 1 || parts[1] == null || parts[1].equals("")){
                continue;
            }
            if(name.equals("triggerPersistenceDelegateClasses")) {
                String[] trigDelegates = parts[1].split(",");
                for(String trigDelClassName: trigDelegates) {
                    try {
                        Class<?> trigDelClass = classLoadHelper.loadClass(trigDelClassName);
                        addTriggerPersistenceDelegate((TriggerPersistenceDelegate) trigDelClass.newInstance());
                    } catch (Exception e) {
                        throw new NoSuchDelegateException("Error instantiating TriggerPersistenceDelegate of type: " + trigDelClassName, e);
                    } 
                }
            }
            else{
                throw new NoSuchDelegateException("Unknown setting: '" + name + "'");
            }
        }
    }

    protected void addDefaultTriggerPersistenceDelegates() {
        addTriggerPersistenceDelegate(new SimpleTriggerPersistenceDelegate());
        addTriggerPersistenceDelegate(new CronTriggerPersistenceDelegate());
//        addTriggerPersistenceDelegate(new CalendarIntervalTriggerPersistenceDelegate());
//        addTriggerPersistenceDelegate(new DailyTimeIntervalTriggerPersistenceDelegate());
    }

    protected boolean canUseProperties() {
        return useProperties;
    }
    
    public void addTriggerPersistenceDelegate(TriggerPersistenceDelegate delegate) {
        logger.debug("Adding TriggerPersistenceDelegate of type: " + delegate.getClass().getCanonicalName());
        delegate.initialize(tablePrefix, schedName);
        this.triggerPersistenceDelegates.add(delegate);
    }
    
//    public TriggerPersistenceDelegate findTriggerPersistenceDelegate(OperableTrigger trigger)  {
//        for(TriggerPersistenceDelegate delegate: triggerPersistenceDelegates) {
//            if(delegate.canHandleTriggerType(trigger)){
//                return delegate;
//            }
//        }
//        logger.error("delegate is not found:{}",trigger.getClass().getName());
//        return null;
////        throw new NoSuchDelegateException("Unknown setting: '" + trigger.getClass().getName() + "'");
//    }
//    /**
//     * <p>
//     * Get the names of all of the triggers in the given group and state that
//     * have misfired.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return an array of <code>{@link
//     * org.quartz.utils.Key}</code> objects
//     */
//    @Override
//    public List<TriggerKey> selectMisfiredTriggersInGroupInState(Connection conn, String groupName, String state, long ts) throws SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//
//        try {
//            ps = conn.prepareStatement(rtp(SELECT_MISFIRED_TRIGGERS_IN_GROUP_IN_STATE));
//            ps.setBigDecimal(1, new BigDecimal(String.valueOf(ts)));
//            ps.setString(2, groupName);
//            ps.setString(3, state);
//            rs = ps.executeQuery();
//
//            LinkedList<TriggerKey> list = new LinkedList<TriggerKey>();
//            while (rs.next()) {
//                String triggerName = rs.getString(COL_TRIGGER_NAME);
//                list.add(triggerKey(triggerName, groupName));
//            }
//            return list;
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }

//
//    @Override
//    public int insertJobCfg(Connection conn, JobCfg jobCfg) throws SQLException, IOException {
//        PreparedStatement ps = null;
//        int insertResult = 0;
//        final String triggerType = jobCfg.getTriggerType();
//        JobCfgImpl jobCfg1 = (JobCfgImpl)jobCfg;
//        try {
//            ps = conn.prepareStatement(rtp(INSERT_JOB_CFG));
//            ps.setString(1,jobCfg1.getSchedName()); // SCHED_NAME
//            ps.setString(2,jobCfg1.getTriggerName()); // TRIGGER_NAME
//            ps.setString(3,jobCfg1.getTriggerType()); // TRIGGER_TYPE
//            ps.setString(4,jobCfg1.getTriggerState()); // TRIGGER_STATE
//            ps.setString(5,jobCfg1.getDescription()); // DESCRIPTION
////            ps.setLong(6,jobCfg1.getNextFireTime()); // NEXT_FIRE_TIME
//            ps.setObject(6,jobCfg1.getNextFireTime()); // NEXT_FIRE_TIME
////            ps.setLong(7,jobCfg1.getPrevFireTime()); // PREV_FIRE_TIME
//            ps.setObject(7,jobCfg1.getPrevFireTime()); // PREV_FIRE_TIME
//            ps.setInt(8,jobCfg1.getPriority()); // PRIORITY
////            ps.setLong(9,jobCfg1.getStartTime()); // START_TIME
//            ps.setObject(9,jobCfg1.getStartTime()); // START_TIME
////            ps.setLong(10,jobCfg1.getEndTime()); // END_TIME
//            ps.setObject(10,jobCfg1.getEndTime()); // END_TIME
//            ps.setString(11,jobCfg1.getCalendarName()); // CALENDAR_NAME
////            ps.setInt(12,jobCfg1.getMisfireInstr()); // MISFIRE_INSTR
//            ps.setObject(12,jobCfg1.getMisfireInstr()); // MISFIRE_INSTR
//            ps.setString(13,jobCfg1.getJobClassName()); // JOB_CLASS_NAME
//            ps.setBoolean(14,jobCfg1.getIsNonconcurrent()); // IS_NONCONCURRENT
//            ps.setBoolean(15,jobCfg1.getIsUpdateData()); // IS_UPDATE_DATA
//            ps.setBoolean(16,jobCfg1.getRequestsRecovery()); // REQUESTS_RECOVERY
//            ps.setString(17,jobCfg1.getJobData()); // JOB_DATA
//            insertResult = ps.executeUpdate();
//        } finally {
//            // 由于实在同一个connection下，所以不可关闭Connection
//            closeStatement(ps);
//        }
//        return insertResult;
//    }
//
//    @Override
//    public int insertExecuteCfg(Connection conn, ExecuteCfg executeCfg) throws SQLException, IOException {
//        PreparedStatement ps = null;
//        int insertResult = 0;
//        final String triggerType = executeCfg.getTriggerType();
//        final ExecuteCfgImpl executeCfg1 = (ExecuteCfgImpl)executeCfg;
//        try {
//            ps = conn.prepareStatement(rtp(INSERT_EXECUTE_CFG));
//            ps.setString(1,executeCfg1.getSchedName());// SCHED_NAME
//            ps.setString(2,executeCfg1.getTriggerName());// TRIGGER_NAME
//            ps.setString(3,executeCfg1.getTriggerType());// TRIGGER_TYPE
//            ps.setString(4,executeCfg1.getCronExpression());// CRON_EXPRESSION
//            ps.setString(5,executeCfg1.getTimeZoneId());// TIME_ZONE_ID
////            ps.setLong(6,executeCfg1.getRepeatCount());// REPEAT_COUNT
//            ps.setObject(6,executeCfg1.getRepeatCount());// REPEAT_COUNT
////            ps.setLong(7,executeCfg1.getRepeatInterval());// REPEAT_INTERVAL
//            ps.setObject(7,executeCfg1.getRepeatInterval());// REPEAT_INTERVAL
////            ps.setLong(8,executeCfg1.getTimesTriggered());// TIMES_TRIGGERED
//            ps.setObject(8,executeCfg1.getTimesTriggered());// TIMES_TRIGGERED
//            insertResult = ps.executeUpdate();
//        } finally {
//            // 由于实在同一个connection下，所以不可关闭Connection
//            closeStatement(ps);
//        }
//        return insertResult;
//    }

    //---------------------------------------------------------------------------
    // jobs
    //---------------------------------------------------------------------------
//
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
//    @Override
//    public int insertJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
//        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
//        PreparedStatement ps = null;
//        int insertResult = 0;
//        try {
//            ps = conn.prepareStatement(rtp(INSERT_JOB_DETAIL));
//            ps.setString(1, job.getKey().getName());
////            ps.setString(2, job.getKey().getGroup());
//            ps.setString(2, job.getDescription());
//            ps.setString(3, job.getJobClassName());
////            setBoolean(ps, 4, job.isDurable());
//            setBoolean(ps, 4, job.isConcurrentExectionDisallowed());
//            setBoolean(ps, 5, job.isPersistJobDataAfterExecution());
//            setBoolean(ps, 6, job.requestsRecovery());
//            setBytes(ps, 7, baos);
//            insertResult = ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//        return insertResult;
//    }
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
//    @Override
//    public int updateJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
//        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
//        PreparedStatement ps = null;
//        int insertResult = 0;
//        try {
//            // UPDATE QRTZ_JOB_DETAILS SET DESCRIPTION = ?, JOB_CLASS_NAME = ?, IS_DURABLE = ?, IS_NONCONCURRENT = ?, IS_UPDATE_DATA = ?, REQUESTS_RECOVERY = ?, JOB_DATA = ?  WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//            ps = conn.prepareStatement(rtp(UPDATE_JOB_DETAIL));
//            ps.setString(1, job.getDescription());
//            ps.setString(2, job.getJobClassName());
////            setBoolean(ps, 3, job.isDurable());
//            setBoolean(ps, 3, job.isConcurrentExectionDisallowed());
//            setBoolean(ps, 4, job.isPersistJobDataAfterExecution());
//            setBoolean(ps, 5, job.requestsRecovery());
//            setBytes(ps, 6, baos);
//            ps.setString(7, job.getKey().getName());
////            ps.setString(9, job.getKey().getGroup());
//            insertResult = ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//        return insertResult;
//    }

//    /**
//     * <p>
//     * Check whether or not the given job is stateful.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return true if the job exists and is stateful, false otherwise
//     */
//    @Override
//    public boolean isJobNonConcurrent(Connection conn,Key jobKey) throws SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            // SELECT IS_NONCONCURRENT FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//            ps = conn.prepareStatement(rtp(SELECT_JOB_NONCONCURRENT));
//            ps.setString(1, jobKey.getName());
////            ps.setString(2, jobKey.getGroup());
//            rs = ps.executeQuery();
//            if (!rs.next()) { return false; }
//            return getBoolean(rs, COL_IS_NONCONCURRENT);
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }


//    /**
//     * <p>
//     * Update all triggers in the given group to the given new state, if they
//     * are in one of the given old states.
//     * </p>
//     *
//     * @param conn
//     *          the DB connection
//     * @param matcher
//     *          the groupMatcher to evaluate the triggers against
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
//    @Override
//    public int updateTriggerGroupStateFromOtherStates(Connection conn,GroupMatcher<Key<?>> matcher, String newState, String oldState1,String oldState2, String oldState3) throws SQLException {
//        PreparedStatement ps = null;
//        try {
//            // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ? AND (TRIGGER_STATE = ? OR TRIGGER_STATE = ? OR TRIGGER_STATE = ?)
//            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER_GROUP_STATE_FROM_STATES));
//            ps.setString(1, newState);
////            ps.setString(2, toSqlLikeClause(matcher));
//            ps.setString(2, oldState1);
//            ps.setString(3, oldState2);
//            ps.setString(4, oldState3);
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }


//    /**
//     * <p>
//     * Update all of the triggers of the given group to the given new state, if
//     * they are in the given old state.
//     * </p>
//     *
//     * @param conn
//     *          the DB connection
//     * @param matcher
//     *          the groupMatcher to evaluate the triggers against
//     * @param newState
//     *          the new state for the trigger group
//     * @param oldState
//     *          the old state the triggers must be in
//     * @return int the number of rows updated
//     * @throws SQLException
//     */
//    @Override
//    public int updateTriggerGroupStateFromOtherState(Connection conn,GroupMatcher<Key<?>> matcher, String newState, String oldState) throws SQLException {
//        PreparedStatement ps = null;
//        try {
//            // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ? AND TRIGGER_STATE = ?
//            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER_GROUP_STATE_FROM_STATE));
//            ps.setString(1, newState);
////            ps.setString(2, toSqlLikeClause(matcher));
//            ps.setString(2, oldState);
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }
//
//    /**
//     * <p>
//     * Update the states of all triggers associated with the given job.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param state
//     *          the new state for the triggers
//     * @return the number of rows updated
//     */
//    @Override
//    public int updateTriggerStatesForJob(Connection conn,Key jobKey, String state) throws SQLException {
//        PreparedStatement ps = null;
//        try {
//            // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//            ps = conn.prepareStatement(rtp(UPDATE_JOB_TRIGGER_STATES));
//            ps.setString(1, state);
//            ps.setString(2, jobKey.getName());
////            ps.setString(3, jobKey.getGroup());
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }
//    @Override
//    public int updateTriggerStatesForJobFromOtherState(Connection conn,Key jobKey, String state, String oldState) throws SQLException {
//        PreparedStatement ps = null;
//        try {
//            // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ? AND TRIGGER_STATE = ?
////            ps = conn.prepareStatement(rtp(UPDATE_JOB_TRIGGER_STATES_FROM_OTHER_STATE));
//            ps = conn.prepareStatement(rtp(UPDATE_JOB_CFG_STATES_FROM_OTHER_STATE));
//            ps.setString(1, state);
//            ps.setString(2, jobKey.getName());
//            ps.setString(3, jobKey.getType());
////            ps.setString(3, jobKey.getGroup());
//            ps.setString(4, oldState);
//
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }
//
//    /**
//     * <p>
//     * Delete the cron trigger data for a trigger.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the number of rows deleted
//     */
//    public int deleteBlobTrigger(Connection conn,Key key) throws SQLException {
//        PreparedStatement ps = null;
//        try {
//            // DELETE FROM QRTZ_BLOB_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//            ps = conn.prepareStatement(rtp(DELETE_BLOB_TRIGGER));
//            ps.setString(1, key.getName());
////            ps.setString(2, triggerKey.getGroup());
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }
//
//    /**
//     * <p>
//     * Delete the base trigger data for a trigger.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the number of rows deleted
//     */
//    @Override
//    public int deleteTrigger(Connection conn,Key key) throws SQLException {
//        PreparedStatement ps = null;
//        deleteTriggerExtension(conn,key);
//        try {
//            // DELETE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//            ps = conn.prepareStatement(rtp(DELETE_TRIGGER));
//            ps.setString(1, key.getName());
////            ps.setString(2, triggerKey.getGroup());
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }
//
//    protected void deleteTriggerExtension(Connection conn,Key key) throws SQLException {
//        for(TriggerPersistenceDelegate tDel: triggerPersistenceDelegates) {
//            if(tDel.deleteExtendedTriggerProperties(conn,key) > 0){
//                return; // as soon as one affects a row, we're done.
//            }
//        }
////        deleteBlobTrigger(conn,key);
//    }
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
//    @Override
//    public int selectNumTriggersForJob(Connection conn,Key jobKey) throws SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//            ps = conn.prepareStatement(rtp(SELECT_NUM_TRIGGERS_FOR_JOB));
//            ps.setString(1, jobKey.getName());
////            ps.setString(2, jobKey.getGroup());
//            rs = ps.executeQuery();
//            if (rs.next()) {
//                return rs.getInt(1);
//            } else {
//                return 0;
//            }
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }
//
//    /**
//     * <p>
//     * Select the job to which the trigger is associated.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the <code>{@link org.quartz.JobDetail}</code> object
//     *         associated with the given trigger
//     * @throws SQLException
//     * @throws ClassNotFoundException
//     */
//    @Override
//    public JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper,Key triggerKey) throws ClassNotFoundException, SQLException {
//        return selectJobForTrigger(conn, loadHelper, triggerKey, true);
//    }
//
//    /**
//     * <p>
//     * Select the job to which the trigger is associated. Allow option to load actual job class or not. When case of
//     * remove, we do not need to load the class, which in many cases, it's no longer exists.
//     *
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the <code>{@link org.quartz.JobDetail}</code> object
//     *         associated with the given trigger
//     * @throws SQLException
//     * @throws ClassNotFoundException
//     */
//    @Override
//    public JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper,Key key, boolean loadJobClass) throws ClassNotFoundException, SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            // SELECT J.JOB_NAME, J.JOB_GROUP, J.IS_DURABLE, J.JOB_CLASS_NAME, J.REQUESTS_RECOVERY FROM QRTZ_TRIGGERS T, QRTZ_JOB_DETAILS J
//            // WHERE T.SCHED_NAME = 'MEE_QUARTZ' AND J.SCHED_NAME = 'MEE_QUARTZ' AND T.TRIGGER_NAME = ? AND T.TRIGGER_GROUP = ?
//            // AND T.JOB_NAME = J.JOB_NAME AND T.JOB_GROUP = J.JOB_GROUP
//            // 	1=>J.JOB_NAME,
//            //	2=>J.IS_DURABLE,
//            //	3=>J.JOB_CLASS_NAME,
//            //	4=>J.REQUESTS_RECOVERY
//            ps = conn.prepareStatement(rtp(SELECT_JOB_FOR_TRIGGER));
//            ps.setString(1, key.getName());
////            ps.setString(2, triggerKey.getGroup());
//            rs = ps.executeQuery();
//            if (rs.next()) {
//                JobDetailImpl job = new JobDetailImpl();
//                job.setKey(new Key(rs.getString(1),this.schedName));
////                job.setName(rs.getString(1));
////                job.setGroup(rs.getString(2));
////                job.setDurability(getBoolean(rs, 2));
//                if (loadJobClass){
//                    job.setJobClass(loadHelper.loadClass(rs.getString(2), Job.class));
//                }
//                job.setRequestsRecovery(getBoolean(rs, 3));
//                return job;
//            } else {
//                if (logger.isDebugEnabled()) {
//                    logger.debug("No job for trigger '" + key + "'.");
//                }
//                return null;
//            }
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }
//
//    /**
//     * <p>
//     * Select the triggers for a job
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return an array of <code>(@link org.quartz.Trigger)</code> objects
//     *         associated with a given job.
//     * @throws SQLException
//     * @throws JobPersistenceException
//     */
//    @Override
//    public List<OperableTrigger> selectTriggersForJob(Connection conn, Key jobKey) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
//        LinkedList<OperableTrigger> trigList = new LinkedList<OperableTrigger>();
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_FOR_JOB));
//            ps.setString(1, jobKey.getName());
////            ps.setString(2, jobKey.getGroup());
//            rs = ps.executeQuery();
//            while (rs.next()) {
////                OperableTrigger t = selectTrigger(conn, triggerKey(rs.getString(COL_TRIGGER_NAME), rs.getString(COL_TRIGGER_GROUP)));
//                final Key key = key(rs.getString(COL_TRIGGER_NAME),rs.getString(COL_SCHEDULER_NAME));
//                OperableTrigger t = selectTrigger(conn,key);
//                if(t != null) {
//                    trigList.add(t);
//                }
//            }
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//        return trigList;
//    }
//    @Override
//    public List<OperableTrigger> selectTriggersForCalendar(Connection conn, String calName) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
//        LinkedList<OperableTrigger> trigList = new LinkedList<OperableTrigger>();
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_FOR_CALENDAR));
//            ps.setString(1, calName);
//            rs = ps.executeQuery();
//            while (rs.next()) {
////                trigList.add(selectTrigger(conn, triggerKey(rs.getString(COL_TRIGGER_NAME), rs.getString(COL_TRIGGER_GROUP))));
//                trigList.add(selectTrigger(conn, triggerKey(rs.getString(COL_TRIGGER_NAME))));
//            }
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//
//        return trigList;
//    }
//
//    /**
//     * <p>
//     * Select a trigger.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @return the <code>{@link org.quartz.Trigger}</code> object
//     * @throws JobPersistenceException
//     */
//    @Override
//    public OperableTrigger selectTrigger(Connection conn,Key triggerKey) throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            OperableTrigger trigger = null;
//            // SELECT * FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//            ps = conn.prepareStatement(rtp(SELECT_TRIGGER));
//            ps.setString(1, triggerKey.getName());
////            ps.setString(2, triggerKey.getGroup());
//            rs = ps.executeQuery();
//            if (rs.next()) {
////                String jobName = rs.getString(COL_JOB_NAME);
//                final String triggerName = rs.getString(COL_TRIGGER_NAME);
////                String jobGroup = rs.getString(COL_JOB_GROUP);
//                String description = rs.getString(COL_DESCRIPTION);
//                long nextFireTime = rs.getLong(COL_NEXT_FIRE_TIME);
//                long prevFireTime = rs.getLong(COL_PREV_FIRE_TIME);
//                String triggerType = rs.getString(COL_TRIGGER_TYPE);
//                long startTime = rs.getLong(COL_START_TIME);
//                long endTime = rs.getLong(COL_END_TIME);
//                String calendarName = rs.getString(COL_CALENDAR_NAME);
//                int misFireInstr = rs.getInt(COL_MISFIRE_INSTRUCTION);
//                int priority = rs.getInt(COL_PRIORITY);
////                Map<?, ?> map = null;
////                if (canUseProperties()) {
////                    map = getMapFromProperties(rs);
////                } else {
////                    map = (Map<?, ?>) getObjectFromBlob(rs, COL_JOB_DATAMAP);
////                }
//                Date nft = null;
//                if (nextFireTime > 0) {
//                    nft = new Date(nextFireTime);
//                }
//                Date pft = null;
//                if (prevFireTime > 0) {
//                    pft = new Date(prevFireTime);
//                }
//                Date startTimeD = new Date(startTime);
//                Date endTimeD = null;
//                if (endTime > 0) {
//                    endTimeD = new Date(endTime);
//                }
////                if (triggerType.equals(TTYPE_BLOB)) {
////                    rs.close(); rs = null;
////                    ps.close(); ps = null;
////                    // SELECT * FROM QRTZ_BLOB_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
////                    ps = conn.prepareStatement(rtp(SELECT_BLOB_TRIGGER));
////                    ps.setString(1, triggerKey.getName());
//////                    ps.setString(2, triggerKey.getGroup());
////                    rs = ps.executeQuery();
////                    if (rs.next()) {
////                        trigger = (OperableTrigger) getObjectFromBlob(rs, COL_BLOB);
////                    }
////                }
////                else {
//                    TriggerPersistenceDelegate tDel = findTriggerPersistenceDelegate(triggerType);
//                    if(tDel == null){
//                        throw new JobPersistenceException("No TriggerPersistenceDelegate for trigger discriminator type: " + triggerType);
//                    }
//                    TriggerPropertyBundle triggerProps = null;
//                    try {
//                        triggerProps = tDel.loadExtendedTriggerProperties(conn, triggerKey);
//                    } catch (IllegalStateException isex) {
//                        if (isTriggerStillPresent(ps)) {
//                            throw isex;
//                        } else {
//                            // QTZ-386 Trigger has been deleted
//                            return null;
//                        }
//                    }
//                    TriggerBuilder<?> tb = newTrigger()
//                        .withDescription(description)
//                        .withPriority(priority)
//                        .startAt(startTimeD)
//                        .endAt(endTimeD)
//                        .withIdentity(triggerKey)
//                        .modifiedByCalendar(calendarName)
//                        .withSchedule(triggerProps.getScheduleBuilder());
////                        .forJob(jobKey(jobName, jobGroup));
//                // todo ...
////                        .forJob(Key.key(triggerName));
////                    if (null != map) {
////                        tb.usingJobData(new JobDataMap(map));
////                    }
//                    trigger = (OperableTrigger) tb.build();
//                    trigger.setMisfireInstruction(misFireInstr);
//                    trigger.setNextFireTime(nft);
//                    trigger.setPreviousFireTime(pft);
//                    setTriggerStateProperties(trigger, triggerProps);
////                }
//            }
//            return trigger;
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }
//
//    @Override
//    public OperableTrigger selectJobCfgTrigger(Connection conn,Key triggerKey) throws SQLException,JobPersistenceException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            OperableTrigger trigger = null;
////            ps = conn.prepareStatement(rtp(SELECT_TRIGGER));
//            // SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            ps = conn.prepareStatement(rtp(SELECT_JOB_CFG));
//            ps.setString(1, triggerKey.getName());
//            ps.setString(2, triggerKey.getType());
//            rs = ps.executeQuery();
//            if (rs.next()) {
////                final String triggerName = rs.getString(COL_TRIGGER_NAME);
//                String description = rs.getString(COL_DESCRIPTION);
//                long nextFireTime = rs.getLong(COL_NEXT_FIRE_TIME);
//                long prevFireTime = rs.getLong(COL_PREV_FIRE_TIME);
//                final String triggerType = rs.getString(COL_TRIGGER_TYPE);
//                long startTime = rs.getLong(COL_START_TIME);
//                long endTime = rs.getLong(COL_END_TIME);
//                String calendarName = rs.getString(COL_CALENDAR_NAME);
//                int misFireInstr = rs.getInt(COL_MISFIRE_INSTRUCTION);
//                int priority = rs.getInt(COL_PRIORITY);
////                Map<?, ?> map = null;
////                if (canUseProperties()) {
////                    map = getMapFromProperties(rs);
////                } else {
////                    map = (Map<?, ?>) getObjectFromBlob(rs, COL_JOB_DATAMAP);
////                }
////                final String jobData = rs.getString(COL_JOB_DATAMAP);
//                Date nft = null;
//                if (nextFireTime > 0) {
//                    nft = new Date(nextFireTime);
//                }
//                Date pft = null;
//                if (prevFireTime > 0) {
//                    pft = new Date(prevFireTime);
//                }
//                Date startTimeD = new Date(startTime);
//                Date endTimeD = null;
//                if (endTime > 0) {
//                    endTimeD = new Date(endTime);
//                }
//                // 获取 Trigger 类型 目前仅限: CronTriggerPersistenceDelegate or SimpleTriggerPersistenceDelegate
//                TriggerPersistenceDelegate tDel = findTriggerPersistenceDelegate(triggerType);
//                if(tDel == null){
//                    throw new JobPersistenceException("No TriggerPersistenceDelegate for trigger discriminator type: " + triggerType);
//                }
//                // 获取对应执行时间配置 如果是 CRON 则是: CRON_EXPRESSION+TIME_ZONE_ID, 如果是SIMPLE则是：REPEAT_COUNT+REPEAT_INTERVAL+TIMES_TRIGGERED
//                TriggerPropertyBundle triggerProps = null;
//                try {
//                    triggerProps = tDel.loadExtendedTriggerProperties(conn, triggerKey);
//                } catch (IllegalStateException isex) {
//                    if (isTriggerStillPresent(ps)) {
//                        throw isex;
//                    } else {
//                        // QTZ-386 Trigger has been deleted
//                        return null;
//                    }
//                }
//                TriggerBuilder<?> tb = newTrigger()
//                        .withDescription(description)
//                        .withPriority(priority)
//                        .startAt(startTimeD)
//                        .endAt(endTimeD)
//                        .withIdentity(triggerKey)
//                        .modifiedByCalendar(calendarName)
//                        .withSchedule(triggerProps.getScheduleBuilder());
////                        .forJob(jobKey(jobName, jobGroup));
//                // todo ...
////                        .forJob(Key.key(triggerName));
////                    if (null != map) {
////                        tb.usingJobData(new JobDataMap(map));
////                    }
//                trigger = (OperableTrigger) tb.build();
//                trigger.setMisfireInstruction(misFireInstr);
//                trigger.setNextFireTime(nft);
//                trigger.setPreviousFireTime(pft);
//                // todo ...
////                setTriggerStateProperties(trigger, triggerProps);
//            }
//            return trigger;
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }


    private void setTriggerStateProperties(OperableTrigger trigger, TriggerPropertyBundle props) throws JobPersistenceException {
        if(props.getStatePropertyNames() == null){
            return;
        }
        Util.setBeanProps(trigger, props.getStatePropertyNames(), props.getStatePropertyValues());
    }

//    /**
//     * <p>
//     * Select a trigger's JobDataMap.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param triggerName
//     *          the name of the trigger
//     * @return the <code>{@link org.quartz.JobDataMap}</code> of the Trigger,
//     * never null, but possibly empty.
//     */
//    @Override
//    public JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName/*, String groupName*/) throws SQLException, ClassNotFoundException, IOException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_DATA));
//            ps.setString(1, triggerName);
////            ps.setString(2, groupName);
//            rs = ps.executeQuery();
//            if (rs.next()) {
//                Map<?, ?> map = null;
//                if (canUseProperties()) {
//                    map = getMapFromProperties(rs);
//                } else {
//                    map = (Map<?, ?>) getObjectFromBlob(rs, COL_JOB_DATAMAP);
//                }
//                rs.close();
//                ps.close();
//                if (null != map) {
//                    return new JobDataMap(map);
//                }
//            }
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//        return new JobDataMap();
//    }

    // new String[]{STATE_WAITING,STATE_ACQUIRED,STATE_EXECUTING,STATE_BLOCKED,STATE_ERROR} to 'WAITING','ACQUIRED','EXECUTING','BLOCKED','ERROR'
    private String buildArrayStr(String[] arr){
        StringJoiner sj = new StringJoiner(",");
        for(String item:arr){
            sj.add("'"+item+"'");
        }
        return sj.toString();
    }


//    @Override
//    // 获取当前正在执行的已标识作业的实例数。
//    public int selectJobExecutionCount(Connection conn, JobKey jobKey) throws SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//            ps = conn.prepareStatement(rtp(SELECT_JOB_EXECUTION_COUNT));
//            ps.setString(1, jobKey.getName());
////            ps.setString(2, jobKey.getGroup());
//            rs = ps.executeQuery();
//            return (rs.next()) ? rs.getInt(1) : 0;
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }

//    @Override
//    public List<SchedulerStateRecord> selectSchedulerStateRecords(Connection conn, String theInstanceId) throws SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            List<SchedulerStateRecord> lst = new LinkedList<SchedulerStateRecord>();
//            if (theInstanceId != null) {
//                ps = conn.prepareStatement(rtp(SELECT_SCHEDULER_STATE));
//                ps.setString(1, theInstanceId);
//            } else {
//                ps = conn.prepareStatement(rtp(SELECT_SCHEDULER_STATES));
//            }
//            rs = ps.executeQuery();
//            while (rs.next()) {
//                SchedulerStateRecord rec = new SchedulerStateRecord();
//                rec.setSchedulerInstanceId(rs.getString(COL_INSTANCE_NAME));
//                rec.setCheckinTimestamp(rs.getLong(COL_LAST_CHECKIN_TIME));
//                rec.setCheckinInterval(rs.getLong(COL_CHECKIN_INTERVAL));
//                lst.add(rec);
//            }
//            return lst;
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }

    //---------------------------------------------------------------------------
    // protected methods that can be overridden by subclasses
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Replace the table prefix in a query by replacing any occurrences of
     * "{0}" with the table prefix.
     * </p>
     * 
     * @param query
     *          the unsubstitued query
     * @return the query, with proper table prefix substituted
     */
    public final String rtp(String query) {
        return Util.rtp(query, tablePrefix, getSchedulerNameLiteral());
    }
    private String schedNameLiteral = null;
    protected String getSchedulerNameLiteral() {
        if(schedNameLiteral == null){
            schedNameLiteral = "'" + schedName + "'";
        }
        return schedNameLiteral;
    }

    /**
     * <p>
     * Create a serialized <code>java.util.ByteArrayOutputStream</code>
     * version of an Object.
     * </p>
     * 
     * @param obj
     *          the object to serialize
     * @return the serialized ByteArrayOutputStream
     * @throws IOException
     *           if serialization causes an error
     */
    protected ByteArrayOutputStream serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (null != obj) {
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            out.flush();
        }
        return baos;
    }

//    /**
//     * <p>
//     * Remove the transient data from and then create a serialized <code>java.util.ByteArrayOutputStream</code>
//     * version of a <code>{@link org.quartz.JobDataMap}</code>.
//     * </p>
//     *
//     * @param data
//     *          the JobDataMap to serialize
//     * @return the serialized ByteArrayOutputStream
//     * @throws IOException
//     *           if serialization causes an error
//     */
//    protected ByteArrayOutputStream serializeJobData(JobDataMap data) throws IOException {
//        if (canUseProperties()) {
//            return serializeProperties(data);
//        }
//        try {
//            return serializeObject(data);
//        } catch (NotSerializableException e) {
//            throw new NotSerializableException(
//                "Unable to serialize JobDataMap for insertion into " +
//                "database because the value of property '" +
//                getKeyOfNonSerializableValue(data) +
//                "' is not serializable: " + e.getMessage());
//        }
//    }

    /**
     * Find the key of the first non-serializable value in the given Map.
     * 
     * @return The key of the first non-serializable value in the given Map or 
     * null if all values are serializable.
     */
    protected Object getKeyOfNonSerializableValue(Map<?, ?> data) {
        for (Iterator<?> entryIter = data.entrySet().iterator(); entryIter.hasNext();) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entryIter.next();
            ByteArrayOutputStream baos = null;
            try {
                baos = serializeObject(entry.getValue());
            } catch (IOException e) {
                return entry.getKey();
            } finally {
                if (baos != null) {
                    try { baos.close(); } catch (IOException ignore) {}
                }
            }
        }
        
        // As long as it is true that the Map was not serializable, we should
        // not hit this case.
        return null;   
    }
//
//    /**
//     * serialize the java.util.Properties
//     */
//    private ByteArrayOutputStream serializeProperties(JobDataMap data) throws IOException {
//        ByteArrayOutputStream ba = new ByteArrayOutputStream();
//        if (null != data) {
//            Properties properties = convertToProperty(data.getWrappedMap());
//            properties.store(ba, "");
//        }
//        return ba;
//    }
//
//    /**
//     * convert the JobDataMap into a list of properties
//     */
//    protected Map<?, ?> convertFromProperty(Properties properties) throws IOException {
//        return new HashMap<Object, Object>(properties);
//    }

    /**
     * convert the JobDataMap into a list of properties
     */
    protected Properties convertToProperty(Map<?, ?> data) throws IOException {
        Properties properties = new Properties();
        for (Iterator<?> entryIter = data.entrySet().iterator(); entryIter.hasNext();) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entryIter.next();
            Object key = entry.getKey();
            Object val = (entry.getValue() == null) ? "" : entry.getValue();
            if(!(key instanceof String)) {
                throw new IOException("JobDataMap keys/values must be Strings " 
                        + "when the 'useProperties' property is set. " 
                        + " offending Key: " + key);
            }
            if(!(val instanceof String)) {
                throw new IOException("JobDataMap values must be Strings " 
                        + "when the 'useProperties' property is set. " 
                        + " Key of offending value: " + key);
            }
            properties.put(key, val);
        }
        return properties;
    }

//    /**
//     * <p>
//     * This method should be overridden by any delegate subclasses that need
//     * special handling for BLOBs. The default implementation uses standard
//     * JDBC <code>java.sql.Blob</code> operations.
//     * </p>
//     *
//     * @param rs
//     *          the result set, already queued to the correct row
//     * @param colName
//     *          the column name for the BLOB
//     * @return the deserialized Object from the ResultSet BLOB
//     * @throws ClassNotFoundException
//     *           if a class found during deserialization cannot be found
//     * @throws IOException
//     *           if deserialization causes an error
//     */
//    protected Object getObjectFromBlob(ResultSet rs, String colName) throws ClassNotFoundException, IOException, SQLException {
//        Object obj = null;
//        Blob blobLocator = rs.getBlob(colName);
//        if (blobLocator != null && blobLocator.length() != 0) {
//            InputStream binaryInput = blobLocator.getBinaryStream();
//            if (null != binaryInput) {
//                if (binaryInput instanceof ByteArrayInputStream && ((ByteArrayInputStream) binaryInput).available() == 0 ) {
//                    //do nothing
//                } else {
//                    ObjectInputStream in = new ObjectInputStream(binaryInput);
//                    try {
//                        obj = in.readObject();
//                    } finally {
//                        in.close();
//                    }
//                }
//            }
//        }
//        return obj;
//    }

//    /**
//     * <p>
//     * This method should be overridden by any delegate subclasses that need
//     * special handling for BLOBs for job details. The default implementation
//     * uses standard JDBC <code>java.sql.Blob</code> operations.
//     * </p>
//     *
//     * @param rs
//     *          the result set, already queued to the correct row
//     * @param colName
//     *          the column name for the BLOB
//     * @return the deserialized Object from the ResultSet BLOB
//     * @throws ClassNotFoundException
//     *           if a class found during deserialization cannot be found
//     * @throws IOException
//     *           if deserialization causes an error
//     */
//    protected Object getJobDataFromBlob(ResultSet rs, String colName) throws ClassNotFoundException, IOException, SQLException {
//        if (canUseProperties()) {
//            Blob blobLocator = rs.getBlob(colName);
//            if (blobLocator != null) {
//                InputStream binaryInput = blobLocator.getBinaryStream();
//                return binaryInput;
//            } else {
//                return null;
//            }
//        }
//        return getObjectFromBlob(rs, colName);
//    }

//    /**
//     * @see org.quartz.impl.jdbcjobstore.DriverDelegate#selectPausedTriggerGroups(java.sql.Connection)
//     */
//    public Set<String> selectPausedTriggerGroups(Connection conn) throws SQLException {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//
//        HashSet<String> set = new HashSet<String>();
//        try {
//            ps = conn.prepareStatement(rtp(SELECT_PAUSED_TRIGGER_GROUPS));
//            rs = ps.executeQuery();
//
//            while (rs.next()) {
//                String groupName = rs.getString(COL_TRIGGER_GROUP);
//                set.add(groupName);
//            }
//            return set;
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//    }

    /**
     * Cleanup helper method that closes the given <code>ResultSet</code>
     * while ignoring any errors.
     */
    protected static void closeResultSet(ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    /**
     * Cleanup helper method that closes the given <code>Statement</code>
     * while ignoring any errors.
     */
    protected static void closeStatement(Statement statement) {
        if (null != statement) {
            try {
                statement.close();
            } catch (SQLException ignore) {
                ignore.printStackTrace();
            }
        }
    }
    
//
//    /**
//     * Sets the designated parameter to the given Java <code>boolean</code> value.
//     * This just wraps <code>{@link PreparedStatement#setBoolean(int, boolean)}</code>
//     * by default, but it can be overloaded by subclass delegates for databases that
//     * don't explicitly support the boolean type.
//     */
//    protected void setBoolean(PreparedStatement ps, int index, boolean val) throws SQLException {
//        ps.setBoolean(index, val);
//    }
//
//    /**
//     * Retrieves the value of the designated column in the current row as
//     * a <code>boolean</code>.
//     * This just wraps <code>{@link ResultSet#getBoolean(java.lang.String)}</code>
//     * by default, but it can be overloaded by subclass delegates for databases that
//     * don't explicitly support the boolean type.
//     */
//    protected boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
//        return rs.getBoolean(columnName);
//    }
    
    /**
     * Retrieves the value of the designated column index in the current row as
     * a <code>boolean</code>.
     * This just wraps <code>{@link ResultSet#getBoolean(java.lang.String)}</code>
     * by default, but it can be overloaded by subclass delegates for databases that
     * don't explicitly support the boolean type.
     */
    protected boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBoolean(columnIndex);
    }
    
    /**
     * Sets the designated parameter to the byte array of the given
     * <code>ByteArrayOutputStream</code>.  Will set parameter value to null if the 
     * <code>ByteArrayOutputStream</code> is null.
     * This just wraps <code>{@link PreparedStatement#setBytes(int, byte[])}</code>
     * by default, but it can be overloaded by subclass delegates for databases that
     * don't explicitly support storing bytes in this way.
     */
    protected void setBytes(PreparedStatement ps, int index, ByteArrayOutputStream baos) throws SQLException {
        ps.setBytes(index, (baos == null) ? new byte[0] : baos.toByteArray());
    }


    @Override
    public int insertQrtzApp(Connection conn,QrtzApp app) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        final String application = app.getApplication();
        try {
            // 查询
            ps = conn.prepareStatement(rtp("SELECT APPLICATION FROM {0}APP WHERE APPLICATION=? "));
            ps.setString(1,application);
            rs = ps.executeQuery();
            while( rs.next() ) {
                rs.close();
                // 默认有就不需要插入了
                return 1;
            }
            // 一定要关闭
            rs.close();
            ps.close();
            // 写入
            ps = conn.prepareStatement(rtp("INSERT INTO {0}APP (APPLICATION,STATE,TIME_PRE,TIME_NEXT,TIME_INTERVAL) VALUES ( ?,?,?,?,? )"));
            ps.setString(1,application);// APPLICATION
            ps.setString(2,app.getState());// STATE
            ps.setBigDecimal(3,new BigDecimal(app.getTimePre()));// TIME_PRE
            ps.setBigDecimal(4,new BigDecimal(app.getTimeNext()));// TIME_NEXT
            ps.setBigDecimal(5,new BigDecimal(app.getTimeInterval()));// TIME_INTERVAL
            int ct = ps.executeUpdate();
            conn.commit();
            return ct;
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            return 0;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public int insertQrtzNode(Connection conn,QrtzNode node) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        final String application = node.getApplication();
        try {
            // 查询
            ps = conn.prepareStatement(rtp("SELECT APPLICATION,HOST_IP  FROM {0}NODE WHERE APPLICATION=? AND HOST_IP=?"));
            ps.setString(1,application);
            ps.setString(2,node.getHostIp());
            rs = ps.executeQuery();
            while( rs.next() ) {
                rs.close();
                // 默认有就不需要插入了
                return 1;
            }
            // 一定要关闭
            rs.close();
            ps.close();
            // 写入
            ps = conn.prepareStatement(rtp("INSERT INTO {0}NODE (APPLICATION,HOST_IP,HOST_NAME,STATE,TIME_CHECK) VALUES ( ?,?,?,?,? )"));
            ps.setString(1,application);// APPLICATION
            ps.setString(2,node.getHostIp());// HOST_IP
            ps.setString(3,node.getHostName());// HOST_NAME
            ps.setString(4,node.getState());// STATE
            ps.setBigDecimal(5,new BigDecimal(node.getTimeCheck()));// TIME_CHECK
            int ct = ps.executeUpdate();
            conn.commit();
            return ct;
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            return 0;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public QrtzApp findQrtzAppByApp(Connection conn,final String application){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
//            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            // 查询
//            ps = conn.prepareStatement(rtp("SELECT * FROM {0}APP WHERE APPLICATION=? FOR UPDATE"));
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}APP WHERE APPLICATION=?"));
            ps.setString(1,application);
            rs = ps.executeQuery();
            while( rs.next() ) {
                String _application = rs.getString("APPLICATION");
                String state = rs.getString("STATE");
                Long timePre = rs.getLong("TIME_PRE");
                Long timeNext = rs.getLong("TIME_NEXT");
                Long timeInterval = rs.getLong("TIME_INTERVAL");
                rs.close();
                return new QrtzApp(_application,state,timePre,timeNext,timeInterval);
            }
            return null;
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            return null;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public void clearHistoryData(Connection conn,Long timeLimit) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // 思路:
            // 获取qrtz_app数据(by time_next>now()+timeLimit)
            // 获取qrtz_job数据(by qrtz_job:applicaiton=qrtz_app:application)
            // 删除qrtz_execute数据(by pid=qrtz_job:id)
            // 删除qrtz_job数据(by qrtz_job:applicaiton=qrtz_app:application)
            // 删除qrtz_node数据(by qrtz_job:applicaiton=qrtz_app:application)
            // 删除qrtz_app数据(by qrtz_app:applicaiton=qrtz_app:application)

            long limitTime = System.currentTimeMillis()-timeLimit;
            List<String> appList = new ArrayList<String>();
            ps = conn.prepareStatement(rtp("SELECT APPLICATION,STATE FROM {0}APP WHERE TIME_NEXT < ? "));
            ps.setBigDecimal(1,new BigDecimal(limitTime));
            rs = ps.executeQuery();
            while( rs.next() ) {
                // todo 这里需要考虑是否要做限制 ...
//                String state = rs.getString("STATE");
//                if("N".equals(state)){
//                    continue;
//                }
                String application = rs.getString("APPLICATION");
                appList.add(application);
            }
            // 一定要关闭
            rs.close();
            ps.close();
            if( appList.isEmpty() ){
                logger.info("clearData is empty! {}",limitTime);
                return;
            }

            // 获取qrtz_job数据(by qrtz_job:applicaiton=qrtz_app:application)
            List<BigDecimal> jobIdList = new ArrayList<BigDecimal>();
            for(String application:appList ){
                ps = conn.prepareStatement(rtp(" SELECT ID FROM {0}JOB WHERE APPLICATION =? "));
                ps.setString(1,application);
                rs = ps.executeQuery();
                while( rs.next() ) {
                   jobIdList.add(rs.getBigDecimal("ID"));
                }
                // 一定要关闭
                rs.close();
                ps.close();
            }

            // 删除qrtz_execute数据(by pid=qrtz_job:id)
            for( BigDecimal id:jobIdList ){
                ps = conn.prepareStatement(rtp(" DELETE FROM {0}EXECUTE WHERE PID=? "));
                ps.setBigDecimal(1,id);
                ps.executeUpdate();
                conn.commit();
                // 一定要关闭
                ps.close();
            }
            // 删除qrtz_job数据(by qrtz_job:applicaiton=qrtz_app:application)
            for( BigDecimal id:jobIdList ){
                ps = conn.prepareStatement(rtp(" DELETE FROM {0}JOB WHERE ID=? "));
                ps.setBigDecimal(1,id);
                ps.executeUpdate();
                conn.commit();
                // 一定要关闭
                ps.close();
            }
            // 删除qrtz_node数据(by qrtz_job:applicaiton=qrtz_app:application)
            for(String application:appList){
                ps = conn.prepareStatement(rtp(" DELETE FROM {0}NODE WHERE APPLICAITON=? "));
                ps.setString(1,application);
                ps.executeUpdate();
                conn.commit();
                // 一定要关闭
                ps.close();
            }
            // 删除qrtz_app数据(by qrtz_app:applicaiton=qrtz_app:application)
            for(String application:appList){
                ps = conn.prepareStatement(rtp(" DELETE FROM {0}APP WHERE APPLICAITON=? "));
                ps.setString(1,application);
                ps.executeUpdate();
                conn.commit();
                // 一定要关闭
                ps.close();
            }
            // 修正执行信息
            //针对中途停止的异常熄火的
            //1.状态
            //2.下一次执行时间


            // conn.commit();
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public int updateQrtzAppByApp(Connection conn, QrtzApp app/*,long wNow,String wState2*/){
        PreparedStatement ps = null;
        try {
//            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // 写入
//            ps = conn.prepareStatement(rtp(" UPDATE {0}APP SET STATE=?,TIME_PRE=?,TIME_NEXT=?,TIME_INTERVAL=? WHERE APPLICATION=? AND TIME_NEXT<=? AND STATE=? "));
//            ps = conn.prepareStatement(rtp(" UPDATE {0}APP SET STATE=?,TIME_PRE=?,TIME_NEXT=?,TIME_INTERVAL=? WHERE APPLICATION=? AND TIME_NEXT=? AND STATE=? "));
            ps = conn.prepareStatement(rtp(" UPDATE {0}APP SET STATE=?,TIME_PRE=?,TIME_NEXT=?,TIME_INTERVAL=? WHERE APPLICATION=? AND TIME_NEXT=? "));
            ps.setString(1,app.getState());
            ps.setBigDecimal(2,new BigDecimal(app.getTimePre()));
            ps.setBigDecimal(3,new BigDecimal(app.getTimeNext()));
            ps.setBigDecimal(4,new BigDecimal(app.getTimeInterval()));
            // WHERE
            ps.setString(5,app.getApplication());
            ps.setBigDecimal(6,new BigDecimal(app.getTimePre()));
//            ps.setString(7,wState);
            int ct = ps.executeUpdate();
//            System.out.println(ct+":"+ LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) +":"+wNow+"=>"+ps);
            conn.commit();
            return ct;
        } catch (Exception e){
            logger.error("异常:{}",app,e);
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            return 0;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }
    @Override
    public QrtzNode findQrtzNodeByAppHost(Connection conn, String app, String hostIP){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // 查询
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}NODE WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setString(1,app);
            ps.setString(2,hostIP);
            rs = ps.executeQuery();
            while( rs.next() ) {
                String _application = rs.getString("APPLICATION");
                String _hostIP = rs.getString("HOST_IP");
                String hostName = rs.getString("HOST_NAME");
                String state = rs.getString("STATE");
                Long timeCheck = rs.getLong("TIME_CHECK");
                rs.close();
                return new QrtzNode(_application,_hostIP,hostName,state,timeCheck);
            }
            return null;
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            return null;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public void updateQrtzNodeOfState(Connection conn, QrtzNode node){
        PreparedStatement ps = null;
        try {
            // 查询
            ps = conn.prepareStatement(rtp(" UPDATE {0}NODE SET STATE=?,TIME_CHECK=? WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setString(1,node.getState());
            ps.setBigDecimal(2,new BigDecimal(node.getTimeCheck()));
            ps.setString(3,node.getApplication());
            ps.setString(4,node.getHostIp());
            if( ps.executeUpdate() < 1){
                logger.error("更新失败:{}",node);
            }
            conn.commit();
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }
    @Override
    public void updateQrtzNodeOfTimeCheck(Connection conn, QrtzNode node){
        PreparedStatement ps = null;
        try {
            // 查询
            ps = conn.prepareStatement(rtp(" UPDATE {0}NODE SET TIME_CHECK=? WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setBigDecimal(1,new BigDecimal(node.getTimeCheck()));
            ps.setString(2,node.getApplication());
            ps.setString(3,node.getHostIp());
            if( ps.executeUpdate() < 1){
                logger.error("updateQrtzNodeOfTimeCheck更新失败:{}",node);
            }
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }
    @Override
    public int clearAllExecuteData(Connection conn, long timeLimit){
        PreparedStatement ps = null;
        try {
            long t = System.currentTimeMillis()-timeLimit;
            // 2. 清理 state=COMPLETE && next_fire_time >1年的清理(删除),按频度执行逻辑
            ps = conn.prepareStatement(rtp("DELETE FROM {0}EXECUTE WHERE STATE=? AND NEXT_FIRE_TIME IS NOT NULL AND NEXT_FIRE_TIME<? "));
            ps.setString(1,"COMPLETE");
            ps.setBigDecimal(2,new BigDecimal(t));
            return ps.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
            return 0;
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public List<QrtzJob> findQrtzJobByAppForRecover(Connection conn, String applicaton){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzJob> resultList = new ArrayList<QrtzJob>(8);
        try {
            //0.获取节点下异常执行项 (start_time>now and end_time>0 end_time is not null and next_fire_time<now and state!=(COMPLETE,INIT,PAUSED) )
            //0.获取节点下异常执行项 ( DELETE FROM {0}JOB WHERE START_TIME>? AND NEXT_FIRE_TIME>0 AND NEXT_FIRE_TIME<? AND STATE!=COMPLETE AND STATE!=INIT AND STATE!=PAUSED )
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}JOB WHERE APPLICATION=? AND STATE!=? AND STATE!=? AND STATE!=? "));
            ps.setString(1,applicaton);
            ps.setString(2,"COMPLETE");
            ps.setString(3,"INIT");
            ps.setString(4,"PAUSED");
            rs = ps.executeQuery();
            while( rs.next() ) {
                Long id = rs.getLong("ID");
                String _application = rs.getString("APPLICATION");
                String state = rs.getString("STATE");
//                Integer jobIdx = rs.getInt("JOB_IDX");
                String jobClass = rs.getString("JOB_CLASS");
                String jobData = rs.getString("JOB_DATA");
                String jobDescription = rs.getString("JOB_DESCRIPTION");
                Long updateTime = rs.getLong("UPDATE_TIME");
                resultList.add( new QrtzJob(id,_application,state,/*jobIdx,*/jobClass,jobData,jobDescription,updateTime) );
            }
            rs.close();
            return resultList;
        } catch (Exception e){
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            return resultList;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public List<QrtzExecute> findQrtzExecuteForRecover(Connection conn, List<QrtzJob> jobs,long now){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzExecute> resultList = new ArrayList<QrtzExecute>(8);
//        final long now = System.currentTimeMillis();
        for(QrtzJob job:jobs) {
            try {
                //0.获取节点下异常执行项 (start_time>now and end_time>0 end_time is not null and next_fire_time<now and state!=(COMPLETE,INIT,PAUSED) )
                //0.获取节点下异常执行项 ( DELETE FROM {0}JOB WHERE START_TIME>? AND NEXT_FIRE_TIME>0 AND NEXT_FIRE_TIME<? AND STATE!=COMPLETE AND STATE!=INIT AND STATE!=PAUSED )
                ps = conn.prepareStatement(rtp("SELECT * FROM {0}EXECUTE WHERE PID=? AND NEXT_FIRE_TIME<? AND STATE!=? AND STATE!=? AND STATE!=? "));
                ps.setBigDecimal(1, new BigDecimal(job.getId()));
                ps.setBigDecimal(2,new BigDecimal(now));
                ps.setString(3, "COMPLETE");
                ps.setString(4, "INIT");
                ps.setString(5, "PAUSED");
                rs = ps.executeQuery();
                while (rs.next()) {
                    Long id = rs.getLong("ID");
                    Long pid = rs.getLong("PID");
//                    Integer executeIdx = rs.getInt("EXECUTE_IDX");
                    String jobType = rs.getString("JOB_TYPE");
                    String state = rs.getString("STATE");
                    String cron = rs.getString("CRON");
                    String zoneId = rs.getString("ZONE_ID");
                    Integer repeatCount = rs.getInt("REPEAT_COUNT");
                    Integer repeatInterval = rs.getInt("REPEAT_INTERVAL");
                    Integer timeTriggered = rs.getInt("TIME_TRIGGERED");
                    Long prevFireTime = rs.getLong("PREV_FIRE_TIME");
                    Long nextFireTime = rs.getLong("NEXT_FIRE_TIME");
                    String hostIp = rs.getString("HOST_IP");
                    String hostName = rs.getString("HOST_NAME");
                    Long startTime = rs.getLong("START_TIME");
                    Long endTime = rs.getLong("END_TIME");
                    resultList.add(new QrtzExecute(id,pid,/*executeIdx,*/jobType,state,cron,zoneId,repeatCount,repeatInterval,timeTriggered,prevFireTime,nextFireTime,hostIp,hostName,startTime,endTime));
                }
                rs.close();
            } catch (Exception e) {
                e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
            } finally {
                // 由于实在同一个connection下，所以不可关闭Connection
                closeStatement(ps);
            }
        }
        return resultList;
    }

    @Override
    public int updateRecoverExecute(Connection conn, QrtzExecute execute){
        PreparedStatement ps = null;
        try {
            // 查询
            ps = conn.prepareStatement(rtp(" UPDATE {0}EXECUTE SET HOST_IP=?, HOST_NAME=?, STATE=?, NEXT_FIRE_TIME=? WHERE ID=? "));
            ps.setString(1,execute.getHostIp());
            ps.setString(2,execute.getHostName());
            ps.setString(3,execute.getState());
            ps.setBigDecimal(4,new BigDecimal(execute.getNextFireTime()));
            ps.setBigDecimal(5,new BigDecimal(execute.getId()));
            return ps.executeUpdate();
        } catch (Exception e){
            logger.error("updateRecoverExecute error:{}",execute,e);
            e.printStackTrace();
            return 0;
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public int clearAllJobData(Connection conn, long timeLimit){
        PreparedStatement ps = null;
        try {
            //2. 清理 state=COMPLETE && update_time >1年的清理(删除),按频度执行逻辑
            long t = System.currentTimeMillis()-timeLimit;
            ps = conn.prepareStatement(rtp("DELETE FROM {0}JOB WHERE STATE=? AND UPDATE_TIME<? "));
            ps.setString(1,"COMPLETE");
            ps.setBigDecimal(2,new BigDecimal(t));
            return ps.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
            return 0;
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public int updateRecoverJob(Connection conn, QrtzJob job){
        PreparedStatement ps = null;
        try {
            // 查询
            ps = conn.prepareStatement(rtp(" UPDATE {0}JOB SET STATE=?, UPDATE_TIME=? WHERE ID=?"));
            ps.setString(1,job.getState());
            ps.setBigDecimal(2,new BigDecimal(job.getUpdateTime()));
            ps.setBigDecimal(3,new BigDecimal(job.getId()));
            return ps.executeUpdate();
        } catch (Exception e){
            logger.error("updateRecoverJob error:{}",job,e);
            e.printStackTrace();
            return 0;
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

    @Override
    public List<QrtzExecute> findAllQrtzExecuteByPID(Connection conn, Long pid){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzExecute> resultList = new ArrayList<QrtzExecute>(4);
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}EXECUTE WHERE PID=? "));
            ps.setBigDecimal(1, new BigDecimal(pid));
            rs = ps.executeQuery();
            while (rs.next()) {
                Long id = rs.getLong("ID");
                Long _pid = rs.getLong("PID");
//                Integer executeIdx = rs.getInt("EXECUTE_IDX");
                String jobType = rs.getString("JOB_TYPE");
                String state = rs.getString("STATE");
                String cron = rs.getString("CRON");
                String zoneId = rs.getString("ZONE_ID");
                Integer repeatCount = rs.getInt("REPEAT_COUNT");
                Integer repeatInterval = rs.getInt("REPEAT_INTERVAL");
                Integer timeTriggered = rs.getInt("TIME_TRIGGERED");
                Long prevFireTime = rs.getLong("PREV_FIRE_TIME");
                Long nextFireTime = rs.getLong("NEXT_FIRE_TIME");
                String hostIp = rs.getString("HOST_IP");
                String hostName = rs.getString("HOST_NAME");
                Long startTime = rs.getLong("START_TIME");
                Long endTime = rs.getLong("END_TIME");
                resultList.add(new QrtzExecute(id,_pid,/*executeIdx,*/jobType,state,cron,zoneId,repeatCount,repeatInterval,timeTriggered,prevFireTime,nextFireTime,hostIp,hostName,startTime,endTime));
            }
            rs.close();
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        } finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return resultList;
    }

    @Override
    public String findNodeStateByPK(Connection conn,String application, String hostIP){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
//            ps = conn.prepareStatement(rtp("SELECT STATE FROM {0}NODE WHERE APPLICATION =? AND HOST_IP =? "));
            ps = conn.prepareStatement(rtp("SELECT A.STATE AS A_STATE,N.STATE AS N_STATE FROM QRTZ_APP A LEFT JOIN QRTZ_NODE N ON A.APPLICATION=N.APPLICATION  WHERE A.APPLICATION=? AND N.APPLICATION=? AND N.HOST_IP=? "));
            ps.setString(1, application);
            ps.setString(2, application);
            ps.setString(3, hostIP);
            rs = ps.executeQuery();
            while (rs.next()) {
                String aState = rs.getString("A_STATE");
                String nState = rs.getString("N_STATE");
                rs.close();
                if("Y".equals(aState) && "Y".equals(nState)){
                    return  "Y";
                }
                return "N";
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        } finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return null;
    }

    @Override
    public List<QrtzExecute> selectExecuteAndJobToAcquire(Connection conn, String application,long _tsw,long _tew,String state){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzExecute> resultList = new ArrayList<>(8);
        try {
            final String sql = "SELECT \n" +
//                    "J.ID AS J_ID,J.APPLICATION AS J_APPLICATION,J.STATE AS J_STATE,J.JOB_IDX AS J_JOB_IDX,J.JOB_CLASS AS J_JOB_CLASS,\n" +
                    "J.ID AS J_ID,J.APPLICATION AS J_APPLICATION,J.STATE AS J_STATE,J.JOB_CLASS AS J_JOB_CLASS,\n" +
                    "J.JOB_DATA AS J_JOB_DATA,J.JOB_DESCRIPTION AS J_JOB_DESCRIPTION,J.UPDATE_TIME AS J_UPDATE_TIME,\n" +
                    "E.*\n" +
                    "FROM {0}JOB J LEFT JOIN {0}EXECUTE E ON J.ID = E.PID  " +
                    "WHERE J.APPLICATION =? AND E.STATE = ? AND E.NEXT_FIRE_TIME>=? AND E.NEXT_FIRE_TIME<=? ";
            ps = conn.prepareStatement(rtp(sql));
            ps.setString(1,application);
            ps.setString(2, state);
            ps.setBigDecimal(3, new BigDecimal(_tsw));
            ps.setBigDecimal(4, new BigDecimal(_tew));
            rs = ps.executeQuery();
            while (rs.next()) {
                // JOB
                Long _id = rs.getLong("J_ID");
                String _application = rs.getString("J_APPLICATION");
                String _state = rs.getString("J_STATE");
//                Integer _job_idx = rs.getInt("J_JOB_IDX");
                String _job_class = rs.getString("J_JOB_CLASS");
                String _job_data = rs.getString("J_JOB_DATA");
                String _job_description = rs.getString("J_JOB_DESCRIPTION");
                Long _update_time = rs.getLong("J_UPDATE_TIME");
                // EXECTUE
                Long id = rs.getLong("ID");
                Long pid = rs.getLong("PID");
//                Integer execute_idx = rs.getInt("EXECUTE_IDX");
                String job_type = rs.getString("JOB_TYPE");
                String state_ = rs.getString("STATE");
                String cron = rs.getString("CRON");
                String zone_id = rs.getString("ZONE_ID");
                Integer repeat_count = rs.getInt("REPEAT_COUNT");
                Integer repeat_interval = rs.getInt("REPEAT_INTERVAL");
                Integer time_triggered = rs.getInt("TIME_TRIGGERED");
                Long prev_fire_time = rs.getLong("PREV_FIRE_TIME");
                Long next_fire_time = rs.getLong("NEXT_FIRE_TIME");
                String host_ip = rs.getString("HOST_IP");
                String host_name = rs.getString("HOST_NAME");
                Long start_time = rs.getLong("START_TIME");
                Long end_time = rs.getLong("END_TIME");
                QrtzJob job = new QrtzJob(_id,_application,_state,/*_job_idx,*/_job_class,_job_data,_job_description,_update_time);
                QrtzExecute execute = new QrtzExecute(id,pid,/*execute_idx,*/job_type,state_,cron,zone_id,repeat_count,repeat_interval,time_triggered,prev_fire_time,next_fire_time,host_ip,host_name,start_time,end_time);
                execute.setJob(job);
                resultList.add(execute);
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        } finally {
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return resultList;
    }


    @Override
    public int toLockAndUpdate(Connection conn, QrtzExecute newCe, String oldState, long oldPrevTime, long oldNextTime){
        PreparedStatement ps = null;
        try {
            final String sql = "UPDATE  {0}EXECUTE SET \n" +
                    "PREV_FIRE_TIME =? ,NEXT_FIRE_TIME = ?,\n" + // #1,2
                    "TIME_TRIGGERED =?,STATE =?,HOST_IP=?,HOST_NAME=?,END_TIME=? \n" + // #3,4,5,6,7
                    "WHERE ID = ? \n" + // #8
                    "AND STATE = ? \n" + // #9
                    "AND PREV_FIRE_TIME = ?\n" +// #10
                    "AND NEXT_FIRE_TIME = ?";// #11
            ps = conn.prepareStatement(rtp(sql));
            ps.setBigDecimal(1,new BigDecimal(newCe.getPrevFireTime()));
            ps.setBigDecimal(2,new BigDecimal(newCe.getNextFireTime()));
            ps.setInt(3,newCe.getTimeTriggered());
            ps.setString(4,newCe.getState());
            ps.setString(5,newCe.getHostIp());
            ps.setString(6,newCe.getHostName());
            ps.setBigDecimal(7,new BigDecimal(newCe.getEndTime()));
            // WHERE
            ps.setBigDecimal(8,new BigDecimal(newCe.getId()));
            ps.setString(9,oldState);
            ps.setBigDecimal(10,new BigDecimal(oldPrevTime));
            ps.setBigDecimal(11,new BigDecimal(oldNextTime));
            return ps.executeUpdate();
        } catch (Exception e){
            logger.error("updateRecoverJob error:{},{}",newCe,oldState);
            e.printStackTrace();
            return 0;
//            throw new SQLException("No misfired trigger count returned.");
        }finally {
//            if(null!=conn){
//                try {
//                    conn.commit();
//                } catch (SQLException e) {
//                }
//            }
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
    }

}

// EOF
