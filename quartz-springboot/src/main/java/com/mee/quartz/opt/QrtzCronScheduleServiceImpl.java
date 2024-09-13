//package com.mee.quartz.opt;
//
//import org.quartz.CronScheduleBuilder;
//import org.quartz.CronTrigger;
//import org.quartz.Job;
//import org.quartz.JobBuilder;
//import org.quartz.JobDataMap;
//import org.quartz.JobDetail;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
//import org.quartz.Trigger;
//import org.quartz.TriggerBuilder;
//import org.quartz.utils.Key;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.util.Date;
//import java.util.HashSet;
//
///**
// * qrtz表达式任务
// *
// * @author shaoow
// * @version 1.0
// * @className QrtzCronScheduleServiceImpl
// * @date 2023/3/23 14:53
// */
//@Service
//public class QrtzCronScheduleServiceImpl {
//    /**
//     * 日志
//     */
//    private static final Logger log = LoggerFactory.getLogger(QrtzCronScheduleServiceImpl.class);
//
//    /**
//     * 定时任务组件
//     */
//    private final Scheduler scheduler;
//
//    public QrtzCronScheduleServiceImpl(Scheduler scheduler) {
//        this.scheduler = scheduler;
//    }
//
////    public void addCronJob2( String job_name,String job_class_name,String cron_ex) {
////        final String description = "测试-"+System.currentTimeMillis();
////        // 具体业务参数
////        JobDataMap data_map = new JobDataMap();
////        data_map.put("_D",description);
////        data_map.put("_CT", System.currentTimeMillis());
////        try {
////            if (checkExists(job_name)) {
////                throw new RuntimeException(String.format("任务已存在, jobName:[%s]", job_name));
////            }
////            final String jobData = JacksonUtil.toJsonString(data_map);
////            ExecuteCfgImpl executeCfg = new ExecuteCfgImpl(job_name, cron_ex);
////            JobCfgImpl jobCfg = new JobCfgImpl(job_name, job_class_name, "CRON", description, jobData, executeCfg);
////
////            Date date = scheduler.scheduleJob(jobDetail, trigger);
////            System.out.println("最近一次执行时间:"+date);
////        } catch (SchedulerException | ClassNotFoundException e) {
////            log.error("任务添加失败:{}",job_class_name,e);
////        }
////    }
////
////    /**
////     * 添加cron表达式任务
////     *
////     * @param param 任务参数
////     *      {
////     *      "job_name": "每两分钟执行一次",
////     *      "job_class_name": "com.mee.quartz.job.BTestJob",
////     *      "job_group_name": "G001",
////     *      "job_description": "任务描述",
////     *      "cron_expression": "2 ?/2 ? ? ? ?",
////     *       "data": {
////     *          "aa": 1,"status": true,"channel":101
////     *      }
////     *  }
////     */
////    public void addCronJob( String job_name,String job_class_name,String cron_ex) {
////        final String description = "测试"+System.currentTimeMillis();
////        // 具体业务参数
////        JobDataMap data_map = new JobDataMap();
////        data_map.put("data",description);
////        data_map.put("create_time", System.currentTimeMillis());
////        try {
////            if (checkExists(job_name)) {
////                throw new RuntimeException(String.format("任务已存在, jobName:[%s]", job_name));
////            }
////            Key key = Key.key(job_name);
////            CronScheduleBuilder schedBuilder = CronScheduleBuilder.cronSchedule(cron_ex).withMisfireHandlingInstructionDoNothing();
////            CronTrigger trigger = TriggerBuilder.newTrigger()
//////                    .withIdentity(triggerKey)
////                    .withIdentity(key)
////                    .withSchedule(schedBuilder)
////                    .withDescription(description)
////                    .build();
//////            trigger.getJobDataMap()
////            Class<? extends Job> clazz = (Class<? extends Job>) Class.forName(job_class_name);
////            JobDetail jobDetail = JobBuilder
////                    .newJob(clazz)
////                    .withIdentity(key)
////                    .withDescription(description)
////                    .usingJobData(data_map)
////                    .build();
////            Date date = scheduler.scheduleJob(jobDetail, trigger);
////            System.out.println("最近一次执行时间:"+date);
////        } catch (SchedulerException | ClassNotFoundException e) {
////            log.error("任务添加失败:{}",job_class_name,e);
////        }
////    }
////
////    /**
////     * 修改定时任务
////     *
////     * @param param .
////     */
////    public void editCronJob(String job_name,String job_class_name,String cron_ex) {
////        final String description = "editCronJob测试"+System.currentTimeMillis();
////        // 具体业务参数
////        JobDataMap data_map = new JobDataMap();
////        data_map.put("data",description);
////        data_map.put("create_time", System.currentTimeMillis());
////        try {
////            if (!checkExists(job_name)) {
////                log.error( String.format("Job不存在, job_name:{%s},job_group:{%s}", job_name));
////                return;
////            }
//////            TriggerKey triggerKey = TriggerKey.triggerKey(job_name);
//////            JobKey jobKey = new JobKey(job_name);
////            Key jobKey = new Key(job_name);
////            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder
////                    .cronSchedule(cron_ex)
////                    .withMisfireHandlingInstructionDoNothing();
////            CronTrigger cronTrigger = TriggerBuilder.newTrigger()
////                    .withIdentity(jobKey)
////                    .withSchedule(cronScheduleBuilder)
////                    .withDescription(description).build();
////            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
////            jobDetail = jobDetail.getJobBuilder().withDescription(description).usingJobData(data_map).build();
////            HashSet<Trigger> triggerSet = new HashSet<>();
////            triggerSet.add(cronTrigger);
////            scheduler.scheduleJob(jobDetail, triggerSet, true);
////        } catch (SchedulerException e) {
////            log.error("类名不存在或执行表达式错误:{}",job_name,e);
////        }
////    }
////
////
////    /**
////     * 验证任务是否存在
////     *
////     * @param job_name  任务名称
////     * @return
////     * @throws SchedulerException
////     */
////    private boolean checkExists(String job_name) throws SchedulerException {
////        Key triggerKey = Key.key(job_name);
////        return scheduler.checkExists(triggerKey);
////    }
//
//}
