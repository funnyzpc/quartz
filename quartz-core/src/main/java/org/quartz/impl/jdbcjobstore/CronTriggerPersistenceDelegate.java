/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.impl.jdbcjobstore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.Key;

public class CronTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants {

    protected String tablePrefix;
    protected String schedNameLiteral;
    @Override
    public void initialize(String theTablePrefix, String schedName) {
        this.tablePrefix = theTablePrefix;
        this.schedNameLiteral = "'" + schedName + "'";
    }
    @Override
    public String getHandledTriggerTypeDiscriminator() {
        return TTYPE_CRON;
    }
    @Override
    public boolean canHandleTriggerType(OperableTrigger trigger) {
        return ((trigger instanceof CronTriggerImpl) /*&& !((CronTriggerImpl)trigger).hasAdditionalProperties()*/);
    }
    @Override
    public int deleteExtendedTriggerProperties(Connection conn, Key key) throws SQLException {
        PreparedStatement ps = null;
        try {
            // DELETE FROM QRTZ_CRON_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
            ps = conn.prepareStatement(Util.rtp(DELETE_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, key.getName());
//            ps.setString(2, triggerKey.getGroup());
            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }
    @Override
    public int insertExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {
        CronTrigger cronTrigger = (CronTrigger)trigger;
        PreparedStatement ps = null;
        try {
            // INSERT INTO QRTZ_CRON_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID)  VALUES('MEE_QUARTZ', ?, ?, ?, ?)
            ps = conn.prepareStatement(Util.rtp(INSERT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps.setString(1, trigger.getKey().getName());
//            ps.setString(2, trigger.getKey().getGroup());
            ps.setString(2, cronTrigger.getCronExpression());
            ps.setString(3, cronTrigger.getTimeZone().getID());
            return ps.executeUpdate();
        } finally {
            Util.closeStatement(ps);
        }
    }
    @Override
    public TriggerPropertyBundle loadExtendedTriggerProperties(Connection conn,Key triggerKey) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // SELECT * FROM QRTZ_CRON_TRIGGERS WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//            ps = conn.prepareStatement(Util.rtp(SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps = conn.prepareStatement(Util.rtp(SELECT_CRON_EXECUTE_CFG, tablePrefix, schedNameLiteral));
            ps.setString(1, triggerKey.getName());
            ps.setString(2,triggerKey.getType());
//            ps.setString(2, triggerKey.getGroup());
            rs = ps.executeQuery();
            if (rs.next()) {
                String cronExpr = rs.getString(COL_CRON_EXPRESSION);
                String timeZoneId = rs.getString(COL_TIME_ZONE_ID);
                CronScheduleBuilder cb = CronScheduleBuilder.cronSchedule(cronExpr);
                if (timeZoneId != null) {
                    cb.inTimeZone(TimeZone.getTimeZone(timeZoneId));
                }
                return new TriggerPropertyBundle(cb, null, null);
            }
//            throw new IllegalStateException("No record found for selection of Trigger with key: '" + triggerKey + "' and statement: " + Util.rtp(SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            throw new IllegalStateException("No record found for selection of Trigger with key: '" + triggerKey + "' and statement: " + ps.toString());
        } finally {
            Util.closeResultSet(rs);
            Util.closeStatement(ps);
        }
    }

    @Override
    public int updateExtendedTriggerProperties(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {
        CronTrigger cronTrigger = (CronTrigger)trigger;
        PreparedStatement ps = null;
        try {
            // UPDATE QRTZ_CRON_TRIGGERS SET CRON_EXPRESSION = ?, TIME_ZONE_ID = ? WHERE SCHED_NAME = 'MEE_QUARTZ' AND TRIGGER_NAME = ? AND TRIGGER_GROUP = ?
//            ps = conn.prepareStatement(Util.rtp(UPDATE_CRON_TRIGGER, tablePrefix, schedNameLiteral));
            ps = conn.prepareStatement(Util.rtp(UPDATE_EXECUTE_CFG, tablePrefix, schedNameLiteral));
            ps.setString(1, cronTrigger.getCronExpression());
            ps.setString(2, cronTrigger.getTimeZone().getID());
            ps.setString(3, trigger.getKey().getName());
            ps.setString(4, trigger.getKey().getType());
//            ps.setString(4, trigger.getKey().getGroup());
            return ps.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        }finally {
            Util.closeStatement(ps);
        }
    }

}
