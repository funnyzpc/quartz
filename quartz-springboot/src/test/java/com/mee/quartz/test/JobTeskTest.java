package com.mee.quartz.test;

import com.mee.quartz.job.Job02TestService;
import com.mee.quartz.opt.QrtzCronScheduleServiceImpl;
import com.mee.quartz.opt.QrtzSimpleScheduleServiceImpl;
import com.mee.quartz.opt.QrtzTaskService;
import org.junit.jupiter.api.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Date;


/**
 * test
 *
 * @author shaoow
 * @version 1.0
 * @className JobTeskTest
 * @date 2024/6/26 14:03
 */

@SpringBootTest
public class JobTeskTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobTeskTest.class);

    @Autowired
    private QrtzTaskService qrtzTaskService;
    @Autowired
    private QrtzSimpleScheduleServiceImpl qrtzSimpleScheduleService;
    @Autowired
    private QrtzCronScheduleServiceImpl qrtzCronScheduleService;
    @Autowired
    private Scheduler scheduler;


    public static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * cron 任务
     */

    @Test
    public void test01() throws Exception {
        String job_name  = "Job01TestService:01任务";
        final String job_class_name = "com.mee.quartz.job.Job01TestService";
//        final String cron_ex = "0 * * * * ?";
        final String cron_ex = "0 0/1 * * * ?";
//        qrtzSimpleScheduleService.addSimpleJob(job_name,job_class_name,1,0);
        qrtzCronScheduleService.addCronJob(job_name,job_class_name,cron_ex);
//        TimeUnit.SECONDS.sleep(60);
    }

    /**
     * simple 任务
     */
    @Test
    public void test02() throws Exception {
        String job_name  = "Job02TestService::01任务";
        final String job_class_name = "com.mee.quartz.job.Job02TestService";
        Integer repeat_interval=10000;
        Integer repeat_count=1000;
        qrtzSimpleScheduleService.addSimpleJob( job_name, job_class_name, repeat_interval, repeat_count );
//        TimeUnit.SECONDS.sleep(60);
    }

    // 添加日历
    @Test
    public void test03() throws SchedulerException, ParseException, ClassNotFoundException {
        final String job_name  = "Job01TestService::execute::04";
        final String cron_ex = "0 * * * * ?";
        final String job_class_name = "com.mee.quartz.job.Job01TestService";
//        final String calendarName = "cron-skip-date";
        JobDataMap data_map = new JobDataMap();
        data_map.put("data","cron任务测试04");
        data_map.put("create_time", System.currentTimeMillis());
//        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        HolidayCalendar holidayCalendar = new HolidayCalendar();
//        // 假设你有一个方法getHolidayDates()可以返回所有节假日的日期列表
//        List<Date> holidayDates = Arrays.asList( fmt.parse("2024-07-08 14:10:00"));
//        for (Date date : holidayDates) {
//            holidayCalendar.addExcludedDate(date);
//        }
//        scheduler.addCalendar(calendarName, holidayCalendar, false, false);
//        TriggerKey triggerKey = TriggerKey.triggerKey(job_name);
//        JobKey jobKey = JobKey.jobKey(job_name);
        Key key = Key.key(job_name);
        CronScheduleBuilder schedBuilder = CronScheduleBuilder
                .cronSchedule(cron_ex)
                .withMisfireHandlingInstructionDoNothing();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(key)
                .withSchedule(schedBuilder)
                .withDescription("cron任务测试04")
//                .modifiedByCalendar(calendarName)
                .build();

        Class<? extends Job> clazz = (Class<? extends Job>) Class.forName(job_class_name);
        JobDetail jobDetail = JobBuilder
                .newJob(clazz)
                .withIdentity(key)
                .withDescription("cron任务测试04")
                .usingJobData(data_map)
                .build();
        Date date = scheduler.scheduleJob(jobDetail, trigger);
    }

    // 内存任务：simple
    @Test
    public void test04()throws Exception {
        // 创建 JobDetail 实例，并将其与 HelloJob 类绑定
        JobDetail job = JobBuilder.newJob(Job02TestService.class)
                .withIdentity("myJob")
                .build();
        // 创建 Trigger
        // 使用 SimpleScheduleBuilder 来创建一个简单的触发器，每10秒执行一次
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("myTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10) // 间隔10秒
                        .repeatForever()) // 无限重复
                .build();
        // 获取调度器
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        // 调度任务
        scheduler.scheduleJob(job, trigger);
        // 启动调度器
        scheduler.start();
        // 等待足够长的时间以便看到一些执行，然后关闭
        try {
            Thread.sleep(60000); // 等待一分钟
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭调度器
        scheduler.shutdown();
    }

    /**
     * 移除key
     * @throws Exception
     */
    @Test
    public void test05()throws Exception{
//        scheduler.pauseJob(new Key("Job01TestService::execute"));
//        scheduler.resumeJob(new Key("Job01TestService::execute"));
        // 删除前必须移除trigger
//        scheduler.unscheduleJob(new Key("Job01TestService:01任务"));
//        scheduler.deleteJob(new Key("Job01TestService:01任务"));

        scheduler.unscheduleJob(new Key("Job02TestService::01任务"));
        scheduler.deleteJob(new Key("Job02TestService::01任务"));


    }

}
