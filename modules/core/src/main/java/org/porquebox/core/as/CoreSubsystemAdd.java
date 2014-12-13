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

package org.porquebox.core.as;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.management.MBeanServer;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.stdio.StdioContext;
import org.jboss.as.jmx.ObjectNameFactory;
import org.jboss.as.jmx.MBeanRegistrationService;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.logging.Logger;
// TODO: Is this really how this works? (Only one class in the same stage and priority, such that I have to subclass this, or does it do something I'm missing?
// import org.projectodd.polyglot.core.processors.DescriptorRootMountProcessor;
import org.porquebox.core.processors.DescriptorRootMountProcessor;
import org.projectodd.polyglot.core.processors.ArchiveStructureProcessor;

import org.porquebox.PorqueBox;
import org.porquebox.PorqueBoxMBean;
import org.porquebox.PorqueBoxStdioContextSelector;
import org.porquebox.core.GlobalPhp;
import org.porquebox.core.app.processors.AppBaconIniParsingProcessor;
import org.porquebox.core.app.processors.ApplicationIniParsingProcessor;
import org.porquebox.core.app.processors.EnvironmentIniParsingProcessor;
import org.porquebox.core.app.processors.PhpApplicationDefaultsProcessor;
import org.porquebox.core.app.processors.PhpApplicationInstaller;
import org.porquebox.core.app.processors.PhpApplicationRecognizer;
import org.porquebox.core.app.processors.PhpIniParsingProcessor;
import org.porquebox.core.injection.analysis.InjectableHandlerRegistry;
import org.porquebox.core.injection.analysis.RuntimeInjectionAnalyzer;
import org.porquebox.core.injection.processors.PredeterminedInjectableProcessor;
import org.porquebox.core.pool.processors.PoolingIniParsingProcessor;
import org.porquebox.core.processors.PorqueBoxPhpProcessor;
import org.porquebox.core.runtime.RuntimeRestartScanner;
import org.porquebox.core.runtime.processors.BasePhpRuntimeInstaller;
import org.porquebox.core.runtime.processors.PhpRuntimeFactoryInstaller;
import org.porquebox.core.runtime.processors.RuntimePoolInstaller;
import org.porquebox.core.runtime.processors.RuntimeRestartProcessor;
import org.porquebox.core.runtime.processors.PhpNamespaceContextSelectorProcessor;

import org.porquebox.core.datasource.DataSourceServices;
import org.porquebox.core.datasource.processors.DatabaseProcessor;
import org.porquebox.core.datasource.processors.DatabaseIniParsingProcessor;


class CoreSubsystemAdd extends AbstractBoottimeAddStepHandler {

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get( "injector" ).setEmptyObject();
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        final InjectableHandlerRegistry registry = new InjectableHandlerRegistry();

        try {
            addCoreServices( context, verificationHandler, newControllers, registry );
            addPorqueBoxStdioContext();
            // TODO: Does Quercus have this issue?
            // workaroundJRubyConstantSetRaceCondition();
        } catch (Exception e) {
            throw new OperationFailedException( e, null );
        }

        context.addStep( new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                addDeploymentProcessors( processorTarget, registry );
            }
        }, OperationContext.Stage.RUNTIME );

    }

    protected void addCoreServices(final OperationContext context, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers,
            InjectableHandlerRegistry registry) throws Exception {
        addPorqueBoxService( context, verificationHandler, newControllers );
        addGlobalPhpServices( context, verificationHandler, newControllers );
        addInjectionServices( context, verificationHandler, newControllers, registry );
        addRuntimeRestartScannerService( context, verificationHandler, newControllers );
    }

    protected void addPorqueBoxStdioContext() {
        // Grab the existing AS7 StdioContext
        final StdioContext defaultContext = StdioContext.getStdioContext();
        // Uninstall to reset System.in, .out, .err to default values
        StdioContext.uninstall();
        // Create debug StdioContext based on System streams
         final StdioContext debugContext = StdioContext.create( System.in, System.out, System.err );
        PorqueBoxStdioContextSelector selector = new PorqueBoxStdioContextSelector( defaultContext, debugContext );

        StdioContext.install();
        StdioContext.setStdioContextSelector( selector );
    }

    protected void addDeploymentProcessors(final DeploymentProcessorTarget processorTarget, final InjectableHandlerRegistry registry) {
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 0, new DescriptorRootMountProcessor( "-bacon.ini" ) );
        // TODO: Consider this for later.
        // processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 0, new ArchiveStructureProcessor( ".bacon" ) );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 800, new AppBaconIniParsingProcessor() );
        // TODO: Consider this for later.
        // processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 810, new AppJarScanningProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 900, new PhpApplicationRecognizer() );
        // TODO: Consider this for later.
        // processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 910, new PorqueBoxIniParsingProcessor() );

        // TODO: Consider a php equivilant for later.
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 920, new PorqueBoxPhpProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 930, new ApplicationIniParsingProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 940, new EnvironmentIniParsingProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 950, new PoolingIniParsingProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 960, new PhpIniParsingProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 970, new PhpApplicationDefaultsProcessor() );
        // TODO: implement the DataSourceServices / DatabaseIniParsingProcessor
        if (DataSourceServices.enabled) {
            processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, 980, new DatabaseIniParsingProcessor() );
        }

        // TODO: Implement the ApplicationExploder.
        // processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.PARSE, 100, new ApplicationExploder() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.PARSE, 4000, new BasePhpRuntimeInstaller() );

        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, 0, new CoreDependenciesProcessor() );
        // TODO: Implement the PredeterminedInjectableProcessor.
        // processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, 1000, new PredeterminedInjectableProcessor( registry ) );
        // TODO: Implement the CorePredeterminedInjectableInstaller.
        // processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, 1001, new CorePredeterminedInjectableInstaller() );
        // TODO: Implement the LoggingPropertiesWorkaroundProcessor.
        // processorTarget.addDeploymentProcessor(  CoreExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, 1, new LoggingPropertiesWorkaroundProcessor() );
        // TODO: Implement the PhpNamespaceContextSelectorProcessor.
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, 110, new PhpNamespaceContextSelectorProcessor() );
        // TODO: Implement the DatabaseProcessor.
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, 5000, new DatabaseProcessor() );

        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.INSTALL, 0, new PhpRuntimeFactoryInstaller() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.INSTALL, 10, new RuntimePoolInstaller() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.INSTALL, 20, new RuntimeRestartProcessor() );
        processorTarget.addDeploymentProcessor( CoreExtension.SUBSYSTEM_NAME, Phase.INSTALL, 9000, new PhpApplicationInstaller() );
    }

    @SuppressWarnings("serial")
    protected void addPorqueBoxService(final OperationContext context, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws IOException {
        PorqueBox porqueBox = new PorqueBox();

        newControllers.add( context.getServiceTarget().addService( CoreServices.PORQUEBOX, porqueBox )
                .setInitialMode( Mode.ACTIVE )
                .addListener( verificationHandler )
                .install() );

        String mbeanName = ObjectNameFactory.create( "porquebox", new Hashtable<String, String>() {
            {
                put( "type", "version" );
            }
        } ).toString();

        ServiceName mbeanServiceName = CoreServices.PORQUEBOX.append( "mbean" );
        MBeanRegistrationService<PorqueBoxMBean> mbeanService = new MBeanRegistrationService<PorqueBoxMBean>( mbeanName, mbeanServiceName );
        newControllers.add( context.getServiceTarget().addService( mbeanServiceName, mbeanService )
                .addDependency( DependencyType.OPTIONAL, MBeanServerService.SERVICE_NAME, MBeanServer.class, mbeanService.getMBeanServerInjector() )
                .addDependency( CoreServices.PORQUEBOX, PorqueBoxMBean.class, mbeanService.getValueInjector() )
                .addListener( verificationHandler )
                .setInitialMode( Mode.PASSIVE )
                .install() );
    }

    protected void addRuntimeRestartScannerService(final OperationContext context, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws IOException {
        RuntimeRestartScanner service = new RuntimeRestartScanner();
        newControllers.add( context.getServiceTarget().addService( CoreServices.RUNTIME_RESTART_SCANNER, service)
                .addListener( verificationHandler )
                .install() );
    }

    protected void addInjectionServices(final OperationContext context, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers,
            InjectableHandlerRegistry registry) {

        newControllers.add( context.getServiceTarget().addService( CoreServices.INJECTABLE_HANDLER_REGISTRY, registry )
                .addListener( verificationHandler )
                .setInitialMode( Mode.PASSIVE )
                .install() );

        RuntimeInjectionAnalyzer runtimeInjectionAnalyzer = new RuntimeInjectionAnalyzer();
        Service<RuntimeInjectionAnalyzer> runtimeService = new ValueService<RuntimeInjectionAnalyzer>( new ImmediateValue<RuntimeInjectionAnalyzer> ( runtimeInjectionAnalyzer ) );
        newControllers.add( context.getServiceTarget().addService( CoreServices.RUNTIME_INJECTION_ANALYZER, runtimeService )
                .addListener( verificationHandler )
                .install() );
    }

    @SuppressWarnings("serial")
    protected void addGlobalPhpServices(final OperationContext context, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) {
        newControllers.add( context.getServiceTarget().addService( CoreServices.GLOBAL_PHP, new GlobalPhp() )
                .addListener( verificationHandler )
                .setInitialMode( Mode.ACTIVE )
                .install() );

        String mbeanName = ObjectNameFactory.create( "porquebox", new Hashtable<String, String>() {
            {
                put( "type", "runtime" );
            }
        } ).toString();
    }

    static ModelNode createOperation(ModelNode address) {
        final ModelNode subsystem = new ModelNode();
        subsystem.get( OP ).set( ADD );
        subsystem.get( OP_ADDR ).set( address );
        return subsystem;
    }

    public CoreSubsystemAdd() {
    }

    static final CoreSubsystemAdd ADD_INSTANCE = new CoreSubsystemAdd();
    static final Logger log = Logger.getLogger( "org.porquebox.core.as" );
}
