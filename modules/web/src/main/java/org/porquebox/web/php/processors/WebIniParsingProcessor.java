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

package org.porquebox.web.php.processors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.projectodd.polyglot.core.util.TimeInterval;
import org.porquebox.core.processors.AbstractSplitIniParsingProcessor;
import org.porquebox.web.php.PhpMetaData;

public class WebIniParsingProcessor extends AbstractSplitIniParsingProcessor {

    public WebIniParsingProcessor() {
        setSectionName( "web" );
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

        PhpMetaData phpAppMetaData = unit.getAttachment( PhpMetaData.ATTACHMENT_KEY );

        if (phpAppMetaData == null) {
            phpAppMetaData = new PhpMetaData();
            phpAppMetaData.attachTo( unit );
        }

        Map<String, Object> webData = (Map<String, Object>) dataObj;

        phpAppMetaData.setContextPath( (String) webData.get( "context" ) );
        phpAppMetaData.setStaticPathPrefix( (String) webData.get( "static" ) );
        phpAppMetaData.setExecute((String) webData.get("execute"));
        phpAppMetaData.setExclude((String) webData.get("exclude"));

        // TODO: Provide sensible defaults for context (/), static (/), exclude, and execute.

        if (webData.get( "default" ) != null) {
            phpAppMetaData.setExecScriptLocation( (String) webData.get( "default" ) );
        }

        Object hosts = webData.get( "host" );

        if (hosts instanceof List) {
            List<String> list = (List<String>) hosts;
            for (String each : list) {
                phpAppMetaData.addHost( each );
            }
        } else {
            phpAppMetaData.addHost( (String) hosts );
        }

        String timeoutStr = null;

        if (webData.containsKey( "session-timeout" )) {
            timeoutStr = webData.get( "session-timeout" ).toString();
        } else if (webData.containsKey( "session_timeout" )) {
            timeoutStr = webData.get( "session_timeout" ).toString();
        }

        phpAppMetaData.setSessionTimeout( TimeInterval.parseInterval( timeoutStr, TimeUnit.MINUTES ) );
    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.web.php" );

}
