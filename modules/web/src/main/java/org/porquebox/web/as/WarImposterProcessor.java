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

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.web.php.PhpMetaData;

/**
 * Ensure our Php applications look like a .war deployment to the AS
 * so it parses WEB-INF/web.xml and sets up other necessary bits for
 * web deployments
 *
 * @author bbrowning
 *
 */
public class WarImposterProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }

        PhpMetaData phpAppMetaData = unit.getAttachment( PhpMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData == null) {
            return;
        }

        DeploymentTypeMarker.setType( DeploymentType.WAR, unit );
        WarMetaData warMetaData = new WarMetaData();
        unit.putAttachment( WarMetaData.ATTACHMENT_KEY, warMetaData );
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // nothing to do
    }
    static final Logger log = Logger.getLogger( "org.porquebox.web.as" );

}
