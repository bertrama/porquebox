/*
 * Copyright 2008-2013 Red Hat, Inc, and individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.porquebox.core.runtime;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.logging.Logger;
import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.pool.InstanceFactory;

public class RestartablePhpRuntimePool implements PhpRuntimePool, RestartablePhpRuntimePoolMBean {

    public RestartablePhpRuntimePool(PhpRuntimePool pool) {
        this.currentPool = pool;
    }

    public void restart() {
        final PhpRuntimePool newPool = this.currentPool.duplicate();
        // Always start replacement runtimes immediately
        newPool.setDeferUntilRequested( false );
        new Thread( new Runnable() {
            public void run() {
                restartInternal( newPool );
            }
        }).start();
    }

    protected void restartInternal(PhpRuntimePool newPool) {
        try {
            newPool.start();
        }
        catch (Exception e) {
            log.error( "Error starting new runtime " + newPool, e);
            return;
        }
        try {
            borrowLock.acquire(NUM_PERMITS);
            returnLock.acquire(NUM_PERMITS);
            PhpRuntimePool oldPool = this.currentPool;
            this.currentPool = newPool;
            if (oldPool.isDrained()) {
                retirePool( oldPool );
            } else {
                this.previousPools.add( oldPool );
            }
        } catch (InterruptedException e) {
            log.error("Error restarting runtime", e);
            return;
        } finally {
            if (returnLock.availablePermits() == 0) {
                returnLock.release(NUM_PERMITS);
            }
            if (borrowLock.availablePermits() == 0) {
                borrowLock.release(NUM_PERMITS);
            }
        }
        for (PhpRuntimePoolRestartListener listener : this.restartListeners) {
            listener.runtimeRestarted();
        }
    }

    @Override
    public QuercusEngine borrowRuntime(String requester) throws Exception {
        borrowLock.acquire();
        try {
            return this.currentPool.borrowRuntime( requester );
        } finally {
            borrowLock.release();
        }
    }

    @Override
    public void returnRuntime(QuercusEngine runtime) {
        try {
            returnLock.acquire();
        } catch (InterruptedException e) {
            log.error("Error returning runtime", e);
            return;
        }
        try {
            this.currentPool.returnRuntime( runtime );
            Iterator<PhpRuntimePool> poolIterator = this.previousPools.iterator();
            while (poolIterator.hasNext()) {
                PhpRuntimePool previousPool = poolIterator.next();
                previousPool.returnRuntime( runtime );
                if (previousPool.isDrained()) {
                    retirePool( previousPool );
                    poolIterator.remove();
                }
            }
        } finally {
            returnLock.release();
        }
    }

    protected void retirePool(PhpRuntimePool pool) {
        try {
            pool.stop();
        } catch (Exception e) {
            log.error( "Error stopping retired runtime pool " + pool, e );
        }
    }

    @Override
    public Set<String> getAllRuntimeNames() {
        return this.currentPool.getAllRuntimeNames();
    }

    @Override
    public String getName() {
        return this.currentPool.getName();
    }

    @Override
    public void start() throws Exception {
        this.currentPool.start();
    }

    @Override
    public void stop() throws Exception {
        this.currentPool.stop();
    }

    public void registerRestartListener(PhpRuntimePoolRestartListener listener) {
        this.restartListeners.add( listener );
    }

    @Override
    public void setNamespaceContextSelector(NamespaceContextSelector nsContextSelector) {
        this.currentPool.setNamespaceContextSelector( nsContextSelector );
    }

    @Override
    public void setInstanceFactory(InstanceFactory<QuercusEngine> factory) {
        this.currentPool.setInstanceFactory( factory );
    }

    @Override
    public PhpRuntimePool duplicate() {
        return new RestartablePhpRuntimePool( this.currentPool.duplicate() );
    }

    @Override
    public boolean isDrained() {
        return this.currentPool.isDrained();
    }

    @Override
    public Object evaluate(String code) throws Exception {
        return this.currentPool.evaluate( code );
    }

    @Override
    public void setMinimumInstances(int minInstances) {
        this.currentPool.setMinimumInstances( minInstances );
    }

    @Override
    public int getMinimumInstances() {
        return this.currentPool.getMinimumInstances();
    }

    @Override
    public void setMaximumInstances(int maxInstances) {
        this.currentPool.setMaximumInstances( maxInstances );
    }

    @Override
    public int getMaximumInstances() {
        return this.currentPool.getMaximumInstances();
    }

    @Override
    public boolean isLazy() {
        return this.currentPool.isLazy();
    }

    @Override
    public boolean isStarted() {
        return this.currentPool.isStarted();
    }

    @Override
    public int getSize() {
        return this.currentPool.getSize();
    }

    @Override
    public int getBorrowed() {
        return this.currentPool.getBorrowed();
    }

    @Override
    public int getAvailable() {
        return this.currentPool.getAvailable();
    }

    @Override
    public boolean isDeferredUntilRequested() {
        return this.currentPool.isDeferredUntilRequested();
    }

    @Override
    public void setDeferUntilRequested(boolean deferUntilRequested) {
        this.currentPool.setDeferUntilRequested( deferUntilRequested );
    }

    public PhpRuntimePool getBackingPool() {
        return this.currentPool;
    }

    private volatile PhpRuntimePool currentPool;
    private Set<PhpRuntimePool> previousPools = new HashSet<PhpRuntimePool>();
    private Set<PhpRuntimePoolRestartListener> restartListeners = new CopyOnWriteArraySet<PhpRuntimePoolRestartListener>();
    private static final int NUM_PERMITS = 5000000; // arbitrary - no more than 5 million concurrent treads
    private final Semaphore borrowLock = new Semaphore(NUM_PERMITS);
    private final Semaphore returnLock = new Semaphore(NUM_PERMITS);
    private static final Logger log = Logger.getLogger( "org.porquebox.core.runtime" );
}
