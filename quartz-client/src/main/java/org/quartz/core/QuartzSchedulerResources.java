
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


import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.simpl.SystemPropGenerator;
import org.quartz.spi.JobStore;

import javax.sql.DataSource;

/**
 * <p>
 * Contains all of the resources (<code>JobStore</code>,<code>ThreadPool</code>,
 * etc.) necessary to create a <code>{@link QuartzScheduler}</code> instance.
 * </p>
 * 
 * @see QuartzScheduler
 * 
 * @author James House
 */
public class QuartzSchedulerResources {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

//    public static final String CREATE_REGISTRY_NEVER = "never";
//    public static final String CREATE_REGISTRY_ALWAYS = "always";
//    public static final String CREATE_REGISTRY_AS_NEEDED = "as_needed";

    private String name;

    private String instanceId;

    private String threadName;

    private JobStore jobStore;


    private boolean makeSchedulerThreadDaemon = false;

    private boolean threadsInheritInitializersClassLoadContext = false;

//    @Deprecated
//    private boolean jmxExport;
//    @Deprecated
//    private String jmxObjectName;

//    private ThreadExecutor threadExecutor;
//    private Thread threadExecutor;

    private long batchTimeWindow = 0;

    private int maxBatchSize = 1;

    private boolean interruptJobsOnShutdown = false;
    private boolean interruptJobsOnShutdownWithWait = false;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create an instance with no properties initialized.
     * </p>
     */
    public QuartzSchedulerResources() {
        // do nothing...
    }
    public QuartzSchedulerResources(final DataSource dataSource,final long idleWaitTime,final String tablePrefix) {
        this.jobStore = new JobStoreSupport(dataSource,idleWaitTime,null==tablePrefix?"QRTZ_":tablePrefix);
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
     * Get the name for the <code>{@link QuartzScheduler}</code>.
     * </p>
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Set the name for the <code>{@link QuartzScheduler}</code>.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty.
     */
    public void setName(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Scheduler name cannot be empty.");
        }
        this.name = name;
        if (threadName == null) {
            // thread name not already set, use default thread name
            setThreadName(name + "_QuartzSchedulerThread");
        }        
    }

    /**
     * <p>
     * Get the instance Id for the <code>{@link QuartzScheduler}</code>.
     * </p>
     */
    public String getInstanceId() {
        return null==instanceId?(name+"#"+ SystemPropGenerator.hostIP()):instanceId;
    }

//    /**
//     * <p>
//     * Set the name for the <code>{@link QuartzScheduler}</code>.
//     * </p>
//     *
//     * @exception IllegalArgumentException
//     *              if name is null or empty.
//     */
//    public void setInstanceId(String instanceId) {
//        if (instanceId == null || instanceId.trim().length() == 0) {
//            throw new IllegalArgumentException("Scheduler instanceId cannot be empty.");
//        }
//        this.instanceId = instanceId;
//    }
//    /**
//     * <p>
//     * Get the port number of the RMI Registry that the scheduler should export
//     * itself to.
//     * </p>
//     */
//    public int getRMIRegistryPort() {
//        return rmiRegistryPort;
//    }
//
//    /**
//     * <p>
//     * Set the port number of the RMI Registry that the scheduler should export
//     * itself to.
//     * </p>
//     */
//    public void setRMIRegistryPort(int port) {
//        this.rmiRegistryPort = port;
//    }
//
//
//    /**
//     * <p>
//     * Get the port number the scheduler server will be bound to.
//     * </p>
//     */
//    public int getRMIServerPort() {
//        return rmiServerPort;
//    }
//
//    /**
//     * <p>
//     * Set the port number the scheduler server will be bound to.
//     * </p>
//     */
//    public void setRMIServerPort(int port) {
//        this.rmiServerPort = port;
//    }
//
//    /**
//     * <p>
//     * Get the setting of whether or not Quartz should create an RMI Registry,
//     * and if so, how.
//     * </p>
//     */
//    public String getRMICreateRegistryStrategy() {
//        return rmiCreateRegistryStrategy;
//    }

    /**
     * <p>
     * Get the name for the <code>{@link QuartzSchedulerThread}</code>.
     * </p>
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * <p>
     * Set the name for the <code>{@link QuartzSchedulerThread}</code>.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if name is null or empty.
     */
    public void setThreadName(String threadName) {
        if (threadName == null || threadName.trim().length() == 0) {
            throw new IllegalArgumentException("Scheduler thread name cannot be empty.");
        }
        this.threadName = threadName;
    }

    /**
     * <p>
     * Get the <code>{@link JobStore}</code> for the <code>{@link QuartzScheduler}</code>
     * to use.
     * </p>
     */
    public JobStore getJobStore() {
        return jobStore;
    }

    /**
     * <p>
     * Set the <code>{@link JobStore}</code> for the <code>{@link QuartzScheduler}</code>
     * to use.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if jobStore is null.
     */
    public void setJobStore(JobStore jobStore) {
        if (jobStore == null) {
            throw new IllegalArgumentException("JobStore cannot be null.");
        }
        this.jobStore = jobStore;
    }

    /**
     * Get whether to mark the Quartz scheduling thread as daemon.
     * 
     * @see Thread#setDaemon(boolean)
     */
    public boolean getMakeSchedulerThreadDaemon() {
        return makeSchedulerThreadDaemon;
    }

    /**
     * Set whether to mark the Quartz scheduling thread as daemon.
     * 
     * @see Thread#setDaemon(boolean)
     */
    public void setMakeSchedulerThreadDaemon(boolean makeSchedulerThreadDaemon) {
        this.makeSchedulerThreadDaemon = makeSchedulerThreadDaemon;
    }

    /**
     * Get whether to set the class load context of spawned threads to that
     * of the initializing thread.
     * 获取是否将生成线程的类加载上下文设置为初始化线程的类负载上下文。
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
     * 默认是 0 ，具体参见配置: org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow
     * @return
     */
    public long getBatchTimeWindow() {
        return batchTimeWindow;
    }

    public void setBatchTimeWindow(long batchTimeWindow) {
        this.batchTimeWindow = batchTimeWindow;
    }

    /**
     *
     * 默认是1,具体参见配置: org.quartz.scheduler.batchTriggerAcquisitionMaxCount
     *
     * @return int
     */
    public int getMaxBatchSize() {
      return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
      this.maxBatchSize = maxBatchSize;
    }
    
    public boolean isInterruptJobsOnShutdown() {
        return interruptJobsOnShutdown;
    }

    public void setInterruptJobsOnShutdown(boolean interruptJobsOnShutdown) {
        this.interruptJobsOnShutdown = interruptJobsOnShutdown;
    }
    
    public boolean isInterruptJobsOnShutdownWithWait() {
        return interruptJobsOnShutdownWithWait;
    }

    public void setInterruptJobsOnShutdownWithWait(boolean interruptJobsOnShutdownWithWait) {
        this.interruptJobsOnShutdownWithWait = interruptJobsOnShutdownWithWait;
    }


}
