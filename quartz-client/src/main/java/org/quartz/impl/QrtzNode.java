package org.quartz.impl;

import java.io.Serializable;

/**
 * QrtzNode
 *
 * @author shaoow
 * @version 1.0
 * @className QrtzNode
 * @date 2024/9/2 10:32
 */
public class QrtzNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 调度名称
     */
    private String application;
    /**
     * 实例机器IP
     */
    private String hostIp;
    /**
     * 实例机器名称
     */
    private String hostName;
    /**
     * 状态
     */
    private String state;
    /**
     * 检查时间
     */
    private Long timeCheck;

    public QrtzNode(String application, String hostIp, String hostName, String state, Long timeCheck) {
        this.application = application;
        this.hostIp = hostIp;
        this.hostName = hostName;
        this.state = state;
        this.timeCheck = timeCheck;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getTimeCheck() {
        return timeCheck;
    }

    public void setTimeCheck(Long timeCheck) {
        this.timeCheck = timeCheck;
    }

    @Override
    public String toString() {
        return "QrtzNode{" +
                "application='" + application + '\'' +
                ", hostIp='" + hostIp + '\'' +
                ", hostName='" + hostName + '\'' +
                ", state='" + state + '\'' +
                ", timeCheck=" + timeCheck +
                '}';
    }

}
