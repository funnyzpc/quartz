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

/**
 * 执行后保留作业数据
 *
 * An annotation that marks a {@link Job} class as one that makes updates to its
 * {@link JobDataMap} during execution, and wishes the scheduler to re-store the
 * <code>JobDataMap</code> when execution completes.
 * 一种注释，将Job类标记为在执行过程中对其JobDataMap进行更新的类，并希望调度器在执行完成时重新存储JobDataMap。
 *   
 * <p>Jobs that are marked with this annotation should also seriously consider
 * using the {@link DisallowConcurrentExecution} annotation, to avoid data
 * storage race conditions with concurrently executing job instances.</p>
 * 标记有此注释的作业还应认真考虑使用DisallowConcurrentExecution注释，以避免并发执行作业实例的数据存储竞争情况。
 *
 * @see DisallowConcurrentExecution
 * 
 * @author jhouse
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PersistJobDataAfterExecution {

}
