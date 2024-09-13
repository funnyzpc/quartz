
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

package org.quartz;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;



/**
 * The base interface with properties common to all <code>Trigger</code>s -
 * use {@link TriggerBuilder} to instantiate an actual Trigger.
 * 
 * <p>
 * <code>Triggers</code>s have a {@link TriggerKey} associated with them, which
 * should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * 
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s
 * are scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * 
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents
 * into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @see TriggerBuilder
 * @see JobDataMap
 * @see JobExecutionContext
 * @see TriggerUtils
 * @see SimpleTrigger
 * @see CronTrigger
 * @see CalendarIntervalTrigger
 * 
 * @author James House
 */
public interface Trigger extends Serializable, Cloneable, Comparable<Trigger> {

    public static final long serialVersionUID = -3904243490805975570L;
    
    public enum TriggerState { NONE, NORMAL, PAUSED, COMPLETE, ERROR, BLOCKED }
    
    /**
     * <p><code>NOOP</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> has no further instructions.</p>
     * 
     * <p><code>RE_EXECUTE_JOB</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> wants the <code>{@link org.quartz.JobDetail}</code> to 
     * re-execute immediately. If not in a 'RECOVERING' or 'FAILED_OVER' situation, the
     * execution context will be re-used (giving the <code>Job</code> the
     * ability to 'see' anything placed in the context by its last execution).</p>
     * 
     * <p><code>SET_TRIGGER_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> should be put in the <code>COMPLETE</code> state.</p>
     * 
     * <p><code>DELETE_TRIGGER</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> wants itself deleted.</p>
     * 
     * <p><code>SET_ALL_JOB_TRIGGERS_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> 
     * that all <code>Trigger</code>s referencing the same <code>{@link org.quartz.JobDetail}</code> 
     * as this one should be put in the <code>COMPLETE</code> state.</p>
     * 
     * <p><code>SET_TRIGGER_ERROR</code> Instructs the <code>{@link Scheduler}</code> that all 
     * <code>Trigger</code>s referencing the same <code>{@link org.quartz.JobDetail}</code> as
     * this one should be put in the <code>ERROR</code> state.</p>
     *
     * <p><code>SET_ALL_JOB_TRIGGERS_ERROR</code> Instructs the <code>{@link Scheduler}</code> that 
     * the <code>Trigger</code> should be put in the <code>ERROR</code> state.</p>
     *
     * NOOP指示调度器触发器没有进一步的指令。
     * RE_EXECUTE_JOB指示调度器触发器希望JobDetail立即重新执行。如果不是在“恢复”或“失败”的情况下，执行上下文将被重新使用（使作业能够“看到”上次执行时放置在上下文中的任何内容）。
     * SET_TRIGGER_COMPLETE指示调度程序应将触发器置于“完成”状态。
     * DELETE_TRIGGER指示调度程序删除触发器。
     * SET_ALL_JOB_TRIGGERS_COMPLETE指示调度器，所有引用与此相同JobDetail的触发器都应处于COMPLETE状态。
     * SET_TRIGGER_ERROR指示调度程序将引用与此相同JobDetail的所有触发器置于ERROR状态。
     * SET_ALL_JOB_TRIGGERS_ERROR指示调度程序应将触发器置于错误状态。
     *
     */
    public enum CompletedExecutionInstruction { NOOP, RE_EXECUTE_JOB, SET_TRIGGER_COMPLETE, DELETE_TRIGGER, 
        SET_ALL_JOB_TRIGGERS_COMPLETE, SET_TRIGGER_ERROR, SET_ALL_JOB_TRIGGERS_ERROR }

    /**
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>updateAfterMisfire()</code> method will be called
     * on the <code>Trigger</code> to determine the mis-fire instruction,
     * which logic will be trigger-implementation-dependent.
     *  指示调度器，在发生误触发情况时，将在触发器上调用updateAfterMisfire（）方法来确定误触发指令，该逻辑将取决于触发器的实现。
     *
     * <p>
     * In order to see if this instruction fits your needs, you should look at
     * the documentation for the <code>updateAfterMisfire()</code> method
     * on the particular <code>Trigger</code> implementation you are using.
     * </p>
     *  为了查看此指令是否符合您的需求，您应该查看您正在使用的特定Trigger实现上的updateAfterMisfire（）方法的文档。
     *  失火指令智能策略
     */
    public static final int MISFIRE_INSTRUCTION_SMART_POLICY = 0;
    
    /**
     * Instructs the <code>{@link Scheduler}</code> that the 
     * <code>Trigger</code> will never be evaluated for a misfire situation, 
     * and that the scheduler will simply try to fire it as soon as it can, 
     * and then update the Trigger as if it had fired at the proper time. 
     *  指示调度器永远不会对触发器进行失火情况评估，调度器只会尽快尝试触发触发器，然后更新触发器，就像它在适当的时间触发一样。
     *
     * <p>NOTE: if a trigger uses this instruction, and it has missed 
     * several of its scheduled firings, then several rapid firings may occur 
     * as the trigger attempt to catch back up to where it would have been. 
     * For example, a SimpleTrigger that fires every 15 seconds which has 
     * misfired for 5 minutes will fire 20 times once it gets the chance to 
     * fire.</p>
     *  注意：如果触发器使用此指令，并且它错过了几次预定的发射，那么当触发器试图恢复到原来的位置时，可能会发生几次快速发射。
     *  例如，每15秒触发一次的SimpleTrigger，如果失火5分钟，一旦有机会触发，将触发20次。
     *   理解： 失火指令忽略失火策略,如果为-1则表示此任务不会被触发
     */
    public static final int MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY = -1;
    
    /**
     * The default value for priority.
     */
    public static final int DEFAULT_PRIORITY = 5;

//    Key getKey();
    String getKey();
//    String getKeyNote();

//    JobKey getJobKey();
    
    /**
     * Return the description given to the <code>Trigger</code> instance by
     * its creator (if any).
     * 
     * @return null if no description was set.
     */
    String getDescription();

    /**
     * Get the name of the <code>{@link Calendar}</code> associated with this
     * Trigger.
     * 
     * @return <code>null</code> if there is no associated Calendar.
     */
    @Deprecated
    String getCalendarName();

    /**
     * Get the <code>JobDataMap</code> that is associated with the
     * <code>Trigger</code>.
     *
     * <p>
     * Changes made to this map during job execution are not re-persisted, and
     * in fact typically result in an <code>IllegalStateException</code>.
     * </p>
     *  有与 SpringBeanJobFactory 的存在，为减少工作量，故将此方法定位default
     *  ###仅用于springboot starter接入用，不做逻辑处理###
     */
    @Deprecated
    default JobDataMap getJobDataMap(){
        return new JobDataMap();
    }

    /**
     * The priority of a <code>Trigger</code> acts as a tiebreaker such that if 
     * two <code>Trigger</code>s have the same scheduled fire time, then the
     * one with the higher priority will get first access to a worker
     * thread.
     * 
     * <p>
     * If not explicitly set, the default value is <code>5</code>.
     * </p>
     * 
     * @see #DEFAULT_PRIORITY
     */
    int getPriority();

    /**
     * Used by the <code>{@link Scheduler}</code> to determine whether or not
     * it is possible for this <code>Trigger</code> to fire again.
     * 
     * <p>
     * If the returned value is <code>false</code> then the <code>Scheduler</code>
     * may remove the <code>Trigger</code> from the <code>{@link org.quartz.spi.JobStore}</code>.
     * </p>
     */
    boolean mayFireAgain();

    /**
     * Get the time at which the <code>Trigger</code> should occur.
     */
    Date getStartTime();

    /**
     * Get the time at which the <code>Trigger</code> should quit repeating -
     * regardless of any remaining repeats (based on the trigger's particular 
     * repeat settings). 
     * 
     * @see #getFinalFireTime()
     */
    Date getEndTime();

    /**
     * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If
     * the trigger will not fire again, <code>null</code> will be returned.  Note that
     * the time returned can possibly be in the past, if the time that was computed
     * for the trigger to next fire has already arrived, but the scheduler has not yet
     * been able to fire the trigger (which would likely be due to lack of resources
     * e.g. threads).
     *
     * <p>The value returned is not guaranteed to be valid until after the <code>Trigger</code>
     * has been added to the scheduler.
     * </p>
     *
     * @see TriggerUtils#computeFireTimesBetween(org.quartz.spi.OperableTrigger, Calendar, java.util.Date, java.util.Date)
     */
    Date getNextFireTime();

    /**
     * Returns the previous time at which the <code>Trigger</code> fired.
     * If the trigger has not yet fired, <code>null</code> will be returned.
     */
    Date getPreviousFireTime();

    /**
     * Returns the next time at which the <code>Trigger</code> will fire,
     * after the given time. If the trigger will not fire after the given time,
     * <code>null</code> will be returned.
     */
    Date getFireTimeAfter(Date afterTime);

    /**
     * Returns the last time at which the <code>Trigger</code> will fire, if
     * the Trigger will repeat indefinitely, null will be returned.
     * 
     * <p>
     * Note that the return time *may* be in the past.
     * </p>
     */
    Date getFinalFireTime();

    /**
     * Get the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be set as this property's value.
     * 
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * </p>
     * 
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see SimpleTrigger
     * @see CronTrigger
     */
    int getMisfireInstruction();

    /**
     * Get a {@link TriggerBuilder} that is configured to produce a 
     * <code>Trigger</code> identical to this one.
     * 
     * @see #getScheduleBuilder()
     */
    default TriggerBuilder<? extends Trigger> getTriggerBuilder(){
        System.out.println(" org.quartz.Trigger:getTriggerBuilder() is not defined!");
        return null;
    }
    
    /**
     * Get a {@link ScheduleBuilder} that is configured to produce a 
     * schedule identical to this trigger's schedule.
     * 
     * @see #getTriggerBuilder()
     */
    default ScheduleBuilder<? extends Trigger> getScheduleBuilder(){
        System.out.println("method getScheduleBuilder is not defined!");
        return null;
    }


    /**
     * Trigger equality is based upon the equality of the TriggerKey.
     * 
     * @return true if the key of this Trigger equals that of the given Trigger.
     */
    @Override
    boolean equals(Object other);
    
    /**
     * <p>
     * Compare the next fire time of this <code>Trigger</code> to that of
     * another by comparing their keys, or in other words, sorts them
     * according to the natural (i.e. alphabetical) order of their keys.
     * </p>
     */
    @Override
    int compareTo(Trigger other);

    /**
     * A Comparator that compares trigger's next fire times, or in other words,
     * sorts them according to earliest next fire time.  If the fire times are
     * the same, then the triggers are sorted according to priority (highest
     * value first), if the priorities are the same, then they are sorted
     * by key.
     */
    class TriggerTimeComparator implements Comparator<Trigger>, Serializable {
      
        private static final long serialVersionUID = -3904243490805975570L;
        
        // This static method exists for comparator in TC clustered quartz
//        public static int compare(Date nextFireTime1, int priority1, TriggerKey key1, Date nextFireTime2, int priority2, TriggerKey key2) {
//        public static int compare(Date nextFireTime1, int priority1,Key key1, Date nextFireTime2, int priority2,Key key2) {
        public static int compare(Date nextFireTime1, int priority1,String key1, Date nextFireTime2, int priority2,String key2) {
            if (nextFireTime1 != null || nextFireTime2 != null) {
                if (nextFireTime1 == null) {
                    return 1;
                }
                if (nextFireTime2 == null) {
                    return -1;
                }
                if(nextFireTime1.before(nextFireTime2)) {
                    return -1;
                }
                if(nextFireTime1.after(nextFireTime2)) {
                    return 1;
                }
            }
            int comp = priority2 - priority1;
            if (comp != 0) {
                return comp;
            }
            return key1.compareTo(key2);
        }
        @Override
        public int compare(Trigger t1, Trigger t2) {
            return compare(t1.getNextFireTime(), t1.getPriority(), t1.getKey(), t2.getNextFireTime(), t2.getPriority(), t2.getKey());
        }
    }
}
