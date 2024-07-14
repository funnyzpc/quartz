/**
 * '调度名称';
 */
private String schedName;
/**
 * '触发器的名称,关联qrtz_job_details';
 */
private String triggerName;
/**
 * '触发器的类型，使用cron表达式';
 */
private String triggerType;
/**
 * '当前触发器状态（ WAITING：等待； PAUSED：暂停； ACQUIRED：正常执行； BLOCKED：阻塞； ERROR：错误；）';
 */
private String triggerState;
/**
 * '详细描述信息';
 */
private String description;
/**
 * '下一次触发时间（毫秒），默认为-1，意味不会自动触发';
 */
private String nextFireTime;
/**
 * '上一次触发时间（毫秒）';
 */
private String prevFireTime;
/**
 * '优先级';
 */
private Integer priority;
/**
 * '开始时间';
 */
private Long startTime;
/**
 * '结束时间';
 */
private Long endTime;
/**
 * '日程表名称，表qrtz_calendars的CALENDAR_NAME字段的值';
 */
private String calendarName;
/**
 * '措施或者是补偿执行的策略';
 */
private Integer misfireInstr;
/**
 * 'DETAILS:集群中job实现类的全名，quartz就是根据这个路径到classpath找到该job类';
 */
private String jobClassName;
/**
 * 'DETAILS:是否持久化，把该属性设置为1，quartz会把job持久化到数据库中';
 */
private Boolean isDurable;
/**
 * 'DETAILS:是否并发执行';
 */
private Boolean isNonconcurrent;
/**
 * 'DETAILS:是否更新数据';
 */
private Boolean isUpdateData;
/**
 * 'DETAILS:是否接受恢复执行，默认为false，设置了RequestsRecovery为true，则该job会被重新执行';
 */
private Boolean requestsRecovery;
/**
 * 'DETAILS:一个blob字段，存放持久化job对象';
 */
private Byte[] jobData;




