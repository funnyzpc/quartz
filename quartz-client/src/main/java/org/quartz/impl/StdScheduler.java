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

    private final QuartzScheduler sched;

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
    public Object[] addApp(QrtzApp qrtzApp){
        String state ;
        if( null==qrtzApp || null==qrtzApp.getApplication() ||"".equals(qrtzApp.getApplication())
            || null==(state=qrtzApp.getState()) || (!"N".equals(state) && !"Y".equals(state))
            /*|| null==qrtzApp.getTimePre() || null==qrtzApp.getTimeNext() || null== qrtzApp.getTimeInterval()*/){
            LOGGER.error("必要参数为空或异常! [qrtzApp] {}",qrtzApp);
            return new Object[]{0,"必要参数为空或异常"};
        }
        final String application = qrtzApp.getApplication();
        if(this.sched.getAppByApplication(application)!=null){
            LOGGER.error("相关记录已经存在! [qrtzApp(application)] {}",qrtzApp);
            return new Object[]{0,"相关记录已经存在"};
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
        int insertCount = sched.addApp(qrtzApp);
        return new Object[]{insertCount,null};
    }
    @Override
    public Object[] deleteApp(String application){
        if(null==application || "".equals(application=application.trim())){
            LOGGER.error("必要参数为空! [application]: {}",application);
            return new Object[]{0,"必要参数为空!"};
        }
        // 如果有node则必须先删除node
        if(sched.containsNode(application)){
            LOGGER.error("存在node，请先移除node后再行删除! [application]: {}",application);
            return new Object[]{0,"存在node，请先移除node后再行删除!"};
        }
        int deleteCount = sched.deleteApp(application);
        return new Object[]{deleteCount,null};
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
    public Object[] addNode(QrtzNode qrtzNode){
        String state;
        if(null==qrtzNode
                || null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                || null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))
                ){
            LOGGER.error("必要参数为空或状态异常! [qrtzNode]: {}",qrtzNode);
            return new Object[]{0,"必要参数为空或状态异常!"};
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
            return new Object[]{0,"对应应用配置为空!"};
        }
        // 对应node也必须唯一
        if(this.sched.containsNode(application,qrtzNode.getHostIp())){
            LOGGER.error("存在对应的node配置! [qrtzNode]: {}",qrtzNode);
            return new Object[]{0,"存在对应的node配置!"};
        }
        int insertCount = sched.addNode(qrtzNode);
        return new Object[]{insertCount,null};
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
    public Object[] addAppAndNode(QrtzApp qrtzApp, QrtzNode qrtzNode){
        String state ;
        if( null==qrtzApp || null==qrtzApp.getApplication() ||"".equals(qrtzApp.getApplication())
                || null==(state=qrtzApp.getState()) || (!"N".equals(state) && !"Y".equals(state))
                /*|| null==qrtzApp.getTimePre() || null==qrtzApp.getTimeNext() || null== qrtzApp.getTimeInterval()*/){
            LOGGER.error("必要参数为空或参数异常! [qrtzApp] {}",qrtzApp);
            return new Object[]{0,"必要参数为空或参数异常!"};
        }
        if(null==qrtzNode
                /*|| null==qrtzNode.getApplication() || "".equals(qrtzNode.getApplication())*/
                || null==qrtzNode.getHostIp() || "".equals(qrtzNode.getHostIp())
                /*|| null==qrtzNode.getHostName()*/
                /*|| null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))*/
                /*|| null==qrtzNode.getTimeCheck()*/ ){
            LOGGER.error("必要参数为空或异常! [qrtzNode] {}",qrtzNode);
            return new Object[]{0,"必要参数为空或异常!"};
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
        int insertCount = sched.addAppAndNode(qrtzApp,qrtzNode);
        return new Object[]{insertCount,null};
    }


    /////////////////////////////////////
    @Override
    public Object[] addJob(QrtzJob qrtzJob){
//        String[] states = {"EXECUTING", "PAUSED", "COMPLETE", "ERROR" ,"INIT"};
        String states = "EXECUTING,PAUSED,COMPLETE,ERROR,INIT";
        // 检查参数
        String state;
        if(null==qrtzJob || null==qrtzJob.getApplication() || null==(state=qrtzJob.getState()) || !states.contains(state) || null==qrtzJob.getJobClass() ){
            LOGGER.error("必要参数不可为空或参数异常:{}",qrtzJob);
            return new Object[]{0,"必要参数不可为空或参数异常"};
        }
        String job_data = qrtzJob.getJobData();
        JSONObject jo;
        JSONArray ja;
        if( null!=job_data
                && !"".equals(job_data=job_data.trim())
                && !((job_data.startsWith("{") && job_data.endsWith("}") && (jo = new JSONObject(job_data)) !=null && (job_data=jo.toString())!=null) ||
                (job_data.startsWith("[") && job_data.endsWith("]") && (ja = new JSONArray(job_data)) !=null) && (job_data=ja.toString())!=null) ){
            LOGGER.error("异常的任务数据：{}",qrtzJob);
            return new Object[]{0,"异常的任务数据"};
        }
        qrtzJob.setJobData(job_data);
        // 赋初始化参数
        qrtzJob.setId(SeqGenUtil.genSeq());
        qrtzJob.setUpdateTime(System.currentTimeMillis()/1000*1000);
        int insertCount = sched.addJob(qrtzJob);
        return new Object[]{insertCount,null};
    }

    @Override
    public Object[] updateJob(QrtzJob qrtzJob){
        // 检查参数
        if(null==qrtzJob || null==qrtzJob.getId() || null==qrtzJob.getApplication() || null==qrtzJob.getState() || null==qrtzJob.getJobClass() ){
            LOGGER.error("必要参数不可为空:{}",qrtzJob);
            return new Object[]{0,"必要参数不可为空"};
        }
        String job_data = qrtzJob.getJobData();
        JSONObject jo;
        JSONArray ja;
        if( null!=job_data
                && !"".equals(job_data=job_data.trim())
                && !((job_data.startsWith("{") && job_data.endsWith("}") && (jo = new JSONObject(job_data)) !=null && (job_data=jo.toString())!=null) ||
                (job_data.startsWith("[") && job_data.endsWith("]") && (ja = new JSONArray(job_data)) !=null) && (job_data=ja.toString())!=null) ){
            LOGGER.error("异常的任务数据：{}",qrtzJob);
            return new Object[]{0,"异常的任务数据"};
        }
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(null!=qrtzJob.getState() && !states.contains(","+qrtzJob.getState()+",") ){
            LOGGER.error("异常的状态项:{}",qrtzJob);
            return new Object[]{0,"异常的状态项"};
        }
        qrtzJob.setJobData(job_data);
        // 赋初始化参数
        qrtzJob.setUpdateTime(System.currentTimeMillis()/1000*1000);
        int updateCount = sched.updateJob(qrtzJob);
        return new Object[]{updateCount,null};
    }
    @Override
    public int deleteJob(String job_id){
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
    public Object[] updateJobStateInAll(String job_id, String state){
        if( null==job_id || null==state || "".equals(state=state.trim())  ){
            LOGGER.error("必要参数为空! [job_id] {},{}",job_id,state);
            return new Object[]{0,"必要参数为空(job_id、state)"};
        }
        if( !("EXECUTING".equals(state)  || "PAUSED".equals(state)) ){
            LOGGER.error("仅可操作状态为(EXECUTING or PAUSED)! [job_id] {},{}",job_id,state);
            return new Object[]{0,"仅可操作状态为(EXECUTING or PAUSED)!"};
        }
        final QrtzJob qrtzJob = sched.getJobInAllByJobId(job_id);
        if( null==qrtzJob || state.equals(qrtzJob.getState()) ){
            LOGGER.error("job状态已经是目标状态或job为空 {},{}",job_id,state);
            return new Object[]{0,"job状态已经是目标状态或job为空"};
        }
        final String bState = qrtzJob.getState();
        int ct = 0;
//        if("EXECUTING".equals(state) && ("PAUSED".equals(bState) || "INIT".equals(bState)) ){
        if("EXECUTING".equals(state) && ("PAUSED".equals(bState) || "INIT".equals(bState) || "COMPLETE".equals(bState)) ){
            // EXECUTING,PAUSED,COMPLETE,ERROR,INIT
            // PAUSED,INIT,COMPLETE -> EXECUTING   因为job存在重新添加执行项，所以也可以从 COMPLETE 调整为 EXECUTING
            if( (ct=sched.updateJobState(job_id,state))>0 ){
                for( QrtzExecute item:qrtzJob.getExecutes() ){
                    final String execute_id = item.getId();
                    final String execute_state = item.getState();
                    if("PAUSED".equals(execute_state) || "INIT".equals(execute_state)){
                        sched.updateExecuteState(execute_id,state);
                    }
                }
                return new Object[]{ct,null};
            }
            return new Object[]{0,"当前状态不可启动 "+bState};
        }
        if("PAUSED".equals(state) && ("EXECUTING".equals(bState) || "ERROR".equals(bState)) ){
            // EXECUTING,PAUSED,COMPLETE,ERROR,INIT
            // EXECUTING,ERROR -> PAUSED
            if( (ct=sched.updateJobState(job_id,state))>0 ){
                for( QrtzExecute item:qrtzJob.getExecutes() ){
                    final String execute_id = item.getId();
                    final String execute_state = item.getState();
                    if("EXECUTING".equals(execute_state) || "ERROR".equals(execute_state)){
                        sched.updateExecuteState(execute_id,state);
                    }
                }
                return new Object[]{ct,null};
            }
            return new Object[]{0,"当前状态不可暂停 "+bState};
        }
        return new Object[]{0,"操作失败!"};
    }

    @Override
    public int updateJobState(String job_id, String state){
        if( null==job_id || null==state || "".equals(state=state.trim()) /*|| (!"N".equals(state) && !"Y".equals(state))*/ ){
            LOGGER.error("必要参数为空! [job_id] {},{}",job_id,state);
            return 0;
        }
        // INIT状态只可在初次写入时候为INIT
        String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,";
        if(null==state || !states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{},{}",job_id,state);
            return 0;
        }
        return sched.updateJobState(job_id,state);
    }
    @Override
    public int updateExecuteState(String execute_id, String state){
        if( null==execute_id || null==state /*|| "".equals(state=state.trim()) || (!"N".equals(state) && !"Y".equals(state))*/ ){
            LOGGER.error("必要参数为空! [execute_id、state] {},{}",execute_id,state);
            return 0;
        }
        // 限制已经执行完成的编辑
//        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,";
        final String states = ",EXECUTING,PAUSED,ERROR,";
        if(!states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{},{}",execute_id,state);
            return 0;
        }
        return sched.updateExecuteState(execute_id,state);
    }
    @Override
    public Object[] addExecute(QrtzExecute qrtzExecute){
        String jobType;
        String state;
        if(null==qrtzExecute || null==qrtzExecute.getPid()
                || null==(jobType=qrtzExecute.getJobType()) || (!"CRON".equals(jobType) && !"SIMPLE".equals(jobType))
                || null==(state=qrtzExecute.getState()) /*|| (!"N".equals(state) && !"Y".equals(state))*/
                ){
            LOGGER.error("必要参数为空或参数异常! [qrtzExecute]:{}",qrtzExecute);
            return new Object[]{0,"必要参数为空或参数异常!"};
        }
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(!states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:{}",qrtzExecute);
            return new Object[]{0,"异常的状态项!(only:EXECUTING,PAUSED,COMPLETE,ERROR,INIT)"};
        }
        final String pid = qrtzExecute.getPid();
        if( sched.getJobByJobId(pid)==null){
            LOGGER.error("对应job配置为空! [qrtzExecute]:{}",qrtzExecute);
            return new Object[]{0,"对应job配置为空!"};
        }
        Long startTime = qrtzExecute.getStartTime();
        if(null==startTime || startTime<1){
            qrtzExecute.setStartTime(startTime=(System.currentTimeMillis()/1000*1000+10000));
        }
        final Date aft = new Date(System.currentTimeMillis() + 5000L);
        // 如果是 SIMPLE任务
        if("SIMPLE".equals(jobType)){
            if(qrtzExecute.getRepeatCount()==null || qrtzExecute.getRepeatCount()<1){
                qrtzExecute.setRepeatCount(-1);  // 永远重复
            }
            final Integer repeatInterval = qrtzExecute.getRepeatInterval();
//            final Long startTime = qrtzExecute.getStartTime();
            final Long endTime = qrtzExecute.getEndTime();
            final Integer repeatCount = qrtzExecute.getRepeatCount();
            if( null==repeatCount || null==repeatInterval /*|| null==qrtzExecute.getTimeTriggered()*/
                || null==qrtzExecute.getStartTime() || repeatInterval<10 ){
                LOGGER.error("SIMPLE任务参数异常! [qrtzExecute]:{}",qrtzExecute);
                return new Object[]{0,"SIMPLE任务参数异常!"};
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
                return new Object[]{0,"SIMPLE任务无效的任务配置!"};
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
//            final Long startTime = qrtzExecute.getStartTime();
            final Long endTime = qrtzExecute.getEndTime();
            if(null==cron || "".equals(cron.trim())){
                LOGGER.error("CRON任务表达式为空! [qrtzExecute]:{}",qrtzExecute);
                return new Object[]{0,"CRON任务表达式为空!"};
            }
            if(null==zoneId || "".equals(zoneId.trim()) || null==ZoneId.of(zoneId)){
                TimeZone zoneDefault = TimeZone.getDefault();
                qrtzExecute.setZoneId(zoneId=(null!=zoneDefault?zoneDefault.getID():"Asia/Shanghai"));
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
                    return new Object[]{0,"CRON任务无效的任务配置!"};
                }
                // SIMPLE任务参数清空
                qrtzExecute.setRepeatCount(null);
                qrtzExecute.setRepeatInterval(null);
//                qrtzExecute.setTimeTriggered(-1);
                qrtzExecute.setNextFireTime(nextFireTime.getTime());
            }catch (Exception e){
                e.printStackTrace();
                LOGGER.error("CRON任务配置异常:{}",qrtzExecute,e);
                return new Object[]{0,"CRON任务配置异常!"};
            }
        }
        if(qrtzExecute.getHostIp()==null){
            qrtzExecute.setHostIp(SystemPropGenerator.hostIP());
        }
        if(qrtzExecute.getHostName()==null){
            qrtzExecute.setHostName(SystemPropGenerator.hostName());
        }
        qrtzExecute.setPrevFireTime(-1L); // 新增都没有前一次执行时间的
        qrtzExecute.setId(SeqGenUtil.genSeq());
        int insert_count = sched.addExecute(qrtzExecute);
        return new Object[]{insert_count,"CRON任务配置异常!"};
    }
    @Override
    public int deleteExecute(String execute_id ){
        if(null==execute_id){
            LOGGER.error("必要参数为空! [execute_id]:{}",execute_id);
            return 0;
        }
        return sched.deleteExecute(execute_id);
    }

    /**
     * 更新执行项
     * @param qrtzExecute
     * @return [更新条数(Integer),异常消息(String)],更新条数>0即为更新成功
     */
    @Override
    public Object[] updateExecute(QrtzExecute qrtzExecute){
        String jobType;
        String state;
        if(null==qrtzExecute || null==qrtzExecute.getId() || null==qrtzExecute.getPid()
                || null==(jobType=qrtzExecute.getJobType()) || (!"CRON".equals(jobType) && !"SIMPLE".equals(jobType))
                || null==(state=qrtzExecute.getState())
        ){
            LOGGER.error("必要参数为空或参数异常! [qrtzExecute]:{}",qrtzExecute);
            return new Object[]{0,"必要参数为空或参数异常!"};
        }
        // 类型不可修改
        QrtzExecute qrtzExecute_ = this.sched.getExecuteByExecuteId(qrtzExecute.getId().toString());
        if( null==qrtzExecute_ || !jobType.equals(qrtzExecute_.getJobType()) ){
            return new Object[]{0,"执行项不存在或类型(SIMPLE、CRON)被修改!"};
        }
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(!states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:{}",qrtzExecute);
            return new Object[]{0,"异常的状态项[state]!"};
        }
        final String pid = qrtzExecute.getPid();
        if( sched.getJobByJobId(pid)==null){
            LOGGER.error("对应job配置为空! [qrtzExecute]:{}",qrtzExecute);
            return new Object[]{0,"对应job配置为空"};
        }
        Long startTime = qrtzExecute.getStartTime();
        if(null==startTime || startTime<1){
            qrtzExecute.setStartTime(startTime=(System.currentTimeMillis()/1000*1000+10000));
        }
        final Date aft = new Date(System.currentTimeMillis() + 5000L);
        // 如果是 SIMPLE任务
        if("SIMPLE".equals(jobType)){
            if(qrtzExecute.getRepeatCount()==null || qrtzExecute.getRepeatCount()<1){
                qrtzExecute.setRepeatCount(-1);  // 永远重复
            }
            final Integer repeatInterval = qrtzExecute.getRepeatInterval();
//            final Long startTime = qrtzExecute.getStartTime();
            final Long endTime = qrtzExecute.getEndTime();
            final Integer repeatCount = qrtzExecute.getRepeatCount();
            if( null==repeatCount || null==repeatInterval
                    || null==qrtzExecute.getStartTime() || repeatInterval<10 ){
                LOGGER.error("SIMPLE任务参数异常! [qrtzExecute]:{}",qrtzExecute);
                return new Object[]{0,"SIMPLE任务参数异常"};
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
                return new Object[]{0,"SIMPLE任务无效的任务配置"};
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
//            final Long startTime = qrtzExecute.getStartTime();
            final Long endTime = qrtzExecute.getEndTime();
            if(null==cron || "".equals(cron.trim())){
                LOGGER.error("CRON任务表达式为空! [qrtzExecute]:{}",qrtzExecute);
                return new Object[]{0,"CRON任务表达式为空"};
            }
            if(null==zoneId || "".equals(zoneId.trim()) || null==ZoneId.of(zoneId)){
                TimeZone zoneDefault = TimeZone.getDefault();
                qrtzExecute.setZoneId(zoneId=(null!=zoneDefault?zoneDefault.getID():"Asia/Shanghai"));
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
                    return new Object[]{0,"CRON任务无效的任务配置"};
                }
                // SIMPLE任务参数清空
                qrtzExecute.setRepeatCount(null);
                qrtzExecute.setRepeatInterval(null);
//                qrtzExecute.setTimeTriggered(-1);
                qrtzExecute.setNextFireTime(nextFireTime.getTime());
            }catch (Exception e){
                e.printStackTrace();
                LOGGER.error("CRON任务配置异常:{}",qrtzExecute,e);
                return new Object[]{0,"CRON任务配置异常"};
            }
        }
        if(qrtzExecute.getHostIp()==null){
            qrtzExecute.setHostIp(SystemPropGenerator.hostIP());
        }
        if(qrtzExecute.getHostName()==null){
            qrtzExecute.setHostName(SystemPropGenerator.hostName());
        }
        int updateCount = sched.updateExecute(qrtzExecute);
        return new Object[]{updateCount,null};
    }



  
}
