
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

import java.util.Date;
import java.util.HashMap;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;


public class JobExecutionContextImpl implements java.io.Serializable, JobExecutionContext {

    private static final long serialVersionUID = -8139417614523942021L;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private transient Scheduler scheduler;

    private Trigger trigger;

    private JobDetail jobDetail;
    
    private JobDataMap jobDataMap;

    private transient Job job;
    
//    private Calendar calendar;

//    private boolean recovering = false;

    private int numRefires = 0;

    private Date fireTime;

    private Date scheduledFireTime;

    private Date prevFireTime;

    private Date nextFireTime;
    
    private long jobRunTime = -1;
    
    private Object result;
    
    private HashMap<Object, Object> data = new HashMap<Object, Object>();
    private String dataStr;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a JobExcecutionContext with the given context data.
     * </p>
     */
//    @Deprecated
//    public JobExecutionContextImpl(Scheduler scheduler, TriggerFiredBundle firedBundle, Job job) {
//        this.scheduler = scheduler;
//        this.trigger = firedBundle.getTrigger();
//        this.calendar = firedBundle.getCalendar();
//        this.jobDetail = firedBundle.getJobDetail();
//        this.job = job;
////        this.recovering = firedBundle.isRecovering();
//        this.fireTime = firedBundle.getFireTime();
//        this.scheduledFireTime = firedBundle.getScheduledFireTime();
//        this.prevFireTime = firedBundle.getPrevFireTime();
//        this.nextFireTime = firedBundle.getNextFireTime();
//
//        this.jobDataMap = new JobDataMap();
//        this.jobDataMap.putAll(jobDetail.getJobDataMap());
////        this.jobDataMap.putAll(trigger.getJobDataMap());
//    }
    public JobExecutionContextImpl(Scheduler scheduler, TriggerFiredBundle firedBundle, Job job) {
        this.scheduler = scheduler;
        this.trigger = firedBundle.getTrigger();
//        this.calendar = firedBundle.getCalendar();
        this.jobDetail = firedBundle.getJobDetail();
        this.job = job;
//        this.recovering = firedBundle.isRecovering();
        this.fireTime = firedBundle.getFireTime();
        this.scheduledFireTime = firedBundle.getScheduledFireTime();
        this.prevFireTime = firedBundle.getPrevFireTime();
        this.nextFireTime = firedBundle.getNextFireTime();

        this.jobDataMap = new JobDataMap();
        this.jobDataMap.putAll(jobDetail.getJobDataMap());
//        this.jobDataMap.putAll(trigger.getJobDataMap());
    }

//    public JobExecutionContextImpl(Scheduler scheduler,JobDetail jobDetail, Job job) {
//        this.scheduler = scheduler;
////        this.trigger = firedBundle.getTrigger();
////        this.calendar = firedBundle.getCalendar();
//        this.jobDetail = jobDetail;
//        this.job = job;
//        this.fireTime = jobDetail.getEJob().getFireTime();
//        this.scheduledFireTime = jobDetail.getEJob().getScheduledFireTime();
//        this.prevFireTime = new Date(jobDetail.getEJob().getPrevFireTime());
//        this.nextFireTime = new Date(jobDetail.getEJob().getNextFireTime());
//        // todo 考虑是否需要序列化
//        this.dataStr = jobDetail.getEJob().getJob().getJobData();
//        this.jobDataMap = new JobDataMap();
//        this.jobDataMap.putAll(jobDetail.getJobDataMap());
//    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Trigger getTrigger() {
        return trigger;
    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public Calendar getCalendar() {
//        return calendar;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public boolean isRecovering() {
//        return recovering;
//    }
//
//    @Override
//    public TriggerKey getRecoveringTriggerKey() {
//        if (isRecovering()) {
////            return new TriggerKey(jobDataMap.getString(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME),jobDataMap.getString(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_GROUP));
//            return new TriggerKey(jobDataMap.getString(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME));
//        } else {
//            throw new IllegalStateException("Not a recovering job");
//        }
//    }
    
    public void incrementRefireCount() {
        numRefires++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRefireCount() {
        return numRefires;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobDataMap getMergedJobDataMap() {
        return jobDataMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobDetail getJobDetail() {
        return jobDetail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Job getJobInstance() {
        return job;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getFireTime() {
        return fireTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getScheduledFireTime() {
        return scheduledFireTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getPreviousFireTime() {
        return prevFireTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getNextFireTime() {
        return nextFireTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getResult() {
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setResult(Object result) {
        this.result = result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getJobRunTime() {
        return jobRunTime;
    }
    
    /**
     * @param jobRunTime The jobRunTime to set.
     */
    public void setJobRunTime(long jobRunTime) {
        this.jobRunTime = jobRunTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(Object key, Object value) {
        data.put(key, value);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        return data.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFireInstanceId() {
        return ((OperableTrigger)trigger).getFireInstanceId();
    }
    @Override
    public String getDataStr(){
        return dataStr;
    }

    @Override
    public String toString() {
        return "JobExecutionContextImpl{" +
                "scheduler=" + scheduler +
                ", trigger=" + trigger +
                ", jobDetail=" + jobDetail +
                ", jobDataMap=" + jobDataMap +
                ", job=" + job +
//                ", calendar=" + calendar +
//                ", recovering=" + recovering +
                ", numRefires=" + numRefires +
                ", fireTime=" + fireTime +
                ", scheduledFireTime=" + scheduledFireTime +
                ", prevFireTime=" + prevFireTime +
                ", nextFireTime=" + nextFireTime +
                ", jobRunTime=" + jobRunTime +
                ", result=" + result +
                ", data=" + data +
                '}';
    }
}
