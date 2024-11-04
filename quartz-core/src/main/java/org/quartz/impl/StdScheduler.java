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

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.json.JSONArray;
import org.quartz.json.JSONObject;
import org.quartz.simpl.SeqGenUtil;
import org.quartz.simpl.SystemPropGenerator;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that directly
 * proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code>
 * instance.
 * </p>
 * 
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzScheduler
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
    public StdScheduler(QuartzScheduler sched) {
        this.sched = sched;
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
     * Returns the name of the <code>Scheduler</code>.
     * </p>
     */
    @Override
    public String getSchedulerName() {
        return sched.getSchedulerName();
    }

//    /**
//     * <p>
//     * Returns the instance Id of the <code>Scheduler</code>.
//     * </p>
//     */
//    @Override
//    public String getSchedulerInstanceId() {
//        return sched.getSchedulerInstanceId();
//    }
//    @Override
//    public SchedulerMetaData getMetaData() {
//        return new SchedulerMetaData(getSchedulerName(),
//                getSchedulerInstanceId(), getClass(), false, isStarted(),
//                isInStandbyMode(), isShutdown(), sched.runningSince(),
//                sched.numJobsExecuted(), sched.getJobStoreClass(),
//                sched.supportsPersistence(), sched.isClustered(), sched.getThreadPoolClass(),
//                sched.getThreadPoolSize(), sched.getVersion());
//
//    }
//
//    /**
//     * <p>
//     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
//     * </p>
//     */
//    @Override
//    public SchedulerContext getContext() throws SchedulerException {
//        return sched.getSchedulerContext();
//    }
//
    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Schedululer State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void start() throws SchedulerException {
        sched.start();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void startDelayed(int seconds) throws SchedulerException {
        sched.startDelayed(seconds);
    }


    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void standby() {
        sched.standby();
    }
    
    /**
     * Whether the scheduler has been started.  
     * 
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever
     * been called on this Scheduler, so it will return <code>true</code> even 
     * if the <code>Scheduler</code> is currently in standby mode or has been 
     * since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */
    @Override
    public boolean isStarted() {
        return (sched.runningSince() != null);
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean isInStandbyMode() {
        return sched.isInStandbyMode();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void shutdown() {
        sched.shutdown();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        sched.shutdown(waitForJobsToComplete);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean isShutdown() {
        return sched.isShutdown();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<JobExecutionContext> getCurrentlyExecutingJobs() {
        return sched.getCurrentlyExecutingJobs();
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void clear() throws SchedulerException {
//        sched.clear();
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
//        return sched.scheduleJob(jobDetail, trigger);
//    }
//    @Override
//    public Date scheduleJobAndExecute(JobCfg jobCfg, ExecuteCfg executeCfg) throws SchedulerException {
//        return sched.scheduleJob(jobCfg,executeCfg);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Date scheduleJob(Trigger trigger) throws SchedulerException {
//        return sched.scheduleJob(trigger);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void addJob(JobDetail jobDetail, boolean replace)throws SchedulerException {
//        sched.addJob(jobDetail, replace);
//    }
//    @Override
//    public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException {
//        sched.addJob(jobDetail, replace, storeNonDurableWhileAwaitingScheduling);
//    }
//
//    @Override
//    public boolean deleteJobs(List<Key> keys) throws SchedulerException {
//        return sched.deleteJobs(keys);
//    }
//    @Override
//    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {
//        sched.scheduleJobs(triggersAndJobs, replace);
//    }
//    @Override
//    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException {
//        sched.scheduleJob(jobDetail,  triggersForJob, replace);
//    }
//    @Override
//    public boolean unscheduleJobs(List<Key> keys) throws SchedulerException {
//        return sched.unscheduleJobs(keys);
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public boolean deleteJob(Key jobKey) throws SchedulerException {
//        return sched.deleteJob(jobKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public boolean unscheduleJob(Key key) throws SchedulerException {
//        return sched.unscheduleJob(key);
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     *  在“proxyed”QuartzScheduler上调用等效方法。 (重新安排作业)
//     * </p>
//     */
//    @Override
//    public Date rescheduleJob(Key triggerKey,Trigger newTrigger) throws SchedulerException {
//        return sched.rescheduleJob(triggerKey, newTrigger);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void triggerJob(Key jobKey) throws SchedulerException {
//        triggerJob(jobKey, null);
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void triggerJob(Key jobKey, JobDataMap data) throws SchedulerException {
//        sched.triggerJob(jobKey, data);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void pauseTrigger(Key triggerKey) throws SchedulerException {
//        sched.pauseTrigger(triggerKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
//        sched.pauseTriggers(matcher);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void pauseJob(Key key) throws SchedulerException {
//        sched.pauseJob(key);
//    }

//    /**
//     * @see org.quartz.Scheduler#getPausedTriggerGroups()
//     */
//    public Set<String> getPausedTriggerGroups() throws SchedulerException {
//        return sched.getPausedTriggerGroups();
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * triggerName allowed empty!
//     * </p>
//     */
//    @Override
//    public void pauseJobs(final String triggerName) throws SchedulerException {
//        sched.pauseJobs(triggerName);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeTrigger(Key triggerKey) throws SchedulerException {
//        sched.resumeTrigger(triggerKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeTriggers(GroupMatcher<Key<?>> matcher) throws SchedulerException {
//        sched.resumeTriggers(matcher);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeJob(Key key) throws SchedulerException {
//        sched.resumeJob(key);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeJobs(final String triggerName) throws SchedulerException {
//        sched.resumeJobs(triggerName);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Deprecated
//    public void pauseAll() throws SchedulerException {
//        sched.pauseAll();
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void resumeAll() throws SchedulerException {
//        sched.resumeAll();
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public List<String> getJobGroupNames() throws SchedulerException {
//        return sched.getJobGroupNames();
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public List<? extends Trigger> getTriggersOfJob(Key jobKey) throws SchedulerException {
//        return sched.getTriggersOfJob(jobKey);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public List<String> getTriggerGroupNames() throws SchedulerException {
//        return sched.getTriggerGroupNames();
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Set<Key> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
//        return sched.getTriggerKeys(matcher);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public JobDetail getJobDetail(Key jobKey)throws SchedulerException {
//        return sched.getJobDetail(jobKey);
//    }
//
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Trigger getTrigger(Key triggerKey) throws SchedulerException {
//        return sched.getTrigger(triggerKey);
//    }
//    /**
//     * Reset the current state of the identified <code>{@link Trigger}</code>
//     * from {@link TriggerState#ERROR} to {@link TriggerState#NORMAL} or
//     * {@link TriggerState#PAUSED} as appropriate.
//     *
//     * <p>Only affects triggers that are in ERROR state - if identified trigger is not
//     * in that state then the result is a no-op.</p>
//     *
//     * <p>The result will be the trigger returning to the normal, waiting to
//     * be fired state, unless the trigger's group has been paused, in which
//     * case it will go into the PAUSED state.</p>
//     *
//     * @see Trigger.TriggerState
//     */
//    @Override
//    public void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException {
//        sched.resetTriggerFromErrorState(triggerKey);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException {
//        sched.addCalendar(calName, calendar, replace, updateTriggers);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public boolean deleteCalendar(String calName) throws SchedulerException {
//        return sched.deleteCalendar(calName);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public Calendar getCalendar(String calName) throws SchedulerException {
//        return sched.getCalendar(calName);
//    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    @Override
//    public List<String> getCalendarNames() throws SchedulerException {
//        return sched.getCalendarNames();
//    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Other Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    

    /**
     * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
     */
    @Override
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        sched.setJobFactory(factory);
    }

//    /**
//     * @see org.quartz.Scheduler#getListenerManager()
//     */
//    @Override
//    public ListenerManager getListenerManager() throws SchedulerException {
//        return sched.getListenerManager();
//    }
//    @Override
//    public boolean interrupt(Key jobKey) throws UnableToInterruptJobException {
//        return sched.interrupt(jobKey);
//    }
//    @Override
//    public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
//        return sched.interrupt(fireInstanceId);
//    }

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
        if("EXECUTING".equals(state) && ("PAUSED".equals(bState) || "INIT".equals(bState)) ){
            // EXECUTING,PAUSED,COMPLETE,ERROR,INIT
            // PAUSED,INIT -> EXECUTING
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
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,";
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
            qrtzExecute.setStartTime(startTime=System.currentTimeMillis()/1000*1000);
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
            if(null==zoneId || "".equals(zoneId.trim()) || null== ZoneId.of(zoneId)){
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
            qrtzExecute.setStartTime(startTime=System.currentTimeMillis()/1000*1000);
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
