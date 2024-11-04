//package com.mee.quartz.opt;
//
//import org.quartz.JobDetail;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
//import org.quartz.Trigger;
//import org.quartz.impl.triggers.CronTriggerImpl;
//import org.quartz.impl.triggers.SimpleTriggerImpl;
//import org.quartz.utils.Key;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//
//
///**
// * 任务配置Service业务层处理
// *
// * @author  shadow
// * @version v1.0
// * @date    2023-03-17 09:22:43
// */
//@Service
//public class QrtzTaskServiceImpl implements QrtzTaskService {
//    /**
//     * 日志
//     */
//    private static final Logger log = LoggerFactory.getLogger(QrtzTaskServiceImpl.class);
//
//    @Autowired
//    private QrtzCronScheduleServiceImpl qrtzCronScheduleService;
//    @Autowired
//    private QrtzSimpleScheduleServiceImpl qrtzSimpleScheduleService;
//
//    private final Scheduler scheduler;
//
//    public QrtzTaskServiceImpl(Scheduler scheduler) {
//        this.scheduler = scheduler;
//    }
//
//
//    /**
//     * 删除定时任务
//     *  删除任务是根据任务的job来删除的
//     *
//     * @param job_name :{"id":xxxxx}
//     */
//    @Override
//    public void deleteJob( String job_name  ) {
//        Key job_key = new Key(job_name);
//        try {
//            if (checkJobExists(job_name)) {
//                boolean delete_result = scheduler.deleteJob(job_key);
//                log.info("任务: {} 删除:{}", job_name,delete_result);
//                return ;
//            }
//            log.warn("未找到任务：{}", job_name);
//        } catch (SchedulerException e) {
//            log.error("删除任务出现异常:{}",job_name,e);
//        }
//    }
//
//    /**
//     * 纯粹删除
//     * @param job_name .
//     * @return .
//     */
//    @Override
//    public void deleteJobPure( String job_name  )  {
//        Key job_key = new Key(job_name);
//        try{
//            boolean delete_result = scheduler.deleteJob(job_key);
//            log.info("任务: {} 删除:{}", job_name,delete_result);
//        }catch (SchedulerException e){
//            log.error("删除任务出错了:{}",job_name,e);
//            throw new RuntimeException(e.getMessage());
//        }
//    }
//    /**
//     * 暂停定时任务
//     *
//     * @param job_name {"id":xxxx}
//     * return .
//     */
//    @Override
//    public void pauseJob(String job_name) {
//        Key job_key = new Key(job_name);
//        try {
//            if (checkJobExists(job_name)) {
//                scheduler.pauseJob(job_key);
//                log.info("任务: {} 暂停成功", job_name);
//                return ;
//            }
//            log.warn("未找到任务：{}", job_name);
//        } catch (SchedulerException e) {
//            log.error("暂停任务出现异常:{}",job_name,e);
//        }
//    }
//
//    /**
//     * 恢复暂停任务
//     *
//     * @param job_name { "id":xxxx}
//     */
//    @Override
//    public void resumeJob( String job_name ) {
//        Key job_key = new Key(job_name);
//        try {
//            if (checkJobExists(job_name)) {
//                scheduler.resumeJob(job_key);
//                log.info("任务: {} 恢复成功", job_name);
//                return ;
//            }
//            log.warn("未找到任务：{}", job_name);
//        } catch (SchedulerException e) {
//            log.error("恢复暂停任务出现异常:{}",job_name,e);
//        }
//    }
//
//    /**
//     * 获取任务状态
//     */
//    @Override
//    public void jobStatus(String job_name)  {
//        Key job_key = new Key(job_name);
//        try {
//            JobDetail jobDetail = scheduler.getJobDetail(job_key);
//            // 是不可以跨集群获取job信息的
//            if( null== jobDetail) {
//                return ;
//            }
//            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
//            List<Map> result = new ArrayList<>(triggers.size());
//            for (Trigger trigger : triggers) {
//                Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
//                Map<String, Object> item = new HashMap<>(24);
//                // 实例id是自动生成的，实例名称则是集群唯一标识
//                item.put("instance_id",scheduler.getMetaData().getSchedulerInstanceId());
//                item.put("instance_name",scheduler.getMetaData().getSchedulerName());
////                item.put("job_name",trigger.getJobKey().getName());
////                item.put("job_group",trigger.getJobKey().getGroup());
//                // NONE, NORMAL, PAUSED, COMPLETE, ERROR, BLOCKED
//                item.put("trigger_state",triggerState.name());
//                item.put("start_time",trigger.getStartTime());
//                item.put("end_time",trigger.getEndTime());
//                item.put("next_fire_time",trigger.getNextFireTime());
//                item.put("previous_fire_time",trigger.getPreviousFireTime());
//                item.put("trigger_name",trigger.getKey().getName());
////                item.put("trigger_group",trigger.getKey().getGroup());
//
////                item.put("description",trigger.getDescription());
//                item.put("description",jobDetail.getDescription());
////                item.put("job_data_map", JacksonUtil.toJsonString(trigger.getJobDataMap()));
////                item.put("job_data_map", JacksonUtil.toJsonString(jobDetail.getJobDataMap()));
//                item.put("job_data_map", jobDetail.getJobDataMap());
//                item.put("calendar_name",trigger.getCalendarName());
//                item.put("misfire_instruction",trigger.getMisfireInstruction());
//                item.put("priority",trigger.getPriority());
//                // class_full_name
//                item.put("job_class_name",jobDetail.getJobClassName());
//                item.put("job_type",trigger.getClass().getSimpleName().toUpperCase(Locale.ROOT).replace("TRIGGERIMPL",""));
//                if( trigger instanceof CronTriggerImpl){
//                    CronTriggerImpl cron_trigger1 =  (CronTriggerImpl)trigger;
//                    item.put("cron_ex",cron_trigger1.getCronExpression());
//                    item.put("fire_instance_id",cron_trigger1.getFireInstanceId());
//                    item.put("time_zone_id",cron_trigger1.getTimeZone().getID());
//                }
//                if( trigger instanceof SimpleTriggerImpl){
//                    SimpleTriggerImpl simple_trigger1 =  (SimpleTriggerImpl)trigger;
//                    item.put("repeat_count",simple_trigger1.getRepeatCount());
//                    item.put("repeat_interval",simple_trigger1.getRepeatInterval());
//                    item.put("times_triggered",simple_trigger1.getTimesTriggered());
//                    item.put("fire_instance_id",simple_trigger1.getFireInstanceId());
//                }
//                result.add(item);
//            }
//        }catch (SchedulerException e){
//            log.error("恢复暂停任务出现异常:{}",job_name,e);
//        }
//    }
//
//    /**
//     * 验证任务是否存在
//     *
//     * @param job_name  任务名称
//     * @return
//     * @throws SchedulerException
//     */
//    private boolean checkJobExists(String job_name) throws SchedulerException {
//        Key triggerKey = new Key(job_name);
//        return scheduler.checkExists(triggerKey);
//    }
//
//    /**
//     *  检查业务参数
//     * @param param
//     * @return
//     */
//    private Boolean checkJobParam(Map param){
//        if(  null==param.get("job_type") || null==param.get("job_name") || null==param.get("job_class_name") || null==param.get("job_group") || null==param.get("description") || null==param.get("instance_addr")){
//            return Boolean.FALSE;
//        }
//        return Boolean.TRUE;
//    }
//
//
//}
