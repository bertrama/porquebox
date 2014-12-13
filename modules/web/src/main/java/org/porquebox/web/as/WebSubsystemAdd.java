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

package org.porquebox.web.as;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.catalina.connector.Connector;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.projectodd.polyglot.web.WebConnectorConfigService;
import org.porquebox.web.component.processors.PhpApplicationComponentResolverInstaller;
import org.porquebox.web.php.processors.PhpApplicationRecognizer;
import org.porquebox.web.php.processors.PhpRuntimeProcessor;
import org.porquebox.web.php.processors.PhpWebApplicationInstaller;
import org.porquebox.web.php.processors.PhpWebApplicationDefaultsProcessor;
import org.porquebox.web.php.processors.WebRuntimePoolProcessor;
import org.porquebox.web.php.processors.WebIniParsingProcessor;
import org.porquebox.web.php.processors.PhpVirtualHostInstaller;

class WebSubsystemAdd extends AbstractBoottimeAddStepHandler {

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        context.addStep( new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                addDeploymentProcessors( processorTarget );
            }
        }, OperationContext.Stage.RUNTIME );

        try {
            addWebConnectorConfigServices( context, verificationHandler, newControllers );
        } catch (Exception e) {
            throw new OperationFailedException( e, null );
        }

    }

    protected void addDeploymentProcessors(final DeploymentProcessorTarget processorTarget) {
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 1000, new PhpApplicationRecognizer() );
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 1020, new WebIniParsingProcessor() );
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 1030, new PhpWebApplicationDefaultsProcessor() );
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 1040, new WarImposterProcessor() );
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT + 10, new PhpWebApplicationInstaller() );
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT + 30, new PhpRuntimeProcessor() );

        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, 1, new WebDependenciesProcessor() );

        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, 100, new WebRuntimePoolProcessor() );

        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, 120, new PhpApplicationComponentResolverInstaller() );
        processorTarget.addDeploymentProcessor( WebExtension.SUBSYSTEM_NAME, Phase.INSTALL, 2100, new PhpVirtualHostInstaller() );
    }

    protected void addWebConnectorConfigServices(final OperationContext context,
            ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws Exception {
        for (Enumeration<?> e = System.getProperties().propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            Matcher matcher = maxThreadsPattern.matcher( key );
            if (matcher.matches()) {
                String connectorName = matcher.group( 1 );
                int maxThreads = Integer.parseInt( System.getProperty( key ) );
                addWebConnectorConfigService( context, verificationHandler, newControllers, connectorName, maxThreads );
            }
        }
    }

    protected void addWebConnectorConfigService(final OperationContext context,
            ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers,
            String connectorName, int maxThreads) throws Exception {
        WebConnectorConfigService service = new WebConnectorConfigService();
        service.setMaxThreads( maxThreads );
        newControllers.add( context.getServiceTarget().addService( WebServices.WEB_CONNECTOR_CONFIG.append( connectorName ), service )
                .addDependency( WebSubsystemServices.JBOSS_WEB_CONNECTOR.append( connectorName ), Connector.class, service.getConnectorInjector() )
                .addListener( verificationHandler )
                .setInitialMode( Mode.ACTIVE )
                .install() );
    }

    static ModelNode createOperation(ModelNode address) {
        final ModelNode subsystem = new ModelNode();
        subsystem.get( OP ).set( ADD );
        subsystem.get( OP_ADDR ).set( address );
        return subsystem;
    }

    static final WebSubsystemAdd ADD_INSTANCE = new WebSubsystemAdd();
    static final Logger log = Logger.getLogger( "org.porquebox.web.as" );
    static final Pattern maxThreadsPattern = Pattern.compile( "org\\.porquebox\\.web\\.(.+)\\.maxThreads" );

}
