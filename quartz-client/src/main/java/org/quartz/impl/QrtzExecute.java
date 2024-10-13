package org.quartz.impl;


import java.io.Serializable;
import java.util.Date;

/**
 * QrtzExecute
 *
 * @author shaoow
 * @version 1.0
 * @className QrtzExecute
 * @date 2024/9/2 10:46
 */
public class QrtzExecute implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;
    /**
     * 关联任务
     */
    private Long pid;
//    /**
//     * 任务执行项编号
//     */
//    private Integer executeIdx;
    /**
     * 任务类型
     */
    private String jobType;
    /**
     * 任务状态
     */
    private String state;
    /**
     * CRON:cron表达式
     */
    private String cron;
    /**
     * CRON:时区
     */
    private String zoneId;
    /**
     * SIMPLE
     */
    private Integer repeatCount;
    /**
     * SIMPLE
     */
    private Integer repeatInterval;
    /**
     * SIMPLE
     */
    private Integer timeTriggered;
    /**
     * 上一次执行时间
     */
    private Long prevFireTime;
    /**
     * 下一次执行时间
     */
    private Long nextFireTime;
    /**
     * 执行机器地址
     */
    private String hostIp;
    /**
     * 执行机器名称
     */
    private String hostName;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     *  扩展:job信息
     */
    private transient QrtzJob job;

    /**
     * 扩展:fire时间
     * */
    private transient Date _fireTime;
    /**
     * 扩展:fire时间
     * */
    private transient Date _scheduledFireTime;
//    /**
//     * 扩展:类对象
//     * */
//    private transient Class<? extends Job> jobClazz;

    public QrtzExecute(Long id,Long pid,/*Integer executeIdx,*/String jobType,String state,String cron,String zoneId,Integer repeatCount,Integer repeatInterval,Integer timeTriggered,Long prevFireTime,Long nextFireTime,String hostIp,String hostName,Long startTime,Long endTime){
        this.id=id;
        this.pid=pid;
        this.jobType=jobType;
        this.state=state;
        this.cron=cron;
        this.zoneId=zoneId;
        this.repeatCount=repeatCount;
        this.repeatInterval=repeatInterval;
        this.timeTriggered=timeTriggered;
        this.prevFireTime=prevFireTime;
        this.nextFireTime=nextFireTime;
        this.hostIp=hostIp;
        this.hostName=hostName;
        this.startTime=startTime;
        this.endTime=endTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPid() {
        return pid;
    }

    public void setPid(Long pid) {
        this.pid = pid;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public Integer getTimeTriggered() {
        return timeTriggered;
    }

    public void setTimeTriggered(Integer timeTriggered) {
        this.timeTriggered = timeTriggered;
    }

    public Long getPrevFireTime() {
        return prevFireTime;
    }

    public void setPrevFireTime(Long prevFireTime) {
        this.prevFireTime = prevFireTime;
    }

    public Long getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Long nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public QrtzJob getJob() {
        return job;
    }

    public QrtzExecute setJob(QrtzJob job) {
        this.job = job;
        return this;
    }

    public Date getFireTime() {
        return _fireTime;
    }

    public QrtzExecute setFireTime(long t) {
        this._fireTime = new Date(t);
        return this;
    }

    public Date getScheduledFireTime() {
        return _scheduledFireTime;
    }

    public QrtzExecute setScheduledFireTime(long t) {
        this._scheduledFireTime = new Date(t);
        return this;
    }

//    public Class<? extends Job> getJobClazz() {
//        return jobClazz;
//    }
//
//    public QrtzExecute setJobClazz(Class<? extends Job> jobClazz) {
//        this.jobClazz = jobClazz;
//        return this;
//    }

    @Override
    public String toString() {
        return "QrtzExecute{" +
                "id=" + id +
                ", pid=" + pid +
                ", jobType='" + jobType + '\'' +
                ", state='" + state + '\'' +
                ", cron='" + cron + '\'' +
                ", zoneId='" + zoneId + '\'' +
                ", repeatCount=" + repeatCount +
                ", repeatInterval=" + repeatInterval +
                ", timeTriggered=" + timeTriggered +
                ", prevFireTime=" + prevFireTime +
                ", nextFireTime=" + nextFireTime +
                ", hostIp='" + hostIp + '\'' +
                ", hostName='" + hostName + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", job=" + job +
                ", _fireTime=" + _fireTime +
                ", _scheduledFireTime=" + _scheduledFireTime +
                '}';
    }
}
