package org.quartz;

import org.quartz.impl.JobCfgImpl;

/**
 * JobCfg
 *
 * @author shaoow
 * @version 1.0
 * @className JobCfg
 * @date 2024/7/12 13:27
 */
public interface JobCfg {
    String getSchedName();
    JobCfg setSchedName(String schedName);
    String getTriggerType();

    boolean checkCfg(String type);
//    boolean checkCronCfg();
//    boolean checkSimpleCfg();


}
