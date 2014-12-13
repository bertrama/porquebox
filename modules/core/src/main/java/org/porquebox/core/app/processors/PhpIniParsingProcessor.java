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

import java.util.Map;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.processors.AbstractSplitIniParsingProcessor;
import org.porquebox.core.runtime.PhpRuntimeMetaData;

/**
 * <pre>
 * Stage: PARSE
 *    In: PhpRuntimeMetaData
 *   Out: PhpRuntimeMetaData
 * </pre>
 *
 * Parsing deployer for {@code php.ini}.
 *
 * @author Bob McWhirter <bmcwhirt@redhat.com>
 */
public class PhpIniParsingProcessor extends AbstractSplitIniParsingProcessor {

    public PhpIniParsingProcessor() {
        setSectionName( "php" );
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

        PhpRuntimeMetaData runtimeMetaData = unit.getAttachment( PhpRuntimeMetaData.ATTACHMENT_KEY );
        if (runtimeMetaData == null) {
            log.debug( "Initializing php runtime metadata: " + unit );
            runtimeMetaData = new PhpRuntimeMetaData();
            unit.putAttachment( PhpRuntimeMetaData.ATTACHMENT_KEY, runtimeMetaData );
        }

        Map<String, Object> config = (Map<String, Object>) dataObj;

        Object debug = config.get( "debug" );
        if ("false".equals( "" + debug )) {
            runtimeMetaData.setDebug( false );
        } else if ("true".equals( "" + debug )) {
            runtimeMetaData.setDebug( true );
        }

        Object interactive = config.get( "interactive" );
        if ("false".equals( "" + interactive )) {
            runtimeMetaData.setInteractive( false );
        } else if ("true".equals( "" + interactive )) {
            runtimeMetaData.setInteractive( true );
        }

        Object profileApi = config.get( "profile_api" );
        if (profileApi != null) {
            runtimeMetaData.setProfileApi( (Boolean) profileApi );
        }
    }

    private static final Logger log = Logger.getLogger( "org.porquebox.core.app.php" );
}
