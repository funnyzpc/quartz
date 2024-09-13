package org.quartz.impl;

import java.io.Serializable;

/**
 * QrtzJob
 *
 * @author shaoow
 * @version 1.0
 * @className QrtzJob
 * @date 2024/9/2 10:40
 */
public class QrtzJob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;
    /**
     * 调度名称
     */
    private String application;
    /**
     * 任务状态
     */
    private String state;
//    /**
//     * 任务编号
//     */
//    private Integer jobIdx;
    /**
     * 任务全类名
     */
    private String jobClass;
    /**
     * 任务数据
     */
    private String jobData;
    /**
     * 任务描述
     */
    private String jobDescription;
    private Long updateTime;

    public QrtzJob(Long id, String application, String state, /*Integer jobIdx,*/ String jobClass, String jobData, String jobDescription,Long updateTime) {
        this.id = id;
        this.application = application;
        this.state = state;
//        this.jobIdx = jobIdx;
        this.jobClass = jobClass;
        this.jobData = jobData;
        this.jobDescription = jobDescription;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "QrtzJob{" +
                "id=" + id +
                ", application='" + application + '\'' +
                ", state='" + state + '\'' +
                ", jobClass='" + jobClass + '\'' +
                ", jobData='" + jobData + '\'' +
                ", jobDescription='" + jobDescription + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }
}
