
package org.quartz.impl;

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * This is class is a simple implementation of a thread pool, based on the
 * <code>{@link ThreadPool}</code> interface.
 *  这个类是线程池的一个简单实现，基于ThreadPool接口。
 * </p>
 * 
 * <p>
 * <CODE>Runnable</CODE> objects are sent to the pool with the <code>{@link #runInThread(Runnable)}</code>
 * method, which blocks until a <code>Thread</code> becomes available.
 *  可运行对象通过runInThread（Runnable）方法发送到池中，该方法会阻塞直到线程可用。
 * </p>
 * 
 * <p>
 * The pool has a fixed number of <code>Thread</code>s, and does not grow or
 * shrink based on demand.
 *  该池具有固定数量的线程，并且不会根据需求增长或收缩。
 * </p>
 * 
 * @author James House
 * @author Juergen Donnerstag
 */
public class MeeThreadPool implements ThreadPool {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Data members.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private int count = -1;

    private int prio = Thread.NORM_PRIORITY;
    private boolean inheritLoader = false;

    private boolean inheritGroup = true;

    private boolean makeThreadsDaemons = false;

    private ThreadGroup threadGroup;

    private ThreadPoolExecutor poolExecutor = null;

    private String application;
    // instanceId
    private String threadNamePrefix;


    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constructors.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a new (unconfigured) <code>SimpleThreadPool</code>.
     * </p>
     *
     * @see #setThreadCount(int)
     * @see #setThreadPriority(int)
     */
    public MeeThreadPool() {
    }

    /**
     * <p>
     * Create a new <code>SimpleThreadPool</code> with the specified number
     * of <code>Thread</code> s that have the given priority.
     * </p>
     *
     * @param threadCount
     *          the number of worker <code>Threads</code> in the pool, must
     *          be > 0.
     * @param threadPriority
     *          the thread priority for the worker threads.
     *
     * @see Thread
     */
    public MeeThreadPool(int threadCount, int threadPriority) {
        setThreadCount(threadCount);
        setThreadPriority(threadPriority);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    @Override
    public int getPoolSize() {
        return getThreadCount();
    }

    /**
     * <p>
     * Set the number of worker threads in the pool - has no effect after
     * <code>initialize()</code> has been called.
     * </p>
     */
    public void setThreadCount(int count) {
        this.count = count;
    }

    /**
     * <p>
     * Get the number of worker threads in the pool.
     * </p>
     */
    public int getThreadCount() {
        return count;
    }

    /**
     * <p>
     * Set the thread priority of worker threads in the pool - has no effect
     * after <code>initialize()</code> has been called.
     * </p>
     */
    public void setThreadPriority(int prio) {
        this.prio = prio;
    }

    /**
     * <p>
     * Get the thread priority of worker threads in the pool.
     * </p>
     */
    public int getThreadPriority() {
        return prio;
    }

    /**
     * @return Returns the
     *         threadsInheritContextClassLoaderOfInitializingThread.
     */
    public boolean isThreadsInheritContextClassLoaderOfInitializingThread() {
        return inheritLoader;
    }

    /**
     * @param inheritLoader
     *          The threadsInheritContextClassLoaderOfInitializingThread to
     *          set.
     */
    public void setThreadsInheritContextClassLoaderOfInitializingThread(boolean inheritLoader) {
        this.inheritLoader = inheritLoader;
    }

    public boolean isThreadsInheritGroupOfInitializingThread() {
        return inheritGroup;
    }

    public void setThreadsInheritGroupOfInitializingThread(boolean inheritGroup) {
        this.inheritGroup = inheritGroup;
    }


    /**
     * @return Returns the value of makeThreadsDaemons.
     */
    public boolean isMakeThreadsDaemons() {
        return makeThreadsDaemons;
    }

    /**
     * @param makeThreadsDaemons
     *          The value of makeThreadsDaemons to set.
     */
    public void setMakeThreadsDaemons(boolean makeThreadsDaemons) {
        this.makeThreadsDaemons = makeThreadsDaemons;
    }
    @Override
    public void setInstanceId(String instanceId) {
        this.threadNamePrefix=instanceId+"-";
    }
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }
    @Override
    public void setApplication(String schedName) {
        this.application = schedName;
    }
    @Override
    public void initialize() throws SchedulerConfigException {
        // already initialized...
        if(null!=poolExecutor){
            return;
        }
        if (count <= 0) {
            throw new SchedulerConfigException("Thread count must be > 0");
        }
        if (prio <= 0 || prio > 9) {
            throw new SchedulerConfigException("Thread priority must be > 0 and <= 9");
        }
        if(isThreadsInheritGroupOfInitializingThread()) {
            threadGroup = Thread.currentThread().getThreadGroup();
        } else {
            // follow the threadGroup tree to the root thread group.
            threadGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parent = threadGroup;
            while ( !parent.getName().equals("main") ) {
                threadGroup = parent;
                parent = threadGroup.getParent();
            }
            threadGroup = new ThreadGroup(parent, application + "-SimpleThreadPool");
            if (isMakeThreadsDaemons()) {
                threadGroup.setDaemon(true);
            }
        }
        if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
            log.info("Job execution threads will use class loader of thread: " + Thread.currentThread().getName());
        }
        // create the worker threads and start them
        this.createWorkerThreads(this.count);
    }

    protected void createWorkerThreads(final int createCount) {
        int cct = this.count = createCount<1? Runtime.getRuntime().availableProcessors() :createCount;
//        final String threadPrefix = schedulerInstanceName + "_QRTZ_";
//        final String threadPrefix = "MEE_QRTZ_";
        final MyThreadFactory myThreadFactory = new MyThreadFactory(this.getThreadNamePrefix(), this);
        this.poolExecutor = new ThreadPoolExecutor(cct<4?1:cct/8+1,cct,8L, TimeUnit.SECONDS, new LinkedBlockingDeque(cct*2),myThreadFactory);
    }

    private final class MyThreadFactory implements ThreadFactory {
        final String threadPrefix ;//= schedulerInstanceName + "_QRTZ_";
        final MeeThreadPool meeThreadPool;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
//        private final ThreadGroup threadGroup;
//        private final String threadPrefix;
        public MyThreadFactory(final String threadPrefix,final MeeThreadPool meeThreadPool) {
            this.threadPrefix = threadPrefix;
            this.meeThreadPool = meeThreadPool;
        }

        @Override
        public Thread newThread(Runnable r) {
            WorkerThread wth = new WorkerThread(
                    meeThreadPool,
                    threadGroup,
                    threadPrefix + ((threadNumber.get())==count?threadNumber.getAndSet(1):threadNumber.getAndIncrement()),
                    getThreadPriority(),
                    isMakeThreadsDaemons(),
                    r);
            if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
                wth.setContextClassLoader(Thread.currentThread().getContextClassLoader());
            }
            return wth;
        }
    }

    /**
     * <p>
     * Terminate any worker threads in this thread group.
     * </p>
     * 
     * <p>
     * Jobs currently in progress will complete.
     * </p>
     */
    public void shutdown() {
        shutdown(true);
    }

    /**
     * <p>
     * Terminate any worker threads in this thread group.
     * </p>
     * 
     * <p>
     * Jobs currently in progress will complete.
     * </p>
     */
    @Override
    public synchronized void shutdown(boolean waitForJobsToComplete) {
        if(poolExecutor!=null && !poolExecutor.isShutdown()){
            // signal each worker thread to shut down
            poolExecutor.shutdown();
        }
    }

    /**
     * <p>
     * Run the given <code>Runnable</code> object in the next available
     * <code>Thread</code>. If while waiting the thread pool is asked to
     * shut down, the Runnable is executed immediately within a new additional
     * thread.
     * </p>
     * 
     * @param runnable
     *          the <code>Runnable</code> to be added.
     */
    @Override
    public boolean runInThread(Runnable runnable) {
        if (runnable == null) {
            return false;
        }
        // 阻塞 直到有可用的线程或队列 (核心线程->队列->最大线程)
        final int maximumPoolSize = poolExecutor.getMaximumPoolSize();
        int activeCount = poolExecutor.getActiveCount();
        int rct = poolExecutor.getQueue().remainingCapacity();
        while(activeCount==maximumPoolSize && rct<=0 && !poolExecutor.isShutdown()) {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
                activeCount = poolExecutor.getActiveCount();
                rct = poolExecutor.getQueue().remainingCapacity();
            } catch (InterruptedException ignore) {
                ignore.printStackTrace();
            }
        }
        if (!poolExecutor.isShutdown()) {
            poolExecutor.submit(runnable);
        } else {
            // If the thread pool is going down, execute the Runnable
            // within a new additional worker thread (no thread from the pool).
            WorkerThread wt = new WorkerThread(this, threadGroup,"WorkerThread-LastJob", prio, isMakeThreadsDaemons(), runnable);
            wt.start();
        }
        return true;
    }

    @Override
    public synchronized int blockForAvailableThreads() {
        final int corePoolSize = poolExecutor.getCorePoolSize();
        final int maximumPoolSize = poolExecutor.getMaximumPoolSize();
        int activeCount = poolExecutor.getActiveCount();
        int rct = poolExecutor.getQueue().remainingCapacity();
        while(activeCount==maximumPoolSize && rct<=0 && !poolExecutor.isShutdown()) {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
                activeCount = poolExecutor.getActiveCount();
                rct = poolExecutor.getQueue().remainingCapacity()-1;
            } catch (InterruptedException ignore) {
                ignore.printStackTrace();
            }
        }
        // 需要预留最少核心线程数个任务，所以队列也必须预留足够的空闲位置
        return corePoolSize;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * WorkerThread Class.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * A Worker loops, waiting to execute tasks.
     * </p>
     */
    class WorkerThread extends Thread {

        private final Object lock = new Object();

        // A flag that signals the WorkerThread to terminate.
        private AtomicBoolean run = new AtomicBoolean(true);

        private MeeThreadPool tp;

        private Runnable runnable = null;
        
        private boolean runOnce = false;

        /**
         * <p>
         * Create a worker thread, start it, execute the runnable and terminate
         * the thread (one time execution).
         * </p>
         */
        WorkerThread(MeeThreadPool tp, ThreadGroup threadGroup, String name, int prio, boolean isDaemon, Runnable runnable) {
            super(threadGroup, name);
            this.tp = tp;
            this.runnable = runnable;
            if(runnable != null){
                runOnce = true;
            }
            setPriority(prio);
            setDaemon(isDaemon);
        }

        /**
         * <p>
         * Signal the thread that it should terminate.
         * </p>
         */
        void shutdown() {
            run.set(false);
        }
        // 这个地方是关键，是所有任务执行的入口处
        public void run(Runnable newRunnable) {
            synchronized(lock) {
                if(runnable != null) {
                    throw new IllegalStateException("Already running a Runnable!");
                }
                runnable = newRunnable;
                lock.notifyAll();
            }
        }

        /**
         * <p>
         * Loop, executing targets as they are received. 循环，在收到目标时执行目标。
         * </p>
         */
        @Override
        public void run() {
            boolean ran = false;
            while (run.get()) {
                try {
                    synchronized(lock) {
                        while (runnable == null && run.get()) {
                            lock.wait(500);
                        }
                        if (runnable != null) {
                            ran = true;
                            runnable.run();
                        }
                    }
                } catch (Throwable e) {
                    // do nothing (loop will terminate if shutdown() was called 不做任何事情（如果调用了shutdown（），循环将终止
                    try {
                        if( e instanceof InterruptedException){
                            log.error("Worker thread was interrupt()'ed.", e);
                        }else{
                            log.error("Error while executing the Runnable: ", e);
                        }
                    } catch(Exception e2) {
                        // ignore to help with a tomcat glitch 忽略以帮助解决tomcat故障
                        e.printStackTrace();
                    }

                } finally {
                    synchronized(lock) {
                        runnable = null;
                    }
                    // repair the thread in case the runnable mucked it up... 修理螺纹，以防运行时把它弄坏了。。。
                    if(getPriority() != tp.getThreadPriority()) {
                        setPriority(tp.getThreadPriority());
                    }
                    if (runOnce) {
                        run.set(false);
//                        clearFromBusyWorkersList(this);
                    } else if(ran) {
                        ran = false;
//                        makeAvailable(this);
                    }
                }
            }
            log.debug("WorkerThread is shut down.");
        }
    }
}
