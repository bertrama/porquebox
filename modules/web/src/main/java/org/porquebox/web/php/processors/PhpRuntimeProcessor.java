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

package org.porquebox.web.php.processors;

import java.io.File;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.runtime.PhpLoadPathMetaData;
import org.porquebox.core.runtime.PhpRuntimeMetaData;
import org.porquebox.core.runtime.RuntimeInitializer;
import org.porquebox.core.runtime.RuntimePreparer;
import org.porquebox.web.php.PhpMetaData;
import org.porquebox.web.php.PhpRuntimeInitializer;

/**
 * Create the php runtime metadata from the php metadata
 */
public class PhpRuntimeProcessor implements DeploymentUnitProcessor {

    public PhpRuntimeProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        PhpAppMetaData phpAppMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );
        
        if ( phpAppMetaData == null ) {
            return;
        }
        
        PhpRuntimeMetaData runtimeMetaData = unit.getAttachment(  PhpRuntimeMetaData.ATTACHMENT_KEY );
        
        if ( runtimeMetaData != null && runtimeMetaData.getRuntimeType() != null ) {
            return;
        }

        if (runtimeMetaData == null) {
            runtimeMetaData = new PhpRuntimeMetaData();
            unit.putAttachment(  PhpRuntimeMetaData.ATTACHMENT_KEY, runtimeMetaData );
        }

        runtimeMetaData.setBaseDir( phpAppMetaData.getRoot() );
        runtimeMetaData.setEnvironment( phpAppMetaData.getEnvironmentVariables() );
        runtimeMetaData.setRuntimeType( PhpRuntimeMetaData.RuntimeType.BARE );
        runtimeMetaData.appendLoadPath( new PhpLoadPathMetaData( phpAppMetaData.getRoot() ) );
        runtimeMetaData.appendLoadPath( new PhpLoadPathMetaData( new File( phpAppMetaData.getRoot(), "lib" ) ) );

        RuntimeInitializer initializer = new PhpRuntimeInitializer( phpAppMetaData );
        // TODO: Do we need to do something comparable with Composer?
        // RuntimePreparer preparer = new BundlerAwareRuntimePreparer( phpAppMetaData );
        runtimeMetaData.setRuntimeInitializer( initializer );
        //runtimeMetaData.setRuntimePreparer( preparer );
    }


    @Override
    public void undeploy(org.jboss.as.server.deployment.DeploymentUnit context) {
        
    }
    
    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.web.php" );

}
