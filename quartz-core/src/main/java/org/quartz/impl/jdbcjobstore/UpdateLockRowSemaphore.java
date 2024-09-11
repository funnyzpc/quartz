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
 */

package org.quartz.impl.jdbcjobstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Provide thread/resource locking in order to protect
 * resources from being altered by multiple threads at the same time using
 * a db row update.
 *  提供线程/资源锁定，以保护资源不被多个线程使用数据库行更新同时更改。
 *
 * <p>
 * <b>Note:</b> This Semaphore implementation is useful for databases that do
 * not support row locking via "SELECT FOR UPDATE" type syntax, for example
 * Microsoft SQLServer (MSSQL).
 *  注意：此Semaphore实现适用于不支持通过“SELECT for UPDATE”类型语法进行行锁定的数据库，例如Microsoft SQLServer（MSSQL）。
 * </p> 
 */
public class UpdateLockRowSemaphore extends DBSemaphore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constants.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    // UPDATE QRTZ_LOCKS SET LOCK_NAME = LOCK_NAME WHERE SCHED_NAME = 'MEE_QUARTZ' AND LOCK_NAME = 'STATE_ACCESS/TRIGGER_ACCESS'
    public static final String UPDATE_FOR_LOCK = 
        "UPDATE " + TABLE_PREFIX_SUBST + TABLE_LOCKS + 
        " SET " + COL_LOCK_NAME + " = " + COL_LOCK_NAME +
        " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_LOCK_NAME + " = ? ";
    // INSERT INTO QRTZ_LOCKS(SCHED_NAME, LOCK_NAME) VALUES ('MEE_QUARTZ', 'STATE_ACCESS/TRIGGER_ACCESS')
    public static final String INSERT_LOCK = "INSERT INTO "
        + TABLE_PREFIX_SUBST + TABLE_LOCKS + "(" + COL_SCHEDULER_NAME + ", " + COL_LOCK_NAME + ") VALUES (" 
        + SCHED_NAME_SUBST + ", ?)"; 

    // 尝试重新获取次数
    private static final int RETRY_COUNT = 2;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constructors.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public UpdateLockRowSemaphore() {
        super(DEFAULT_TABLE_PREFIX, null, UPDATE_FOR_LOCK, INSERT_LOCK);
    }
    public UpdateLockRowSemaphore(String tablePrefix,String schedName, String defaultSelectSQL ) {
        super(tablePrefix, schedName, null==defaultSelectSQL?UPDATE_FOR_LOCK:defaultSelectSQL, INSERT_LOCK);
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
     * 执行将锁定正确数据库的SQL执行将锁定适当数据库行的SQL选择更新。(说人话就是 在update之前执行一个select for update语句以锁定行)
     *
     * expandedSQL: SELECT * FROM QRTZ_LOCKS
     * expandedInsertSQL: INSERT INTO QRTZ_LOCKS
     */
    @Override
    protected void executeSQL(Connection conn,final String lockName,final String expandedSQL,final String expandedInsertSQL) throws LockException {
        SQLException lastFailure = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                // 这两句十分关键，语句内外都用try~catch处理, 且一次执行仅可执行更新或者插入其一
                if (!lockViaUpdate(conn, lockName, expandedSQL)) {
                    lockViaInsert(conn, lockName, expandedInsertSQL);
                }
                // 只有到这里才是正常完成，其他情况就是:重试或者抛出错误
                return;
            } catch (SQLException e) {
                e.printStackTrace();
                lastFailure = e;
//                if ((i + 1) == RETRY_COUNT) {
//                    getLog().debug("Lock '{}' was not obtained by: {}", lockName, Thread.currentThread().getName());
//                } else {
//                    getLog().debug("Lock '{}' was not obtained by: {} - will try again.", lockName, Thread.currentThread().getName());
//                }
                try {
                    // 一次请求最多可以获取两次，失败后要休眠1秒
                    Thread.sleep(1000L);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new LockException("Failure obtaining db row lock: " + lastFailure.getMessage(), lastFailure);
    }
    
//    protected String getUpdateLockRowSQL() {
//        return getSQL();
//    }
//
//    public void setUpdateLockRowSQL(String updateLockRowSQL) {
//        setSQL(updateLockRowSQL);
//    }

    private boolean lockViaUpdate(Connection conn, String lockName, String sql) throws SQLException {
        //  expandedSQL: SELECT * FROM QRTZ_LOCKS
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, lockName);
            getLog().debug("Lock '" + lockName + "' is being obtained: " + Thread.currentThread().getName());
            return ps.executeUpdate() >= 1;
        } finally {
            ps.close();
        }
    }

    private void lockViaInsert(Connection conn, String lockName, String sql) throws SQLException {
        // expandedInsertSQL: INSERT INTO QRTZ_LOCKS
        getLog().debug("Inserting new lock row for lock: '" + lockName + "' being obtained by thread: " + Thread.currentThread().getName());
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, lockName); // STATE_ACCESS or TRIGGER_ACCESS
            if(ps.executeUpdate() != 1) {
                throw new SQLException(Util.rtp(
                    "No row exists, and one could not be inserted in table " + TABLE_PREFIX_SUBST + TABLE_LOCKS + 
                    " for lock named: " + lockName, getTablePrefix(), getSchedulerNameLiteral()));
            }
        } finally {
            ps.close();
        }
    }

}
