//package com.mee.quartz.opt;
//
//import org.quartz.Job;
//import org.quartz.JobBuilder;
//import org.quartz.JobDataMap;
//import org.quartz.JobDetail;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
//import org.quartz.SimpleScheduleBuilder;
//import org.quartz.SimpleTrigger;
//import org.quartz.Trigger;
//import org.quartz.TriggerBuilder;
//import org.quartz.utils.Key;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.Date;
//import java.util.HashSet;
//
///**
// * qrtz简单任务
// *
// * @author shaoow
// * @version 1.0
// * @className QrtzSimpleScheduleServiceImpl
// * @date 2023/3/23 14:52
// */
//@Service
//public class QrtzSimpleScheduleServiceImpl {
//    /**
//     * 日志
//     */
//    private static final Logger log = LoggerFactory.getLogger(QrtzSimpleScheduleServiceImpl.class);
//
//    /**
//     * 定时任务组件
//     */
//    private final Scheduler scheduler;
//
//    public QrtzSimpleScheduleServiceImpl(Scheduler scheduler) {
//        this.scheduler = scheduler;
//    }
//
//    public void addSimpleJob( String job_name,String job_class_name,Integer repeat_interval,Integer repeat_count) {
//        // 执行起始时间
//        final Date start_time =Date.from(LocalDateTime.now().plusSeconds(3).atZone(ZoneOffset.systemDefault()).toInstant());
//        // 执行终止时间(不再循环执行的时间)
//        final Date end_time =Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneOffset.systemDefault()).toInstant());
//
//        // 业务参数
//        JobDataMap dataMap = new JobDataMap();
//        dataMap.put("data","{}");
//        dataMap.put("create_time",System.currentTimeMillis());
//        try {
//            Key key = new Key(job_name);
//            /* 简单调度 */
//            SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(repeat_interval);
//            // 分三种 repeat_count -1.永远重复执行 0.不重复(也就是默认只执行一次) 1~∞ 指定的重复次数
//            // 以下可以不写
//            if( SimpleTrigger.REPEAT_INDEFINITELY==repeat_count ){
//                simpleScheduleBuilder.repeatForever();
//            }else{
//                simpleScheduleBuilder.withRepeatCount(repeat_count);
//            }
//            SimpleTrigger trigger = TriggerBuilder
//                    .newTrigger()
//                    .withIdentity(key)
//                    .startAt(start_time)
//                    .withSchedule(simpleScheduleBuilder)
//                    .endAt(end_time)
////                    .modifiedByCalendar("skip_date2")
//                    .build();
//            Class<? extends Job> clazz = (Class<? extends Job>) Class.forName(job_class_name);
//            JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(key).usingJobData(dataMap).build();
//            // 返回的是首次执行的时间
//            Date date = scheduler.scheduleJob(jobDetail, trigger);
//            System.out.println("最近一次执行时间:"+date);
//        } catch (SchedulerException | ClassNotFoundException e) {
//            log.error("任务添加失败:{}",job_class_name,e);
//        }
//    }
//
//    public void editSimpleJob(String job_name,String job_class_name,Integer repeat_interval,Integer repeat_count) {
//        // final String job_class_name = (String)param.get("job_class_name");
//        // 执行起始时间
//        final Date start_time =Date.from(LocalDateTime.now().plusSeconds(3).atZone(ZoneOffset.systemDefault()).toInstant());
//        // 执行终止时间(不再循环执行的时间)
//        final Date end_time =Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneOffset.systemDefault()).toInstant());
//
//        // 业务参数
//        JobDataMap data_map = new JobDataMap();
//        data_map.put("data","{\"action\":\"editSimpleJob\"}");
//        data_map.put("create_time", System.currentTimeMillis());
//        try {
////            TriggerKey triggerKey = TriggerKey.triggerKey(job_name);
////            JobKey job_key = new JobKey(job_name);
//            Key key = new Key(job_name);
//            if (!checkJobExists(key)) {
//                System.out.println(String.format("Job不存在, job_name:{%s}",job_name));
//                return;
//            }
//            // 简单调度
//            SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder
//                    .simpleSchedule()
//                    .withIntervalInMilliseconds(repeat_interval)
//                    .withRepeatCount(repeat_count);
//            SimpleTrigger trigger = TriggerBuilder
//                    .newTrigger()
//                    .withIdentity(key)
//                    .startAt(start_time)
//                    .withSchedule(simpleScheduleBuilder)
//                    .endAt(end_time)
//                    .build();
//            JobDetail jobDetail = scheduler.getJobDetail(key);
//            jobDetail = jobDetail.getJobBuilder().usingJobData(data_map).build();
//            HashSet<Trigger> triggerSet = new HashSet<>();
//            triggerSet.add(trigger);
//            scheduler.scheduleJob(jobDetail, triggerSet, true);
//        } catch (SchedulerException e) {
//            log.error("任务修改失败:{}",job_class_name,e);
//        }
//    }
//
//    /**
//     * 验证任务是否存在
//     *
//     * @param job_key  任务KEY(job_name,job_group)
//     * @return
//     * @throws SchedulerException
//     */
//    private boolean checkJobExists(Key job_key) throws SchedulerException {
//        return scheduler.checkExists(job_key);
//    }
//
//
//}
