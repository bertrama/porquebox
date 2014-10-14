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

import java.io.File;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.PorqueBoxMetaData;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.processors.AbstractSplitIniParsingProcessor;

/**
 * <pre>
 * Stage: PARSE
 *    In: web.yml
 *   Out: RackApplicationMetaData
 * </pre>
 * 
 * Internal deployment descriptor for setting vhosts, web context, and static
 * content dir
 * 
 */
public class ApplicationIniParsingProcessor extends AbstractSplitIniParsingProcessor {

    public ApplicationIniParsingProcessor() {
        setSectionName( "application" );
        setSupportsStandalone( false );
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (DeploymentUtils.isUnitRootless( phaseContext.getDeploymentUnit() )) {
            return;
        }
        super.deploy( phaseContext );
    }

    @SuppressWarnings("unchecked")
    public void parse(DeploymentUnit unit, Object dataObj) throws Exception {
        Map<String, String> app = (Map<String, String>) dataObj;

        PhpAppMetaData appMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );

        if (appMetaData == null) {
            appMetaData = new PhpAppMetaData( unit.getName() );
            appMetaData.attachTo( unit );
        }

        if (appMetaData.getRoot() == null) {
            File root = PorqueBoxMetaData.findApplicationRootFile( app );

            if (root != null ) {
                appMetaData.setRoot( root );
            }
        }

        if (appMetaData.getEnvironmentName() == null) {
            String env = PorqueBoxMetaData.findApplicationEnvironment( app );

            if (env != null && !env.trim().equals( "" )) {
                appMetaData.setEnvironmentName( env.trim() );
            }

        }
    }

}
