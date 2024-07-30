
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The thread responsible for performing the work of firing <code>{@link Trigger}</code>
 * s that are registered with the <code>{@link QuartzScheduler}</code>.
 * </p>
 *
 * @see QuartzScheduler
 * @see org.quartz.Job
 * @see Trigger
 *
 * @author James House
 */
public class QuartzSchedulerThread extends Thread {
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Data members.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private QuartzScheduler qs;

    private QuartzSchedulerResources qsRsrcs;

    private final Object sigLock = new Object();

    private boolean signaled;
    private long signaledNextFireTime;

    private boolean paused;

    // 停止标志位
    private AtomicBoolean halted;

    private Random random = new Random(System.currentTimeMillis());

    // When the scheduler finds there is no current trigger to fire, how long
    // it should wait until checking again...
    private static long DEFAULT_IDLE_WAIT_TIME = 30L * 1000L;

    private long idleWaitTime = DEFAULT_IDLE_WAIT_TIME;

    private int idleWaitVariablness = 7 * 1000;

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
     * Construct a new <code>QuartzSchedulerThread</code> for the given
     * <code>QuartzScheduler</code> as a non-daemon <code>Thread</code>
     * with normal priority.
     *  为给定的QuartzScheduler构造一个新的QuartzSchedulerThread，作为具有正常优先级的非守护线程。
     * </p>
     */
    QuartzSchedulerThread(QuartzScheduler qs, QuartzSchedulerResources qsRsrcs) {
        this(qs, qsRsrcs, qsRsrcs.getMakeSchedulerThreadDaemon(), Thread.NORM_PRIORITY);
    }

    /**
     * <p>
     * Construct a new <code>QuartzSchedulerThread</code> for the given
     * <code>QuartzScheduler</code> as a <code>Thread</code> with the given
     * attributes.
     *  为给定的QuartzScheduler构造一个新的QuartzSchedulerThread作为具有给定属性的线程。
     * </p>
     */
    QuartzSchedulerThread(QuartzScheduler qs, QuartzSchedulerResources qsRsrcs, boolean setDaemon, int threadPrio) {
        // 设置线程组以及线程名称
        super(qs.getSchedulerThreadGroup(), qsRsrcs.getThreadName());
        this.qs = qs;
        this.qsRsrcs = qsRsrcs;
        this.setDaemon(setDaemon);//默认为false（非守护线程）
        if(qsRsrcs.isThreadsInheritInitializersClassLoadContext()) {
            log.info("QuartzSchedulerThread Inheriting ContextClassLoader of thread: " + Thread.currentThread().getName());
            this.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
        this.setPriority(threadPrio); // 线程默认优先级设置为中(5)

        // start the underlying thread, but put this object into the 'paused' 启动底层线程，但将此对象放入“暂停”状态
        // state
        // so processing doesn't start yet... 所以处理还没有开始。。。
        paused = true; // 这里置为 true run中获取到锁后会wait
        halted = new AtomicBoolean(false); // 设置停止标志位
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    void setIdleWaitTime(long waitTime) {
        idleWaitTime = waitTime;
        idleWaitVariablness = (int) (waitTime * 0.2);
    }

    private long getRandomizedIdleWaitTime() {
        // 30S(默认) - 随机4位数的int， idleWaitVariablness 是跳跃值
        return idleWaitTime - random.nextInt(idleWaitVariablness);
    }

    /**
     * 切换暂停
     * <p>
     * Signals the main processing loop to pause at the next possible point.
     *  通知主处理循环在下一个可能的点暂停。
     * </p>
     */
    void togglePause(boolean pause) {
        synchronized (sigLock) {
            paused = pause;
            if (paused) {
                // 里面会调用唤醒： notifyAll
                signalSchedulingChange(0);
            } else {
                sigLock.notifyAll();
            }
        }
    }

    /**
     * <p>
     * Signals the main processing loop to pause at the next possible point.
     *  通知主处理循环在下一个可能的点暂停
     * </p>
     */
    void halt(boolean wait) {
        // halted : 停止
        synchronized (sigLock) {
            halted.set(true);
            if (paused) {
                // 这里是通知其他wait的地方结束wait，同时这里是不阻塞的
                sigLock.notifyAll();
            } else {
                // 里面会调用唤醒： notifyAll
                signalSchedulingChange(0);
            }
        }
        
        if (wait) {
            boolean interrupted = false;
            try {
                while (true) {
                    try {
                        // join阻塞操作，直到被唤醒
                        join();
                        break;
                    } catch (InterruptedException _) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    // 如果是中断结束的，则在此补充终端为true 与此相反的是： Thread.interrupted();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    boolean isPaused() {
        return paused;
    }

    /**
     *  信号调度变更(认定下一次点火时间)
     * <p>
     * Signals the main processing loop that a change in scheduling has been
     * made - in order to interrupt any sleeping that may be occuring while
     * waiting for the fire time to arrive.
     *
     * 向主处理循环发出信号，表示调度已发生变化，以中断在等待点火时间到来时可能发生的任何睡眠。 （就是准备终端睡眠）
     * </p>
     *
     * @param candidateNewNextFireTime the time (in millis) when the newly scheduled trigger
     * will fire.  If this method is being called do to some other even (rather
     * than scheduling a trigger), the caller should pass zero (0).
     *  新计划触发的触发时间（以毫秒为单位）。如果正在调用此方法以执行其他事件（而不是调度触发器），则调用者应传递零（0）。
     */
    public void signalSchedulingChange(long candidateNewNextFireTime) {
        synchronized(sigLock) {
            signaled = true;
            signaledNextFireTime = candidateNewNextFireTime;
            sigLock.notifyAll();
        }
    }

    // 清除信号调度变更
    public void clearSignaledSchedulingChange() {
        synchronized(sigLock) {
            // 信号状态
            signaled = false;
            signaledNextFireTime = 0;
        }
    }

    public boolean isScheduleChanged() {
        // 这里用synchronized代码块不仅仅是保证 signaled 的读取安全，也能保证可见性
        synchronized(sigLock) {
            return signaled;
        }
    }

    // 获取下一次点火信号的时间
    public long getSignaledNextFireTime() {
        synchronized(sigLock) {
            // synchronized 内读取保证了signaledNextFireTime的可见性
            return signaledNextFireTime;
        }
    }

    /**
     * <p>
     * The main processing loop of the <code>QuartzSchedulerThread</code>.
     * QuartzSchedulerThread的主处理循环。
     * </p>
     *
     *  这里的run方法是在  DefaultThreadExecutor::execute 中唤醒的
     */
    @Override
    public void run() {
        // 定义获取失败的次数
        int acquiresFailed = 0;
        while (!halted.get()) {
            try {
                // check if we're supposed to pause... 检查我们是否应该暂停。。。
                synchronized (sigLock) {
                    while (paused && !halted.get()) {
                        try {
                            // wait until togglePause(false) is called... 等待 togglePause(false) 直到被唤醒
                            // 这是调用对象的 wait 方法，等待1S后继续往下执行，注意：暂停1秒并不代表能退出while循环，因为后面还会检查halted状态
                            sigLock.wait(1000L);
                        } catch (InterruptedException ignore) {
                        }
                        // reset failure counter when paused, so that we don't wait again after unpausing
                        // 暂停时重置失败计数器，这样我们就不会在取消暂停后再次等待
                        acquiresFailed = 0;
                    }

                    if (halted.get()) {
                        break;
                    }
                }

                // wait a bit, if reading from job store is consistently 如果从作业存储中读取的内容一致，请稍等
                // failing (e.g. DB is down or restarting).. 失败（例如数据库关闭或重新启动）。。
                // 这里所做的是对错误次数的容忍度 当错误次数>=2时候 默认情况下sleep 10分钟，且可配置(org.quartz.scheduler.dbFailureRetryInterval)
                if (acquiresFailed > 1) {
                    try {
                        long delay = computeDelayForRepeatedErrors(qsRsrcs.getJobStore(), acquiresFailed);
                        Thread.sleep(delay);
                    } catch (Exception ignore) {
                    }
                }

                // 获取可用执行线程个数,确保可有
                int availThreadCount = qsRsrcs.getThreadPool().blockForAvailableThreads();
                if(availThreadCount > 0) { // will always be true, due to semantics of blockForAvailableThreads... 将始终为真，由于blockForAvailableThreads的语义。。。
                    List<OperableTrigger> triggers;
                    long now = System.currentTimeMillis();
                    // 清除调度信号变更
                    clearSignaledSchedulingChange();
                    try {
                        // 这里面做这几件事儿 (JobStoreSupport:acquireNextTrigger)：
                        //      1.从 job_cfg 中获取 state=WAITING 且 nextFiretime=now+30S 的所有记录
                        //      2.检查并发状态(true:一个job同时只能支持一个执行时间执行 )
                        //      3.更新每一条对应 job_cfg 中的记录的状态为 state=ACQUIRED(获得/正常执行)
                        //      4.trigger设置fireInstanceId
                        //      5.记录写入 FIRED_TRIGGERS 表且状态为 state=ACQUIRED(获得/正常执行)
                        //      6.返回已更新 job_cfg 的记录
                        triggers = qsRsrcs.getJobStore().acquireNextTriggers(now + idleWaitTime, Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()), qsRsrcs.getBatchTimeWindow());
                        acquiresFailed = 0;
//                        if (log.isDebugEnabled()){
//                            log.debug("batch acquisition of " + (triggers == null ? 0 : triggers.size()) + " triggers");
//                        }
                    } catch (JobPersistenceException jpe) {
                        if (acquiresFailed == 0) {
                            log.error("An error occurred while scanning for the next triggers to fire.",jpe);
//                            qs.notifySchedulerListenersError("An error occurred while scanning for the next triggers to fire.",jpe);
                        }
                        if (acquiresFailed < Integer.MAX_VALUE){
                            acquiresFailed++;
                        }
                        continue;
                    } catch (RuntimeException e) {
                        if (acquiresFailed == 0) {
                            log.error("quartzSchedulerThreadLoop: RuntimeException " +e.getMessage(), e);
                        }
                        if (acquiresFailed < Integer.MAX_VALUE){
                            acquiresFailed++;
                        }
                        continue;
                    }

                    if (triggers != null && !triggers.isEmpty()) {
                        // 当前时间
                        now = System.currentTimeMillis();
                        long triggerTime = triggers.get(0).getNextFireTime().getTime();
                        long timeUntilTrigger = triggerTime - now; // 下一次点火时间与当前时间的间隔(毫秒)
                        while(timeUntilTrigger > 2) {
                            synchronized (sigLock) {
                                // 停止标志
                                if (halted.get()) {
                                    break;
                                }
                                // 通知点火时间 >= 任务点火时间(返回false)的执行等待
                                if (!isCandidateNewTimeEarlierWithinReason(triggerTime, false)) {
                                    try {
                                        // we could have blocked a long while on 'synchronize', so we must recompute
                                        //  我们可能在“同步”上阻塞了很长时间，所以我们必须重新计算
                                        now = System.currentTimeMillis();
                                        timeUntilTrigger = triggerTime - now;
                                        if(timeUntilTrigger >= 1){
                                            // 等待到点火时间
                                            sigLock.wait(timeUntilTrigger);
                                        }
                                    } catch (InterruptedException ignore) {
                                    }
                                }
                            }
                            // 通知点火时间 < 任务点火时间 的执行清理任务，整体来看这里其实只有两毫秒的容忍度，如果经过上面等待的话这里几乎是不会为true的
                            if(releaseIfScheduleChangedSignificantly(triggers, triggerTime)) {
                                break;
                            }
                            now = System.currentTimeMillis();
                            timeUntilTrigger = triggerTime - now;
                        }

                        // this happens if releaseIfScheduleChangedSignificantly decided to release triggers 如果releaseIfScheduleChanged Significaly决定释放触发器，就会发生这种情况
                        // 也就是在等待后发现有被修改,故会在 releaseIfScheduleChangedSignificantly 中trigger被删除，才会发生此情况
                        // todo : 此类特殊情况的处理是否可以通过锁定时间来解决？
                        if(triggers.isEmpty()){
                            continue;
                        }
                        // set triggers to 'executing' 将触发器设置为“正在执行”
                        List<TriggerFiredResult> bndles = new ArrayList<TriggerFiredResult>();
                        boolean goAhead = true;
                        synchronized(sigLock) {
                            // 停止标志位为true是表示停止
                            goAhead = !halted.get();
                        }
                        if(goAhead) {
                            try {
                                // 这里主要做这几件事：
                                //  1.从 JOB_CFG 获取对应记录并判断 trigger_state == ACQUIRED
                                //  2.更新对应 FIRED_TRIGGERS 的 state=EXECUTING
                                //  3.更新 JOB_CFG 对应记录状态 TRIGGER_STATE=WAITING
                                //  4.构建 TriggerFiredBundle 并返回
                                List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);
                                if(res != null){
                                    bndles = res;
                                }
                            } catch (SchedulerException se) {
                                se.printStackTrace();
                                log.error("An error occurred while firing triggers '"+ triggers + "'", se);
//                                qs.notifySchedulerListenersError("An error occurred while firing triggers '"+ triggers + "'", se);
                                //QTZ-179 : a problem occurred interacting with the triggers from the db
                                //we release them and loop again
                                // QTZ-179：与数据库中的触发器交互时出现问题，我们释放它们并再次循环
                                for (int i = 0; i < triggers.size(); i++) {
                                    // 1. 将 JOB_CFG 中 TRIGGER_STATE 由 ACQUIRED->WAITING , BLOCKED->WAITING
                                    // 2. 删除 FIRED_TRIGGERS 对应记录
                                    qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                }
                                continue;
                            }
                        }
                        for (int i = 0; i < bndles.size(); i++) {
                            TriggerFiredResult result =  bndles.get(i);
                            TriggerFiredBundle bndle =  result.getTriggerFiredBundle();
                            Exception exception = result.getException();
                            // 处理异常情况
                            if (exception instanceof RuntimeException) {
                                log.error("RuntimeException while firing trigger " + triggers.get(i), exception);
                                // 1. 将 JOB_CFG 中 TRIGGER_STATE 由 ACQUIRED->WAITING , BLOCKED->WAITING
                                // 2. 删除 FIRED_TRIGGERS 对应记录
                                qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                continue;
                            }
                            // it's possible to get 'null' if the triggers was paused,
                            // blocked, or other similar occurrences that prevent it being
                            // fired at this time...  or if the scheduler was shutdown (halted)
                            // 如果触发器被暂停、阻止或其他类似事件阻止它在此时被触发，则可能会得到“null”。。。或者如果调度程序已关闭（暂停）
                            if (bndle == null) {
                                // 1. 将 JOB_CFG 中 TRIGGER_STATE 由 ACQUIRED->WAITING , BLOCKED->WAITING
                                // 2. 删除 FIRED_TRIGGERS 对应记录
                                qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                continue;
                            }
                            JobRunShell shell = null;
                            try {
                                // 创建jobShell
                                shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(bndle);
                                // 1.创建job实例并补充上下文及参数
                                // 2.设置JobExecutionContext
                                shell.initialize(qs);
                            } catch (SchedulerException se) {
                                // 1.设置触发器的状态为ERROR ： TRIGGER_STATE ='ERROR'
                                // 2.设置线程本地变量为0L
                                // 3.判断并发执行: 01.更新QRTZ_TRIGGERS 状态 BLOCKED->WAITING PAUSED_BLOCKED->PAUSED  02.设置线程本地变量为0L
                                // 4.判断保留作业数据: 01.更新 QRTZ_JOB_DETAILS::JOB_DATA
                                qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                                continue;
                            }
                            // 这一句很关键，所有的执行都经这个方法调用
                            // 所有任务都会被包装为 Runnable 对象然后扔进线程池执行，具体执行逻辑见 MeeThreadPool#run
                            if (qsRsrcs.getThreadPool().runInThread(shell) == false) {
                                // this case should never happen, as it is indicative of the
                                // scheduler being shutdown or a bug in the thread pool or
                                // a thread pool being used concurrently - which the docs
                                // say not to do...
                                // 这种情况永远不应该发生，因为这表明调度程序正在关闭，或者线程池或线程池中的错误正在并发使用——文档说不要这样做。。。
                                log.error("ThreadPool.runInThread() return false!");
                                qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                            }

                        }

                        continue; // while (!halted)
                    }
                } else {
                    // 走到这里说明没有可用的执行线程
                    // if(availThreadCount > 0)
                    // should never happen, if threadPool.blockForAvailableThreads() follows contract 如果threadPool.blockForAvailableThreads（）遵循约定，则永远不会发生
                    continue; // while (!halted)
                }

                long now = System.currentTimeMillis();
                // 这个 waitTime 默认就是 now+(0S~7S) 的样子
                long waitTime = now + getRandomizedIdleWaitTime();
                long timeUntilContinue = waitTime - now;
                synchronized(sigLock) {
                    try {
                      if(!halted.get()) {
                        // QTZ-336 A job might have been completed in the mean time and we might have
                        // missed the scheduled changed signal by not waiting for the notify() yet
                        // Check that before waiting for too long in case this very job needs to be
                        // scheduled very soon
                          // QTZ-336作业可能同时完成，我们可能因为还没有等待notify（）而错过了预定的更改信号。
                          // 在等待太久之前检查一下，以防这项工作需要很快安排
                        if (!isScheduleChanged()) {
                            // 这是随机等待，具体范围是(0~7S)内
                          sigLock.wait(timeUntilContinue);
                        }
                      }
                    } catch (InterruptedException ignore) {
                    }
                }

            } catch(RuntimeException re) {
                log.error("Runtime error occurred in main trigger firing loop.", re);
            }
        } // while (!halted)

        // drop references to scheduler stuff to aid garbage collection...
        qs = null;
        qsRsrcs = null;
    }

    private static final long MIN_DELAY = 20;
    private static final long MAX_DELAY = 600000;

    // 计算重复错误的延迟 acquiresFailed:一般为>1的值
    private static long computeDelayForRepeatedErrors(JobStore jobStore, int acquiresFailed) {
        long delay;
        try {
            // 默认15000(15秒) 具体配置见: org.quartz.scheduler.dbFailureRetryInterval
            delay = jobStore.getAcquireRetryDelay(acquiresFailed);
        } catch (Exception ignored) {
            // we're trying to be useful in case of error states, not cause
            // additional errors..
            // 我们试图在出现错误状态时发挥作用，而不是导致额外的错误。。
            delay = 100;
        }
        // sanity check per getAcquireRetryDelay specification 根据getAcquireMetricDelay规范进行健全性检查
        if (delay < MIN_DELAY){
            delay = MIN_DELAY;
        }
        // 延迟 10分钟
        if (delay > MAX_DELAY){
            delay = MAX_DELAY;
        }
        return delay;
    }

    private boolean releaseIfScheduleChangedSignificantly(List<OperableTrigger> triggers, long triggerTime) {
        // 一般是:通知点火时间 < 任务点火时间
        if (isCandidateNewTimeEarlierWithinReason(triggerTime, true)) {
            // above call does a clearSignaledSchedulingChange()
            for (OperableTrigger trigger : triggers) {
                // 这里面大致处理有：
                //  1.把对应 job_cfg 中的 state in (ACQUIRED,BLOCKED) 改为 WAITING 状态
                //  2.把对应 FIRED_TRIGGERS 删除
                qsRsrcs.getJobStore().releaseAcquiredTrigger(trigger);
            }
            // 清理触发器
            triggers.clear();
            return true;
        }
        return false;
    }

    // 候选人新时间提前是否合理
    private boolean isCandidateNewTimeEarlierWithinReason(long oldTime, boolean clearSignal) {

        // So here's the deal: We know due to being signaled that 'the schedule'
        // has changed.  We may know (if getSignaledNextFireTime() != 0) the
        // new earliest fire time.  We may not (in which case we will assume
        // that the new time is earlier than the trigger we have acquired).
        // In either case, we only want to abandon our acquired trigger and
        // go looking for a new one if "it's worth it".  It's only worth it if
        // the time cost incurred to abandon the trigger and acquire a new one
        // is less than the time until the currently acquired trigger will fire,
        // otherwise we're just "thrashing" the job store (e.g. database).
        // 所以，事情是这样的：我们知道，由于收到“日程表”已更改的信号。我们可能知道（如果 getSignaledNextFireTime() !=0)新的最早点火时间。
        // 我们可能不会（在这种情况下，我们将假设新时间早于我们获得的触发时间）。
        // 在任何一种情况下，我们只想放弃我们获得的触发器，并在“值得”的情况下寻找新的触发器。
        // 只有当放弃触发器并获取新触发器所花费的时间小于当前获取的触发器触发所需的时间时，这样做才是值得的，否则我们只是在“折腾”作业存储（例如数据库）。
        //
        // So the question becomes when is it "worth it"?  This will depend on
        // the job store implementation (and of course the particular database
        // or whatever behind it).  Ideally we would depend on the job store
        // implementation to tell us the amount of time in which it "thinks"
        // it can abandon the acquired trigger and acquire a new one.  However
        // we have no current facility for having it tell us that, so we make
        // a somewhat educated but arbitrary guess ;-).
        // 所以问题就变成了什么时候“值得”这么做？这将取决于作业存储实现（当然还有特定的数据库或其背后的任何东西）。
        // 理想情况下，我们会依赖作业存储实现来告诉我们它“认为”它可以放弃已获取的触发器并获取新触发器的时间量。
        // 然而，我们目前没有设备可以告诉我们，所以我们做出了一个有点有根据但武断的猜测；-）。

        // oldTime=nextFireTime
        synchronized(sigLock) {
            // 获取 signaled 的值
            if (!isScheduleChanged()){
                return false;
            }
//            boolean earlier = false;
//            // 获取 signaledNextFireTime 的值
//            if(getSignaledNextFireTime() == 0){
//                earlier = true;
//            }
//            else if(getSignaledNextFireTime() < oldTime ){
//                earlier = true;
//            }
            // signaledNextFireTime:发出下次点火时间的信号,这个参数一开始就是0
            System.out.println("getSignaledNextFireTime()="+getSignaledNextFireTime()+", oldTime="+oldTime);
            boolean earlier = getSignaledNextFireTime() == 0 || getSignaledNextFireTime() < oldTime ?true:false;
            if(earlier) {
                // so the new time is considered earlier, but is it enough earlier? 所以新的时间被认为更早，但足够早吗？
                long diff = oldTime - System.currentTimeMillis();
                // 这里的 supportsPersistence 针对基于DB的任务是70，内存任务是7
                if(diff < (qsRsrcs.getJobStore().supportsPersistence() ? 70L : 7L)){
                    earlier = false;
                }
            }
            if(clearSignal) {
                clearSignaledSchedulingChange();
            }
            return earlier;
        }
    }

} // end of QuartzSchedulerThread
