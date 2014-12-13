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

package org.porquebox.core.datasource;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.runtime.PhpRuntimeFactory;
import org.jboss.logging.Logger;

public class JDBCDriverLoadingRuntimeService implements Service<QuercusEngine> {

    @Override
    public QuercusEngine getValue() throws IllegalStateException, IllegalArgumentException {
        return this.runtime;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.runtime = this.runtimeFactoryInjector.getValue().createInstance( "JDBC Driver Loader", false );
        } catch (IllegalStateException e) {
            throw new StartException( e);
        } catch (Exception e) {
            throw new StartException( e);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.runtimeFactoryInjector.getValue().destroyInstance( this.runtime );
        this.runtime = null;
    }

    public Injector<PhpRuntimeFactory> getRuntimeFactoryInjector() {
        return this.runtimeFactoryInjector;
    }

    private InjectedValue<PhpRuntimeFactory> runtimeFactoryInjector = new InjectedValue<PhpRuntimeFactory>();

    private QuercusEngine runtime;
    private static final Logger log = Logger.getLogger("org.porquebox.core.datasource");
}
