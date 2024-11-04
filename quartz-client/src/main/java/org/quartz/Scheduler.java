
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


import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;

import java.util.List;

/**
 * This is the main interface of a Quartz Scheduler.
 * 
 * <p>
 * A <code>Scheduler</code> maintains a registry of <code>{@link JobDetail}</code>s
 * and <code>{@link Trigger}</code>s. Once registered, the <code>Scheduler</code>
 * is responsible for executing <code>Job</code> s when their associated
 * <code>Trigger</code> s fire (when their scheduled time arrives).
 * </p>
 * 
 * <p>
 * <code>Scheduler</code> instances are produced by a <code>{@link SchedulerFactory}</code>.
 * A scheduler that has already been created/initialized can be found and used
 * through the same factory that produced it. After a <code>Scheduler</code>
 * has been created, it is in "stand-by" mode, and must have its 
 * <code>start()</code> method called before it will fire any <code>Job</code>s.
 * </p>
 * 
 * <p>
 * <code>Job</code> s are to be created by the 'client program', by defining
 * a class that implements the <code>{@link Job}</code>
 * interface. <code>{@link JobDetail}</code> objects are then created (also
 * by the client) to define a individual instances of the <code>Job</code>.
 * <code>JobDetail</code> instances can then be registered with the <code>Scheduler</code>
 * via the <code>scheduleJob(JobDetail, Trigger)</code> or <code>addJob(JobDetail, boolean)</code>
 * method.
 * </p>
 * 
 * <p>
 * <code>Trigger</code> s can then be defined to fire individual <code>Job</code>
 * instances based on given schedules. <code>SimpleTrigger</code> s are most
 * useful for one-time firings, or firing at an exact moment in time, with N
 * repeats with a given delay between them. <code>CronTrigger</code> s allow
 * scheduling based on time of day, day of week, day of month, and month of
 * year.
 * </p>
 * 
 * <p>
 * <code>Job</code> s and <code>Trigger</code> s have a name and group
 * associated with them, which should uniquely identify them within a single
 * <code>{@link Scheduler}</code>. The 'group' feature may be useful for
 * creating logical groupings or categorizations of <code>Jobs</code> s and
 * <code>Triggers</code>s. If you don't have need for assigning a group to a
 * given <code>Jobs</code> of <code>Triggers</code>, then you can use the
 * <code>DEFAULT_GROUP</code> constant defined on this interface.
 * </p>
 * 
 * <p>
 * Stored <code>Job</code> s can also be 'manually' triggered through the use
 * of the <code>triggerJob(String jobName, String jobGroup)</code> function.
 * </p>
 * 
 * <p>
 * Client programs may also be interested in the 'listener' interfaces that are
 * available from Quartz. The <code>{@link JobListener}</code> interface
 * provides notifications of <code>Job</code> executions. The <code>{@link TriggerListener}</code>
 * interface provides notifications of <code>Trigger</code> firings. The
 * <code>{@link SchedulerListener}</code> interface provides notifications of
 * <code>Scheduler</code> events and errors.  Listeners can be associated with
 * local schedulers through the {@link ListenerManager} interface.  
 * </p>
 * 
 * <p>
 * The setup/configuration of a <code>Scheduler</code> instance is very
 * customizable. Please consult the documentation distributed with Quartz.
 * </p>
 * 
 * @see Job
 * @see JobDetail
 * @see JobBuilder
 * @see Trigger
 * @see TriggerBuilder
 * @see JobListener
 * @see TriggerListener
 * @see SchedulerListener
 * 
 * @author James House
 * @author Sharada Jambula
 */
public interface Scheduler {
//    // 获取当前应用信息  [application,host_ip,host_name]
//    String[] getCurrentNodeInfo();
    // 获取数据库信息(  [数据库厂商,数据库schema] )
    String[] getDBInfo() ;
    // 获取所有应用(不含节点)
    List<QrtzApp> getAllApp();
    // 根据应用查询应用下所有节点
    List<QrtzNode> getNodeByApp(String application);
    // 根据job_id获取job信息
    QrtzJob getJobByJobId(String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzExecute getExecuteByExecuteId(String execute_id);
    // 根据job_id获取job下所有execute信息
    List<QrtzExecute> getExecuteByJobId(String job_id);
    // 根据job_id获取job及其下所有execute信息
    QrtzJob getJobInAllByJobId(String job_id);
    // 根据execute_id获取execute及关联的job信息
    QrtzExecute getExecuteInAllByExecuteId(String execute_id);

    // 添加应用
    Object[] addApp(QrtzApp qrtzApp);
    // 删除应用
    Object[] deleteApp(String application);
    // 暂停/启动应用
    int updateAppState(String application,String state);

    // 添加节点
    Object[] addNode(QrtzNode qrtzNode);
    // 删除节点
    int deleteNode(String application,String hostIP);
    // 暂停/开启节点
    int updateNodeState(QrtzNode qrtzNode);
    int updateNode(QrtzNode qrtzNode);

    // 添加应用及节点
    Object[] addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode);

    // 添加任务
    Object[] addJob(QrtzJob qrtzJob) ;
    // 更新任务
    Object[] updateJob(QrtzJob qrtzJob) ;
    // 删除任务
    int deleteJob(String job_id) ;
    // 修改job及其下所有执行项状态 注意:仅可操作为 EXECUTING or PAUSED
    Object[] updateJobStateInAll(String job_id, String state);
    // 修改指定job状态
    int updateJobState(String job_id, String state);
    // 修改指定execute状态
    int updateExecuteState(String execute_id, String state);
    // 添加execute
    Object[] addExecute(QrtzExecute qrtzExecute);
    // 删除execute
    int deleteExecute(String execute_id );
    // 更新执行项
    Object[] updateExecute(QrtzExecute qrtzExecute);


}
