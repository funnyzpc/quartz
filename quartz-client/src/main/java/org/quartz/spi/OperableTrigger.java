package org.quartz.spi;


import java.util.Date;

public interface OperableTrigger extends MutableTrigger {

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     * 
     * <p>
     * Usable by <code>{@link JobStore}</code>
     * implementations, in order to facilitate 'recognizing' instances of fired
     * <code>Trigger</code> s as their jobs complete execution.
     * </p>
     * 
     *  
     */
    void setFireInstanceId(String id);
    
    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     */
    String getFireInstanceId();

    void setNextFireTime(Date nextFireTime);
    
    void setPreviousFireTime(Date previousFireTime);

}