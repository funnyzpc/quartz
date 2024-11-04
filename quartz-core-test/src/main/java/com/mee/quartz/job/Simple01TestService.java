package com.mee.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

//@DisallowConcurrentExecution
public class Simple01TestService implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(Simple01TestService.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("=>>{}-{}.{}-{}",context.getJobId(),context.getExecuteId(),context.getJobType(),context.getJobClassName());
        System.out.println( new JSONArray(context.getJobDataList()));
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
