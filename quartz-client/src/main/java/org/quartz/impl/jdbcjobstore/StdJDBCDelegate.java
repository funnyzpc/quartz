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

import org.quartz.impl.QrtzApp;
import org.quartz.impl.QrtzExecute;
import org.quartz.impl.QrtzJob;
import org.quartz.impl.QrtzNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This is meant to be an abstract base class for most, if not all, <code>{@link DriverDelegate}</code>
 * implementations. Subclasses should override only those methods that need
 * special handling for the DBMS driver in question.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 * @author Eric Mueller
 */
public class StdJDBCDelegate implements DriverDelegate, StdJDBCConstants {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StdJDBCDelegate.class);

    protected String tablePrefix = "QRTZ_";

    protected String schedName;

    protected boolean useProperties;


    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create new StdJDBCDelegate instance.
     * </p>
     */
    public StdJDBCDelegate() {
    }

    /**
     * @param initString of the format: settingName=settingValue|otherSettingName=otherSettingValue|...
     * @throws NoSuchDelegateException
     */
    @Override
    public void initialize( String tablePrefix, String schedName,boolean useProperties){
        this.tablePrefix = tablePrefix;
        this.schedName = schedName;
        this.useProperties = useProperties;
    }

    protected boolean canUseProperties() {
        return useProperties;
    }

//    // new String[]{STATE_WAITING,STATE_ACQUIRED,STATE_EXECUTING,STATE_BLOCKED,STATE_ERROR} to 'WAITING','ACQUIRED','EXECUTING','BLOCKED','ERROR'
//    private String buildArrayStr(String[] arr){
//        StringJoiner sj = new StringJoiner(",");
//        for(String item:arr){
//            sj.add("'"+item+"'");
//        }
//        return sj.toString();
//    }

    //---------------------------------------------------------------------------
    // protected methods that can be overridden by subclasses
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Replace the table prefix in a query by replacing any occurrences of
     * "{0}" with the table prefix.
     * </p>
     * 
     * @param query
     *          the unsubstitued query
     * @return the query, with proper table prefix substituted
     */
    public final String rtp(String query) {
        return Util.rtp(query, tablePrefix, getSchedulerNameLiteral());
    }
    private String schedNameLiteral = null;
    protected String getSchedulerNameLiteral() {
        if(schedNameLiteral == null){
            schedNameLiteral = "'" + schedName + "'";
        }
        return schedNameLiteral;
    }
//
//    /**
//     * <p>
//     * Create a serialized <code>java.util.ByteArrayOutputStream</code>
//     * version of an Object.
//     * </p>
//     *
//     * @param obj
//     *          the object to serialize
//     * @return the serialized ByteArrayOutputStream
//     * @throws IOException
//     *           if serialization causes an error
//     */
//    protected ByteArrayOutputStream serializeObject(Object obj) throws IOException {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        if (null != obj) {
//            ObjectOutputStream out = new ObjectOutputStream(baos);
//            out.writeObject(obj);
//            out.flush();
//        }
//        return baos;
//    }

//    /**
//     * Find the key of the first non-serializable value in the given Map.
//     *
//     * @return The key of the first non-serializable value in the given Map or
//     * null if all values are serializable.
//     */
//    protected Object getKeyOfNonSerializableValue(Map<?, ?> data) {
//        for (Iterator<?> entryIter = data.entrySet().iterator(); entryIter.hasNext();) {
//            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entryIter.next();
//            ByteArrayOutputStream baos = null;
//            try {
//                baos = serializeObject(entry.getValue());
//            } catch (IOException e) {
//                return entry.getKey();
//            } finally {
//                if (baos != null) {
//                    try { baos.close(); } catch (IOException ignore) {}
//                }
//            }
//        }
//
//        // As long as it is true that the Map was not serializable, we should
//        // not hit this case.
//        return null;
//    }
//
//    /**
//     * convert the JobDataMap into a list of properties
//     */
//    protected Properties convertToProperty(Map<?, ?> data) throws IOException {
//        Properties properties = new Properties();
//        for (Iterator<?> entryIter = data.entrySet().iterator(); entryIter.hasNext();) {
//            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entryIter.next();
//            Object key = entry.getKey();
//            Object val = (entry.getValue() == null) ? "" : entry.getValue();
//            if(!(key instanceof String)) {
//                throw new IOException("JobDataMap keys/values must be Strings "
//                        + "when the 'useProperties' property is set. "
//                        + " offending Key: " + key);
//            }
//            if(!(val instanceof String)) {
//                throw new IOException("JobDataMap values must be Strings "
//                        + "when the 'useProperties' property is set. "
//                        + " Key of offending value: " + key);
//            }
//            properties.put(key, val);
//        }
//        return properties;
//    }

    /**
     * Cleanup helper method that closes the given <code>ResultSet</code>
     * while ignoring any errors.
     */
    protected static void closeResultSet(ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    /**
     * Cleanup helper method that closes the given <code>Statement</code>
     * while ignoring any errors.
     */
    protected static void closeStatement(Statement statement) {
        if (null != statement) {
            try {
                statement.close();
            } catch (SQLException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    @Override
    public String[] getDBInfo(Connection conn) {
        try {
            String databaseProductName = conn.getMetaData().getDatabaseProductName();
            String schema = conn.getSchema();
            return new String[]{databaseProductName,schema};
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",e);
        }
        return null;
    }

    @Override
    public List<QrtzApp> getAllApp(Connection conn){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzApp> result = new ArrayList<>(8);
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}APP ORDER BY TIME_NEXT"));
            rs = ps.executeQuery();
            while (rs.next()) {
                String application = rs.getString("APPLICATION");
                String state = rs.getString("STATE");
                Long time_pre = rs.getLong("TIME_PRE");
                Long time_next = rs.getLong("TIME_NEXT");
                Long time_interval = rs.getLong("TIME_INTERVAL");
                result.add(new QrtzApp(application,state,time_pre,time_next,time_interval));
            }
        } catch (Exception e) {
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        } finally {
            closeResultSet(rs);
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return result;
    }
    @Override
    public QrtzApp getAppByApplication(Connection conn,String application){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}APP WHERE APPLICATION=? "));
            ps.setString(1,application);
            rs = ps.executeQuery();
            while (rs.next()) {
                String application_ = rs.getString("APPLICATION");
                String state = rs.getString("STATE");
                Long time_pre = rs.getLong("TIME_PRE");
                Long time_next = rs.getLong("TIME_NEXT");
                Long time_interval = rs.getLong("TIME_INTERVAL");
                return new QrtzApp(application_,state,time_pre,time_next,time_interval);
            }
        } catch (Exception e) {
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        } finally {
            closeResultSet(rs);
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return null;
    }
    @Override
    public List<QrtzNode> getNodeByApp(Connection conn,String application){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzNode> result = new ArrayList<>(8);
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}NODE WHERE APPLICATION=? "));
            ps.setString(1,application);
            rs = ps.executeQuery();
            while (rs.next()) {
                String application_ = rs.getString("APPLICATION");
                String host_ip = rs.getString("HOST_IP");
                String host_name = rs.getString("HOST_NAME");
                String state = rs.getString("STATE");
                Long time_check = rs.getLong("TIME_CHECK");
                result.add(new QrtzNode(application_,host_ip,host_name,state,time_check));
            }
        } catch (Exception e) {
            e.printStackTrace();
//            throw new SQLException("No misfired trigger count returned.");
        } finally {
            closeResultSet(rs);
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return result;
    }
    @Override
    public QrtzJob getJobByJobId(Connection conn,String job_id){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}JOB WHERE ID = ? "));
            ps.setBigDecimal(1,new BigDecimal(job_id));
            rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("ID");
                String application = rs.getString("APPLICATION");
                String state = rs.getString("STATE");
                String job_class = rs.getString("JOB_CLASS");
                String job_data = rs.getString("JOB_DATA");
                String job_description = rs.getString("JOB_DESCRIPTION");
                Long update_time = rs.getLong("UPDATE_TIME");
                return new QrtzJob(id,application,state,job_class,job_data,job_description,update_time);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return null;
    }
    @Override
    public QrtzExecute getExecuteByExecuteId(Connection conn,String execute_id){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzExecute> result = new ArrayList<>(4);
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}EXECUTE WHERE ID=? "));
            ps.setBigDecimal(1,new BigDecimal(execute_id));
            rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("ID");
                String pid = rs.getString("PID");
                String job_type = rs.getString("JOB_TYPE");
                String state = rs.getString("STATE");
                String cron = rs.getString("CRON");
                String zone_id = rs.getString("ZONE_ID");
                Integer repeat_count = rs.getInt("REPEAT_COUNT");
                Integer repeat_interval = rs.getInt("REPEAT_INTERVAL");
                Integer time_triggered = rs.getInt("TIME_TRIGGERED");
                Long prev_fire_time = rs.getLong("PREV_FIRE_TIME");
                Long next_fire_time = rs.getLong("NEXT_FIRE_TIME");
                String host_ip = rs.getString("HOST_IP");
                String host_name = rs.getString("HOST_NAME");
                Long start_time = rs.getLong("START_TIME");
                Long end_time = rs.getLong("END_TIME");
                return  QrtzExecute.build(id,pid,job_type,state,cron,zone_id,repeat_count,repeat_interval,time_triggered,prev_fire_time,next_fire_time,host_ip,host_name,start_time,end_time);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return null;
    }
    @Override
    public List<QrtzExecute> getExecuteByJobId(Connection conn,String job_id){
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<QrtzExecute> result = new ArrayList<>(4);
        try {
            ps = conn.prepareStatement(rtp("SELECT * FROM {0}EXECUTE WHERE PID=? "));
            ps.setBigDecimal(1,new BigDecimal(job_id));
            rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("ID");
                String pid = rs.getString("PID");
                String job_type = rs.getString("JOB_TYPE");
                String state = rs.getString("STATE");
                String cron = rs.getString("CRON");
                String zone_id = rs.getString("ZONE_ID");
                Integer repeat_count = rs.getInt("REPEAT_COUNT");
                Integer repeat_interval = rs.getInt("REPEAT_INTERVAL");
                Integer time_triggered = rs.getInt("TIME_TRIGGERED");
                Long prev_fire_time = rs.getLong("PREV_FIRE_TIME");
                Long next_fire_time = rs.getLong("NEXT_FIRE_TIME");
                String host_ip = rs.getString("HOST_IP");
                String host_name = rs.getString("HOST_NAME");
                Long start_time = rs.getLong("START_TIME");
                Long end_time = rs.getLong("END_TIME");
                result.add( QrtzExecute.build(id,pid,job_type,state,cron,zone_id,repeat_count,repeat_interval,time_triggered,prev_fire_time,next_fire_time,host_ip,host_name,start_time,end_time));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            // 由于实在同一个connection下，所以不可关闭Connection
            closeStatement(ps);
        }
        return result;
    }
    @Override
    public QrtzJob getJobInAllByJobId(Connection conn,String job_id){
        QrtzJob qrtzJob = this.getJobByJobId(conn, job_id);
        if(null==qrtzJob){
            LOGGER.error("未能获取到指定数据或数据为空 job_id:{}",job_id);
            return null;
        }
        List<QrtzExecute> qrtzExecutes = this.getExecuteByJobId(conn, job_id);
        return qrtzJob.setExecutes(qrtzExecutes);
    }
    @Override
    public QrtzExecute getExecuteInAllByExecuteId(Connection conn,String execute_id){
        QrtzExecute qrtzExecute = this.getExecuteByExecuteId(conn, execute_id);
        if(null==qrtzExecute){
            LOGGER.error("未能获取到指定数据或数据为空 execute_id:{}",execute_id);
            return null;
        }
        final String pid = qrtzExecute.getPid();
        return qrtzExecute.setJob(this.getJobByJobId(conn,pid));
    }
    @Override
    public int addApp(Connection conn,QrtzApp qrtzApp){
        String state;
        if(null==(state=qrtzApp.getState()) || (!"N".equals(state) && !"Y".equals(state))){
            LOGGER.error("异常的状态项:state=>{}",qrtzApp);
            return 0;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("INSERT INTO {0}APP (APPLICATION,STATE,TIME_PRE,TIME_NEXT,TIME_INTERVAL) VALUES (?,?,?,?,?) "));
            ps.setString(1, qrtzApp.getApplication());
            ps.setString(2, qrtzApp.getState());
            ps.setObject(3, qrtzApp.getTimePre());
            ps.setObject(4, qrtzApp.getTimeNext());
            ps.setLong(5, qrtzApp.getTimeInterval());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",qrtzApp,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int deleteApp(Connection conn,String application){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("DELETE FROM {0}APP WHERE APPLICATION=? "));
            ps.setString(1,application);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",application,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int updateAppState(Connection conn,String application,String state){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}APP SET STATE=? WHERE APPLICATION=? "));
            ps.setString(1,state);
            ps.setString(2,application);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",application,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int addNode(Connection conn,QrtzNode qrtzNode){
        String state;
        if(null==(state=qrtzNode.getState()) || (!"N".equals(state) && !"Y".equals(state))){
            LOGGER.error("异常的状态项:state=>{}",qrtzNode);
            return 0;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("INSERT INTO {0}NODE (APPLICATION,HOST_IP,HOST_NAME,STATE,TIME_CHECK) VALUES (?,?,?,?,?)"));
            ps.setString(1, qrtzNode.getApplication());
            ps.setString(2, qrtzNode.getHostIp());
            ps.setString(3, qrtzNode.getHostName());
            ps.setString(4, qrtzNode.getState());
            ps.setObject(5, qrtzNode.getTimeCheck());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",qrtzNode,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public boolean containsNode(Connection conn,String application ,String hostIP){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp("SELECT APPLICATION,HOST_IP FROM {0}NODE WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setString(1,application);
            ps.setString(2,hostIP);
            rs = ps.executeQuery();
            while (rs.next()) {
                return Boolean.TRUE;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",application,hostIP,e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
        return Boolean.FALSE;
    }
    @Override
    public boolean containsNode(Connection conn,String application){
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp("SELECT APPLICATION,HOST_IP FROM {0}NODE WHERE APPLICATION=?"));
            ps.setString(1,application);
            rs = ps.executeQuery();
            while (rs.next()) {
                return Boolean.TRUE;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",application,e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
        return Boolean.FALSE;
    }
    @Override
    public int deleteNode(Connection conn,String application,String hostIP){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("DELETE FROM {0}NODE WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setString(1,application);
            ps.setString(2,hostIP);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",application,hostIP,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int updateNodeState(Connection conn,QrtzNode qrtzNode){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}NODE SET STATE=? WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setString(1,qrtzNode.getState());
            ps.setString(2,qrtzNode.getApplication());
            ps.setString(3,qrtzNode.getHostIp());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",qrtzNode,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int updateNode(Connection conn,QrtzNode qrtzNode){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}NODE SET STATE=?,HOST_NAME=? WHERE APPLICATION=? AND HOST_IP=? "));
            ps.setString(1,qrtzNode.getState());
            ps.setString(2,qrtzNode.getHostName());
            ps.setString(3,qrtzNode.getApplication());
            ps.setString(4,qrtzNode.getHostIp());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",qrtzNode,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int updateNodeStateBatch(Connection conn,String application,String state){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}NODE SET STATE=? WHERE APPLICATION=? AND STATE!=? "));
            ps.setString(1,state);
            ps.setString(2,application);
            ps.setString(3,state);
            ps.executeUpdate();
            // 如果状态即为目标状态则可能更新记录就是0，这里只关注更新操作本身
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",state,application,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int addAppAndNode(Connection conn,QrtzApp qrtzApp, QrtzNode qrtzNode){
        if(this.getAppByApplication(conn,qrtzApp.getApplication())!=null || this.addApp(conn, qrtzApp)<1){
            LOGGER.error("未能添加app记录：{}",qrtzApp);
            return 0;
        }
        return this.addNode(conn,qrtzNode);
    }

    /**
     * <p>
     * Select a trigger' state value.
     * </p>
     *
     * @param conn
     *          the DB Connection
     * @return the <code>{@link org.quartz.Trigger}</code> object
     */
    @Override
    public int addJob(Connection conn, QrtzJob qrtzJob) {
        String state;
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(null==(state=qrtzJob.getState()) || !states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{}",qrtzJob);
            return 0;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("INSERT INTO {0}JOB (ID,APPLICATION,STATE,JOB_CLASS,JOB_DATA,JOB_DESCRIPTION,UPDATE_TIME) VALUES (?,?,?,?,?,?,?)"));
            ps.setBigDecimal(1,new BigDecimal(qrtzJob.getId()));
            ps.setString(2, qrtzJob.getApplication());
            ps.setString(3, qrtzJob.getState());
            ps.setString(4, qrtzJob.getJobClass());
            ps.setString(5, qrtzJob.getJobData());
            ps.setString(6, qrtzJob.getJobDescription());
            ps.setBigDecimal(7, new BigDecimal(qrtzJob.getUpdateTime()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",qrtzJob,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }

    @Override
    public int updateJob(Connection conn, QrtzJob qrtzJob) {
        PreparedStatement ps = null;
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(null!=qrtzJob.getState() && !states.contains(","+qrtzJob.getState()+",") ){
            LOGGER.error("异常的状态项:{}",qrtzJob);
            return 0;
        }
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}JOB SET APPLICATION =?,STATE=?,JOB_CLASS =?,JOB_DATA=?,JOB_DESCRIPTION=?,UPDATE_TIME=? WHERE ID=?"));
            ps.setString(1,qrtzJob.getApplication());
            ps.setString(2, qrtzJob.getState());
            ps.setString(3, qrtzJob.getJobClass());
            ps.setString(4, qrtzJob.getJobData());
            ps.setString(5, qrtzJob.getJobDescription());
            ps.setBigDecimal(6,new BigDecimal(qrtzJob.getUpdateTime()));
            ps.setBigDecimal(7,new BigDecimal(qrtzJob.getId()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",qrtzJob,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }

    @Override
    public int deleteJob(Connection conn,String job_id) {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("DELETE FROM {0}JOB WHERE ID=?"));
            ps.setBigDecimal(1,new BigDecimal(job_id));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",job_id,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int updateJobState(Connection conn,String job_id, String state){
        // 只有写入执行项时才是INIT，其他任何状态都不可以变更为INIT
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,";
        if(null==state || !states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{},{}",job_id,state);
            return 0;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}JOB SET STATE=? WHERE ID=?"));
            ps.setString(1,state);
            ps.setBigDecimal(2,new BigDecimal(job_id));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",job_id,state,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int updateExecuteState(Connection conn, String execute_id, String state){
        // 只有写入执行项时才是INIT，其他任何状态都不可以变更为INIT
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,";
        if(null==state || !states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{},{}",execute_id,state);
            return 0;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("UPDATE {0}EXECUTE SET STATE=? WHERE ID=?"));
            ps.setString(1,state);
            ps.setBigDecimal(2,new BigDecimal(execute_id));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",execute_id,state,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }
    @Override
    public int addExecute(Connection conn,QrtzExecute qrtzExecute){
        String state;
        final String states = ",EXECUTING,PAUSED,COMPLETE,ERROR,INIT,";
        if(null==(state=qrtzExecute.getState()) || !states.contains(","+state+",") ){
            LOGGER.error("异常的状态项:state=>{}",qrtzExecute);
            return 0;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("INSERT INTO {0}EXECUTE (ID,PID,JOB_TYPE,STATE,CRON,ZONE_ID,REPEAT_COUNT,REPEAT_INTERVAL,TIME_TRIGGERED,PREV_FIRE_TIME,NEXT_FIRE_TIME,HOST_IP,HOST_NAME,START_TIME,END_TIME) VALUES\n" +
                    "\t (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));
            ps.setBigDecimal(1,new BigDecimal(qrtzExecute.getId())); // ID
            ps.setBigDecimal(2, new BigDecimal(qrtzExecute.getPid())); // PID
            ps.setString(3, qrtzExecute.getJobType()); // JOB_TYPE
            ps.setString(4, qrtzExecute.getState()); // STATE
            ps.setString(5, qrtzExecute.getCron()); // CRON
            ps.setString(6, qrtzExecute.getZoneId()); // ZONE_ID
            ps.setObject(7, qrtzExecute.getRepeatCount()); // REPEAT_COUNT
            ps.setObject(8, qrtzExecute.getRepeatInterval()); // REPEAT_INTERVAL
            ps.setObject(9, qrtzExecute.getTimeTriggered()); // TIME_TRIGGERED
            ps.setObject(10, qrtzExecute.getPrevFireTime()); // PREV_FIRE_TIME
            ps.setObject(11, qrtzExecute.getNextFireTime()); // NEXT_FIRE_TIME
            ps.setString(12, qrtzExecute.getHostIp()); // HOST_IP
            ps.setString(13, qrtzExecute.getHostName()); // HOST_NAME
            ps.setObject(14, qrtzExecute.getStartTime()); // START_TIME
            ps.setObject(15, qrtzExecute.getEndTime()); // END_TIME
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",qrtzExecute,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }

    @Override
    public int deleteExecute(Connection conn,String execute_id ){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp("DELETE FROM {0}EXECUTE WHERE ID=? "));
            ps.setBigDecimal(1,new BigDecimal(execute_id));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",execute_id,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }

//    @Override
//    public int findQrtzExecuteCountById(Connection conn,Long job_id) {
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            ps = conn.prepareStatement(rtp("SELECT COUNT(1)  FROM {0}EXECUTE WHERE pid=? "));
//            ps.setBigDecimal(1,new BigDecimal(job_id));
//            rs = ps.executeQuery();
//            while (rs.next()) {
//                return rs.getInt(1);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            LOGGER.error("异常:{}",job_id,e);
//        } finally {
//            closeResultSet(rs);
//            closeStatement(ps);
//        }
//        return 0;
//    }

    @Override
    public boolean containsExecute(Connection conn,String job_id) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp("SELECT ID  FROM {0}EXECUTE WHERE PID=? "));
            ps.setBigDecimal(1,new BigDecimal(job_id));
            rs = ps.executeQuery();
            while (rs.next()) {
                return Boolean.TRUE;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{}",job_id,e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
        return Boolean.FALSE;
    }
    @Override
    public int updateExecute(Connection conn, QrtzExecute qrtzExecute){
        PreparedStatement ps = null;
        final String jobType = qrtzExecute.getJobType();
        try {
            if("CRON".equals(jobType)){
                ps = conn.prepareStatement(rtp("UPDATE {0}EXECUTE SET STATE=?,CRON=?,ZONE_ID=?,PREV_FIRE_TIME=?,NEXT_FIRE_TIME =?,HOST_IP =?,HOST_NAME =?,START_TIME =?,END_TIME =?  WHERE ID = ? AND PID=?"));
                ps.setString(1,qrtzExecute.getState()); // state=?
                ps.setString(2,qrtzExecute.getCron()); // cron=?
                ps.setString(3,qrtzExecute.getZoneId()); // zone_id=?
                ps.setLong(4,qrtzExecute.getPrevFireTime()); // prev_fire_time=?
                ps.setLong(5,qrtzExecute.getNextFireTime()); // next_fire_time =?
                ps.setString(6,qrtzExecute.getHostIp()); // host_ip =?
                ps.setString(7,qrtzExecute.getHostName()); // host_name =?
                ps.setLong(8,qrtzExecute.getStartTime()); // start_time =?
                ps.setLong(9,qrtzExecute.getEndTime()); // end_time =?
                ps.setBigDecimal(10,new BigDecimal(qrtzExecute.getId())); // where  id = ?
                ps.setBigDecimal(11,new BigDecimal(qrtzExecute.getPid())); // where pid=?
            }else if("SIMPLE".equals(jobType)){
                ps = conn.prepareStatement(rtp("UPDATE {0}EXECUTE SET STATE=?,REPEAT_COUNT =?,REPEAT_INTERVAL =?,PREV_FIRE_TIME=?,NEXT_FIRE_TIME =?,HOST_IP =?,HOST_NAME =?,START_TIME =?,END_TIME =?  WHERE ID = ? AND PID=?"));
                ps.setString(1,qrtzExecute.getState()); // state=?
                ps.setInt(2,qrtzExecute.getRepeatCount()); // repeat_count =?
                ps.setInt(3,qrtzExecute.getRepeatInterval()); // repeat_interval =?
                ps.setLong(4,qrtzExecute.getPrevFireTime()); // prev_fire_time=?
                ps.setLong(5,qrtzExecute.getNextFireTime()); // next_fire_time =?
                ps.setString(6,qrtzExecute.getHostIp()); // host_ip =?
                ps.setString(7,qrtzExecute.getHostName()); // host_name =?
                ps.setLong(8,qrtzExecute.getStartTime()); // start_time =?
                ps.setLong(9,qrtzExecute.getEndTime()); // end_time =?
                ps.setBigDecimal(10,new BigDecimal(qrtzExecute.getId())); // where  id = ?
                ps.setBigDecimal(11,new BigDecimal(qrtzExecute.getPid())); // where pid=?
            }else{
                LOGGER.error("不支持的job_type:{}",jobType);
                return 0;
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.error("异常:{},{}",qrtzExecute,e);
        } finally {
            closeStatement(ps);
        }
        return 0;
    }


}

// EOF
