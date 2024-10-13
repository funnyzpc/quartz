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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.quartz.Job;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.SystemPropGenerator;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.DBConnectionManager;
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
public abstract class JobStoreSupport implements JobStore/*, Constants*/ {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

//    protected static final String LOCK_TRIGGER_ACCESS = "TRIGGER_ACCESS";
//
//    protected static final String LOCK_STATE_ACCESS = "STATE_ACCESS";

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected String dsName;

    protected String tablePrefix = "QRTZ_";

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

    private boolean useDBLocks = true;
    
    private boolean lockOnInsert = true;

//    private Semaphore lockHandler = null; // set in initialize() method...

//    private String selectWithLockSQL = null;
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
//        // If the user hasn't specified an explicit lock handler, then
//        // choose one based on CMT/Clustered/UseDBLocks.
//        // 如果用户没有指定显式的锁处理程序，则根据CMT/Clustered/UseDBLocks选择一个。
//        if (getLockHandler() == null) {
//            // If the user hasn't specified an explicit lock handler,
//            // then we *must* use DB locks with clustering
//            if (isClustered()) {
//                setUseDBLocks(true);
//            }
//            if (getUseDBLocks()) {
//                if(getDriverDelegateClass() != null && getDriverDelegateClass().equals(MSSQLDelegate.class.getName())) {
//                    if(getSelectWithLockSQL() == null) {
//                        String msSqlDflt = "SELECT * FROM {0}LOCKS WITH (UPDLOCK,ROWLOCK) WHERE " + COL_SCHEDULER_NAME + " = {1} AND LOCK_NAME = ?";
//                        getLog().info("Detected usage of MSSQLDelegate class - defaulting 'selectWithLockSQL' to '" + msSqlDflt + "'.");
//                        setSelectWithLockSQL(msSqlDflt);
//                    }
//                }
//                getLog().info("Using db table-based data access locking (synchronization).");
//                setLockHandler(new StdRowLockSemaphore(getTablePrefix(), getInstanceName(), getSelectWithLockSQL()));
////                setLockHandler(new UpdateLockRowSemaphore(getTablePrefix(), getInstanceName(), getSelectWithLockSQL()));
//            } else {
//                getLog().info("Using thread monitor-based data access locking (synchronization).");
//                setLockHandler(new SimpleSemaphore());
//            }
//        }
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
                List<QrtzExecute> dataList = getDelegate().selectExecuteAndJobToAcquire(conn,application,_tsw,_tew,"EXECUTING");
                // No trigger is ready to fire yet. 触发器还没有准备好点火
                if (dataList == null || dataList.isEmpty()){
                    return executeList;
                }
                // 打乱顺序可能在集群环境下有执行优势
                Collections.shuffle(dataList);
                for( QrtzExecute item:dataList){
                    final String jobType = item.getJobType(); // SIMPLE、CRON
                    final Long endTime = item.getEndTime();
                    final Integer repeatCount = item.getRepeatCount();
                    item.setHostIp(hostIP);
                    item.setHostName(hostName);
//                    item.setTimeTriggered( ("SIMPLE".equals(jobType))? (item.getTimeTriggered()+1) : item.getTimeTriggered());
                    // timeTriggered 是与 repeatCount 绑定的，有指定重复次数才会更新已触发次数
                    item.setTimeTriggered( repeatCount!=null && repeatCount>0 ? (item.getTimeTriggered()+1) : item.getTimeTriggered() );
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
                            // Integer repeatCount = item.getRepeatCount();
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

//    protected Semaphore getLockHandler() {
//        return lockHandler;
//    }
//
//    public void setLockHandler(Semaphore lockHandler) {
//        this.lockHandler = lockHandler;
//    }
//
//    protected void signalSchedulingChangeImmediately(long candidateNewNextFireTime) {
//        schedSignaler.signalSchedulingChange(candidateNewNextFireTime);
//    }

    //---------------------------------------------------------------------------
    // Cluster management methods 集群管理方法
    //---------------------------------------------------------------------------

//    protected boolean firstCheckIn = true;

//    protected long lastCheckin = System.currentTimeMillis();


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
                    boolean hasComplete = false;
                    for( QrtzExecute execute:executes ){
                        final String state = execute.getState();
                        if("EXECUTING".equals(state)){
                            hasExecuting=true;
                        }else if("PAUSED".equals(state)){
                            hasPaused=true;
                        }else if("ERROR".equals(state)){
                            hasError=true;
                        }else if("COMPLETE".equals(state)){
                            hasComplete=true;
                        }else{
                            continue; // 这里一般是INIT
                        }
                    }
                    // 如果所有状态都有则按以下优先级来
                    String beforeState = job.getState();
                    if(hasError){
                        job.setState("ERROR");
                    }else if(hasExecuting){
                        job.setState("EXECUTING");
                    }else if(hasPaused){
                        job.setState("PAUSED");
                    }else if(hasComplete){
                        job.setState("COMPLETE");
                    }else{
                        continue; // 这里对应上面的INIT状态，不做处理
                    }
                    // 不做无谓的更新...
                    if(!job.getState().equals(beforeState)){
                        job.setUpdateTime(now);
                        getDelegate().updateRecoverJob(conn,job);
                    }
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
            /***** try end... *****/

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
                // SELECT * FROM QRTZ_EXECUTE WHERE PID=? AND NEXT_FIRE_TIME<=? AND STATE!=COMPLETE AND STATE!=INIT AND STATE!=PAUSED
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
                        }else if( execute.getRepeatCount()>0 && execute.getTimeTriggered()>=execute.getRepeatCount() ){
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
