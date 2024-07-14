package com.mee.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Job02TestService implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(Job02TestService.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("=>>"+context.getTrigger().getKey().getName());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        System.out.println("=====> [Job02TestService:execute] 已经执行! <=====");
    }
}
