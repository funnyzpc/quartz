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

import java.sql.Connection;
import java.util.List;

/**
 * <p>
 * This is the base interface for all driver delegate classes.
 * 这是所有驱动程序委派类的基本接口。
 * </p>
 * 
 * <p>
 * This interface is very similar to the <code>{@link
 * org.quartz.spi.JobStore}</code>
 * interface except each method has an additional <code>{@link Connection}</code>
 * parameter.
 * 这个接口与org.quartz.spi非常相似。除了每个方法外，JobStore接口都有一个额外的Connection参数。
 * </p>
 * 
 * <p>
 * Unless a database driver has some <strong>extremely-DB-specific</strong>
 * requirements, any DriverDelegate implementation classes should extend the
 * <code>{@link StdJDBCDelegate}</code> class.
 * 除非数据库驱动程序有一些非常特定于数据库的要求，否则任何DriverDelegate实现类都应该扩展StdJDBCDelegate类。
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 */
public interface DriverDelegate {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    /**
     * @param initString of the format: settingName=settingValue|otherSettingName=otherSettingValue|...
     * @throws NoSuchDelegateException
     */
    void initialize(String tablePrefix, String schedName,boolean useProperties) ;

    String[] getDBInfo(Connection conn);

    /*********** 任务操作 ***************/
    // 获取所有应用(不含节点)
    List<QrtzApp> getAllApp(Connection conn);
    QrtzApp getAppByApplication(Connection conn,String application);
    // 根据应用查询应用下所有节点
    List<QrtzNode> getNodeByApp(Connection conn,String application);
    // 根据job_id获取job信息
    QrtzJob getJobByJobId(Connection conn,String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzExecute getExecuteByExecuteId(Connection conn,String execute_id);
    List<QrtzExecute> getExecuteByJobId(Connection conn,String job_id);
    // 根据job_id获取job下所有execute信息
    QrtzJob getJobInAllByJobId(Connection conn,String job_id);
    // 根据execute_id获取execute及job信息
    QrtzExecute getExecuteInAllByExecuteId(Connection conn,String execute_id);

    // 添加应用
    int addApp(Connection conn,QrtzApp qrtzApp);
    // 删除应用
    int deleteApp(Connection conn,String application);
    // 暂停/启动应用
    int updateAppState(Connection conn,String application,String state);

    // 添加节点
    int addNode(Connection conn,QrtzNode qrtzNode);
    boolean containsNode(Connection conn,String application ,String hostIP);
    boolean containsNode(Connection conn,String application);
    // 删除节点
    int deleteNode(Connection conn,String application,String hostIP);
    // 暂停节点
    int updateNodeState(Connection conn,QrtzNode qrtzNode);
    int updateNode(Connection conn,QrtzNode qrtzNode);
    int updateNodeStateBatch(Connection conn,String application,String state);

    // 添加应用及节点
    int addAppAndNode(Connection conn,QrtzApp qrtzApp, QrtzNode qrtzNode);

    int addJob(Connection conn, QrtzJob qrtzJob);
    int updateJob(Connection conn, QrtzJob qrtzJob);
    int deleteJob(Connection conn,String job_id);
    // 暂停指定job下的所有execute
    int updateJobState(Connection conn,String job_id, String state);
    // 暂停指定execute
    int updateExecuteState(Connection conn, String execute_id, String state);
    // 添加execute
    int addExecute(Connection conn,QrtzExecute qrtzExecute);
    // 删除execute
    int deleteExecute(Connection conn,String execute_id );
//    int findQrtzExecuteCountById(Connection conn, Long job_id);
    // 是否存在execute
    boolean containsExecute(Connection conn,String job_id);
    // 更新执行项
    int updateExecute(Connection conn, QrtzExecute qrtzExecute);

}

// EOF
