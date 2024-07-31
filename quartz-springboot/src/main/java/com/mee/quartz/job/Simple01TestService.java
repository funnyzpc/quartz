package com.mee.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

//@DisallowConcurrentExecution
public class Simple01TestService implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(Simple01TestService.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Key key = context.getTrigger().getKey();
        LOGGER.info("=>>"+key.getName()+"::"+key.getType());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
