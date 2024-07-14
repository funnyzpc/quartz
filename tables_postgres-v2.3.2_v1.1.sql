
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
CREATE TABLE QRTZ_JOB_DETAILS
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  TRIGGER_NAME      VARCHAR(200) NOT NULL,
  DESCRIPTION       VARCHAR(250) NULL,
  JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
  IS_DURABLE        BOOL         NOT NULL,
  IS_NONCONCURRENT  BOOL         NOT NULL,
  IS_UPDATE_DATA    BOOL         NOT NULL,
  REQUESTS_RECOVERY BOOL         NOT NULL,
  JOB_DATA          BYTEA        NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME)
);

-- QRTZ_JOB_DETAILS
COMMENT ON COLUMN QRTZ_JOB_DETAILS.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.TRIGGER_NAME IS '集群中job的名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.DESCRIPTION IS '详细描述信息';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.JOB_CLASS_NAME IS '集群中job实现类的全名，quartz就是根据这个路径到classpath找到该job类';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.IS_DURABLE IS '是否持久化，把该属性设置为1，quartz会把job持久化到数据库中';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.IS_NONCONCURRENT IS '是否并发执行';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.IS_UPDATE_DATA IS '是否更新数据';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.REQUESTS_RECOVERY IS '是否接受恢复执行，默认为false，设置了RequestsRecovery为true，则该job会被重新执行';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.JOB_DATA IS '一个blob字段，存放持久化job对象';

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME);

INSERT INTO qrtz_job_details (sched_name,TRIGGER_NAME,description,job_class_name,is_durable,is_nonconcurrent,is_update_data,requests_recovery,job_data) VALUES
	 ('MEE_QUARTZ','QRTZ_AUTO_CHECK_JOB','系统定时检查，切勿删除此任务！','com.mee.quartz.job.QrtzAutoCheckJob',false,true,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C77080000001000000003740004646174617074000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000018923BFCEB2740003544944740010323330373035303935323030313030317800','hex')),
	 ('MEE_QUARTZ2','QRTZ_AUTO_CHECK_JOB','系统定时检查，切勿删除此任务！','com.mee.quartz.job.QrtzAutoCheckJob',false,true,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C77080000001000000003740004646174617074000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000018E74023F5B740003544944707800','hex')),
	 ('QUARTZ-SPRINGBOOT','Simple01TestService::execute::01',NULL,'com.mee.quartz.job.Simple01TestService',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C77080000001000000002740004646174617400027B7D74000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B020000787000000190822F818D7800','hex')),
	 ('QUARTZ-SPRINGBOOT','Job01TestService::execute::02','测试1720170703518','com.mee.quartz.job.Job01TestService',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000274000464617461740013E6B58BE8AF953137323031373037303335313874000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B0200007870000001908229EA9E7800','hex')),
	 ('MEE_QUARTZ','ATestJob::SIMPLE',NULL,'com.mee.quartz.job.ATestJob',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C77080000001000000003740004646174617074000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B0200007870000001909701DC99740003544944740010323430373034313734333134313032317800','hex')),
	 ('QUARTZ-SPRINGBOOT','Simple01TestService::execute::02',NULL,'com.mee.quartz.job.Simple01TestService',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C77080000001000000002740004646174617400027B7D74000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000019090BF871A7800','hex')),
	 ('MEE_QUARTZ','SimpleJob::SIMPLE',NULL,'com.mee.quartz.job.SimpleJob',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C77080000001000000003740004646174617074000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B0200007870000001909701DCA3740003544944740010323430373034313731373331313031377800','hex')),
	 ('QUARTZ-SPRINGBOOT','Job01TestService::execute::03','cron任务测试03','com.mee.quartz.job.Job01TestService',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000027400046461746174001263726F6EE4BBBBE58AA1E6B58BE8AF95303374000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000019091C6405F7800','hex')),
	 ('QUARTZ-SPRINGBOOT','Job02TestService::execute::01','测试1720492508249','com.mee.quartz.job.Job02TestService',false,false,false,false,decode('ACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000274000464617461740013E6B58BE8AF953137323034393235303832343974000B6372656174655F74696D657372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B020000787000000190955844597800','hex'));

-- select * from QRTZ_TRIGGERS ;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE QRTZ_TRIGGERS  CASCADE ;

CREATE TABLE QRTZ_TRIGGERS
(
  SCHED_NAME     VARCHAR(120) NOT NULL,
  TRIGGER_NAME   VARCHAR(200) NOT NULL,
  DESCRIPTION    VARCHAR(250) NULL,
  NEXT_FIRE_TIME BIGINT       NULL,
  PREV_FIRE_TIME BIGINT       NULL,
  PRIORITY       INTEGER      NULL,
  TRIGGER_STATE  VARCHAR(16)  NOT NULL,
  TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
  START_TIME     BIGINT       NOT NULL,
  END_TIME       BIGINT       NULL,
  CALENDAR_NAME  VARCHAR(200) NULL,
  MISFIRE_INSTR  SMALLINT     NULL,
  JOB_DATA       BYTEA        NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME)
  REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME,TRIGGER_NAME)
);


CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS (SCHED_NAME);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS (SCHED_NAME);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);

-- QRTZ_TRIGGERS
COMMENT ON TABLE QRTZ_TRIGGERS IS '存储已配置的Trigger的基本信息';
COMMENT ON COLUMN QRTZ_TRIGGERS.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_TRIGGERS.TRIGGER_NAME IS '触发器的名称,关联qrtz_job_details';
COMMENT ON COLUMN QRTZ_TRIGGERS.DESCRIPTION IS '详细描述信息';
COMMENT ON COLUMN QRTZ_TRIGGERS.NEXT_FIRE_TIME IS '下一次触发时间（毫秒），默认为-1，意味不会自动触发';
COMMENT ON COLUMN QRTZ_TRIGGERS.PREV_FIRE_TIME IS '上一次触发时间（毫秒）';
COMMENT ON COLUMN QRTZ_TRIGGERS.PRIORITY IS '优先级';
COMMENT ON COLUMN QRTZ_TRIGGERS.TRIGGER_STATE IS '当前触发器状态（ WAITING：等待； PAUSED：暂停； ACQUIRED：正常执行； BLOCKED：阻塞； ERROR：错误；）';
COMMENT ON COLUMN QRTZ_TRIGGERS.TRIGGER_TYPE IS '触发器的类型，使用cron表达式';
COMMENT ON COLUMN QRTZ_TRIGGERS.START_TIME IS '开始时间';
COMMENT ON COLUMN QRTZ_TRIGGERS.END_TIME IS '结束时间';
COMMENT ON COLUMN QRTZ_TRIGGERS.CALENDAR_NAME IS '日程表名称，表qrtz_calendars的CALENDAR_NAME字段的值';
COMMENT ON COLUMN QRTZ_TRIGGERS.MISFIRE_INSTR IS '措施或者是补偿执行的策略';
COMMENT ON COLUMN QRTZ_TRIGGERS.JOB_DATA IS '一个blob字段，存放持久化job对象';

INSERT INTO QRTZ_TRIGGERS (SCHED_NAME,TRIGGER_NAME,description,next_fire_time,prev_fire_time,priority,trigger_state,trigger_type,start_time,end_time,calendar_name,misfire_instr,job_data) VALUES
	 ('MEE_QUARTZ2','QRTZ_AUTO_CHECK_JOB','系统定时检查，切勿删除此任务！',1711343400000,-1,5,'WAITING','CRON',1711343288000,0,NULL,2,decode('','hex')),
	 ('MEE_QUARTZ','QRTZ_AUTO_CHECK_JOB','系统定时检查，切勿删除此任务！',1720520520000,1720520400000,5,'WAITING','CRON',1688521920000,0,NULL,2,decode('','hex')),
	 ('QUARTZ-SPRINGBOOT','Job02TestService::execute::01','测试1720492508249',1720520520000,1720520400000,5,'WAITING','CRON',1720492508000,0,NULL,2,decode('','hex')),
	 ('MEE_QUARTZ','SimpleJob::SIMPLE',NULL,1720520297141,-1,5,'WAITING','SIMPLE',1720520297141,0,NULL,0,decode('','hex')),
	 ('QUARTZ-SPRINGBOOT','Job01TestService::execute::03','cron任务测试03',1720520460000,1720520400000,5,'WAITING','CRON',1720432607000,0,NULL,2,decode('','hex')),
	 ('QUARTZ-SPRINGBOOT','Simple01TestService::execute::01','测试简单任务',1720520406175,1720520400175,5,'ACQUIRED','SIMPLE',1720510668175,0,'skip_date',0,decode('','hex')),
	 ('QUARTZ-SPRINGBOOT','Simple01TestService::execute::02',NULL,-1,1720418861990,5,'COMPLETE','SIMPLE',1720418911285,1720418989466,NULL,0,decode('','hex')),
	 ('QUARTZ-SPRINGBOOT','Job01TestService::execute::02','测试1720170703518',1720520460000,1720520400000,5,'WAITING','CRON',1720170703000,0,NULL,2,decode('','hex')),
	 ('MEE_QUARTZ','ATestJob::SIMPLE',NULL,1720520297138,-1,5,'WAITING','SIMPLE',1720520297138,0,NULL,0,decode('','hex'));


CREATE TABLE QRTZ_CRON_TRIGGERS
(
  SCHED_NAME      VARCHAR(120) NOT NULL,
  TRIGGER_NAME    VARCHAR(200) NOT NULL,
  CRON_EXPRESSION VARCHAR(120) NOT NULL,
  TIME_ZONE_ID    VARCHAR(80),
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME)
  REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME)
);
COMMENT ON TABLE QRTZ_CRON_TRIGGERS IS '存放Cron类型的Trigger，包括Cron表达式和时区信息';
-- QRTZ_CRON_TRIGGERS
COMMENT ON COLUMN QRTZ_CRON_TRIGGERS.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_CRON_TRIGGERS.TRIGGER_NAME IS '触发器的名称，qrtz_triggers表的TRIGGER_NAME的外键';
COMMENT ON COLUMN QRTZ_CRON_TRIGGERS.CRON_EXPRESSION IS 'cron表达式';
COMMENT ON COLUMN QRTZ_CRON_TRIGGERS.TIME_ZONE_ID IS '时区';
INSERT INTO qrtz_cron_triggers (sched_name,trigger_name,cron_expression,time_zone_id) VALUES
	 ('QUARTZ-SPRINGBOOT','Job01TestService::execute','0 * * * * ?','Asia/Shanghai'),
	 ('MEE_QUARTZ2','QRTZ_AUTO_CHECK_JOB','0 */2 * ? * *','Asia/Shanghai'),
	 ('MEE_QUARTZ','QRTZ_AUTO_CHECK_JOB','0 */2 * ? * *','Asia/Shanghai');



CREATE TABLE QRTZ_FIRED_TRIGGERS
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  ENTRY_ID          VARCHAR(95)  NOT NULL,
  TRIGGER_NAME      VARCHAR(200) NOT NULL,
  INSTANCE_NAME     VARCHAR(200) NOT NULL,
  FIRED_TIME        BIGINT       NOT NULL,
  SCHED_TIME        BIGINT       NOT NULL,
  PRIORITY          INTEGER      NOT NULL,
  STATE             VARCHAR(16)  NOT NULL,
  IS_NONCONCURRENT  BOOL         NULL,
  REQUESTS_RECOVERY BOOL         NULL,
  PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME);

-- QRTZ_FIRED_TRIGGERS
COMMENT ON TABLE QRTZ_FIRED_TRIGGERS IS '存储与已触发的Trigger相关的状态信息，以及相联Job的执行信息';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.ENTRY_ID IS '调度器实例id';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.TRIGGER_NAME IS '触发器的名称，qrtz_triggers表的TRIGGER_NAME的外键';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.INSTANCE_NAME IS '调度器实例名';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.FIRED_TIME IS '触发的时间';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.SCHED_TIME IS '定时器制定的时间';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.PRIORITY IS '优先级';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.STATE IS '状态';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.IS_NONCONCURRENT IS '是否并发';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.REQUESTS_RECOVERY IS '是否接受恢复执行，默认为false，设置了RequestsRecovery为true，则会被重新执行';

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
(
  SCHED_NAME      VARCHAR(120) NOT NULL,
  TRIGGER_NAME    VARCHAR(200) NOT NULL,
  REPEAT_COUNT    BIGINT       NOT NULL,
  REPEAT_INTERVAL BIGINT       NOT NULL,
  TIMES_TRIGGERED BIGINT       NOT NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME),
  FOREIGN KEY (SCHED_NAME, TRIGGER_NAME)
  REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME)
);
COMMENT ON TABLE QRTZ_SIMPLE_TRIGGERS IS '存储Simple类型的Trigger，包括重复次数、间隔、以及已触的次数';
INSERT INTO qrtz_simple_triggers (sched_name,trigger_name,repeat_count,repeat_interval,times_triggered) VALUES
	 ('MEE_QUARTZ','SimpleJob::SIMPLE',1001,30000,10),
	 ('MEE_QUARTZ','SimpleJob::SIMPLE_TEST',1001,30000,10),
	 ('MEE_QUARTZ','CTestJob::SIMPLE',1001,30000,10),
	 ('MEE_QUARTZ','ATestJob::SIMPLE',1000,30000,-1);


--------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------

CREATE TABLE QRTZ_JOB_DETAILS
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  JOB_NAME          VARCHAR(200) NOT NULL,
  DESCRIPTION       VARCHAR(250) NULL,
  JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
  IS_DURABLE        BOOL         NOT NULL,
  IS_NONCONCURRENT  BOOL         NOT NULL,
  IS_UPDATE_DATA    BOOL         NOT NULL,
  REQUESTS_RECOVERY BOOL         NOT NULL,
  JOB_DATA          BYTEA        NULL,
  PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS
(
  SCHED_NAME     VARCHAR(120) NOT NULL,
  TRIGGER_NAME   VARCHAR(200) NOT NULL,
  TRIGGER_GROUP  VARCHAR(200) NOT NULL,
  JOB_GROUP      VARCHAR(200) NOT NULL,
  DESCRIPTION    VARCHAR(250) NULL,
  NEXT_FIRE_TIME BIGINT       NULL,
  PREV_FIRE_TIME BIGINT       NULL,
  PRIORITY       INTEGER      NULL,
  TRIGGER_STATE  VARCHAR(16)  NOT NULL,
  TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
  START_TIME     BIGINT       NOT NULL,
  END_TIME       BIGINT       NULL,
  CALENDAR_NAME  VARCHAR(200) NULL,
  MISFIRE_INSTR  SMALLINT     NULL,
  JOB_DATA       BYTEA        NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
  REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_CALENDARS
(
  SCHED_NAME    VARCHAR(120) NOT NULL,
  CALENDAR_NAME VARCHAR(200) NOT NULL,
  CALENDAR      BYTEA        NOT NULL,
  PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);


CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
(
  SCHED_NAME    VARCHAR(120) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);


CREATE TABLE QRTZ_SCHEDULER_STATE
(
  SCHED_NAME        VARCHAR(120) NOT NULL,
  INSTANCE_NAME     VARCHAR(200) NOT NULL,
  LAST_CHECKIN_TIME BIGINT       NOT NULL,
  CHECKIN_INTERVAL  BIGINT       NOT NULL,
  PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS
(
  SCHED_NAME VARCHAR(120) NOT NULL,
  LOCK_NAME  VARCHAR(40)  NOT NULL,
  PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);



COMMENT ON TABLE QRTZ_CALENDARS IS '存储Quartz日历信息';
COMMENT ON TABLE QRTZ_PAUSED_TRIGGER_GRPS IS '存储已暂停的Trigger组的信息';
COMMENT ON TABLE QRTZ_SCHEDULER_STATE IS '存储少量的Scheduler相关的状态信息';
COMMENT ON TABLE QRTZ_LOCKS IS '存储锁信息，为多个节点调度提供分布式锁，实现分布式调度，默认有2个锁: STATE_ACCESS, TRIGGER_ACCESS';
COMMENT ON TABLE QRTZ_JOB_DETAILS IS '存储每一个已配置的JobDetail信息';


-- QRTZ_SCHEDULER_STATE
COMMENT ON COLUMN QRTZ_SCHEDULER_STATE.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_SCHEDULER_STATE.INSTANCE_NAME IS '调度实例id，配置文件中org.quartz.scheduler.instanceId配置的名字，就会写入该字段';
COMMENT ON COLUMN QRTZ_SCHEDULER_STATE.LAST_CHECKIN_TIME IS '上次检查时间';
COMMENT ON COLUMN QRTZ_SCHEDULER_STATE.CHECKIN_INTERVAL IS '检查间隔时间';

-- QRTZ_LOCKS
COMMENT ON COLUMN QRTZ_LOCKS.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_LOCKS.LOCK_NAME IS '悲观锁名称';

-- QRTZ_JOB_DETAILS
COMMENT ON COLUMN QRTZ_JOB_DETAILS.SCHED_NAME IS '调度名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.JOB_NAME IS '集群中job的名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.DESCRIPTION IS '详细描述信息';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.JOB_CLASS_NAME IS '集群中job实现类的全名，quartz就是根据这个路径到classpath找到该job类';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.IS_DURABLE IS '是否持久化，把该属性设置为1，quartz会把job持久化到数据库中';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.IS_NONCONCURRENT IS '是否并发执行';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.IS_UPDATE_DATA IS '是否更新数据';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.REQUESTS_RECOVERY IS '是否接受恢复执行，默认为false，设置了RequestsRecovery为true，则该job会被重新执行';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.JOB_DATA IS '一个blob字段，存放持久化job对象';


COMMIT;
