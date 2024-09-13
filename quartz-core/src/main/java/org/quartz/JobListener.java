
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

package org.quartz;

/**
 * The interface to be implemented by classes that want to be informed when a
 * <code>{@link org.quartz.JobDetail}</code> executes. In general,
 * applications that use a <code>Scheduler</code> will not have use for this
 * mechanism.
 * 
 * @see ListenerManager#addJobListener(JobListener, Matcher)
 * @see Matcher
 * @see Job
 * @see JobExecutionContext
 * @see JobExecutionException
 * @see TriggerListener
 * 
 * @author James House
 */
public interface JobListener {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the name of the <code>JobListener</code>.
     * </p>
     */
    String getName();

//    /**
//     * <p>
//     * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.JobDetail}</code>
//     * is about to be executed (an associated <code>{@link Trigger}</code>
//     * has occurred).
//     * </p>
//     *
//     * <p>
//     * This method will not be invoked if the execution of the Job was vetoed
//     * by a <code>{@link TriggerListener}</code>.
//     * </p>
//     *
//     * @see #jobExecutionVetoed(JobExecutionContext)
//     * 当JobDetail即将执行时（相关触发器已发生），由调度器调用。
//     * 如果作业的执行被TriggerListener否决，则不会调用此方法。
//     * 另请参见：
//     * 作业执行已被审查（作业执行上下文）
//     */
//    void jobToBeExecuted(JobExecutionContext context);
//
//    /**
//     * <p>
//     * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.JobDetail}</code>
//     * was about to be executed (an associated <code>{@link Trigger}</code>
//     * has occurred), but a <code>{@link TriggerListener}</code> vetoed it's
//     * execution.
//     * </p>
//     *
//     * @see #jobToBeExecuted(JobExecutionContext)
//     *
//     * 当JobDetail即将被执行时（关联的触发器已经发生），由调度器调用，但TriggerListener否决了它的执行。
//     * 另请参见：
//     * jobToBeExecute（作业执行上下文）
//     */
//    void jobExecutionVetoed(JobExecutionContext context);

    
//    /**
//     * <p>
//     * Called by the <code>{@link Scheduler}</code> after a <code>{@link org.quartz.JobDetail}</code>
//     * has been executed, and be for the associated <code>Trigger</code>'s
//     * <code>triggered(xx)</code> method has been called.
//     * </p>
//     */
//    void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException);

}
