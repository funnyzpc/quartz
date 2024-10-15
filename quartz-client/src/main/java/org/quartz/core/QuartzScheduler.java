
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


import org.quartz.SchedulerException;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.simpl.SystemPropGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * This is the heart of Quartz, an indirect implementation of the <code>{@link org.quartz.Scheduler}</code>
 * interface, containing methods to schedule <code>{@link org.quartz.Job}</code>s,
 * register <code>{@link org.quartz.JobListener}</code> instances, etc.
 * </p>
 * 
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzSchedulerThread
 * @see org.quartz.spi.JobStore
 * @see org.quartz.spi.ThreadPool
 * 
 * @author James House
 */
public class QuartzScheduler implements RemotableQuartzScheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private static String VERSION_MAJOR = "UNKNOWN";
    private static String VERSION_MINOR = "UNKNOWN";
    private static String VERSION_ITERATION = "UNKNOWN";

    static {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = QuartzScheduler.class.getResourceAsStream("quartz-build.properties");
            if(is != null) {
                props.load(is);
                String version = props.getProperty("version");
                if (version != null) {
                    String[] versionComponents = version.split("\\.");
                    VERSION_MAJOR = versionComponents[0];
                    VERSION_MINOR = versionComponents[1];
                    if(versionComponents.length > 2){
                        VERSION_ITERATION = versionComponents[2];
                    }
                    else{
                        VERSION_ITERATION = "0";
                    }
                } else {
                  (LoggerFactory.getLogger(QuartzScheduler.class)).error("Can't parse Quartz version from quartz-build.properties");
                }
            }
        } catch (Exception e) {
            (LoggerFactory.getLogger(QuartzScheduler.class)).error("Error loading version info from quartz-build.properties.", e);
        } finally {
            if(is != null) {
                try { is.close(); } catch(Exception ignore) {}
            }
        }
    }
    

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private QuartzSchedulerResources resources;

    private ThreadGroup threadGroup;

//    private SchedulerContext context = new SchedulerContext();

//    private ListenerManager listenerManager = new ListenerManagerImpl();

//    private HashMap<String, TriggerListener> internalTriggerListeners = new HashMap<String, TriggerListener>(10);

//    private ArrayList<SchedulerListener> internalSchedulerListeners = new ArrayList<SchedulerListener>(10);

//    ErrorLogger errLogger = null;

    private ArrayList<Object> holdToPreventGC = new ArrayList<Object>(5);

    private boolean signalOnSchedulingChange = true;

    private volatile boolean closed = false;
    private volatile boolean shuttingDown = false;
//    private boolean boundRemotely = false;

//    private QuartzSchedulerMBean jmxBean = null;
    
    private Date initialStart = null;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // private static final Map<String, ManagementServer> MGMT_SVR_BY_BIND = new
    // HashMap<String, ManagementServer>();
    // private String registeredManagementServerBind;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a <code>QuartzScheduler</code> with the given configuration
     * properties.
     * </p>
     * 
     * @see QuartzSchedulerResources
     */
    public QuartzScheduler(final DataSource dataSource, long idleWaitTime,final String tablePrefix)  {
        this.resources = new QuartzSchedulerResources(dataSource,idleWaitTime,null==tablePrefix?"QRTZ_":tablePrefix);
    }


    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    @Override
    public String getVersion() {
        return getVersionMajor() + "." + getVersionMinor() + "." + getVersionIteration();
    }

    public static String getVersionMajor() {
        return VERSION_MAJOR;
    }
    
    public static String getVersionMinor() {
        return VERSION_MINOR;
    }

    public static String getVersionIteration() {
        return VERSION_ITERATION;
    }
    public Logger getLog() {
        return log;
    }

    /**
     * <p>
     * Returns the name of the <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public String getSchedulerName() {
        return resources.getName();
    }

    /**
     * <p>
     * Returns the instance Id of the <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public String getSchedulerInstanceId() {
        return resources.getInstanceId();
    }

    /**
     * <p>
     * Returns the name of the thread group for Quartz's main threads.
     * </p>
     */
    public ThreadGroup getSchedulerThreadGroup() {
        if (threadGroup == null) {
            threadGroup = new ThreadGroup("QuartzScheduler:" + getSchedulerName());
            if (resources.getMakeSchedulerThreadDaemon()) {
                threadGroup.setDaemon(true);
            }
        }
        return threadGroup;
    }

    public void addNoGCObject(Object obj) {
        holdToPreventGC.add(obj);
    }

//    public boolean removeNoGCObject(Object obj) {
//        return holdToPreventGC.remove(obj);
//    }
//
//    /**
//     * <p>
//     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
//     * </p>
//     */
//    @Override
//    public SchedulerContext getSchedulerContext() throws SchedulerException {
//        return context;
//    }

    public boolean isSignalOnSchedulingChange() {
        return signalOnSchedulingChange;
    }

    public void setSignalOnSchedulingChange(boolean signalOnSchedulingChange) {
        this.signalOnSchedulingChange = signalOnSchedulingChange;
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduler State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public Date runningSince() {
        if(initialStart == null){
            return null;
        }
        return new Date(initialStart.getTime());
    }
//    @Override
//    public int numJobsExecuted() {
//        return jobMgr.getNumJobsFired();
//    }
//    @Override
//    public Class<?> getJobStoreClass() {
//        return resources.getJobStore().getClass();
//    }
    @Override
    public boolean supportsPersistence() {
        return resources.getJobStore().supportsPersistence();
    }
    @Override
    public boolean isClustered() {
        return resources.getJobStore().isClustered();
    }

    /**
     * <p>
     * Halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.Trigger}s</code>,
     * and cleans up all resources associated with the QuartzScheduler.
     * Equivalent to <code>shutdown(false)</code>.
     * </p>
     * 
     * <p>
     * The scheduler cannot be re-started.
     * </p>
     */
    @Override
    public void shutdown() {
        shutdown(false);
    }

    /**
     * <p>
     * Halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.Trigger}s</code>,
     * and cleans up all resources associated with the QuartzScheduler.
     * </p>
     * 
     * <p>
     * The scheduler cannot be re-started.
     * </p>
     * 
     * @param waitForJobsToComplete
     *          if <code>true</code> the scheduler will not allow this method
     *          to return until all currently executing jobs have completed.
     */
    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        if(shuttingDown || closed) {
            return;
        }
        shuttingDown = true;
//        getLog().info("Scheduler " + resources.getUniqueIdentifier() + " shutting down.");
        getLog().info("Scheduler " + SystemPropGenerator.hostIP() + " shutting down.");
        // boolean removeMgmtSvr = false;
        // if (registeredManagementServerBind != null) {
        // ManagementServer standaloneRestServer =
        // MGMT_SVR_BY_BIND.get(registeredManagementServerBind);
        //
        // try {
        // standaloneRestServer.unregister(this);
        //
        // if (!standaloneRestServer.hasRegistered()) {
        // removeMgmtSvr = true;
        // standaloneRestServer.stop();
        // }
        // } catch (Exception e) {
        // getLog().warn("Failed to shutdown the ManagementRESTService", e);
        // } finally {
        // if (removeMgmtSvr) {
        // MGMT_SVR_BY_BIND.remove(registeredManagementServerBind);
        // }
        //
        // registeredManagementServerBind = null;
        // }
        // }
//        standby();
//        schedThread.halt(waitForJobsToComplete);
////        notifySchedulerListenersShuttingdown();
//        if( (resources.isInterruptJobsOnShutdown() && !waitForJobsToComplete) ||
//                (resources.isInterruptJobsOnShutdownWithWait() && waitForJobsToComplete)) {
//            List<JobExecutionContext> jobs = getCurrentlyExecutingJobs();
//            for(JobExecutionContext job: jobs) {
//                if(job.getJobInstance() instanceof InterruptableJob){
//                    try {
//                        ((InterruptableJob)job.getJobInstance()).interrupt();
//                    } catch (Throwable e) {
//                        e.printStackTrace();
//                        // do nothing, this was just a courtesy effort
////                        getLog().warn("Encountered error when interrupting job {} during shutdown: {}", job.getJobDetail().getKey(), e);
//                    }
//                }
//            }
//        }
//
//        resources.getThreadPool().shutdown(waitForJobsToComplete);
//        closed = true;
////        if (resources.getJMXExport()) {
////            try {
////                unregisterJMX();
////            } catch (Exception e) {
////            }
////        }
//
////        if(boundRemotely) {
////            try {
////                unBind();
////            } catch (RemoteException re) {
////            }
////        }
//
//        shutdownPlugins();
//        resources.getJobStore().shutdown();
////        notifySchedulerListenersShutdown();
//        SchedulerRepository.getInstance().remove(resources.getName());
//        holdToPreventGC.clear();
//        getLog().info("Scheduler " + resources.getUniqueIdentifier() + " shutdown complete.");
    }

    /**
     * <p>
     * Reports whether the <code>Scheduler</code> has been shutdown.
     * </p>
     */
    @Override
    public boolean isShutdown() {
        return closed;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    
    public void validateState() throws SchedulerException {
        if (isShutdown()) {
            throw new SchedulerException("The Scheduler has been shutdown.");
        }
        // other conditions to check (?)
    }


    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////


    /**
     * <p>
     * Pause all triggers - equivalent of calling <code>pauseTriggers(GroupMatcher<TriggerKey>)</code>
     * with a matcher matching all known groups.
     * </p>
     * 
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     * 
     * @see #resumeAll()
//     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     * @see #standby()
     */
    public void pauseAll() throws SchedulerException {
        validateState();
//        resources.getJobStore().pauseAll();
//        // 發送郵件消息
//        notifySchedulerListenersPausedTriggers(null);
    }



    @Override
    public String[] getDBInfo() {
        return resources.getJobStore().getDBInfo();
    }
    @Override
    public List<QrtzApp> getAllApp(){
        return resources.getJobStore().getAllApp();
    }
    @Override
    public QrtzApp getAppByApplication(String application){
        return resources.getJobStore().getAppByApplication(application);
    }
    @Override
    public List<QrtzNode> getNodeByApp(String application){
        return resources.getJobStore().getNodeByApp(application);
    }
    @Override
    public QrtzJob getJobByJobId(String job_id){
        return resources.getJobStore().getJobByJobId(job_id);
    }
    @Override
    public QrtzExecute getExecuteByExecuteId(String execute_id){
        return resources.getJobStore().getExecuteByExecuteId(execute_id);
    }
    @Override
    public List<QrtzExecute> getExecuteByJobId(String job_id){
        return resources.getJobStore().getExecuteByJobId(job_id);
    }
    @Override
    public QrtzJob getJobInAllByJobId(String job_id){
        return resources.getJobStore().getJobInAllByJobId(job_id);
    }
    @Override
    public QrtzExecute getExecuteInAllByExecuteId(String execute_id){
        return resources.getJobStore().getExecuteInAllByExecuteId(execute_id);
    }
    @Override
    public int addApp(QrtzApp qrtzApp){
        return resources.getJobStore().addApp(qrtzApp);
    }
    @Override
    public int deleteApp(String application){
        return resources.getJobStore().deleteApp(application);
    }
    @Override
    public int updateAppState( String application, String state){
        return resources.getJobStore().updateAppState(application,state);
    }
    @Override
    public int addNode(QrtzNode qrtzNode){
        return resources.getJobStore().addNode(qrtzNode);
    }
    @Override
    public boolean containsNode(String application ,String hostIP){
        return resources.getJobStore().containsNode(application,hostIP);
    }
    @Override
    public int deleteNode(String application,String hostIP){
        return resources.getJobStore().deleteNode(application,hostIP);
    }
    @Override
    public int updateNodeState(QrtzNode qrtzNode){
        return resources.getJobStore().updateNodeState(qrtzNode);
    }
    @Override
    public int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode){
        return resources.getJobStore().addAppAndNode(qrtzApp,qrtzNode);
    }

    ///////////////////////////////////////
    @Override
    public int addJob(QrtzJob qrtzJob) {
        return resources.getJobStore().addJob(qrtzJob);
    }
    @Override
    public int updateJob(QrtzJob qrtzJob) {
        return resources.getJobStore().updateJob(qrtzJob);
    }
    @Override
    public int deleteJob(Long job_id) {
        return resources.getJobStore().deleteJob(job_id);
    }
    @Override
    public int findQrtzExecuteCountById(Long job_id){
        return resources.getJobStore().findQrtzExecuteCountById(job_id);
    }
    @Override
    public int updateExecuteStateByJobId(Long job_id,String state){
        return resources.getJobStore().updateExecuteStateByJobId(job_id,state);
    }
    @Override
    public int updateExecuteStateByExecuteId( Long execute_id,String state){
        return resources.getJobStore().updateExecuteStateByExecuteId(execute_id,state);
    }
    @Override
    public int addExecute(QrtzExecute qrtzExecute){
        return resources.getJobStore().addExecute(qrtzExecute);
    }
    @Override
    public int deleteExecute(Long execute_id ){
        return resources.getJobStore().deleteExecute(execute_id);
    }



}


