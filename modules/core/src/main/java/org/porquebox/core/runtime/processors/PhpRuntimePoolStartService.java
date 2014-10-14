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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.projectodd.polyglot.core.AsyncService;

public class PhpRuntimePoolStartService extends AsyncService<PhpRuntimePool> {

    public PhpRuntimePoolStartService(PhpRuntimePool pool) {
        this.pool = pool;
    }

    @Override
    public PhpRuntimePool getValue() throws IllegalStateException, IllegalArgumentException {
        return pool;
    }

    @Override
    public void startAsync(final StartContext context) throws Exception {
        this.pool.start();
    }

    @Override
    public void stop(StopContext context) {
        // we intentionally only are responsible for starting a pool.
        // the pool will be responsible for stopping itself.
    }

    public Injector<PhpRuntimeFactory> getPhpRuntimeFactoryInjector() {
        return this.runtimeFactoryInjector;
    }

    private InjectedValue<PhpRuntimeFactory> runtimeFactoryInjector = new InjectedValue<PhpRuntimeFactory>();
    private PhpRuntimePool pool;

}
