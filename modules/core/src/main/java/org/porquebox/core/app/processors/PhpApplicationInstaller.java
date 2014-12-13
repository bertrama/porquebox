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

package org.porquebox.core.app.processors;

import java.util.Hashtable;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanRegistrationService;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.jmx.ObjectNameFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.app.PhpApplication;
import org.porquebox.core.app.PhpApplicationMBean;

/** Deploys a PhpApplication, primarily for JMX access.  Not very functional.
 *
 * @author bob
 */
public class PhpApplicationInstaller implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        final PhpAppMetaData phpAppMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );

        if ( phpAppMetaData == null ) {
            return;
        }

        String mbeanName = ObjectNameFactory.create( "porquebox.apps", new Hashtable<String, String>() {
            {
                put( "name", phpAppMetaData.getApplicationName() );
            }
        } ).toString();

        ServiceName serviceName = unit.getServiceName();

        PhpApplication application = new PhpApplication();
        application.setEnvironmentName( phpAppMetaData.getEnvironmentName() );
        application.setRootPath( phpAppMetaData.getRoot().getAbsolutePath() );
        application.setName( phpAppMetaData.getApplicationName() );

        ServiceName mbeanServiceName = serviceName.append( "mbean" );
        MBeanRegistrationService<PhpApplicationMBean> mbeanService = new MBeanRegistrationService<PhpApplicationMBean>( mbeanName, mbeanServiceName, new ImmediateValue<PhpApplicationMBean>( application ) );
        phaseContext.getServiceTarget().addService( mbeanServiceName, mbeanService )
                .addDependency( DependencyType.OPTIONAL, MBeanServerService.SERVICE_NAME, MBeanServer.class, mbeanService.getMBeanServerInjector() )
                .setInitialMode( Mode.PASSIVE )
                .install();

    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
