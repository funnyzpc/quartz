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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Internal database based lock handler for providing thread/resource locking 
 * in order to protect resources from being altered by multiple threads at the 
 * same time.
 *  基于数据库的内部锁处理程序，用于提供线程/资源锁定，以保护资源不被多个线程同时更改。
 *
 * @author jhouse
 */
public class StdRowLockSemaphore extends DBSemaphore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    // SELECT * FROM QRTZ_LOCKS WHERE SCHED_NAME = 'MEE_QUARTZ' AND LOCK_NAME = 'STATE_ACCESS' FOR UPDATE
    public static final String SELECT_FOR_LOCK = "SELECT * FROM "
            + TABLE_PREFIX_SUBST + TABLE_LOCKS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_LOCK_NAME + " = ? FOR UPDATE";

    // INSERT INTO QRTZ_LOCKS(SCHED_NAME, LOCK_NAME) VALUES ('MEE_QUARTZ','STATE_ACCESS')
    public static final String INSERT_LOCK = "INSERT INTO "
        + TABLE_PREFIX_SUBST + TABLE_LOCKS + "(" + COL_SCHEDULER_NAME + ", " + COL_LOCK_NAME + ") VALUES (" 
        + SCHED_NAME_SUBST + ", ?)"; 

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

//    public StdRowLockSemaphore() {
//        super(DEFAULT_TABLE_PREFIX, null, SELECT_FOR_LOCK, INSERT_LOCK);
//    }

    public StdRowLockSemaphore(String tablePrefix, String schedName, String selectWithLockSQL) {
        super(tablePrefix, schedName, selectWithLockSQL != null ? selectWithLockSQL : SELECT_FOR_LOCK, INSERT_LOCK);
    }

    // Data Members

    // Configurable lock retry parameters
    private int maxRetry = 3;
    private long retryPeriod = 1000L;

    // 这个getter setter其实是给子类用的用于 extends
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
    public void setRetryPeriod(long retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public long getRetryPeriod() {
        return retryPeriod;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Execute the SQL select for update that will lock the proper database row.
     *  执行SQL select for update，这将锁定正确的数据库行。
     *  todo : 待优化
     *
     *   * expandedSQL: SELECT * FROM QRTZ_LOCKS
     *   * expandedInsertSQL: INSERT INTO QRTZ_LOCKS
     */
    @Override
    protected void executeSQL(Connection conn, final String lockName, final String expandedSQL, final String expandedInsertSQL) throws LockException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        SQLException initCause = null;
        // attempt lock two times (to work-around possible race conditions in inserting the lock row the first time running)
        // 尝试锁定两次（以解决第一次运行时插入锁定行时可能出现的比赛条件）
        int count = 0;
        // Configurable lock retry attempts 可配置的锁定重试尝试
        int maxRetryLocal = this.maxRetry; // 3
        long retryPeriodLocal = this.retryPeriod; // 1000
        do {
            count++;
            try {
                //  expandedSQL: SELECT * FROM QRTZ_LOCKS
                ps = conn.prepareStatement(expandedSQL);
                ps.setString(1, lockName);
//                if (getLog().isDebugEnabled()) {
//                    getLog().debug("Lock '" + lockName + "' is being obtained: " + Thread.currentThread().getName());
//                }
                rs = ps.executeQuery();
                // 也就是里面没有数据
                if (!rs.next()) {
                    getLog().debug("Inserting new lock row for lock: '" + lockName + "' being obtained by thread: " + Thread.currentThread().getName());
                    rs.close();
                    rs = null;
                    ps.close();
                    ps = null;
                    // expandedInsertSQL: INSERT INTO QRTZ_LOCKS
                    ps = conn.prepareStatement(expandedInsertSQL);
                    ps.setString(1, lockName);
                    int res = ps.executeUpdate();
                    if(res != 1) {
                        if(count < maxRetryLocal) {
                            // pause a bit to give another thread some time to commit the insert of the new lock row
                            // 暂停一点，给另一个线程一些时间来提交新锁行的插入
                            try {
                                Thread.sleep(retryPeriodLocal);
                            } catch (InterruptedException ignore) {
                                // 这里只是传递中断状态
                                Thread.currentThread().interrupt();
                            }
                            // try again ...
                            continue;
                        }
                        throw new SQLException(Util.rtp(
                            "No row exists, and one could not be inserted in table " + TABLE_PREFIX_SUBST + TABLE_LOCKS + 
                            " for lock named: " + lockName, getTablePrefix(), getSchedulerNameLiteral()));
                    }
                }
                
                return; // obtained lock, go 获得锁定，go (注意:只有这个地方是正常结束，其他情况都会抛出)
            } catch (SQLException sqle) {
                // todo : 是否可以考虑整体try~catch后统一在do~while之后处理呢？
                //Exception src =
                // (Exception)getThreadLocksObtainer().get(lockName);
                //if(src != null)
                //  src.printStackTrace();
                //else
                //  System.err.println("--- ***************** NO OBTAINER!");
    
                if(initCause == null)
                    initCause = sqle;
//                if (getLog().isDebugEnabled()) {
//                    getLog().debug(
//                        "Lock '" + lockName + "' was not obtained by: " +
//                        Thread.currentThread().getName() + (count < maxRetryLocal ? " - will try again." : ""));
//                }
                if(count < maxRetryLocal) {
                    // pause a bit to give another thread some time to commit the insert of the new lock row
                    try {
                        Thread.sleep(retryPeriodLocal);
                    } catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }
                    // try again ...
                    continue;
                }
                throw new LockException("Failure obtaining db row lock: " + sqle.getMessage(), sqle);
            } finally {
                if (rs != null) { 
                    try {
                        rs.close();
                    } catch (Exception ignore) {
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        } while(count < (maxRetryLocal + 1));
        throw new LockException("Failure obtaining db row lock, reached maximum number of attempts. Initial exception (if any) attached as root cause.", initCause);
    }
    // new
    protected void executeSQL2(Connection conn, final String lockName, final String expandedSQL, final String expandedInsertSQL) throws LockException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Exception initCause = null;
        // attempt lock two times (to work-around possible race conditions in inserting the lock row the first time running)
        int count = 0;
        // Configurable lock retry attempts
        int maxRetryLocal = this.maxRetry; // 3
        long retryPeriodLocal = this.retryPeriod; // 1000
        do {
            count++;
            try {
                //  expandedSQL: SELECT * FROM QRTZ_LOCKS
                ps = conn.prepareStatement(expandedSQL);
                ps.setString(1, lockName);
                rs = ps.executeQuery();
                // 也就是里面没有数据
                if (!rs.next()) {
                    getLog().debug("Inserting new lock row for lock: '" + lockName + "' being obtained by thread: " + Thread.currentThread().getName());
                    rs.close();
                    rs = null; // 这里置为null只是为了让 finally 块中的关闭不抛错而已
                    ps.close();
                    ps = null;
                    // expandedInsertSQL: INSERT INTO QRTZ_LOCKS
                    ps = conn.prepareStatement(expandedInsertSQL);
                    ps.setString(1, lockName);
                    int res = ps.executeUpdate();
                    if(res != 1) {
                        // 这里的抛出会进入到catch中，catch中会尝试等待
                        throw new SQLException(Util.rtp(
                                "No row exists, and one could not be inserted in table " + TABLE_PREFIX_SUBST + TABLE_LOCKS +
                                        " for lock named: " + lockName, getTablePrefix(), getSchedulerNameLiteral()));
                    }
                }
                return; // obtained lock, go 获得锁定，go
            } catch (Exception sqle) {
                if(initCause == null){
                    initCause = sqle;
                }
                if(count < maxRetryLocal) {
                    try {
                        Thread.sleep(retryPeriodLocal);
                    } catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }
                }
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Exception ignore) {
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        } while(count < (maxRetryLocal + 1));
        throw new LockException("Failure obtaining db row lock, reached maximum number of attempts. Initial exception (if any) attached as root cause.", initCause);
    }

//    protected String getSelectWithLockSQL() {
//        return getSQL();
//    }

//    public void setSelectWithLockSQL(String selectWithLockSQL) {
//        setSQL(selectWithLockSQL);
//    }

}
