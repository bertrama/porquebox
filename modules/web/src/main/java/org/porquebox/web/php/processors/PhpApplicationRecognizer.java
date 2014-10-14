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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;
import org.projectodd.polyglot.core.processors.FileLocatingProcessor;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.web.php.PhpMetaData;

public class PhpApplicationRecognizer extends FileLocatingProcessor {

    public static final String DEFAULT_PHP_PATH = "index.php";

    public PhpApplicationRecognizer() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }
        ResourceRoot resourceRoot = unit.getAttachment( Attachments.DEPLOYMENT_ROOT );
        VirtualFile root = resourceRoot.getRoot();

        if (isPhpApplication( root )) {
            PhpMetaData phpAppMetaData = unit.getAttachment( PhpMetaData.ATTACHMENT_KEY );

            if (phpAppMetaData == null) {
                phpAppMetaData = new PhpMetaData();
                phpAppMetaData.setExecScriptLocation( DEFAULT_PHP_PATH );
                phpAppMetaData.attachTo( unit );
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }


     static boolean isPhpApplication(VirtualFile file) {
        boolean result = hasAnyOf( file, DEFAULT_PHP_PATH );
        return result;
    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.web.php" );
}
