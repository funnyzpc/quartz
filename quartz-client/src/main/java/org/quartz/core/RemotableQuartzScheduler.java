
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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.Date;
import java.util.List;

/**
 * @author James House
 */
public interface RemotableQuartzScheduler extends Remote {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    String getSchedulerName() throws RemoteException;

    String getSchedulerInstanceId() throws RemoteException;

//    SchedulerContext getSchedulerContext() throws SchedulerException, RemoteException;

    void shutdown() throws RemoteException;

    void shutdown(boolean waitForJobsToComplete) throws RemoteException;

    boolean isShutdown() throws RemoteException;

    Date runningSince() throws RemoteException;

    String getVersion() throws RemoteException;

//    int numJobsExecuted() throws RemoteException;

//    Class<?> getJobStoreClass() throws RemoteException;

    boolean supportsPersistence() throws RemoteException;

    boolean isClustered() throws RemoteException;

//    void clear() throws SchedulerException, RemoteException;

//    Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException, RemoteException;
//    default Date scheduleJob(JobCfg jobCfg, ExecuteCfg executeCfg) throws SchedulerException, RemoteException{
//        throw new SchedulerException("Undefined logic!");
//    }

//    Date scheduleJob(Trigger trigger) throws SchedulerException, RemoteException;

//    void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException, RemoteException;

//    void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException, RemoteException;

//    boolean deleteJob(Key jobKey) throws SchedulerException, RemoteException;

//    boolean unscheduleJob(Key triggerKey) throws SchedulerException, RemoteException;

//    Date rescheduleJob(Key triggerKey, Trigger newTrigger) throws SchedulerException, RemoteException;
        
//    void triggerJob(Key jobKey, JobDataMap data) throws SchedulerException, RemoteException;

//    void triggerJob(OperableTrigger trig) throws SchedulerException, RemoteException;
    
//    void pauseTrigger(Key triggerKey) throws SchedulerException, RemoteException;

//    void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException, RemoteException;

    String[] getDBInfo();
    List<QrtzApp> getAllApp();
    QrtzApp getAppByApplication(String application);
    List<QrtzNode> getNodeByApp(String application);
    // 根据job_id获取job信息
    QrtzJob getJobByJobId(String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzExecute getExecuteByExecuteId(String execute_id);
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
    // 删除节点
    int deleteNode(String application,String hostIP);
    // 暂停节点
    int updateNodeState(QrtzNode qrtzNode);

    // 添加应用及节点
    int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode);

    int addJob(QrtzJob qrtzJob) throws SchedulerException, RemoteException;
    int updateJob(QrtzJob qrtzJob) throws SchedulerException, RemoteException;
    int deleteJob(Long job_id) ;
    int findQrtzExecuteCountById(Long job_id);

    // 暂停指定job下的所有execute
    int updateExecuteStateByJobId(Long job_id,String state);
    // 暂停指定execute
    int updateExecuteStateByExecuteId( Long execute_id,String state);

    // 添加execute
    int addExecute(QrtzExecute qrtzExecute);
    // 删除execute
    int deleteExecute(Long execute_id );

    
}
