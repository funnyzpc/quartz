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
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for database based lock handlers for providing thread/resource locking 
 * in order to protect resources from being altered by multiple threads at the 
 * same time.
 *  基于数据库的锁处理程序的基类，用于提供线程/资源锁定，以保护资源不被多个线程同时更改。
 *
 */
public abstract class DBSemaphore implements Semaphore, Constants,StdJDBCConstants/*,TablePrefixAware*/ {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     *  按线程存储 STATE_ACCESS/TRIGGER_ACCESS
     *  因为ThreadLocal里面可能同时存 STATE_ACCESS/TRIGGER_ACCESS,故需要用HashSet包一层
     *  todo : 是否可以考虑用String数组包一层呢？ （ThreadLocal<String[]>）
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<HashSet<String>>();

//    private String sql;
//    private String insertSql;

    private final String tablePrefix;
    
    private final String schedName;
    // 扩展SQL
    private String expandedSQL;
    // 扩展插入SQL
    private String expandedInsertSQL;
    // = '${schedName}'
    private String schedNameLiteral = null;


    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public DBSemaphore(String tablePrefix, String schedName, String defaultSQL, String defaultInsertSQL) {
        this.tablePrefix = tablePrefix; // QRTZ_
        this.schedName = schedName; // spring.quartz.properties.org.quartz.scheduler.instanceName
//        setSQL(defaultSQL); // StdRowLockSemaphore.SELECT_FOR_LOCK
//        setInsertSQL(defaultInsertSQL); //  // StdRowLockSemaphore.INSERT_LOCK
        setAllSQL(defaultSQL,defaultInsertSQL);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected Logger getLog() {
        return log;
    }

    private HashSet<String> getThreadLocks() {
        HashSet<String> threadLocks = lockOwners.get();
        if (threadLocks == null) {
            threadLocks = new HashSet<String>();
            lockOwners.set(threadLocks);
        }
        return threadLocks;
    }

    /**
     * Execute the SQL that will lock the proper database row.
     * 执行将锁定正确数据库行的SQL。（使用数据库行锁锁定指定行数据）
     */
    protected abstract void executeSQL(Connection conn, String lockName, String theExpandedSQL, String theExpandedInsertSQL) throws LockException;
    
    /**
     * Grants a lock on the identified resource to the calling thread (blocking
     * until it is available).
     * 将已标识资源的锁授予调用线程（阻塞直到资源可用）。
     * 
     * @return true if the lock was obtained.
     */
    @Override
    public boolean obtainLock(Connection conn, String lockName) throws LockException {
//        if(log.isDebugEnabled()) {
//            log.debug("Lock '" + lockName + "' is desired by: " + Thread.currentThread().getName());
//        }
        // lockName没有被当前线程本地变量持有则执行if内的
        if (!isLockOwner(lockName)) {
            // SELECT or UPDATE QRTZ_LOCKS
            executeSQL(conn, lockName, expandedSQL, expandedInsertSQL);
//            if(log.isDebugEnabled()) {
//                log.debug("Lock '" + lockName + "' given to: " + Thread.currentThread().getName());
//            }
            getThreadLocks().add(lockName);
            //getThreadLocksObtainer().put(lockName, new
            // Exception("Obtainer..."));
        } else if(log.isDebugEnabled()) {
            log.debug("Lock '" + lockName + "' Is already owned by: " + Thread.currentThread().getName());
        }
        return true;
    }

       
    /**
     * Release the lock on the identified resource if it is held by the calling thread.
     * 如果已标识的资源由调用线程持有，请释放该资源上的锁。
     */
    @Override
    public void releaseLock(String lockName) {
        // 因为加锁成功都会创建线程本地变量，这里能否考虑直接调用 getThreadLocks().remove(lockName);
        if (isLockOwner(lockName)) {
//            if(getLog().isDebugEnabled()) {
//                getLog().debug("Lock '" + lockName + "' returned by: " + Thread.currentThread().getName());
//            }
            getThreadLocks().remove(lockName);
            //getThreadLocksObtainer().remove(lockName);
        } else if (getLog().isDebugEnabled()) {
            getLog().warn("Lock '" + lockName + "' attempt to return by: " + Thread.currentThread().getName() + " -- but not owner!", new Exception("stack-trace of wrongful returner"));
        }
    }
    // new
    public void releaseLock2(String lockName) {
        // 因为加锁成功都会创建线程本地变量，这里能否考虑直接调用 getThreadLocks().remove(lockName);
         if ( !getThreadLocks().remove(lockName) && getLog().isDebugEnabled()) {
            getLog().warn("Lock '" + lockName + "' attempt to return by: " + Thread.currentThread().getName() + " -- but not owner!", new Exception("stack-trace of wrongful returner"));
        }
    }

    /**
     * Determine whether the calling thread owns a lock on the identified
     * resource.
     */
    public boolean isLockOwner(String lockName) {
        return getThreadLocks().contains(lockName);
    }

    /**
     * This Semaphore implementation does use the database.
     */
    @Override
    public boolean requiresConnection() {
        return true;
    }

//    protected String getSQL() {
//        return sql;
//    }

    protected void setAllSQL(String selectSQL,String insertSQL){
        if ( (selectSQL == null) || (selectSQL=selectSQL.trim()).length() == 0 ) {
            selectSQL=null;
        }
        if ((insertSQL == null) || (insertSQL=insertSQL.trim()).length() == 0 ) {
            insertSQL=null;
        }
        setExpandedSQL(selectSQL,insertSQL);
    }
    private void setExpandedSQL(final String selectSQL,final String insertSQL) {
        if (getTablePrefix() != null && getSchedName() != null && selectSQL != null && insertSQL != null) {
            // SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'QUARTZ-SPRINGBOOT' AND LOCK_NAME = ? FOR UPDATE
            this.expandedSQL = Util.rtp(selectSQL, getTablePrefix(), getSchedulerNameLiteral());
            // INSERT INTO QRTZ_LOCKS(SCHED_NAME, LOCK_NAME) VALUES ('QUARTZ-SPRINGBOOT', ?)
            this.expandedInsertSQL = Util.rtp(insertSQL, getTablePrefix(), getSchedulerNameLiteral());
        }
    }

//    protected void setSQL(String sql) {
//        if ((sql != null) && (sql.trim().length() != 0)) {
//            this.sql = sql.trim();
//        }
//        setExpandedSQL();
//    }
//
//    protected void setInsertSQL(String insertSql) {
//        if ((insertSql != null) && (insertSql.trim().length() != 0)) {
//            this.insertSql = insertSql.trim();
//        }
//        setExpandedSQL();
//    }


    
    protected String getSchedulerNameLiteral() {
        if(schedNameLiteral == null){
            schedNameLiteral = "'" + schedName + "'";
        }
        return schedNameLiteral;
    }

    public String getSchedName() {
        return schedName;
    }

//    @Override
//    public void setSchedName(String schedName) {
//        this.schedName = schedName;
//        setExpandedSQL();
//    }
//
    public String getTablePrefix() {
        return tablePrefix;
    }
//    @Override
//    public void setTablePrefix(String tablePrefix) {
//        this.tablePrefix = tablePrefix;
//        setExpandedSQL();
//    }
}
