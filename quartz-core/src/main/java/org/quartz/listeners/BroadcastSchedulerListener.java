//package org.quartz.listeners;
//
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//
//import org.quartz.JobDetail;
//import org.quartz.SchedulerException;
//import org.quartz.SchedulerListener;
//import org.quartz.Trigger;
//import org.quartz.utils.Key;
//
///**
// * Holds a List of references to SchedulerListener instances and broadcasts all
// * events to them (in order).
// * 保存对SchedulerListener实例的引用列表，并向它们广播所有事件（按顺序）
// *
// * <p>This may be more convenient than registering all of the listeners
// * directly with the Scheduler, and provides the flexibility of easily changing
// * which listeners get notified.</p>
// * 这可能比直接向Scheduler注册所有侦听器更方便，并提供了轻松更改哪些侦听器得到通知的灵活性。
// *
// * @see #addListener(org.quartz.SchedulerListener)
// * @see #removeListener(org.quartz.SchedulerListener)
// *
// * @author James House (jhouse AT revolition DOT net)
// */
//public class BroadcastSchedulerListener implements SchedulerListener {
//
//    private List<SchedulerListener> listeners;
//
//    public BroadcastSchedulerListener() {
//        listeners = new LinkedList<SchedulerListener>();
//    }
//
//    /**
//     * Construct an instance with the given List of listeners.
//     *
//     * @param listeners the initial List of SchedulerListeners to broadcast to.
//     */
//    public BroadcastSchedulerListener(List<SchedulerListener> listeners) {
//        this();
//        this.listeners.addAll(listeners);
//    }
//
//
//    public void addListener(SchedulerListener listener) {
//        listeners.add(listener);
//    }
//
//    public boolean removeListener(SchedulerListener listener) {
//        return listeners.remove(listener);
//    }
//
//    public List<SchedulerListener> getListeners() {
//        return java.util.Collections.unmodifiableList(listeners);
//    }
//    @Override
//    public void jobAdded(JobDetail jobDetail) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobAdded(jobDetail);
//        }
//    }
//    @Override
//    public void jobDeleted(Key jobKey) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobDeleted(jobKey);
//        }
//    }
//    @Override
//    public void jobScheduled(Trigger trigger) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobScheduled(trigger);
//        }
//    }
//    @Override
//    public void jobUnscheduled(Key triggerKey) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobUnscheduled(triggerKey);
//        }
//    }
//    @Override
//    public void triggerFinalized(Trigger trigger) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.triggerFinalized(trigger);
//        }
//    }
//    @Override
//    public void triggerPaused(Key key) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.triggerPaused(key);
//        }
//    }
//    @Override
//    public void triggersPaused(String triggerGroup) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.triggersPaused(triggerGroup);
//        }
//    }
//    @Override
//    public void triggerResumed(Key key) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.triggerResumed(key);
//        }
//    }
//    @Override
//    public void triggersResumed(String triggerGroup) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.triggersResumed(triggerGroup);
//        }
//    }
//    @Override
//    public void schedulingDataCleared() {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulingDataCleared();
//        }
//    }
//    @Override
//    public void jobPaused(Key key) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobPaused(key);
//        }
//    }
//    @Override
//    public void jobsPaused(String jobGroup) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobsPaused(jobGroup);
//        }
//    }
//    @Override
//    public void jobResumed(Key key) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobResumed(key);
//        }
//    }
//    @Override
//    public void jobsResumed(String jobGroup) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.jobsResumed(jobGroup);
//        }
//    }
//    @Override
//    public void schedulerError(String msg, SchedulerException cause) {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulerError(msg, cause);
//        }
//    }
//    @Override
//    public void schedulerStarted() {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulerStarted();
//        }
//    }
//    @Override
//    public void schedulerStarting() {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while (itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulerStarting();
//        }
//    }
//    @Override
//    public void schedulerInStandbyMode() {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulerInStandbyMode();
//        }
//    }
//    @Override
//    public void schedulerShutdown() {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulerShutdown();
//        }
//    }
//    @Override
//    public void schedulerShuttingdown() {
//        Iterator<SchedulerListener> itr = listeners.iterator();
//        while(itr.hasNext()) {
//            SchedulerListener l = itr.next();
//            l.schedulerShuttingdown();
//        }
//    }
//
//}
