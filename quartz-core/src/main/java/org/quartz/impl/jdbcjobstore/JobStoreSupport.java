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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.quartz.Job;
import org.quartz.JobCfg;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.SystemPropGenerator;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.quartz.utils.DBConnectionManager;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Contains base functionality for JDBC-based JobStore implementations.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 */
public abstract class JobStoreSupport implements JobStore, Constants {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected static final String LOCK_TRIGGER_ACCESS = "TRIGGER_ACCESS";

    protected static final String LOCK_STATE_ACCESS = "STATE_ACCESS";

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected String dsName;

    protected String tablePrefix = DEFAULT_TABLE_PREFIX;

    protected boolean useProperties = false;

    protected String instanceId;

//    protected String instanceName;
    protected String application;

    protected String delegateClassName;

    protected String delegateInitString;
    
    protected Class<? extends DriverDelegate> delegateClass = StdJDBCDelegate.class;

//    protected HashMap<String, Calendar> calendarCache = new HashMap<String, Calendar>();

    private DriverDelegate delegate;

    private long misfireThreshold = 60000L; // one minute

    private boolean dontSetAutoCommitFalse = false;

    private boolean isClustered = false;

    private boolean useDBLocks = false;
    
    private boolean lockOnInsert = true;

    private Semaphore lockHandler = null; // set in initialize() method...

    private String selectWithLockSQL = null;
    /**
     * 群集签入间隔
     */
    private long clusterCheckinInterval = 7500L;

//    private ClusterManager clusterManagementThread = null;
    private ClusterMisfireHandler clusterMisfireHandler = null;

//    private MisfireHandler misfireHandler = null;

    private ClassLoadHelper classLoadHelper;

    private SchedulerSignaler schedSignaler;

    protected int maxToRecoverAtATime = 20;
    
    private boolean setTxIsolationLevelSequential = false;
    
    private boolean acquireTriggersWithinLock = false;
    
    private long dbRetryInterval = 15000L; // 15 secs
    
    private boolean makeThreadsDaemons = false;

    private boolean threadsInheritInitializersClassLoadContext = false;
    private ClassLoader initializersLoader = null;
    
    private boolean doubleCheckLockMisfireHandler = true;
    
//    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Logger log = LoggerFactory.getLogger(JobStoreSupport.class);

//    private ThreadExecutor threadExecutor = new DefaultThreadExecutor();
    
    private volatile boolean schedulerRunning = false;
    private volatile boolean shutdown = false;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Set the name of the <code>DataSource</code> that should be used for
     * performing database functions.
     * </p>
     */
    public void setDataSource(String dsName) {
        this.dsName = dsName;
    }

    /**
     * <p>
     * Get the name of the <code>DataSource</code> that should be used for
     * performing database functions.
     * </p>
     */
    public String getDataSource() {
        return dsName;
    }

    /**
     * <p>
     * Set the prefix that should be pre-pended to all table names.
     * </p>
     */
    public void setTablePrefix(String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        this.tablePrefix = prefix;
    }

    /**
     * <p>
     * Get the prefix that should be pre-pended to all table names.
     * </p>
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * <p>
     * Set whether String-only properties will be handled in JobDataMaps.
     * </p>
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setUseProperties(String useProp) {
        if (useProp == null) {
            useProp = "false";
        }
        this.useProperties = Boolean.valueOf(useProp);
    }

    /**
     * <p>
     * Get whether String-only properties will be handled in JobDataMaps.
     * </p>
     */
    public boolean canUseProperties() {
        return useProperties;
    }

    /**
     * <p>
     * Set the instance Id of the Scheduler (must be unique within a cluster).
     * </p>
     */
    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * <p>
     * Get the instance Id of the Scheduler (must be unique within a cluster).
     * </p>
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Set the instance name of the Scheduler (must be unique within this server instance).
     */
    @Override
    public void setApplication(String instanceName) {
        this.application = instanceName;
    }
    @Override
    public void setThreadPoolSize(final int poolSize) {
        //
    }
    
//    public void setThreadExecutor(ThreadExecutor threadExecutor) {
//        this.threadExecutor = threadExecutor;
//    }
//
//    public ThreadExecutor getThreadExecutor() {
//        return threadExecutor;
//    }
    

    /**
     * Get the instance name of the Scheduler (must be unique within this server instance).
     */
    @Override
    public String getInstanceName() {
        return this.application;
    }
//    @Override
//    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
//        return 70;
//    }

    /**
     * <p>
     * Set whether this instance is part of a cluster.
     * 见配置: org.quartz.jobStore.isClustered (StdSchedulerFactory:setBeanProps)
     * </p>
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setIsClustered(boolean isClustered) {
        this.isClustered = isClustered;
    }

    /**
     * <p>
     * Get whether this instance is part of a cluster.
     * </p>
     */
    @Override
    public boolean isClustered() {
        return isClustered;
    }

    /**
     * <p>
     * Get the frequency (in milliseconds) at which this instance "checks-in"
     * with the other instances of the cluster. -- Affects the rate of
     * detecting failed instances.
     * 获取此实例与集群的其他实例“签入”的频率（以毫秒为单位）。——影响检测失败实例的速率。
     * </p>
     */
    public long getClusterCheckinInterval() {
        return clusterCheckinInterval;
    }

    /**
     * <p>
     * Set the frequency (in milliseconds) at which this instance "checks-in"
     * with the other instances of the cluster. -- Affects the rate of
     * detecting failed instances.
     * 设置此实例与集群的其他实例“签入”的频率（以毫秒为单位）。--影响检测失败实例的速率。
     * </p>
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setClusterCheckinInterval(long l) {
        clusterCheckinInterval = l;
    }

    /**
     * <p>
     * Get the maximum number of misfired triggers that the misfire handling
     * thread will try to recover at one time (within one transaction).  The
     * default is 20.
     * </p>
     */
    public int getMaxMisfiresToHandleAtATime() {
        return maxToRecoverAtATime;
    }

    /**
     * <p>
     * Set the maximum number of misfired triggers that the misfire handling
     * thread will try to recover at one time (within one transaction).  The
     * default is 20.
     * </p>
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setMaxMisfiresToHandleAtATime(int maxToRecoverAtATime) {
        this.maxToRecoverAtATime = maxToRecoverAtATime;
    }

    /**
     * @return Returns the dbRetryInterval.
     */
    public long getDbRetryInterval() {
        return dbRetryInterval;
    }
    /**
     * @param dbRetryInterval The dbRetryInterval to set.
     */
    public void setDbRetryInterval(long dbRetryInterval) {
        this.dbRetryInterval = dbRetryInterval;
    }
    
    /**
     * <p>
     * Set whether this instance should use database-based thread
     * synchronization.
     * </p>
     */
    public void setUseDBLocks(boolean useDBLocks) {
        this.useDBLocks = useDBLocks;
    }

    /**
     * <p>
     * Get whether this instance should use database-based thread
     * synchronization.
     * </p>
     */
    public boolean getUseDBLocks() {
        return useDBLocks;
    }

    public boolean isLockOnInsert() {
        return lockOnInsert;
    }
    
    /**
     * Whether or not to obtain locks when inserting new jobs/triggers.  
     * <p>
     * Defaults to <code>true</code>, which is safest. Some databases (such as 
     * MS SQLServer) seem to require this to avoid deadlocks under high load,
     * while others seem to do fine without.  Settings this to false means
     * isolation guarantees between job scheduling and trigger acquisition are
     * entirely enforced by the database.  Depending on the database and it's
     * configuration this may cause unusual scheduling behaviors.
     * 
     * <p>Setting this property to <code>false</code> will provide a 
     * significant performance increase during the addition of new jobs 
     * and triggers.</p>
     * 
     * @param lockOnInsert whether locking should be used when inserting new jobs/triggers
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setLockOnInsert(boolean lockOnInsert) {
        this.lockOnInsert = lockOnInsert;
    }
    
    public long getMisfireThreshold() {
        return misfireThreshold;
    }

    /**
     * The the number of milliseconds by which a trigger must have missed its
     * next-fire-time, in order for it to be considered "misfired" and thus
     * have its misfire instruction applied.
     * 
     * @param misfireThreshold the misfire threshold to use, in millis
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setMisfireThreshold(long misfireThreshold) {
        if (misfireThreshold < 1) {
            throw new IllegalArgumentException("Misfirethreshold must be larger than 0");
        }
        this.misfireThreshold = misfireThreshold;
    }

    public boolean isDontSetAutoCommitFalse() {
        return dontSetAutoCommitFalse;
    }

    /**
     * Don't call set autocommit(false) on connections obtained from the
     * DataSource. This can be helpful in a few situations, such as if you
     * have a driver that complains if it is called when it is already off.
     * 
     * @param b whether or not autocommit should be set to false on db connections
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setDontSetAutoCommitFalse(boolean b) {
        dontSetAutoCommitFalse = b;
    }

    public boolean isTxIsolationLevelSerializable() {
        return setTxIsolationLevelSequential;
    }

    /**
     * Set the transaction isolation level of DB connections to sequential.
     * 
     * @param b whether isolation level should be set to sequential.
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setTxIsolationLevelSerializable(boolean b) {
        setTxIsolationLevelSequential = b;
    }

    /**
     * Whether or not the query and update to acquire a Trigger for firing
     * should be performed after obtaining an explicit DB lock (to avoid 
     * possible race conditions on the trigger's db row).  This is the
     * behavior prior to Quartz 1.6.3, but is considered unnecessary for most
     * databases (due to the nature of the SQL update that is performed), 
     * and therefore a superfluous performance hit.     
     */
    public boolean isAcquireTriggersWithinLock() {
        return acquireTriggersWithinLock;
    }

    /**
     * Whether or not the query and update to acquire a Trigger for firing
     * should be performed after obtaining an explicit DB lock.  This is the
     * behavior prior to Quartz 1.6.3, but is considered unnecessary for most
     * databases, and therefore a superfluous performance hit.     
     * 
     * However, if batch acquisition is used, it is important for this behavior
     * to be used for all dbs.
     *
     * 是否应在获得显式DB锁后执行查询和更新以获取触发触发器。这是Quartz 1.6.3之前的行为，但对于大多数数据库来说是不必要的，因此对性能的影响是多余的。但是，如果使用批量采集，则将此行为用于所有数据库非常重要。
     *
     *  配置见 StdSchedulerFactory:setBeanProps
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setAcquireTriggersWithinLock(boolean acquireTriggersWithinLock) {
        this.acquireTriggersWithinLock = acquireTriggersWithinLock;
    }

    
    /**
     * <p>
     * Set the JDBC driver delegate class.
     * </p>
     * 
     * @param delegateClassName
     *          the delegate class name
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setDriverDelegateClass(String delegateClassName) {
        synchronized(this) {
            this.delegateClassName = delegateClassName;
        }
    }

    /**
     * <p>
     * Get the JDBC driver delegate class name.
     * </p>
     * 
     * @return the delegate class name
     */
    public String getDriverDelegateClass() {
        return delegateClassName;
    }

    /**
     * <p>
     * Set the JDBC driver delegate's initialization string.
     * </p>
     * 
     * @param delegateInitString
     *          the delegate init string
     */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setDriverDelegateInitString(String delegateInitString) {
        this.delegateInitString = delegateInitString;
    }

    /**
     * <p>
     * Get the JDBC driver delegate's initialization string.
     * </p>
     * 
     * @return the delegate init string
     */
    public String getDriverDelegateInitString() {
        return delegateInitString;
    }

    public String getSelectWithLockSQL() {
        return selectWithLockSQL;
    }

    /**
     * <p>
     * set the SQL statement to use to select and lock a row in the "locks"
     * table.
     * </p>
     * 
     * @see StdRowLockSemaphore
     */
    public void setSelectWithLockSQL(String string) {
        selectWithLockSQL = string;
    }

    private ClassLoadHelper getClassLoadHelper() {
        return classLoadHelper;
    }

    /**
     * Get whether the threads spawned by this JobStore should be
     * marked as daemon.  Possible threads include the <code>MisfireHandler</code> 
     * and the <code>ClusterManager</code>.
     * 
     * @see Thread#setDaemon(boolean)
     */
    public boolean getMakeThreadsDaemons() {
        return makeThreadsDaemons;
    }

    /**
     * Set whether the threads spawned by this JobStore should be
     * marked as daemon.  Possible threads include the <code>MisfireHandler</code> 
     * and the <code>ClusterManager</code>.
     *
     * @see Thread#setDaemon(boolean)
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setMakeThreadsDaemons(boolean makeThreadsDaemons) {
        this.makeThreadsDaemons = makeThreadsDaemons;
    }
    
    /**
     * Get whether to set the class load context of spawned threads to that
     * of the initializing thread.
     */
    public boolean isThreadsInheritInitializersClassLoadContext() {
        return threadsInheritInitializersClassLoadContext;
    }

    /**
     * Set whether to set the class load context of spawned threads to that
     * of the initializing thread.
     */
    public void setThreadsInheritInitializersClassLoadContext(boolean threadsInheritInitializersClassLoadContext) {
        this.threadsInheritInitializersClassLoadContext = threadsInheritInitializersClassLoadContext;
    }

    /**
     * Get whether to check to see if there are Triggers that have misfired
     * before actually acquiring the lock to recover them.  This should be 
     * set to false if the majority of the time, there are are misfired
     * Triggers.
     */
    public boolean getDoubleCheckLockMisfireHandler() {
        return doubleCheckLockMisfireHandler;
    }

    /**
     * Set whether to check to see if there are Triggers that have misfired
     * before actually acquiring the lock to recover them.  This should be 
     * set to false if the majority of the time, there are are misfired
     * Triggers.
     */
    @SuppressWarnings("UnusedDeclaration") /* called reflectively */
    public void setDoubleCheckLockMisfireHandler(boolean doubleCheckLockMisfireHandler) {
        this.doubleCheckLockMisfireHandler = doubleCheckLockMisfireHandler;
    }

//    /**
//     * 默认15s, 具体可参见配置: org.quartz.scheduler.dbFailureRetryInterval
//     * @param failureCount the number of successive failures seen so far
//     * @return
//     */
//    @Override
//    public long getAcquireRetryDelay(int failureCount) {
//        return dbRetryInterval;
//    }
    @Override
    public String findNodeStateByPK(String application, String hostIP) {
        Connection conn = null;
        try {
            conn = getConnection();
            return getDelegate().findNodeStateByPK(conn,application,hostIP);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            cleanupConnection(conn);
        }
    }
    @Override
    public int toLockAndUpdate(QrtzExecute newCe, String oldState, long oldPrevTime, long oldNextTime) {
        Connection conn = null;
        try {
            conn = getConnection();
            int ct = getDelegate().toLockAndUpdate(conn, newCe, oldState, oldPrevTime, oldNextTime);
//            log.info("写入:{},{}",ct,newCe);
            return ct;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }finally {
            try {
                if (null != conn && !conn.getAutoCommit()) {
                    conn.commit();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            cleanupConnection(conn);
        }
    }

    //---------------------------------------------------------------------------
    // interface methods
    //---------------------------------------------------------------------------

    protected Logger getLog() {
        return log;
    }

    /**
     * <p>
     * Called by the QuartzScheduler before the <code>JobStore</code> is
     * used, in order to give it a chance to initialize.
     * 在使用JobStore之前由QuartzScheduler调用，以便给它一个初始化的机会。
     * </p>
     */
    @Override
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
        if (dsName == null) { 
            throw new SchedulerConfigException("DataSource name not set."); 
        }
        classLoadHelper = loadHelper;
        if(isThreadsInheritInitializersClassLoadContext()) {
            log.info("JDBCJobStore threads will inherit ContextClassLoader of thread: " + Thread.currentThread().getName());
            initializersLoader = Thread.currentThread().getContextClassLoader();
        }
        this.schedSignaler = signaler;
        // If the user hasn't specified an explicit lock handler, then 
        // choose one based on CMT/Clustered/UseDBLocks.
        if (getLockHandler() == null) {
            // If the user hasn't specified an explicit lock handler, 
            // then we *must* use DB locks with clustering
            if (isClustered()) {
                setUseDBLocks(true);
            }
            if (getUseDBLocks()) {
                if(getDriverDelegateClass() != null && getDriverDelegateClass().equals(MSSQLDelegate.class.getName())) {
                    if(getSelectWithLockSQL() == null) {
                        String msSqlDflt = "SELECT * FROM {0}LOCKS WITH (UPDLOCK,ROWLOCK) WHERE " + COL_SCHEDULER_NAME + " = {1} AND LOCK_NAME = ?";
                        getLog().info("Detected usage of MSSQLDelegate class - defaulting 'selectWithLockSQL' to '" + msSqlDflt + "'.");
                        setSelectWithLockSQL(msSqlDflt);
                    }
                }
                getLog().info("Using db table-based data access locking (synchronization).");
                setLockHandler(new StdRowLockSemaphore(getTablePrefix(), getInstanceName(), getSelectWithLockSQL()));
//                setLockHandler(new UpdateLockRowSemaphore(getTablePrefix(), getInstanceName(), getSelectWithLockSQL()));
            } else {
                getLog().info("Using thread monitor-based data access locking (synchronization).");
                setLockHandler(new SimpleSemaphore());
            }
        }

    }
   
    /**
     * @see org.quartz.spi.JobStore#schedulerStarted()
     */
    @Override
    public void schedulerStarted() throws SchedulerException {
//        if (isClustered()) {
//            clusterManagementThread = new ClusterManager();
//            if(initializersLoader != null){
//                clusterManagementThread.setContextClassLoader(initializersLoader);
//            }
//            clusterManagementThread.initialize();
//        } else {
//            try {
//                recoverJobs();
//            } catch (SchedulerException se) {
//                throw new SchedulerConfigException("Failure occured during job recovery.", se);
//            }
//        }
//
//        misfireHandler = new MisfireHandler();
//        if(initializersLoader != null){
//            misfireHandler.setContextClassLoader(initializersLoader);
//        }
//        misfireHandler.initialize();

        clusterMisfireHandler = new ClusterMisfireHandler();
        if(initializersLoader != null){
            clusterMisfireHandler.setContextClassLoader(initializersLoader);
        }
        // 前置处理(仅启动时一次)
        clusterMisfireHandler.preProcess(); // 清理
        clusterMisfireHandler.recoverJob(); // 恢复job
        clusterMisfireHandler.recoverExecute(System.currentTimeMillis()/1000*1000); // 恢复execute
        FIRST_CHECK=false;
        clusterMisfireHandler.start();
        schedulerRunning = true;
        getLog().debug("JobStore background threads started (as scheduler was started).");
    }
    @Override
    public void schedulerPaused() {
        schedulerRunning = false;
    }
    @Override
    public void schedulerResumed() {
        schedulerRunning = true;
    }
    
    /**
     * <p>
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * it should free up all of it's resources because the scheduler is
     * shutting down.
     * </p>
     */
    @Override
    public void shutdown() {
        shutdown = true;
//        if (misfireHandler != null) {
//            misfireHandler.shutdown();
//            try {
//                misfireHandler.join();
//            } catch (InterruptedException ignore) {
//            }
//        }
//        if (clusterManagementThread != null) {
//            clusterManagementThread.shutdown();
//            try {
//                clusterManagementThread.join();
//            } catch (InterruptedException ignore) {
//            }
//        }
        if (clusterMisfireHandler != null) {
            clusterMisfireHandler.shutdown();
            try {
                clusterMisfireHandler.join();
            } catch (InterruptedException ignore) {
            }
        }
        try {
            DBConnectionManager.getInstance().shutdown(getDataSource());
        } catch (SQLException sqle) {
            getLog().warn("Database connection shutdown unsuccessful.", sqle);
        }
        getLog().debug("JobStore background threads shutdown.");
    }
    @Override
    public boolean supportsPersistence() {
        return true;
    }

    //---------------------------------------------------------------------------
    // helper methods for subclasses
    //---------------------------------------------------------------------------

    protected abstract Connection getNonManagedTXConnection() throws JobPersistenceException;

    /**
     * Wrap the given <code>Connection</code> in a Proxy such that attributes 
     * that might be set will be restored before the connection is closed 
     * (and potentially restored to a pool).
     */
    protected Connection getAttributeRestoringConnection(Connection conn) {
        return (Connection)Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] { Connection.class },
                new AttributeRestoringConnectionInvocationHandler(conn));
    }
    
    protected Connection getConnection() throws JobPersistenceException {
        Connection conn;
        try {
            conn = DBConnectionManager.getInstance().getConnection(getDataSource());
        } catch (SQLException sqle) {
            throw new JobPersistenceException("Failed to obtain DB connection from data source '" + getDataSource() + "': " + sqle.toString(), sqle);
        } catch (Throwable e) {
            throw new JobPersistenceException("Failed to obtain DB connection from data source '" + getDataSource() + "': " + e.toString(), e);
        }
        if (conn == null) { 
            throw new JobPersistenceException("Could not get connection from DataSource '" + getDataSource() + "'");
        }
        // Protect connection attributes we might change.
        conn = getAttributeRestoringConnection(conn);
        // Set any connection connection attributes we are to override.
        try {
            if (!isDontSetAutoCommitFalse()) {
                conn.setAutoCommit(false);
            }
            if(isTxIsolationLevelSerializable()) {
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            }
        } catch (SQLException sqle) {
            getLog().warn("Failed to override connection auto commit/transaction isolation.", sqle);
        } catch (Throwable e) {
            try { conn.close(); } catch(Throwable ignored) {}
            throw new JobPersistenceException("Failure setting up connection.", e);
        }
        return conn;
    }

    protected void releaseLock(String lockName, boolean doIt) {
        if (doIt) {
            try {
                getLockHandler().releaseLock(lockName);
            } catch (LockException le) {
                getLog().error("Error returning lock: " + le.getMessage(), le);
            }
        }
    }

//    /**
//     * Recover any failed or misfired jobs and clean up the data store as
//     * appropriate.
//     *
//     * @throws JobPersistenceException if jobs could not be recovered
//     */
//    protected void recoverJobs() throws JobPersistenceException {
//        executeInNonManagedTXLock(
//            LOCK_TRIGGER_ACCESS,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    recoverJobs(conn);
//                }
//            }, null);
//    }
//
//    /**
//     * <p>
//     * Will recover any failed or misfired jobs and clean up the data store as
//     * appropriate.
//     * </p>
//     *
//     * @throws JobPersistenceException
//     *           if jobs could not be recovered
//     */
//    protected void recoverJobs(Connection conn) throws JobPersistenceException {
//        try {
//            // update inconsistent job states
//            int rows = getDelegate().updateTriggerStatesFromOtherStates(conn,STATE_WAITING, STATE_ACQUIRED, STATE_BLOCKED);
//            rows += getDelegate().updateTriggerStatesFromOtherStates(conn,
//                        STATE_PAUSED, STATE_PAUSED_BLOCKED, STATE_PAUSED_BLOCKED);
//            getLog().info("Freed " + rows + " triggers from 'acquired' / 'blocked' state.");
//            // clean up misfired jobs
//            recoverMisfiredJobs(conn, true);
//            // recover jobs marked for recovery that were not fully executed
//            List<OperableTrigger> recoveringJobTriggers = getDelegate().selectTriggersForRecoveringJobs(conn);
//            getLog().info("Recovering " + recoveringJobTriggers.size() + " jobs that were in-progress at the time of the last shut-down.");
//
//            for (OperableTrigger recoveringJobTrigger: recoveringJobTriggers) {
//                // todo ...
//                if (jobDetailExists(conn, recoveringJobTrigger.getKey())) {
//                    recoveringJobTrigger.computeFirstFireTime(null);
//                    storeTrigger(conn, recoveringJobTrigger, null, false, STATE_WAITING, false, true);
//                }
//            }
//            getLog().info("Recovery complete.");
//
//            // remove lingering 'complete' triggers...
//            List<Key> cts = getDelegate().selectTriggersInState(conn, STATE_COMPLETE);
//            for(Key ct: cts) {
//                removeTrigger(conn, ct);
//            }
//            getLog().info("Removed " + cts.size() + " 'complete' triggers.");
//            // clean up any fired trigger entries
//            int n = getDelegate().deleteFiredTriggers(conn);
//            getLog().info("Removed " + n + " stale fired job entries.");
//        } catch (JobPersistenceException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new JobPersistenceException("Couldn't recover jobs: " + e.getMessage(), e);
//        }
//    }

    // return : now - 60s
    protected long getMisfireTime() {
        long misfireTime = System.currentTimeMillis();
        // misfireThreshold: 60000L
        if (getMisfireThreshold() > 0) {
            misfireTime -= getMisfireThreshold();
        }
        return (misfireTime > 0) ? misfireTime : 0;
    }

    /**
     * Helper class for returning the composite result of trying to recover misfired jobs.
     * 用于返回尝试恢复不匹配的作业的合成结果的帮助程序类。
     */
    protected static class RecoverMisfiredJobsResult {
        public static final RecoverMisfiredJobsResult NO_OP =new RecoverMisfiredJobsResult(false, 0, Long.MAX_VALUE);
        private final boolean _hasMoreMisfiredTriggers; // _有更多误触发
        private final int _processedMisfiredTriggerCount; //_已处理的误触发计数
        private final long _earliestNewTime; // _最早的新时间
        
        public RecoverMisfiredJobsResult(boolean hasMoreMisfiredTriggers, int processedMisfiredTriggerCount, long earliestNewTime) {
            _hasMoreMisfiredTriggers = hasMoreMisfiredTriggers;
            _processedMisfiredTriggerCount = processedMisfiredTriggerCount;
            _earliestNewTime = earliestNewTime;
        }
        
        public boolean hasMoreMisfiredTriggers() {
            return _hasMoreMisfiredTriggers;
        }
        public int getProcessedMisfiredTriggerCount() {
            return _processedMisfiredTriggerCount;
        } 
        public long getEarliestNewTime() {
            return _earliestNewTime;
        } 
    }
    // 恢复已经熄火的job
//    protected RecoverMisfiredJobsResult recoverMisfiredJobs(Connection conn, boolean recovering) throws JobPersistenceException, SQLException {
//        // If recovering, we want to handle all of the misfired triggers right away.
//        // 如果恢复，我们希望立即处理所有不匹配的触发器。
//        // recovering=false => getMaxMisfiresToHandleAtATime() = 20
//        // recovering=true =>  getMaxMisfiresToHandleAtATime() = -1
//        int maxMisfiresToHandleAtATime = (recovering) ? -1 : getMaxMisfiresToHandleAtATime();
//
//        List<Key> misfiredTriggers = new LinkedList<Key>();
//        long earliestNewTime = Long.MAX_VALUE;
//        // We must still look for the MISFIRED state in case triggers were left
//        // in this state when upgrading to this version that does not support it.
//        // 我们仍然必须寻找MISFIRED状态，以防在升级到不支持它的版本时触发器处于这种状态。
//        // SELECT TRIGGER_NAME,SCHED_NAME,TRIGGER_TYPE FROM QRTZ_JOB_CFG
//        // WHERE SCHED_NAME = 'QUARTZ-SPRINGBOOT' AND NOT (MISFIRE_INSTR = -1)
//        // AND NEXT_FIRE_TIME < (20 || -1) AND TRIGGER_STATE = 'WAITING'
//        // ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
//        boolean hasMoreMisfiredTriggers =
//            getDelegate().hasMisfiredTriggersInState(conn, STATE_WAITING, getMisfireTime(), maxMisfiresToHandleAtATime, misfiredTriggers);
//
//        if (hasMoreMisfiredTriggers) {
//            getLog().info(
//                "Handling the first " + misfiredTriggers.size() +
//                " triggers that missed their scheduled fire-time.  " +
//                "More misfired triggers remain to be processed.");
//        } else if (misfiredTriggers.size() > 0) {
//            getLog().info("Handling " + misfiredTriggers.size() + " trigger(s) that missed their scheduled fire-time.");
//        } else {
//            // 没有直接结束当前逻辑
//            getLog().debug("Found 0 triggers that missed their scheduled fire-time.");
//            return RecoverMisfiredJobsResult.NO_OP;
//        }
//        for (Key triggerKey: misfiredTriggers) {
//            // SELECT * FROM QRTZ_JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            OperableTrigger trig = retrieveTrigger(conn, triggerKey);
//            if (trig == null) {
//                continue;
//            }
//            // UPDATE QRTZ_JOB_CFG SET DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?,
//            // TRIGGER_STATE =('WAITING' || 'COMPLETE'), TRIGGER_TYPE = ?,
//            // START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ?
//            // WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            doUpdateOfMisfiredTrigger(conn, trig, false, STATE_WAITING, recovering);
//            if(trig.getNextFireTime() != null && trig.getNextFireTime().getTime() < earliestNewTime){
//                earliestNewTime = trig.getNextFireTime().getTime();
//            }
//        }
//        return new RecoverMisfiredJobsResult(hasMoreMisfiredTriggers, misfiredTriggers.size(), earliestNewTime);
//    }
//    // 更新熄火的trigger
//    protected boolean updateMisfiredTrigger(Connection conn,Key triggerKey, String newStateIfNotComplete, boolean forceState) throws JobPersistenceException {
//        try {
//            OperableTrigger trig = retrieveTrigger(conn, triggerKey);
//            long misfireTime = System.currentTimeMillis();
//            if (getMisfireThreshold() > 0) {
//                misfireTime -= getMisfireThreshold();
//            }
//            if (trig.getNextFireTime().getTime() > misfireTime) {
//                return false;
//            }
//            doUpdateOfMisfiredTrigger(conn, trig, forceState, newStateIfNotComplete, false);
//            return true;
//        } catch (Exception e) {
//            throw new JobPersistenceException("Couldn't update misfired trigger '" + triggerKey + "': " + e.getMessage(), e);
//        }
//    }
//    // 更新熄火的trigger
//    private void doUpdateOfMisfiredTrigger(Connection conn, OperableTrigger trig, boolean forceState, String newStateIfNotComplete, boolean recovering) throws JobPersistenceException {
//        Calendar cal = null;
////        if (trig.getCalendarName() != null) {
////            cal = retrieveCalendar(conn, trig.getCalendarName());
////        }
////        schedSignaler.notifyTriggerListenersMisfired(trig);
//        // 主要做：setNextFireTime
//        trig.updateAfterMisfire(cal);
//        if (trig.getNextFireTime() == null) {
//            // 设置为完成
//            storeTrigger(conn, trig,null, true, STATE_COMPLETE, forceState, recovering);
////            schedSignaler.notifySchedulerListenersFinalized(trig);
//        } else {
//            storeTrigger(conn, trig, null, true, newStateIfNotComplete,forceState, recovering);
//        }
//    }
//
//    /**
//     * <p>
//     * Store the given <code>{@link org.quartz.JobDetail}</code> and <code>{@link org.quartz.Trigger}</code>.
//     * 存储给定的JobDetail和Trigger。
//     * </p>
//     *
//     * @param newJob
//     *          The <code>JobDetail</code> to be stored.
//     * @param newTrigger
//     *          The <code>Trigger</code> to be stored.
//     * newJob–要存储的JobDetail。newTrigger–要存储的触发器。
//     *
//     * @throws ObjectAlreadyExistsException
//     *           if a <code>Job</code> with the same name/group already
//     *           exists.
//     */
//    @Override
//    public void storeJobAndTrigger(final JobDetail newJob,final OperableTrigger newTrigger) throws JobPersistenceException {
//        executeInLock(
//            (isLockOnInsert()) ? LOCK_TRIGGER_ACCESS : null,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    // 写job_detail
//                    storeJob(conn, newJob, false);
//                    // 获取
//                    storeTrigger(conn, newTrigger, newJob, false,Constants.STATE_WAITING, false, false);
//                }
//            });
//    }
//
//    @Override
//    public void storeJobAndExecute(JobCfg jobCfg, ExecuteCfg executeCfg) throws JobPersistenceException {
//        executeInLock(
//                (isLockOnInsert()) ? LOCK_TRIGGER_ACCESS : null,
//                new VoidTransactionCallback() {
//                    @Override
//                    public void executeVoid(Connection conn) throws JobPersistenceException {
//                        storeJobAndExecute(conn,jobCfg,executeCfg);
//                    }
//                });
//    }
//
//    private void storeJobAndExecute(Connection conn,JobCfg jobCfg, ExecuteCfg executeCfg) throws JobPersistenceException{
////        // 写job_detail
////        storeJob(conn, newJob, false);
////        // 获取
////        storeTrigger(conn, newTrigger, newJob, false,Constants.STATE_WAITING, false, false);
//        try{
//            ((JobCfgImpl)jobCfg).setTriggerState(Constants.STATE_WAITING);
//            this.getDelegate().insertJobCfg(conn,jobCfg);
//            this.getDelegate().insertExecuteCfg(conn,executeCfg);
//        }catch (Exception e){
//            e.printStackTrace();
//            throw new JobPersistenceException("store JobCfg or ExecuteCfg error: "+ e.getMessage(), e);
//        }
//    }
//
//    /**
//     * <p>
//     * Store the given <code>{@link org.quartz.JobDetail}</code>.
//     * </p>
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
//    @Override
//    public void storeJob(final JobDetail newJob,final boolean replaceExisting) throws JobPersistenceException {
//        executeInLock(
//            (isLockOnInsert() || replaceExisting) ? LOCK_TRIGGER_ACCESS : null,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    storeJob(conn, newJob, replaceExisting);
//                }
//            });
//    }
//
//    /**
//     * <p>
//     * Insert or update a job.
//     * </p>
//     */
//    protected void storeJob(Connection conn, JobDetail newJob, boolean replaceExisting) throws JobPersistenceException {
//        boolean existingJob = jobDetailExists(conn, newJob.getKey());
//        try {
//            if (existingJob) {
//                // QRTZ_JOB_DETAILS存在 且 QRTZ_TRIGGERS 不存在时抛出
//                if (!replaceExisting) {
//                    throw new ObjectAlreadyExistsException(newJob);
//                }
//                getDelegate().updateJobDetail(conn, newJob);
//            } else {
//                getDelegate().insertJobDetail(conn, newJob);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new JobPersistenceException("Couldn't store job: "+ e.getMessage(), e);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            throw new JobPersistenceException("Couldn't store job: " + e.getMessage(), e);
//        }
//    }

    /**
     * <p>
     * Check existence of a given job.
     * </p>
     */
    protected boolean jobDetailExists(Connection conn, Key jobKey) throws JobPersistenceException {
        try {
            return getDelegate().jobDetailExists(conn, jobKey);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't determine job existence (" + jobKey + "): " + e.getMessage(), e);
        }
    }
//
//    /**
//     * <p>
//     * Store the given <code>{@link org.quartz.Trigger}</code>.
//     * </p>
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
//     */
//    @Override
//    public void storeTrigger(final OperableTrigger newTrigger,final boolean replaceExisting) throws JobPersistenceException {
//        executeInLock(
//            (isLockOnInsert() || replaceExisting) ? LOCK_TRIGGER_ACCESS : null,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    storeTrigger(conn, newTrigger, null, replaceExisting,STATE_WAITING, false, false);
//                }
//            });
//    }
    
//    /**
//     * <p>
//     * Insert or update a trigger.
//     * </p>
//     */
//    @SuppressWarnings("ConstantConditions")
//    protected void storeTrigger(Connection conn,OperableTrigger newTrigger, JobDetail job, boolean replaceExisting, String state,boolean forceState, boolean recovering) throws JobPersistenceException {
////        boolean existingTrigger = triggerExists(conn, newTrigger.getKey());
//        boolean existingTrigger = jobCfgExists(conn, newTrigger.getKey());
//        if ((existingTrigger) && (!replaceExisting)) {
//            throw new ObjectAlreadyExistsException(newTrigger);
//        }
//        try {
//            // 检索作业/获取 JOB_DETAILS
//            // SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            if ( (job = retrieveJob(conn, newTrigger.getKey())) == null) {
//                throw new JobPersistenceException("The job ("+ newTrigger.getKey()+ ") referenced by the trigger does not exist.");
//            }
//            // UPDATE {0}JOB_CFG SET ... WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            // UPDATE QRTZ_JOB_CFG SET DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE ='WAITING', TRIGGER_TYPE = ?,
//            // START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ?
//            // WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            getDelegate().updateJobCfg(conn, newTrigger, state, job);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new JobPersistenceException("Couldn't store trigger '" + newTrigger.getKey() + "' for '" + newTrigger.getKey() + "' job:" + e.getMessage(), e);
//        }
//    }

    /**
     * <p>
     * Check existence of a given trigger.
     * 检查给定的trigger是否存在
     * </p>
     */
    protected boolean jobCfgExists(Connection conn,Key key) throws JobPersistenceException {
        try {
//            return getDelegate().triggerExists(conn,key);
            return getDelegate().jobCfgExists(conn,key);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't determine trigger existence (" + key + "): " + e.getMessage(), e);
        }
    }

//    /**
//     * <p>
//     * Remove (delete) the <code>{@link org.quartz.Job}</code> with the given
//     * name, and any <code>{@link org.quartz.Trigger}</code> s that reference
//     * it.
//     * 移除（删除）具有给定名称的作业以及引用该作业的任何触发器。
//     * </p>
//     *
//     * <p>
//     * If removal of the <code>Job</code> results in an empty group, the
//     * group should be removed from the <code>JobStore</code>'s list of
//     * known group names.
//     * 如果删除作业导致一个空组，则应将该组从JobStore的已知组名列表中删除。
//     * </p>
//     *
//     * @return <code>true</code> if a <code>Job</code> with the given name &
//     *         group was found and removed from the store.
//     *         如果找到具有给定名称和组的作业并将其从存储中删除，则为true。
//     */
//    @Override
//    public boolean removeJob(final Key jobKey) throws JobPersistenceException {
//        return (Boolean) executeInLock(
//                LOCK_TRIGGER_ACCESS,
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return removeJob(conn, jobKey) ?
//                                Boolean.TRUE : Boolean.FALSE;
//                    }
//                });
//    }
//
//    protected boolean removeJob(Connection conn, final Key jobKey)throws JobPersistenceException {
//        try {
//            List<Key> jobTriggers = getDelegate().selectTriggerKeysForJob(conn, jobKey);
//            for (Key jobTrigger: jobTriggers) {
//                deleteTriggerAndChildren(conn, jobTrigger);
//            }
//            return deleteJobAndChildren(conn, jobKey);
//        } catch (SQLException e) {
//            throw new JobPersistenceException("Couldn't remove job: "+ e.getMessage(), e);
//        }
//    }
//    @Override
//    public boolean removeJobs(final List<Key> keys) throws JobPersistenceException {
//        return (Boolean) executeInLock(
//                LOCK_TRIGGER_ACCESS,
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        boolean allFound = true;
//                        // FUTURE_TODO: make this more efficient with a true bulk operation...
//                        for (Key k : keys){
//                            allFound = removeJob(conn,k) && allFound;
//                        }
//                        return allFound ? Boolean.TRUE : Boolean.FALSE;
//                    }
//                });
//    }
//    @Override
//    public boolean removeTriggers(final List<Key> keys) throws JobPersistenceException {
//        return (Boolean) executeInLock(
//                LOCK_TRIGGER_ACCESS,
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        boolean allFound = true;
//                        // FUTURE_TODO: make this more efficient with a true bulk operation...
//                        for (Key k : keys){
//                            allFound = removeTrigger(conn,k) && allFound;
//                        }
//                        return allFound ? Boolean.TRUE : Boolean.FALSE;
//                    }
//                });
//    }
//    @Override
//    public void storeJobsAndTriggers(final Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, final boolean replace) throws JobPersistenceException {
//        executeInLock(
//                (isLockOnInsert() || replace) ? LOCK_TRIGGER_ACCESS : null,
//                new VoidTransactionCallback() {
//                    @Override
//                    public void executeVoid(Connection conn) throws JobPersistenceException {
//                        // FUTURE_TODO: make this more efficient with a true bulk operation...
//                        for(JobDetail job: triggersAndJobs.keySet()) {
//                            storeJob(conn, job, replace);
//                            for(Trigger trigger: triggersAndJobs.get(job)) {
//                                storeTrigger(conn, (OperableTrigger) trigger, job, replace,Constants.STATE_WAITING, false, false);
//                            }
//                        }
//                    }
//                });
//    }
//
//    /**
//     * Delete a job and its listeners.
//     *
//     * @see #removeJob(java.sql.Connection, JobKey)
//     * @see #removeTrigger(Connection, TriggerKey)
//     */
//    private boolean deleteJobAndChildren(Connection conn,Key key) throws NoSuchDelegateException, SQLException {
//        return (getDelegate().deleteJobDetail(conn, key) > 0);
//    }
//
//    /**
//     * Delete a trigger, its listeners, and its Simple/Cron/BLOB sub-table entry.
//     *
//     * @see #removeJob(java.sql.Connection, .JobKey)
//     * @see #removeTrigger(Connection, TriggerKey)
//     * @see #replaceTrigger(Connection, TriggerKey, OperableTrigger)
//     */
//    private boolean deleteTriggerAndChildren(Connection conn,Key key) throws SQLException, NoSuchDelegateException {
//        return (getDelegate().deleteTrigger(conn, key) > 0);
//    }
//
    /**
     * <p>
     * Retrieve the <code>{@link org.quartz.JobDetail}</code> for the given
     * <code>{@link org.quartz.Job}</code>.
     * 检索给定作业的JobDetail。
     * </p>
     * 
     * @return The desired <code>Job</code>, or null if there is no match.
     */
    @Override
    public JobDetail retrieveJob(final Key jobKey) throws JobPersistenceException {
        return (JobDetail)executeWithoutLock( // no locks necessary for read...
            new TransactionCallback() {
                @Override
                public Object execute(Connection conn) throws JobPersistenceException {
                    return retrieveJob(conn, jobKey);
                }
            });
    }
    
    protected JobDetail retrieveJob(Connection conn, Key key) throws JobPersistenceException {
        try {
//            return getDelegate().selectJobDetail(conn, key,getClassLoadHelper());
            // SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
            return getDelegate().selectJobCfg(conn, key,getClassLoadHelper());
        } catch (ClassNotFoundException e) {
            throw new JobPersistenceException("Couldn't retrieve job because a required class was not found: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new JobPersistenceException("Couldn't retrieve job because the BLOB couldn't be deserialized: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't retrieve job: " + e.getMessage(), e);
        }
    }
//
//    /**
//     * <p>
//     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
//     * given name.
//     *  删除（删除）具有给定名称的触发器。
//     * </p>
//     *
//     * <p>
//     * If removal of the <code>Trigger</code> results in an empty group, the
//     * group should be removed from the <code>JobStore</code>'s list of
//     * known group names.
//     *  如果删除触发器导致组为空，则应从作业存储的已知组名称列表中删除该组。
//     * </p>
//     *
//     * <p>
//     * If removal of the <code>Trigger</code> results in an 'orphaned' <code>Job</code>
//     * that is not 'durable', then the <code>Job</code> should be deleted
//     * also.
//     * 如果删除触发器会导致“孤立”作业，而该作业不是“持久的”，则也应删除该作业。
//     * </p>
//     *
//     * @return <code>true</code> if a <code>Trigger</code> with the given
//     *         name & group was found and removed from the store.
//     *  如果找到了具有给定名称和组的触发器并从存储中删除，则为 true。
//     */
//    @Override
//    public boolean removeTrigger(final Key k) throws JobPersistenceException {
//        return (Boolean) executeInLock(
//                LOCK_TRIGGER_ACCESS,
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return removeTrigger(conn,k) ? Boolean.TRUE : Boolean.FALSE;
//                    }
//                });
//    }
//
//    public boolean removeTrigger(Connection conn, Key key) throws JobPersistenceException {
//        boolean removedTrigger;
//        try {
//            // this must be called before we delete the trigger, obviously 显然，在我们删除触发器之前，必须调用它
//            // SELECT J.JOB_NAME, J.JOB_GROUP, J.IS_DURABLE, J.JOB_CLASS_NAME, J.REQUESTS_RECOVERY FROM QRTZ_TRIGGERS T, QRTZ_JOB_DETAILS J
//            // WHERE T.SCHED_NAME = 'MEE_QUARTZ' AND J.SCHED_NAME = 'MEE_QUARTZ' AND T.TRIGGER_NAME = ? AND T.TRIGGER_GROUP = ?
//            // AND T.JOB_NAME = J.JOB_NAME AND T.JOB_GROUP = J.JOB_GROUP
//            JobDetail job = getDelegate().selectJobForTrigger(conn,getClassLoadHelper(), key, false);
//            // DELETE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//            removedTrigger = deleteTriggerAndChildren(conn, key);
//            if (null != job /*&& !job.isDurable()*/ ) {
//                // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//                int numTriggers = getDelegate().selectNumTriggersForJob(conn,job.getKey());
//                if (numTriggers == 0) {
//                    // Don't call removeJob() because we don't want to check for
//                    // triggers again.
//                    // DELETE FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//                    deleteJobAndChildren(conn, job.getKey());
//                }
//            }
//        } catch (ClassNotFoundException e) {
//            throw new JobPersistenceException("Couldn't remove trigger: "+ e.getMessage(), e);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            throw new JobPersistenceException("Couldn't remove trigger: " + e.getMessage(), e);
//        }
//        return removedTrigger;
//    }

//    /**
//     * @see org.quartz.spi.JobStore#replaceTrigger(TriggerKey, OperableTrigger)
//     */
//    @Override
//    public boolean replaceTrigger(final Key triggerKey,final OperableTrigger newTrigger) throws JobPersistenceException {
//        return (Boolean) executeInLock(
//                LOCK_TRIGGER_ACCESS,
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return replaceTrigger(conn, triggerKey, newTrigger) ?
//                                Boolean.TRUE : Boolean.FALSE;
//                    }
//                });
//    }
//
//    protected boolean replaceTrigger(Connection conn,Key key, OperableTrigger newTrigger) throws JobPersistenceException {
//        try {
//            // this must be called before we delete the trigger, obviously
//            JobDetail job = getDelegate().selectJobForTrigger(conn,getClassLoadHelper(), key);
//            if (job == null) {
//                return false;
//            }
////            if (!newTrigger.getJobKey().equals(job.getKey())) {
//            if (!newTrigger.getKey().equals(job.getKey())) {
//                throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
//            }
//            boolean removedTrigger = deleteTriggerAndChildren(conn, key);
//            storeTrigger(conn, newTrigger, job, false, STATE_WAITING, false, false);
//            return removedTrigger;
//        } catch (ClassNotFoundException e) {
//            throw new JobPersistenceException("Couldn't remove trigger: " + e.getMessage(), e);
//        } catch (SQLException e) {
//            throw new JobPersistenceException("Couldn't remove trigger: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * <p>
//     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
//     * 检索给定的触发器。
//     * </p>
//     *
//     * @return The desired <code>Trigger</code>, or null if there is no
//     *         match.
//     */
//    @Override
//    public OperableTrigger retrieveTrigger(final Key triggerKey) throws JobPersistenceException {
//        return (OperableTrigger)executeWithoutLock( // no locks necessary for read...
//            new TransactionCallback() {
//                @Override
//                public Object execute(Connection conn) throws JobPersistenceException {
//                    return retrieveTrigger(conn, triggerKey);
//                }
//            });
//    }
//
//    protected OperableTrigger retrieveTrigger(Connection conn,Key key) throws JobPersistenceException {
//        try {
//            //  SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
////            return getDelegate().selectTrigger(conn, key);
//            // SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            return getDelegate().selectJobCfgTrigger(conn, key);
//        } catch (Exception e) {
//            log.error("出现异常了:{}",key,e);
//            throw new JobPersistenceException("Couldn't retrieve trigger: " + e.getMessage(), e);
//        }
//    }
//
    /**
     * <p>
     * Get the current state of the identified <code>{@link Trigger}</code>.
     * 获取已标识触发器的当前状态。
     * </p>
     * 
     * @see TriggerState#NORMAL
     * @see TriggerState#PAUSED
     * @see TriggerState#COMPLETE
     * @see TriggerState#ERROR
     * @see TriggerState#NONE
     */
    @Override
    public TriggerState getTriggerState(final Key triggerKey) throws JobPersistenceException {
        return (TriggerState)executeWithoutLock( // no locks necessary for read...
                new TransactionCallback() {
                    @Override
                    public Object execute(Connection conn) throws JobPersistenceException {
                        return getTriggerState(conn, triggerKey);
                    }
                });
    }
    
    public TriggerState getTriggerState(Connection conn,Key key) throws JobPersistenceException {
        try {
            // SELECT TRIGGER_STATE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ?
            String ts = getDelegate().selectTriggerState(conn, key);
            if (ts == null) {
                return TriggerState.NONE;
            }
            if (ts.equals(STATE_DELETED)) {
                return TriggerState.NONE;
            }
            if (ts.equals(STATE_COMPLETE)) {
                return TriggerState.COMPLETE;
            }
            if (ts.equals(STATE_PAUSED)) {
                return TriggerState.PAUSED;
            }
            if (ts.equals(STATE_PAUSED_BLOCKED)) {
                return TriggerState.PAUSED;
            }
            if (ts.equals(STATE_ERROR)) {
                return TriggerState.ERROR;
            }
            if (ts.equals(STATE_BLOCKED)) {
                return TriggerState.BLOCKED;
            }
            return TriggerState.NORMAL;
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't determine state of trigger (" + key + "): " + e.getMessage(), e);
        }
    }

//    /**
//     * <p>
//     * Get the number of <code>{@link org.quartz.Job}</code> s that are
//     * stored in the <code>JobStore</code>.
//     *  获取存储在JobStore中的作业数。
//     * </p>
//     */
//    @Override
//    public int getNumberOfJobs() throws JobPersistenceException {
//        return (Integer) executeWithoutLock( // no locks necessary for read...
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return getNumberOfJobs(conn);
//                    }
//                });
//    }
    
    protected int getNumberOfJobs(Connection conn) throws JobPersistenceException {
        try {
            // SELECT COUNT(JOB_NAME)  FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ'
            return getDelegate().selectNumJobs(conn);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't obtain number of jobs: " + e.getMessage(), e);
        }
    }
//
//    /**
//     * <p>
//     * Get the number of <code>{@link org.quartz.Trigger}</code> s that are
//     * stored in the <code>JobsStore</code>.
//     *  获取存储在JobsStore中的触发器数。
//     * </p>
//     */
//    @Override
//    public int getNumberOfTriggers() throws JobPersistenceException {
//        return (Integer) executeWithoutLock( // no locks necessary for read...
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return getNumberOfTriggers(conn);
//                    }
//                });
//    }
    
    protected int getNumberOfTriggers(Connection conn) throws JobPersistenceException {
        try {
            // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
            return getDelegate().selectNumTriggers(conn);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't obtain number of triggers: " + e.getMessage(), e);
        }
    }

//    /**
//     * <p>
//     * Get the number of <code>{@link org.quartz.Calendar}</code> s that are
//     * stored in the <code>JobsStore</code>.
//     *  获取存储在JobsStore中的日历数。
//     * </p>
//     */
//    @Override
//    public int getNumberOfCalendars()
//        throws JobPersistenceException {
//        return (Integer) executeWithoutLock( // no locks necessary for read...
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return getNumberOfCalendars(conn);
//                    }
//                });
//    }
//
//    protected int getNumberOfCalendars(Connection conn) throws JobPersistenceException {
//        try {
//            return getDelegate().selectNumCalendars(conn);
//        } catch (SQLException e) {
//            throw new JobPersistenceException("Couldn't obtain number of calendars: " + e.getMessage(), e);
//        }
//    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Job}</code> s that
     * matcher the given groupMatcher.
     *  获取与给定groupMatcher匹配的所有作业的名称。
     * </p>
     * 
     * <p>
     * If there are no jobs in the given group name, the result should be an empty Set
     *   如果给定组名称中没有作业，则结果应为空Set
     * </p>
     *  getJobKeys -> getAllJobKeysInSched
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Key> getAllJobKeysInSched(final String triggerName) throws JobPersistenceException {
        return (Set<Key>)executeWithoutLock( // no locks necessary for read...
            new TransactionCallback() {
                @Override
                public Object execute(Connection conn) throws JobPersistenceException {
                   return getTriggerNames(conn,triggerName);
                }
            });
    }
    // getJobNames -> getTriggerNames
    protected Set<Key> getTriggerNames(Connection conn,final String triggerName) throws JobPersistenceException {
        try {
            return getDelegate().selectAllJobsInSched(conn,triggerName);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't obtain job names: " + e.getMessage(), e);
        }
    }
    
    
//    /**
//     * Determine whether a {@link Job} with the given identifier already
//     * exists within the scheduler.
//     *  确定调度程序中是否已存在具有给定标识符的作业。
//     *
//     * @param jobKey the identifier to check for
//     * @return true if a Job exists with the given identifier
//     * @throws JobPersistenceException
//     */
//    @Override
//    public boolean checkDetailExists(final Key k) throws JobPersistenceException {
//        return (Boolean)executeWithoutLock( // no locks necessary for read...
//                new TransactionCallback() {
//                    @Override
//                    public Object execute(Connection conn) throws JobPersistenceException {
//                        return checkDetailExists(conn,k);
//                    }
//                });
//    }
   
    protected boolean checkDetailExists(Connection conn,Key k) throws JobPersistenceException {
        try {
            return getDelegate().jobDetailExists(conn,k);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't check for existence of job: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determine whether a {@link Trigger} with the given identifier already 
     * exists within the scheduler.
     *  确定调度程序中是否已经存在具有给定标识符的触发器。
     *
     * @param triggerKey the identifier to check for
     * @return true if a Trigger exists with the given identifier
     * @throws JobPersistenceException
     */
    @Override
    public boolean checkTriggerExists(final Key triggerKey) throws JobPersistenceException {
        return (Boolean)executeWithoutLock( // no locks necessary for read...
                new TransactionCallback() {
                    @Override
                    public Object execute(Connection conn) throws JobPersistenceException {
                        return checkTriggerExists(conn, triggerKey);
                    }
                });
    }
    
    protected boolean checkTriggerExists(Connection conn,Key triggerKey) throws JobPersistenceException {
        try {
            return getDelegate().triggerExists(conn, triggerKey);
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't check for existence of job: " + e.getMessage(), e);
        }
    }

//    /**
//     * Clear (delete!) all scheduling data - all {@link Job}s, {@link Trigger}s
//     * {@link Calendar}s.
//     * 清除（删除！）所有计划数据-所有作业、触发器日历。
//     *
//     * @throws JobPersistenceException
//     */
//    @Override
//    public void clearAllSchedulingData() throws JobPersistenceException {
//        executeInLock(
//                LOCK_TRIGGER_ACCESS,
//                new VoidTransactionCallback() {
//                    @Override
//                    public void executeVoid(Connection conn) throws JobPersistenceException {
////                        clearAllSchedulingData(conn);
//                        try {
//                            getDelegate().clearData(conn);
//                        } catch (SQLException e) {
//                            throw new JobPersistenceException("Error clearing scheduling data: " + e.getMessage(), e);
//                        }
//                    }
//                });
//    }
//
//    /**
//     * <p>
//     * Get all of the Triggers that are associated to the given Job.
//     * 获取与给定作业关联的所有触发器。
//     * </p>
//     *
//     * <p>
//     * If there are no matches, a zero-length array should be returned.
//     * 如果没有匹配项，则应返回一个长度为零的数组。
//     * </p>
//     */
//    @Override
//    @SuppressWarnings("unchecked")
//    public List<OperableTrigger> getTriggersForJob(final Key jobKey) throws JobPersistenceException {
//        return (List<OperableTrigger>)executeWithoutLock( // no locks necessary for read...
//            new TransactionCallback() {
//                @Override
//                public Object execute(Connection conn) throws JobPersistenceException {
//                    return getTriggersForJob(conn, jobKey);
//                }
//            });
//    }
//
//    protected List<OperableTrigger> getTriggersForJob(Connection conn,Key key) throws JobPersistenceException {
//        List<OperableTrigger> list;
//        try {
//            list = getDelegate().selectTriggersForJob(conn, key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new JobPersistenceException("Couldn't obtain triggers for job: " + e.getMessage(), e);
//        }
//        return list;
//    }

    /**
     * <p>
     * Pause the <code>{@link org.quartz.Trigger}</code> with the given name.
     *  暂停具有给定名称的触发器。
     * </p>
     * 
     * @see #resumeTrigger(TriggerKey)
     */
    @Override
    public void pauseTrigger(final Key triggerKey) throws JobPersistenceException {
        executeInLock(
            LOCK_TRIGGER_ACCESS,
            new VoidTransactionCallback() {
                @Override
                public void executeVoid(Connection conn) throws JobPersistenceException {
                    pauseTrigger(conn, triggerKey);
                }
            });
    }
    
    /**
     * <p>
     * Pause the <code>{@link org.quartz.Trigger}</code> with the given name.
     *  暂停具有给定名称的触发器。
     * </p>
     * 
     * @see #resumeTrigger(Connection, TriggerKey)
     */
    public void pauseTrigger(Connection conn,Key triggerKey) throws JobPersistenceException {
        try {
            String oldState = getDelegate().selectTriggerState(conn,triggerKey);
            if (oldState.equals(STATE_WAITING) || oldState.equals(STATE_ACQUIRED)) {
                getDelegate().updateTriggerState(conn, triggerKey, STATE_PAUSED);
            } else if (oldState.equals(STATE_BLOCKED)) {
                getDelegate().updateTriggerState(conn, triggerKey, STATE_PAUSED_BLOCKED);
            }
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't pause trigger '" + triggerKey + "': " + e.getMessage(), e);
        }
    }

//    /**
//     * <p>
//     * Pause the <code>{@link org.quartz.Job}</code> with the given name - by
//     * pausing all of its current <code>Trigger</code>s.
//     *  暂停具有给定名称的作业-通过暂停其所有当前触发器。
//     * </p>
//     *
//     * @see #resumeJob (JobKey)
//     */
//    @Override
//    public void pauseJob(final Key key) throws JobPersistenceException {
//        executeInLock(
//            LOCK_TRIGGER_ACCESS,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    List<OperableTrigger> triggers = getTriggersForJob(conn,key);
//                    for (OperableTrigger trigger: triggers) {
//                        pauseTrigger(conn, trigger.getKey());
//                    }
//                }
//            });
//    }
    
//    /**
//     * <p>
//     * Pause all of the <code>{@link org.quartz.Job}s</code> matching the given
//     * groupMatcher - by pausing all of their <code>Trigger</code>s.
//     * 暂停与给定groupMatcher匹配的所有作业-通过暂停其所有触发器。
//     * </p>
//     *
//     * @see #resumeJobs(org.quartz.impl.matchers.GroupMatcher)
//     */
//    @Override
//    @SuppressWarnings("unchecked")
//    public Set<String> pauseJobs(final String triggerName) throws JobPersistenceException {
//        return (Set<String>) executeInLock(
//            LOCK_TRIGGER_ACCESS,
//            new TransactionCallback() {
//                @Override
//                public Set<String> execute(final Connection conn) throws JobPersistenceException {
//                    Set<String> triggerNames = new HashSet<String>();
////                    Set<JobKey> jobNames = getJobNames(conn, matcher);
//                    Set<Key> jobNames = getTriggerNames(conn,triggerName);
//                    for (Key key : jobNames) {
//                        List<OperableTrigger> triggers = getTriggersForJob(conn, key);
//                        for (OperableTrigger trigger : triggers) {
//                            pauseTrigger(conn, trigger.getKey());
//                        }
//                        triggerNames.add(key.getName());
//                    }
//                    return triggerNames;
//                }
//            });
//    }
//
    /**
     * Determines if a Trigger for the given job should be blocked.
     * 确定是否应阻止给定作业的触发器
     * State can only transition to STATE_PAUSED_BLOCKED/BLOCKED from 
     * PAUSED/STATE_WAITING respectively.
     * 
     * @return STATE_PAUSED_BLOCKED, BLOCKED, or the currentState. 
     */
    protected String checkBlockedState(Connection conn,Key jobKey, String currentState) throws JobPersistenceException {
        // State can only transition to BLOCKED from PAUSED or WAITING.
        // 状态只能从PAUSED或WAITING转换到BLOCKED。
        if ((!currentState.equals(STATE_WAITING)) && (!currentState.equals(STATE_PAUSED))) {
            return currentState;
        }
        // todo ... 考虑是否需要移除！
        try {
            // SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ?
            List<FiredTriggerRecord> lst = getDelegate().selectFiredTriggerRecordsByJob(conn,jobKey/*,jobKey.getGroup()*/);
            if (lst.size() > 0) {
                FiredTriggerRecord rec = lst.get(0);
                if (rec.isJobDisallowsConcurrentExecution()) { // OLD_TODO: worry about failed/recovering/volatile job  states?
                    return (STATE_PAUSED.equals(currentState)) ? STATE_PAUSED_BLOCKED : STATE_BLOCKED;
                }
            }
            return currentState;
        } catch (SQLException e) {
            throw new JobPersistenceException("Couldn't determine if trigger should be in a blocked state '" + jobKey + "': " + e.getMessage(), e);
        }
    }
//
//    /**
//     * <p>
//     * Resume (un-pause) the <code>{@link org.quartz.Trigger}</code> with the
//     * given name.
//     * 恢复（取消暂停）具有给定名称的触发器。
//     * </p>
//     *
//     * <p>
//     * If the <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     *  如果触发器错过了一次或多次点火，则将应用触发器的失火指令
//     * </p>
//     *
//     * @see #pauseTrigger(TriggerKey)
//     */
//    @Override
//    public void resumeTrigger(final Key triggerKey) throws JobPersistenceException {
//        executeInLock(
//            LOCK_TRIGGER_ACCESS,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    resumeTrigger(conn, triggerKey);
//                }
//            });
//    }
//
//    /**
//     * <p>
//     * Resume (un-pause) the <code>{@link org.quartz.Trigger}</code> with the
//     * given name.
//     * 恢复（取消暂停）具有给定名称的触发器。
//     * </p>
//     *
//     * <p>
//     * If the <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     *  如果触发器错过了一次或多次点火，则将应用触发器的失火指令。
//     * </p>
//     *
//     * @see #pauseTrigger(Connection, TriggerKey)
//     */
//    public void resumeTrigger(Connection conn,Key key) throws JobPersistenceException {
//        try {
//            TriggerStatus status = getDelegate().selectTriggerStatus(conn,key);
//            if (status == null || status.getNextFireTime() == null) {
//                return;
//            }
//            boolean blocked = false;
//            if(STATE_PAUSED_BLOCKED.equals(status.getStatus())) {
//                blocked = true;
//            }
////            String newState = checkBlockedState(conn, status.getJobKey(), STATE_WAITING);
//            String newState = checkBlockedState(conn, status.getKey(), STATE_WAITING);
//            boolean misfired = false;
//            if (schedulerRunning && status.getNextFireTime().before(new Date())) {
//                misfired = updateMisfiredTrigger(conn, key, newState, true);
//            }
//            if(!misfired) {
//                if(blocked) {
//                    getDelegate().updateTriggerStateFromOtherState(conn,key, newState, STATE_PAUSED_BLOCKED);
//                } else {
//                    getDelegate().updateTriggerStateFromOtherState(conn,key, newState, STATE_PAUSED);
//                }
//            }
//
//        } catch (SQLException e) {
//            throw new JobPersistenceException("Couldn't resume trigger '" + key + "': " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * <p>
//     * Resume (un-pause) the <code>{@link org.quartz.Job}</code> with the
//     * given name.
//     *  继续（取消暂停）具有给定名称的作业。
//     * </p>
//     *
//     * <p>
//     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
//     * or more fire-times, then the <code>Trigger</code>'s misfire
//     * instruction will be applied.
//     *  如果任何作业的触发器错过了一次或多次点火，则将应用触发器的失火指令。
//     * </p>
//     *
//     * @see #pauseJob(JobKey)
//     */
//    @Override
//    public void resumeJob(final Key key) throws JobPersistenceException {
//        executeInLock(
//            LOCK_TRIGGER_ACCESS,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    List<OperableTrigger> triggers = getTriggersForJob(conn, key);
//                    for (OperableTrigger trigger: triggers) {
//                        resumeTrigger(conn, trigger.getKey());
//                    }
//                }
//            });
//    }
    
//    /**
//     * <p>
//     * Resume (un-pause) all of the <code>{@link org.quartz.Job}s</code> in
//     * the given group.
//     *  恢复（取消暂停）给定组中的所有作业。
//     * </p>
//     *
//     * <p>
//     * If any of the <code>Job</code> s had <code>Trigger</code> s that
//     * missed one or more fire-times, then the <code>Trigger</code>'s
//     * misfire instruction will be applied.
//     *  如果任何作业的触发器错过了一次或多次点火，则将应用触发器的失火指令。
//     * </p>
//     *
//     * @see #pauseJobs(org.quartz.impl.matchers.GroupMatcher)
//     */
//    @Deprecated
//    @Override
//    @SuppressWarnings("unchecked")
//    public Set<String> resumeJobs(final String triggerName) throws JobPersistenceException {
//        return (Set<String>) executeInLock(
//            LOCK_TRIGGER_ACCESS,
//            new TransactionCallback() {
//                @Override
//                public Set<String> execute(Connection conn) throws JobPersistenceException {
////                    Set<JobKey> jobKeys = getJobNames(conn, matcher);
//                    Set<Key> jobKeys = getTriggerNames(conn,triggerName);
//                    Set<String> triggerNames = new HashSet<String>();
//                    for (Key key: jobKeys) {
//                        List<OperableTrigger> triggers = getTriggersForJob(conn,key);
//                        for (OperableTrigger trigger: triggers) {
//                            resumeTrigger(conn, trigger.getKey());
//                        }
//                        triggerNames.add(key.getName());
//                    }
//                    return triggerNames;
//                }
//            });
//    }

//    private static long ftrCtr = System.currentTimeMillis();
//
//    protected synchronized String getFiredTriggerRecordId() {
//        return getInstanceId() + ftrCtr++;
//    }
//
//    /**
//     * <p>
//     * Get a handle to the next N triggers to be fired, and mark them as 'reserved'
//     * by the calling scheduler.
//     *  获取下一个要触发的N个触发器的句柄，并由调用调度程序将它们标记为“保留”。
//     * </p>
//     *
//     * @see #releaseAcquiredTrigger(OperableTrigger)
//     */
//    @Override
//    public List<OperableTrigger> acquireNextTriggers(final long noLaterThan, final int maxCount, final long timeWindow) throws JobPersistenceException {
//        // noLaterThan: now + 30S
//        // maxCount: Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()) maxBatchSize:默认是1且可配置 org.quartz.scheduler.batchTriggerAcquisitionMaxCount
//        // timeWindow: qsRsrcs.getBatchTimeWindow()
//        String lockName;
//        if(isAcquireTriggersWithinLock() || maxCount > 1) {
//            lockName = LOCK_TRIGGER_ACCESS;
//        } else {
//            lockName = null;
//        }
//        return executeInNonManagedTXLock(lockName,
//                new TransactionCallback<List<OperableTrigger>>() {
//                    @Override
//                    public List<OperableTrigger> execute(Connection conn) throws JobPersistenceException {
//                        return acquireNextTrigger(conn, noLaterThan, maxCount, timeWindow);
//                    }
//                },
//                new TransactionValidator<List<OperableTrigger>>() {
//                    @Override
//                    public Boolean validate(Connection conn, List<OperableTrigger> result) throws JobPersistenceException {
//                        try {
//                            List<FiredTriggerRecord> acquired = getDelegate().selectInstancesFiredTriggerRecords(conn, getInstanceId());
//                            Set<String> fireInstanceIds = new HashSet<String>();
//                            for (FiredTriggerRecord ft : acquired) {
//                                fireInstanceIds.add(ft.getFireInstanceId());
//                            }
//                            for (OperableTrigger tr : result) {
//                                if (fireInstanceIds.contains(tr.getFireInstanceId())) {
//                                    return true;
//                                }
//                            }
//                            return false;
//                        } catch (SQLException e) {
//                            throw new JobPersistenceException("error validating trigger acquisition", e);
//                        }
//                    }
//                });
//    }
//
//    // FUTURE_TODO: this really ought to return something like a FiredTriggerBundle,
//    // so that the fireInstanceId doesn't have to be on the trigger...
//    // FUTURE_TODO：这真的应该返回类似FiredTriggerBundle的东西，
//    // 这样fireInstanceId就不必在触发器上。。。
//    protected List<OperableTrigger> acquireNextTrigger(Connection conn, long noLaterThan, int maxCount, long timeWindow) throws JobPersistenceException {
//        // noLaterThan: now + 30S
//        // maxCount: Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()) , 这个在获取记录前会做判断，如果是<=0,则只可获取到一条记录
//        // timeWindow: qsRsrcs.getBatchTimeWindow() 默认是0且可配置:org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow
//        if (timeWindow < 0) {
//          throw new IllegalArgumentException();
//        }
//        List<OperableTrigger> acquiredTriggers = new ArrayList<OperableTrigger>();
////        Set<Key> acquiredJobKeysForNoConcurrentExec = new HashSet<Key>();
//        final int MAX_DO_LOOP_RETRY = 3;
//        int currentLoopCount = 0;
//        do {
//            currentLoopCount ++;
//            try {
//                // SELECT TRIGGER_NAME, NEXT_FIRE_TIME, PRIORITY, SCHED_NAME,TRIGGER_TYPE FROM QRTZ_JOB_CFG
//                // WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_STATE = 'WAITING' AND NEXT_FIRE_TIME <= :noLaterThan
//                // AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= :noEarlierThan ))
//                // ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
//                // noLaterThan=now+30S+0
//                // noEarlierThan=now-60000L(60秒前)
//                // NEXT_FIRE_TIME: 下一次触发时间（毫秒），默认为-1，意味不会自动触发 ; 以上取值范围大致为 now-60S<=NEXT_FIRE_TIME<=now+30S
//                // 注意：这个查询只取 TRIGGER_STATE = 'WAITING' 的记录，其他条件看以上SQL
//                List<Key> keys = getDelegate().selectTriggerToAcquire(conn, noLaterThan + timeWindow, getMisfireTime(), maxCount);
//                // No trigger is ready to fire yet. 触发器还没有准备好点火
//                if (keys == null || keys.size() == 0){
//                    return acquiredTriggers;
//                }
//                long batchEnd = noLaterThan; // now + 30S
//                for(Key triggerKey: keys) {
//                    // If our trigger is no longer available, try a new one. 如果我们的触发器不再可用，请尝试新的触发器。
//                    // SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//                    OperableTrigger nextTrigger = retrieveTrigger(conn, triggerKey);
//                    if(nextTrigger == null) {
//                        continue; // next trigger
//                    }
//                    Date nextFireTime = nextTrigger.getNextFireTime();
//                    if (nextFireTime == null) {
//                        log.warn("Trigger {} returned null on nextFireTime and yet still exists in DB!",nextTrigger.getKey());
//                        continue;
//                    }
//                    // batchEnd=noLaterThan=now+30S+0 ,也就是只取未来30秒以内的
//                    if (nextFireTime.getTime() > batchEnd) {
//                      break;
//                    }
//                    // We now have a acquired trigger, let's add to return list.
//                    // If our trigger was no longer in the expected state, try a new one.
//                    //  我们现在有一个已获取的触发器，让我们将其添加到返回列表中。
//                    //  如果我们的触发器不再处于预期状态，请尝试新的触发器。
//                    //  内部执行的是:　UPDATE {0}JOB_CFG SET TRIGGER_STATE = 'ACQUIRED' WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_STATE = ? and TRIGGER_TYPE = 'WAITING'
////                    int rowsUpdated = getDelegate().updateTriggerStateFromOtherState(conn, triggerKey, STATE_ACQUIRED, STATE_WAITING);
//                    int rowsUpdated = getDelegate().updateJobCfgStateToAcquired(conn, triggerKey, STATE_ACQUIRED,new String[]{STATE_WAITING,STATE_ACQUIRED,STATE_EXECUTING,STATE_BLOCKED,STATE_ERROR});
//                    if (rowsUpdated <= 0) {
//                        // 小于0意味着状态已经发生变化需要从新循环从数据库捞数据
//                        continue; // next trigger
//                    }
//                    // todo ... 这里需要重新规划
////                    // instanceId + ftrCtr++;
////                    nextTrigger.setFireInstanceId(getFiredTriggerRecordId());
////                    // 执行 INSERT INTO {0}FIRED_TRIGGERS ...(sate=ACQUIRED)
////                    getDelegate().insertFiredTrigger(conn, nextTrigger, STATE_ACQUIRED, null);
//                    if(acquiredTriggers.isEmpty()) {
//                        // MAX(下一次点火时间,时间)+0 其中 timeWindow默认为0，具体配置见: org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow
//                        batchEnd = Math.max(nextFireTime.getTime(), System.currentTimeMillis()) + timeWindow;
//                    }
//                    // 已经写入的记录同时保存至List
//                    acquiredTriggers.add(nextTrigger);
//                }
//                // if we didn't end up with any trigger to fire from that first
//                // batch, try again for another batch. We allow with a max retry count.
//                //  如果我们最终没有从第一批中获得任何触发器，请重试另一批。我们允许最大重试次数。
//                //  触发器未能获取到时当前请求循环次数最大只能为2
//                if(acquiredTriggers.size() == 0 && currentLoopCount < MAX_DO_LOOP_RETRY) {
//                    continue;
//                }
//                // We are done with the while loop. 我们已经完成了while循环。
//                break;
//            } catch (Exception ee) {
//                ee.printStackTrace();
//                throw new JobPersistenceException("Couldn't acquire next trigger: " + ee.getMessage(), ee);
//            }
//        } while (true);
//        // Return the acquired trigger list
//        return acquiredTriggers;
//    }
    @Override
    public List<QrtzExecute> acquireNextTriggers(final String application,final long _tsw,final long _tew) throws JobPersistenceException {
        long scheduledFireTime = -1;
        if ((scheduledFireTime=System.currentTimeMillis()) > _tew) {
            throw new IllegalArgumentException();
        }
        final String hostIP = SystemPropGenerator.hostIP();
        final String hostName = SystemPropGenerator.hostName();
        List<QrtzExecute> executeList = new ArrayList<QrtzExecute>(8);
        final int MAX_DO_LOOP_RETRY = 3;
        int currentLoopCount = 0;
        do {
            currentLoopCount ++;
            Connection conn=null;
            try {
                conn = getConnection();
                // 1. 获取窗口内的执行任务
                // 2. 根据任务时间配置计算膨胀任务
                // 3. 返回记录
//                List<Key> keys = getDelegate().selectTriggerToAcquire(conn, noLaterThan + timeWindow, getMisfireTime(), maxCount);
                List<QrtzExecute> dataList = getDelegate().selectExecuteAndJobToAcquire(conn,application,_tsw,_tew,STATE_EXECUTING);
                // No trigger is ready to fire yet. 触发器还没有准备好点火
                if (dataList == null || dataList.isEmpty()){
                    return executeList;
                }
                // 打乱顺序可能在集群环境下有执行优势
                Collections.shuffle(dataList);
                for( QrtzExecute item:dataList){
                    final String jobType = item.getJobType(); // SIMPLE、CRON
                    final Long endTime = item.getEndTime();
                    item.setHostIp(hostIP);
                    item.setHostName(hostName);
                    item.setTimeTriggered( ("SIMPLE".equals(jobType))? (item.getTimeTriggered()+1) : item.getTimeTriggered());
                    item.setScheduledFireTime(scheduledFireTime);
                    // 类加载
                    final String jobClass = item.getJob().getJobClass();
                    if( !"".equals(jobClass.trim()) && null!=getClassLoadHelper() ){
                        Class<? extends Job> jobClazz = null;
                        try {
                            jobClazz = getClassLoadHelper().loadClass(jobClass, Job.class);
                        }catch (Exception e){
                            log.error("类不存在：{}",jobClass,e);
                        }
                        if(null==jobClazz){
                            continue;
                        }
                        item.setJobClazz(jobClazz);
                    }else{
                        // 不是任务
                        continue;
                    }
                    executeList.add(item);
                    if( null!=endTime && (endTime<=0 || endTime<=_tew ) ){
                        List<Date> dateList = null;
                        if("CRON".equals(jobType)){
                            final String cron = item.getCron();
                            Long nextFireTime = item.getNextFireTime();
                            Long startTime = item.getStartTime();
                            Long _endTime = endTime<=0?_tew:(endTime>_tew?null:endTime);
                            String _zoneId = item.getZoneId();
                            if(!( _endTime!=null
                                    && (dateList=this.getExecuteCronTimes(cron,nextFireTime,startTime,_endTime,_zoneId)) !=null && !dateList.isEmpty()
                            )){
                                continue;
                            }
                        }else if("SIMPLE".equals(jobType)){
                            Long nextFireTime = item.getNextFireTime();
                            Long startTime = item.getStartTime();
                            Long _endTime = endTime<=0?_tew:(endTime>_tew?null:endTime);
                            Integer repeatCount = item.getRepeatCount();
                            Integer timeTriggered = item.getTimeTriggered();
                            Integer repeatInterval = item.getRepeatInterval();
                            if(!( _endTime!=null && timeTriggered<repeatCount
                                    && nextFireTime+repeatInterval <=  _tew
                                    && (dateList=this.getExecuteSimpleTimes(nextFireTime,startTime,_endTime,repeatCount,(long)repeatInterval,timeTriggered )) !=null && !dateList.isEmpty()
                            )){
                                continue;
                            }
                        }else{
                            log.error("not support jobType! {}",item);
                        }
                        // 遍历处理
                        final QrtzJob job = item.getJob();
                        for( int i =0;i<dateList.size();i++ ){
                            Date date = dateList.get(i);
                            QrtzExecute _execute = new QrtzExecute(
                                    item.getId(),
                                    item.getPid(),
                                    /*item.getExecuteIdx(),*/
                                    item.getJobType(),
                                    item.getState(),
                                    item.getCron(),
                                    item.getZoneId(),
                                    item.getRepeatCount(),
                                    item.getRepeatInterval(),
                                    item.getTimeTriggered()+i+1,
//                                    i==0?item.getPrevFireTime():dateList.get(i-1).getTime(),// prevFireTime
                                    i==0?item.getNextFireTime():dateList.get(i-1).getTime(),// prevFireTime
                                    date.getTime(), // nextFireTime
                                    item.getHostIp(),
                                    item.getHostName(),
                                    item.getStartTime(),
                                    item.getEndTime());
                            _execute.setJobClazz(item.getJobClazz());
                            _execute.setJob(job);
                            executeList.add(_execute);
                        }
                    }
                }
                return executeList;
            } catch (Exception ee) {
//                throw new JobPersistenceException("Couldn't acquire next trigger: ", ee);
                log.error("Couldn't acquire next trigger: {},{}",application,hostIP, ee);
            }finally {
                cleanupConnection(conn);
            }
        } while (currentLoopCount<=MAX_DO_LOOP_RETRY);
        // Return the acquired trigger list
        return executeList;
    }

    private List<Date> getExecuteCronTimes(String cron, Long execTime, Long startTime, Long endTime,String zoneId){
        List<Date> resultList = new ArrayList<>();
        Date nextFireTime = new Date(execTime);
        CronTriggerImpl cronTrigger=null;
        try {
            cronTrigger = new CronTriggerImpl()
                    .setCronExpression(cron)
                    .setStartTime(new Date(startTime))
                    .setEndTime(new Date(endTime))
                    .setTimeZone(TimeZone.getTimeZone(zoneId));
        }catch (Exception e){
            e.printStackTrace();
        }
        if(null==cronTrigger){
            return resultList;
        }
        do {
            try {
                nextFireTime = cronTrigger.getFireTimeAfter(nextFireTime);
                if(null==nextFireTime || nextFireTime.getTime()>endTime ){
                    break;
                }
                resultList.add(nextFireTime);
            }catch (Exception e){
                e.printStackTrace();
                return resultList;
            }
        }while (nextFireTime!=null && nextFireTime.getTime()<=endTime);
        return resultList;
    }

    private List<Date> getExecuteSimpleTimes(Long execTime,Long startTime,Long endTime,Integer repeatCount,Long repeatInterval,Integer timesTriggered){
        List<Date> resultList = new ArrayList<>();
        Date nextFireTime = new Date(execTime);
        SimpleTriggerImpl simpleTrigger=null;
        try {
            simpleTrigger = new SimpleTriggerImpl()
                    .setStartTime(new Date(startTime))
                    .setEndTime(new Date(endTime))
                    .setRepeatCount(repeatCount)
                    .setRepeatInterval(repeatInterval)
                    .setTimesTriggered(timesTriggered);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(null==simpleTrigger){
            return resultList;
        }
        do {
            try {
                nextFireTime = simpleTrigger.getFireTimeAfter(nextFireTime);
                if(null==nextFireTime || nextFireTime.getTime()>endTime ){
                    break;
                }
                resultList.add(nextFireTime);
            }catch (Exception e){
                e.printStackTrace();
                return resultList;
            }
        }while (nextFireTime!=null && nextFireTime.getTime()<=endTime);
        return resultList;
    }

//    /**
//     * <p>
//     * Inform the <code>JobStore</code> that the scheduler no longer plans to
//     * fire the given <code>Trigger</code>, that it had previously acquired
//     * (reserved).
//     *  通知JobStore调度程序不再计划激发其先前获取（保留）的给定触发器。
//     * </p>
//     */
//    @Override
//    public void releaseAcquiredTrigger(final OperableTrigger trigger) {
//        retryExecuteInNonManagedTXLock(
//            LOCK_TRIGGER_ACCESS,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    try {
//                        // UPDATE {0}JOB_CFG SET TRIGGER_STATE = 'WAITING' WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_STATE = 'ACQUIRED' and TRIGGER_TYPE = ?
//                        getDelegate().updateTriggerStateFromOtherState(conn,trigger.getKey(), STATE_WAITING, STATE_ACQUIRED);
//                        // UPDATE {0}JOB_CFG SET TRIGGER_STATE = 'WAITING' WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_STATE = 'BLOCKED' and TRIGGER_TYPE = ?
//                        getDelegate().updateTriggerStateFromOtherState(conn,trigger.getKey(), STATE_WAITING, STATE_BLOCKED);
//                        // todo ...
////                        // DELETE FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND ENTRY_ID = ? AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ?
////                        getDelegate().deleteFiredTrigger(conn, trigger.getFireInstanceId(),trigger.getKey());
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                        throw new JobPersistenceException("Couldn't release acquired trigger: " + e.getMessage(), e);
//                    }
//                }
//            });
//    }
//
//    protected void releaseAcquiredTrigger(Connection conn, OperableTrigger trigger) throws JobPersistenceException {
//        try {
//            getDelegate().updateTriggerStateFromOtherState(conn,trigger.getKey(), STATE_WAITING, STATE_ACQUIRED);
//            getDelegate().updateTriggerStateFromOtherState(conn,trigger.getKey(), STATE_WAITING, STATE_BLOCKED);
//            getDelegate().deleteFiredTrigger(conn, trigger.getFireInstanceId());
//        } catch (SQLException e) {
//            throw new JobPersistenceException("Couldn't release acquired trigger: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * <p>
//     * Inform the <code>JobStore</code> that the scheduler is now firing the
//     * given <code>Trigger</code> (executing its associated <code>Job</code>),
//     * that it had previously acquired (reserved).
//     *  通知JobStore，调度程序现在正在启动其先前获取（保留）的给定触发器（执行其关联的作业）。
//     * </p>
//     *
//     * @return null if the trigger or its job or calendar no longer exist, or
//     *         if the trigger was not successfully put into the 'executing'
//     *         state.
//     *  如果触发器或其作业或日历不再存在，或者触发器未成功进入“正在执行”状态，则为null。
//     */
//    @Override
//    @SuppressWarnings("unchecked")
//    public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> triggers) throws JobPersistenceException {
//        return executeInNonManagedTXLock(LOCK_TRIGGER_ACCESS,
//                new TransactionCallback<List<TriggerFiredResult>>() {
//                    @Override
//                    public List<TriggerFiredResult> execute(Connection conn) throws JobPersistenceException {
//                        List<TriggerFiredResult> results = new ArrayList<TriggerFiredResult>();
//                        TriggerFiredResult result;
//                        for (OperableTrigger trigger : triggers) {
//                            try {
//                              TriggerFiredBundle bundle = triggerFired(conn, trigger);
//                              result = new TriggerFiredResult(bundle);
//                            } catch (JobPersistenceException jpe) {
//                                result = new TriggerFiredResult(jpe);
//                            } catch(RuntimeException re) {
//                                result = new TriggerFiredResult(re);
//                            }
//                            results.add(result);
//                        }
//                        return results;
//                    }
//                },
//                new TransactionValidator<List<TriggerFiredResult>>() {
//                    @Override
//                    public Boolean validate(Connection conn, List<TriggerFiredResult> result) throws JobPersistenceException {
//                        try {
//                            List<FiredTriggerRecord> acquired = getDelegate().selectInstancesFiredTriggerRecords(conn, getInstanceId());
//                            Set<String> executingTriggers = new HashSet<String>();
//                            for (FiredTriggerRecord ft : acquired) {
//                                if (STATE_EXECUTING.equals(ft.getFireInstanceState())) {
//                                    executingTriggers.add(ft.getFireInstanceId());
//                                }
//                            }
//                            for (TriggerFiredResult tr : result) {
//                                if (tr.getTriggerFiredBundle() != null && executingTriggers.contains(tr.getTriggerFiredBundle().getTrigger().getFireInstanceId())) {
//                                    return true;
//                                }
//                            }
//                            return false;
//                        } catch (SQLException e) {
//                            throw new JobPersistenceException("error validating trigger acquisition", e);
//                        }
//                    }
//                });
//    }
//    protected TriggerFiredBundle triggerFired(Connection conn, OperableTrigger trigger) throws JobPersistenceException {
//        JobDetail jobDetail;
//        Calendar cal = null;
////        // TODO ... 考虑是否需要合并
////        // Make sure trigger wasn't deleted, paused, or completed... 确保触发器未被删除、暂停或完成。。。
////        try {
////            // if trigger was deleted, state will be STATE_DELETED 如果触发器被删除，状态将为STATE_DELETED
////            // SELECT TRIGGER_STATE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE= ?
////            String state = getDelegate().selectTriggerState(conn,trigger.getKey());
////            // 必须是正常执行中的状态
////            // todo ... 考虑是否调整
////            if (!state.equals(STATE_ACQUIRED)) {
////                return null;
////            }
////        } catch (SQLException e) {
////            throw new JobPersistenceException("Couldn't select trigger state: " + e.getMessage(), e);
////        }catch (Exception e2){
////            e2.printStackTrace();
////        }
//
//        try {
//            //  SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ?
//            jobDetail = retrieveJob(conn, trigger.getKey());
//            if (jobDetail == null) { return null; }
//        } catch (JobPersistenceException jpe) {
//            try {
//                getLog().error("Error retrieving job, setting trigger state to ERROR.", jpe);
//                getDelegate().updateTriggerState(conn, trigger.getKey(), STATE_ERROR);
//            } catch (SQLException sqle) {
//                getLog().error("Unable to set trigger state to ERROR.", sqle);
//            }
//            throw jpe;
//        }
////        if (trigger.getCalendarName() != null) {
////            cal = retrieveCalendar(conn, trigger.getCalendarName());
////            if (cal == null) { return null; }
////        }
////        try {
////            // UPDATE QRTZ_FIRED_TRIGGERS SET INSTANCE_NAME = 'SCD2022121400041721629530668', FIRED_TIME = 1721632890871, SCHED_TIME = now(), STATE = 'EXECUTING', IS_NONCONCURRENT = false, REQUESTS_RECOVERY = false
////            //       WHERE SCHED_NAME ='QUARTZ-SPRINGBOOT' AND ENTRY_ID = 'SCD20221214000417216295306681721629530802' AND TRIGGER_NAME = 'Job02TestService::task01' AND TRIGGER_TYPE = 'CRON'
////            getDelegate().updateFiredTrigger(conn, trigger, STATE_EXECUTING,jobDetail);
////        } catch (SQLException e) {
////            throw new JobPersistenceException("Couldn't insert fired trigger: " + e.getMessage(), e);
////        }
//        Date prevFireTime = trigger.getPreviousFireTime();
//        // call triggered - to update the trigger's next-fire-time state...
//        trigger.triggered(cal);
//        String state = STATE_WAITING;
//        boolean force = true;
//        if (trigger.getNextFireTime() == null) {
//            state = STATE_COMPLETE;
//            force = true;
//        }
//        // UPDATE {0}JOB_CFG SET TRIGGER_STATE='WAITING'  ....
//        storeTrigger(conn, trigger, jobDetail, true, state, force, false);
//        jobDetail.getJobDataMap().clearDirtyFlag();
//        // 触发器点火管束 ?
//        return new TriggerFiredBundle(
//                jobDetail,
//                trigger,
//                cal,
////                trigger.getKey().getGroup().equals(Scheduler.DEFAULT_RECOVERY_GROUP),
//                new Date(),
//                trigger.getPreviousFireTime(),
//                prevFireTime,
//                trigger.getNextFireTime()
//        );
//    }
//
//    /**
//     * <p>
//     * Inform the <code>JobStore</code> that the scheduler has completed the
//     * firing of the given <code>Trigger</code> (and the execution its
//     * associated <code>Job</code>), and that the <code>{@link org.quartz.JobDataMap}</code>
//     * in the given <code>JobDetail</code> should be updated if the <code>Job</code>
//     * is stateful.
//     *  通知JobStore调度程序已完成给定触发器的激发（及其相关作业的执行），并且如果作业是有状态的，则应更新给定JobDetail中的JobDataMap。
//     * </p>
//     */
//    @Override
//    public void triggeredJobComplete(final OperableTrigger trigger,final JobDetail jobDetail, final CompletedExecutionInstruction triggerInstCode) {
//        retryExecuteInNonManagedTXLock(
//            LOCK_TRIGGER_ACCESS,
//            new VoidTransactionCallback() {
//                @Override
//                public void executeVoid(Connection conn) throws JobPersistenceException {
//                    triggeredJobComplete(conn, trigger, jobDetail,triggerInstCode);
//                }
//            });
//    }
//    @Deprecated
//    protected void triggeredJobComplete(Connection conn, OperableTrigger trigger, JobDetail jobDetail,CompletedExecutionInstruction triggerInstCode) throws JobPersistenceException {
//        try {
//            if (triggerInstCode == CompletedExecutionInstruction.DELETE_TRIGGER) {
//                if(trigger.getNextFireTime() == null) {
//                    // double check for possible reschedule within job
//                    // execution, which would cancel the need to delete...
//                    // 仔细检查作业执行中可能的重新安排，这将取消删除的需要。。。
//                    TriggerStatus stat = getDelegate().selectTriggerStatus(conn, trigger.getKey());
//                    if(stat != null && stat.getNextFireTime() == null) {
//                        removeTrigger(conn, trigger.getKey());
//                    }
//                } else{
//                    removeTrigger(conn, trigger.getKey());
//                    signalSchedulingChangeOnTxCompletion(0L);
//                }
//            } else if (triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
//                getDelegate().updateTriggerState(conn, trigger.getKey(), STATE_COMPLETE);
//                signalSchedulingChangeOnTxCompletion(0L);
//            } else if (triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
//                getLog().info("Trigger " + trigger.getKey() + " set to ERROR state.");
//                getDelegate().updateTriggerState(conn, trigger.getKey(), STATE_ERROR);
//                signalSchedulingChangeOnTxCompletion(0L);
//            } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
//                getDelegate().updateTriggerStatesForJob(conn, trigger.getKey(), STATE_COMPLETE);
//                signalSchedulingChangeOnTxCompletion(0L);
//            } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
//                getLog().info("All triggers of Job " + trigger.getKey() + " set to ERROR state.");
//                // UPDATE {0}TRIGGERS SET TRIGGER_STATE ='ERROR' WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ?
//                getDelegate().updateTriggerStatesForJob(conn,trigger.getKey(), STATE_ERROR);
//                // 设置线程本地变量 0L
//                signalSchedulingChangeOnTxCompletion(0L);
//            }
////            // todo .. 考虑是否删除
////            if (jobDetail.isConcurrentExectionDisallowed()) {
////                // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE ='WAITING' WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND TRIGGER_STATE = 'BLOCKED'
////                getDelegate().updateTriggerStatesForJobFromOtherState(conn,jobDetail.getKey(),STATE_WAITING,STATE_BLOCKED);
////                // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE ='PAUSED' WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND TRIGGER_STATE = 'PAUSED_BLOCKED'
////                getDelegate().updateTriggerStatesForJobFromOtherState(conn,jobDetail.getKey(), STATE_PAUSED,STATE_PAUSED_BLOCKED);
////                // 设置线程本地变量 0L
////                signalSchedulingChangeOnTxCompletion(0L);
////            }
////            // todo ... 考虑是否需要删除
////            if (jobDetail.isPersistJobDataAfterExecution()) {
////                try {
////                    if (jobDetail.getJobDataMap().isDirty()) {
////                        // UPDATE QRTZ_JOB_DETAILS SET JOB_DATA = ? ...
////                        getDelegate().updateJobData(conn, jobDetail);
////                    }
////                } catch (IOException e) {
////                    throw new JobPersistenceException("Couldn't serialize job data: " + e.getMessage(), e);
////                } catch (SQLException e) {
////                    throw new JobPersistenceException("Couldn't update job data: " + e.getMessage(), e);
////                }
////            }
//        } catch (SQLException e) {
//            throw new JobPersistenceException("Couldn't update trigger state(s): " + e.getMessage(), e);
//        }
//
////        try {
////            // DELETE FROM {0}FIRED_TRIGGERS ...
////            getDelegate().deleteFiredTrigger(conn, trigger.getFireInstanceId(),trigger.getKey());
////        } catch (SQLException e) {
////            e.printStackTrace();
////            throw new JobPersistenceException("Couldn't delete fired trigger: " + e.getMessage(), e);
////        }
//
//    }

    /**
     * <P>
     * Get the driver delegate for DB operations.
     * 获取DB操作的驱动程序委托。
     * </p>
     */
    protected DriverDelegate getDelegate() throws NoSuchDelegateException {
        synchronized(this) {
            if(null == delegate) {
                try {
                    if(delegateClassName != null) {
                        delegateClass = getClassLoadHelper().loadClass(delegateClassName, DriverDelegate.class);
                    }
                    delegate = delegateClass.newInstance();
                    delegate.initialize(getLog(), tablePrefix,application, instanceId, getClassLoadHelper(), canUseProperties(), getDriverDelegateInitString());
                } catch (InstantiationException e) {
                    throw new NoSuchDelegateException("Couldn't create delegate: " + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    throw new NoSuchDelegateException("Couldn't create delegate: " + e.getMessage(), e);
                } catch (ClassNotFoundException e) {
                    throw new NoSuchDelegateException("Couldn't load delegate class: " + e.getMessage(), e);
                }
            }
            return delegate;
        }
    }

    protected Semaphore getLockHandler() {
        return lockHandler;
    }

    public void setLockHandler(Semaphore lockHandler) {
        this.lockHandler = lockHandler;
    }

//    //---------------------------------------------------------------------------
//    // Management methods
//    //---------------------------------------------------------------------------
//    // 恢复熄火
//    // 将已熄火的任务恢复或置为完成
//    protected RecoverMisfiredJobsResult doRecoverMisfires() throws JobPersistenceException {
//        boolean transOwner = false;
//        // 获取连接
//        Connection conn = getNonManagedTXConnection();
//        try {
//            RecoverMisfiredJobsResult result = RecoverMisfiredJobsResult.NO_OP;
//            // Before we make the potentially expensive call to acquire the
//            // trigger lock, peek ahead to see if it is likely we would find
//            // misfired triggers requiring recovery.
//            // 在我们做出可能代价高昂的调用以获取触发锁之前，请提前查看我们是否可能发现需要恢复的误触发。
//            // #getDoubleCheckLockMisfireHandler() 总是返回true
//            // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_JOB_CFG
//            // WHERE SCHED_NAME ='QUARTZ-SPRINGBOOT' AND NOT (MISFIRE_INSTR = -1)
//            // AND NEXT_FIRE_TIME < (now()-60s) AND TRIGGER_STATE = 'WAITING'
//            int misfireCount = (getDoubleCheckLockMisfireHandler()) ?
//                getDelegate().countMisfiredTriggersInState(conn, STATE_WAITING, getMisfireTime()) :
//                Integer.MAX_VALUE;
//
//            if (misfireCount == 0) {
//                getLog().debug("Found 0 triggers that missed their scheduled fire-time.");
//            } else {
//                // 加锁+
//                transOwner = getLockHandler().obtainLock(conn, LOCK_TRIGGER_ACCESS);
//                result = recoverMisfiredJobs(conn, false);
//            }
//            commitConnection(conn);
//            return result;
//        } catch (JobPersistenceException e) {
//            rollbackConnection(conn);
//            throw e;
//        } catch (SQLException e) {
//            rollbackConnection(conn);
//            throw new JobPersistenceException("Database error recovering from misfires.", e);
//        } catch (RuntimeException e) {
//            rollbackConnection(conn);
//            throw new JobPersistenceException("Unexpected runtime exception: " + e.getMessage(), e);
//        } finally {
//            try {
//                releaseLock(LOCK_TRIGGER_ACCESS, transOwner);
//            } finally {
//                cleanupConnection(conn);
//            }
//        }
//    }

    protected ThreadLocal<Long> sigChangeForTxCompletion = new ThreadLocal<Long>();
    protected void signalSchedulingChangeOnTxCompletion(long candidateNewNextFireTime) {
        Long sigTime = sigChangeForTxCompletion.get();
        if(sigTime == null && candidateNewNextFireTime >= 0L){
            sigChangeForTxCompletion.set(candidateNewNextFireTime);
        }
        else {
            if(sigTime == null || candidateNewNextFireTime < sigTime){
                sigChangeForTxCompletion.set(candidateNewNextFireTime);
            }
        }
    }
    
    protected Long clearAndGetSignalSchedulingChangeOnTxCompletion() {
        Long t = sigChangeForTxCompletion.get();
        sigChangeForTxCompletion.set(null);
        return t;
    }

    protected void signalSchedulingChangeImmediately(long candidateNewNextFireTime) {
        schedSignaler.signalSchedulingChange(candidateNewNextFireTime);
    }

    //---------------------------------------------------------------------------
    // Cluster management methods 集群管理方法
    //---------------------------------------------------------------------------

//    protected boolean firstCheckIn = true;

//    protected long lastCheckin = System.currentTimeMillis();


//
//    // 群集恢复
//    @SuppressWarnings("ConstantConditions")
//    protected void clusterRecover(Connection conn, List<SchedulerStateRecord> failedInstances) throws JobPersistenceException {
//        if (failedInstances.size() > 0) {
//            long recoverIds = System.currentTimeMillis();
//            // 日志记录 如果是>0记录为info，否则为debug
//            logWarnIfNonZero(failedInstances.size(), "ClusterManager: detected " + failedInstances.size() + " failed or restarted instances.");
//            try {
//                for (SchedulerStateRecord rec : failedInstances) {
//                    getLog().info("ClusterManager: Scanning for instance \"" + rec.getSchedulerInstanceId() + "\"'s failed in-progress jobs.");
//                    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = 'SCD2022121400041721196413141'
//                    List<FiredTriggerRecord> firedTriggerRecs = getDelegate().selectInstancesFiredTriggerRecords(conn,rec.getSchedulerInstanceId());
//                    int acquiredCount = 0;
//                    int recoveredCount = 0;
//                    int otherCount = 0;
////                    Set<TriggerKey> triggerKeys = new HashSet<TriggerKey>();
//                    Set<Key> keys = new HashSet<Key>();
//                    for (FiredTriggerRecord ftRec : firedTriggerRecs) {
////                        TriggerKey tKey = ftRec.getTriggerKey();
////                        JobKey jKey = ftRec.getJobKey();
//                        Key key = ftRec.getKey();
//                        keys.add(key);
//                        // release blocked triggers.. 释放被阻止的触发器。。
//                        if (ftRec.getFireInstanceState().equals(STATE_BLOCKED)) {
//                            // UPDATE QRTZ_JOB_CFG SET TRIGGER_STATE = 'WAITING' WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? AND TRIGGER_STATE = 'BLOCKED'
//                            getDelegate().updateTriggerStatesForJobFromOtherState(conn,key,STATE_WAITING, STATE_BLOCKED);
//                        } else if (ftRec.getFireInstanceState().equals(STATE_PAUSED_BLOCKED)) {
//                            // UPDATE QRTZ_JOB_CFG SET TRIGGER_STATE = 'PAUSED' WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? AND TRIGGER_STATE = 'PAUSED_BLOCKED'
//                            getDelegate().updateTriggerStatesForJobFromOtherState(conn,key,STATE_PAUSED,STATE_PAUSED_BLOCKED);
//                        }
//
//                        // release acquired triggers.. 释放已获取的触发器。。
//                        if (ftRec.getFireInstanceState().equals(STATE_ACQUIRED)) {
//                            // UPDATE QRTZ_JOB_CFG SET TRIGGER_STATE ='WAITING' WHERE SCHED_NAME ='MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_STATE = 'ACQUIRED' and TRIGGER_TYPE = ?
//                            getDelegate().updateTriggerStateFromOtherState(conn,key, STATE_WAITING, STATE_ACQUIRED);
//                            acquiredCount++;
//                        }
//                        //  todo .. 这里总会是false，考虑是否移除
//                        else if (ftRec.isJobRequestsRecovery()) {
//                            // handle jobs marked for recovery that were not fully
//                            // executed..
//                            if (jobDetailExists(conn,key)) {
//                                @SuppressWarnings("deprecation")
////                                SimpleTriggerImpl rcvryTrig = new SimpleTriggerImpl(
////                                        "recover_"+ rec.getSchedulerInstanceId()+ "_"+ (recoverIds++),
////                                        Scheduler.DEFAULT_RECOVERY_GROUP,
////                                        new Date(ftRec.getScheduleTimestamp()));
//                                // 构建个SIMPLE触发器，同时将其存储至JOB_CFG
//                                SimpleTriggerImpl rcvryTrig = new SimpleTriggerImpl("recover_"+ rec.getSchedulerInstanceId()+ "_"+ (recoverIds++),new Date(ftRec.getScheduleTimestamp()));
////                                rcvryTrig.setJobName(jKey.getName());
////                                rcvryTrig.setJobGroup(jKey.getGroup());
//                                rcvryTrig.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
//                                rcvryTrig.setPriority(ftRec.getPriority());
////                                JobDataMap jd = getDelegate().selectTriggerJobDataMap(conn, tKey.getName(), tKey.getGroup());
////                                JobDataMap jd = getDelegate().selectTriggerJobDataMap(conn,key.getName());
////                                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME,key.getName());
//////                                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_GROUP, tKey.getGroup());
////                                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_FIRETIME_IN_MILLISECONDS, String.valueOf(ftRec.getFireTimestamp()));
////                                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS, String.valueOf(ftRec.getScheduleTimestamp()));
////                                rcvryTrig.setJobDataMap(jd);
//
//                                rcvryTrig.computeFirstFireTime(null);
//                                storeTrigger(conn, rcvryTrig, null, false,STATE_WAITING, false, true);
//                                recoveredCount++;
//                            } else {
//                                getLog().warn("ClusterManager: failed job '" + key + "' no longer exists, cannot schedule recovery.");
//                                otherCount++;
//                            }
//                        } else {
//                            otherCount++;
//                        }
//
//                        // free up stateful job's triggers 释放有状态作业的触发器
//                        // todo ... 这里机会总是false，考虑是否要移除
//                        if (ftRec.isJobDisallowsConcurrentExecution()) {
//                            // UPDATE QRTZ_JOB_CFG SET TRIGGER_STATE = 'WAITING' WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? AND TRIGGER_STATE = 'BLOCKED'
//                            getDelegate().updateTriggerStatesForJobFromOtherState(conn,key,STATE_WAITING,STATE_BLOCKED);
//                            // UPDATE QRTZ_JOB_CFG SET TRIGGER_STATE = 'PAUSED' WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? AND TRIGGER_STATE = 'PAUSED_BLOCKED'
//                            getDelegate().updateTriggerStatesForJobFromOtherState(conn,key,STATE_PAUSED,STATE_PAUSED_BLOCKED);
//                        }
//                    }
//                    // DELETE FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
//                    getDelegate().deleteFiredTriggers(conn,rec.getSchedulerInstanceId());
//
//                    // Check if any of the fired triggers we just deleted were the last fired trigger
//                    // records of a COMPLETE trigger.
//                    // 检查我们刚刚删除的任何已触发触发器是否是COMPLETE触发器的最后一个已触发触发器记录。
//                    int completeCount = 0;
//                    for (Key k : keys) {
//                        // k from QRTZ_FIRED_TRIGGERS
//                        if (getDelegate().selectTriggerState(conn,k).equals(STATE_COMPLETE)) {
//                            // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ?
//                            List<FiredTriggerRecord> firedTriggers = getDelegate().selectFiredTriggerRecords(conn,k/*, triggerKey.getGroup()*/);
//                            if (firedTriggers.isEmpty()) {
//                                // // DELETE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//                                if (removeTrigger(conn,k)) {
//                                    completeCount++;
//                                }
//                            }
//                        }
//                    }
//                    logWarnIfNonZero(acquiredCount,"ClusterManager: ......Freed " + acquiredCount + " acquired trigger(s).");
//                    logWarnIfNonZero(completeCount,"ClusterManager: ......Deleted " + completeCount + " complete triggers(s).");
//                    logWarnIfNonZero(recoveredCount,"ClusterManager: ......Scheduled " + recoveredCount + " recoverable job(s) for recovery.");
//                    logWarnIfNonZero(otherCount,"ClusterManager: ......Cleaned-up " + otherCount + " other failed job(s).");
//                    // schedulerInstanceId 即是 QRTZ_SCHEDULER_STATE::INSTANCE_NAME
//                    if (!rec.getSchedulerInstanceId().equals(getInstanceId())) {
//                        // DELETE FROM QRTZ_SCHEDULER_STATE WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
//                        getDelegate().deleteSchedulerState(conn,rec.getSchedulerInstanceId());
//                    }
//                }
//            } catch (Throwable e) {
//                e.printStackTrace();
//                throw new JobPersistenceException("Failure recovering jobs: " + e.getMessage(), e);
//            }
//        }
//    }

    protected void logWarnIfNonZero(int val, String warning) {
        if (val > 0) {
            getLog().info(warning);
        } else {
            getLog().debug(warning);
        }
    }

    /**
     * <p>
     * Cleanup the given database connection.  This means restoring
     * any modified auto commit or transaction isolation connection
     * attributes, and then closing the underlying connection.
     *  清理给定的数据库连接。这意味着恢复任何修改后的自动提交或事务隔离连接属性，然后关闭基础连接。
     * </p>
     * 
     * <p>
     * This is separate from closeConnection() because the Spring 
     * integration relies on being able to overload closeConnection() and
     * expects the same connection back that it originally returned
     * from the datasource.
     *  这与“closeConnection（）”是分开的，因为Spring集成依赖于能够重载“closeConnection”（），并期望返回与最初从数据源返回的连接相同的连接。
     * </p>
     * 
     * @see #closeConnection(Connection)
     */
    protected void cleanupConnection(Connection conn) {
        if (conn != null) {
            if (conn instanceof Proxy) {
                Proxy connProxy = (Proxy)conn;
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(connProxy);
                if (invocationHandler instanceof AttributeRestoringConnectionInvocationHandler) {
                    AttributeRestoringConnectionInvocationHandler connHandler = (AttributeRestoringConnectionInvocationHandler)invocationHandler;
                    connHandler.restoreOriginalAtributes();
                    closeConnection(connHandler.getWrappedConnection());
                    return;
                }
            }
            // Wan't a Proxy, or was a Proxy, but wasn't ours.
            closeConnection(conn);
        }
    }
    
    
    /**
     * Closes the supplied <code>Connection</code>.
     * 关闭提供的连接。
     * <p>
     * Ignores a <code>null Connection</code>.  
     * Any exception thrown trying to close the <code>Connection</code> is
     * logged and ignored.
     *  忽略空连接。任何试图关闭连接时引发的异常都会被记录并忽略。
     * </p>
     * 
     * @param conn The <code>Connection</code> to close (Optional).
     */
    protected void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                getLog().error("Failed to close Connection", e);
            } catch (Throwable e) {
                getLog().error("Unexpected exception closing Connection." + "  This is often due to a Connection being returned after or during shutdown.", e);
            }
        }
    }

    /**
     * Rollback the supplied connection. 回滚提供的连接。
     * 
     * <p>  
     * Logs any SQLException it gets trying to rollback, but will not propogate
     * the exception lest it mask the exception that caused the caller to 
     * need to rollback in the first place.
     *  记录它试图回滚的任何SQLException，但不会建议该异常，以免它首先掩盖导致调用方需要回滚的异常。
     * </p>
     *
     * @param conn (Optional)
     */
    protected void rollbackConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                getLog().error("Couldn't rollback jdbc connection. "+e.getMessage(), e);
            }
        }
    }
    
    /**
     * Commit the supplied connection
     * 提交（事物）提供的连接
     * @param conn (Optional)
     * @throws JobPersistenceException thrown if a SQLException occurs when the
     * connection is committed
     */
    protected void commitConnection(Connection conn) throws JobPersistenceException {
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new JobPersistenceException("Couldn't commit jdbc connection. "+e.getMessage(), e);
            }
        }
    }
    
    /**
     * Implement this interface to provide the code to execute within
     * the a transaction template.  If no return value is required, execute
     * should just return null.
     *  实现此接口以提供要在事务模板中执行的代码。如果不需要返回值，execute应该只返回null。
     * 
     * @see JobStoreSupport#executeInNonManagedTXLock(String, TransactionCallback, TransactionValidator)
     * @see JobStoreSupport#executeInLock(String, TransactionCallback)
     * @see JobStoreSupport#executeWithoutLock(TransactionCallback)
     */
    protected interface TransactionCallback<T> {
        T execute(Connection conn) throws JobPersistenceException;
    }

    protected interface TransactionValidator<T> {
        Boolean validate(Connection conn, T result) throws JobPersistenceException;
    }
    
    /**
     * Implement this interface to provide the code to execute within
     * the a transaction template that has no return value.
     *  实现此接口以提供在没有返回值的事务模板中执行的代码。
     *
     * @see JobStoreSupport#executeInNonManagedTXLock(String, TransactionCallback, TransactionValidator)
     */
    protected abstract class VoidTransactionCallback implements TransactionCallback<Void> {
        @Override
        public final Void execute(Connection conn) throws JobPersistenceException {
            executeVoid(conn);
            return null;
        }
        
        abstract void executeVoid(Connection conn) throws JobPersistenceException;
    }

    /**
     * Execute the given callback in a transaction. Depending on the JobStore, 
     * the surrounding transaction may be assumed to be already present 
     * (managed).  
     *  在事务中执行给定的回调。根据JobStore，可以假设周围的事务已经存在（管理）。
     *
     * <p>
     * This method just forwards to executeInLock() with a null lockName.
     *  此方法只是转发到具有null lockName的executeInLock（）。
     * </p>
     * 
     * @see #executeInLock(String, TransactionCallback)
     */
    public <T> T executeWithoutLock(TransactionCallback<T> txCallback) throws JobPersistenceException {
        return executeInLock(null, txCallback);
    }

    /**
     * Execute the given callback having acquired the given lock.
     * Depending on the JobStore, the surrounding transaction may be 
     * assumed to be already present (managed).
     *  获取给定锁后执行给定回调。根据JobStore，可以假设周围的事务已经存在（管理）。
     *
     * @param lockName The name of the lock to acquire, for example
     * "TRIGGER_ACCESS".  If null, then no lock is acquired, but the
     * lockCallback is still executed in a transaction.
     *  lockName–要获取的锁的名称，例如“TRIGGER_ACCESS”。如果为null，则不会获取锁，但lockCallback仍在事务中执行。
     *
     */
    protected abstract <T> T executeInLock(String lockName, TransactionCallback<T> txCallback) throws JobPersistenceException;
    
    protected <T> T retryExecuteInNonManagedTXLock(String lockName, TransactionCallback<T> txCallback) {
        for (int retry = 1; !shutdown; retry++) {
            try {
                return executeInNonManagedTXLock(lockName, txCallback, null);
            } catch (JobPersistenceException jpe) {
                if(retry % 4 == 0) {
                    log.error("An error occurred while {},{} " ,txCallback, jpe);
//                    schedSignaler.notifySchedulerListenersError("An error occurred while " + txCallback, jpe);
                }
            } catch (RuntimeException e) {
                getLog().error("retryExecuteInNonManagedTXLock: RuntimeException " + e.getMessage(), e);
            }
            try {
                Thread.sleep(getDbRetryInterval()); // retry every N seconds (the db connection must be failed)
            } catch (InterruptedException e) {
                throw new IllegalStateException("Received interrupted exception", e);
            }
        }
        throw new IllegalStateException("JobStore is shutdown - aborting retry");
    }
    
    /**
     * Execute the given callback having optionally acquired the given lock.
     * This uses the non-managed transaction connection.
     *  执行给定的回调，可以选择获取给定的锁。这使用非托管事务连接。
     *
     * @param lockName The name of the lock to acquire, for example
     * "TRIGGER_ACCESS".  If null, then no lock is acquired, but the
     * lockCallback is still executed in a non-managed transaction.
     *  lockName–要获取的锁的名称，例如“TRIGGER_ACCESS”。如果为null，则不会获取锁，但lockCallback仍在非托管事务中执行。
     */
    @Deprecated
    protected <T> T executeInNonManagedTXLock(String lockName, TransactionCallback<T> txCallback, final TransactionValidator<T> txValidator) throws JobPersistenceException {
        boolean transOwner = false;
        Connection conn = null;
        try {
            if (lockName != null) {
                // If we aren't using db locks, then delay getting DB connection 
                // until after acquiring the lock since it isn't needed.
                if (getLockHandler().requiresConnection()) {
                    conn = getNonManagedTXConnection();
                }
                transOwner = getLockHandler().obtainLock(conn, lockName);
            }
            if (conn == null) {
                conn = getNonManagedTXConnection();
            }
            final T result = txCallback.execute(conn);
            try {
                commitConnection(conn);
            } catch (JobPersistenceException e) {
                rollbackConnection(conn);
                if (txValidator == null || !retryExecuteInNonManagedTXLock(lockName, new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean execute(Connection conn) throws JobPersistenceException {
                        return txValidator.validate(conn, result);
                    }
                })) {
                    throw e;
                }
            }
            Long sigTime = clearAndGetSignalSchedulingChangeOnTxCompletion();
            if(sigTime != null && sigTime >= 0) {
                signalSchedulingChangeImmediately(sigTime);
            }
            return result;
        } catch (JobPersistenceException e) {
            rollbackConnection(conn);
            throw e;
        } catch (RuntimeException e) {
            rollbackConnection(conn);
            throw new JobPersistenceException("Unexpected runtime exception: " + e.getMessage(), e);
        } finally {
            try {
                releaseLock(lockName, transOwner);
            } finally {
                cleanupConnection(conn);
            }
        }
    }
    
    /////////////////////////////////////////////////////////////////////////////
    //
    // ClusterManager Thread 群集管理器线程
    //
    /////////////////////////////////////////////////////////////////////////////

    private volatile boolean FIRST_CHECK = true;
    private static Object lockCheck = new Object();
    // 集群及熄火任务处理
    final class ClusterMisfireHandler extends Thread{

        private long TIME_CHECK_INTERVAL = 15000L;
        // 上一次清理时间
        private long PRE_CLEAR_TIME = System.currentTimeMillis()-86400_000L;
        ClusterMisfireHandler(){
            this.setPriority(Thread.NORM_PRIORITY + 2);
//            this.setName("QuartzScheduler_" + instanceName + "-" + instanceId + "_ClusterMisfireHandler");
//            this.setName("QuartzScheduler_" +SystemPropGenerator.hostIP()+ "_ClusterMisfireHandler");
            this.setName(getInstanceId()+ "_ClusterMisfireHandler");
            this.setDaemon(getMakeThreadsDaemons());
        }
        public void shutdown() {
            shutdown = true;
            this.interrupt();
        }
        @Override
        public void run(){
            synchronized (lockCheck){
            final String hostIP = SystemPropGenerator.hostIP();
//            long _t = System.currentTimeMillis();
             long _start  = System.currentTimeMillis()/1000*1000;
              while (!shutdown){
//                  System.out.println("support耗时:"+(System.currentTimeMillis()-_t));
//                  _t=System.currentTimeMillis();
//                  getLog().info("........PROCESS...........");

                  Connection conn = null;
                  /***** try start... *****/
                  try {
                      // String hostName = SystemPropGenerator.hostName();
                      // ## 加锁
                      //1.获取qrtz_app记录
                      conn = getNonManagedTXConnection();
//                      conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
//                      conn.setAutoCommit(true);
                      QrtzApp app = getDelegate().findQrtzAppByApp(conn, getInstanceName());
                      // 由于状态可能存在不一致 app:state ~ node:state
//                      if( null==app || (app.getTimeNext()-4)>_start /*|| "N".equals(app.getState())*/){
                      if( null==app /*|| "N".equals(app.getState())*/){
                          getLog().info("APPLICATION is empty:{},{} ",getInstanceName(),hostIP);
                          continue;
                      }
                      //2.更新qrtz_app以获取锁
                      app.setTimePre(app.getTimeNext());
                      app.setTimeNext(_start+TIME_CHECK_INTERVAL);
                      app.setTimeInterval(TIME_CHECK_INTERVAL);
//                      if( getDelegate().updateQrtzAppByApp(conn,app,_start,"Y") < 1){
//                          getLog().info("ClusterMisfireHandler lock by other node ... ");
//                          continue;
//                      }
                      getDelegate().updateQrtzAppByApp(conn,app/*,_start,"Y"*/);// 更新app,不论成功与否必须要这样做，否则当前support在state=N的节点执行时无法更新job及execute
//                      getLog().info(">>> In process Cluster! <<<");
                      // ## 更新节点信息
                      //1.获取qrtz_node记录
                      QrtzNode node = getDelegate().findQrtzNodeByAppHost(conn, getInstanceName(),hostIP);
                      if( null==node || "N".equals(node.getState() )){ // 这个state判断十分重要，决定了当前节点是否参与 job及execute的 更新，如果是N就无需执行之后的逻辑
                          getLog().info("node is empty or state=N : {},{}",getInstanceName(),hostIP);
                          continue;
                      }
                      getLog().info(">>> In process Cluster "+getInstanceId()+" ! <<<");
                      //2.更新状态不一致的记录 (app:state=N且node:state=Y时:应用关闭时节点一定要关闭 )
                      if( "N".equals(app.getState()) &&  "Y".equals(node.getState()) ){
                          node.setState("N");
                          node.setTimeCheck(_start);
                          getDelegate().updateQrtzNodeOfState(conn,node);
                          continue;
                      }
                      //3.更新time_check (根据频度)
                      else if( (_start - node.getTimeCheck()) >= 86400_000L ){
                          node.setTimeCheck(_start);
                          getDelegate().updateQrtzNodeOfTimeCheck(conn,node);
                      }
                      if(FIRST_CHECK==false) {
                          // ## 清理历史记录(每间隔七天&&上午十点)
                          if ((_start - PRE_CLEAR_TIME) >= 86400_000L * 7 && LocalDateTime.now().getHour() - 10 == 0) {
                              getDelegate().clearHistoryData(conn, 366 * 86400_000L);// 1年=1天*366
                              PRE_CLEAR_TIME = _start;// update time
                          }
                          // 修正执行信息
                          recoverExecute(_start/*conn,app,node*/);
                          // 修正配置信息
                          recoverJob(/*conn,app,node*/);
                      }
                  }catch (Exception e){
                      e.printStackTrace();
                  }
                  finally {
                      // ## 休眠8S
                      // 这个8S是当前次循环所耗费的总时间,所以这里的休眠时间一般小于8S
                      // getLog().info("invoke sleep ... ");
//                      System.out.println("invoke sleep ... ");
                      // 休眠等待下一次
                      long sleep_time = 0;
                      if ( (sleep_time = (TIME_CHECK_INTERVAL-(System.currentTimeMillis() - _start)-2))>0 ) {
                          try {
                              conn.commit();
                              cleanupConnection(conn);
//                              getLog().info("SLEEP ：{} ",sleep_time);
                              TimeUnit.MILLISECONDS.sleep(sleep_time);
                          } catch (Exception e) {
                          }
                      }
                      _start=System.currentTimeMillis();
                  }
                  /***** try end... *****/
//                  long sleep_time = 0;
//                  if ( (sleep_time = (TIME_CHECK_INTERVAL-(System.currentTimeMillis() - _start)-2))>4 ) {
//                      try {
////                              getLog().info("to sleep ：{} ",sleep_time);
////                          TimeUnit.MILLISECONDS.sleep(sleep_time);
//                          lockCheck.wait(sleep_time);
////                              try {
////                                  conn.commit();
////                              } catch (SQLException e) {
////                                  e.printStackTrace();
////                              }
////                          cleanupConnection(conn);
//                      } catch (InterruptedException e) {
//                      }
//                  }
              }
            }
        }
        // 修正配置信息
        private void recoverJob(/*Connection conn, QrtzApp app, QrtzNode node*/) {
            // 修正配置信息
            //针对已有终态任务做状态更新
            //1.状态
            //2.更新时间

            // 更新状态及时间(update_time)
            // 1.查找job记录( state!=COMPLETE,INIT,PAUSED ) 及其下的所有execute记录
            //  + 从execute列表中寻找,若有 EXECUTING 则 更新job信息 state=EXECUTING update_time=now
            //  + 从execute列表中寻找,若有 PAUSED  则 更新job信息 state=PAUSED update_time=now
            //  + 从execute列表中寻找,若有 ERROR  则 更新job信息 state=ERROR update_time=now
            // 2.查找job记录( state=COMPLETE update_time>1年的 ) 并删除,按频度执行逻辑
            Connection conn = null;
            final String applicaton = getInstanceName();
            final String hostIP = SystemPropGenerator.hostIP();
            final String hostName = SystemPropGenerator.hostName();
            final Long now = System.currentTimeMillis();
            /***** try start... *****/
            try {
                //0.获取节点下异常执行项 (start_time>now and end_time>0 end_time is not null and next_fire_time<now and state!=(COMPLETE,INIT,PAUSED) )
                conn = getNonManagedTXConnection();
                List<QrtzJob> jobs = getDelegate().findQrtzJobByAppForRecover(conn,applicaton);
                for(QrtzJob job:jobs){
                    List<QrtzExecute> executes = getDelegate().findAllQrtzExecuteByPID(conn,job.getId());
                    boolean hasExecuting = false;
                    boolean hasPaused = false;
                    boolean hasError = false;
                    for( QrtzExecute execute:executes ){
                        final String state = execute.getState();
                        if("EXECUTING".equals(state)){
                            hasExecuting=true;
                        }else if("PAUSED".equals(state)){
                            hasPaused=true;
                        }else if("ERROR".equals(state)){
                            hasError=true;
                        }else{
                            continue;
                        }
                    }
                    // 如果所有状态都有则按以下优先级来
                    if(hasError){
                        job.setState("ERROR");
                    }else if(hasExecuting){
                        job.setState("EXECUTING");
                    }else if(hasPaused){
                        job.setState("PAUSED");
                    }else{
                        continue;
                    }
                    job.setUpdateTime(now);
                    getDelegate().updateRecoverJob(conn,job);
                }
                //2. 清理 state=COMPLETE && update_time >1年的清理(删除),按频度执行逻辑
                if( (now - PRE_CLEAR_TIME) >= 86400_000L*7 && LocalDateTime.now().getHour()-10==0 ){
                    int ct = getDelegate().clearAllJobData(conn,366*86400_000L);
                    log.error(".....已清理execute数据 {}条.....",ct);
                }
            }catch (Exception e){
                log.error("异常了：{},{},{},{}",applicaton,hostIP,hostName,now,e);
                e.printStackTrace();
            }finally {
                cleanupConnection(conn);
            }
            /***** try start... *****/

        }

        // 修正执行信息
        private void recoverExecute(long now/*Connection conn, QrtzApp app, QrtzNode node */) {
            // 修正执行信息
            //针对中途停止的异常熄火的
            //0.获取节点下异常执行项 (start_time<=now and end_time!=0 end_time is not null and next_fire_time<now and state!=(COMPLETE,INIT,PAUSED) )
            //1.调整状态 及 下一次执行时间
            // + 对于SIMPLE任务
            //  - 若 end_time>0 and end_time>=now 则修改 state=COMPLETE(完成)
            //  - 若 repeat_count>0 && time_triggered>=repeat_count 则修改 state=COMPLETE(完成)
            //  - 根据 (now+24S)>start_time?(now+24S):start_time 计算 next_fire_time 同时修改 next_fire_time=next_fire_time(new) state=EXECUTING
            // + 对于CRON任务 先计算 next_fire_time (参照时间: (now+24S)>start_time?(now+24S):start_time 且end_time>now )
            //  - 若无 则修正 end_time=next_fire_time state=COMPLETE
            //  - 若有 则修正 next_fire_time=next_fire_time(new) state=EXECUTING
            // + todo : 对于调整好的且仍为EXECUTING状态的任务是否应该唤醒QuartzSchedulerThread进行执行(如果不唤醒对于短时任务可能错过执行时间 )
            //2. 清理 state=COMPLETE && next_fire_time >1年的清理(删除),按频度执行逻辑

            Connection conn = null;
            final String applicaton = getInstanceName();
            final String hostIP = SystemPropGenerator.hostIP();
            final String hostName = SystemPropGenerator.hostName();
//            final Long now = System.currentTimeMillis();
            /***** try start... *****/
            try {
                //0.获取节点下异常执行项 (start_time>now and end_time>0 end_time is not null and next_fire_time<now and state!=(COMPLETE,INIT,PAUSED) )
//                conn = getNonManagedTXConnection();
                conn = getConnection();
                // SELECT * FROM {0}JOB WHERE APPLICATION="QUARTZ-SPRINGBOOT" AND STATE!="COMPLETE" AND STATE!="INIT" AND STATE!=PAUSED
                List<QrtzJob> jobs = getDelegate().findQrtzJobByAppForRecover(conn,applicaton);
//                for(int i=0;i<jobs.size();i++ ){
//                    // state!=(COMPLETE,INIT,PAUSED)
//                    String jobState = jobs.get(i).getState();
//                    if("COMPLETE".equals(jobState) || "INIT".equals(jobState) || "PAUSED".equals(jobState)){
//                        jobs.remove(jobs.get(i));
//                    }
//                }
                // SELECT * FROM QRTZ_EXECUTE WHERE PID=? AND NEXT_FIRE_TIME<=? AND STATE!=? AND STATE!=? AND STATE!=?
                List<QrtzExecute> executes = getDelegate().findQrtzExecuteForRecover(conn,jobs,now-5000L-8); // 这个5S很重要，一旦与QuartzSchedulerThread的执行时间无法错开则导致任务无法执行
                for(QrtzExecute execute:executes){
                    final String jobType = execute.getJobType();
                    final Long endTime = execute.getEndTime();
                    if( "CRON".equals(jobType) ){
                        execute.setHostIp(hostIP);
                        execute.setHostName(hostName);
                        // + 对于CRON任务 先计算 next_fire_time (参照时间: (now+24S)>start_time?(now+24S):start_time 且end_time>now )
                        //  - 若无 则修正 end_time=next_fire_time state=COMPLETE
                        //  - 若有 则修正 next_fire_time=next_fire_time(new) state=EXECUTING
                        // + todo : 对于调整好的且仍为EXECUTING状态的任务是否应该唤醒QuartzSchedulerThread进行执行(如果不唤醒对于短时任务可能错过执行时间 )
                        CronTriggerImpl cronTrigger = new CronTriggerImpl()
                                .setCronExpression(execute.getCron())
                                .setStartTime(new Date(execute.getStartTime()))
                                .setEndTime(null==endTime?null:new Date(endTime))
                                .setTimeZone(TimeZone.getTimeZone(execute.getZoneId()));
                        // loop时间 => QuartzSchedulerThread:run一次的时间,默认5S
                        // check时间 => JobStoreSupport:run一次的时间,默认15S
                        // 这个afterTime时间非常重要，它一定要大于check时间 ( loop时间<=afterTime<check时间 )以保证loop时被扫到
                        Date nextFireTime =cronTrigger.getFireTimeAfter(new Date(now+TIME_CHECK_INTERVAL/2));
                        // 对 SIMPLE 任务的保存
                        if(null==nextFireTime){
                            // 没有下一次执行时间就是执行完成
//                                execute.setNextFireTime(null);
                            execute.setState("COMPLETE");
                        }else{
                            execute.setNextFireTime(nextFireTime.getTime());
                            execute.setState("EXECUTING");
                        }
//                        getDelegate().updateRecoverExecute(conn,execute);
                    }else if("SIMPLE".equals(jobType)){
                        execute.setHostIp(hostIP);
                        execute.setHostName(hostName);
                        //+ 对于SIMPLE任务
                        // - 若 end_time>0 and end_time>=now 则修改 state=COMPLETE(完成)
                        // - 若 repeat_count>0 && time_triggered>=repeat_count 则修改 state=COMPLETE(完成)
                        // - 根据 (now+16S)>start_time?(now+16S):start_time 计算 next_fire_time 同时修改 next_fire_time=next_fire_time(new) state=EXECUTING
                        if( null!=execute.getEndTime() && execute.getEndTime()>=now){
                            execute.setState("COMPLETE");
                        }else if( execute.getTimeTriggered()>=execute.getRepeatCount() ){
                            execute.setState("COMPLETE");
                        }else if( (now+TIME_CHECK_INTERVAL*2)>execute.getStartTime() ){
                            SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl()
                                    .setStartTime(new Date(execute.getStartTime()))
//                                    .setEndTime(new Date(execute.getEndTime()))
                                    .setEndTime(null==endTime?null:new Date(endTime))
                                    .setRepeatCount(execute.getRepeatCount())
                                    .setRepeatInterval(execute.getRepeatInterval())
                                    .setTimesTriggered(execute.getTimeTriggered());
                            Date nextFireTime = simpleTrigger.getFireTimeAfter(new Date(now+TIME_CHECK_INTERVAL));
                            if(null==nextFireTime){
                                // 没有下一次执行时间就是执行完成
//                                execute.setNextFireTime(null);
                                execute.setState("COMPLETE");
                            }else{
                                execute.setNextFireTime(nextFireTime.getTime());
                                execute.setState("EXECUTING");
                            }
                        }else {
                            continue;
                        }

                    }else{
                        log.error("jobType is not support! {},{},{}",applicaton,hostIP,jobType);
                        continue;
                    }
                    // 对 CRON/SIMPLE 任务的保存
                    getDelegate().updateRecoverExecute(conn,execute);
                }
                //2. 清理 state=COMPLETE && next_fire_time >1年的清理(删除),按频度执行逻辑
                if( (now - PRE_CLEAR_TIME) >= 86400_000L*7 && LocalDateTime.now().getHour()-10==0 ){
                    int ct = getDelegate().clearAllExecuteData(conn,366*86400_000L);
                    log.error(".....已清理execute数据 {}条.....",ct);
                }
            }catch (Exception e){
                log.error("异常了：{},{},{},{}",applicaton,hostIP,hostName,now,e);
                e.printStackTrace();
            }finally {
                cleanupConnection(conn);
            }
            /***** try start... *****/

        }

        // 前置处理
        //0.加对象锁
        //1.尝试写入qrtz_app
        //2.尝试写入qrtz_node
        //3.尝试清理 qrtz_node
        //  及任务 and 执行信息
        private void preProcess(){
            log.info("=====>invoke preProcess...<=====");
            // 前置处理
            Connection conn = null;
            try{
                conn = getNonManagedTXConnection();
                // 构造对象
                String application = getInstanceName();
                String state = "Y";
                Long timePre = 0L;
                Long now = System.currentTimeMillis();
                Long timeNext = now+TIME_CHECK_INTERVAL;
                Long timeInterval = TIME_CHECK_INTERVAL;
                final String hostIP = SystemPropGenerator.hostIP();
                final String hostName = SystemPropGenerator.hostName();
                QrtzApp app = new QrtzApp(application,state,timePre,timeNext,timeInterval);
                QrtzNode node = new QrtzNode(application,hostIP,hostName,state,now); // 1天 86400_000L
                // 写入 qrtz_app、 qrtz_node、清理
                getDelegate().insertQrtzApp(conn,app);
                getDelegate().insertQrtzNode(conn,node);
//                if( (now - PRE_CLEAR_TIME) >= 86400_000L){
//                    getDelegate().clearHistoryData(conn,366*86400_000L);// 1年=1天*366
//                    PRE_CLEAR_TIME=now;// update time
//                }
                getDelegate().clearHistoryData(conn,366*86400_000L);// 1年=1天*366
                // conn.commit();
            }catch (Exception e){
                e.printStackTrace();
                cleanupConnection(conn);
            }finally {
                cleanupConnection(conn);
//                ALREADY_CHECK=true;
            }
        }

    }

}

// EOF
