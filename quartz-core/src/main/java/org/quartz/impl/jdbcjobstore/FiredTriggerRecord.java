///*
// * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License. You may obtain a copy
// * of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// *
// */
//
//package org.quartz.impl.jdbcjobstore;
//
//import org.quartz.utils.Key;
//
///**
// * <p>
// * Conveys the state of a fired-trigger record.
// *  传送已触发的触发器记录的状态。
// * </p>
// *
// * @author James House
// */
//public class FiredTriggerRecord implements java.io.Serializable {
//
//    private static final long serialVersionUID = -7183096398865657533L;
//
//    /*
//     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//     *
//     * Data members.
//     *
//     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//     */
//
//    private String fireInstanceId;
//
//    private long fireTimestamp;
//
//    private long scheduleTimestamp;
//
//    private String schedulerInstanceId;
////    @Deprecated
////    private TriggerKey triggerKey;
//    private Key key;
//
//    private String fireInstanceState;
//
////    @Deprecated
////    private JobKey jobKey;
//
//    private boolean jobDisallowsConcurrentExecution;
//
//    private boolean jobRequestsRecovery;
//
//    private int priority;
//
//    /*
//     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//     *
//     * Interface.
//     *
//     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//     */
//
//    public String getFireInstanceId() {
//        return fireInstanceId;
//    }
//
//    public long getFireTimestamp() {
//        return fireTimestamp;
//    }
//
//    public long getScheduleTimestamp() {
//        return scheduleTimestamp;
//    }
//
//    public boolean isJobDisallowsConcurrentExecution() {
//        return jobDisallowsConcurrentExecution;
//    }
//
////    public JobKey getJobKey() {
////        return jobKey;
////    }
////    public void setJobKey(JobKey key) {
////        jobKey = key;
////    }
//    public String getSchedulerInstanceId() {
//        return schedulerInstanceId;
//    }
//
////    @Deprecated
////    public TriggerKey getTriggerKey() {
////        return triggerKey;
////    }
////    @Deprecated
////    public void setTriggerKey(TriggerKey key) {
////        triggerKey = key;
////    }
//    public String getFireInstanceState() {
//        return fireInstanceState;
//    }
//
//    public void setFireInstanceId(String string) {
//        fireInstanceId = string;
//    }
//
//    public void setFireTimestamp(long l) {
//        fireTimestamp = l;
//    }
//
//    public void setScheduleTimestamp(long l) {
//        scheduleTimestamp = l;
//    }
//
//    public void setJobDisallowsConcurrentExecution(boolean b) {
//        jobDisallowsConcurrentExecution = b;
//    }
//    public void setSchedulerInstanceId(String string) {
//        schedulerInstanceId = string;
//    }
//
//    public void setFireInstanceState(String string) {
//        fireInstanceState = string;
//    }
//    public boolean isJobRequestsRecovery() {
//        return jobRequestsRecovery;
//    }
//    public void setJobRequestsRecovery(boolean b) {
//        jobRequestsRecovery = b;
//    }
//    public int getPriority() {
//        return priority;
//    }
//
//    public void setPriority(int priority) {
//        this.priority = priority;
//    }
//
//    public Key getKey() {
//        return key;
//    }
//
//    public void setKey(Key key) {
//        this.key = key;
//    }
//
//    @Override
//    public String toString() {
//        return "FiredTriggerRecord{" +
//                "fireInstanceId='" + fireInstanceId + '\'' +
//                ", fireTimestamp=" + fireTimestamp +
//                ", scheduleTimestamp=" + scheduleTimestamp +
//                ", schedulerInstanceId='" + schedulerInstanceId + '\'' +
//                ", key=" + key +
//                ", fireInstanceState='" + fireInstanceState + '\'' +
//                ", jobDisallowsConcurrentExecution=" + jobDisallowsConcurrentExecution +
//                ", jobRequestsRecovery=" + jobRequestsRecovery +
//                ", priority=" + priority +
//                '}';
//    }
//}
//
//// EOF
