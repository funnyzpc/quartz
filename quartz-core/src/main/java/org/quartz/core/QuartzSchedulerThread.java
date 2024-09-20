
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

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.SystemPropGenerator;
import org.quartz.spi.OperableTrigger;
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

//    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Logger log = LoggerFactory.getLogger(QuartzSchedulerThread.class);

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

    /**
     * 启动
     * 调用方:
     * QuartzScheduler::QuartzScheduler(xx,xx,xx)::this.schedThread.start(); ❌
     * QuartzScheduler::start()::this.schedThread.start(); ✔
     */
    @Override
    public void start(){
        super.start();
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

    private static long LOOP_INTERVAL = 5000L;
    private static long LOOP_WINDOW = 8L;
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
//        log.info("=====>invoke QuartzSchedulerThread::run()<=====");
        // 定义获取失败的次数
        int acquiresFailed = 0;
        final String application = qsRsrcs.getJobStore().getInstanceName();
        final String hostIP = SystemPropGenerator.hostIP();
        long now = System.currentTimeMillis(); // 这个时间不调整
//        long _t = System.currentTimeMillis();
        while (!halted.get()) {
//            System.out.println("##scheduler耗时:"+(System.currentTimeMillis()-_t));
//            _t = System.currentTimeMillis();
            try {
                long _ts = System.currentTimeMillis(); // 这个是减去sleep的时间了的
                // check if we're supposed to pause... 检查我们是否应该暂停。。。

                // 加锁
                //1.判断锁定对象防止并发 synchronized (sigLock)
                //2.获取实例(node)锁判断是否停止/暂停 qrtz_node::state=N
                synchronized (sigLock) {
//                    final String state = qsRsrcs.getJobStore().findNodeStateByPK(application,hostIP);
                    int _stop = 0;
                    // 是否暂停，是否停止
                    while (!"Y".equals(qsRsrcs.getJobStore().findNodeStateByPK(application,hostIP)) /*&& !halted.get()*/) {
                        _stop=_stop>10?1:1+_stop;
                        try {
                            // 适当延长等待时间，减少空转
                            sigLock.wait(LOOP_INTERVAL*(_stop/3==0?1:2)-LOOP_WINDOW);
                            _ts = System.currentTimeMillis(); // 必须要重置，否则获取执行信息会出现时间误差
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

//                // wait a bit, if reading from job store is consistently 如果从作业存储中读取的内容一致，请稍等
//                // failing (e.g. DB is down or restarting).. 失败（例如数据库关闭或重新启动）。。
//                // 这里所做的是对错误次数的容忍度 当错误次数>=2时候 默认情况下sleep 10分钟，且可配置(org.quartz.scheduler.dbFailureRetryInterval)
//                // 异常忍耐度
//                // 1.异常次数判断
//                // 2.异常等待时间
//                if (acquiresFailed > 1) {
//                    try {
////                        long _tl = LOOP_INTERVAL*2;
//                        long _tl = LOOP_INTERVAL;
////                        long delay = computeDelayForRepeatedErrors(qsRsrcs.getJobStore(), acquiresFailed);
//                        long delay = acquiresFailed<=2?_tl/4 : _tl-6;
//                        Thread.sleep(delay);
//                        _ts = System.currentTimeMillis(); // 必须要重置，否则获取执行信息会出现时间误差
//                    } catch (Exception ignore) {
//                    }
//                }

                // 获取可用执行线程个数,确保可有
                int availThreadCount = qsRsrcs.getThreadPool().blockForAvailableThreads();
                if(availThreadCount > 0) { // will always be true, due to semantics of blockForAvailableThreads... 将始终为真，由于blockForAvailableThreads的语义。。。
                    List<QrtzExecute> executeList = null;
//                    long _tew = _ts+LOOP_INTERVAL*2; // time end window
                    long _tew = _ts+LOOP_INTERVAL; // time end window
                    // 清除调度信号变更
                    clearSignaledSchedulingChange();
                    try {
                        executeList = qsRsrcs.getJobStore().acquireNextTriggers(application,now,_tew);
                        acquiresFailed = 0;
                        if (executeList == null || executeList.isEmpty()) {
                            continue;
                        }
                    } catch (JobPersistenceException | RuntimeException jpe) {
                        if (acquiresFailed == 0) {
                            log.error("An error occurred while scanning for the next triggers to fire.",jpe);
//                            qs.notifySchedulerListenersError("An error occurred while scanning for the next triggers to fire.",jpe);
                        }
                        if (acquiresFailed < Integer.MAX_VALUE){
                            acquiresFailed++;
                        }
                        continue;
                    }
//                    catch (InterruptedException e) {
//                        // 这是 Thread.sleep 抛出来的，直接continue即可
//                        continue;
//                    }

                    // 要重复检查是否暂停/停止
                    if (!"Y".equals(qsRsrcs.getJobStore().findNodeStateByPK(application,hostIP)) ){
                        // 这里不处理 交给上方 while ... findNodeStateByPK 处理
//                        long w = 0;
//                        if((w = (System.currentTimeMillis()-now-8)) >0 ){
//                            Thread.sleep(w);
//                        }
                        continue;
                    }else{
                        // 循环等待
                        //1.直至误差时间内(6毫秒)
                        long ww = executeList.size()-1000<0 ? 4L : ((executeList.size()-1000L)/2000L)+4L ;
                        ww= Math.min(ww, 8L);
//                        while( !executeList.isEmpty() && (System.currentTimeMillis()-now)<=LOOP_INTERVAL*2 ){
                        while( !executeList.isEmpty() && (System.currentTimeMillis()-now)<=LOOP_INTERVAL ){
                            long _et  = System.currentTimeMillis();
                            QrtzExecute ce = null; // executeList.get(0);
                            for( int i = 0;i< executeList.size();i++ ){
                                QrtzExecute el = executeList.get(i);
                                // 这是要马上执行的任务
                                if( el.getNextFireTime()-_et <= ww){
                                    ce=el;
                                    break;
                                }
                                if(i==0){
                                    ce=el;
                                    continue;
                                }
                                // 总是获取最近时间呢个
                                if( el.getNextFireTime() <= ce.getNextFireTime() ){
                                    ce = el;
//                                    continue;
                                }
                            }
                            executeList.remove(ce); // 一定要移除，否则无法退出while循环!!!
                            // 延迟
                            long w = 0;
//                            if((w = (ce.getNextFireTime()-System.currentTimeMillis()-5)) >0 ){
                            if((w = (ce.getNextFireTime()-System.currentTimeMillis()-ww)) >0 ){
                                try {
                                    Thread.sleep(w);
                                }catch (Exception e){
                                }
                            }

                            // 尝试获取执行记录锁
                            //## 尝试获取任务锁
                            //1.判断是否是本次执行
                            //2.修改下一次执行时间(next_file_time)
                            if( !tryAcquireLockAndUpdate(ce) && null!=ce.setFireTime(System.currentTimeMillis()) ){
                                log.info("任务未能获取执行锁:{},{}-{}",ce.getId(),ce.getJobType(),ce.getJob().getJobClass());
                                continue;
                            }
//                            log.error("=>已执行:{}->{},{}<=",ce.getId(),ce.getJobType(),ce.getJob().getJobClass()+"#"+ce.getExecuteIdx());
//                            System.out.println(DateUtil.N()+"=>已执行:"+ce.getId()+","+ce.getJobType()+"-"+ce.getJob().getJobClass()+"#"+ce.getExecuteIdx());

                            JobRunShell shell = null;
                            try {
                                // 创建jobShell
                                shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(ce);
                                // 1.创建job实例并补充上下文及参数
                                // 2.设置JobExecutionContext
                                shell.initialize(qs);
                            } catch (SchedulerException se) {
                                se.printStackTrace();
                                // todo： 是否重试需要根据job配置来,同时重试后仍然失败是否需要将state改为ERROR？。。。这里暂且如此
                                continue;
                            }
                            // 这一句很关键，所有的执行都经这个方法调用
                            // 所有任务都会被包装为 Runnable 对象然后扔进线程池执行，具体执行逻辑见 MeeThreadPool#run
                            if (qsRsrcs.getThreadPool().runInThread(shell) == false) {
                                // todo： 是否重试需要根据job配置来,同时重试后仍然失败是否需要将state改为ERROR？。。。这里暂且如此
                                continue;
                            }

                        }

                    }
                } else {
                    // 走到这里说明没有可用的执行线程
                    // if(availThreadCount > 0)
                    // should never happen, if threadPool.blockForAvailableThreads() follows contract 如果threadPool.blockForAvailableThreads（）遵循约定，则永远不会发生
                    continue; // while (!halted)
                }

//                // 以下逻辑只会在以上出现异常时进入
////                long now = System.currentTimeMillis();
//                // 这个 waitTime 默认就是 now+(0S~7S) 的样子
//                long waitTime = now + getRandomizedIdleWaitTime();
//                long timeUntilContinue = waitTime - now;
//                synchronized(sigLock) {
//                    try {
//                      if(!halted.get()) {
//                        // QTZ-336 A job might have been completed in the mean time and we might have
//                        // missed the scheduled changed signal by not waiting for the notify() yet
//                        // Check that before waiting for too long in case this very job needs to be
//                        // scheduled very soon
//                          // QTZ-336作业可能同时完成，我们可能因为还没有等待notify（）而错过了预定的更改信号。
//                          // 在等待太久之前检查一下，以防这项工作需要很快安排
//                        if (!isScheduleChanged()) {
//                            // 这是随机等待，具体范围是(0~7S)内
//                          sigLock.wait(timeUntilContinue);
//                        }
//                      }
//                    } catch (InterruptedException ignore) {
//                    }
//                }

            } catch(RuntimeException re) {
                log.error("Runtime error occurred in main trigger firing loop.", re);
            }
//            catch (InterruptedException e) {
//                log.error("Runtime error occurred in main trigger firing loop.",e);
////                throw new RuntimeException(e);
//            }
            finally {
                // 延迟
                long st = 0;
                // if ( (sleep_time = (TIME_CHECK_INTERVAL-(System.currentTimeMillis() - _start)-2))>0 )
//                if((st = (LOOP_INTERVAL*2-(System.currentTimeMillis()-now)-4)) >0 ){
                if((st = (LOOP_INTERVAL-(System.currentTimeMillis()-now)-2)) >0 ){
                    try {
                        Thread.sleep(st);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // 限制错误
                if(acquiresFailed>3){
                    try {
                        Thread.sleep(LOOP_INTERVAL*(acquiresFailed>6?3:2));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    acquiresFailed=acquiresFailed>9?0:acquiresFailed;
                }
                now = System.currentTimeMillis();
            }
        } // while (!halted)

        // drop references to scheduler stuff to aid garbage collection...
        qs = null;
        qsRsrcs = null;
    }

    private boolean tryAcquireLockAndUpdate(QrtzExecute ce)  {
        // 1.计算更新 prev_fire_time、next_fire_time .... etc
        long now = System.currentTimeMillis();
        final String jobType = ce.getJobType();
        final Long endTime = (null==ce.getEndTime() || ce.getEndTime()<1) ?-1:ce.getEndTime();
        // 这两个字段相当于是版本，也可以理解为锁
        long old_prev_time = ce.getPrevFireTime();
        long old_next_time = ce.getNextFireTime();
        final String old_state = ce.getState();
        Date nextFireTime = new Date(ce.getNextFireTime());
        QrtzExecute newCe = new QrtzExecute(ce.getId(),ce.getPid(),ce.getJobType(),ce.getState(),ce.getCron(),ce.getZoneId(),ce.getRepeatCount(),ce.getRepeatInterval(),ce.getTimeTriggered(),ce.getPrevFireTime(),ce.getNextFireTime(),ce.getHostIp(),ce.getHostName(),ce.getStartTime(),ce.getEndTime());
        try {
            if ("CRON".equals(jobType)) {
                CronTriggerImpl cronTrigger = new CronTriggerImpl()
                        .setCronExpression(newCe.getCron())
                        .setStartTime(new Date(newCe.getStartTime()))
                        .setEndTime(new Date(endTime))
                        .setTimeZone(TimeZone.getTimeZone(newCe.getZoneId()));
//                if(endTime>0){
//                    cronTrigger.setEndTime(new Date(endTime));
//                }
//                Date _ds = nextFireTime;
                nextFireTime = cronTrigger.getFireTimeAfter(nextFireTime);
//                System.out.println("CRON=>"+ (_ds.getTime()>now)+" | "+(nextFireTime.getTime()>now));
                if (nextFireTime == null) {
//                old_state = ce.getState();
                    newCe.setEndTime(now);
                    newCe.setState("COMPLETE");
                } else {
                    newCe.setPrevFireTime(newCe.getNextFireTime());
                    newCe.setNextFireTime(nextFireTime.getTime());
                }
            } else {
                SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl()
                        .setStartTime(new Date(newCe.getStartTime()))
                        .setEndTime(new Date(endTime))
                        .setRepeatCount(newCe.getRepeatCount())
                        .setRepeatInterval(newCe.getRepeatInterval())
                        .setTimesTriggered(newCe.getTimeTriggered());
//                if(endTime>0){
//                    simpleTrigger.setEndTime(new Date(endTime));
//                }
//                Date _ds = nextFireTime;
                nextFireTime = simpleTrigger.getFireTimeAfter(nextFireTime);
//                System.out.println("CRON=>"+ (_ds.getTime()>now)+" | "+(nextFireTime.getTime()>now));
                if (nextFireTime == null || (endTime > 0 && endTime < now) || (newCe.getRepeatCount()>0 && newCe.getTimeTriggered() > newCe.getRepeatCount())) {
//                old_state = ce.getState();
                    newCe.setEndTime(now);
                    newCe.setState("COMPLETE");
                } else {
                    newCe.setPrevFireTime(newCe.getNextFireTime());
                    newCe.setNextFireTime(nextFireTime.getTime());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            newCe.setState("ERROR");
        }
        return qsRsrcs.getJobStore().toLockAndUpdate(newCe,old_state,old_prev_time,old_next_time)>0;
    }
//    private boolean tryAcquireLockAndUpdate(QrtzExecute ce)  {
//        // 1.计算更新 prev_fire_time、next_fire_time .... etc
//        long now = System.currentTimeMillis();
//        final String jobType = ce.getJobType();
//        final Long endTime = (null==ce.getEndTime() || ce.getEndTime()<1) ?-1:ce.getEndTime();
//        // 这两个字段相当于是版本，也可以理解为锁
//        long old_prev_time = ce.getPrevFireTime();
//        long old_next_time = ce.getNextFireTime();
//        final String old_state = ce.getState();
//        Date nextFireTime = new Date(ce.getNextFireTime());
//        QrtzExecute newCe = new QrtzExecute(ce.getId(),ce.getPid(),ce.getExecuteIdx(),ce.getJobType(),ce.getState(),ce.getCron(),ce.getZoneId(),ce.getRepeatCount(),ce.getRepeatInterval(),ce.getTimeTriggered(),ce.getPrevFireTime(),ce.getNextFireTime(),ce.getHostIp(),ce.getHostName(),ce.getStartTime(),ce.getEndTime());
//        try {
//            if ("CRON".equals(jobType)) {
////                System.out.println("CRON=>"+ (_ds.getTime()>now)+" | "+(nextFireTime.getTime()>now));
//                System.out.println("CRON=>"+" | "+(nextFireTime.getTime()>now));
//                if (nextFireTime == null) {
//                    newCe.setEndTime(now);
//                    newCe.setState("COMPLETE");
//                } else {
////                    newCe.setPrevFireTime(newCe.getNextFireTime());
////                    newCe.setNextFireTime(nextFireTime.getTime());
//                }
//            } else {
//
////                System.out.println("CRON=>"+ (_ds.getTime()>now)+" | "+(nextFireTime.getTime()>now));
//                System.out.println("CRON=>"+ " | "+(nextFireTime.getTime()>now));
//                if (nextFireTime == null || (endTime > 0 && endTime < now) || newCe.getTimeTriggered() > newCe.getRepeatCount()) {
//                    newCe.setEndTime(now);
//                    newCe.setState("COMPLETE");
//                } else {
////                    newCe.setPrevFireTime(newCe.getNextFireTime());
////                    newCe.setNextFireTime(nextFireTime.getTime());
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            newCe.setState("ERROR");
//        }
//        return qsRsrcs.getJobStore().toLockAndUpdate(newCe,old_state,old_prev_time,old_next_time)>0;
//    }

    private static final long MIN_DELAY = 20;
    private static final long MAX_DELAY = 600000;

//    // 计算重复错误的延迟 acquiresFailed:一般为>1的值
//    private static long computeDelayForRepeatedErrors(JobStore jobStore, int acquiresFailed) {
//        long delay;
//        try {
//            // 默认15000(15秒) 具体配置见: org.quartz.scheduler.dbFailureRetryInterval
//            delay = jobStore.getAcquireRetryDelay(acquiresFailed);
////            delay = LOOP_INTERVAL*2;
//        } catch (Exception ignored) {
//            // we're trying to be useful in case of error states, not cause
//            // additional errors..
//            // 我们试图在出现错误状态时发挥作用，而不是导致额外的错误。。
//            delay = 100;
//        }
//        // sanity check per getAcquireRetryDelay specification 根据getAcquireMetricDelay规范进行健全性检查
//        if (delay < MIN_DELAY){
//            delay = MIN_DELAY;
//        }
//        // 延迟 10分钟
//        if (delay > MAX_DELAY){
//            delay = MAX_DELAY;
//        }
//        return delay;
//    }

    private boolean releaseIfScheduleChangedSignificantly(List<OperableTrigger> triggers, long triggerTime) {
        // 一般是:通知点火时间 < 任务点火时间
        if (isCandidateNewTimeEarlierWithinReason(triggerTime, true)) {
//            // above call does a clearSignaledSchedulingChange()
//            for (OperableTrigger trigger : triggers) {
//                // 这里面大致处理有：
//                //  1.把对应 job_cfg 中的 state in (ACQUIRED,BLOCKED) 改为 WAITING 状态
//                //  2.把对应 FIRED_TRIGGERS 删除
//                qsRsrcs.getJobStore().releaseAcquiredTrigger(trigger);
//            }
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
//            System.out.println("getSignaledNextFireTime()="+getSignaledNextFireTime()+", oldTime="+oldTime);
//            log.info("getSignaledNextFireTime()="+getSignaledNextFireTime()+", oldTime="+oldTime);
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
