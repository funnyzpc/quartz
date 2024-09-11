
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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.StatefulJob;
import org.quartz.Trigger;
import org.quartz.utils.ClassUtils;
import org.quartz.utils.Key;


/**
 * <p>
 * Conveys the detail properties of a given <code>Job</code> instance.
 * </p>
 * 
 * <p>
 * Quartz does not store an actual instance of a <code>Job</code> class, but
 * instead allows you to define an instance of one, through the use of a <code>JobDetail</code>.
 * </p>
 * 
 * <p>
 * <code>Job</code>s have a name and group associated with them, which
 * should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * 
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s
 * are scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * 
 * @see Job
 * @see StatefulJob
 * @see JobDataMap
 * @see Trigger
 * 
 * @author James House
 * @author Sharada Jambula
 */
@SuppressWarnings("deprecation")
public class JobDetailImpl implements Cloneable, java.io.Serializable, JobDetail {

    private static final long serialVersionUID = -6069784757781506897L;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
//    @Deprecated
//    private String name;
//    @Deprecated
//    private String group = Scheduler.DEFAULT_GROUP;

    private String description;

    private Class<? extends Job> jobClass;

    private JobDataMap jobDataMap;
    private String jobData;

//    private boolean durability = false;

    private boolean shouldRecover = false;

//    private transient JobKey key = null;
    @Deprecated
    private transient Key key = null;
    private transient QrtzExecute eJob;

    /*
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    *
    * Constructors.
    *
    * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    */

    /**
     * <p>
     * Create a <code>JobDetail</code> with no specified name or group, and
     * the default settings of all the other properties.
     * </p>
     * 
     * <p>
     * Note that the {@link #setName(String)},{@link #setGroup(String)}and
     * {@link #setJobClass(Class)}methods must be called before the job can be
     * placed into a {@link Scheduler}
     * </p>
     */
    public JobDetailImpl() {
        // do nothing...
        this.eJob=null;
    }
    public JobDetailImpl(QrtzExecute eJob) {
        this.eJob=eJob;
        this.jobClass=eJob.getJobClazz();
//        setName(name);
//        this.setKey(new Key(triggerName));
//        setJobClass(jobClass);
    }
    /**
     * <p>
     * Create a <code>JobDetail</code> with the given name, given class, default group,
     * and the default settings of all the other properties.
     * </p>
     *
     * @exception IllegalArgumentException
     *              if name is null or empty, or the group is an empty string.
     *
     * @deprecated use {@link JobBuilder}
     */
    public JobDetailImpl(String triggerName, Class<? extends Job> jobClass) {
//        setName(name);
        this.setKey(new Key(triggerName));
        setJobClass(jobClass);
        this.eJob=null;
    }

//    /**
//     * <p>
//     * Create a <code>JobDetail</code> with the given name, group and class,
//     * and the default settings of all the other properties.
//     * </p>
//     *
//     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
//     *
//     * @exception IllegalArgumentException
//     *              if name is null or empty, or the group is an empty string.
//     *
//     * @deprecated use {@link JobBuilder}
//     */
//    @Deprecated
//    public JobDetailImpl(String name, String group, Class<? extends Job> jobClass) {
//        setName(name);
//        setGroup(group);
//        setJobClass(jobClass);
//    }
//
//    /**
//     * <p>
//     * Create a <code>JobDetail</code> with the given name, and group, and
//     * the given settings of all the other properties.
//     * </p>
//     *
//     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
//     *
//     * @exception IllegalArgumentException
//     *              if name is null or empty, or the group is an empty string.
//     *
//     * @deprecated use {@link JobBuilder}
//     */
//    @Deprecated
//    public JobDetailImpl(String triggerName, /*String group, */Class<? extends Job> jobClass,/*boolean durability,*/ boolean recover) {
//        this.setKey(new Key(triggerName));
////        setGroup(group);
//        setJobClass(jobClass);
////        setDurability(durability);
//        setRequestsRecovery(recover);
//    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

//    /**
//     * <p>
//     * Get the name of this <code>Job</code>.
//     * </p>
//     */
//    public String getName() {
//        return name;
//    }

//    /**
//     * <p>
//     * Set the name of this <code>Job</code>.
//     * </p>
//     *
//     * @exception IllegalArgumentException
//     *              if name is null or empty.
//     */
//    public void setName(String name) {
//        if (name == null || name.trim().length() == 0) {
//            throw new IllegalArgumentException("Job name cannot be empty.");
//        }
//
//        this.name = name;
//        this.key = null;
//    }

//    /**
//     * <p>
//     * Get the group of this <code>Job</code>.
//     * </p>
//     */
//    @Deprecated
//    public String getGroup() {
//        return group;
//    }
//
//    /**
//     * <p>
//     * Set the group of this <code>Job</code>.
//     * </p>
//     *
//     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
//     *
//     * @exception IllegalArgumentException
//     *              if the group is an empty string.
//     */
//    @Deprecated
//    public void setGroup(String group) {
//        if (group != null && group.trim().length() == 0) {
//            throw new IllegalArgumentException("Group name cannot be empty.");
//        }
//        if (group == null) {
//            group = Scheduler.DEFAULT_GROUP;
//        }
//        this.group = group;
//        this.key = null;
//    }

//    /**
//     * <p>
//     * Returns the 'full name' of the <code>JobDetail</code> in the format
//     * "group.name".
//     * </p>
//     */
//    public String getFullName() {
//        return group + "." + name;
//    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getKey()
     */
    @Override
    public Key getKey() {
//        if(key == null) {
////            if(getName() == null){
////                return null;
////            }
////            key = new JobKey(getName(), getGroup());
//            key = new Key(getName());
//        }
        return this.key;
    }
    
    public JobDetailImpl setKey(Key key) {
        if(key == null){
            throw new IllegalArgumentException("Key cannot be null!");
        }
//        setName(key.getName());
//        setGroup(key.getGroup());
        this.key = key;
        return this;
    }
    public JobDetailImpl setKey(String triggerName) {
        if(triggerName == null){
            throw new IllegalArgumentException("triggerName cannot be null!");
        }
        this.key = new Key(triggerName);
        return this;
    }
    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Set a description for the <code>Job</code> instance - may be useful
     * for remembering/displaying the purpose of the job, though the
     * description has no meaning to Quartz.
     * </p>
     */
    public JobDetailImpl setDescription(String description) {
        this.description = description;
        return this;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getJobClass()
     */
    @Override
    public Class<? extends Job> getJobClass() {
        return this.jobClass;
    }
    @Override
    public String getJobClassName(){
        return eJob.getJob().getJobClass();
    }
    /**
     * <p>
     * Set the instance of <code>Job</code> that will be executed.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if jobClass is null or the class is not a <code>Job</code>.
     */
    public JobDetailImpl setJobClass(Class<? extends Job> jobClass) {
        if (jobClass == null) {
            throw new IllegalArgumentException("Job class cannot be null.");
        }
        if (!Job.class.isAssignableFrom(jobClass)) {
            throw new IllegalArgumentException("Job class must implement the Job interface.");
        }
        this.jobClass = jobClass;
        return this;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#getJobDataMap()
     */
    @Deprecated
    @Override
    public JobDataMap getJobDataMap() {
        if (jobDataMap == null) {
            jobDataMap = new JobDataMap();
        }
        return jobDataMap;
    }

    /**
     * <p>
     * Set the <code>JobDataMap</code> to be associated with the <code>Job</code>.
     * </p>
     */
    @Deprecated
    public JobDetailImpl setJobDataMap(JobDataMap jobDataMap) {
        this.jobDataMap = jobDataMap;
        return this;
    }

    public String getJobData() {
        return jobData;
    }

    public JobDetailImpl setJobData(String jobData) {
        this.jobData = jobData;
        return this;
    }

//    /**
//     * <p>
//     * Set whether or not the <code>Job</code> should remain stored after it
//     * is orphaned (no <code>{@link Trigger}s</code> point to it).
//     * </p>
//     *
//     * <p>
//     * If not explicitly set, the default value is <code>false</code>.
//     * </p>
//     */
//    public JobDetailImpl setDurability(boolean durability) {
//        this.durability = durability;
//        return this;
//    }

    /**
     * <p>
     * Set whether or not the the <code>Scheduler</code> should re-execute
     * the <code>Job</code> if a 'recovery' or 'fail-over' situation is
     * encountered.
     * </p>
     * 
     * <p>
     * If not explicitly set, the default value is <code>false</code>.
     * </p>
     * 
     * @see JobExecutionContext#isRecovering()
     */
    public JobDetailImpl setRequestsRecovery(boolean shouldRecover) {
        this.shouldRecover = shouldRecover;
        return this;
    }

//    /* (non-Javadoc)
//     * @see org.quartz.JobDetailI#isDurable()
//     */
//    @Override
//    public boolean isDurable() {
//        return durability;
//    }

    /**
     *  执行后保留作业数据
     * @return whether the associated Job class carries the {@link PersistJobDataAfterExecution} annotation.
     */
    @Deprecated
    @Override
    public boolean isPersistJobDataAfterExecution() {
        return ClassUtils.isAnnotationPresent(jobClass, PersistJobDataAfterExecution.class);
    }

    /**
     * 不允许并发执行
     * @return whether the associated Job class carries the {@link DisallowConcurrentExecution} annotation.
     */
    @Deprecated
    @Override
    public boolean isConcurrentExectionDisallowed() {
        return ClassUtils.isAnnotationPresent(jobClass, DisallowConcurrentExecution.class);
    }

    /* (non-Javadoc)
     * @see org.quartz.JobDetailI#requestsRecovery()
     */
    @Override
    public boolean requestsRecovery() {
        return shouldRecover;
    }

    /**
     * <p>
     * Return a simple string representation of this object.
     * </p>
     */
    @Override
    public String toString() {
//        return "JobDetail '" + getFullName() + "':  jobClass: '"
        return "JobDetail :  jobClass: '"
                + getJobClassName()
                + " concurrentExectionDisallowed: " + isConcurrentExectionDisallowed() 
                + " persistJobDataAfterExecution: " + isPersistJobDataAfterExecution() 
                + /*" isDurable: " + isDurable() +*/ " requestsRecovers: " + requestsRecovery();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JobDetail)) {
            return false;
        }
        JobDetail other = (JobDetail) obj;
        if(other.getKey() == null || getKey() == null){
            return false;
        }
        if (!other.getKey().equals(getKey())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
//        JobKey key = getKey();
        Key key = getKey();
        return key == null ? 0 : getKey().hashCode();
    }
    
    @Override
    public Object clone() {
        JobDetailImpl copy;
        try {
            copy = (JobDetailImpl) super.clone();
            if (jobDataMap != null) {
                copy.jobDataMap = (JobDataMap) jobDataMap.clone();
            }
        } catch (CloneNotSupportedException ex) {
            throw new IncompatibleClassChangeError("Not Cloneable.");
        }
        return copy;
    }
    @Override
    public JobBuilder getJobBuilder() {
        JobBuilder b = JobBuilder.newJob()
            .ofType(getJobClass())
            .requestRecovery(requestsRecovery())
//            .storeDurably(isDurable())
            .usingJobData(getJobDataMap())
            .withDescription(getDescription())
            .withIdentity(getKey());
        return b;
    }
    @Override
    public QrtzExecute getEJob() {
        return eJob;
    }
    // 这个只是预留，正常都需要从构造函数传入!
    public JobDetailImpl setEJob(QrtzExecute eJob) {
        this.eJob=eJob;
        return this;
    }

}
