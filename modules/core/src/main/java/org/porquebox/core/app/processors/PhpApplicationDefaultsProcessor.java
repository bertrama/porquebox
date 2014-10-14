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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.processors.AbstractSplitIniParsingProcessor;
import org.jboss.logging.Logger;

public class PhpApplicationDefaultsProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        PhpAppMetaData phpAppMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData != null) {
            if (phpAppMetaData.getEnvironmentName() != null) {
                AbstractSplitIniParsingProcessor.logDeprecation( unit, "Setting the application environment via the 'application:' section is deprecated. Set RAILS_ROOT or RACK_ROOT via the 'environment:' section instead." );
            }
            phpAppMetaData.extractAppEnvironment();
            phpAppMetaData.applyDefaults();
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
    static final Logger log = Logger.getLogger( "org.porquebox.core.app.processors" );
}
