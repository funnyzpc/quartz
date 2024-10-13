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

package org.quartz.impl;

import org.quartz.Scheduler;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.json.JSONArray;
import org.quartz.json.JSONObject;
import org.quartz.simpl.SeqGenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that directly
 * proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code>
 * instance.
 * </p>
 * 
 * @see Scheduler
 * @see QuartzScheduler
 *
 * @author James House
 */
public class StdScheduler implements Scheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdScheduler.class);

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private QuartzScheduler sched;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Construct a <code>StdScheduler</code> instance to proxy the given
     * <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
     * </p>
     */
    public StdScheduler(DataSource dataSource) {
        this.sched = new QuartzScheduler(dataSource,-1,null);
    }
    public StdScheduler(DataSource dataSource,long idleWaitTime,final String tablePrefix) {
        this.sched = new QuartzScheduler(dataSource,idleWaitTime,tablePrefix);
    }
    @Override
    public String[] getDBInfo(){
        return sched.getDBInfo();
    }

    @Override
    public List<QrtzApp> getAllApp(){
        return sched.getAllApp();
    }
    @Override
    public List<QrtzNode> getNodeByApp(String application){
        if( null==application || "".equals(application) ){
            LOGGER.error("必要参数为空! [application]");
            return null;
        }
        return sched.getNodeByApp(application);
    }
    @Override
    public QrtzJob getJobByJobId(String job_id){
        if( null==job_id || "".equals(job_id) ){
            LOGGER.error("必要参数为空! [job_id]");
            return null;
        }
        return sched.getJobByJobId(job_id);
    }
    @Override
    public QrtzExecute getExecuteByExecuteId(String execute_id){
        if( null==execute_id || "".equals(execute_id) ){
            LOGGER.error("必要参数为空! [execute_id]");
            return null;
        }
        return sched.getExecuteByExecuteId(execute_id);
    }
    @Override
    public List<QrtzExecute> getExecuteByJobId(String job_id){
        if( null==job_id || "".equals(job_id) ){
            LOGGER.error("必要参数为空! [job_id]");
            return null;
        }
        return sched.getExecuteByJobId(job_id);
    }
    @Override
    public QrtzJob getJobInAllByJobId(String job_id){
        if( null==job_id || "".equals(job_id) ){
            LOGGER.error("必要参数为空! [job_id] {}",job_id);
            return null;
        }
        return sched.getJobInAllByJobId(job_id);
    }
    @Override
    public QrtzExecute getExecuteInAllByExecuteId(String execute_id){
        if( null==execute_id || "".equals(execute_id) ){
            LOGGER.error("必要参数为空! [job_id] {}",execute_id);
            return null;
        }
        return sched.getExecuteInAllByExecuteId(execute_id);
    }
    @Override
    public int addApp(QrtzApp qrtzApp){
        String state ;
        if( null==qrtzApp || null==qrtzApp.getApplication() ||"".equals(qrtzApp.getApplication())
            || null==(state=qrtzApp.getState()) || (!"N".equals(state) && !"Y".equals(state))
            || null==qrtzApp.getTimePre() || null==qrtzApp.getTimeNext() || null== qrtzApp.getTimeInterval()){
            LOGGER.error("必要参数为空或异常! [qrtzApp]");
            return 0;
        }
        return sched.addApp(qrtzApp);
    }
    @Override
    public int deleteApp(String application){
        if(null==application || "".equals(application=application.trim())){
            LOGGER.error("必要参数为空! [application]: {}",application);
            return 0;
        }
        return sched.deleteApp(application);
    }
    @Override
    public int updateAppState(String application,String state){
        if(null==application || "".equals(application=application.trim())
            || null==state || (!"N".equals(state) && !"Y".equals(state))){
            LOGGER.error("必要参数为空! [application、state]: {},{}",application,state);
            return 0;
        }
        return sched.updateAppState(application,state);
    }
    @Override
    public int addNode(QrtzNode qrtzNode){
        String state;
        if(null==qrtzNode
                || null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                || null==qrtzNode.getHostName()
                || null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))
                || null==qrtzNode.getTimeCheck() ){
            LOGGER.error("必要参数为空! [qrtzNode]: {}",qrtzNode);
            return 0;
        }
        return sched.addNode(qrtzNode);
    }
    @Override
    public int deleteNode(String application,String hostIP){
        if( null==application || "".equals(application=application.trim()) || null==hostIP || "".equals(hostIP=hostIP.trim())){
            LOGGER.error("必要参数为空! [application、hostIP]: {},{}",application,hostIP);
            return 0;
        }
        return sched.deleteNode(application,hostIP);
    }
    // 调整节点运行状态
    @Override
    public int updateNodeState(QrtzNode qrtzNode){
        String state;
        if( null==qrtzNode
                || null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                || null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state)) ){
            LOGGER.error("参数为空或参数异常:{}",qrtzNode);
            return 0;
        }
        return sched.updateNodeState(qrtzNode);
    }
    @Override
    public int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode){
        String state ;
        if( null==qrtzApp || null==qrtzApp.getApplication() ||"".equals(qrtzApp.getApplication())
                || null==(state=qrtzApp.getState()) || (!"N".equals(state) && !"Y".equals(state))
                || null==qrtzApp.getTimePre() || null==qrtzApp.getTimeNext() || null== qrtzApp.getTimeInterval()){
            LOGGER.error("必要参数为空或异常! [qrtzApp] {}",qrtzApp);
            return 0;
        }
        if(null==qrtzNode
                || null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                || null==qrtzNode.getHostName()
                || null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))
                || null==qrtzNode.getTimeCheck() ){
            LOGGER.error("必要参数为空或异常! [qrtzNode] {}",qrtzNode);
            return 0;
        }
        return sched.addAppAndNode(qrtzApp,qrtzNode);
    }


    /////////////////////////////////////
    @Override
    public int addJob(QrtzJob qrtzJob){
        // 检查参数
        if(null==qrtzJob || null==qrtzJob.getApplication() || null==qrtzJob.getState() || null==qrtzJob.getJobClass() ){
            LOGGER.error("必要参数不可为空:{}",qrtzJob);
            return 0;
        }
        String job_data = qrtzJob.getJobData();
        JSONObject jo;
        JSONArray ja;
        if( null!=job_data
                && !"".equals(job_data=job_data.trim())
                && !((job_data.startsWith("{") && job_data.endsWith("}") && (jo = new JSONObject(job_data)) !=null && (job_data=jo.toString())!=null) ||
                (job_data.startsWith("[") && job_data.endsWith("]") && (ja = new JSONArray(job_data)) !=null) && (job_data=ja.toString())!=null) ){
            LOGGER.error("异常的任务数据：{}",qrtzJob);
            return 0;
        }
        qrtzJob.setJobData(job_data);
        // 赋初始化参数
        qrtzJob.setId(SeqGenUtil.shortKey());
        qrtzJob.setUpdateTime(System.currentTimeMillis()/1000*1000);
        return sched.addJob(qrtzJob);
    }

    @Override
    public int updateJob(QrtzJob qrtzJob){
        // 检查参数
        if(null==qrtzJob || null==qrtzJob.getId() || null==qrtzJob.getApplication() || null==qrtzJob.getState() || null==qrtzJob.getJobClass() ){
            LOGGER.error("必要参数不可为空:{}",qrtzJob);
            return 0;
        }
        String job_data = qrtzJob.getJobData();
        JSONObject jo;
        JSONArray ja;
        if( null!=job_data
                && !"".equals(job_data=job_data.trim())
                && !((job_data.startsWith("{") && job_data.endsWith("}") && (jo = new JSONObject(job_data)) !=null && (job_data=jo.toString())!=null) ||
                (job_data.startsWith("[") && job_data.endsWith("]") && (ja = new JSONArray(job_data)) !=null) && (job_data=ja.toString())!=null) ){
            LOGGER.error("异常的任务数据：{}",qrtzJob);
            return 0;
        }
        qrtzJob.setJobData(job_data);
        // 赋初始化参数
        qrtzJob.setUpdateTime(System.currentTimeMillis()/1000*1000);
        return sched.updateJob(qrtzJob);
    }
    @Override
    public int deleteJob(Long job_id){
        if( null==job_id ){
            LOGGER.error("必要参数为空! [job_id] {}",job_id);
            return 0;
        }
        return sched.deleteJob(job_id);
    }

    @Override
    public int updateExecuteStateByJobId(Long job_id,String state){
        if( null==job_id || null==state || "".equals(state=state.trim()) || (!"N".equals(state) && !"Y".equals(state))){
            LOGGER.error("必要参数为空! [job_id] {},{}",job_id,state);
            return 0;
        }
        return sched.updateExecuteStateByJobId(job_id,state);
    }
    @Override
    public int updateExecuteStateByExecuteId(Long execute_id,String state){
        if( null==execute_id || null==state || "".equals(state=state.trim()) || (!"N".equals(state) && !"Y".equals(state)) ){
            LOGGER.error("必要参数为空! [execute_id、state] {},{}",execute_id,state);
            return 0;
        }
        return sched.updateExecuteStateByExecuteId(execute_id,state);
    }
    @Override
    public int addExecute(QrtzExecute qrtzExecute){
        String jobType;
        String state;
        if(null==qrtzExecute || null==qrtzExecute.getPid()
                || null==(jobType=qrtzExecute.getJobType()) || (!"CRON".equals(jobType) && !"SIMPLE".equals(jobType))
                || null==(state=qrtzExecute.getState()) || (!"N".equals(state) && !"Y".equals(state))
                ){
            LOGGER.error("必要参数为空或参数异常! [qrtzExecute]:{}",qrtzExecute);
            return 0;
        }
        final Long pid = qrtzExecute.getPid();
        if( sched.getJobByJobId(pid.toString())==null){
            LOGGER.error("对应job配置为空! [qrtzExecute]:{}",qrtzExecute);
            return 0;
        }
        final Date aft = new Date(System.currentTimeMillis() + 5000L);
        // 如果是 SIMPLE任务
        if("SIMPLE".equals(jobType)){
            final Integer repeatInterval = qrtzExecute.getRepeatInterval();
            final Long startTime = qrtzExecute.getStartTime();
            final Long endTime = qrtzExecute.getEndTime();
            final Integer repeatCount = qrtzExecute.getRepeatCount();
            if( null==repeatCount || null==repeatInterval || null==qrtzExecute.getTimeTriggered()
                || null==qrtzExecute.getStartTime() || repeatInterval<10 ){
                LOGGER.error("SIMPLE任务参数异常! [qrtzExecute]:{}",qrtzExecute);
                return 0;
            }
            SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl()
                    .setStartTime(new Date(startTime))
                    .setEndTime(new Date(endTime))
                    .setRepeatCount(repeatCount)
                    .setRepeatInterval(repeatInterval)
                    .setTimesTriggered(0);
            // 必须要滞后5分钟，否则任务无法扫到
            Date nextFireTime = simpleTrigger.getFireTimeAfter(aft);
            if(null==nextFireTime){
                LOGGER.error("SIMPLE任务无效的任务配置 [qrtzExecute]:{}",qrtzExecute);
                return 0;
            }
            qrtzExecute.setNextFireTime(nextFireTime.getTime());
        }
        // 如果是 CRON任务
        if("CRON".equals(jobType)){
            final String cron = qrtzExecute.getCron();
            String zoneId = qrtzExecute.getZoneId();
            final Long startTime = qrtzExecute.getStartTime();
            final Long endTime = qrtzExecute.getEndTime();
            if(null==cron || "".equals(cron.trim())){
                LOGGER.error("CRON任务参数异常! [qrtzExecute]:{}",qrtzExecute);
                return 0;
            }
            if(null==zoneId || "".equals(zoneId.trim()) || null==ZoneId.of(zoneId)){
                qrtzExecute.setZoneId(zoneId="Asia/Shanghai");
            }
            try {
                CronTriggerImpl cronTrigger = new CronTriggerImpl()
                        .setCronExpression(cron)
                        .setStartTime(new Date(startTime))
                        .setEndTime(new Date(endTime))
                        .setTimeZone(TimeZone.getTimeZone(zoneId));

                Date nextFireTime = cronTrigger.getFireTimeAfter(aft);
                if (null == nextFireTime) {
                    LOGGER.error("CRON任务无效的任务配置 [qrtzExecute]:{}", qrtzExecute);
                    return 0;
                }
                qrtzExecute.setNextFireTime(nextFireTime.getTime());
            }catch (Exception e){
                e.printStackTrace();
                LOGGER.error("CRON任务配置异常:{}",qrtzExecute,e);
                return 0;
            }
        }
        qrtzExecute.setId(SeqGenUtil.shortKey());
        return sched.addExecute(qrtzExecute);
    }
    @Override
    public int deleteExecute(Long execute_id ){
        if(null==execute_id){
            LOGGER.error("必要参数为空! [execute_id]:{}",execute_id);
            return 0;
        }
        return sched.deleteExecute(execute_id);
    }



  
}
