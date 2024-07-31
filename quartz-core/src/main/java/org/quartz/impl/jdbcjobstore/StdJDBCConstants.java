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

import org.quartz.Trigger;

/**
 * <p>
 * This interface extends <code>{@link
 * org.quartz.impl.jdbcjobstore.Constants}</code>
 * to include the query string constants in use by the <code>{@link
 * org.quartz.impl.jdbcjobstore.StdJDBCDelegate}</code>
 * class.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 */
public interface StdJDBCConstants extends Constants {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    // table prefix substitution string (表前缀，也就是: QRTZ_)
    String TABLE_PREFIX_SUBST = "{0}";

    // table prefix substitution string (也就是应用的名字，分布式部署下以此区分，如果是springboot项目则一般定义spring.quartz.properties.org.quartz.scheduler.instanceName为${spring.application.name}的值)
    String SCHED_NAME_SUBST = "{1}";

    // QUERIES
    // TRIGGER_STATE IS '当前触发器状态（ WAITING：等待； PAUSED：暂停； ACQUIRED：正常执行； BLOCKED：阻塞； ERROR：错误； PAUSED_BLOCKED:暂停阻塞状态 ）'
    // -- 正常执行或阻塞状态 更新为 等待状态
    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = 'WAITING' WHERE SCHED_NAME = 'MEE_QUARTZ' AND (TRIGGER_STATE = 'ACQUIRED' OR TRIGGER_STATE = 'BLOCKED')
    // -- 阻塞或暂停阻塞状态 更新为 暂停状态
    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = 'PAUSED' WHERE SCHED_NAME = 'MEE_QUARTZ' AND (TRIGGER_STATE = 'BLOCKED' OR TRIGGER_STATE = 'PAUSED_BLOCKED')
    String UPDATE_TRIGGER_STATES_FROM_OTHER_STATES = "UPDATE "
            + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS
            + " SET "
            + COL_TRIGGER_STATE
            + " = ?"
            + " WHERE "
            + COL_SCHEDULER_NAME 
            + " = " + SCHED_NAME_SUBST + " AND ("
            + COL_TRIGGER_STATE
            + " = ? OR "
            + COL_TRIGGER_STATE + " = ?)";

    // 获取所有不匹配的触发器的名称。
    // NEXT_FIRE_TIME IS '下一次触发时间（毫秒），默认为-1，意味不会自动触发'
    // SELECT * FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
    @Deprecated
    String SELECT_MISFIRED_TRIGGERS = "SELECT * FROM "
        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND NOT ("
        + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND " 
        + COL_NEXT_FIRE_TIME + " < ? "
        + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";
    // 查询给定状态下的所有触发器
    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_STATE = 'COMPLETE'
    String SELECT_TRIGGERS_IN_STATE = "SELECT "
            + COL_TRIGGER_NAME + " FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND "
            + COL_TRIGGER_STATE + " = ?";
//    String SELECT_TRIGGERS_IN_STATE = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND "
//            + COL_TRIGGER_STATE + " = ?";

    // 根据给定的时间戳，获取给定状态下所有不匹配的触发器的名称。
    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS
    //  WHERE SCHED_NAME = 'MEE_QUARTZ' AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
    @Deprecated
    String SELECT_MISFIRED_TRIGGERS_IN_STATE = "SELECT "
        + COL_TRIGGER_NAME + " FROM "
        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
        + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND " 
        + COL_NEXT_FIRE_TIME + " < ? AND " + COL_TRIGGER_STATE + " = ? "
        + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";
//    String SELECT_MISFIRED_TRIGGERS_IN_STATE = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
//            + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND "
//            + COL_NEXT_FIRE_TIME + " < ? AND " + COL_TRIGGER_STATE + " = ? "
//            + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";

    String COUNT_MISFIRED_JOB_IN_STATE = "SELECT COUNT(TRIGGER_NAME) FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ?";
    // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ?
//    String COUNT_MISFIRED_TRIGGERS_IN_STATE = "SELECT COUNT("
//        + COL_TRIGGER_NAME + ") FROM "
//        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
//        + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND "
//        + COL_NEXT_FIRE_TIME + " < ? "
//        + "AND " + COL_TRIGGER_STATE + " = ?";

    String SELECT_HAS_MISFIRED_JOB_TRIGGERS_IN_STATE = "SELECT TRIGGER_NAME,SCHED_NAME,TRIGGER_TYPE FROM {0}JOB_CFG " +
            "WHERE SCHED_NAME = {1} AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC ";
    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
//    String SELECT_HAS_MISFIRED_TRIGGERS_IN_STATE = "SELECT "
//        + COL_TRIGGER_NAME +" , "+COL_SCHEDULER_NAME+ " FROM "
//        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
//        + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND "
//        + COL_NEXT_FIRE_TIME + " < ? "
//        + "AND " + COL_TRIGGER_STATE + " = ? "
//        + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";
//    String SELECT_HAS_MISFIRED_TRIGGERS_IN_STATE = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
//            + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND "
//            + COL_NEXT_FIRE_TIME + " < ? "
//            + "AND " + COL_TRIGGER_STATE + " = ? "
//            + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";

    // SELECT TRIGGER_NAME FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND NOT (MISFIRE_INSTR = -1) AND NEXT_FIRE_TIME < ? AND TRIGGER_GROUP = ? AND TRIGGER_STATE = ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
//    String SELECT_MISFIRED_TRIGGERS_IN_GROUP_IN_STATE = "SELECT "
//        + COL_TRIGGER_NAME
//        + " FROM "
//        + TABLE_PREFIX_SUBST
//        + TABLE_TRIGGERS
//        + " WHERE "
//        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
//        + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND "
//        + COL_NEXT_FIRE_TIME
//        + " < ? "
//        + "AND " + COL_TRIGGER_STATE + " = ? "
//        + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";
//    String SELECT_MISFIRED_TRIGGERS_IN_GROUP_IN_STATE = "SELECT "
//            + COL_TRIGGER_NAME
//            + " FROM "
//            + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND NOT ("
//            + COL_MISFIRE_INSTRUCTION + " = " + Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY + ") AND "
//            + COL_NEXT_FIRE_TIME
//            + " < ? AND "
//            + COL_TRIGGER_GROUP
//            + " = ? AND " + COL_TRIGGER_STATE + " = ? "
//            + "ORDER BY " + COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";

    // DELETE FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_FIRED_TRIGGERS = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS
            + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    // INSERT INTO QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)  VALUES('MEE_QUARTZ', ?, ?, ?, ?, ?, ?, ?, ?, ?)
    String INSERT_JOB_DETAIL = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " (" 
            + COL_SCHEDULER_NAME + ", " + COL_TRIGGER_NAME
            + ", " + COL_DESCRIPTION + ", "
            + COL_JOB_CLASS + ", "
            + COL_IS_NONCONCURRENT +  ", " + COL_IS_UPDATE_DATA + ", " 
            + COL_REQUESTS_RECOVERY + ", "
            + COL_JOB_DATAMAP + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?,?,?)";
//    String INSERT_JOB_DETAIL = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " ("
//            + COL_SCHEDULER_NAME + ", " + COL_JOB_NAME
//            + ", " + COL_JOB_GROUP + ", " + COL_DESCRIPTION + ", "
//            + COL_JOB_CLASS + ", " + COL_IS_DURABLE + ", "
//            + COL_IS_NONCONCURRENT +  ", " + COL_IS_UPDATE_DATA + ", "
//            + COL_REQUESTS_RECOVERY + ", "
//            + COL_JOB_DATAMAP + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // UPDATE QRTZ_JOB_DETAILS SET DESCRIPTION = ?, JOB_CLASS_NAME = ?, IS_DURABLE = ?, IS_NONCONCURRENT = ?, IS_UPDATE_DATA = ?, REQUESTS_RECOVERY = ?, JOB_DATA = ?  WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String UPDATE_JOB_DETAIL = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " SET "
            + COL_DESCRIPTION + " = ?, " + COL_JOB_CLASS + " = ?, "
            + COL_IS_NONCONCURRENT + " = ?, " + COL_IS_UPDATE_DATA + " = ?, "
            + COL_REQUESTS_RECOVERY + " = ?, "
            + COL_JOB_DATAMAP + " = ? " + " WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String UPDATE_JOB_DETAIL = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " SET "
//            + COL_DESCRIPTION + " = ?, " + COL_JOB_CLASS + " = ?, "
//            + COL_IS_DURABLE + " = ?, "
//            + COL_IS_NONCONCURRENT + " = ?, " + COL_IS_UPDATE_DATA + " = ?, "
//            + COL_REQUESTS_RECOVERY + " = ?, "
//            + COL_JOB_DATAMAP + " = ? " + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String SELECT_TRIGGERS_FOR_JOB = "SELECT "
            + COL_TRIGGER_NAME +" , "+COL_SCHEDULER_NAME+ " FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String SELECT_TRIGGERS_FOR_JOB = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

//    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//    String SELECT_TRIGGERS_FOR_CALENDAR = "SELECT "
//        + COL_TRIGGER_NAME + " FROM "
//        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//        + " AND " + COL_CALENDAR_NAME
//        + " = ?";
//    String SELECT_TRIGGERS_FOR_CALENDAR = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_CALENDAR_NAME
//            + " = ?";

    // DELETE FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String DELETE_JOB_DETAIL = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String DELETE_JOB_DETAIL = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

    // SELECT IS_NONCONCURRENT FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String SELECT_JOB_NONCONCURRENT = "SELECT "
            + COL_IS_NONCONCURRENT + " FROM " + TABLE_PREFIX_SUBST
            + TABLE_JOB_DETAILS + " WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String SELECT_JOB_NONCONCURRENT = "SELECT "
//            + COL_IS_NONCONCURRENT + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

    // SELECT JOB_NAME FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String SELECT_JOB_EXISTENCE = "SELECT " + COL_TRIGGER_NAME
            + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String SELECT_JOB_EXISTENCE = "SELECT " + COL_JOB_NAME
//            + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

    // UPDATE QRTZ_JOB_DETAILS SET JOB_DATA = ?  WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String UPDATE_JOB_DATA = "UPDATE " + TABLE_PREFIX_SUBST
            + TABLE_JOB_DETAILS + " SET " + COL_JOB_DATAMAP + " = ? "
            + " WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String UPDATE_JOB_DATA = "UPDATE " + TABLE_PREFIX_SUBST
//            + TABLE_JOB_DETAILS + " SET " + COL_JOB_DATAMAP + " = ? "
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

    String SELECT_JOB_CFG="SELECT * FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ? ";
    // SELECT * FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String SELECT_JOB_DETAIL = "SELECT *" + " FROM "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND " + COL_TRIGGER_NAME
            + " = ? " ;
//    String SELECT_JOB_DETAIL = "SELECT *" + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND " + COL_JOB_GROUP + " = ?";

    // SELECT COUNT(JOB_NAME)  FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String SELECT_NUM_JOBS = "SELECT COUNT(" + COL_TRIGGER_NAME
            + ") " + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    // SELECT DISTINCT(JOB_GROUP) FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ'
//    String SELECT_JOB_GROUPS = "SELECT DISTINCT("
//            + COL_JOB_GROUP + ") FROM " + TABLE_PREFIX_SUBST
//            + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    // SELECT_JOBS_IN_GROUP_LIKE
//    String SELECT_JOBS_IN_GROUP_LIKE = "SELECT " + COL_JOB_NAME + ", " + COL_JOB_GROUP
//            + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_GROUP + " LIKE ?";

    String SELECT_JOBS_IN_SCHED = "SELECT " + COL_TRIGGER_NAME
            + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    String SELECT_JOBS_IN_SCHED_TRIGGER_NAME = "SELECT " + COL_TRIGGER_NAME
            + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND "+COL_TRIGGER_NAME +" = ?";

    // SELECT JOB_NAME, JOB_GROUP FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_GROUP = ?
//    String SELECT_JOBS_IN_GROUP = "SELECT " + COL_JOB_NAME + ", " + COL_JOB_GROUP
//            + " FROM " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_GROUP + " = ?";

    // INSERT INTO QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA, PRIORITY)  VALUES('MEE_QUARTZ', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    String INSERT_TRIGGER = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " (" + COL_SCHEDULER_NAME + ", " + COL_TRIGGER_NAME
            + ", "
            + COL_DESCRIPTION
            + ", " + COL_NEXT_FIRE_TIME + ", " + COL_PREV_FIRE_TIME + ", "
            + COL_TRIGGER_STATE + ", " + COL_TRIGGER_TYPE + ", "
            + COL_START_TIME + ", " + COL_END_TIME + ", " + COL_CALENDAR_NAME
            + ", " + COL_MISFIRE_INSTRUCTION + ", " + COL_PRIORITY + ") "
            + " VALUES(" + SCHED_NAME_SUBST + ", ?,?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
//    String INSERT_TRIGGER = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " (" + COL_SCHEDULER_NAME + ", " + COL_TRIGGER_NAME
//            + ", " + COL_TRIGGER_GROUP + ", " + COL_JOB_NAME + ", "
//            + COL_JOB_GROUP + ", " + COL_DESCRIPTION
//            + ", " + COL_NEXT_FIRE_TIME + ", " + COL_PREV_FIRE_TIME + ", "
//            + COL_TRIGGER_STATE + ", " + COL_TRIGGER_TYPE + ", "
//            + COL_START_TIME + ", " + COL_END_TIME + ", " + COL_CALENDAR_NAME
//            + ", " + COL_MISFIRE_INSTRUCTION + ", " + COL_JOB_DATAMAP + ", " + COL_PRIORITY + ") "
//            + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // INSERT INTO QRTZ_SIMPLE_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED)  VALUES('MEE_QUARTZ', ?, ?, ?, ?, ?)
    String INSERT_SIMPLE_TRIGGER = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " ("
            + COL_SCHEDULER_NAME + ", "
            + COL_TRIGGER_NAME + ", "
            + COL_REPEAT_COUNT + ", " + COL_REPEAT_INTERVAL + ", "
            + COL_TIMES_TRIGGERED + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?)";
//    String INSERT_SIMPLE_TRIGGER = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " ("
//            + COL_SCHEDULER_NAME + ", "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", "
//            + COL_REPEAT_COUNT + ", " + COL_REPEAT_INTERVAL + ", "
//            + COL_TIMES_TRIGGERED + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?)";

    // INSERT INTO QRTZ_CRON_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID)  VALUES('MEE_QUARTZ', ?, ?, ?, ?)
    String INSERT_CRON_TRIGGER = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " ("
            + COL_SCHEDULER_NAME + ", "
            + COL_TRIGGER_NAME + ", "
            + COL_CRON_EXPRESSION + ", " + COL_TIME_ZONE_ID + ") "
            + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?)";
//    String INSERT_CRON_TRIGGER = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " ("
//            + COL_SCHEDULER_NAME + ", "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", "
//            + COL_CRON_EXPRESSION + ", " + COL_TIME_ZONE_ID + ") "
//            + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?)";

    // INSERT INTO QRTZ_BLOB_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, BLOB_DATA)  VALUES('MEE_QUARTZ', ?, ?, ?)
//    String INSERT_BLOB_TRIGGER = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " ("
//            + COL_SCHEDULER_NAME + ", "
//            + COL_TRIGGER_NAME + ", " + COL_BLOB
//            + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?)";
//    String INSERT_BLOB_TRIGGER = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " ("
//            + COL_SCHEDULER_NAME + ", "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", " + COL_BLOB
//            + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?)";

    String UPDATE_JOB_CFG_SKIP_DATA = "UPDATE {0}JOB_CFG SET DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE = ? ";
    // UPDATE QRTZ_TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String UPDATE_TRIGGER_SKIP_DATA = "UPDATE " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " SET "
            + COL_DESCRIPTION + " = ?, " + COL_NEXT_FIRE_TIME + " = ?, "
            + COL_PREV_FIRE_TIME + " = ?, " + COL_TRIGGER_STATE + " = ?, "
            + COL_TRIGGER_TYPE + " = ?, " + COL_START_TIME + " = ?, "
            + COL_END_TIME + " = ?, " + COL_CALENDAR_NAME + " = ?, "
            + COL_MISFIRE_INSTRUCTION + " = ?, " + COL_PRIORITY 
            + " = ? WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String UPDATE_TRIGGER_SKIP_DATA = "UPDATE " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " SET " + COL_JOB_NAME + " = ?, "
//            + COL_JOB_GROUP + " = ?, "
//            + COL_DESCRIPTION + " = ?, " + COL_NEXT_FIRE_TIME + " = ?, "
//            + COL_PREV_FIRE_TIME + " = ?, " + COL_TRIGGER_STATE + " = ?, "
//            + COL_TRIGGER_TYPE + " = ?, " + COL_START_TIME + " = ?, "
//            + COL_END_TIME + " = ?, " + COL_CALENDAR_NAME + " = ?, "
//            + COL_MISFIRE_INSTRUCTION + " = ?, " + COL_PRIORITY
//            + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME
//            + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // UPDATE QRTZ_TRIGGERS SET JOB_NAME = ?, JOB_GROUP = ?, DESCRIPTION = ?, NEXT_FIRE_TIME = ?, PREV_FIRE_TIME = ?, TRIGGER_STATE = ?, TRIGGER_TYPE = ?, START_TIME = ?, END_TIME = ?, CALENDAR_NAME = ?, MISFIRE_INSTR = ?, PRIORITY = ?, JOB_DATA = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String UPDATE_TRIGGER = "UPDATE " + TABLE_PREFIX_SUBST
        + TABLE_TRIGGERS + " SET "
        + COL_DESCRIPTION + " = ?, " + COL_NEXT_FIRE_TIME + " = ?, "
        + COL_PREV_FIRE_TIME + " = ?, " + COL_TRIGGER_STATE + " = ?, "
        + COL_TRIGGER_TYPE + " = ?, " + COL_START_TIME + " = ?, "
        + COL_END_TIME + " = ?, " + COL_CALENDAR_NAME + " = ?, "
        + COL_MISFIRE_INSTRUCTION + " = ?, " + COL_PRIORITY + " = ?, " 
        + COL_JOB_DATAMAP + " = ? WHERE " 
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_TRIGGER_NAME + " = ? " ;
//    String UPDATE_TRIGGER = "UPDATE " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " SET " + COL_JOB_NAME + " = ?, "
//            + COL_JOB_GROUP + " = ?, "
//            + COL_DESCRIPTION + " = ?, " + COL_NEXT_FIRE_TIME + " = ?, "
//            + COL_PREV_FIRE_TIME + " = ?, " + COL_TRIGGER_STATE + " = ?, "
//            + COL_TRIGGER_TYPE + " = ?, " + COL_START_TIME + " = ?, "
//            + COL_END_TIME + " = ?, " + COL_CALENDAR_NAME + " = ?, "
//            + COL_MISFIRE_INSTRUCTION + " = ?, " + COL_PRIORITY + " = ?, "
//            + COL_JOB_DATAMAP + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // UPDATE QRTZ_SIMPLE_TRIGGERS SET REPEAT_COUNT = ?, REPEAT_INTERVAL = ?, TIMES_TRIGGERED = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String UPDATE_SIMPLE_EXECUTE_CFG = "UPDATE {0}EXECUTE_CFG SET REPEAT_COUNT = ?, REPEAT_INTERVAL = ?, TIMES_TRIGGERED = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
//    String UPDATE_SIMPLE_TRIGGER = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " SET "
//            + COL_REPEAT_COUNT + " = ?, " + COL_REPEAT_INTERVAL + " = ?, "
//            + COL_TIMES_TRIGGERED + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME
//            + " = ? " ;
//    String UPDATE_SIMPLE_TRIGGER = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " SET "
//            + COL_REPEAT_COUNT + " = ?, " + COL_REPEAT_INTERVAL + " = ?, "
//            + COL_TIMES_TRIGGERED + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME
//            + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    String UPDATE_EXECUTE_CFG = "UPDATE {0}EXECUTE_CFG SET CRON_EXPRESSION = ?, TIME_ZONE_ID = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
    // UPDATE QRTZ_CRON_TRIGGERS SET CRON_EXPRESSION = ?, TIME_ZONE_ID = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String UPDATE_CRON_TRIGGER = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " SET "
            + COL_CRON_EXPRESSION + " = ?, " + COL_TIME_ZONE_ID  
            + " = ? WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME
            + " = ? ";
//    String UPDATE_CRON_TRIGGER = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " SET "
//            + COL_CRON_EXPRESSION + " = ?, " + COL_TIME_ZONE_ID
//            + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME
//            + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // UPDATE QRTZ_BLOB_TRIGGERS SET BLOB_DATA = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//    String UPDATE_BLOB_TRIGGER = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " SET " + COL_BLOB
//            + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String UPDATE_BLOB_TRIGGER = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " SET " + COL_BLOB
//            + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND "
//            + COL_TRIGGER_GROUP + " = ?";

    String SELECT_JOB_CFG_EXISTENCE = "SELECT TRIGGER_NAME,SCHED_NAME,TRIGGER_TYPE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
    // 检查触发器是否存在。
    // SELECT TRIGGER_NAME FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_TRIGGER_EXISTENCE = "SELECT "
            + COL_TRIGGER_NAME + " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_TRIGGER_EXISTENCE = "SELECT "
//            + COL_TRIGGER_NAME + " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP
//            + " = ?";

    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String UPDATE_TRIGGER_STATE = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
            + " = ?" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String UPDATE_TRIGGER_STATE = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
//            + " = ?" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND "
//            + COL_TRIGGER_GROUP + " = ?";

    String UPDATE_JOB_STATE_FROM_STATE_RANGE = "UPDATE {0}JOB_CFG SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_STATE IN (%TRIGGER_STATE%) and TRIGGER_TYPE = ? ";

    String UPDATE_JOB_STATE_FROM_STATE = "UPDATE {0}JOB_CFG SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_STATE = ? and TRIGGER_TYPE = ? ";
    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? AND TRIGGER_STATE = ?
    String UPDATE_TRIGGER_STATE_FROM_STATE = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
            + " = ?" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_STATE + " = ?";
//    String UPDATE_TRIGGER_STATE_FROM_STATE = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
//            + " = ?" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND "
//            + COL_TRIGGER_GROUP + " = ? AND " + COL_TRIGGER_STATE + " = ?";

    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ? AND TRIGGER_STATE = ?
    String UPDATE_TRIGGER_GROUP_STATE_FROM_STATE = "UPDATE "
            + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS
            + " SET "
            + COL_TRIGGER_STATE
            + " = ?"
            + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " LIKE ? AND "
            + COL_TRIGGER_STATE + " = ?";
//    String UPDATE_TRIGGER_GROUP_STATE_FROM_STATE = "UPDATE "
//            + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS
//            + " SET "
//            + COL_TRIGGER_STATE
//            + " = ?"
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP
//            + " LIKE ? AND "
//            + COL_TRIGGER_STATE + " = ?";

    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ? AND (TRIGGER_STATE = ? OR TRIGGER_STATE = ? OR TRIGGER_STATE = ?)
    String UPDATE_TRIGGER_STATE_FROM_STATES = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
            + " = ?" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? AND (" + COL_TRIGGER_STATE + " = ? OR "
            + COL_TRIGGER_STATE + " = ? OR " + COL_TRIGGER_STATE + " = ?)";
//    String UPDATE_TRIGGER_STATE_FROM_STATES = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
//            + " = ?" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND "
//            + COL_TRIGGER_GROUP + " = ? AND (" + COL_TRIGGER_STATE + " = ? OR "
//            + COL_TRIGGER_STATE + " = ? OR " + COL_TRIGGER_STATE + " = ?)";

    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ? AND (TRIGGER_STATE = ? OR TRIGGER_STATE = ? OR TRIGGER_STATE = ?)
    String UPDATE_TRIGGER_GROUP_STATE_FROM_STATES = "UPDATE "
            + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS
            + " SET "
            + COL_TRIGGER_STATE
            + " = ?"
            + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " LIKE ? AND ("
            + COL_TRIGGER_STATE
            + " = ? OR "
            + COL_TRIGGER_STATE
            + " = ? OR "
            + COL_TRIGGER_STATE + " = ?)";
//    String UPDATE_TRIGGER_GROUP_STATE_FROM_STATES = "UPDATE "
//            + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS
//            + " SET "
//            + COL_TRIGGER_STATE
//            + " = ?"
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP
//            + " LIKE ? AND ("
//            + COL_TRIGGER_STATE
//            + " = ? OR "
//            + COL_TRIGGER_STATE
//            + " = ? OR "
//            + COL_TRIGGER_STATE + " = ?)";

    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String UPDATE_JOB_TRIGGER_STATES = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
            + " = ? WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String UPDATE_JOB_TRIGGER_STATES = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " SET " + COL_TRIGGER_STATE
//            + " = ? WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME + " = ? AND " + COL_JOB_GROUP
//            + " = ?";

    String UPDATE_JOB_CFG_STATES_FROM_OTHER_STATE = "UPDATE {0}JOB_CFG SET TRIGGER_STATE = ? WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? AND TRIGGER_STATE = ?";
    // UPDATE QRTZ_TRIGGERS SET TRIGGER_STATE = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ? AND TRIGGER_STATE = ?
//    String UPDATE_JOB_TRIGGER_STATES_FROM_OTHER_STATE = "UPDATE "
//            + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS
//            + " SET "
//            + COL_TRIGGER_STATE
//            + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME
//            + " = ? AND "+ COL_TRIGGER_STATE + " = ?";
//    String UPDATE_JOB_TRIGGER_STATES_FROM_OTHER_STATE = "UPDATE "
//            + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS
//            + " SET "
//            + COL_TRIGGER_STATE
//            + " = ? WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME
//            + " = ? AND "
//            + COL_JOB_GROUP
//            + " = ? AND " + COL_TRIGGER_STATE + " = ?";

    // DELETE FROM QRTZ_SIMPLE_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String DELETE_SIMPLE_TRIGGER = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String DELETE_SIMPLE_TRIGGER = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // DELETE FROM QRTZ_CRON_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String DELETE_CRON_TRIGGER = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String DELETE_CRON_TRIGGER = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // DELETE FROM QRTZ_BLOB_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String DELETE_BLOB_TRIGGER = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String DELETE_BLOB_TRIGGER = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // DELETE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String DELETE_TRIGGER = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String DELETE_TRIGGER = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String SELECT_NUM_TRIGGERS_FOR_JOB = "SELECT COUNT("
            + COL_TRIGGER_NAME + ") FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_NUM_TRIGGERS_FOR_JOB = "SELECT COUNT("
//            + COL_TRIGGER_NAME + ") FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME + " = ? AND "
//            + COL_JOB_GROUP + " = ?";


    // SELECT J.JOB_NAME, J.JOB_GROUP, J.IS_DURABLE, J.JOB_CLASS_NAME, J.REQUESTS_RECOVERY FROM QRTZ_TRIGGERS T, QRTZ_JOB_DETAILS J WHERE T.SCHED_NAME = 'MEE_QUARTZ' AND J.SCHED_NAME = 'MEE_QUARTZ' AND T.TRIGGER_NAME = ? AND T.TRIGGER_GROUP = ? AND T.JOB_NAME = J.JOB_NAME AND T.JOB_GROUP = J.JOB_GROUP
    String SELECT_JOB_FOR_TRIGGER = "SELECT J."
            + COL_TRIGGER_NAME
            + ", J." + COL_JOB_CLASS + ", J." + COL_REQUESTS_RECOVERY + " FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " T, " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS
            + " J WHERE T." + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST 
            + " AND J." + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND T." + COL_TRIGGER_NAME + " = ? AND T." + COL_TRIGGER_NAME + " = J."
            + COL_TRIGGER_NAME ;
//    String SELECT_JOB_FOR_TRIGGER = "SELECT J."
//            + COL_JOB_NAME + ", J." + COL_JOB_GROUP + ", J." + COL_IS_DURABLE
//            + ", J." + COL_JOB_CLASS + ", J." + COL_REQUESTS_RECOVERY + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " T, " + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS
//            + " J WHERE T." + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND J." + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND T." + COL_TRIGGER_NAME + " = ? AND T."
//            + COL_TRIGGER_GROUP + " = ? AND T." + COL_JOB_NAME + " = J."
//            + COL_JOB_NAME + " AND T." + COL_JOB_GROUP + " = J."
//            + COL_JOB_GROUP;

    // SELECT * FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_TRIGGER = "SELECT * FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ?";
//    String SELECT_TRIGGER = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // SELECT JOB_DATA FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//    String SELECT_TRIGGER_DATA = "SELECT " +
//            COL_JOB_DATAMAP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? " ;
//    String SELECT_TRIGGER_DATA = "SELECT " +
//            COL_JOB_DATAMAP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    String SELECT_JOB_STATE = "SELECT TRIGGER_STATE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? and TRIGGER_TYPE= ? ";
    // SELECT TRIGGER_STATE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_TRIGGER_STATE = "SELECT "
            + COL_TRIGGER_STATE + " FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_TRIGGER_STATE = "SELECT "
//            + COL_TRIGGER_STATE + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND "
//            + COL_TRIGGER_GROUP + " = ?";

    // SELECT TRIGGER_STATE, NEXT_FIRE_TIME, JOB_NAME, JOB_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_TRIGGER_STATUS = "SELECT "
            + COL_TRIGGER_STATE + ", " + COL_NEXT_FIRE_TIME + ", "
            + COL_TRIGGER_NAME + " FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? " ;
//    String SELECT_TRIGGER_STATUS = "SELECT "
//            + COL_TRIGGER_STATE + ", " + COL_NEXT_FIRE_TIME + ", "
//            + COL_JOB_NAME + ", " + COL_JOB_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // SELECT * FROM QRTZ_SIMPLE_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_SIMPLE_EXECUTE_CFG = "SELECT * FROM {0}EXECUTE_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
    String SELECT_SIMPLE_TRIGGER = "SELECT *" + " FROM "
            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? " ;
//    String SELECT_SIMPLE_TRIGGER = "SELECT *" + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_SIMPLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    String SELECT_CRON_EXECUTE_CFG = "SELECT * FROM {0}EXECUTE_CFG WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ?";
    // SELECT * FROM QRTZ_CRON_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_CRON_TRIGGER = "SELECT *" + " FROM "
            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_CRON_TRIGGER = "SELECT *" + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_CRON_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // SELECT * FROM QRTZ_BLOB_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_BLOB_TRIGGER = "SELECT *" + " FROM "
            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_BLOB_TRIGGER = "SELECT *" + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_BLOB_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // SELECT COUNT(TRIGGER_NAME)  FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String SELECT_NUM_TRIGGERS = "SELECT COUNT("
            + COL_TRIGGER_NAME + ") " + " FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    // SELECT COUNT(TRIGGER_NAME)  FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP = ?
    String SELECT_NUM_TRIGGERS_IN_GROUP = "SELECT COUNT("
            + COL_TRIGGER_NAME + ") " + " FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_NUM_TRIGGERS_IN_GROUP = "SELECT COUNT("
//            + COL_TRIGGER_NAME + ") " + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP + " = ?";

    @Deprecated
    // SELECT DISTINCT(TRIGGER_GROUP) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String SELECT_TRIGGER_GROUPS = "SELECT DISTINCT() FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_TRIGGER_GROUPS = "SELECT DISTINCT("
//            + COL_TRIGGER_GROUP + ") FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    @Deprecated
    // SELECT DISTINCT(TRIGGER_GROUP) FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ?
    String SELECT_TRIGGER_GROUPS_FILTERED = "SELECT DISTINCT() FROM " + TABLE_PREFIX_SUBST
            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST ;
//    String SELECT_TRIGGER_GROUPS_FILTERED = "SELECT DISTINCT("
//            + COL_TRIGGER_GROUP + ") FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND " + COL_TRIGGER_GROUP + " LIKE ?";

    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ?
    String SELECT_TRIGGERS_IN_GROUP_LIKE = "SELECT "
            + COL_TRIGGER_NAME +" , " +COL_SCHEDULER_NAME+ " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST ;
//    String SELECT_TRIGGERS_IN_GROUP_LIKE = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP + " LIKE ?";

    String SELECT_ALL_TRIGGERS_IN_SCHED = "SELECT "
            + COL_TRIGGER_NAME + " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP = ?
    String SELECT_TRIGGERS_IN_GROUP = "SELECT "
            + COL_TRIGGER_NAME +" , "+COL_SCHEDULER_NAME+ " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_TRIGGERS_IN_GROUP = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP + " = ?";

//    // INSERT INTO QRTZ_CALENDARS (SCHED_NAME, CALENDAR_NAME, CALENDAR)  VALUES('MEE_QUARTZ', ?, ?)
//    String INSERT_CALENDAR = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_CALENDARS + " (" + COL_SCHEDULER_NAME + ", " + COL_CALENDAR_NAME
//            + ", " + COL_CALENDAR + ") " + " VALUES(" + SCHED_NAME_SUBST + ", ?, ?)";
//
//    // UPDATE QRTZ_CALENDARS SET CALENDAR = ?  WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//    String UPDATE_CALENDAR = "UPDATE " + TABLE_PREFIX_SUBST
//            + TABLE_CALENDARS + " SET " + COL_CALENDAR + " = ? " + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_CALENDAR_NAME + " = ?";
//
//    // SELECT CALENDAR_NAME FROM QRTZ_CALENDARS WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//    String SELECT_CALENDAR_EXISTENCE = "SELECT "
//            + COL_CALENDAR_NAME + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_CALENDARS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_CALENDAR_NAME + " = ?";
//
//    // SELECT * FROM QRTZ_CALENDARS WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//    String SELECT_CALENDAR = "SELECT *" + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_CALENDARS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_CALENDAR_NAME + " = ?";
//
//    // SELECT CALENDAR_NAME FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//    String SELECT_REFERENCED_CALENDAR = "SELECT "
//            + COL_CALENDAR_NAME + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_CALENDAR_NAME + " = ?";
//
//    // DELETE FROM QRTZ_CALENDARS WHERE SCHED_NAME = 'MEE_QUARTZ' AND CALENDAR_NAME = ?
//    String DELETE_CALENDAR = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_CALENDARS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_CALENDAR_NAME + " = ?";
//
//    // SELECT COUNT(CALENDAR_NAME)  FROM QRTZ_CALENDARS WHERE SCHED_NAME = 'MEE_QUARTZ'
//    String SELECT_NUM_CALENDARS = "SELECT COUNT("
//            + COL_CALENDAR_NAME + ") " + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_CALENDARS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//
//    // SELECT CALENDAR_NAME FROM QRTZ_CALENDARS WHERE SCHED_NAME = 'MEE_QUARTZ'
//    String SELECT_CALENDARS = "SELECT " + COL_CALENDAR_NAME
//            + " FROM " + TABLE_PREFIX_SUBST + TABLE_CALENDARS
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    // todo ...
    // SELECT MIN(NEXT_FIRE_TIME) AS ALIAS_NXT_FR_TM FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME >= 0
    String SELECT_NEXT_FIRE_TIME = "SELECT MIN("
            + COL_NEXT_FIRE_TIME + ") AS " + ALIAS_COL_NEXT_FIRE_TIME
            + " FROM " + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_STATE + " = ? AND " + COL_NEXT_FIRE_TIME + " >= 0";
    // 选择将在给定激发时间激发的触发器。
    // SELECT TRIGGER_NAME, TRIGGER_GROUP FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME = ?
    String SELECT_TRIGGER_FOR_FIRE_TIME = "SELECT "
            + COL_TRIGGER_NAME +" , "+COL_SCHEDULER_NAME+ " FROM "
            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_TRIGGER_STATE + " = ? AND " + COL_NEXT_FIRE_TIME + " = ?";
//    String SELECT_TRIGGER_FOR_FIRE_TIME = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_STATE + " = ? AND " + COL_NEXT_FIRE_TIME + " = ?";

//    String SELECT_NEXT_JOB_TO_ACQUIRE = "SELECT TRIGGER_NAME, NEXT_FIRE_TIME, PRIORITY, SCHED_NAME,TRIGGER_TYPE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME <= ? AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= ?)) ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC";
//    String SELECT_NEXT_JOB_TO_ACQUIRE = "SELECT TRIGGER_NAME, NEXT_FIRE_TIME, PRIORITY, SCHED_NAME,TRIGGER_TYPE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} AND TRIGGER_STATE IN (%TRIGGER_STATE%) AND NEXT_FIRE_TIME <= ? AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= ?)) ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC";
//    String SELECT_NEXT_JOB_TO_ACQUIRE = "SELECT TRIGGER_NAME, NEXT_FIRE_TIME, PRIORITY, SCHED_NAME,TRIGGER_TYPE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} " +
//        "AND TRIGGER_STATE IN (%TRIGGER_STATE%) " +
//        "AND NEXT_FIRE_TIME <= ? " +
//        "AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= ?)) ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC";
    String SELECT_NEXT_JOB_TO_ACQUIRE = "SELECT TRIGGER_NAME, NEXT_FIRE_TIME, PRIORITY, SCHED_NAME,TRIGGER_TYPE FROM {0}JOB_CFG WHERE SCHED_NAME = {1} " +
            "AND TRIGGER_STATE IN (%TRIGGER_STATE%) " +
            "AND (NEXT_FIRE_TIME <= ? AND NEXT_FIRE_TIME >= ?) OR NEXT_FIRE_TIME < ? ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC";
    // 选择将在两个给定时间戳之间激发的下一个触发器，按激发时间的升序，然后按优先级降序排列。
    // SELECT TRIGGER_NAME, TRIGGER_GROUP, NEXT_FIRE_TIME, PRIORITY FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME <= ? AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= ?)) ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC
    String SELECT_NEXT_TRIGGER_TO_ACQUIRE = "SELECT "
        + COL_TRIGGER_NAME + ", "
        + COL_NEXT_FIRE_TIME + ", " + COL_PRIORITY +" , "+COL_SCHEDULER_NAME+ " FROM "
        + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_TRIGGER_STATE + " = ? AND " + COL_NEXT_FIRE_TIME + " <= ? " 
        + "AND (" + COL_MISFIRE_INSTRUCTION + " = -1 OR (" +COL_MISFIRE_INSTRUCTION+ " != -1 AND "+ COL_NEXT_FIRE_TIME + " >= ?)) "
        + "ORDER BY "+ COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";
//    String SELECT_NEXT_TRIGGER_TO_ACQUIRE = "SELECT "
//            + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", "
//            + COL_NEXT_FIRE_TIME + ", " + COL_PRIORITY + " FROM "
//            + TABLE_PREFIX_SUBST + TABLE_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_STATE + " = ? AND " + COL_NEXT_FIRE_TIME + " <= ? "
//            + "AND (" + COL_MISFIRE_INSTRUCTION + " = -1 OR (" +COL_MISFIRE_INSTRUCTION+ " != -1 AND "+ COL_NEXT_FIRE_TIME + " >= ?)) "
//            + "ORDER BY "+ COL_NEXT_FIRE_TIME + " ASC, " + COL_PRIORITY + " DESC";

    @Deprecated
    // 写入 存储与已触发的Trigger相关的状态信息
    // INSERT INTO QRTZ_FIRED_TRIGGERS (SCHED_NAME, ENTRY_ID, TRIGGER_NAME, TRIGGER_GROUP, INSTANCE_NAME, FIRED_TIME, SCHED_TIME, STATE, JOB_NAME, JOB_GROUP, IS_NONCONCURRENT, REQUESTS_RECOVERY, PRIORITY) VALUES('MEE_QUARTZ', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    String INSERT_FIRED_TRIGGER = "INSERT INTO {0}FIRED_TRIGGERS (SCHED_NAME, ENTRY_ID, TRIGGER_NAME,TRIGGER_TYPE, INSTANCE_NAME, FIRED_TIME, SCHED_TIME, STATE, IS_NONCONCURRENT, REQUESTS_RECOVERY, PRIORITY) " +
            "VALUES({1}, ?,?, ?, ?, ?, ?, ?, ?, ?,?) ";
//    String INSERT_FIRED_TRIGGER = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " (" + COL_SCHEDULER_NAME + ", " + COL_ENTRY_ID
//            + ", " + COL_TRIGGER_NAME + ", " + COL_TRIGGER_GROUP + ", "
//            + COL_INSTANCE_NAME + ", "
//            + COL_FIRED_TIME + ", " + COL_SCHED_TIME + ", " + COL_ENTRY_STATE + ", " + COL_JOB_NAME
//            + ", " + COL_JOB_GROUP + ", " + COL_IS_NONCONCURRENT + ", "
//            + COL_REQUESTS_RECOVERY + ", " + COL_PRIORITY
//            + ") VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Deprecated
    // 更新 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 + 调度器实例id
    // UPDATE QRTZ_FIRED_TRIGGERS SET INSTANCE_NAME = ?, FIRED_TIME = ?, SCHED_TIME = ?, STATE = ?, JOB_NAME = ?, JOB_GROUP = ?, IS_NONCONCURRENT = ?, REQUESTS_RECOVERY = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND ENTRY_ID = ?
    String UPDATE_FIRED_TRIGGER = "UPDATE {0}FIRED_TRIGGERS SET INSTANCE_NAME = ?, FIRED_TIME = ?, SCHED_TIME = ?, STATE = ?, IS_NONCONCURRENT = ?, REQUESTS_RECOVERY = ? " +
            "WHERE SCHED_NAME = {1} AND ENTRY_ID = ? AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
//    String UPDATE_FIRED_TRIGGER = "UPDATE "
//        + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " SET "
//        + COL_INSTANCE_NAME + " = ?, "
//        + COL_FIRED_TIME + " = ?, " + COL_SCHED_TIME + " = ?, " + COL_ENTRY_STATE + " = ?, "
//        + COL_IS_NONCONCURRENT + " = ?, "
//        + COL_REQUESTS_RECOVERY + " = ? WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//        + " AND " + COL_ENTRY_ID + " = ?";
//    String UPDATE_FIRED_TRIGGER = "UPDATE "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " SET "
//            + COL_INSTANCE_NAME + " = ?, "
//            + COL_FIRED_TIME + " = ?, " + COL_SCHED_TIME + " = ?, " + COL_ENTRY_STATE + " = ?, " + COL_JOB_NAME
//            + " = ?, " + COL_JOB_GROUP + " = ?, " + COL_IS_NONCONCURRENT + " = ?, "
//            + COL_REQUESTS_RECOVERY + " = ? WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_ENTRY_ID + " = ?";

    // 查询 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 + 调度器实例名
    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
    String SELECT_INSTANCES_FIRED_TRIGGERS = "SELECT * FROM "
            + TABLE_PREFIX_SUBST
            + TABLE_FIRED_TRIGGERS
            + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_INSTANCE_NAME + " = ?";

    // 查询 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 + 调度器实例名 + 是否接受恢复执行
    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ? AND REQUESTS_RECOVERY = ?
    String SELECT_INSTANCES_RECOVERABLE_FIRED_TRIGGERS = "SELECT * FROM "
            + TABLE_PREFIX_SUBST
            + TABLE_FIRED_TRIGGERS
            + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_INSTANCE_NAME + " = ? AND " + COL_REQUESTS_RECOVERY + " = ?";
    // 获取当前正在执行的已标识作业的实例数。
    // SELECT COUNT(TRIGGER_NAME) FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
//    String SELECT_JOB_EXECUTION_COUNT = "SELECT COUNT("
//            + COL_TRIGGER_NAME + ") FROM " + TABLE_PREFIX_SUBST
//            + TABLE_FIRED_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_JOB_EXECUTION_COUNT = "SELECT COUNT("
//            + COL_TRIGGER_NAME + ") FROM " + TABLE_PREFIX_SUBST
//            + TABLE_FIRED_TRIGGERS + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME + " = ? AND "
//            + COL_JOB_GROUP + " = ?";

//    @Deprecated
//    String SELECT_FIRED_TRIGGERS = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 查询 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 + 触发器的名称 + 触发器所属组的名称
    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
    String SELECT_FIRED_TRIGGER =  "SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ?  ";
//    String SELECT_FIRED_TRIGGER = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? ";
//    String SELECT_FIRED_TRIGGER = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? AND " + COL_TRIGGER_GROUP + " = ?";

    // 查询 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 + 触发器所属组的名称
    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP = ?
//    String SELECT_FIRED_TRIGGER_GROUP = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_FIRED_TRIGGER_GROUP = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP + " = ?";

    // 查询 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 + 集群中job的名称 + 集群中job的所属组的名称
    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_NAME = ? AND JOB_GROUP = ?
    String SELECT_FIRED_TRIGGERS_OF_JOB = "SELECT * FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
//    String SELECT_FIRED_TRIGGERS_OF_JOB = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_NAME + " = ? " ;
//    String SELECT_FIRED_TRIGGERS_OF_JOB = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_NAME + " = ? AND " + COL_JOB_GROUP + " = ?";

    // 查询 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称 及 实例名称集群中job的所属组的名称
    // SELECT * FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND JOB_GROUP = ?
//    String SELECT_FIRED_TRIGGERS_OF_JOB_GROUP = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST
//            + TABLE_FIRED_TRIGGERS
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_FIRED_TRIGGERS_OF_JOB_GROUP = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST
//            + TABLE_FIRED_TRIGGERS
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_JOB_GROUP + " = ?";
    @Deprecated
    // 删除 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称及实例名称 及 调度器实例id
    // DELETE FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND ENTRY_ID = ?
    String DELETE_FIRED_TRIGGER = "DELETE FROM {0}FIRED_TRIGGERS WHERE SCHED_NAME = {1} AND ENTRY_ID = ? AND TRIGGER_NAME = ? AND TRIGGER_TYPE = ? ";
//    String DELETE_FIRED_TRIGGER = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_ENTRY_ID + " = ?";
    @Deprecated
    // 删除 存储与已触发的Trigger相关的状态信息 根据应用实例/调度名称及实例名称
    // DELETE FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
    String DELETE_INSTANCES_FIRED_TRIGGERS = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_FIRED_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
            + " AND " + COL_INSTANCE_NAME + " = ?";

//    @Deprecated
//    String DELETE_NO_RECOVERY_FIRED_TRIGGERS = "DELETE FROM "
//            + TABLE_PREFIX_SUBST
//            + TABLE_FIRED_TRIGGERS
//            + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_INSTANCE_NAME + " = ?" + COL_REQUESTS_RECOVERY + " = ?";
    // 删除 存储Simple类型的Trigger 根据应用实例/调度名称
    // DELETE FROM QRTZ_SIMPLE_TRIGGERS  WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_ALL_SIMPLE_TRIGGERS = "DELETE FROM " + TABLE_PREFIX_SUBST + "SIMPLE_TRIGGERS " + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    @Deprecated
    // 删除 存储CalendarIntervalTrigger和DailyTimeIntervalTrigger两种类型的触发器 根据应用实例/调度名称
    // DELETE FROM QRTZ_SIMPROP_TRIGGERS  WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_ALL_SIMPROP_TRIGGERS = "DELETE FROM " + TABLE_PREFIX_SUBST + "SIMPROP_TRIGGERS " + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 删除 存放Cron类型的Trigger 根据应用实例/调度名称
    // DELETE FROM QRTZ_CRON_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_ALL_CRON_TRIGGERS = "DELETE FROM " + TABLE_PREFIX_SUBST + "CRON_TRIGGERS" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 删除 以Blob类型存储的Trigger 根据应用实例/调度名称
    // DELETE FROM QRTZ_BLOB_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_ALL_BLOB_TRIGGERS = "DELETE FROM " + TABLE_PREFIX_SUBST + "BLOB_TRIGGERS" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 删除已配置的Trigger的基本信息 根据应用实例/调度名称
    // DELETE FROM QRTZ_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_ALL_TRIGGERS = "DELETE FROM " + TABLE_PREFIX_SUBST + "TRIGGERS" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 已配置的JobDetail信息 根据应用实例/调度名称
    // DELETE FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String DELETE_ALL_JOB_DETAILS = "DELETE FROM " + TABLE_PREFIX_SUBST + "JOB_DETAILS" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    // 删除日历信息 根据应用实例/调度名称
//    // DELETE FROM QRTZ_CALENDARS WHERE SCHED_NAME = 'MEE_QUARTZ'
//    String DELETE_ALL_CALENDARS = "DELETE FROM " + TABLE_PREFIX_SUBST + "CALENDARS" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 删除已暂停的Trigger组的信息 根据应用名称(调度名称)
    // DELETE FROM QRTZ_PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = 'MEE_QUARTZ'
//    String DELETE_ALL_PAUSED_TRIGGER_GRPS = "DELETE FROM " + TABLE_PREFIX_SUBST + "PAUSED_TRIGGER_GRPS" + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    @Deprecated
    // 查询已触发的Trigger相关的状态信息 ，只取实例名称
    // SELECT DISTINCT INSTANCE_NAME FROM QRTZ_FIRED_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String SELECT_FIRED_TRIGGER_INSTANCE_NAMES = 
            "SELECT DISTINCT " + COL_INSTANCE_NAME + " FROM "
            + TABLE_PREFIX_SUBST
            + TABLE_FIRED_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 写入计划程序实例状态记录
    // INSERT INTO QRTZ_SCHEDULER_STATE (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) VALUES('MEE_QUARTZ', ?, ?, ?)
    String INSERT_SCHEDULER_STATE = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_SCHEDULER_STATE + " ("
            + COL_SCHEDULER_NAME + ", "
            + COL_INSTANCE_NAME + ", " + COL_LAST_CHECKIN_TIME + ", "
            + COL_CHECKIN_INTERVAL + ") VALUES(" + SCHED_NAME_SUBST + ", ?, ?, ?)";
    // 查询计划程序实例状态记录 根据应用及实例名臣
    // SELECT * FROM QRTZ_SCHEDULER_STATE WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
//    String SELECT_SCHEDULER_STATE = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_SCHEDULER_STATE + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_INSTANCE_NAME + " = ?";
    // 查询计划程序实例状态记录
    // SELECT * FROM QRTZ_SCHEDULER_STATE WHERE SCHED_NAME = 'MEE_QUARTZ'
//    String SELECT_SCHEDULER_STATES = "SELECT * FROM "
//            + TABLE_PREFIX_SUBST + TABLE_SCHEDULER_STATE
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
    // 更新计划程序实例状态记录 根据应用及实例名称
    // DELETE FROM QRTZ_SCHEDULER_STATE WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
    String DELETE_SCHEDULER_STATE = "DELETE FROM "
        + TABLE_PREFIX_SUBST + TABLE_SCHEDULER_STATE + " WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_INSTANCE_NAME + " = ?";
    // 更新计划程序实例状态记录。
    // UPDATE QRTZ_SCHEDULER_STATE SET LAST_CHECKIN_TIME = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND INSTANCE_NAME = ?
    String UPDATE_SCHEDULER_STATE = "UPDATE "
        + TABLE_PREFIX_SUBST + TABLE_SCHEDULER_STATE + " SET " 
        + COL_LAST_CHECKIN_TIME + " = ? WHERE "
        + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
        + " AND " + COL_INSTANCE_NAME + " = ?";

    @Deprecated
    // 写入暂停的
    // INSERT INTO QRTZ_PAUSED_TRIGGER_GRPS (SCHED_NAME, TRIGGER_GROUP) VALUES('MEE_QUARTZ', ?)
    String INSERT_PAUSED_TRIGGER_GROUP = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_PAUSED_TRIGGERS + " ("
            + COL_SCHEDULER_NAME +") VALUES(" + SCHED_NAME_SUBST + ")";
//    String INSERT_PAUSED_TRIGGER_GROUP = "INSERT INTO "
//            + TABLE_PREFIX_SUBST + TABLE_PAUSED_TRIGGERS + " ("
//            + COL_SCHEDULER_NAME + ", "
//            + COL_TRIGGER_GROUP + ") VALUES(" + SCHED_NAME_SUBST + ", ?)";

    @Deprecated
    // todo ...
    // SELECT TRIGGER_GROUP FROM QRTZ_PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP = ?
    String SELECT_PAUSED_TRIGGER_GROUP = "SELECT  FROM " + TABLE_PREFIX_SUBST
            + TABLE_PAUSED_TRIGGERS + " WHERE " 
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_PAUSED_TRIGGER_GROUP = "SELECT "
//            + COL_TRIGGER_GROUP + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_PAUSED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP + " = ?";

    @Deprecated
    // SELECT TRIGGER_GROUP FROM QRTZ_PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = 'MEE_QUARTZ'
    String SELECT_PAUSED_TRIGGER_GROUPS = "SELECT " + " FROM " + TABLE_PREFIX_SUBST
        + TABLE_PAUSED_TRIGGERS
        + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String SELECT_PAUSED_TRIGGER_GROUPS = "SELECT "
//            + COL_TRIGGER_GROUP + " FROM " + TABLE_PREFIX_SUBST
//            + TABLE_PAUSED_TRIGGERS
//            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    @Deprecated
    // 已暂停的Trigger组的信息
    // DELETE FROM QRTZ_PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_GROUP LIKE ?
    String DELETE_PAUSED_TRIGGER_GROUP = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_PAUSED_TRIGGERS + " WHERE "
            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;
//    String DELETE_PAUSED_TRIGGER_GROUP = "DELETE FROM "
//            + TABLE_PREFIX_SUBST + TABLE_PAUSED_TRIGGERS + " WHERE "
//            + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST
//            + " AND " + COL_TRIGGER_GROUP + " LIKE ?";

    // 删除已暂停的Trigger组的信息
    // DELETE FROM QRTZ_PAUSED_TRIGGER_GRPS WHERE SCHED_NAME = 'MEE_QUARTZ'
    @Deprecated
    String DELETE_PAUSED_TRIGGER_GROUPS = "DELETE FROM "
            + TABLE_PREFIX_SUBST + TABLE_PAUSED_TRIGGERS
            + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST;

    //  CREATE TABLE qrtz_scheduler_state(INSTANCE_NAME VARCHAR2(80) NOT NULL,
    // LAST_CHECKIN_TIME NUMBER(13) NOT NULL, CHECKIN_INTERVAL NUMBER(13) NOT
    // NULL, PRIMARY KEY (INSTANCE_NAME));

    String INSERT_EXECUTE_CFG ="INSERT INTO "+TABLE_PREFIX_SUBST+"EXECUTE_CFG (SCHED_NAME,TRIGGER_NAME,TRIGGER_TYPE,CRON_EXPRESSION,TIME_ZONE_ID,REPEAT_COUNT,REPEAT_INTERVAL,TIMES_TRIGGERED) VALUES (?,?,?,?,?,?,?,?)";
    String INSERT_JOB_CFG ="INSERT INTO "+TABLE_PREFIX_SUBST+"JOB_CFG(\n" +
            "SCHED_NAME,TRIGGER_NAME,TRIGGER_TYPE,TRIGGER_STATE,DESCRIPTION,NEXT_FIRE_TIME,PREV_FIRE_TIME,PRIORITY,START_TIME,END_TIME,CALENDAR_NAME,MISFIRE_INSTR,JOB_CLASS_NAME,IS_NONCONCURRENT,IS_UPDATE_DATA,REQUESTS_RECOVERY,JOB_DATA          \n" +
            ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

}

// EOF
