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

package org.quartz;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.transaction.UserTransaction;

/**
 * An annotation that marks a {@link Job} class as one that will have its 
 * execution wrapped by a JTA Transaction. 
 *   
 * <p>If this annotation is present, Quartz will begin a JTA transaction 
 * before calling the <code>execute()</code> method, and will commit
 * the transaction if the method does not throw an exception and the
 * transaction has not had <code>setRollbackOnly()</code> called on it 
 * (otherwise the transaction will be rolled-back by Quartz).</p>
 * 
 * <p>This is essentially the same behavior as setting the configuration
 * property <code>org.quartz.scheduler.wrapJobExecutionInUserTransaction</code>
 * to <code>true</code> - except that it only affects the job that has
 * the annotation, rather than all jobs (as the property does).  If the
 * property is set to <code>true</code> and the annotation is also set,
 * then of course the annotation becomes redundant.</p>
 *
 *  一个注释，将一个作业类标记为将由JTA事务包装其执行。
 * 如果存在此注释，Quartz将在调用execute()方法之前启动一个JTA事务，如果该方法没有抛出异常并且没有对它调用setRollbackOnly()，则将提交事务（否则事务将被Quartz回滚）。
 * 这与将配置属性org.quartz.scheduler.wrapJobExecutionInUserTransaction设置为true的行为基本相同，只是它只影响具有注释的作业，而不是所有作业（如属性所做的那样）。如果属性设置为true，并且注释也设置，那么注释当然就变得多余了。
 *
 * @author jhouse
 *
 *
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExecuteInJTATransaction {

  /**
   * The JTA transaction timeout.
   * <p>
   * If set then the {@code UserTransaction} timeout will be set to this
   * value before beginning the transaction.
   * 
   * @see UserTransaction#setTransactionTimeout(int) 
   * @return the transaction timeout.
   */
  int timeout() default -1;
}
