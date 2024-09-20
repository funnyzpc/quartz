/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;

import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;

/**
 * <p>
 * <code>JobStoreTX</code> is meant to be used in a standalone environment.
 * Both commit and rollback will be handled by this class.
 * </p>
 * 
 * <p>
 * If you need a <code>{@link org.quartz.spi.JobStore}</code> class to use
 * within an application-server environment, use <code>{@link
 * org.quartz.impl.jdbcjobstore.JobStoreCMT}</code>
 * instead.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 */
public class JobStoreTX extends JobStoreSupport {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    @Override
    public void initialize(ClassLoadHelper classLoadHelper,SchedulerSignaler schedSignaler) throws SchedulerConfigException {
        super.initialize(classLoadHelper, schedSignaler);
        getLog().info("JobStoreTX initialized.");
    }

//    // todo ...
//    @Override
//    public boolean removeTrigger(Key key) throws JobPersistenceException {
//        return false;
//    }

    /**
     * For <code>JobStoreTX</code>, the non-managed TX connection is just 
     * the normal connection because it is not CMT.
     * 
     * @see JobStoreSupport#getConnection()
     */
    @Override
    protected Connection getNonManagedTXConnection() throws JobPersistenceException {
        return getConnection();
    }

}
// EOF
