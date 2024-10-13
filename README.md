

## quartz-mee

### 待办任务
+ 移除qrtz_job::job_idx字段 ✔
+ 移除qrtz_execute::execute_idx字段 ✔
+ 移除/优化 JobDetail::getKey() job_id#execute_id#job_class#job_type ✔
+ 优化 JobRunShell:initialize() 参数组织  ✔
+ 去掉所有builder ✔
+ 去掉 qrtz_job_cfg 及 qrtz_execute_cfg 相关代码 ✔
+ 去掉 Calendar 相关代码,仅保留Calendar本身  ✔
+ 添加json序列化及反序列化操作 ✔
+ 去掉顯示的鎖處理 ✔
+ 兼容mysql及oracle數據庫 ✔
+ 啓動時自動獲取數據庫類型
+ ResultSet 关闭处理
+ dsName 成员变量相关逻辑梳理
+ 
