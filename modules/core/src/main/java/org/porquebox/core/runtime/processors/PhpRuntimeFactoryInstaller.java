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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.as.CoreServices;
import org.porquebox.core.component.ComponentResolver;
import org.porquebox.core.injection.analysis.Injectable;
import org.porquebox.core.runtime.PhpLoadPathMetaData;
import org.porquebox.core.runtime.PhpRuntimeFactory;
import org.porquebox.core.runtime.PhpRuntimeFactoryService;
import org.porquebox.core.runtime.PhpRuntimeMetaData;

public class PhpRuntimeFactoryInstaller implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }

        PhpAppMetaData phpAppMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );
        PhpRuntimeMetaData runtimeMetaData = unit.getAttachment( PhpRuntimeMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData != null && runtimeMetaData != null) {
            PhpRuntimeFactory factory = new PhpRuntimeFactory( runtimeMetaData.getRuntimeInitializer(),
                    runtimeMetaData.getRuntimePreparer() );

            List<String> loadPaths = new ArrayList<String>();

            for (PhpLoadPathMetaData loadPath : runtimeMetaData.getLoadPaths()) {
                loadPaths.add( loadPath.getPath().getAbsolutePath() );
            }

            Module module = unit.getAttachment( Attachments.MODULE );

            if (module != null) {
                factory.setClassLoader( module.getClassLoader() );
            }

            factory.setServiceRegistry( phaseContext.getServiceRegistry() );
            factory.setLoadPaths( loadPaths );
            factory.setApplicationName( phpAppMetaData.getApplicationName() );
            factory.setApplicationEnvironment( phpAppMetaData.getEnvironmentVariables() );
            factory.setDebug( runtimeMetaData.isDebug() );
            factory.setInteractive( runtimeMetaData.isInteractive() );
            factory.setProfileApi( runtimeMetaData.isProfileApi() );

            // TODO: Determine whether we need to be indentifying the PHP version.
            // TODO: Determine whether we can leverage other compile modes.

            // factory.setPhpVersion( CompatVersion.V_5_4 );
            // factory.setCompileMode( CompileMode.OFF );

            PhpRuntimeFactoryService service = new PhpRuntimeFactoryService( factory );
            ServiceName name = CoreServices.runtimeFactoryName( unit );

            ServiceBuilder<PhpRuntimeFactory> builder = phaseContext.getServiceTarget().addService( name, service );
            addPredeterminedInjections( phaseContext, builder, factory );
            builder.install();

            installLightweightFactory( phaseContext, factory );
        }
    }

    protected void addPredeterminedInjections(DeploymentPhaseContext phaseContext, ServiceBuilder<?> builder, PhpRuntimeFactory factory)
            throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        AttachmentList<Injectable> additionalInjectables = unit.getAttachment( ComponentResolver.ADDITIONAL_INJECTABLES );

        if (additionalInjectables != null) {
            for (Injectable injectable : additionalInjectables) {
                try {
                    ServiceName serviceName = injectable.getServiceName( phaseContext.getServiceTarget(), phaseContext.getDeploymentUnit() );
                    if (serviceName != null) {
                        builder.addDependency( serviceName, factory.getInjector( injectable.getKey() ) );
                    } else if (!injectable.isOptional()) {
                        log.error( "Unable to inject: " + injectable.getName() );
                    }
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException( e );
                }
            }
        }
    }

    protected void installLightweightFactory(DeploymentPhaseContext phaseContext, PhpRuntimeFactory factory) {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        PhpRuntimeFactoryService service = new PhpRuntimeFactoryService( factory );
        ServiceName name = CoreServices.runtimeFactoryName( unit ).append( "lightweight" );

        phaseContext.getServiceTarget().addService( name, service ).setInitialMode( Mode.ON_DEMAND ).install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private static final Logger log = Logger.getLogger( "org.porquebox.core.runtime" );

}
