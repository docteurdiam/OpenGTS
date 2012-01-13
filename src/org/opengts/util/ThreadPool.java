// ----------------------------------------------------------------------------
// Copyright 2007-2011, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Thread pool manager
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/03  Martin D. Flynn
//     -Removed reference to JavaMail api imports
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2006/11/28  Martin D. Flynn
//     -Added method "setMaxSize(size)"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** Thread pool manager
**/

public class ThreadPool
{
    
    // ------------------------------------------------------------------------

    private static final int DFT_POOL_SIZE  = 20;
    
    public  static final int STOP_WAITING   = -1;
    public  static final int STOP_NEVER     = 0;
    public  static final int STOP_NOW       = 1;

    // ------------------------------------------------------------------------

    private ThreadGroup                 poolGroup       = null;
    private java.util.List<ThreadJob>   jobThreadPool   = null;
    private int                         maxPoolSize     = DFT_POOL_SIZE;
    private int                         threadId        = 1;
    private java.util.List<Runnable>    jobQueue        = null;
    private int                         waitingCount    = 0;
    private int                         stopThreads     = STOP_NEVER;

    /**
    *** Constuctor
    *** @param name The name of the thread pool
    **/
    public ThreadPool(String name)
    {
        this(name, DFT_POOL_SIZE);
    }
    
    /**
    *** Constructor
    *** @param name The name of the thread pool
    *** @param maxPoolSize The maximum number of threads in the thread pool[CHECK]
    **/
    public ThreadPool(String name, int maxPoolSize)
    {
        super();
        this.poolGroup     = new ThreadGroup((name != null)? name : "ThreadPool");
        this.jobThreadPool = new Vector<ThreadJob>();
        this.jobQueue      = new Vector<Runnable>();
        this.setMaxSize(maxPoolSize);
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the name of the thread pool
    *** @return The name of the thread pool
    **/
    public String getName()
    {
        return this.getThreadGroup().getName();
    }
    
    /**
    *** Returns the name of the thread pool
    *** @return The name of the thread pool
    **/
    public String toString()
    {
        return this.getName();
    }
    
    /**
    *** Returns true if this object is equal to <code>other</code>. This will
    *** only return true if they are the same object
    *** @param other The object to check equality with
    *** @return True if <code>other</code> is the same object
    **/
    public boolean equals(Object other)
    {
        return (this == other); // equals only if same object
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the thread group of the Threads in this pool
    *** @return The thread group of the Threads in this pool
    **/
    public ThreadGroup getThreadGroup()
    {
        return this.poolGroup;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the current size of this thread pool
    *** @return The number of thread jobs in this thread pool
    **/
    public int getSize()
    {
        int size = 0;
        synchronized (this.jobThreadPool) {
            size = this.jobThreadPool.size();
        }
        return size;
    }

    /**
    *** Sets the maximum size of this thread pool
    *** @param maxSize The maximum size of the thread pool
    **/
    public void setMaxSize(int maxSize)
    {
        this.maxPoolSize = (maxSize > 0)? maxSize : DFT_POOL_SIZE;
    }

    /**
    *** Gets the maximum size of this thread pool
    *** @return The maximum size of the thread pool
    **/
    public int getMaxSize()
    {
        return this.maxPoolSize;
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Adds a new job to the thread pool's queue
    *** @param job The job to add to the queue
    **/
    public void run(Runnable job)
    {
        if (job != null) {
            synchronized (this.jobThreadPool) { // <-- modification of threadPool is likely
                synchronized (this.jobQueue) { // <-- modification of job queue mandatory
                    // It's possible that we may end up adding more threads than we need if this
                    // section executes multiple times before the newly added thread has a chance 
                    // to pull a job off the queue.
                    this.jobQueue.add(job);
                    if ((this.waitingCount == 0) && (this.jobThreadPool.size() < this.maxPoolSize)) {
                        ThreadJob tj = new ThreadJob(this, (this.getName() + "_" + (this.threadId++)));
                        this.jobThreadPool.add(tj);
                        Print.logDebug("New Thread: " + tj.getName() + " [" + this.getMaxSize() + "]");
                    }
                    this.jobQueue.notify(); // notify a waiting thread
                }
            }
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Stops all threads in this pool once queued jobs are complete
    **/
    public void stopThreads()
    {
        synchronized (this.jobQueue) {
            this.stopThreads = STOP_WAITING;
            this.jobQueue.notifyAll();
        }
    }
    
    /**
    *** Removes the specified worker thread from the pool
    *** @param thread The thread to remove from the pool
    **/
    protected void _removeThread(ThreadJob thread)
    {
        if (thread != null) {
            synchronized (this.jobThreadPool) {
                //Print.logDebug("Removing thread: " + thread.getName());
                this.jobThreadPool.remove(thread);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    private static class ThreadJob
        extends Thread
    {
        private Runnable job = null;
        private ThreadPool threadPool = null;
        
        public ThreadJob(ThreadPool pool, String name) {
            super(pool.getThreadGroup(), name);
            this.threadPool = pool;
            this.start(); // auto start
        }
       
        public void run() {
    
            /* loop forever */
            while (true) {
    
                /* get next job */
                // 'this.job' is always null here
                boolean stop = false;
                synchronized (this.threadPool.jobQueue) {
                    //Print.logDebug("Thread checking for jobs: " + this.getName());
                    while (this.job == null) {
                        if (this.threadPool.stopThreads == STOP_NOW) {
                            // stop now, no more jobs
                            stop = true;
                            break;
                        } else
                        if (this.threadPool.jobQueue.size() > 0) {
                            this.job = this.threadPool.jobQueue.remove(0);
                        } else
                        if (this.threadPool.stopThreads == STOP_WAITING) {
                            // stop after all jobs have completed
                            stop = true;
                            break;
                        } else {
                            this.threadPool.waitingCount++;
                            try { this.threadPool.jobQueue.wait(20000); } catch (InterruptedException ie) {}
                            this.threadPool.waitingCount--;
                        }
                    }
                }
                if (stop) { break; }
                
                /* run job */
                //Print.logDebug("Thread running: " + this.getName());
                this.job.run();
                this.job = null;
                
            }
            
            /* remove thread from pool */
            this.threadPool._removeThread(this);
            
        }
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        ThreadPool pool = new ThreadPool("Test", 3);
        for (int i = 0; i < 12; i++) {
            final int n = i;
            Print.logInfo("Job " + i);
            Runnable job = new Runnable() {
                int num = n;
                public void run() {
                    Print.logInfo("Starting Job: " + this.getName());
                    try { Thread.sleep(2000 + (num * 479)); } catch (Throwable t) {}
                    Print.logInfo("Stopping Job:                " + this.getName());
                }
                public String getName() {
                    return "[" + Thread.currentThread().getName() + "] " + num;
                }
            };
            pool.run(job);
            try { Thread.sleep(500 + (i * 58)); } catch (Throwable t) {}
        }
        Print.logInfo("Stop Threads");
        pool.stopThreads();
    }
    
}
