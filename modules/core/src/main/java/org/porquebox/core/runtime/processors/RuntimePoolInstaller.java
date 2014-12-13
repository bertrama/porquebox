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

import java.util.Hashtable;
import java.util.List;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanRegistrationService;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.jmx.ObjectNameFactory;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.projectodd.polyglot.core.as.DeploymentNotifier;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.as.CoreServices;
import org.porquebox.core.runtime.BasicPhpRuntimePoolMBean;
import org.porquebox.core.runtime.DefaultPhpRuntimePool;
import org.porquebox.core.runtime.PoolMetaData;
import org.porquebox.core.runtime.RestartablePhpRuntimePool;
import org.porquebox.core.runtime.RestartablePhpRuntimePoolMBean;
import org.porquebox.core.runtime.PhpRuntimeFactory;
import org.porquebox.core.runtime.PhpRuntimeFactoryPoolService;
import org.porquebox.core.runtime.PhpRuntimePoolStartService;
import org.porquebox.core.runtime.SharedPhpRuntimePool;

/**
 * <pre>
 * Stage: REAL
 *    In: PoolMetaData, DeployerPhp
 *   Out: PhpRuntimePool
 * </pre>
 *
 * Creates the proper PhpRuntimePool as specified by the PoolMetaData
 */
public class RuntimePoolInstaller implements DeploymentUnitProcessor {

    public RuntimePoolInstaller() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        List<PoolMetaData> allAttachments = unit.getAttachmentList( PoolMetaData.ATTACHMENTS_KEY );

        for (PoolMetaData each : allAttachments) {
            deploy( phaseContext, each );
        }
    }

    protected void deploy(DeploymentPhaseContext phaseContext, final PoolMetaData poolMetaData) {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final PhpAppMetaData phpAppMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData == null) {
            return;
        }

        if (poolMetaData.isShared()) {
            SharedPhpRuntimePool pool = new SharedPhpRuntimePool();

            pool.setName( poolMetaData.getName() );
            pool.setDeferUntilRequested( poolMetaData.isDeferUntilRequested() );

            RestartablePhpRuntimePool restartablePool = new RestartablePhpRuntimePool( pool );
            PhpRuntimeFactoryPoolService service = new PhpRuntimeFactoryPoolService( restartablePool );

            ServiceName name = CoreServices.runtimePoolName( unit, pool.getName() );

            phaseContext.getServiceTarget().addService( name, service )
                    .addDependency( CoreServices.runtimeFactoryName( unit ), PhpRuntimeFactory.class, service.getPhpRuntimeFactoryInjector() )
                    .addDependency( CoreServices.appNamespaceContextSelector( unit ), NamespaceContextSelector.class, service.getNamespaceContextSelectorInjector() )
                    .install();

            unit.addToAttachmentList( DeploymentNotifier.SERVICES_ATTACHMENT_KEY, name );

            ServiceName startName = CoreServices.runtimeStartPoolName( unit, pool.getName() );
            phaseContext.getServiceTarget().addService( startName, new PhpRuntimePoolStartService( pool ) )
                    .addDependency( name )
                    .setInitialMode( Mode.PASSIVE )
                    .install();

            String mbeanName = ObjectNameFactory.create( "porquebox.pools", new Hashtable<String, String>() {
                {
                    put( "app", phpAppMetaData.getApplicationName() );
                    put( "name", poolMetaData.getName() );
                }
            } ).toString();

            ServiceName mbeanServiceName = name.append( "mbean" );
            MBeanRegistrationService<BasicPhpRuntimePoolMBean> mbeanService = new MBeanRegistrationService<BasicPhpRuntimePoolMBean>( mbeanName, mbeanServiceName );
            phaseContext.getServiceTarget().addService( mbeanServiceName, mbeanService )
                    .addDependency( DependencyType.OPTIONAL, MBeanServerService.SERVICE_NAME, MBeanServer.class, mbeanService.getMBeanServerInjector() )
                    .addDependency( name, BasicPhpRuntimePoolMBean.class, mbeanService.getValueInjector() )
                    .setInitialMode( Mode.PASSIVE )
                    .install();

        } else {
            DefaultPhpRuntimePool pool = new DefaultPhpRuntimePool();

            pool.setName( poolMetaData.getName() );
            pool.setMinimumInstances( poolMetaData.getMinimumSize() );
            pool.setMaximumInstances( poolMetaData.getMaximumSize() );
            pool.setDeferUntilRequested( poolMetaData.isDeferUntilRequested() );

            RestartablePhpRuntimePool restartablePool = new RestartablePhpRuntimePool( pool );
            PhpRuntimeFactoryPoolService service = new PhpRuntimeFactoryPoolService( restartablePool );

            ServiceName name = CoreServices.runtimePoolName( unit, pool.getName() );
            phaseContext.getServiceTarget().addService( name, service )
                    .addDependency( CoreServices.runtimeFactoryName( unit ), PhpRuntimeFactory.class, service.getPhpRuntimeFactoryInjector() )
                    .addDependency( CoreServices.appNamespaceContextSelector( unit ), NamespaceContextSelector.class, service.getNamespaceContextSelectorInjector() )
                    .install();

            unit.addToAttachmentList( DeploymentNotifier.SERVICES_ATTACHMENT_KEY, name );

            phaseContext.getServiceTarget().addService( name.append( "START" ), new PhpRuntimePoolStartService( pool ) )
                    .addDependency( name )
                    .setInitialMode( Mode.PASSIVE )
                    .install();

            String mbeanName = ObjectNameFactory.create( "porquebox.pools", new Hashtable<String, String>() {
                {
                    put( "app", phpAppMetaData.getApplicationName() );
                    put( "name", poolMetaData.getName() );
                }
            } ).toString();

            ServiceName mbeanServiceName = name.append( "mbean" );
            MBeanRegistrationService<RestartablePhpRuntimePoolMBean> mbeanService = new MBeanRegistrationService<RestartablePhpRuntimePoolMBean>( mbeanName, mbeanServiceName );
            phaseContext.getServiceTarget().addService( mbeanServiceName, mbeanService )
                    .addDependency( DependencyType.OPTIONAL, MBeanServerService.SERVICE_NAME, MBeanServer.class, mbeanService.getMBeanServerInjector() )
                    .addDependency( name, RestartablePhpRuntimePoolMBean.class, mbeanService.getValueInjector() )
                    .setInitialMode( Mode.PASSIVE )
                    .install();
        }
    }

    @Override
    public void undeploy(org.jboss.as.server.deployment.DeploymentUnit context) {

    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.core.pool" );
}
