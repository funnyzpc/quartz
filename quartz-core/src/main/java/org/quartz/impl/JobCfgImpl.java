//package org.quartz.impl;
//
//import org.quartz.Calendar;
//import org.quartz.CronExpression;
//import org.quartz.ExecuteCfg;
//import org.quartz.JobCfg;
//import org.quartz.Trigger;
//
//import java.io.Serializable;
//import java.util.Date;
//
///**
// * 合并QRTZ_TRIGGERS & QRTZ_JOB_DETAILS对象 qrtz_job_cfg
// *
// * @author  ash
// * @version v1.1
// * @date    2024-07-12 13:42:11
// */
//public class JobCfgImpl implements JobCfg, Serializable {
//
//    /**
//     * 序列化标识
//     */
//    private static final long serialVersionUID = 1L;
//    /**
//     * ------------------------------------------------------------
//     *  执行配置 @see  org.quartz.impl.ExecuteCfgImpl
//     * ------------------------------------------------------------
//     */
//    private transient ExecuteCfg executeCfg;
//    /**
//     * 调度名称 添加或修改时可以为空，此时默认值为当前配置的${spring.quartz.properties.org.quartz.scheduler.instanceName}
//     */
//    private String schedName;
//    /**
//     * 触发器的名称,关联qrtz_job_details
//     */
//    private String triggerName;
//    /**
//     * '触发器的类型，使用cron表达式';
//     */
//    private String triggerType;
//    /**
//     * '当前触发器状态（ WAITING：等待； PAUSED：暂停； ACQUIRED：正常执行； BLOCKED：阻塞； ERROR：错误；）';
//     */
//    private String triggerState;
//    /**
//     * '详细描述信息';
//     */
//    private String description;
//    /**
//     * '下一次触发时间（毫秒），默认为-1，意味不会自动触发';
//     */
//    private Date nextFireTime;
//    /**
//     * '上一次触发时间（毫秒）';
//     */
//    private Long prevFireTime;
//    /**
//     * '优先级';
//     */
//    private Integer priority=5;
//    /**
//     * '开始时间';
//     */
//    private Date startTime;
//    /**
//     * '结束时间';
//     */
//    private Date endTime;
//    /**
//     * '日程表名称，表qrtz_calendars的CALENDAR_NAME字段的值';
//     */
//    private String calendarName;
//    /**
//     * '措施或者是补偿执行的策略';
//     */
//    private Integer misfireInstr= Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
//    /**
//     * 'DETAILS:集群中job实现类的全名，quartz就是根据这个路径到classpath找到该job类';
//     */
//    private String jobClassName;
////    /**
////     * 'DETAILS:是否持久化，把该属性设置为1，quartz会把job持久化到数据库中';
////     */
////    private Boolean isDurable;
//    /**
//     * 'DETAILS:是否并发执行';
//     */
//    private Boolean isNonconcurrent=false;
//    /**
//     * 'DETAILS:是否更新数据';
//     */
//    private Boolean isUpdateData=false;
//    /**
//     * 'DETAILS:是否接受恢复执行，默认为false，设置了RequestsRecovery为true，则该job会被重新执行';
//     */
//    private Boolean requestsRecovery=false;
//    /**
//     * 'DETAILS:一个blob字段，存放持久化job对象';
//     */
//    private String jobData;
//
//    public ExecuteCfg getExecuteCfg() {
//        return executeCfg;
//    }
//
//    public void setExecuteCfg(ExecuteCfg executeCfg) {
//        this.executeCfg = executeCfg;
//    }
//    @Override
//    public String getSchedName() {
//        return null==schedName||"".equals(schedName.trim())?null:schedName;
//    }
//    @Override
//    public JobCfgImpl setSchedName(String schedName) {
//        this.schedName=schedName;
//        return this;
//    }
//    public String getTriggerName() {
//        return triggerName;
//    }
//
//    public JobCfgImpl setTriggerName(String triggerName) {
//        this.triggerName=triggerName;
//        return this;
//    }
//    @Override
//    public String getTriggerType() {
//        return triggerType;
//    }
//
//    public JobCfgImpl setTriggerType(String triggerType) {
//        this.triggerType=triggerType;
//        return this;
//    }
//    public String getTriggerState() {
//        return triggerState;
//    }
//
//    public JobCfgImpl setTriggerState(String triggerState) {
//        this.triggerState=triggerState;
//        return this;
//    }
//    public String getDescription() {
//        return description;
//    }
//
//    public JobCfgImpl setDescription(String description) {
//        this.description=description;
//        return this;
//    }
//    public Date getNextFireTime() {
//        return nextFireTime;
//    }
//
//    public JobCfgImpl setNextFireTime(Date nextFireTime) {
//        this.nextFireTime=nextFireTime;
//        return this;
//    }
//    public Long getPrevFireTime() {
//        return prevFireTime;
//    }
//
//    public JobCfgImpl setPrevFireTime(Long prevFireTime) {
//        this.prevFireTime=prevFireTime;
//        return this;
//    }
//    public Integer getPriority() {
//        return priority;
//    }
//
//    public JobCfgImpl setPriority(Integer priority) {
//        this.priority=priority;
//        return this;
//    }
//    public Date getStartTime() {
//        return startTime;
//    }
//
//    public JobCfgImpl setStartTime(Date startTime) {
//        this.startTime=startTime;
//        return this;
//    }
//    public Date getEndTime() {
//        return endTime;
//    }
//
//    public JobCfgImpl setEndTime(Date endTime) {
//        this.endTime=endTime;
//        return this;
//    }
//    public String getCalendarName() {
//        return calendarName;
//    }
//
//    public JobCfgImpl setCalendarName(String calendarName) {
//        this.calendarName=calendarName;
//        return this;
//    }
//    public Integer getMisfireInstr() {
//        return misfireInstr;
//    }
//
//    public JobCfgImpl setMisfireInstr(Integer misfireInstr) {
//        this.misfireInstr=misfireInstr;
//        return this;
//    }
//    public String getJobClassName() {
//        return jobClassName;
//    }
//
//    public JobCfgImpl setJobClassName(String jobClassName) {
//        this.jobClassName=jobClassName;
//        return this;
//    }
////    public Boolean getIsDurable() {
////        return isDurable;
////    }
////
////    public JobCfgImpl setIsDurable(Boolean isDurable) {
////        this.isDurable=isDurable;
////        return this;
////    }
//    public Boolean getIsNonconcurrent() {
//        return isNonconcurrent;
//    }
//
//    public JobCfgImpl setIsNonconcurrent(Boolean isNonconcurrent) {
//        this.isNonconcurrent=isNonconcurrent;
//        return this;
//    }
//    public Boolean getIsUpdateData() {
//        return isUpdateData;
//    }
//
//    public JobCfgImpl setIsUpdateData(Boolean isUpdateData) {
//        this.isUpdateData=isUpdateData;
//        return this;
//    }
//    public Boolean getRequestsRecovery() {
//        return requestsRecovery;
//    }
//
//    public JobCfgImpl setRequestsRecovery(Boolean requestsRecovery) {
//        this.requestsRecovery=requestsRecovery;
//        return this;
//    }
//    public String getJobData() {
//        return jobData;
//    }
//
//    public JobCfgImpl setJobData(String jobData) {
//        this.jobData=jobData;
//        return this;
//    }
//
//    @Override
//    public String toString() {
//        return "JobCfgImpl::{"+
//                "executeCfg:"+this.executeCfg+
//                ", schedName:"+this.schedName+
//                ", triggerName:"+this.triggerName+
//                ", triggerType:"+this.triggerType+
//                ", triggerState:"+this.triggerState+
//                ", description:"+this.description+
//                ", nextFireTime:"+this.nextFireTime+
//                ", prevFireTime:"+this.prevFireTime+
//                ", priority:"+this.priority+
//                ", startTime:"+this.startTime+
//                ", endTime:"+this.endTime+
//                ", calendarName:"+this.calendarName+
//                ", misfireInstr:"+this.misfireInstr+
//                ", jobClassName:"+this.jobClassName+
////                ", isDurable:"+this.isDurable+
//                ", isNonconcurrent:"+this.isNonconcurrent+
//                ", isUpdateData:"+this.isUpdateData+
//                ", requestsRecovery:"+this.requestsRecovery+
//                ", jobData:"+this.jobData+
//                "}";
//    }
//
//    /**
//     * for cron
//     */
//    public JobCfgImpl(String triggerName,String jobClassName, String triggerType, String description, String jobData/*,ExecuteCfg executeCfg*/) {
//        this.triggerName = triggerName;
//        this.triggerType = triggerType;
//        this.description = description;
//        this.jobClassName = jobClassName;
//        this.jobData = jobData;
//        this.startTime=new Date(System.currentTimeMillis());
////        this.executeCfg = executeCfg;
//    }
//    // for cron
//    public JobCfgImpl(String schedName, String triggerName, String triggerType, String description, String jobClassName, String jobData,ExecuteCfg executeCfg) {
//        this.schedName = schedName;
//        this.triggerName = triggerName;
//        this.triggerType = triggerType;
//        this.description = description;
//        this.jobClassName = jobClassName;
//        this.jobData = jobData;
//        this.executeCfg = executeCfg;
//        this.startTime=new Date(System.currentTimeMillis());
//    }
//
//    @Override
//    public boolean checkCfg(String type) {
//       if("CRON".equals(type)){
//           return this.checkCronCfg();
//       }else if( "SIMPLE".equals(type) ){
//           return this.checkSimpleCfg();
//       }else{
//           return false;
//       }
//    }
//    public boolean checkCronCfg() {
//        if(null==this.getTriggerName() || "".equals(this.getTriggerName().trim())
//            || null==this.getJobClassName() || "".equals(this.getJobClassName().trim())
//            || null==this.getTriggerType() || !"CRON".equals(this.getTriggerType())
//            /*|| null==this.getExecuteCfg()*/ ){
//            return false;
//        }
//        return true;
//    }
//
//    public boolean checkSimpleCfg() {
//        // todo ...
//        if( null==this.getTriggerType() || !"SIMPLE".equals(this.getTriggerType()) ){
//            return false;
//        }
//        return true;
//    }
//
//    // for cron
//    public Date computeFirstFireTime(/*org.quartz.Calendar calendar*/CronExpression cronEx) {
//        this.nextFireTime = getFireTimeAfterForCron(new Date(this.getStartTime().getTime() - 1000L),cronEx);
////        while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(this.nextFireTime.getTime())) {
////            this.nextFireTime = getFireTimeAfter(this.nextFireTime);
////        }
//        return this.nextFireTime;
//    }
//    // cron
//    private Date getFireTimeAfterForCron(Date afterTime, CronExpression cronEx) {
//        if (afterTime == null) {
//            afterTime = new Date();
//        }
//        if (getStartTime().after(afterTime)) {
//            afterTime = new Date(getStartTime().getTime() - 1000L);
//        }
//        if (getEndTime() != null && (afterTime.compareTo(getEndTime()) >= 0)) {
//            return null;
//        }
////        Date pot = getTimeAfter(afterTime);
//        Date pot =  (cronEx == null) ? null : cronEx.getTimeAfter(afterTime);
//        if (getEndTime() != null && pot != null && pot.after(getEndTime())) {
//            return null;
//        }
//        return pot;
//    }
//
////    public Date computeFirstFireTime(Calendar calendar) {
////        final int YEAR_TO_GIVEUP_SCHEDULING_AT = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 100;
////        this.nextFireTime = getStartTime();
////        while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
////            nextFireTime = getFireTimeAfterForSimple(this.nextFireTime);
////            if(nextFireTime == null){
////                break;
////            }
////            //avoid infinite loop
////            java.util.Calendar c = java.util.Calendar.getInstance();
////            c.setTime(nextFireTime);
////            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
////                return null;
////            }
////        }
////
////        return nextFireTime;
////    }
//
////    public Date getFireTimeAfterForSimple(Date afterTime) {
////        boolean complete = false;
////        if (complete) {
////            return null;
////        }
////        if ((timesTriggered > repeatCount) && (repeatCount != REPEAT_INDEFINITELY)) {
////            return null;
////        }
////        if (afterTime == null) {
////            afterTime = new Date();
////        }
////        if (repeatCount == 0 && afterTime.compareTo(getStartTime()) >= 0) {
////            return null;
////        }
////        long startMillis = getStartTime().getTime();
////        long afterMillis = afterTime.getTime();
////        long endMillis = (getEndTime() == null) ? Long.MAX_VALUE : getEndTime().getTime();
////        if (endMillis <= afterMillis) {
////            return null;
////        }
////        if (afterMillis < startMillis) {
////            return new Date(startMillis);
////        }
////        long numberOfTimesExecuted = ((afterMillis - startMillis) / repeatInterval) + 1;
////
////        if ((numberOfTimesExecuted > repeatCount) &&
////                (repeatCount != REPEAT_INDEFINITELY)) {
////            return null;
////        }
////        Date time = new Date(startMillis + (numberOfTimesExecuted * repeatInterval));
////        if (endMillis <= time.getTime()) {
////            return null;
////        }
////        return time;
////    }
//
//}
