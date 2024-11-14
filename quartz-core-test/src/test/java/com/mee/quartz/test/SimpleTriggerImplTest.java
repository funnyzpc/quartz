package com.mee.quartz.test;

import org.junit.jupiter.api.Test;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SimpleTriggerImplTest
 *
 * @author shaoow
 * @version 1.0
 * @className SimpleTriggerImplTest
 * @date 2024/9/11 9:34
 */
public class SimpleTriggerImplTest {

     SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private long TIME_CHECK_INTERVAL = 15000L;

    @Test
    public void test01(){
        Long  endTime = -1L;
        long now = System.currentTimeMillis();
        SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl()
                .setStartTime(new Date(1725881756768L)) // 2024-09-09 17...
                .setStartTime(new Date(1726019781812L)) // 2024-09-11 09...
                .setEndTime(null==endTime?null:new Date(endTime))
                .setRepeatCount(10000)
                .setRepeatInterval(2000)
                .setTimesTriggered(0);
        Date nextFireTime = simpleTrigger.getFireTimeAfter(new Date(now+TIME_CHECK_INTERVAL));
//        Date nextFireTime = simpleTrigger.getFireTimeAfter(new Date(now));
        System.out.println(nextFireTime);
    }

    @Test
    public void test02(){
        Long  endTime = -1L;
        long now = System.currentTimeMillis()/1000*1000+10000+10000;
        SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl()
                .setStartTime(new Date(now)) // 2024-09-09 17...
                .setEndTime(new Date(endTime))
                .setRepeatCount(10)
                .setRepeatInterval(10000)
                .setTimesTriggered(0);
//        Date nextFireTime = simpleTrigger.getFireTimeAfter(new Date(now+TIME_CHECK_INTERVAL));
        Date nextFireTime =new Date(now);
//        Date nextFireTime = simpleTrigger.getFireTimeAfter(new Date(now));
//        System.out.println(null!=nextFireTime?fmt.format(nextFireTime):null);
        for(int i=0;i<20;i++ ){
            if(nextFireTime!=null){
                simpleTrigger.setTimesTriggered(i+1);
                nextFireTime = simpleTrigger.getFireTimeAfter(nextFireTime);
                System.out.println(simpleTrigger.getTimesTriggered() +"->"+(null!=nextFireTime?fmt.format(nextFireTime):null));
                continue;
            }
            break;
        }
    }

}
