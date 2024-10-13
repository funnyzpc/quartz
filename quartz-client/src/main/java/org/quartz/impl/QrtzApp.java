package org.quartz.impl;

import java.io.Serializable;

/**
 * QrtzApp
 *
 * @author shaoow
 * @version 1.0
 * @className QrtzApp
 * @date 2024/8/29 17:53
 */
public class QrtzApp implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 调度名称/应用名称
     */
    private String application;
    /**
     * 状态
     */
    private String state;
    /**
     * 上一次check时间
     */
    private Long timePre;
    /**
     * 下一次check时间
     */
    private Long timeNext;
    /**
     * check的检查间隔(毫秒)
     */
    private Long timeInterval;

    public QrtzApp(String application, String state, Long timePre, Long timeNext, Long timeInterval) {
        this.application = application;
        this.state = state;
        this.timePre = timePre;
        this.timeNext = timeNext;
        this.timeInterval = timeInterval;
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

    public Long getTimePre() {
        return timePre;
    }

    public void setTimePre(Long timePre) {
        this.timePre = timePre;
    }

    public Long getTimeNext() {
        return timeNext;
    }

    public void setTimeNext(Long timeNext) {
        this.timeNext = timeNext;
    }

    public Long getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(Long timeInterval) {
        this.timeInterval = timeInterval;
    }

    @Override
    public String toString() {
        return "QrtzApp{" +
                "application='" + application + '\'' +
                ", state='" + state + '\'' +
                ", timePre=" + timePre +
                ", timeNext=" + timeNext +
                ", timeInterval=" + timeInterval +
                '}';
    }
}
