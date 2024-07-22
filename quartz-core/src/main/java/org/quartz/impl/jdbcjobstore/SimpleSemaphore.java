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

import java.sql.Connection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal in-memory lock handler for providing thread/resource locking in 
 * order to protect resources from being altered by multiple threads at the 
 * same time.
 *  内存中的内部锁处理程序，用于提供线程/资源锁定，以保护资源不被多个线程同时更改。
 * 
 * @author jhouse
 */
public class SimpleSemaphore implements Semaphore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    // 线程本地变量保证的是 变量从添加到删除都在一个线程中
    // 不是很明白的是为什么要用HashSet套一层，是否可以这样： ThreadLocal<String>
    private ThreadLocal<HashSet<String>> lockOwners = new ThreadLocal<HashSet<String>>();
    //此变量保证的是在多线程中有且保持只有一个唯一的变量被添加及移除，它的安全是由方法上的 synchronized 保证的
    HashSet<String> locks = new HashSet<String>();

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected Logger getLog() {
        return log;
    }

    private HashSet<String> getThreadLocks() {
        HashSet<String> threadLocks = lockOwners.get();
        if (threadLocks == null) {
            threadLocks = new HashSet<String>();
            lockOwners.set(threadLocks);
        }
        return threadLocks;
    }

    /**
     * Grants a lock on the identified resource to the calling thread (blocking
     * until it is available).
     *  将已标识资源的锁授予调用线程（阻塞直到资源可用）。
     *
     * @return true if the lock was obtained.
     */
    @Override
    public synchronized boolean obtainLock(Connection conn, String lockName) {
        lockName = lockName.intern();
//        if(log.isDebugEnabled()) {
//            log.debug("Lock '" + lockName + "' is desired by: " + Thread.currentThread().getName());
//        }
        if (!isLockOwner(lockName)) {
//            if(log.isDebugEnabled()) {
//                log.debug("Lock '" + lockName + "' is being obtained: " + Thread.currentThread().getName());
//            }
            while (locks.contains(lockName)) {
                try {
                    // 如果 locks 里已经有lockName则表示其他线程已经持有此锁，此时需要等待被唤醒
                    this.wait();
                } catch (InterruptedException ie) {
                    if(log.isDebugEnabled()) {
                        log.debug("Lock '" + lockName + "' was not obtained by: " + Thread.currentThread().getName());
                    }
                }
            }
//            if(log.isDebugEnabled()) {
//                log.debug("Lock '" + lockName + "' given to: " + Thread.currentThread().getName());
//            }
            // 将lockName放到lockOwners线程本地变量中
            getThreadLocks().add(lockName);
            locks.add(lockName);
        } else if(log.isDebugEnabled()) {
            log.debug("Lock '" + lockName + "' already owned by: " + Thread.currentThread().getName() + " -- but not owner!",new Exception("stack-trace of wrongful returner"));
        }
        return true;
    }

    /**
     * Release the lock on the identified resource if it is held by the calling
     * thread.
     */
    @Override
    public synchronized void releaseLock(String lockName) {
        lockName = lockName.intern();
        if (isLockOwner(lockName)) {
//            if(getLog().isDebugEnabled()) {
//                getLog().debug("Lock '" + lockName + "' retuned by: " + Thread.currentThread().getName());
//            }
            // 从线程本地变量中移除
            getThreadLocks().remove(lockName);
            // 从公共变量中移除
            locks.remove(lockName);
            // 这个是唤醒的是 obtainLock 中的wait()
            this.notifyAll();
        } else if (getLog().isDebugEnabled()) {
            getLog().debug("Lock '" + lockName + "' attempt to retun by: " + Thread.currentThread().getName() + " -- but not owner!", new Exception("stack-trace of wrongful returner"));
        }
    }

    /**
     * Determine whether the calling thread owns a lock on the identified
     * resource.
     *  确定调用线程是否拥有已标识资源的锁。
     */
    public synchronized boolean isLockOwner(String lockName) {
        lockName = lockName.intern();
        return getThreadLocks().contains(lockName);
    }

    /**
     * This Semaphore implementation does not use the database.
     */
    @Override
    public boolean requiresConnection() {
        return false;
    }
}
