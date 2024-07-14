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

import org.quartz.SchedulerConfigException;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a thread
 * pool for the <code>{@link org.quartz.core.QuartzScheduler}</code>'s use.
 *  要由想要为org.quartz.core提供线程池的类实现的接口。QuartzScheduler的使用。
 * </p>
 *
 * <p>
 * <code>ThreadPool</code> implementation instances should ideally be made
 * for the sole use of Quartz.  Most importantly, when the method
 * <code>blockForAvailableThreads()</code> returns a value of 1 or greater,
 * there must still be at least one available thread in the pool when the
 * method <code>runInThread(Runnable)</code> is called a few moments (or
 * many moments) later.  If this assumption does not hold true, it may
 * result in extra JobStore queries and updates, and if clustering features
 * are being used, it may result in greater imballance of load.
 *  ThreadPool实现实例最好只供Quartz使用。最重要的是，当方法blockForAvailableThreads（）返回值为1或更大时，当方法runInThread（Runnable）稍后被调用时，池中必须至少有一个可用线程。
 *  如果这一假设不成立，可能会导致额外的JobStore查询和更新，如果正在使用集群功能，则可能会导致更大的负载不足。
 * </p>
 *
 * @see org.quartz.core.QuartzScheduler
 *
 * @author James House
 */
public interface ThreadPool {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Execute the given <code>{@link java.lang.Runnable}</code> in the next
     * available <code>Thread</code>.
     *  在下一个可用线程中执行给定的Runnable。
     * </p>
     *
     * <p>
     * The implementation of this interface should not throw exceptions unless
     * there is a serious problem (i.e. a serious misconfiguration). If there
     * are no immediately available threads <code>false</code> should be returned.
     *  除非出现严重问题（即严重的配置错误），否则此接口的实现不应引发异常。如果没有立即可用的线程，则应返回false。
     * </p>
     *
     * @return true, if the runnable was assigned to run on a Thread.
     *      true，如果可运行程序被分配为在线程上运行。
     */
    boolean runInThread(Runnable runnable);

    /**
     * <p>
     * Determines the number of threads that are currently available in in
     * the pool.  Useful for determining the number of times
     * <code>runInThread(Runnable)</code> can be called before returning
     * false.
     * 确定池中当前可用的线程数。用于确定在返回false之前可以调用runInThread（Runnable）的次数。
     * </p>
     *
     * <p>The implementation of this method should block until there is at
     * least one available thread.
     * 此方法的实现应该阻塞，直到至少有一个可用线程为止。
     * </p>
     *
     * @return the number of currently available threads 当前可用线程的数量
     */
    int blockForAvailableThreads();

    /**
     * <p>
     * Must be called before the <code>ThreadPool</code> is
     * used, in order to give the it a chance to initialize.
     *  必须在使用ThreadPool之前调用，以便给它一个初始化的机会。
     * </p>
     * 
     * <p>
     *   Typically called by the <code>SchedulerFactory</code>.
     *   通常由SchedulerFactory调用。
     * </p>
     */
    void initialize() throws SchedulerConfigException;

    /**
     * <p>
     * Called by the QuartzScheduler to inform the <code>ThreadPool</code>
     * that it should free up all of it's resources because the scheduler is
     * shutting down.
     *  由QuartzScheduler调用，以通知ThreadPool它应该释放所有资源，因为调度器正在关闭。
     * </p>
     */
    void shutdown(boolean waitForJobsToComplete);

    /**
     * <p>Get the current number of threads in the <code>ThreadPool</code>.
     * 获取线程池中的当前线程数。
     * </p>
     */
    int getPoolSize();

    /**
     * <p>Inform the <code>ThreadPool</code> of the Scheduler instance's Id,
     * prior to initialize being invoked.
     * 在调用初始化之前，将Scheduler实例的Id通知ThreadPool。
     * </p>
     *
     * @since 1.7
     */
    void setInstanceId(String schedInstId);

    /**
     * <p>Inform the <code>ThreadPool</code> of the Scheduler instance's name,
     * prior to initialize being invoked.
     * 在调用初始化之前，将Scheduler实例的名称通知ThreadPool。
     * </p>
     *
     * @since 1.7
     */
    void setInstanceName(String schedName);

}
