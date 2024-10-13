package com.quartz.client.api.impl;

import com.quartz.client.api.MyQrtzClientService;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * MyQrtzClientServiceImpl
 *
 * @author shaoow
 * @version 1.0
 * @className MyQrtzClientServiceImpl
 * @date 2024/9/27 10:30
 */
@Service
public class MyQrtzClientServiceImpl implements MyQrtzClientService {


    private final DataSource dataSource;

    public MyQrtzClientServiceImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

//    @Scheduled(cron = "0 0/5 * * * ?")
//    public void test01(){
//        System.out.println("hello...");
//    }



}
