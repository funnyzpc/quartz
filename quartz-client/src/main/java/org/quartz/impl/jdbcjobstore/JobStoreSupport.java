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


import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 * <p>
 * Contains base functionality for JDBC-based JobStore implementations.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 */
public final class JobStoreSupport implements JobStore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private final DataSource dataSource;

    protected String dsName;

    private String tablePrefix = "QRTZ_";

    protected boolean useProperties = false;

    protected String instanceId;

    protected String application;

    protected String delegateClassName;

    protected Class<? extends DriverDelegate> delegateClass = StdJDBCDelegate.class;

    private DriverDelegate delegate;

    private long misfireThreshold = 60000L; // one minute

    private boolean dontSetAutoCommitFalse = false;

    private boolean isClustered = false;

    private boolean useDBLocks = true;
    
    private boolean lockOnInsert = true;

//    private Semaphore lockHandler = null; // set in initialize() method...

    /**
     * 群集签入间隔
     */
    private long clusterCheckinInterval = 7500L;

//    private ClusterManager clusterManagementThread = null;

//    private MisfireHandler misfireHandler = null;

    protected int maxToRecoverAtATime = 20;
    
    private boolean setTxIsolationLevelSequential = false;
    
    private boolean acquireTriggersWithinLock = false;
    
    private long dbRetryInterval = 15000L; // 15 secs
    
    private boolean makeThreadsDaemons = false;

    private boolean threadsInheritInitializersClassLoadContext = false;
    private ClassLoader initializersLoader = null;
    
    private boolean doubleCheckLockMisfireHandler = true;
    
    private final Logger log = LoggerFactory.getLogger(JobStoreSupport.class);


    private volatile boolean schedulerRunning = false;
    private volatile boolean shutdown = false;

    public JobStoreSupport(DataSource dataSource,final long idleWaitTime,final String tablePrefix) {
        this.dataSource = dataSource;
        this.tablePrefix=tablePrefix;
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
     * Get the name of the <code>DataSource</code> that should be used for
     * performing database functions.
     * </p>
     */
    public String getDataSource() {
        return dsName;
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

    @Override
    public void schedulerPaused() {
        schedulerRunning = false;
    }
    @Override
    public void schedulerResumed() {
        schedulerRunning = true;
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

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
    
    private Connection getConnection() throws JobPersistenceException {
        Connection conn;
        try {
            if(null==this.dataSource ){
                // 需要在构造函数内赋值
                throw new JobPersistenceException("dataSource is not init!");
            }
             conn = this.dataSource.getConnection();
        }catch (Throwable e) {
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
//                        delegateClass = getClassLoadHelper().loadClass(delegateClassName, DriverDelegate.class);
                        delegateClass = (Class<? extends DriverDelegate>) Class.forName(delegateClassName);
                    }
                    delegate = delegateClass.newInstance();
                    delegate.initialize( tablePrefix,application,canUseProperties());
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

    //---------------------------------------------------------------------------
    // Cluster management methods 集群管理方法
    //---------------------------------------------------------------------------

//    protected boolean firstCheckIn = true;

//    protected long lastCheckin = System.currentTimeMillis();

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


    @Override
    public String[] getDBInfo() {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getDBInfo(conn);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public List<QrtzApp> getAllApp() {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getAllApp(conn);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public QrtzApp getAppByApplication( String application){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getAppByApplication(conn,application);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public List<QrtzNode> getNodeByApp(String application) {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getNodeByApp(conn,application);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public QrtzJob getJobByJobId(String job_id) {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getJobByJobId(conn,job_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public QrtzExecute getExecuteByExecuteId( String execute_id) {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getExecuteByExecuteId(conn,execute_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public List<QrtzExecute> getExecuteByJobId(String job_id){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getExecuteByJobId(conn,job_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public QrtzJob getJobInAllByJobId(String job_id){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getJobInAllByJobId(conn,job_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public QrtzExecute getExecuteInAllByExecuteId(String execute_id){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().getExecuteInAllByExecuteId(conn,execute_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return null;
    }
    @Override
    public int addApp(QrtzApp qrtzApp){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().addApp(conn,qrtzApp);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int deleteApp(String application){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().deleteApp(conn,application);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int updateAppState(String application,String state){
        Connection conn =  null ;
        try{
            conn = getConnection();
            conn.setAutoCommit(false);
            int ct=0;
            if( (ct=getDelegate().updateAppState(conn,application,state))<1 || getDelegate().updateNodeStateBatch(conn,application,state)<1 ){
                conn.rollback();
            }
            return ct;
        }catch (Exception e){
            if( null!=conn ){
                try {
                    conn.rollback();
                } catch (SQLException ex) {

                }
            }
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int addNode(QrtzNode qrtzNode){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().addNode(conn,qrtzNode);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public boolean containsNode(String application ,String hostIP){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().containsNode(conn,application,hostIP);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return Boolean.FALSE;
    }
    @Override
    public boolean containsNode(String application){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().containsNode(conn,application);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return Boolean.FALSE;
    }
    @Override
    public int deleteNode(String application,String hostIP){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().deleteNode(conn,application,hostIP);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int updateNodeState(QrtzNode qrtzNode){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().updateNodeState(conn,qrtzNode);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int updateNode(QrtzNode qrtzNode){
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().updateNode(conn,qrtzNode);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }

    @Override
    public int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode){
        Connection conn =  null ;
        try{
            conn = getConnection();
            conn.setAutoCommit(false);
            int ct = getDelegate().addAppAndNode(conn,qrtzApp,qrtzNode);
            if( ct==0){
                conn.rollback();
            }else{
                conn.commit();
            }
            return ct;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }


    ///////////////////////////////////////////////////
    @Override
    public int addJob(final QrtzJob qrtzJob)   {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().addJob(conn,qrtzJob);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }

    @Override
    public int updateJob(final QrtzJob qrtzJob)   {
        Connection conn =  null ;
        try{
            conn = getConnection();
            return getDelegate().updateJob(conn,qrtzJob);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int deleteJob(final Long job_id)   {
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            if(getDelegate().findQrtzExecuteCountById(conn,job_id)>0){
                throw new SchedulerException("存在execute记录，请先移除后再行删除job!");
            }
            return getDelegate().deleteJob(conn,job_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int findQrtzExecuteCountById(Long job_id){
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            return getDelegate().findQrtzExecuteCountById(conn,job_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public boolean containsExecute(Long job_id){
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            return getDelegate().containsExecute(conn,job_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return Boolean.FALSE;
    }
    @Override
    public int updateExecuteStateByJobId(Long job_id,String state) {
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            return getDelegate().updateExecuteStateByJobId(conn,job_id,state);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int updateExecuteStateByExecuteId(Long execute_id,String state) {
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            return getDelegate().updateExecuteStateByExecuteId(conn,execute_id,state);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int addExecute(QrtzExecute qrtzExecute) {
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            return getDelegate().addExecute(conn,qrtzExecute);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }
    @Override
    public int deleteExecute(String execute_id ){
        Connection conn =  null ;
        try{
            conn = getConnection();
            // 先查询execute，如果有execute存在则不可删除
            return getDelegate().deleteExecute(conn,execute_id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            closeConnection(conn);
        }
        return 0;
    }




}

// EOF
