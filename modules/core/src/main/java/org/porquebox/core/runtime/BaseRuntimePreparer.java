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

import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceRegistry;
import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.component.ComponentRegistry;
//import org.torquebox.core.util.JRubyConstants;
//import org.torquebox.core.util.RuntimeHelper;

public class BaseRuntimePreparer implements RuntimePreparer {

    public BaseRuntimePreparer(PhpAppMetaData phpAppMetaData) {
        this.phpAppMetaData = phpAppMetaData;
    }

    @Override
    public void prepareRuntime(QuercusEngine php, String runtimeContext, ServiceRegistry serviceRegistry) throws Exception {
        if (phpAppMetaData != null) {
            // TODO: Set the CWD in quercus.
            // php.setCurrentDirectory( phpAppMetaData.getRoot().getCanonicalPath() );
        }

        // TODO: See what needs to be prepared for Quercus to run happily.
        php.getQuercus().start();
        php.getQuercus().init();


        injectServiceRegistry( php, serviceRegistry );
        ComponentRegistry.createRegistryFor( php );
    }
    
    private void injectServiceRegistry(QuercusEngine runtime, ServiceRegistry serviceRegistry) {
        // TODO: Implement a service registry in php.
        // RuntimeHelper.require( runtime, "torquebox/service_registry" );
        // RuntimeHelper.invokeClassMethod( runtime, "TorqueBox::ServiceRegistry", "service_registry=", new Object[] { serviceRegistry } );
    }

    private static final Logger log = Logger.getLogger( "org.porquebox.core.runtime" );

    protected PhpAppMetaData phpAppMetaData;

}
