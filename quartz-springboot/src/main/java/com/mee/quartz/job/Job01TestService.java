package com.mee.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.QrtzExecute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Job01TestService  implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(Job01TestService.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        QrtzExecute eJob = context.getJobDetail().getEJob();
        LOGGER.info("=>>{}-{}.{}-{}",eJob.getJob().getId(),eJob.getId(),eJob.getJobType(),eJob.getJob().getJobClass());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        System.out.println("=====> [Job01TestService:execute] Already Executed! <=====");
    }
}
