package com.mee.quartz.opt;

/**
 * 任务配置Service接口
 * 
 * @author  shadow
 * @version v1.0
 * @date    2023-03-17 09:22:43
 */
public interface QrtzTaskService {

    void deleteJob( String job_name );

    void deleteJobPure( String job_name );

    void pauseJob(String job_name);

    void resumeJob( String job_name );
    void jobStatus(String job_name);


}
