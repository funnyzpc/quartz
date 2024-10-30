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
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.json.JSONArray;
import org.quartz.json.JSONObject;
import org.quartz.simpl.SeqGenUtil;
import org.quartz.simpl.SystemPropGenerator;
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
            /*|| null==qrtzApp.getTimePre() || null==qrtzApp.getTimeNext() || null== qrtzApp.getTimeInterval()*/){
            LOGGER.error("必要参数为空或异常! [qrtzApp] {}",qrtzApp);
            return 0;
        }
        final String application = qrtzApp.getApplication();
        if(this.sched.getAppByApplication(application)!=null){
            LOGGER.error("相关记录已经存在! [qrtzApp(application)] {}",qrtzApp);
            return 0;
        }
        if(null==qrtzApp.getTimePre()){
            qrtzApp.setTimePre(-1L);
        }
        if(null==qrtzApp.getTimeNext()){
            qrtzApp.setTimeNext(System.currentTimeMillis()/1000*1000);
        }
        if(null==qrtzApp.getTimeInterval()){
            qrtzApp.setTimeInterval(15000L); // 15S
        }
        return sched.addApp(qrtzApp);
    }
    @Override
    public int deleteApp(String application){
        if(null==application || "".equals(application=application.trim())){
            LOGGER.error("必要参数为空! [application]: {}",application);
            return 0;
        }
        // 如果有node则必须先删除node
        if(sched.containsNode(application)){
            LOGGER.error("存在node，请先移除node后再行删除! [application]: {}",application);
            return 0;
        }
        return sched.deleteApp(application);
    }
    @Override
    public int updateAppState(String application,String state){
        if(null==application || "".equals(application=application.trim())
            || null==state || (!"N".equals(state) && !"Y".equals(state))){
            LOGGER.error("必要参数为空或状态非法! [application、state]: {},{}",application,state);
            return 0;
        }
        QrtzApp app = null ;
        if( (app=sched.getAppByApplication(application))!=null && state.equals(app.getState()) ){
            LOGGER.error("app已经是目标状态了! [application:{}、state:{}]",application,state);
            return 0;
        }
        // 更新app状态需要同步更新对应node节点状态
        return sched.updateAppState(application,state);
    }
    @Override
    public int addNode(QrtzNode qrtzNode){
        String state;
        if(null==qrtzNode
                || null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                || null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))
                ){
            LOGGER.error("必要参数为空或状态异常! [qrtzNode]: {}",qrtzNode);
            return 0;
        }
        if(null==qrtzNode.getHostName()){
            qrtzNode.setHostName(qrtzNode.getHostIp());
        }
        if(qrtzNode.getTimeCheck()==null){
            qrtzNode.setTimeCheck(-1L);
        }
        final String application = qrtzNode.getApplication();
        // 对应app不可为空
        if( this.sched.getAppByApplication(application)==null ){
            LOGGER.error("对应应用配置为空! [qrtzApp]: {}",qrtzNode);
            return 0;
        }
        // 对应node也必须唯一
        if(this.sched.containsNode(application,qrtzNode.getHostIp())){
            LOGGER.error("存在对应的node配置! [qrtzNode]: {}",qrtzNode);
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
    public int updateNode(QrtzNode qrtzNode){
        String state;
        if( null==qrtzNode
                || null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                || null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))
        ){
            LOGGER.error("参数为空或参数异常:{}",qrtzNode);
            return 0;
        }
        // host_name 不可为空
        if(qrtzNode.getHostName()==null){
            qrtzNode.setHostName(qrtzNode.getHostIp());
        }
        return sched.updateNode(qrtzNode);
    }
    @Override
    public int addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode){
        String state ;
        if( null==qrtzApp || null==qrtzApp.getApplication() ||"".equals(qrtzApp.getApplication())
                || null==(state=qrtzApp.getState()) || (!"N".equals(state) && !"Y".equals(state))
                /*|| null==qrtzApp.getTimePre() || null==qrtzApp.getTimeNext() || null== qrtzApp.getTimeInterval()*/){
            LOGGER.error("必要参数为空或参数异常! [qrtzApp] {}",qrtzApp);
            return 0;
        }
        if(null==qrtzNode
                /*|| null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())*/
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                /*|| null==qrtzNode.getHostName()*/
                /*|| null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))*/
                /*|| null==qrtzNode.getTimeCheck()*/ ){
            LOGGER.error("必要参数为空或异常! [qrtzNode] {}",qrtzNode);
            return 0;
        }
        // 补充默认参数
        if(null==qrtzApp.getTimePre()){
            qrtzApp.setTimePre(-1L);
        }
        if(null==qrtzApp.getTimeNext()){
            qrtzApp.setTimeNext(System.currentTimeMillis()/1000*1000);
        }
        if(null==qrtzApp.getTimeInterval()){
            qrtzApp.setTimeInterval(15000L); // 15S
        }
        if(qrtzNode.getHostName()==null){
            qrtzNode.setHostName(qrtzNode.getHostIp());
        }
        if(null==qrtzNode.getTimeCheck()){
            qrtzNode.setTimeCheck(-1L);
        }
        // 保持一致
        qrtzNode.setApplication(qrtzApp.getApplication());
        qrtzNode.setState(qrtzApp.getState());
        return sched.addAppAndNode(qrtzApp,qrtzNode);
    }


    /////////////////////////////////////
    @Override
    public int addJob(QrtzJob qrtzJob){
//        String[] states = {"EXECUTING", "PAUSED", "COMPLETE", "ERROR" ,"INIT"};
        String states = "EXECUTING,PAUSED,COMPLETE,ERROR,INIT";
        // 检查参数
        String state;
        if(null==qrtzJob || null==qrtzJob.getApplication() || null==(state=qrtzJob.getState()) || !states.contains(state) || null==qrtzJob.getJobClass() ){
            LOGGER.error("必要参数不可为空或参数异常:{}",qrtzJob);
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
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(null!=qrtzJob.getState() && !states.contains(","+qrtzJob.getState()+",") ){
            LOGGER.error("异常的状态项:{}",qrtzJob);
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
        // 先查询execute，如果有execute存在则不可删除
        if(sched.containsExecute(job_id)){
            LOGGER.error("存在execute记录，请先移除后再行删除job! [job_id] {}",job_id);
            return 0;
        }
        return sched.deleteJob(job_id);
    }

    @Override
    public int updateExecuteStateByJobId(Long job_id,String state){
        if( null==job_id || null==state || "".equals(state=state.trim()) /*|| (!"N".equals(state) && !"Y".equals(state))*/ ){
            LOGGER.error("必要参数为空! [job_id] {},{}",job_id,state);
            return 0;
        }
        String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(null==state || !states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{},{}",job_id,state);
            return 0;
        }
        return sched.updateExecuteStateByJobId(job_id,state);
    }
    @Override
    public int updateExecuteStateByExecuteId(Long execute_id,String state){
        if( null==execute_id || null==state /*|| "".equals(state=state.trim()) || (!"N".equals(state) && !"Y".equals(state))*/ ){
            LOGGER.error("必要参数为空! [execute_id、state] {},{}",execute_id,state);
            return 0;
        }
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(!states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{},{}",execute_id,state);
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
                || null==(state=qrtzExecute.getState()) /*|| (!"N".equals(state) && !"Y".equals(state))*/
                ){
            LOGGER.error("必要参数为空或参数异常! [qrtzExecute]:{}",qrtzExecute);
            return 0;
        }
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(!states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:{}",qrtzExecute);
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
            if( null==repeatCount || null==repeatInterval /*|| null==qrtzExecute.getTimeTriggered()*/
                || null==qrtzExecute.getStartTime() || repeatInterval<10 ){
                LOGGER.error("SIMPLE任务参数异常! [qrtzExecute]:{}",qrtzExecute);
                return 0;
            }
            SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl()
                    .setStartTime(new Date(startTime>0?startTime:System.currentTimeMillis()/1000*1000))
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
            // CRON任务参数清空
            qrtzExecute.setCron(null);
            qrtzExecute.setZoneId(null);
            if(qrtzExecute.getRepeatCount()>0){
                qrtzExecute.setTimeTriggered(0);
            }else{
                qrtzExecute.setTimeTriggered(-1); // 没有次数限制
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
                // SIMPLE任务参数清空
                qrtzExecute.setRepeatCount(null);
                qrtzExecute.setRepeatInterval(null);
//                qrtzExecute.setTimeTriggered(-1);
                qrtzExecute.setNextFireTime(nextFireTime.getTime());
            }catch (Exception e){
                e.printStackTrace();
                LOGGER.error("CRON任务配置异常:{}",qrtzExecute,e);
                return 0;
            }
        }
        if(qrtzExecute.getHostIp()==null){
            qrtzExecute.setHostIp(SystemPropGenerator.hostIP());
        }
        if(qrtzExecute.getHostName()==null){
            qrtzExecute.setHostName(SystemPropGenerator.hostName());
        }
        qrtzExecute.setId(SeqGenUtil.shortKey());
        return sched.addExecute(qrtzExecute);
    }
    @Override
    public int deleteExecute(String execute_id ){
        if(null==execute_id){
            LOGGER.error("必要参数为空! [execute_id]:{}",execute_id);
            return 0;
        }
        return sched.deleteExecute(execute_id);
    }



  
}
