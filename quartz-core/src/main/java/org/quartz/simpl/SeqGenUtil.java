package org.quartz.simpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shadow
 * @description 序列生成器
 */
public final class SeqGenUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SeqGenUtil.class);
    private static final AtomicInteger IT = new AtomicInteger(1000);
    private static final DateTimeFormatter DATE_SHORT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId zoneId = ZoneId.of("Asia/Shanghai");


    /** 生成主键：14(日期时间)+4(有序序列) **/
    public static synchronized String genSeq(){
        LocalDateTime dataTime = LocalDateTime.now(zoneId);
        if(IT.intValue()>9990){
            LOG.info("重置genSeq序列 1000");
            IT.getAndSet(1000);
        }
        return dataTime.format(DATE_SHORT_FORMAT)+ IT.getAndIncrement();
    }

    public static Long shortKey(){
        return Long.parseLong(genSeq());
    }



}
