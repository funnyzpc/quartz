
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

package org.quartz.ee.jta;

import org.quartz.ExecuteInJTATransaction;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShell;
import org.quartz.core.JobRunShellFactory;
import org.quartz.impl.QrtzExecute;
import org.quartz.utils.ClassUtils;

/**
 * <p>
 * Responsible for creating the instances of a {@link JobRunShell}
 * to be used within the <class>{@link org.quartz.core.QuartzScheduler}
 * </code> instance.  It will create a standard {@link JobRunShell}
 * unless the job class has the {@link ExecuteInJTATransaction}
 * annotation in which case it will create a {@link JTAJobRunShell}.
 * </p>
 * 
 * <p>
 * This implementation does not re-use any objects, it simply makes a new
 * JTAJobRunShell each time <code>borrowJobRunShell()</code> is called.
 * </p>
 *
 * 负责创建在org.quartz.core中使用的JobRunShell实例。QuartzScheduler实例。它将创建一个标准的JobRunShell，
 * 除非作业类具有ExecuteInJTATransaction注释，在这种情况下，它将创建JTAJobRunShell。
 * 此实现不重用任何对象，它只是在每次调用borrowJobRunShell（）时创建一个新的JTAJobRunShell。
 *
 * @author James House
 */
public class JTAAnnotationAwareJobRunShellFactory implements JobRunShellFactory {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private Scheduler scheduler;

//    private final ClassLoadHelper classLoadHelper;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public JTAAnnotationAwareJobRunShellFactory() {
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
     * Initialize the factory, providing a handle to the <code>Scheduler</code>
     * that should be made available within the <code>JobRunShell</code> and
     * the <code>JobExecutionContext</code> s within it, and a handle to the
     * <code>SchedulingContext</code> that the shell will use in its own
     * operations with the <code>JobStore</code>.
     * </p>
     */
    @Override
    public void initialize(Scheduler sched)
        throws SchedulerConfigException {
        this.scheduler = sched;
    }

//    /**
//     * <p>
//     * Called by the <class>{@link org.quartz.core.QuartzSchedulerThread}
//     * </code> to obtain instances of <code>
//     * {@link org.quartz.core.JobRunShell}</code>.
//     * </p>
//     */
//    @Deprecated
//    @Override
//    public JobRunShell createJobRunShell(TriggerFiredBundle bundle) throws SchedulerException {
//        ExecuteInJTATransaction jtaAnnotation = ClassUtils.getAnnotation(bundle.getJobDetail().getJobClass(), ExecuteInJTATransaction.class);
//        if(jtaAnnotation == null){
//            return new JobRunShell(scheduler, bundle);
//        }
//        else {
//            int timeout = jtaAnnotation.timeout();
//            if (timeout >= 0) {
//                return new JTAJobRunShell(scheduler, bundle, timeout);
//            } else {
//                return new JTAJobRunShell(scheduler, bundle);
//            }
//        }
//    }

    /**
     * <p>
     * Called by the <class>{@link org.quartz.core.QuartzSchedulerThread}
     * </code> to obtain instances of <code>
     * {@link org.quartz.core.JobRunShell}</code>.
     * </p>
     */
    @Override
    public JobRunShell createJobRunShell(QrtzExecute eJob) throws SchedulerException {
//        Class<? extends Job> jobClass= null;
//        try {
//            jobClass = classLoadHelper.loadClass(eJob.getJob().getJobClass(), Job.class);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
        Class<? extends Job> jobClazz = eJob.getJobClazz();
        ExecuteInJTATransaction jtaAnnotation = (jobClazz!=null)?ClassUtils.getAnnotation(jobClazz, ExecuteInJTATransaction.class):null;
        if(jtaAnnotation == null){
            return new JobRunShell(scheduler,eJob,null);
        }
        else {
            int timeout = jtaAnnotation.timeout();
            // todo : 这部分可以优化 jobClazz
            if (timeout >= 0) {
                return new JTAJobRunShell(scheduler,eJob,jobClazz,timeout);
            } else {
                return new JTAJobRunShell(scheduler,eJob,jobClazz,null);
            }
        }
    }

}