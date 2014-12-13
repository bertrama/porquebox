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

package org.porquebox.core.runtime.processors;

import java.io.File;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.runtime.BaseRuntimeInitializer;
import org.porquebox.core.runtime.BaseRuntimePreparer;
// TODO: ComposerAwareRuntimePreparer ?
//import org.porquebox.core.runtime.BundlerAwareRuntimePreparer;
import org.porquebox.core.runtime.PhpLoadPathMetaData;
import org.porquebox.core.runtime.PhpRuntimeMetaData;
import org.porquebox.core.runtime.RuntimeInitializer;
import org.porquebox.core.runtime.RuntimePreparer;
import org.jboss.logging.Logger;

public class BasePhpRuntimeInstaller implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        PhpAppMetaData phpAppMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData == null) {
            return;
        }

        PhpRuntimeMetaData runtimeMetaData = unit.getAttachment( PhpRuntimeMetaData.ATTACHMENT_KEY );

        if (runtimeMetaData != null && runtimeMetaData.getRuntimeType() != null) {
            return;
        }

        if (runtimeMetaData == null) {
            runtimeMetaData = new PhpRuntimeMetaData();
            unit.putAttachment( PhpRuntimeMetaData.ATTACHMENT_KEY, runtimeMetaData );
        }

        File root = phpAppMetaData.getRoot();
        runtimeMetaData.setBaseDir( root );
        runtimeMetaData.setEnvironment( phpAppMetaData.getEnvironmentVariables() );
        runtimeMetaData.setRuntimeType( PhpRuntimeMetaData.RuntimeType.BARE );
        runtimeMetaData.appendLoadPath( new PhpLoadPathMetaData( root ) );
        // TODO: Composer-ify
        // runtimeMetaData.appendLoadPath( new PhpLoadPathMetaData( new File( root, "lib" ) ) );
        // runtimeMetaData.appendLoadPath( new PhpLoadPathMetaData( new File( root, "config" ) ) );

        RuntimeInitializer initializer = new BaseRuntimeInitializer( phpAppMetaData );
        RuntimePreparer preparer = new BaseRuntimePreparer( phpAppMetaData );
        // TODO: Composeer-ify.
        // File gemfile = new File( root, "Gemfile" );
        // if (gemfile.exists()) {
            // preparer = new BundlerAwareRuntimePreparer( phpAppMetaData );
        // } else {
            // preparer = new BaseRuntimePreparer( phpAppMetaData );
        // }
        runtimeMetaData.setRuntimeInitializer( initializer );
        runtimeMetaData.setRuntimePreparer( preparer );
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // TODO Auto-generated method stub
    }
    static final Logger log = Logger.getLogger( "org.porquebox.core.runtime.processors" );
}
