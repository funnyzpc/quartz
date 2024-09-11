package com.mee.quartz.test;

import org.junit.jupiter.api.Test;
import org.quartz.impl.triggers.SimpleTriggerImpl;

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
}
