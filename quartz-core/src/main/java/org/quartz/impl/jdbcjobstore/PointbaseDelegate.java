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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.quartz.JobDetail;
import org.quartz.spi.OperableTrigger;

/**
 * <p>
 * This is a driver delegate for the Pointbase JDBC driver.
 * </p>
 * 
 * @author Gregg Freeman
 */
public class PointbaseDelegate extends StdJDBCDelegate {

    //---------------------------------------------------------------------------
    // jobs
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert the job detail record.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to insert
     * @return number of rows inserted
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    @Override           
    public int insertJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
        //log.debug( "Inserting JobDetail " + job );
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PreparedStatement ps = null;
        int insertResult = 0;
        try {
            ps = conn.prepareStatement(rtp(INSERT_JOB_DETAIL));
            ps.setString(1, job.getKey().getName());
//            ps.setString(2, job.getKey().getGroup());
            ps.setString(2, job.getDescription());
            ps.setString(3, job.getJobClassName());
//            setBoolean(ps, 4, job.isDurable());
            setBoolean(ps, 4, job.isConcurrentExectionDisallowed());
            setBoolean(ps, 5, job.isPersistJobDataAfterExecution());
            setBoolean(ps, 6, job.requestsRecovery());
            ps.setBinaryStream(7, bais, len);
            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
        return insertResult;
    }

    /**
     * <p>
     * Update the job detail record.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to update
     * @return number of rows updated
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    @Override           
    public int updateJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
        //log.debug( "Updating job detail " + job );
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PreparedStatement ps = null;
        int insertResult = 0;
        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_DETAIL));
            ps.setString(1, job.getDescription());
            ps.setString(2, job.getJobClassName());
//            setBoolean(ps, 3, job.isDurable());
            setBoolean(ps, 3, job.isConcurrentExectionDisallowed());
            setBoolean(ps, 4, job.isPersistJobDataAfterExecution());
            setBoolean(ps, 5, job.requestsRecovery());
            ps.setBinaryStream(6, bais, len);
            ps.setString(7, job.getKey().getName());
//            ps.setString(9, job.getKey().getGroup());
            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
        return insertResult;
    }

    @Override
    public int insertTrigger(Connection conn, OperableTrigger trigger, String state,JobDetail jobDetail) throws SQLException, IOException {
//        ByteArrayOutputStream baos = serializeJobData(trigger.getJobDataMap());
//        int len = baos.toByteArray().length;
//        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PreparedStatement ps = null;
        int insertResult = 0;
        try {
            ps = conn.prepareStatement(rtp(INSERT_TRIGGER));
            ps.setString(1, trigger.getKey().getName());
//            ps.setString(2, trigger.getKey().getGroup());
//            ps.setString(2, trigger.getJobKey().getName());
//            ps.setString(4, trigger.getJobKey().getGroup());
            ps.setString(2, trigger.getDescription());
            ps.setBigDecimal(3, new BigDecimal(String.valueOf(trigger.getNextFireTime().getTime())));
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(4, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(5, state);
            TriggerPersistenceDelegate tDel = findTriggerPersistenceDelegate(trigger);
//            String type = TTYPE_BLOB;
//            if(tDel != null){
//                type = tDel.getHandledTriggerTypeDiscriminator();
//            }
            String type = tDel.getHandledTriggerTypeDiscriminator();
            ps.setString(6, type);
            ps.setBigDecimal(7, new BigDecimal(String.valueOf(trigger.getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(8, new BigDecimal(String.valueOf(endTime)));
            ps.setString(9, trigger.getCalendarName());
            ps.setInt(10, trigger.getMisfireInstruction());
//            ps.setBinaryStream(11, bais, len);
            ps.setInt(11, trigger.getPriority());
            insertResult = ps.executeUpdate();
//            if(tDel == null)
//                insertBlobTrigger(conn, trigger);
//            else
//                tDel.insertExtendedTriggerProperties(conn, trigger, state, jobDetail);
            tDel.insertExtendedTriggerProperties(conn, trigger, state, jobDetail);

        } finally {
            closeStatement(ps);
        }
        return insertResult;
    }
    
//    @Override
//    public int updateTrigger(Connection conn, OperableTrigger trigger, String state,JobDetail jobDetail) throws SQLException, IOException {
////        ByteArrayOutputStream baos = serializeJobData(trigger.getJobDataMap());
////        int len = baos.toByteArray().length;
////        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//        PreparedStatement ps = null;
//        int insertResult = 0;
//        try {
//            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER));
//            ps.setString(1, trigger.getKey().getName());
////            ps.setString(2, trigger.getJobKey().getGroup());
//            ps.setString(2, trigger.getDescription());
//            long nextFireTime = -1;
//            if (trigger.getNextFireTime() != null) {
//                nextFireTime = trigger.getNextFireTime().getTime();
//            }
//            ps.setBigDecimal(3, new BigDecimal(String.valueOf(nextFireTime)));
//            long prevFireTime = -1;
//            if (trigger.getPreviousFireTime() != null) {
//                prevFireTime = trigger.getPreviousFireTime().getTime();
//            }
//            ps.setBigDecimal(4, new BigDecimal(String.valueOf(prevFireTime)));
//            ps.setString(5, state);
//            TriggerPersistenceDelegate tDel = findTriggerPersistenceDelegate(trigger);
////            String type = TTYPE_BLOB;
////            if(tDel != null)
////                type = tDel.getHandledTriggerTypeDiscriminator();
//            String type = tDel.getHandledTriggerTypeDiscriminator();
//            ps.setString(6, type);
//            ps.setBigDecimal(7, new BigDecimal(String.valueOf(trigger
//                    .getStartTime().getTime())));
//            long endTime = 0;
//            if (trigger.getEndTime() != null) {
//                endTime = trigger.getEndTime().getTime();
//            }
//            ps.setBigDecimal(8, new BigDecimal(String.valueOf(endTime)));
//            ps.setString(9, trigger.getCalendarName());
//            ps.setInt(10, trigger.getMisfireInstruction());
//
//            ps.setInt(11, trigger.getPriority());
////            ps.setBinaryStream(12, bais, len);
//            ps.setString(12, trigger.getKey().getName());
////            ps.setString(15, trigger.getKey().getGroup());
//
//            insertResult = ps.executeUpdate();
////            if(tDel == null)
////                updateBlobTrigger(conn, trigger);
////            else
////                tDel.updateExtendedTriggerProperties(conn, trigger, state, jobDetail);
//            tDel.updateExtendedTriggerProperties(conn, trigger, state, jobDetail);
//
//        } finally {
//            closeStatement(ps);
//        }
//        return insertResult;
//    }

    /**
     * <p>
     * Update the job data map for the given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to update
     * @return the number of rows updated
     */
    @Override           
    public int updateJobData(Connection conn, JobDetail job) throws IOException, SQLException {
        //log.debug( "Updating Job Data for Job " + job );
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_DATA));
            ps.setBinaryStream(1, bais, len);
            ps.setString(2, job.getKey().getName());
//            ps.setString(3, job.getKey().getGroup());
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    //---------------------------------------------------------------------------
    // triggers
    //---------------------------------------------------------------------------

    //---------------------------------------------------------------------------
    // calendars
    //---------------------------------------------------------------------------

//    /**
//     * <p>
//     * Insert a new calendar.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name for the new calendar
//     * @param calendar
//     *          the calendar
//     * @return the number of rows inserted
//     * @throws IOException
//     *           if there were problems serializing the calendar
//     */
//    @Override
//    public int insertCalendar(Connection conn, String calendarName,Calendar calendar) throws IOException, SQLException {
//        //log.debug( "Inserting Calendar " + calendarName + " : " + calendar
//        // );
//        ByteArrayOutputStream baos = serializeObject(calendar);
//        byte buf[] = baos.toByteArray();
//        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
//        PreparedStatement ps = null;
//        try {
//            ps = conn.prepareStatement(rtp(INSERT_CALENDAR));
//            ps.setString(1, calendarName);
//            ps.setBinaryStream(2, bais, buf.length);
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }

//    /**
//     * <p>
//     * Update a calendar.
//     * </p>
//     *
//     * @param conn
//     *          the DB Connection
//     * @param calendarName
//     *          the name for the new calendar
//     * @param calendar
//     *          the calendar
//     * @return the number of rows updated
//     * @throws IOException
//     *           if there were problems serializing the calendar
//     */
//    @Override
//    public int updateCalendar(Connection conn, String calendarName,Calendar calendar) throws IOException, SQLException {
//        //log.debug( "Updating calendar " + calendarName + " : " + calendar );
//        ByteArrayOutputStream baos = serializeObject(calendar);
//        byte buf[] = baos.toByteArray();
//        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
//        PreparedStatement ps = null;
//        try {
//            ps = conn.prepareStatement(rtp(UPDATE_CALENDAR));
//            ps.setBinaryStream(1, bais, buf.length);
//            ps.setString(2, calendarName);
//            return ps.executeUpdate();
//        } finally {
//            closeStatement(ps);
//        }
//    }

    //---------------------------------------------------------------------------
    // protected methods that can be overridden by subclasses
    //---------------------------------------------------------------------------

//    /**
//     * <p>
//     * This method should be overridden by any delegate subclasses that need
//     * special handling for BLOBs. The default implementation uses standard
//     * JDBC <code>java.sql.Blob</code> operations.
//     * </p>
//     *
//     * @param rs
//     *          the result set, already queued to the correct row
//     * @param colName
//     *          the column name for the BLOB
//     * @return the deserialized Object from the ResultSet BLOB
//     * @throws ClassNotFoundException
//     *           if a class found during deserialization cannot be found
//     * @throws IOException
//     *           if deserialization causes an error
//     */
//    @Override
//    protected Object getObjectFromBlob(ResultSet rs, String colName) throws ClassNotFoundException, IOException, SQLException {
//        //log.debug( "Getting blob from column: " + colName );
//        Object obj = null;
//        byte binaryData[] = rs.getBytes(colName);
//        InputStream binaryInput = new ByteArrayInputStream(binaryData);
//        if (null != binaryInput && binaryInput.available() != 0) {
//            ObjectInputStream in = new ObjectInputStream(binaryInput);
//            try {
//                obj = in.readObject();
//            } finally {
//                in.close();
//            }
//        }
//        return obj;
//    }

//    /**
//     * <p>
//     * This method should be overridden by any delegate subclasses that need
//     * special handling for BLOBs for job details. The default implementation
//     * uses standard JDBC <code>java.sql.Blob</code> operations.
//     * </p>
//     *
//     * @param rs
//     *          the result set, already queued to the correct row
//     * @param colName
//     *          the column name for the BLOB
//     * @return the deserialized Object from the ResultSet BLOB
//     * @throws ClassNotFoundException
//     *           if a class found during deserialization cannot be found
//     * @throws IOException
//     *           if deserialization causes an error
//     */
//    @Override
//    protected Object getJobDataFromBlob(ResultSet rs, String colName) throws ClassNotFoundException, IOException, SQLException {
//        //log.debug( "Getting Job details from blob in col " + colName );
//        if (canUseProperties()) {
//            byte data[] = rs.getBytes(colName);
//            if(data == null) {
//                return null;
//            }
//            InputStream binaryInput = new ByteArrayInputStream(data);
//            return binaryInput;
//        }
//        return getObjectFromBlob(rs, colName);
//    }

}

// EOF
