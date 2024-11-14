
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

import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.impl.SchedulerRepository;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.simpl.SystemPropGenerator;
import org.quartz.spi.JobFactory;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.SchedulerSignaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private QuartzSchedulerThread schedThread;

    private ThreadGroup threadGroup;

//    private SchedulerContext context = new SchedulerContext();

//    private ListenerManager listenerManager = new ListenerManagerImpl();
    
    private HashMap<String, JobListener> internalJobListeners = new HashMap<String, JobListener>(10);

//    private HashMap<String, TriggerListener> internalTriggerListeners = new HashMap<String, TriggerListener>(10);

//    private ArrayList<SchedulerListener> internalSchedulerListeners = new ArrayList<SchedulerListener>(10);

    private JobFactory jobFactory = new PropertySettingJobFactory();
    
    ExecutingJobsManager jobMgr = null;

//    ErrorLogger errLogger = null;

    private SchedulerSignaler signaler;

    private Random random = new Random();

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
    public QuartzScheduler(QuartzSchedulerResources resources, long idleWaitTime, @Deprecated long dbRetryInterval) throws SchedulerException {
        this.resources = resources;
        if (resources.getJobStore() instanceof JobListener) {
            addInternalJobListener((JobListener)resources.getJobStore());
        }
        this.schedThread = new QuartzSchedulerThread(this, resources);
//        ThreadExecutor schedThreadExecutor = resources.getThreadExecutor();
//        schedThreadExecutor.execute(this.schedThread); // 启动
//        this.schedThread.start();
        // 空闲等待时间，默认是-1
        if (idleWaitTime > 0) {
            this.schedThread.setIdleWaitTime(idleWaitTime);
        }
        jobMgr = new ExecutingJobsManager();
        addInternalJobListener(jobMgr);
//        errLogger = new ErrorLogger();
//        addInternalSchedulerListener(errLogger);
        signaler = new SchedulerSignalerImpl(this, this.schedThread);
        getLog().info("Quartz Scheduler v." + getVersion() + " created.");
    }

//    public void initialize() throws SchedulerException {
//        try {
//            bind();
//        } catch (Exception re) {
//            throw new SchedulerException("Unable to bind scheduler to RMI Registry.", re);
//        }
//        if (resources.getJMXExport()) {
//            try {
//                registerJMX();
//            } catch (Exception e) {
//                throw new SchedulerException("Unable to register scheduler with MBeanServer.", e);
//            }
//        }
//
//        // ManagementRESTServiceConfiguration managementRESTServiceConfiguration
//        // = resources.getManagementRESTServiceConfiguration();
//        //
//        // if (managementRESTServiceConfiguration != null &&
//        // managementRESTServiceConfiguration.isEnabled()) {
//        // try {
//        // /**
//        // * ManagementServer will only be instantiated and started if one
//        // * isn't already running on the configured port for this class
//        // * loader space.
//        // */
//        // synchronized (QuartzScheduler.class) {
//        // if
//        // (!MGMT_SVR_BY_BIND.containsKey(managementRESTServiceConfiguration.getBind()))
//        // {
//        // Class<?> managementServerImplClass =
//        // Class.forName("org.quartz.management.ManagementServerImpl");
//        // Class<?> managementRESTServiceConfigurationClass[] = new Class[] {
//        // managementRESTServiceConfiguration.getClass() };
//        // Constructor<?> managementRESTServiceConfigurationConstructor =
//        // managementServerImplClass
//        // .getConstructor(managementRESTServiceConfigurationClass);
//        // Object arglist[] = new Object[] { managementRESTServiceConfiguration
//        // };
//        // ManagementServer embeddedRESTServer = ((ManagementServer)
//        // managementRESTServiceConfigurationConstructor.newInstance(arglist));
//        // embeddedRESTServer.start();
//        // MGMT_SVR_BY_BIND.put(managementRESTServiceConfiguration.getBind(),
//        // embeddedRESTServer);
//        // }
//        // registeredManagementServerBind =
//        // managementRESTServiceConfiguration.getBind();
//        // ManagementServer embeddedRESTServer =
//        // MGMT_SVR_BY_BIND.get(registeredManagementServerBind);
//        // embeddedRESTServer.register(this);
//        // }
//        // } catch (Exception e) {
//        // throw new
//        // SchedulerException("Unable to start the scheduler management REST service",
//        // e);
//        // }
//        // }
//
//        getLog().info("Scheduler meta-data: " +
//                (new SchedulerMetaData(getSchedulerName(),
//                        getSchedulerInstanceId(), getClass(), boundRemotely, runningSince() != null,
//                        isInStandbyMode(), isShutdown(), runningSince(),
//                        numJobsExecuted(), getJobStoreClass(),
//                        supportsPersistence(), isClustered(), getThreadPoolClass(),
//                        getThreadPoolSize(), getVersion())).toString());
//    }
    
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

    public SchedulerSignaler getSchedulerSignaler() {
        return signaler;
    }

    public Logger getLog() {
        return log;
    }
    
//    /**
//     * Register the scheduler in the local MBeanServer.
//     */
//    private void registerJMX() throws Exception {
//        String jmxObjectName = resources.getJMXObjectName();
//        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
//        jmxBean = new QuartzSchedulerMBeanImpl(this);
//        mbs.registerMBean(jmxBean, new ObjectName(jmxObjectName));
//    }
//
//    /**
//     * Unregister the scheduler from the local MBeanServer.
//     */
//    private void unregisterJMX() throws Exception {
//        String jmxObjectName = resources.getJMXObjectName();
//        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
//        mbs.unregisterMBean(new ObjectName(jmxObjectName));
//        jmxBean.setSampledStatisticsEnabled(false);
//        getLog().info("Scheduler unregistered from name '" + jmxObjectName + "' in the local MBeanServer.");
//    }

//    /**
//     * <p>
//     * Bind the scheduler to an RMI registry.
//     * </p>
//     */
//    private void bind() throws RemoteException {
//        String host = resources.getRMIRegistryHost();
//        // don't export if we're not configured to do so...
//        if (host == null || host.length() == 0) {
//            return;
//        }
//        RemotableQuartzScheduler exportable = null;
//        if(resources.getRMIServerPort() > 0) {
//            exportable = (RemotableQuartzScheduler) UnicastRemoteObject.exportObject(this, resources.getRMIServerPort());
//        } else {
//            exportable = (RemotableQuartzScheduler) UnicastRemoteObject.exportObject(this);
//        }
//        Registry registry = null;
//        if (resources.getRMICreateRegistryStrategy().equals(
//                QuartzSchedulerResources.CREATE_REGISTRY_AS_NEEDED)) {
//            try {
//                // First try to get an existing one, instead of creating it,
//                // since if
//                // we're in a web-app being 'hot' re-depoloyed, then the JVM
//                // still
//                // has the registry that we created above the first time...
//                registry = LocateRegistry.getRegistry(resources.getRMIRegistryPort());
//                registry.list();
//            } catch (Exception e) {
//                registry = LocateRegistry.createRegistry(resources.getRMIRegistryPort());
//            }
//        } else if (resources.getRMICreateRegistryStrategy().equals(
//                QuartzSchedulerResources.CREATE_REGISTRY_ALWAYS)) {
//            try {
//                registry = LocateRegistry.createRegistry(resources.getRMIRegistryPort());
//            } catch (Exception e) {
//                // Fall back to an existing one, instead of creating it, since
//                // if
//                // we're in a web-app being 'hot' re-depoloyed, then the JVM
//                // still
//                // has the registry that we created above the first time...
//                registry = LocateRegistry.getRegistry(resources.getRMIRegistryPort());
//            }
//        } else {
//            registry = LocateRegistry.getRegistry(resources.getRMIRegistryHost(), resources.getRMIRegistryPort());
//        }
//        String bindName = resources.getRMIBindName();
//        registry.rebind(bindName, exportable);
//        boundRemotely = true;
//        getLog().info("Scheduler bound to RMI registry under name '" + bindName + "'");
//    }

//    /**
//     * <p>
//     * Un-bind the scheduler from an RMI registry.
//     * </p>
//     */
//    private void unBind() throws RemoteException {
//        String host = resources.getRMIRegistryHost();
//        // don't un-export if we're not configured to do so...
//        if (host == null || host.length() == 0) {
//            return;
//        }
//        Registry registry = LocateRegistry.getRegistry(resources.getRMIRegistryHost(), resources.getRMIRegistryPort());
//        String bindName = resources.getRMIBindName();
//        try {
//            registry.unbind(bindName);
//            UnicastRemoteObject.unexportObject(this, true);
//        } catch (java.rmi.NotBoundException nbe) {
//        }
//        getLog().info("Scheduler un-bound from name '" + bindName + "' in RMI registry");
//    }

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

    /**
     * <p>
     * Starts the <code>QuartzScheduler</code>'s threads that fire <code>{@link org.quartz.Trigger}s</code>.
     * </p>
     * 
     * <p>
     * All <code>{@link org.quartz.Trigger}s</code> that have misfired will
     * be passed to the appropriate TriggerListener(s).
     * </p>
     */
    @Override
    public void start() throws SchedulerException {
        if (shuttingDown|| closed) {
            throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
        }
        // QTZ-212 : calling new schedulerStarting() method on the listeners
        // right after entering start()
//        notifySchedulerListenersStarting();
        if (initialStart == null) {
            initialStart = new Date();
            // 这里是保证 clusterMisfireHandler.preProcess() 优先执行
            this.resources.getJobStore().schedulerStarted();
            // 任务扫描
            this.schedThread.start();
            startPlugins();
        } else {
            resources.getJobStore().schedulerResumed();
        }
        schedThread.togglePause(false);
//        getLog().info("Scheduler " + resources.getUniqueIdentifier() + " started.");
        getLog().info("Scheduler " + SystemPropGenerator.hostIP() + " started.");
//        notifySchedulerListenersStarted();
    }
    @Override
    public void startDelayed(final int seconds) throws SchedulerException
    {
        if (shuttingDown || closed) {
            throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(seconds * 1000L); }
                catch(InterruptedException ignore) {}
                try { start(); }
                catch(SchedulerException se) {
                    getLog().error("Unable to start scheduler after startup delay.", se);
                }
            }
        });
        t.start();
    }

    /**
     * <p>
     * Temporarily halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.Trigger}s</code>.
     * </p>
     * 
     * <p>
     * The scheduler is not destroyed, and can be re-started at any time.
     * </p>
     */
    @Override
    public void standby() {
        resources.getJobStore().schedulerPaused();
        schedThread.togglePause(true);
//        getLog().info("Scheduler " + resources.getUniqueIdentifier() + " paused.");
        getLog().info("Scheduler " + SystemPropGenerator.hostIP() + " paused.");
//        notifySchedulerListenersInStandbyMode();
    }

    /**
     * <p>
     * Reports whether the <code>Scheduler</code> is paused.
     * </p>
     */
    @Override
    public boolean isInStandbyMode() {
        return schedThread.isPaused();
    }
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
    @Override
    public Class<?> getThreadPoolClass() {
        return resources.getThreadPool().getClass();
    }
    @Override
    public int getThreadPoolSize() {
        return resources.getThreadPool().getPoolSize();
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
        standby();
        schedThread.halt(waitForJobsToComplete);
//        notifySchedulerListenersShuttingdown();
        if( (resources.isInterruptJobsOnShutdown() && !waitForJobsToComplete) || 
                (resources.isInterruptJobsOnShutdownWithWait() && waitForJobsToComplete)) {
            List<JobExecutionContext> jobs = getCurrentlyExecutingJobs();
            for(JobExecutionContext job: jobs) {
                if(job.getJobInstance() instanceof InterruptableJob){
                    try {
                        ((InterruptableJob)job.getJobInstance()).interrupt();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        // do nothing, this was just a courtesy effort
//                        getLog().warn("Encountered error when interrupting job {} during shutdown: {}", job.getJobDetail().getKey(), e);
                    }
                }
            }
        }
        
        resources.getThreadPool().shutdown(waitForJobsToComplete);
        closed = true;
//        if (resources.getJMXExport()) {
//            try {
//                unregisterJMX();
//            } catch (Exception e) {
//            }
//        }

//        if(boundRemotely) {
//            try {
//                unBind();
//            } catch (RemoteException re) {
//            }
//        }
        
        shutdownPlugins();
        resources.getJobStore().shutdown();
//        notifySchedulerListenersShutdown();
        SchedulerRepository.getInstance().remove(resources.getName());
        holdToPreventGC.clear();
        getLog().info("Scheduler " + resources.getUniqueIdentifier() + " shutdown complete.");
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

    public boolean isStarted() {
        return !shuttingDown && !closed && !isInStandbyMode() && initialStart != null;
    }
    
    public void validateState() throws SchedulerException {
        if (isShutdown()) {
            throw new SchedulerException("The Scheduler has been shutdown.");
        }
        // other conditions to check (?)
    }

    /**
     * <p>
     * Return a list of <code>JobExecutionContext</code> objects that
     * represent all currently executing Jobs in this Scheduler instance.
     * </p>
     * 
     * <p>
     * This method is not cluster aware.  That is, it will only return Jobs
     * currently executing in this Scheduler instance, not across the entire
     * cluster.
     * </p>
     * 
     * <p>
     * Note that the list returned is an 'instantaneous' snap-shot, and that as
     * soon as it's returned, the true list of executing jobs may be different.
     * </p>
     */
    @Override
    public List<JobExecutionContext> getCurrentlyExecutingJobs() {
        return jobMgr.getExecutingJobs();
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Pause all of the <code>{@link org.quartz.JobDetail}s</code> in the
     * matching groups - by pausing all of their <code>Trigger</code>s.
     * triggerName可以为空
     * </p>
     *  
     */
    @Override
    public void pauseJobs(final String triggerName) throws SchedulerException {
        validateState();
//        if(groupMatcher == null) {
//            groupMatcher = GroupMatcher.groupEquals(Scheduler.DEFAULT_GROUP);
//        }
//        Collection<String> pausedGroups = resources.getJobStore().pauseJobs(triggerName);
        notifySchedulerThread(0L);
//        for (String pausedGroup : pausedGroups) {
//            notifySchedulerListenersPausedJobs(pausedGroup);
//        }
    }
//
//    /**
//     * <p>
//     * Resume (un-pause) the <code>{@link Trigger}</code> with the given
//     * name.
//     * </p>
//     *
//     * <p>
//     * If the <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     * </p>
//     *
//     */
//    @Override
//    public void resumeTrigger(Key triggerKey) throws SchedulerException {
//        validateState();
//        resources.getJobStore().resumeTrigger(triggerKey);
//        notifySchedulerThread(0L);
////        notifySchedulerListenersResumedTrigger(triggerKey);
//    }

//    /**
//     * <p>
//     * Resume (un-pause) all of the <code>{@link Trigger}s</code> in the
//     * matching groups.
//     * </p>
//     *
//     * <p>
//     * If any <code>Trigger</code> missed one or more fire-times, then the
//     * <code>Trigger</code>'s misfire instruction will be applied.
//     * </p>
//     *
//     */
//    @Override
//    public void resumeTriggers(Key key) throws SchedulerException {
//        validateState();
////        if(matcher == null) {
////            matcher = GroupMatcher.groupEquals(Scheduler.DEFAULT_GROUP);
////        }
//        Collection<String> pausedGroups = resources.getJobStore().resumeTriggers(matcher);
//        notifySchedulerThread(0L);
//        for (String pausedGroup : pausedGroups) {
//            notifySchedulerListenersResumedTriggers(pausedGroup);
//        }
//    }
//    @Override
//    public Set<String> getPausedTriggerGroups() throws SchedulerException {
//        return resources.getJobStore().getPausedTriggerGroups();
//    }
//
//    /**
//     * <p>
//     * Resume (un-pause) the <code>{@link org.quartz.JobDetail}</code> with
//     * the given name.
//     * </p>
//     *
//     * <p>
//     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
//     * or more fire-times, then the <code>Trigger</code>'s misfire
//     * instruction will be applied.
//     * </p>
//     *
//     */
//    @Override
//    public void resumeJob(Key key) throws SchedulerException {
//        validateState();
//        resources.getJobStore().resumeJob(key);
//        notifySchedulerThread(0L);
////        notifySchedulerListenersResumedJob(key);
//    }

    /**
     * <p>
     * Resume (un-pause) all of the <code>{@link org.quartz.JobDetail}s</code>
     * in the matching groups.
     * </p>
     * 
     * <p>
     * If any of the <code>Job</code> s had <code>Trigger</code> s that
     * missed one or more fire-times, then the <code>Trigger</code>'s
     * misfire instruction will be applied.
     * </p>
     *  
     */
    @Override
    public void resumeJobs(final String triggerName) throws SchedulerException {
        validateState();
//        if(matcher == null) {
//            matcher = GroupMatcher.groupEquals(Scheduler.DEFAULT_GROUP);
//        }
//        Collection<String> resumedGroups = resources.getJobStore().resumeJobs(triggerName);
        notifySchedulerThread(0L);
//        for (String pausedGroup : resumedGroups) {
//            notifySchedulerListenersResumedJobs(pausedGroup);
//        }
    }

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
        // 喚醒/通知其他任務
        notifySchedulerThread(0L);
//        // 發送郵件消息
//        notifySchedulerListenersPausedTriggers(null);
    }

    /**
     * <p>
     * Resume (un-pause) all triggers - equivalent of calling <code>resumeTriggerGroup(group)</code>
     * on every group.
     * </p>
     * 
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * 
     * @see #pauseAll()
     */
    @Override
    public void resumeAll() throws SchedulerException {
        validateState();
//        resources.getJobStore().resumeAll();
        notifySchedulerThread(0L);
//        notifySchedulerListenersResumedTrigger(null);
    }

//    /**
//     * <p>
//     * Get the names of all known <code>{@link org.quartz.Job}</code> groups.
//     * </p>
//     */
//    @Deprecated
//    @Override
//    public List<String> getJobGroupNames() throws SchedulerException {
//        validateState();
//        return resources.getJobStore().getJobGroupNames();
//    }
//
//    /**
//     * <p>
//     * Get all <code>{@link Trigger}</code> s that are associated with the
//     * identified <code>{@link org.quartz.JobDetail}</code>.
//     * </p>
//     */
//    @Override
//    public List<? extends Trigger> getTriggersOfJob(Key jobKey) throws SchedulerException {
//        validateState();
//        return resources.getJobStore().getTriggersForJob(jobKey);
//    }

//    /**
//     * <p>
//     * Get the names of all known <code>{@link org.quartz.Trigger}</code>
//     * groups.
//     * </p>
//     */
//    public List<String> getTriggerGroupNames()
//        throws SchedulerException {
//        validateState();
//
//        return resources.getJobStore().getTriggerGroupNames();
//    }
//
//    /**
//     * <p>
//     * Get the names of all the <code>{@link org.quartz.Trigger}s</code> in
//     * the matching groups.
//     * </p>
//     */
//    @Override
//    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
//        validateState();
//        if(matcher == null) {
////            matcher = GroupMatcher.groupEquals(Scheduler.DEFAULT_GROUP);
//            matcher = GroupMatcher.groupEquals(null);
//        }
//        return resources.getJobStore().getTriggerKeys(matcher);
//    }
//
//    /**
//     * <p>
//     * Get the <code>{@link JobDetail}</code> for the <code>Job</code>
//     * instance with the given name and group.
//     * </p>
//     */
//    @Override
//    public JobDetail getJobDetail(Key jobKey) throws SchedulerException {
//        validateState();
//        return resources.getJobStore().retrieveJob(jobKey);
//    }
//
//    /**
//     * <p>
//     * Get the <code>{@link Trigger}</code> instance with the given name and
//     * group.
//     * </p>
//     */
//    @Override
//    public Trigger getTrigger(Key triggerKey) throws SchedulerException {
//        validateState();
//        return resources.getJobStore().retrieveTrigger(triggerKey);
//    }
    
//    /**
//     * Clears (deletes!) all scheduling data - all {@link Job}s, {@link Trigger}s
//     * {@link Calendar}s.
//     *
//     * @throws SchedulerException
//     */
//    @Override
//    public void clear() throws SchedulerException {
//        validateState();
//        resources.getJobStore().clearAllSchedulingData();
////        notifySchedulerListenersUnscheduled(null);
//    }
//    @Override
//    public void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException  {
//        validateState();
//        resources.getJobStore().resetTriggerFromErrorState(triggerKey);
//    }

//    /**
//     * <p>
//     * Add (register) the given <code>Calendar</code> to the Scheduler.
//     * </p>
//     *
//     * @throws SchedulerException
//     *           if there is an internal Scheduler error, or a Calendar with
//     *           the same name already exists, and <code>replace</code> is
//     *           <code>false</code>.
//     */
//    @Override
//    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException {
//        validateState();
//        resources.getJobStore().storeCalendar(calName, calendar, replace, updateTriggers);
//    }

//    /**
//     * <p>
//     * Delete the identified <code>Calendar</code> from the Scheduler.
//     * </p>
//     *
//     * @return true if the Calendar was found and deleted.
//     * @throws SchedulerException
//     *           if there is an internal Scheduler error.
//     */
//    @Override
//    public boolean deleteCalendar(String calName) throws SchedulerException {
//        validateState();
//        return resources.getJobStore().removeCalendar(calName);
//    }

//    /**
//     * <p>
//     * Get the <code>{@link Calendar}</code> instance with the given name.
//     * </p>
//     */
//    @Override
//    public Calendar getCalendar(String calName) throws SchedulerException {
//        validateState();
//        return resources.getJobStore().retrieveCalendar(calName);
//    }

//    /**
//     * <p>
//     * Get the names of all registered <code>{@link Calendar}s</code>.
//     * </p>
//     */
//    @Override
//    public List<String> getCalendarNames() throws SchedulerException {
//        validateState();
//        return resources.getJobStore().getCalendarNames();
//    }

//    public ListenerManager getListenerManager() {
//        return listenerManager;
//    }
    
    /**
     * <p>
     * Add the given <code>{@link org.quartz.JobListener}</code> to the
     * <code>Scheduler</code>'s <i>internal</i> list.
     * </p>
     */
    public void addInternalJobListener(JobListener jobListener) {
        if (jobListener.getName() == null || jobListener.getName().length() == 0) {
            throw new IllegalArgumentException("JobListener name cannot be empty.");
        }
        synchronized (internalJobListeners) {
            internalJobListeners.put(jobListener.getName(), jobListener);
        }
    }

//    /**
//     * <p>
//     * Remove the identified <code>{@link JobListener}</code> from the <code>Scheduler</code>'s
//     * list of <i>internal</i> listeners.
//     * </p>
//     *
//     * @return true if the identified listener was found in the list, and
//     *         removed.
//     */
//    public boolean removeInternalJobListener(String name) {
//        synchronized (internalJobListeners) {
//            return (internalJobListeners.remove(name) != null);
//        }
//    }
//
//    /**
//     * <p>
//     * Get a List containing all of the <code>{@link org.quartz.JobListener}</code>s
//     * in the <code>Scheduler</code>'s <i>internal</i> list.
//     * </p>
//     */
//    public List<JobListener> getInternalJobListeners() {
//        synchronized (internalJobListeners) {
//            return java.util.Collections.unmodifiableList(new LinkedList<JobListener>(internalJobListeners.values()));
//        }
//    }
//
//    /**
//     * <p>
//     * Get the <i>internal</i> <code>{@link org.quartz.JobListener}</code>
//     * that has the given name.
//     * </p>
//     */
//    public JobListener getInternalJobListener(String name) {
//        synchronized (internalJobListeners) {
//            return internalJobListeners.get(name);
//        }
//    }
//
//    /**
//     * <p>
//     * Add the given <code>{@link org.quartz.TriggerListener}</code> to the
//     * <code>Scheduler</code>'s <i>internal</i> list.
//     * </p>
//     */
//    public void addInternalTriggerListener(TriggerListener triggerListener) {
//        if (triggerListener.getName() == null || triggerListener.getName().length() == 0) {
//            throw new IllegalArgumentException("TriggerListener name cannot be empty.");
//        }
//        synchronized (internalTriggerListeners) {
//            internalTriggerListeners.put(triggerListener.getName(), triggerListener);
//        }
//    }
//
//    /**
//     * <p>
//     * Remove the identified <code>{@link TriggerListener}</code> from the <code>Scheduler</code>'s
//     * list of <i>internal</i> listeners.
//     * </p>
//     *
//     * @return true if the identified listener was found in the list, and
//     *         removed.
//     */
//    public boolean removeinternalTriggerListener(String name) {
//        synchronized (internalTriggerListeners) {
//            return (internalTriggerListeners.remove(name) != null);
//        }
//    }
//
//    /**
//     * <p>
//     * Get a list containing all of the <code>{@link org.quartz.TriggerListener}</code>s
//     * in the <code>Scheduler</code>'s <i>internal</i> list.
//     * </p>
//     */
//    public List<TriggerListener> getInternalTriggerListeners() {
//        synchronized (internalTriggerListeners) {
//            return java.util.Collections.unmodifiableList(new LinkedList<TriggerListener>(internalTriggerListeners.values()));
//        }
//    }

//    /**
//     * <p>
//     * Get the <i>internal</i> <code>{@link TriggerListener}</code> that
//     * has the given name.
//     * </p>
//     *  获取具有给定名称的内部TriggerListener。
//     */
//    public TriggerListener getInternalTriggerListener(String name) {
//        synchronized (internalTriggerListeners) {
//            return internalTriggerListeners.get(name);
//        }
//    }
//
//    /**
//     * <p>
//     * Register the given <code>{@link SchedulerListener}</code> with the
//     * <code>Scheduler</code>'s list of internal listeners.
//     * </p>
//     *  将给定的SchedulerListener注册到Scheduler的内部监听器列表中。
//     */
//    public void addInternalSchedulerListener(SchedulerListener schedulerListener) {
//        synchronized (internalSchedulerListeners) {
//            internalSchedulerListeners.add(schedulerListener);
//        }
//    }
//
//    /**
//     * <p>
//     * Remove the given <code>{@link SchedulerListener}</code> from the
//     * <code>Scheduler</code>'s list of internal listeners.
//     * </p>
//     *
//     * @return true if the identified listener was found in the list, and
//     *         removed.
//     */
//    public boolean removeInternalSchedulerListener(SchedulerListener schedulerListener) {
//        synchronized (internalSchedulerListeners) {
//            return internalSchedulerListeners.remove(schedulerListener);
//        }
//    }
//
//    /**
//     * <p>
//     * Get a List containing all of the <i>internal</i> <code>{@link SchedulerListener}</code>s
//     * registered with the <code>Scheduler</code>.
//     * </p>
//     */
//    public List<SchedulerListener> getInternalSchedulerListeners() {
//        synchronized (internalSchedulerListeners) {
//            return java.util.Collections.unmodifiableList(new ArrayList<SchedulerListener>(internalSchedulerListeners));
//        }
//    }

//    protected void notifyJobStoreJobComplete(OperableTrigger trigger, JobDetail detail, CompletedExecutionInstruction instCode) {
//        resources.getJobStore().triggeredJobComplete(trigger, detail, instCode);
//    }

//    protected void notifyJobStoreJobVetoed(OperableTrigger trigger, JobDetail detail, CompletedExecutionInstruction instCode) {
//        resources.getJobStore().triggeredJobComplete(trigger, detail, instCode);
//    }

    protected void notifySchedulerThread(long candidateNewNextFireTime) {
        if (isSignalOnSchedulingChange()) {
            signaler.signalSchedulingChange(candidateNewNextFireTime);
        }
    }

//    private List<TriggerListener> buildTriggerListenerList() throws SchedulerException {
//        List<TriggerListener> allListeners = new LinkedList<TriggerListener>();
//        allListeners.addAll(getListenerManager().getTriggerListeners());
//        allListeners.addAll(getInternalTriggerListeners());
//        return allListeners;
//    }

//    private List<JobListener> buildJobListenerList() throws SchedulerException {
//        List<JobListener> allListeners = new LinkedList<JobListener>();
//        allListeners.addAll(getListenerManager().getJobListeners());
//        allListeners.addAll(getInternalJobListeners());
//        return allListeners;
//    }
//
//    private List<SchedulerListener> buildSchedulerListenerList() {
//        List<SchedulerListener> allListeners = new LinkedList<SchedulerListener>();
//        allListeners.addAll(getListenerManager().getSchedulerListeners());
//        allListeners.addAll(getInternalSchedulerListeners());
//        return allListeners;
//    }
    
//    private boolean matchJobListener(JobListener listener,Key key) {
//        List<Matcher<Key<?>>> matchers = getListenerManager().getJobListenerMatchers(listener.getName());
//        if(matchers == null){
//            return true;
//        }
//        for(Matcher<Key<?>> matcher: matchers) {
//            if(matcher.isMatch(key)){
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private boolean matchTriggerListener(TriggerListener listener,Key key) {
//        List<Matcher<Key<?>>> matchers = getListenerManager().getTriggerListenerMatchers(listener.getName());
//        if(matchers == null){
//            return true;
//        }
//        for(Matcher<Key<?>> matcher: matchers) {
//            if(matcher.isMatch(key)){
//                return true;
//            }
//        }
//        return false;
//    }

//    public boolean notifyTriggerListenersFired(JobExecutionContext jec) throws SchedulerException {
//        boolean vetoedExecution = false;
//        // build a list of all trigger listeners that are to be notified...  构建一个要通知的所有触发器侦听器的列表。。。
//        List<TriggerListener> triggerListeners = buildTriggerListenerList();
//        // notify all trigger listeners in the list  通知列表中的所有触发器侦听器
//        for(TriggerListener tl: triggerListeners) {
//            try {
//                if(!matchTriggerListener(tl, jec.getTrigger().getKey())){
//                    continue;
//                }
//                tl.triggerFired(jec.getTrigger(), jec);
//                if(tl.vetoJobExecution(jec.getTrigger(), jec)) {
//                    vetoedExecution = true;
//                }
//            } catch (Exception e) {
//                SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
//                throw se;
//            }
//        }
//        return vetoedExecution;
//    }
    

//    public void notifyTriggerListenersMisfired(Trigger trigger) throws SchedulerException {
//        // build a list of all trigger listeners that are to be notified... 构建一个要通知的所有触发器侦听器的列表。。。
//        List<TriggerListener> triggerListeners = buildTriggerListenerList();
//        // notify all trigger listeners in the list 通知列表中的所有触发器侦听器
//        for(TriggerListener tl: triggerListeners) {
//            try {
//                if(!matchTriggerListener(tl, trigger.getKey())){
//                    continue;
//                }
//                tl.triggerMisfired(trigger);
//            } catch (Exception e) {
//                SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
//                throw se;
//            }
//        }
//    }

//    public void notifyTriggerListenersComplete(JobExecutionContext jec,CompletedExecutionInstruction instCode) throws SchedulerException {
//        // build a list of all trigger listeners that are to be notified...
//        List<TriggerListener> triggerListeners = buildTriggerListenerList();
//        // notify all trigger listeners in the list
//        for(TriggerListener tl: triggerListeners) {
//            try {
//                if(!matchTriggerListener(tl, jec.getTrigger().getKey())){
//                    continue;
//                }
//                tl.triggerComplete(jec.getTrigger(), jec, instCode);
//            } catch (Exception e) {
//                SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName()+ "' threw exception: " + e.getMessage(), e);
//                throw se;
//            }
//        }
//    }

//    public void notifyJobListenersToBeExecuted(JobExecutionContext jec) throws SchedulerException {
//        // build a list of all job listeners that are to be notified...
//        List<JobListener> jobListeners = buildJobListenerList();
//        // notify all job listeners
//        for(JobListener jl: jobListeners) {
//            try {
//                if(!matchJobListener(jl, jec.getJobDetail().getKey())){
//                    continue;
//                }
//                jl.jobToBeExecuted(jec);
//            } catch (Exception e) {
//                SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
//                throw se;
//            }
//        }
//    }

//    public void notifyJobListenersWasVetoed(JobExecutionContext jec) throws SchedulerException {
//        // build a list of all job listeners that are to be notified... 构建一个要通知的所有作业侦听器的列表。。。
//        List<JobListener> jobListeners = buildJobListenerList();
//        // notify all job listeners 通知所有作业侦听器
//        for(JobListener jl: jobListeners) {
//            try {
//                if(!matchJobListener(jl, jec.getJobDetail().getKey())){
//                    continue;
//                }
//                jl.jobExecutionVetoed(jec);
//            } catch (Exception e) {
//                SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
//                throw se;
//            }
//        }
//    }

//    public void notifyJobListenersWasExecuted(JobExecutionContext jec,JobExecutionException je) throws SchedulerException {
//        // build a list of all job listeners that are to be notified...
//        List<JobListener> jobListeners = buildJobListenerList();
//        // notify all job listeners
//        for(JobListener jl: jobListeners) {
//            try {
//                if(!matchJobListener(jl, jec.getJobDetail().getKey())){
//                    continue;
//                }
//                // todo ...
////                jl.jobWasExecuted(jec, je);
//            } catch (Exception e) {
//                SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
//                throw se;
//            }
//        }
//    }

//    public void notifySchedulerListenersError(String msg, SchedulerException se) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.schedulerError(msg, se);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of error: ", e);
//                getLog().error( "Original error (for notification) was: " + msg, se);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersSchduled(Trigger trigger) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobScheduled(trigger);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of scheduled job." + "  Triger=" + trigger.getKey(), e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersUnscheduled(Key triggerKey) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                if(triggerKey == null){
//                    sl.schedulingDataCleared();
//                }
//                else{
//                    sl.jobUnscheduled(triggerKey);
//                }
//            } catch (Exception e) {
//                getLog().error(
//                        "Error while notifying SchedulerListener of unscheduled job."
//                                + "  Triger=" + (triggerKey == null ? "ALL DATA" : triggerKey), e);
//            }
//        }
//    }

//    public void notifySchedulerListenersFinalized(Trigger trigger) {
//        // build a list of all scheduler listeners that are to be notified... 构建一个要通知的所有调度程序侦听器的列表。。。
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.triggerFinalized(trigger);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of finalized trigger." + "  Triger=" + trigger.getKey(), e);
//            }
//        }
//    }

//    public void notifySchedulerListenersPausedTrigger(Key triggerKey) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.triggerPaused(triggerKey);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of paused trigger: " + triggerKey, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersPausedTriggers(String group) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.triggersPaused(group);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of paused trigger group." + group, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersResumedTrigger(Key key) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.triggerResumed(key);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of resumed trigger: " + key, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersResumedTriggers(String group) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.triggersResumed(group);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of resumed group: " + group, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersPausedJob(Key key) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobPaused(key);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of paused job: " + key, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersPausedJobs(String group) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobsPaused(group);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of paused job group: " + group, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersResumedJob(Key key) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobResumed(key);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of resumed job: " + key, e);
//            }
//        }
//    }
//
//    // todo group不使用后需要优化此
//    public void notifySchedulerListenersResumedJobs(String group) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobsResumed(group);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of resumed job group: " + group, e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersInStandbyMode() {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.schedulerInStandbyMode();
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of inStandByMode.", e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersStarted() {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.schedulerStarted();
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of startup.", e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersStarting() {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for (SchedulerListener sl : schedListeners) {
//            try {
//                sl.schedulerStarting();
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of startup.", e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersShutdown() {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.schedulerShutdown();
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of shutdown.", e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersShuttingdown() {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.schedulerShuttingdown();
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of shutdown.", e);
//            }
//        }
//    }
//
//    public void notifySchedulerListenersJobAdded(JobDetail jobDetail) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobAdded(jobDetail);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of JobAdded.", e);
//            }
//        }
//    }

//    public void notifySchedulerListenersJobDeleted(Key jobKey) {
//        // build a list of all scheduler listeners that are to be notified...
//        List<SchedulerListener> schedListeners = buildSchedulerListenerList();
//        // notify all scheduler listeners
//        for(SchedulerListener sl: schedListeners) {
//            try {
//                sl.jobDeleted(jobKey);
//            } catch (Exception e) {
//                getLog().error("Error while notifying SchedulerListener of JobAdded.", e);
//            }
//        }
//    }
    
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        if(factory == null) {
            throw new IllegalArgumentException("JobFactory cannot be set to null!");
        }
        getLog().info("JobFactory set to: " + factory);
        this.jobFactory = factory;
    }

    public JobFactory getJobFactory()  {
        return jobFactory;
    }
//
//
//    /**
//     * Interrupt all instances of the identified InterruptableJob executing in
//     * this Scheduler instance.
//     *
//     * <p>
//     * This method is not cluster aware.  That is, it will only interrupt
//     * instances of the identified InterruptableJob currently executing in this
//     * Scheduler instance, not across the entire cluster.
//     * </p>
//     *
//     * @see org.quartz.core.RemotableQuartzScheduler#interrupt(JobKey)
//     */
//    @Override
//    public boolean interrupt(Key jobKey) throws UnableToInterruptJobException {
//        List<JobExecutionContext> jobs = getCurrentlyExecutingJobs();
//        JobDetail jobDetail = null;
//        Job job = null;
//        boolean interrupted = false;
//        for(JobExecutionContext jec : jobs) {
//            jobDetail = jec.getJobDetail();
//            if (jobKey.equals(jobDetail.getKey())) {
//                job = jec.getJobInstance();
//                if (job instanceof InterruptableJob) {
//                    ((InterruptableJob)job).interrupt();
//                    interrupted = true;
//                } else {
//                    throw new UnableToInterruptJobException(
//                            "Job " + jobDetail.getKey() +
//                            " can not be interrupted, since it does not implement " +
//                            InterruptableJob.class.getName());
//                }
//            }
//        }
//        return interrupted;
//    }
//
//    /**
//     * Interrupt the identified InterruptableJob executing in this Scheduler instance.
//     *
//     * <p>
//     * This method is not cluster aware.  That is, it will only interrupt
//     * instances of the identified InterruptableJob currently executing in this
//     * Scheduler instance, not across the entire cluster.
//     * </p>
//     *
//     * @see org.quartz.core.RemotableQuartzScheduler#interrupt(JobKey)
//     */
//    @Override
//    public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
//        List<JobExecutionContext> jobs = getCurrentlyExecutingJobs();
//        Job job = null;
//        for(JobExecutionContext jec : jobs) {
//            if (jec.getFireInstanceId().equals(fireInstanceId)) {
//                job = jec.getJobInstance();
//                if (job instanceof InterruptableJob) {
//                    ((InterruptableJob)job).interrupt();
//                    return true;
//                } else {
//                    throw new UnableToInterruptJobException(
//                        "Job " + jec.getJobDetail().getKey() +
//                        " can not be interrupted, since it does not implement " +
//                        InterruptableJob.class.getName());
//                }
//            }
//        }
//        return false;
//    }
    
    private void shutdownPlugins() {
        java.util.Iterator<SchedulerPlugin> itr = resources.getSchedulerPlugins().iterator();
        while (itr.hasNext()) {
            SchedulerPlugin plugin = itr.next();
            plugin.shutdown();
        }
    }

    private void startPlugins() {
        java.util.Iterator<SchedulerPlugin> itr = resources.getSchedulerPlugins().iterator();
        while (itr.hasNext()) {
            SchedulerPlugin plugin = itr.next();
            plugin.start();
        }
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
    public boolean containsNode(String application){
        return resources.getJobStore().containsNode(application);
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
    public int updateNode(QrtzNode qrtzNode){
        return resources.getJobStore().updateNode(qrtzNode);
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
    public int deleteJob(final String job_id) {
        return resources.getJobStore().deleteJob(job_id);
    }
    //    @Override
//    public int findQrtzExecuteCountById(Long job_id){
//        return resources.getJobStore().findQrtzExecuteCountById(job_id);
//    }
    @Override
    public boolean containsExecute(String job_id){
        return resources.getJobStore().containsExecute(job_id);
    }
    @Override
    public int updateJobState(String job_id, String state){
        return resources.getJobStore().updateJobState(job_id,state);
    }
    @Override
    public int updateExecuteState(String execute_id, String state){
        return resources.getJobStore().updateExecuteState(execute_id,state);
    }
    @Override
    public int addExecute(QrtzExecute qrtzExecute){
        return resources.getJobStore().addExecute(qrtzExecute);
    }
    @Override
    public int deleteExecute(String execute_id ){
        return resources.getJobStore().deleteExecute(execute_id);
    }

    @Override
    public int updateExecute(QrtzExecute qrtzExecute) {
        return resources.getJobStore().updateExecute(qrtzExecute);
    }


}

/////////////////////////////////////////////////////////////////////////////
//
// ErrorLogger - Scheduler Listener Class
//
/////////////////////////////////////////////////////////////////////////////

//class ErrorLogger extends SchedulerListenerSupport {
//    ErrorLogger() {
//    }
//
//    @Override
//    public void schedulerError(String msg, SchedulerException cause) {
//        getLog().error(msg, cause);
//    }
//
//}

/////////////////////////////////////////////////////////////////////////////
//
// ExecutingJobsManager - Job Listener Class
//
/////////////////////////////////////////////////////////////////////////////

class ExecutingJobsManager implements JobListener {
    HashMap<String, JobExecutionContext> executingJobs = new HashMap<String, JobExecutionContext>();

    AtomicInteger numJobsFired = new AtomicInteger(0);

    ExecutingJobsManager() {
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

//    public int getNumJobsCurrentlyExecuting() {
//        synchronized (executingJobs) {
//            return executingJobs.size();
//        }
//    }
//    @Override
//    public void jobToBeExecuted(JobExecutionContext context) {
//        numJobsFired.incrementAndGet();
//        synchronized (executingJobs) {
//            executingJobs.put(((OperableTrigger)context.getTrigger()).getFireInstanceId(), context);
//        }
//    }
//    @Override
//    public void jobWasExecuted(JobExecutionContext context,JobExecutionException jobException) {
//        synchronized (executingJobs) {
//            executingJobs.remove(((OperableTrigger)context.getTrigger()).getFireInstanceId());
//        }
//    }

    public int getNumJobsFired() {
        return numJobsFired.get();
    }

    public List<JobExecutionContext> getExecutingJobs() {
        synchronized (executingJobs) {
            return java.util.Collections.unmodifiableList(new ArrayList<JobExecutionContext>(executingJobs.values()));
        }
    }

//    @Override
//    public void jobExecutionVetoed(JobExecutionContext context) {
//
//    }




}
