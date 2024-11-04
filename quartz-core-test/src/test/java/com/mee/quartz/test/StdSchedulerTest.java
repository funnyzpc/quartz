package com.mee.quartz.test;

import com.mee.quartz.util.JacksonUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
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
//    @Autowired
//    private HikariDataSource dataSource;
    @Autowired
    private Scheduler scheduler;

    @Test
    public void test01()throws Exception{
        String schedulerName = scheduler.getSchedulerName();
        QrtzJob job = new QrtzJob();
        job.setApplication(schedulerName);
        job.setState("INIT");
        job.setJobClass("com.mee.quartz.job.Job01TestService");
        job.setJobData("{}}");
        job.setJobDescription("描述测试01。。。");
        Object[] ct = scheduler.addJob(job);
        System.out.println("写入job结果:"+JacksonUtil.toJsonString(ct));

    }

    @Test
    public void test02(){
        String[] dbInfo = scheduler.getDBInfo();
        System.out.println(JacksonUtil.toJsonString(dbInfo));
    }

    @Test
    public void test03(){
        List<QrtzApp> qrtzApps = scheduler.getAllApp();
        System.out.println(JacksonUtil.toJsonString(qrtzApps));
    }

    ////////////////////////////////////////////
    @Test
    public void test04(){
        List<QrtzNode> qrtzApps = scheduler.getNodeByApp("mee_generator");
        System.out.println(JacksonUtil.toJsonString(qrtzApps));
    }

    @Test
    public void test05(){
        QrtzJob result = scheduler.getJobByJobId("202409271449221000");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test06(){
        QrtzExecute result = scheduler.getExecuteByExecuteId("202410311615351001");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test07(){
        List<QrtzExecute> result = scheduler.getExecuteByJobId("2409200930181000");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test08(){
        QrtzJob result = scheduler.getJobInAllByJobId("2409200930181000");
        System.out.println(JacksonUtil.toJsonString(result));
    }
    @Test
    public void test09(){
        QrtzExecute result = scheduler.getExecuteInAllByExecuteId("2409191732251003");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test10(){
//        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST","Y",-1L,System.currentTimeMillis(),1L);
        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST1","Y",null,null,null);
        Object[] result = scheduler.addApp(qrtzApp);
        System.out.println("app写入结果:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test11(){
        Object[] result = scheduler.deleteApp("APPLICATION-TEST3");
        System.out.println("删除app:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test12(){
        int result = scheduler.updateAppState("APPLICATION-TEST2","Y");
        System.out.println(JacksonUtil.toJsonString(result));
    }

    @Test
    public void test13(){
//        QrtzNode node = new QrtzNode("APPLICATION-TEST","172.18.18.11", SeqGenUtil.genSeq(),"Y",System.currentTimeMillis());
        QrtzNode node = new QrtzNode("APPLICATION-TEST","172.18.18.12", SeqGenUtil.genSeq(),"Y",-1L);
        Object[] result = scheduler.addNode(node);
        System.out.println("添加node结果:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test14(){
        int result = scheduler.deleteNode("APPLICATION-TEST3","172.18.18.11");
        System.out.println("删除node结果:"+result);
    }
    @Test
    public void test15(){
        QrtzNode node = new QrtzNode("APPLICATION-TEST", "172.18.18.12", null, "N", null);
        int result = scheduler.updateNodeState(node);
        System.out.println("修改node状态结果:"+result);
    }

    @Test
    public void test16(){
        QrtzApp qrtzApp = new QrtzApp("APPLICATION-TEST3","Y",null,null,null);
        QrtzNode node = new QrtzNode("APPLICATION-TEST3", "172.18.18.11", null, "N", null);
        Object[] result = scheduler.addAppAndNode(qrtzApp,node);
        System.out.println("新增app&node结果:"+JacksonUtil.toJsonString(result));
    }
    @Test
    public void test17(){
        QrtzJob qrtzJob = new QrtzJob(null,"APPLICATION-TEST2","INIT","com.mee.quartz.Test02","[]",null,null);

        Object[] result = scheduler.addJob(qrtzJob);
        System.out.println("job写入结果=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test18(){
        QrtzJob qrtzJob = new QrtzJob("202410141453521000L","APPLICATION-TEST2","PAUSE","com.mee.quartz.Test02","{}","测试数据任务01",null);

        Object[] result = scheduler.updateJob(qrtzJob);
        System.out.println("job更新结果=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test19(){
        int result = scheduler.deleteJob("202410301331261000L");
        System.out.println("job删除结果=>"+result);
    }

    @Test
    public void test20(){
        int result = scheduler.updateJobState("202410141453521000L","COMPLETE");
        System.out.println("execute更新结果=>"+result);
    }

    @Test
    public void test21(){
        int result = scheduler.updateExecuteState("202410141950091001L","EXECUTING");
        System.out.println("execute更新结果=>"+result);
    }

    @Test
    public void test22(){
        QrtzExecute execute = QrtzExecute.build(null,"202410141453521000","SIMPLE","INIT",null,null,999,10000,0,-1L,-1L,null,null,-1L,-1L);
//        QrtzExecute execute = new QrtzExecute(null,202410141453521000L,"CRON","INIT","0 0/5 * * * ?",null,-1,10000,0,-1L,-1L,null,null,-1L,-1L);
        Object[] result = scheduler.addExecute(execute);
        System.out.println("execute添加结果=>"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test23(){
        int result = scheduler.deleteExecute("-2");
        System.out.println("execute删除结果=>"+result);
    }

    @Test
    public void test24(){
        QrtzNode node = new QrtzNode("APPLICATION-TEST", "172.18.18.12", "", "N", null);
        int result = scheduler.updateNode(node);
        System.out.println("修改node结果:"+result);
    }

    @Test
    public void test25(){
        QrtzExecute qrtzExecute = scheduler.getExecuteByExecuteId("5");
        qrtzExecute.setStartTime(System.currentTimeMillis()/1000*1000);
        qrtzExecute.setTimeTriggered(0);
        qrtzExecute.setState("PAUSED");
        Object[] result = scheduler.updateExecute(qrtzExecute);
        System.out.println("修改node结果:"+JacksonUtil.toJsonString(result));
    }

    @Test
    public void test26(){
        int result = scheduler.updateExecuteState("202410311656221001","EXECUTING");
        System.out.println("修改execute结果:"+result);
    }

    @Test
    public void test27(){
        Object[] result = scheduler.updateJobStateInAll("202410311540071000","PAUSED");
        System.out.println("修改job状态(in all)结果:"+JacksonUtil.toJsonString(result));
    }


}
