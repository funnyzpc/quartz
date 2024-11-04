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

import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;

import java.util.List;


/**
 * <p>
 * The interface to be implemented by classes that want to provide a <code>{@link org.quartz.Job}</code>
 * and <code>{@link org.quartz.Trigger}</code> storage mechanism for the
 * <code>{@link org.quartz.core.QuartzScheduler}</code>'s use.
 * </p>
 *
 * <p>
 * Storage of <code>Job</code> s and <code>Trigger</code> s should be keyed
 * on the combination of their name and group for uniqueness.
 * </p>
 *
 * @see org.quartz.core.QuartzScheduler
 * @see org.quartz.Trigger
 * @author James House
 * @author Eric Mueller
 */
public interface JobStore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */


    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has been paused.
     */
    void schedulerPaused();

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has resumed after being paused.
     */
    void schedulerResumed();

    boolean supportsPersistence();
    
//    /**
//     * How long (in milliseconds) the <code>JobStore</code> implementation
//     * estimates that it will take to release a trigger and acquire a new one.
//     */
//    long getEstimatedTimeToReleaseAndAcquireTrigger();
    
    /**
     * Whether or not the <code>JobStore</code> implementation is clustered.
     */
    boolean isClustered();

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's Id,
     * prior to initialize being invoked.
     *  eg: QUARTZ-SPRINGBOOT#192.168.1.1
     * @since 1.7
     */
    void setInstanceId(String schedInstId);

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's name,
     * prior to initialize being invoked.
     *  eg: QUARTZ-SPRINGBOOT
     * @since 1.7
     */
//    void setInstanceName(String schedName);
    void setApplication(String application);
    String getInstanceName();

    String[] getDBInfo()  ;
    List<QrtzApp> getAllApp();
    QrtzApp getAppByApplication(String application);

    List<QrtzNode> getNodeByApp(String application);
    // 根据job_id获取job信息
    QrtzJob getJobByJobId(String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzExecute getExecuteByExecuteId( String execute_id);
    // 根据job_id获取job下所有execute信息
    List<QrtzExecute> getExecuteByJobId(String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzJob getJobInAllByJobId(String job_id);
    // 根据execute_id获取execute及job信息
    QrtzExecute getExecuteInAllByExecuteId(String execute_id);
    // 添加应用
    int addApp(QrtzApp qrtzApp);
    // 删除应用
    int deleteApp(String application);
    // 暂停/启动应用
    int updateAppState(String application,String state);
    // 添加节点
    int addNode(QrtzNode qrtzNode);
    boolean containsNode(String application ,String hostIP);
    boolean containsNode(String application);
    // 删除节点
    int deleteNode(String application,String hostIP);
    // 暂停节点
    int updateNodeState(QrtzNode qrtzNode);
    int updateNode(QrtzNode qrtzNode);

    // 添加应用及节点
    int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode);

    int addJob(QrtzJob qrtzJob) ;
    int updateJob(QrtzJob qrtzJob) ;
    int deleteJob(String job_id) ;
//    int findQrtzExecuteCountById(Long job_id);
    boolean containsExecute(String job_id);

    // 暂停指定job下的所有execute
    int updateJobState(String job_id, String state);
    // 暂停指定execute
    int updateExecuteState(String execute_id, String state);

    // 添加execute
    int addExecute(QrtzExecute qrtzExecute);
    // 删除execute
    int deleteExecute(String execute_id );

    int updateExecute(QrtzExecute qrtzExecute);

}
