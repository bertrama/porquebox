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

package org.porquebox.core.datasource.processors;

import java.util.Map;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.porquebox.core.as.CoreServices;
import org.porquebox.core.datasource.DatabaseMetaData;
import org.porquebox.core.processors.AbstractSplitIniParsingProcessor;


/**
 * This class is used to read rails database.yml, not a database section
 * of porquebox.yml.
 *
 */
public class DatabaseIniParsingProcessor extends AbstractSplitIniParsingProcessor {

    public DatabaseIniParsingProcessor() {
        setSectionName( "database" );
        setSupportsStandalone( true );
        setStandaloneDeprecated( false );
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        try {
            //this.ruby = (GlobalRuby) phaseContext.getServiceRegistry().getRequiredService( CoreServices.GLOBAL_RUBY ).getValue();
        } catch (org.jboss.msc.service.ServiceNotFoundException e) {
            log.warn ("No GlobalRuby available to parse ERB in database.yml");
        }
        try {
            super.deploy( phaseContext );
        } catch (DeploymentUnitProcessingException ignored) {
            log.warnf( "Failed to parse database.yml - XA will not be enabled for %s", phaseContext.getDeploymentUnit() );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void parse(DeploymentUnit unit, Object data) throws Exception {
        Map<String, Object> configuration = (Map<String, Object>) data;
        configuration.put("xa", (Object) true);

        DatabaseMetaData md = new DatabaseMetaData( "configuration", configuration );
        unit.addToAttachmentList( DatabaseMetaData.ATTACHMENTS, md );
    }

    private static final Logger log = Logger.getLogger( DatabaseIniParsingProcessor.class );
}
