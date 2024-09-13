
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

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * JobRunShell instances are responsible for providing the 'safe' environment
 * for <code>Job</code> s to run in, and for performing all of the work of
 * executing the <code>Job</code>, catching ANY thrown exceptions, updating
 * the <code>Trigger</code> with the <code>Job</code>'s completion code,
 * etc.
 * </p>
 *
 * <p>
 * A <code>JobRunShell</code> instance is created by a <code>JobRunShellFactory</code>
 * on behalf of the <code>QuartzSchedulerThread</code> which then runs the
 * shell in a thread from the configured <code>ThreadPool</code> when the
 * scheduler determines that a <code>Job</code> has been triggered.
 * </p>
 *
 * @see JobRunShellFactory
 * @see org.quartz.core.QuartzSchedulerThread
 * @see org.quartz.Job
 * @see org.quartz.Trigger
 *
 * @author James House
 */
public class JobRunShell /*extends SchedulerListenerSupport*/ implements Runnable {
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Data members.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected JobExecutionContextImpl jec = null;

    protected QuartzScheduler qs = null;
//    @Deprecated
//    protected TriggerFiredBundle firedTriggerBundle = null;
    private QrtzExecute eJob = null;
//    private Class<? extends Job> jobClass = null;

    protected Scheduler scheduler = null;

    protected volatile boolean shutdownRequested = false;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constructors.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a JobRunShell instance with the given settings.
     * </p>
     *
     * @param scheduler
     *          The <code>Scheduler</code> instance that should be made
     *          available within the <code>JobExecutionContext</code>.
     */
//    @Deprecated
//    public JobRunShell(Scheduler scheduler, TriggerFiredBundle bndle) {
//        this.scheduler = scheduler;
//        this.firedTriggerBundle = bndle;
//    }
    public JobRunShell(Scheduler scheduler, QrtzExecute eJob,Class<? extends Job> jobClass) {
        this.scheduler = scheduler;
//        this.firedTriggerBundle = bndle;
        this.eJob = eJob;
//        this.jobClass=jobClass;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

//    @Override
//    public void schedulerShuttingdown() {
//        requestShutdown();
//    }

//    @Override
//    protected Logger getLog() {
//        return log;
//    }

//    public void initialize(QuartzScheduler sched) throws SchedulerException {
//        this.qs = sched;
//        Job job = null;
//        JobDetail jobDetail = firedTriggerBundle.getJobDetail();
//        try {
//            // 创建job实例并补充上下文及参数
//            job = sched.getJobFactory().newJob(firedTriggerBundle, scheduler);
//        } catch (SchedulerException se) {
//            log.error("An error occured instantiating job to be executed. job= '" + jobDetail.getKey() + "'", se);
////            sched.notifySchedulerListenersError("An error occured instantiating job to be executed. job= '" + jobDetail.getKey() + "'", se);
//            throw se;
//        } catch (Throwable ncdfe) { // such as NoClassDefFoundError
//            SchedulerException se = new SchedulerException("Problem instantiating class '" + jobDetail.getJobClass().getName() + "' - ", ncdfe);
////            sched.notifySchedulerListenersError("An error occured instantiating job to be executed. job= '" + jobDetail.getKey() + "'", se);
//            throw se;
//        }
//        this.jec = new JobExecutionContextImpl(scheduler, firedTriggerBundle, job);
//    }
    public void initialize(QuartzScheduler sched) throws SchedulerException {
        this.qs = sched;
        Job job = null;
        final String keyNote = this.eJob.getJob().getId()+"#"+ this.eJob.getId()+"#"+this.eJob.getJob().getJobClass();
        final JobDetail jobDetail = new JobDetailImpl(this.eJob.getJobClazz(), this.eJob.getJob().getJobClass(), keyNote);
        final OperableTrigger trigger = "CRON".equals(this.eJob.getJobType())?new CronTriggerImpl(): new SimpleTriggerImpl();
//        TriggerFiredBundle bundle = new TriggerFiredBundle(jobDetail,trigger,null,fireTime,scheduledFireTime,prevFireTime,nextFireTime);
        final TriggerFiredBundle bundle = new TriggerFiredBundle(jobDetail,trigger);
        try {
            // 创建job实例并补充上下文及参数
            job = sched.getJobFactory().newJob(bundle,this.scheduler);
        } catch (SchedulerException se) {
            log.error("An error occured instantiating job to be executed. job= '" + jobDetail.getKeyNote() + "'", se);
            throw se;
        } catch (Throwable ncdfe) { // such as NoClassDefFoundError
            SchedulerException se = new SchedulerException("Problem instantiating class '" + jobDetail.getJobClassName() + "' - ", ncdfe);
            throw se;
        }
        this.jec = new JobExecutionContextImpl(this.scheduler,job,keyNote,this.eJob);
    }

    public void requestShutdown() {
        shutdownRequested = true;
    }

    @Override
    public void run() {
//        qs.addInternalSchedulerListener(this);
        try {
//            OperableTrigger trigger = (OperableTrigger) jec.getTrigger();
//            JobDetail jobDetail = jec.getJobDetail();
            do {
                JobExecutionException jobExEx = null;
                Job job = jec.getJobInstance();
                try {
                    begin();
                } catch (SchedulerException se) {
                    log.error("Error executing Job (" + jec.getKeyNote() + "): couldn't begin execution.", se);
//                    qs.notifySchedulerListenersError("Error executing Job ("
//                            + jec.getJobDetail().getKey()
//                            + ": couldn't begin execution.", se);
                    break;
                }

                long startTime = System.currentTimeMillis();
                long endTime = startTime;

                // execute the job
                try {
//                    log.debug("Calling execute on job " + jobDetail.getKeyNote()); // JOB_ID#JOB_CLASS#EXECUTE_ID
                    job.execute(jec); // 执行
                    endTime = System.currentTimeMillis();
                } catch (JobExecutionException jee) {
                    endTime = System.currentTimeMillis();
                    jobExEx = jee;
//                    log.info("Job " + jobDetail.getKeyNote() + " threw a JobExecutionException: ", jobExEx);
                } catch (Throwable e) {
                    endTime = System.currentTimeMillis();
//                    log.error("Job " + jobDetail.getKeyNote() + " threw an unhandled Exception: ", e);
                    SchedulerException se = new SchedulerException("Job threw an unhandled exception.", e);
                    jobExEx = new JobExecutionException(se, false);//第二个参数refireImmediately: true.立即点火 false.不点火
                }
                jec.setJobRunTime(endTime - startTime); // 执行时间

//                CompletedExecutionInstruction instCode = CompletedExecutionInstruction.NOOP;
//                // update the trigger
//                try {
//                    instCode = trigger.executionComplete(jec, jobExEx); /** 获取指示状态 @org.quartz.Trigger.CompletedExecutionInstruction **/
//                } catch (Exception e) {
//                    // If this happens, there's a bug in the trigger...
//                    SchedulerException se = new SchedulerException("Trigger threw an unhandled exception.", e);
//                    log.error("Please report this error to the Quartz developers.", se);
////                    qs.notifySchedulerListenersError("Please report this error to the Quartz developers.", se);
//                }

//                // update job/trigger or re-execute job  重新执行任务
//                if (instCode == CompletedExecutionInstruction.RE_EXECUTE_JOB) {
//                    jec.incrementRefireCount();
//                    try {
//                        complete(false);
//                    } catch (SchedulerException se) {
//                        se.printStackTrace();
//                        log.error("Error executing Job (" + jec.getJobDetail().getKey() + ": couldn't finalize execution.", se);
////                        qs.notifySchedulerListenersError("Error executing Job (" + jec.getJobDetail().getKey() + ": couldn't finalize execution.", se);
//                    }
//                    continue;
//                }
                // 判断是否重新执行任务
                if(null!=jobExEx && (jobExEx instanceof JobExecutionException) && jobExEx.refireImmediately() && jec.getRefireCount()<3 ){
                    jec.incrementRefireCount(); // 必须要+1
                    try {
                        complete(false);
                    } catch (SchedulerException se) {
                        se.printStackTrace();
//                        log.error("Error executing Job (" + jec.getJobDetail().getKey() + ": couldn't finalize execution.", se);
                    }
                    continue;
                }
                try {
                    complete(true);
                } catch (SchedulerException se) {
                    // 不管怎么样都得退出，否则会重复执行
                    se.printStackTrace();
//                    log.error("Error executing Job (" + jec.getJobDetail().getKey() + ": couldn't finalize execution.", se);
//                    qs.notifySchedulerListenersError("Error executing Job (" + jec.getJobDetail().getKey() + ": couldn't finalize execution.", se);
//                    continue;
                }
                break;
            } while (true);

        }catch (Exception e){
            e.printStackTrace();
            log.error("异常:",e);
        }
//        finally {
//            qs.removeInternalSchedulerListener(this);
//        }
    }

    protected void begin() throws SchedulerException {
    }

    protected void complete(boolean successfulExecution) throws SchedulerException {
    }

    public void passivate() {
        jec = null;
        qs = null;
    }

}
