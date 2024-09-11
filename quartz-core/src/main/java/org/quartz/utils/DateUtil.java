package org.quartz.utils;


import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
* 日期时间工具类
* @className    DateUtil
* @author       shadow
* @date         2023/6/2 17:09
* @version      1.0
*/
public class DateUtil {

    // 格式化样式
    public static final DateTimeFormatter FORMAT_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FORMAT_DAY2 = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter FORMAT_DAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FORMAT_DAY_TIME2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final ZoneId zoneId = ZoneId.of("Asia/Shanghai");


    /**
     * 获取服务器启动时间
     */
    public static LocalDateTime getServerStartDate() {
        long timestamp = ManagementFactory.getRuntimeMXBean().getStartTime();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),zoneId);
    }

    public static String N(){
        return LocalDateTime.now(zoneId).format(FORMAT_DAY_TIME2);
    }

}
