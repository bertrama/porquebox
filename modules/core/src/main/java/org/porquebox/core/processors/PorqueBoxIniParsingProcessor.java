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

package org.porquebox.core.processors;

import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;
import org.projectodd.polyglot.core.processors.AbstractParsingProcessor;
import org.porquebox.core.PorqueBoxMetaData;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.Configuration;

public class PorqueBoxIniParsingProcessor extends AbstractParsingProcessor {

    public static final String PORQUEBOX_INI_FILE = "porquebox.ini";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        ResourceRoot resourceRoot = unit.getAttachment( Attachments.DEPLOYMENT_ROOT );
        VirtualFile root = resourceRoot.getRoot();

        VirtualFile file = getMetaDataFile( root, PORQUEBOX_INI_FILE );

        if (file != null) {
            Map<String, Object> data = null;
            try {
                Configuration conf =  new HierarchicalINIConfiguration( file.asFileURL() );
                //TODO: Import configuration into data Map.
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Error processing ini: ", e);
            }

            PorqueBoxMetaData metaData = new PorqueBoxMetaData( data );
            PorqueBoxMetaData externalMetaData = unit.getAttachment( PorqueBoxMetaData.ATTACHMENT_KEY );
            if (externalMetaData != null) {
                metaData = externalMetaData.overlayOnto( metaData );
            }

            try {
                metaData.validate();
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Configuration validation failed: ", e);
            }
            unit.putAttachment( PorqueBoxMetaData.ATTACHMENT_KEY, metaData );
        }

    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.core" );

}
