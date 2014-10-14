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

import java.io.File;
import org.jboss.logging.Logger;
import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.app.PhpAppMetaData;
// TODO: Does php needs the RuntimeHelper?
//import org.porquebox.core.util.RuntimeHelper;

/**
 * {@link RuntimeInitializer} for Php applications.
 * 
 */
public class BaseRuntimeInitializer implements RuntimeInitializer {


    public BaseRuntimeInitializer(PhpAppMetaData phpAppMetaData) {
        this.phpAppMetaData = phpAppMetaData;
    }

    @Override
    public void initialize(QuercusEngine php, String runtimeContext) throws Exception {
        String appName = this.phpAppMetaData.getApplicationName();

        StringBuilder script = new StringBuilder();
        // TODO: Set the environment variables in Quercus.
        // script.append( "PORQUEBOX_APP_NAME=%q(" + appName + ")\n" );
        // script.append( "ENV['PORQUEBOX_APP_NAME']=%q(" + appName + ")\n" );
        // script.append( "ENV['PORQUEBOX_CONTEXT']=%q(" + runtimeContext + ")\n" );
        // RuntimeHelper.evalScriptlet( php, script.toString() );
        // RuntimeHelper.requireTorqueBoxInit(php);
    }

    public PhpAppMetaData getPhpAppMetaData() {
        return phpAppMetaData;
    }

    public File getApplicationRoot() {
        return phpAppMetaData.getRoot();
    }
    
    private static final Logger log = Logger.getLogger( "org.porquebox.core.runtime" );
    
    private PhpAppMetaData phpAppMetaData;
    
}
