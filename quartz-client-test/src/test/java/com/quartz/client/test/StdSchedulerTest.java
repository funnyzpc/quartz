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
        job.setJobDescription("描述测试01。。。");
        Object[] ct = scheduler.addJob(job);
        System.out.println("写入job结果:"+JacksonUtil.toJsonString(ct));

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
        QrtzExecute result = scheduler.getExecuteByExecuteId("202410311615351001");
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
        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST1","Y",null,null,null);
        Object[] result = scheduler.addApp(qrtzApp);
        System.out.println("app写入结果:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test11(){
        Scheduler scheduler = new StdScheduler(dataSource);
        Object[] result = scheduler.deleteApp("APPLICATION-TEST3");
        System.out.println("删除app:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test12(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateAppState("APPLICATION-TEST2","Y");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test13(){
        Scheduler scheduler = new StdScheduler(dataSource);
//        QrtzNode node = new QrtzNode("APPLICATION-TEST","172.18.18.11", SeqGenUtil.genSeq(),"Y",System.currentTimeMillis());
        QrtzNode node = new QrtzNode("APPLICATION-TEST","172.18.18.12", SeqGenUtil.genSeq(),"Y",-1L);
        Object[] result = scheduler.addNode(node);
        System.out.println("添加node结果:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test14(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteNode("APPLICATION-TEST3","172.18.18.11");
        System.out.println("删除node结果:"+result);
    }
    @Test
    public void test15(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzNode node = new QrtzNode("APPLICATION-TEST", "172.18.18.12", null, "N", null);
        int result = scheduler.updateNodeState(node);
        System.out.println("修改node状态结果:"+result);
    }

    @Test
    public void test16(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST3","Y",null,null,null);
        QrtzNode node = new QrtzNode("APPLICATION-TEST3", "172.18.18.11", null, "N", null);
        Object[] result = scheduler.addAppAndNode(qrtzApp,node);
        System.out.println("新增app&node结果:"+JacksonUtil.toJsonString(result));
    }
    @Test
    public void test17(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob qrtzJob = new QrtzJob(null,"APPLICATION-TEST2","INIT","com.mee.quartz.Test02","[]",null,null);

        Object[] result = scheduler.addJob(qrtzJob);
        System.out.println("job写入结果=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test18(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzJob qrtzJob = new QrtzJob("202410141453521000L","APPLICATION-TEST2","PAUSE","com.mee.quartz.Test02","{}","测试数据任务01",null);

        Object[] result = scheduler.updateJob(qrtzJob);
        System.out.println("job更新结果=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test19(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteJob("202410301331261000L");
        System.out.println("job删除结果=>"+result);
    }

    @Test
    public void test20(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateJobState("202410141453521000L","COMPLETE");
        System.out.println("execute更新结果=>"+result);
    }

    @Test
    public void test21(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateExecuteState("202410141950091001L","EXECUTING");
        System.out.println("execute更新结果=>"+result);
    }

    @Test
    public void test22(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzExecute execute = QrtzExecute.build(null,"202410141453521000","SIMPLE","INIT",null,null,999,10000,0,-1L,-1L,null,null,-1L,-1L);
//        QrtzExecute execute = new QrtzExecute(null,202410141453521000L,"CRON","INIT","0 0/5 * * * ?",null,-1,10000,0,-1L,-1L,null,null,-1L,-1L);
        Object[] result = scheduler.addExecute(execute);
        System.out.println("execute添加结果=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test23(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.deleteExecute("-2");
        System.out.println("execute删除结果=>"+result);
    }

    @Test
    public void test24(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzNode node = new QrtzNode("APPLICATION-TEST", "172.18.18.12", "", "N", null);
        int result = scheduler.updateNode(node);
        System.out.println("修改node结果:"+result);
    }

    @Test
    public void test25(){
        Scheduler scheduler = new StdScheduler(dataSource);
        QrtzExecute qrtzExecute = scheduler.getExecuteByExecuteId("5");
        qrtzExecute.setStartTime(System.currentTimeMillis()/1000*1000);
        qrtzExecute.setTimeTriggered(0);
        qrtzExecute.setState("PAUSED");
        Object[] result = scheduler.updateExecute(qrtzExecute);
        System.out.println("修改node结果:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test26(){
        Scheduler scheduler = new StdScheduler(dataSource);
        int result = scheduler.updateExecuteState("202410311656221001","EXECUTING");
        System.out.println("修改execute结果:"+result);
    }

    @Test
    public void test27(){
        Scheduler scheduler = new StdScheduler(dataSource);
        Object[] result = scheduler.updateJobStateInAll("202410311540071000","PAUSED");
        System.out.println("修改job状态(in all)结果:"+JacksonUtil.toJsonString(result));
    }


}
