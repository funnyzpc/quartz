package com.quartz.client.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.StdScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


/**
 * StdSchedulerTest
 *
 * @author shaoow
 * @version 1.0
 * @className StdSchedulerTest
 * @date 2024/9/26 15:16
 */
@SpringBootTest
@ActiveProfiles("postgresql")
public class StdSchedulerTest {

    @Autowired
    private HikariDataSource dataSource;

    @Test
    public void test01(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob job = new QrtzJob();
        job.setApplication("MEE_TEST");
        job.setState("INIT");
        job.setJobClass("com.mee.quartz.job.Job01TestService");
        job.setJobData("{}}");
        job.setJobDescription("描述测试。。。");
        scheduler.addJob(job);
    }

    @Test
    public void test02(){
        Scheduler scheduler = new StdScheduler(dataSource);
        String[] dbInfo = scheduler.getDBInfo();
        System.out.println(dbInfo);
    }

}
