package com.quartz.client.test;

import com.quartz.client.util.JacksonUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.quartz.impl.StdScheduler;
import org.quartz.simpl.SeqGenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;


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

    private static final Logger LOGGER = LoggerFactory.getLogger(StdSchedulerTest.class);
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
        System.out.println(JacksonUtil.toJsonString(dbInfo));
    }

    @Test
    public void test03(){
        Scheduler scheduler = new StdScheduler(dataSource);
        List<QrtzApp> qrtzApps = scheduler.getAllApp();
        System.out.println(JacksonUtil.toJsonString(qrtzApps));
    }
    ////////////////////////////////////////////
    @Test
    public void test04(){
        Scheduler scheduler = new StdScheduler(dataSource);
        List<QrtzNode> qrtzApps = scheduler.getNodeByApp("mee_generator");
        System.out.println(JacksonUtil.toJsonString(qrtzApps));
//        LOGGER.info(JacksonUtil.toJsonString(qrtzApps));
    }

    @Test
    public void test05(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob result = scheduler.getJobByJobId("202409271449221000");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test06(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzExecute result = scheduler.getExecuteByExecuteId("2409200935241002");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test07(){
        Scheduler scheduler = new StdScheduler(dataSource);
        List<QrtzExecute> result = scheduler.getExecuteByJobId("2409200930181000");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test08(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob result = scheduler.getJobInAllByJobId("2409200930181000");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test09(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzExecute result = scheduler.getExecuteInAllByExecuteId("2409191732251003");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test10(){
        Scheduler scheduler = new StdScheduler(dataSource);
//        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST","Y",-1L,System.currentTimeMillis(),1L);
        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST2","Y",null,null,null);

        int result = scheduler.addApp(qrtzApp);
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test11(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteApp("APPLICATION-TEST");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test12(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateAppState("APPLICATION-TEST2","N");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test13(){
        Scheduler scheduler = new StdScheduler(dataSource);
//        QrtzNode node = new QrtzNode("APPLICATION-TEST","172.18.18.11", SeqGenUtil.genSeq(),"Y",System.currentTimeMillis());
        QrtzNode node = new QrtzNode("APPLICATION-TEST","172.18.18.11", null,"Y",null);
        int result = scheduler.addNode(node);
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test14(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteNode("APPLICATION-TEST","172.18.18.11");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test15(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzNode node = new QrtzNode("APPLICATION-TEST", "172.18.18.11", null, "N", null);
        int result = scheduler.updateNodeState(node);
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test16(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST3","Y",null,null,null);
        QrtzNode node = new QrtzNode("APPLICATION-TEST3", "172.18.18.11", null, null, null);
        int result = scheduler.addAppAndNode(qrtzApp,node);
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test17(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob qrtzJob = new QrtzJob(null,"APPLICATION-TEST","INIT","com.mee.quartz.Test02","[]",null,null);

        int result = scheduler.addJob(qrtzJob);
        System.out.println(result+"=>"+JacksonUtil.toJsonString(qrtzJob));
    }

    @Test
    public void test18(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob qrtzJob = new QrtzJob(202410141453521000L,"APPLICATION-TEST","EXECUTING","com.mee.quartz.Test02","[]","测试数据任务01",null);

        int result = scheduler.updateJob(qrtzJob);
        System.out.println(result+"=>"+JacksonUtil.toJsonString(qrtzJob));
    }

    @Test
    public void test19(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteJob(202410141453521000L);
        System.out.println(result+"=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test20(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateExecuteStateByJobId(202410141453521000L,"COMPLETE");
        System.out.println(result+"=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test21(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateExecuteStateByExecuteId(2409200940191099L,"EXECUTING");
        System.out.println(result+"=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test22(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzExecute execute = new QrtzExecute(null,202410141453521000L,"SIMPLE","INIT",null,null,999,10000,0,-1L,-1L,null,null,-1L,-1L);
//        QrtzExecute execute = new QrtzExecute(null,202410141453521000L,"CRON","INIT","0 0/5 * * * ?",null,-1,10000,0,-1L,-1L,null,null,-1L,-1L);
        int result = scheduler.addExecute(execute);
        System.out.println(result+"=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test23(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteExecute(2409200940191099L);
        System.out.println(result+"=>"+JacksonUtil.toJsonString(result));
    }



}
