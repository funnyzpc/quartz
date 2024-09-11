package com.mee.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.QrtzExecute;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

//@DisallowConcurrentExecution
public class Simple01TestService implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(Simple01TestService.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        QrtzExecute eJob = context.getJobDetail().getEJob();
        LOGGER.info("=>>{}.{}-{}#{}",eJob.getId(),eJob.getJobType(),eJob.getJob().getJobClass(),eJob.getExecuteIdx());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
