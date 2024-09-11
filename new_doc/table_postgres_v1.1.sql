
-- QRTZ_APP
DROP TABLE IF EXISTS QRTZ_APP;

CREATE TABLE QRTZ_APP(
  APPLICATION VARCHAR(50),
  STATE VARCHAR(10) NOT NULL,
  TIME_PRE  int8,
  TIME_NEXT int8 NOT NULL,
  TIME_INTERVAL int8,
  PRIMARY KEY (APPLICATION)
);

COMMENT ON TABLE QRTZ_APP IS '定时任务::应用表';
COMMENT ON COLUMN QRTZ_APP.APPLICATION IS '调度名称/应用名称';
COMMENT ON COLUMN QRTZ_APP.STATE IS '状态 N.停止/不可用  Y.开启/可用';
COMMENT ON COLUMN QRTZ_APP.TIME_PRE IS '上一次check时间';
COMMENT ON COLUMN QRTZ_APP.TIME_NEXT IS '下一次check时间';
COMMENT ON COLUMN QRTZ_APP.TIME_INTERVAL IS 'check的检查间隔(毫秒)';

-- QRTZ_NODE
DROP TABLE IF EXISTS QRTZ_NODE;
CREATE TABLE QRTZ_NODE(
  APPLICATION VARCHAR(50),
  HOST_IP VARCHAR(50),
  HOST_NAME VARCHAR(80),
  STATE CHAR(1) NOT NULL,
  TIME_CHECK  INT8,
  PRIMARY KEY (APPLICATION,HOST_IP)
);

COMMENT ON TABLE QRTZ_NODE IS '定时任务::节点实例表';
COMMENT ON COLUMN QRTZ_NODE.APPLICATION IS '调度名称/应用名称';
COMMENT ON COLUMN QRTZ_NODE.HOST_IP IS '实例机器IP';
COMMENT ON COLUMN QRTZ_NODE.HOST_NAME IS '实例机器名称';
COMMENT ON COLUMN QRTZ_NODE.STATE IS '状态 N.停止/不可用  Y.开启/可用';
COMMENT ON COLUMN QRTZ_NODE.TIME_CHECK IS '检查时间';

-- QRTZ_JOB
DROP TABLE IF EXISTS QRTZ_JOB;
CREATE TABLE QRTZ_JOB(
  ID INT8,
  APPLICATION VARCHAR(50),
  STATE VARCHAR(10),
  JOB_IDX INT4,
  JOB_CLASS VARCHAR(127),
  JOB_DATA VARCHAR(255),
  JOB_DESCRIPTION VARCHAR(100),
  UPDATE_TIME int8 not null,
  PRIMARY KEY (ID)
);
ALTER TABLE QRTZ_JOB ADD CONSTRAINT IDX_QRTZ_JOB_UNIQUE UNIQUE (JOB_IDX,JOB_CLASS);

COMMENT ON TABLE QRTZ_JOB IS '定时任务::任务配置表';
COMMENT ON COLUMN QRTZ_JOB.ID IS '主键';
COMMENT ON COLUMN QRTZ_JOB.APPLICATION IS '调度名称/应用名称';
COMMENT ON COLUMN QRTZ_JOB.STATE IS '任务状态 传入_默认INIT(EXECUTING.执行中 PAUSED.暂停 COMPLETE.完成 ERROR.异常 INIT.初始化/未启动）';
COMMENT ON COLUMN QRTZ_JOB.JOB_IDX IS '任务/触发器标签';
COMMENT ON COLUMN QRTZ_JOB.JOB_CLASS IS '任务全类名';
COMMENT ON COLUMN QRTZ_JOB.JOB_DATA IS '任务数据';
COMMENT ON COLUMN QRTZ_JOB.JOB_DESCRIPTION IS '任务描述';
COMMENT ON COLUMN QRTZ_JOB.update_time IS '更新时间';

-- QRTZ_EXECUTE
DROP TABLE IF EXISTS QRTZ_EXECUTE;
CREATE TABLE QRTZ_EXECUTE(
  ID INT8,
  PID INT8,
  EXECUTE_IDX INT4,
  JOB_TYPE VARCHAR(8),
  STATE VARCHAR(10),
  CRON VARCHAR(50),
  ZONE_ID VARCHAR(50),
  REPEAT_COUNT INT4,
  REPEAT_INTERVAL INT4,
  TIME_TRIGGERED INT8,
  PREV_FIRE_TIME INT8,
  NEXT_FIRE_TIME INT8,
  HOST_IP VARCHAR(50),
  HOST_NAME VARCHAR(80),
  START_TIME INT8,
  END_TIME INT8,
  PRIMARY KEY (ID)
);

ALTER TABLE QRTZ_EXECUTE ADD CONSTRAINT IDX_QRTZ_EXECUTE_UNIQUE UNIQUE (PID,EXECUTE_IDX);

COMMENT ON TABLE QRTZ_EXECUTE IS '定时任务::执行配置表';
COMMENT ON COLUMN QRTZ_EXECUTE.ID IS '主键';
COMMENT ON COLUMN QRTZ_EXECUTE.PID IS '关联任务';
COMMENT ON COLUMN QRTZ_EXECUTE.EXECUTE_IDX IS '任务/触发器标签';
COMMENT ON COLUMN QRTZ_EXECUTE.JOB_TYPE IS '任务类型';
COMMENT ON COLUMN QRTZ_EXECUTE.STATE IS '任务状态 传入_默认INIT(EXECUTING.执行中 PAUSED.暂停 COMPLETE.完成 ERROR.异常 INIT.初始化/未启动）';
COMMENT ON COLUMN QRTZ_EXECUTE.CRON IS 'CRON:cron表达式';
COMMENT ON COLUMN QRTZ_EXECUTE.ZONE_ID IS 'CRON:时区';
COMMENT ON COLUMN QRTZ_EXECUTE.REPEAT_COUNT IS 'SIMPLE:重复/执行次数';
COMMENT ON COLUMN QRTZ_EXECUTE.REPEAT_INTERVAL IS 'SIMPLE:执行时间间隔';
COMMENT ON COLUMN QRTZ_EXECUTE.TIME_TRIGGERED IS 'SIMPLE:执行时间';
COMMENT ON COLUMN QRTZ_EXECUTE.PREV_FIRE_TIME IS '上一次执行时间';
COMMENT ON COLUMN QRTZ_EXECUTE.NEXT_FIRE_TIME IS '下一次执行时间';
COMMENT ON COLUMN QRTZ_EXECUTE.HOST_IP IS '执行机器地址';
COMMENT ON COLUMN QRTZ_EXECUTE.HOST_NAME IS '执行机器名称';
COMMENT ON COLUMN QRTZ_EXECUTE.START_TIME IS '开始时间';
COMMENT ON COLUMN QRTZ_EXECUTE.END_TIME IS '结束时间';