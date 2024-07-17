//package org.quartz.core;
//
//import static org.quartz.utils.Key.key;
//
//import java.beans.BeanInfo;
//import java.beans.IntrospectionException;
//import java.beans.Introspector;
//import java.beans.MethodDescriptor;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.text.ParseException;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicLong;
//
//import javax.management.ListenerNotFoundException;
//import javax.management.MBeanNotificationInfo;
//import javax.management.NotCompliantMBeanException;
//import javax.management.Notification;
//import javax.management.NotificationBroadcasterSupport;
//import javax.management.NotificationEmitter;
//import javax.management.NotificationFilter;
//import javax.management.NotificationListener;
//import javax.management.StandardMBean;
//import javax.management.openmbean.CompositeData;
//import javax.management.openmbean.TabularData;
//
//import org.quartz.JobDataMap;
//import org.quartz.JobDetail;
//import org.quartz.JobExecutionContext;
//import org.quartz.JobListener;
//import org.quartz.SchedulerException;
//import org.quartz.SchedulerListener;
//import org.quartz.Trigger;
//import org.quartz.Trigger.TriggerState;
//import org.quartz.core.jmx.JobDetailSupport;
//import org.quartz.core.jmx.JobExecutionContextSupport;
//import org.quartz.core.jmx.QuartzSchedulerMBean;
//import org.quartz.core.jmx.TriggerSupport;
//import org.quartz.impl.matchers.GroupMatcher;
//import org.quartz.impl.triggers.AbstractTrigger;
//import org.quartz.spi.OperableTrigger;
//import org.quartz.utils.Key;
//
//public class QuartzSchedulerMBeanImpl extends StandardMBean implements NotificationEmitter, QuartzSchedulerMBean, JobListener,SchedulerListener {
//    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
//
//    private final QuartzScheduler scheduler;
//    private boolean sampledStatisticsEnabled;
//    private SampledStatistics sampledStatistics;
//
//    private final static SampledStatistics NULL_SAMPLED_STATISTICS = new NullSampledStatisticsImpl();
//
//    static {
//        final String[] notifTypes = new String[] { SCHEDULER_STARTED, SCHEDULER_PAUSED, SCHEDULER_SHUTDOWN, };
//        final String name = Notification.class.getName();
//        final String description = "QuartzScheduler JMX Event";
//        NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes,name,description), };
//    }
//
//    /**
//     * emitter
//     */
//    protected final Emitter emitter = new Emitter();
//
//    /**
//     * sequenceNumber
//     */
//    protected final AtomicLong sequenceNumber = new AtomicLong();
//
//    /**
//     * QuartzSchedulerMBeanImpl
//     *
//     * @throws NotCompliantMBeanException
//     */
//    protected QuartzSchedulerMBeanImpl(QuartzScheduler scheduler) throws NotCompliantMBeanException {
//        super(QuartzSchedulerMBean.class);
//        this.scheduler = scheduler;
//        this.scheduler.addInternalJobListener(this);
//        this.scheduler.addInternalSchedulerListener(this);
//        this.sampledStatistics = NULL_SAMPLED_STATISTICS;
//        this.sampledStatisticsEnabled = false;
//    }
//    @Override
//    public TabularData getCurrentlyExecutingJobs() throws Exception {
//        try {
//            List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
//            return JobExecutionContextSupport.toTabularData(currentlyExecutingJobs);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
////    @Override
////    public TabularData getAllJobDetails() throws Exception {
////        try {
////            List<JobDetail> detailList = new ArrayList<JobDetail>();
////            for (String jobGroupName : scheduler.getJobGroupNames()) {
////                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroupName))) {
////                    detailList.add(scheduler.getJobDetail(jobKey));
////                }
////            }
////            return JobDetailSupport.toTabularData(detailList.toArray(new JobDetail[detailList.size()]));
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
////    @Override
////    public List<CompositeData> getAllTriggers() throws Exception {
////        try {
////            List<Trigger> triggerList = new ArrayList<Trigger>();
////            for (String triggerGroupName : scheduler.getTriggerGroupNames()) {
////                for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroupName))) {
////                    triggerList.add(scheduler.getTrigger(triggerKey));
////                }
////            }
////            return TriggerSupport.toCompositeList(triggerList);
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//    @Override
//    public void addJob(CompositeData jobDetail, boolean replace) throws Exception {
//        try {
//            scheduler.addJob(JobDetailSupport.newJobDetail(jobDetail), replace);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
//    private static void invokeSetter(Object target, String attribute, Object value) throws Exception {
//        String setterName = "set" + Character.toUpperCase(attribute.charAt(0)) + attribute.substring(1);
//        Class<?>[] argTypes = {value.getClass()};
//        Method setter = findMethod(target.getClass(), setterName, argTypes);
//        if(setter != null) {
//            setter.invoke(target, value);
//        } else {
//            throw new Exception("Unable to find setter for attribute '" + attribute + "' and value '" + value + "'");
//        }
//    }
//
//    private static Class<?> getWrapperIfPrimitive(Class<?> c) {
//        Class<?> result = c;
//        try {
//            Field f = c.getField("TYPE");
//            f.setAccessible(true);
//            result = (Class<?>) f.get(null);
//        } catch (Exception e) {
//            /**/
//        }
//        return result;
//    }
//
//    private static Method findMethod(Class<?> targetType, String methodName,Class<?>[] argTypes) throws IntrospectionException {
//        BeanInfo beanInfo = Introspector.getBeanInfo(targetType);
//        if (beanInfo != null) {
//            for(MethodDescriptor methodDesc: beanInfo.getMethodDescriptors()) {
//                Method method = methodDesc.getMethod();
//                Class<?>[] parameterTypes = method.getParameterTypes();
//                if (methodName.equals(method.getName()) && argTypes.length == parameterTypes.length) {
//                    boolean matchedArgTypes = true;
//                    for (int i = 0; i < argTypes.length; i++) {
//                        if (getWrapperIfPrimitive(argTypes[i]) != parameterTypes[i]) {
//                            matchedArgTypes = false;
//                            break;
//                        }
//                    }
//                    if (matchedArgTypes) {
//                        return method;
//                    }
//                }
//            }
//        }
//        return null;
//    }
//    @Override
//    public void scheduleBasicJob(Map<String, Object> jobDetailInfo,Map<String, Object> triggerInfo) throws Exception {
//        try {
//            JobDetail jobDetail = JobDetailSupport.newJobDetail(jobDetailInfo);
//            OperableTrigger trigger = TriggerSupport.newTrigger(triggerInfo);
//            scheduler.deleteJob(jobDetail.getKey());
//            scheduler.scheduleJob(jobDetail, trigger);
//        } catch (ParseException pe) {
//            throw pe;
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void scheduleJob(Map<String, Object> abstractJobInfo,Map<String, Object> abstractTriggerInfo) throws Exception {
//        try {
//            String triggerClassName = (String) abstractTriggerInfo.remove("triggerClass");
//            if(triggerClassName == null) {
//                throw new IllegalArgumentException("No triggerClass specified");
//            }
//            Class<?> triggerClass = Class.forName(triggerClassName);
//            Trigger trigger = (Trigger) triggerClass.newInstance();
//
//            String jobDetailClassName = (String) abstractJobInfo.remove("jobDetailClass");
//            if(jobDetailClassName == null) {
//                throw new IllegalArgumentException("No jobDetailClass specified");
//            }
//            Class<?> jobDetailClass = Class.forName(jobDetailClassName);
//            JobDetail jobDetail = (JobDetail) jobDetailClass.newInstance();
//
//            String jobClassName = (String) abstractJobInfo.remove("jobClass");
//            if(jobClassName == null) {
//                throw new IllegalArgumentException("No jobClass specified");
//            }
//            Class<?> jobClass = Class.forName(jobClassName);
//            abstractJobInfo.put("jobClass", jobClass);
//
//            for(Map.Entry<String, Object> entry : abstractTriggerInfo.entrySet()) {
//                String key = entry.getKey();
//                Object value = entry.getValue();
//                if("jobDataMap".equals(key)) {
//                    value = new JobDataMap((Map<?, ?>)value);
//                }
//                invokeSetter(trigger, key, value);
//            }
//
//            for(Map.Entry<String, Object> entry : abstractJobInfo.entrySet()) {
//                String key = entry.getKey();
//                Object value = entry.getValue();
//                if("jobDataMap".equals(key)) {
//                    value = new JobDataMap((Map<?, ?>)value);
//                }
//                invokeSetter(jobDetail, key, value);
//            }
//
//            AbstractTrigger<?> at = (AbstractTrigger<?>)trigger;
////            at.setKey(new TriggerKey(at.getName(), at.getGroup()));
//            at.setKey(new Key(at.getName()));
//
//            Date startDate = at.getStartTime();
//            if(startDate == null || startDate.before(new Date())) {
//                at.setStartTime(new Date());
//            }
//
//            scheduler.deleteJob(jobDetail.getKey());
//            scheduler.scheduleJob(jobDetail, trigger);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void scheduleJob(String jobName,Map<String, Object> abstractTriggerInfo) throws Exception {
//        try {
////            JobKey jobKey = new JobKey(jobName);
//            Key jobKey = new Key(jobName);
//            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
//            if(jobDetail == null) {
//                throw new IllegalArgumentException("No such job '" + jobKey + "'");
//            }
//            String triggerClassName = (String) abstractTriggerInfo.remove("triggerClass");
//            if(triggerClassName == null) {
//                throw new IllegalArgumentException("No triggerClass specified");
//            }
//            Class<?> triggerClass = Class.forName(triggerClassName);
//            Trigger trigger = (Trigger) triggerClass.newInstance();
//
//            for(Map.Entry<String, Object> entry : abstractTriggerInfo.entrySet()) {
//                String key = entry.getKey();
//                Object value = entry.getValue();
//                if("jobDataMap".equals(key)) {
//                    value = new JobDataMap((Map<?, ?>)value);
//                }
//                invokeSetter(trigger, key, value);
//            }
//
//            AbstractTrigger<?> at = (AbstractTrigger<?>)trigger;
////            at.setKey(new TriggerKey(at.getName(), at.getGroup()));
//            at.setKey(new Key(at.getName()));
//
//            Date startDate = at.getStartTime();
//            if(startDate == null || startDate.before(new Date())) {
//                at.setStartTime(new Date());
//            }
//
//            scheduler.scheduleJob(trigger);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void addJob(Map<String, Object> abstractJobInfo, boolean replace) throws Exception {
//        try {
//            String jobDetailClassName = (String) abstractJobInfo.remove("jobDetailClass");
//            if(jobDetailClassName == null) {
//                throw new IllegalArgumentException("No jobDetailClass specified");
//            }
//            Class<?> jobDetailClass = Class.forName(jobDetailClassName);
//            JobDetail jobDetail = (JobDetail) jobDetailClass.newInstance();
//
//            String jobClassName = (String) abstractJobInfo.remove("jobClass");
//            if(jobClassName == null) {
//                throw new IllegalArgumentException("No jobClass specified");
//            }
//            Class<?> jobClass = Class.forName(jobClassName);
//            abstractJobInfo.put("jobClass", jobClass);
//
//            for(Map.Entry<String, Object> entry : abstractJobInfo.entrySet()) {
//                String key = entry.getKey();
//                Object value = entry.getValue();
//                if("jobDataMap".equals(key)) {
//                    value = new JobDataMap((Map<?, ?>)value);
//                }
//                invokeSetter(jobDetail, key, value);
//            }
//            scheduler.addJob(jobDetail, replace);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    private Exception newPlainException(Exception e) {
//        String type = e.getClass().getName();
//        if(type.startsWith("java.") || type.startsWith("javax.")) {
//            return e;
//        } else {
//            Exception result = new Exception(e.getMessage());
//            result.setStackTrace(e.getStackTrace());
//            return result;
//        }
//    }
//
////    @Override
////    public void deleteCalendar(String calendarName) throws Exception {
////        try {
////            scheduler.deleteCalendar(calendarName);
////        } catch(Exception e) {
////            throw newPlainException(e);
////        }
////    }
//
//    @Override
//    public boolean deleteJob(String jobName) throws Exception {
//        try {
//            return scheduler.deleteJob(key(jobName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
////    @Override
////    public List<String> getCalendarNames() throws Exception {
////        try {
////            return scheduler.getCalendarNames();
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//    @Override
//    public CompositeData getJobDetail(String jobName, String jobGroupName) throws Exception {
//        try {
////            JobDetail jobDetail = scheduler.getJobDetail(jobKey(jobName, jobGroupName));
//            JobDetail jobDetail = scheduler.getJobDetail(key(jobName));
//            return JobDetailSupport.toCompositeData(jobDetail);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
////    public List<String> getJobGroupNames()    throws Exception {
////        try {
////            return scheduler.getJobGroupNames();
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//    @Override
//    public List<String> getJobNames(String groupName) throws Exception {
//        try {
//            List<String> jobNames = new ArrayList<String>();
//            // todo 需要修正
//            for(Key key: scheduler.getAllJobKeysInSched(groupName)) {
//                jobNames.add(key.getName());
//            }
//            return jobNames;
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public String getJobStoreClassName() {
//        return scheduler.getJobStoreClass().getName();
//    }
//
////    public Set<String> getPausedTriggerGroups() throws Exception {
////        try {
////            return scheduler.getPausedTriggerGroups();
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//
//    @Deprecated
//    @Override
//    public CompositeData getTrigger(String name, String groupName) throws Exception {
//        try {
////            Trigger trigger = scheduler.getTrigger(triggerKey(name, groupName));
//            Trigger trigger = scheduler.getTrigger(key(name));
//            return TriggerSupport.toCompositeData(trigger);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
////    public List<String> getTriggerGroupNames()    throws Exception {
////        try {
////            return scheduler.getTriggerGroupNames();
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
////    @Override
////    public List<String> getTriggerNames(String groupName) throws Exception {
////        try {
////            List<String> triggerNames = new ArrayList<String>();
////            for(TriggerKey key: scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupName))) {
////                triggerNames.add(key.getName());
////            }
////            return triggerNames;
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//    @Override
//    public String getTriggerState(String triggerName, String triggerGroupName) throws Exception {
//        try {
////            TriggerKey triggerKey = triggerKey(triggerName, triggerGroupName);
//            Key triggerKey = key(triggerName);
//            TriggerState ts = scheduler.getTriggerState(triggerKey);
//            return ts.name();
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public List<CompositeData> getTriggersOfJob(String jobName) throws Exception {
//        try {
//            Key jobKey = new Key(jobName);
//            return TriggerSupport.toCompositeList(scheduler.getTriggersOfJob(jobKey));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public boolean interruptJob(String jobName, String jobGroupName) throws Exception {
//        try {
////            return scheduler.interrupt(jobKey(jobName, jobGroupName));
//            return scheduler.interrupt(key(jobName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public boolean interruptJob(String fireInstanceId) throws Exception {
//        try {
//            return scheduler.interrupt(fireInstanceId);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public Date scheduleJob(String jobName, String jobGroup,String triggerName, String triggerGroup) throws Exception {
//        try {
////            JobKey jobKey = jobKey(jobName, jobGroup);
//            Key jobKey = new Key(jobName);
//            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
//            if (jobDetail == null) {
//                throw new IllegalArgumentException("No such job: " + jobKey);
//            }
////            TriggerKey triggerKey = triggerKey(triggerName, triggerGroup);
//            Key triggerKey = key(triggerName);
//            Trigger trigger = scheduler.getTrigger(triggerKey);
//            if (trigger == null) {
//                throw new IllegalArgumentException("No such trigger: " + triggerKey);
//            }
//            return scheduler.scheduleJob(jobDetail, trigger);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public boolean unscheduleJob(String triggerName, String triggerGroup) throws Exception {
//        try {
////            return scheduler.unscheduleJob(triggerKey(triggerName, triggerGroup));
//            return scheduler.unscheduleJob(key(triggerName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//   public void clear() throws Exception {
//       try {
//           scheduler.clear();
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public String getVersion() {
//        return scheduler.getVersion();
//    }
//    @Override
//    public boolean isShutdown() {
//        return scheduler.isShutdown();
//    }
//    @Override
//    public boolean isStarted() {
//        return scheduler.isStarted();
//    }
//    @Override
//    public void start() throws Exception {
//        try {
//            scheduler.start();
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void shutdown() {
//        scheduler.shutdown();
//    }
//    @Override
//    public void standby() {
//        scheduler.standby();
//    }
//    @Override
//    public boolean isStandbyMode() {
//        return scheduler.isInStandbyMode();
//    }
//    @Override
//    public String getSchedulerName() {
//        return scheduler.getSchedulerName();
//    }
//    @Override
//    public String getSchedulerInstanceId() {
//        return scheduler.getSchedulerInstanceId();
//    }
//    @Override
//    public String getThreadPoolClassName() {
//        return scheduler.getThreadPoolClass().getName();
//    }
//    @Override
//    public int getThreadPoolSize() {
//        return scheduler.getThreadPoolSize();
//    }
//    @Override
//    public void pauseJob(String jobName) throws Exception {
//        try {
//            scheduler.pauseJob(key(jobName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
//    public void pauseJobs(final String triggerName) throws Exception {
//        try {
//            scheduler.pauseJobs(triggerName);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void pauseJobGroup(final String triggerName) throws Exception {
//        pauseJobs(triggerName);
//    }
////    @Override
////    public void pauseJobsStartingWith(String jobGroupPrefix) throws Exception {
////        pauseJobs(GroupMatcher.<JobKey>groupStartsWith(jobGroupPrefix));
////    }
////    @Override
////    public void pauseJobsEndingWith(String jobGroupSuffix) throws Exception {
////        pauseJobs(GroupMatcher.<JobKey>groupEndsWith(jobGroupSuffix));
////    }
////    @Override
////    public void pauseJobsContaining(String jobGroupToken) throws Exception {
////        pauseJobs(GroupMatcher.<JobKey>groupContains(jobGroupToken));
////    }
//    @Override
//    public void pauseJobsAll() throws Exception {
//        pauseJobs(null);
//    }
//    @Override
//    public void pauseAllTriggers() throws Exception {
//        try {
//            scheduler.pauseAll();
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
////    private void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws Exception {
////        try {
////            scheduler.pauseTriggers(matcher);
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
////    @Override
////    public void pauseTriggerGroup(String triggerGroup) throws Exception {
////        pauseTriggers(GroupMatcher.<TriggerKey>groupEquals(triggerGroup));
////    }
////    @Override
////    public void pauseTriggersStartingWith(String triggerGroupPrefix) throws Exception {
////        pauseTriggers(GroupMatcher.<TriggerKey>groupStartsWith(triggerGroupPrefix));
////    }
////    @Override
////    public void pauseTriggersEndingWith(String triggerGroupSuffix) throws Exception {
////        pauseTriggers(GroupMatcher.<TriggerKey>groupEndsWith(triggerGroupSuffix));
////    }
////    @Override
////    public void pauseTriggersContaining(String triggerGroupToken) throws Exception {
////        pauseTriggers(GroupMatcher.<TriggerKey>groupContains(triggerGroupToken));
////    }
////    @Override
////    public void pauseTriggersAll() throws Exception {
////        pauseTriggers(GroupMatcher.anyTriggerGroup());
////    }
//    @Override
//    public void pauseTrigger(String triggerName, String triggerGroup) throws Exception {
//        try {
////            scheduler.pauseTrigger(triggerKey(triggerName, triggerGroup));
//            scheduler.pauseTrigger(key(triggerName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
////    @Override
////    public void resumeAllTriggers() throws Exception {
////        try {
////            scheduler.resumeAll();
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//    @Override
//    public void resumeJob(String jobName, String jobGroup) throws Exception {
//        try {
////            scheduler.resumeJob(jobKey(jobName, jobGroup));
//            scheduler.resumeJob(key(jobName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
//    public void resumeJobs(final String triggerName) throws Exception {
//        try {
//            scheduler.resumeJobs(triggerName);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void resumeJobGroup(final String triggerName) throws Exception {
//        resumeJobs(triggerName);
//    }
////    @Override
////    public void resumeJobsStartingWith(String jobGroupPrefix) throws Exception {
////        resumeJobs(GroupMatcher.<JobKey>groupStartsWith(jobGroupPrefix));
////    }
////    @Override
////    public void resumeJobsEndingWith(String jobGroupSuffix) throws Exception {
////        resumeJobs(GroupMatcher.<JobKey>groupEndsWith(jobGroupSuffix));
////    }
////    @Override
////    public void resumeJobsContaining(String jobGroupToken) throws Exception {
////        resumeJobs(GroupMatcher.<JobKey>groupContains(jobGroupToken));
////    }
//    @Override
//    public void resumeJobsAll() throws Exception {
//        resumeJobs(null);
//    }
//    @Override
//    public void resumeTrigger(String triggerName, String triggerGroup) throws Exception {
//        try {
////            scheduler.resumeTrigger(triggerKey(triggerName, triggerGroup));
//            scheduler.resumeTrigger(key(triggerName));
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//
//    private void resumeTriggers(GroupMatcher<Key<?>> matcher) throws Exception {
//        try {
//            scheduler.resumeTriggers(matcher);
//        } catch (Exception e) {
//            throw newPlainException(e);
//        }
//    }
//    @Override
//    public void resumeTriggerGroup(String triggerGroup) throws Exception {
//        resumeTriggers(GroupMatcher.groupEquals(triggerGroup));
//    }
////    @Override
////    public void resumeTriggersStartingWith(String triggerGroupPrefix) throws Exception {
////        resumeTriggers(GroupMatcher.groupStartsWith(triggerGroupPrefix));
////    }
////    @Override
////    public void resumeTriggersEndingWith(String triggerGroupSuffix) throws Exception {
////        resumeTriggers(GroupMatcher.<TriggerKey>groupEndsWith(triggerGroupSuffix));
////    }
//    @Override
//    public void resumeTriggersContaining(String triggerGroupToken) throws Exception {
//        resumeTriggers(GroupMatcher.groupContains(triggerGroupToken));
//    }
//    @Override
//    public void resumeTriggersAll() throws Exception {
//        resumeTriggers(GroupMatcher.anyTriggerGroup());
//    }
////    @Override
////    public void triggerJob(String jobName,Map<String, String> jobDataMap) throws Exception {
////        try {
//////            scheduler.triggerJob(jobKey(jobName, jobGroup), new JobDataMap(jobDataMap));
////            scheduler.triggerJob(jobKey(jobName), new JobDataMap(jobDataMap));
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
////
////    public void triggerJob(CompositeData trigger) throws Exception {
////        try {
////            scheduler.triggerJob(TriggerSupport.newTrigger(trigger));
////        } catch (Exception e) {
////            throw newPlainException(e);
////        }
////    }
//
//    // ScheduleListener
//    @Override
//    public void jobAdded(JobDetail jobDetail) {
//        sendNotification(JOB_ADDED, JobDetailSupport.toCompositeData(jobDetail));
//    }
//    @Override
//    public void jobDeleted(Key jobKey) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("jobName", jobKey.getName());
////        map.put("jobGroup", jobKey.getGroup());
//        sendNotification(JOB_DELETED, map);
//    }
//    @Override
//    public void jobScheduled(Trigger trigger) {
//        sendNotification(JOB_SCHEDULED, TriggerSupport.toCompositeData(trigger));
//    }
//    @Override
//    public void jobUnscheduled(Key triggerKey) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("triggerName", triggerKey.getName());
////        map.put("triggerGroup", triggerKey.getGroup());
//        sendNotification(JOB_UNSCHEDULED, map);
//    }
//    @Override
//    public void schedulingDataCleared() {
//        sendNotification(SCHEDULING_DATA_CLEARED);
//    }
//    @Override
//    public void jobPaused(Key jobKey) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("jobName", jobKey.getName());
////        map.put("jobGroup", jobKey.getGroup());
//        sendNotification(JOBS_PAUSED, map);
//    }
//    @Override
//    public void jobsPaused(String jobGroup) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("jobName", null);
////        map.put("jobGroup", jobGroup);
//        sendNotification(JOBS_PAUSED, map);
//    }
//    @Override
//    public void jobsResumed(String jobGroup) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("jobName", null);
////        map.put("jobGroup", jobGroup);
//        sendNotification(JOBS_RESUMED, map);
//    }
//    @Override
//    public void jobResumed(Key key) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("jobName", key.getName());
////        map.put("jobGroup", jobKey.getGroup());
//        sendNotification(JOBS_RESUMED, map);
//    }
//    @Override
//    public void schedulerError(String msg, SchedulerException cause) {
//        sendNotification(SCHEDULER_ERROR, cause.getMessage());
//    }
//    @Override
//    public void schedulerStarted() {
//        sendNotification(SCHEDULER_STARTED);
//    }
//
//    @Override
//    //not doing anything, just like schedulerShuttingdown
//    public void schedulerStarting() {
//    }
//    @Override
//    public void schedulerInStandbyMode() {
//        sendNotification(SCHEDULER_PAUSED);
//    }
//    @Override
//    public void schedulerShutdown() {
//        scheduler.removeInternalSchedulerListener(this);
//        scheduler.removeInternalJobListener(getName());
//        sendNotification(SCHEDULER_SHUTDOWN);
//    }
//    @Override
//    public void schedulerShuttingdown() {
//    }
//    @Override
//    public void triggerFinalized(Trigger trigger) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("triggerName", trigger.getKey().getName());
////        map.put("triggerGroup", trigger.getKey().getGroup());
//        sendNotification(TRIGGER_FINALIZED, map);
//    }
//    @Override
//    public void triggersPaused(String triggerGroup) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("triggerName", null);
////        map.put("triggerGroup", triggerGroup);
//        sendNotification(TRIGGERS_PAUSED, map);
//    }
//    @Override
//    public void triggerPaused(Key triggerKey) {
//        Map<String, String> map = new HashMap<String, String>();
//        if(triggerKey != null) {
//            map.put("triggerName", triggerKey.getName());
////            map.put("triggerGroup", triggerKey.getGroup());
//        }
//        sendNotification(TRIGGERS_PAUSED, map);
//    }
//    @Override
//    public void triggersResumed(String triggerGroup) {
//        Map<String, String> map = new HashMap<String, String>();
//        map.put("triggerName", null);
////        map.put("triggerGroup", triggerGroup);
//        sendNotification(TRIGGERS_RESUMED, map);
//    }
//    @Override
//    public void triggerResumed(Key triggerKey) {
//        Map<String, String> map = new HashMap<String, String>();
//        if(triggerKey != null) {
//            map.put("triggerName", triggerKey.getName());
////            map.put("triggerGroup", triggerKey.getGroup());
//        }
//        sendNotification(TRIGGERS_RESUMED, map);
//    }
//
//    // JobListener
//    @Override
//    public String getName() {
//        return "QuartzSchedulerMBeanImpl.listener";
//    }
////    @Override
////    public void jobExecutionVetoed(JobExecutionContext context) {
////        try {
////            sendNotification(JOB_EXECUTION_VETOED, JobExecutionContextSupport.toCompositeData(context));
////        } catch (Exception e) {
////            throw new RuntimeException(newPlainException(e));
////        }
////    }
//
//    // 待执行作业
//    @Override
//    public void jobToBeExecuted(JobExecutionContext context) {
//        try {
//            sendNotification(JOB_TO_BE_EXECUTED, JobExecutionContextSupport.toCompositeData(context));
//        } catch (Exception e) {
//            throw new RuntimeException(newPlainException(e));
//        }
//    }
////    @Override
////    public void jobWasExecuted(JobExecutionContext context,
////            JobExecutionException jobException) {
////        try {
////            sendNotification(JOB_WAS_EXECUTED, JobExecutionContextSupport.toCompositeData(context));
////        } catch (Exception e) {
////            throw new RuntimeException(newPlainException(e));
////        }
////    }
//
//    // NotificationBroadcaster
//
//    /**
//     * sendNotification
//     *
//     * @param eventType
//     */
//    public void sendNotification(String eventType) {
//        sendNotification(eventType, null, null);
//    }
//
//    /**
//     * sendNotification
//     *
//     * @param eventType
//     * @param data
//     */
//    public void sendNotification(String eventType, Object data) {
//        sendNotification(eventType, data, null);
//    }
//
//    /**
//     * sendNotification
//     *
//     * @param eventType
//     * @param data
//     * @param msg
//     */
//    public void sendNotification(String eventType, Object data, String msg) {
//        Notification notif = new Notification(eventType, this, sequenceNumber.incrementAndGet(), System.currentTimeMillis(), msg);
//        if (data != null) {
//            notif.setUserData(data);
//        }
//        emitter.sendNotification(notif);
//    }
//
//    /**
//     * @author gkeim
//     */
//    private class Emitter extends NotificationBroadcasterSupport {
//        /**
//         * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
//         */
//        @Override
//        public MBeanNotificationInfo[] getNotificationInfo() {
//            return QuartzSchedulerMBeanImpl.this.getNotificationInfo();
//        }
//    }
//
//    /**
//     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
//     *      javax.management.NotificationFilter, java.lang.Object)
//     */
//    @Override
//    public void addNotificationListener(NotificationListener notif,NotificationFilter filter, Object callBack) {
//        emitter.addNotificationListener(notif, filter, callBack);
//    }
//
//    /**
//     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
//     */
//    @Override
//    public MBeanNotificationInfo[] getNotificationInfo() {
//        return NOTIFICATION_INFO;
//    }
//
//    /**
//     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
//     */
//    @Override
//    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
//        emitter.removeNotificationListener(listener);
//    }
//
//    /**
//     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
//     *      javax.management.NotificationFilter, java.lang.Object)
//     */
//    @Override
//    public void removeNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack) throws ListenerNotFoundException {
//        emitter.removeNotificationListener(notif, filter, callBack);
//    }
//    @Override
//    public synchronized boolean isSampledStatisticsEnabled() {
//        return sampledStatisticsEnabled;
//    }
//    @Override
//    public void setSampledStatisticsEnabled(boolean enabled) {
//        if (enabled != this.sampledStatisticsEnabled) {
//            this.sampledStatisticsEnabled = enabled;
//            if(enabled) {
//                this.sampledStatistics = new SampledStatisticsImpl(scheduler);
//            }
//            else {
//                 this.sampledStatistics.shutdown();
//                 this.sampledStatistics = NULL_SAMPLED_STATISTICS;
//            }
//            sendNotification(SAMPLED_STATISTICS_ENABLED, Boolean.valueOf(enabled));
//        }
//    }
//    @Override
//    public long getJobsCompletedMostRecentSample() {
//        return this.sampledStatistics.getJobsCompletedMostRecentSample();
//    }
//    @Override
//    public long getJobsExecutedMostRecentSample() {
//        return this.sampledStatistics.getJobsExecutingMostRecentSample();
//    }
//    @Override
//    public long getJobsScheduledMostRecentSample() {
//        return this.sampledStatistics.getJobsScheduledMostRecentSample();
//    }
//    @Override
//    public Map<String, Long> getPerformanceMetrics() {
//        Map<String, Long> result = new HashMap<String, Long>();
//        result.put("JobsCompleted", Long.valueOf(getJobsCompletedMostRecentSample()));
//        result.put("JobsExecuted", Long.valueOf(getJobsExecutedMostRecentSample()));
//        result.put("JobsScheduled", Long.valueOf(getJobsScheduledMostRecentSample()));
//        return result;
//    }
//}
