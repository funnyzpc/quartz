package org.quartz.impl;

import org.quartz.ExecuteCfg;

import java.io.Serializable;
import java.time.ZoneId;

/**
 * QrtzExecuteCfgImpl
 *
 * @author shaoow
 * @version 1.0
 * @className QrtzExecuteCfgImpl
 * @date 2024/7/12 13:55
 */
public class ExecuteCfgImpl implements ExecuteCfg, Serializable {
    /**
     * 序列化标识
     */
    private static final long serialVersionUID = 1L;


    /**
     * 调度名称(应用名称)
     */
    private String schedName;

    /**
     * 触发器的名称，qrtz_triggers表的TRIGGER_NAME的外键
     */
    private String triggerName;

    /**
     * 执行时间类型(CRON/SIMPLE)
     */
    private String triggerType;

    /**
     * CRON:cron表达式
     */
    private String cronExpression;

    /**
     * CRON:时区
     */
    private String timeZoneId;

    /**
     * SIMPLE:重复次数
     */
    private Long repeatCount;

    /**
     * SIMPLE:执行间隔
     */
    private Long repeatInterval;

    /**
     * SIMPLE:触发时间
     */
    private Long timesTriggered;

    public String getSchedName() {
        return schedName;
    }

    public ExecuteCfg setSchedName(String schedName) {
        this.schedName=schedName;
        return this;
    }
    public String getTriggerName() {
        return triggerName;
    }

    public ExecuteCfg setTriggerName(String triggerName) {
        this.triggerName=triggerName;
        return this;
    }
    @Override
    public String getTriggerType() {
        return triggerType;
    }

    public ExecuteCfg setTriggerType(String triggerType) {
        this.triggerType=triggerType;
        return this;
    }
    public String getCronExpression() {
        return cronExpression;
    }

    public ExecuteCfg setCronExpression(String cronExpression) {
        this.cronExpression=cronExpression;
        this.triggerType="CRON";
        return this;
    }
    public String getTimeZoneId() {
        return timeZoneId;
    }

    public ExecuteCfg setTimeZoneId(String timeZoneId) {
        this.timeZoneId=timeZoneId;
        return this;
    }
    public Long getRepeatCount() {
        return repeatCount;
    }

    public ExecuteCfg setRepeatCount(Long repeatCount) {
        this.repeatCount=repeatCount;
        return this;
    }
    public Long getRepeatInterval() {
        return repeatInterval;
    }

    public ExecuteCfg setRepeatInterval(Long repeatInterval) {
        this.repeatInterval=repeatInterval;
        return this;
    }
    public Long getTimesTriggered() {
        return timesTriggered;
    }

    public ExecuteCfg setTimesTriggered(Long timesTriggered) {
        this.timesTriggered=timesTriggered;
        return this;
    }

    @Override
    public String toString() {
        return "QrtzExecuteCfg::{"+
                "schedName:"+this.schedName+
                ", triggerName:"+this.triggerName+
                ", triggerType:"+this.triggerType+
                ", cronExpression:"+this.cronExpression+
                ", timeZoneId:"+this.timeZoneId+
                ", repeatCount:"+this.repeatCount+
                ", repeatInterval:"+this.repeatInterval+
                ", timesTriggered:"+this.timesTriggered+
                "}";
    }
    // for cron
    public ExecuteCfgImpl(String triggerName, String cronExpression) {
        this.triggerName = triggerName;
        this.cronExpression = cronExpression;
        this.triggerType="CRON";
        this.timeZoneId=ZoneId.systemDefault().getId();
    }

    // for cron
    public ExecuteCfgImpl(String schedName, String triggerName, String triggerType, String cronExpression, String timeZoneId) {
        this.schedName = schedName;
        this.triggerName = triggerName;
        this.triggerType = triggerType;
        this.cronExpression = cronExpression;
        this.timeZoneId = timeZoneId;
    }

    // for simple
    public ExecuteCfgImpl(String triggerName, Long repeatCount, Long repeatInterval) {
        this.triggerName = triggerName;
        this.repeatCount = repeatCount;
        this.repeatInterval = repeatInterval;
        this.triggerType="SIMPLE";
        this.timesTriggered = 0L;
    }

    // for simple
    public ExecuteCfgImpl(String schedName, String triggerName, String triggerType, Long repeatCount, Long repeatInterval, Long timesTriggered) {
        this.schedName = schedName;
        this.triggerName = triggerName;
        this.triggerType = triggerType;
        this.repeatCount = repeatCount;
        this.repeatInterval = repeatInterval;
        this.timesTriggered = timesTriggered;
    }

    @Override
    public boolean checkCfg(String type) {
        if("CRON".equals(type)){
            return this.checkCronCfg();
        }else if( "SIMPLE".equals(type) ){
            return this.checkSimpleCfg();
        }else{
            return false;
        }
    }

    private boolean checkCronCfg() {
        if(this.getTimeZoneId()==null){
            this.setTimeZoneId(ZoneId.systemDefault().getId());
        }
        if( this.getCronExpression()==null || this.getCronExpression().length()<10
            || null==this.getTriggerType() || !this.getTriggerType().equals("CRON")
            || null==this.getTriggerName() || "".equals(this.getTriggerName().trim())
            || null==this.getTimeZoneId()  || "".equals(this.getTimeZoneId().trim())){
            return false;
        }
        return true;
    }

    private boolean checkSimpleCfg() {
        if(this.getTimesTriggered()==null){
            this.setTimesTriggered(0L);
        }
        if( this.getRepeatCount()==null || this.getRepeatInterval()==null
                || null==this.getTriggerType() || !this.getTriggerType().equals("SIMPLE")
                || null==this.getTriggerName() || "".equals(this.getTriggerName().trim())
                || null==this.getTimesTriggered()  ){
            return false;
        }
        return false;
    }


}
