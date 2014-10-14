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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.porquebox.core.PorqueBoxMetaData;
import org.porquebox.core.as.CoreServices;
import org.porquebox.core.app.PhpAppMetaData;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.logging.Logger;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import org.projectodd.polyglot.core.processors.AbstractParsingProcessor;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.ConfigurationRuntimeException;

public class AppBaconIniParsingProcessor  extends AbstractParsingProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        PorqueBoxMetaData metaData = null;
        File rootFile = null;
        ResourceRoot appRoot = null;
        try {
            VirtualFile appBaconIni = getFile( unit );
            if (appBaconIni == null) {
                return;
            }
            metaData = new PorqueBoxMetaData(new HierarchicalINIConfiguration(appBaconIni.asFileURL()));
            rootFile = metaData.getApplicationRootFile();

            if (rootFile != null) {
                VirtualFile root = VFS.getChild( rootFile.toURI() );
                if (!root.exists()) {
                    throw new DeploymentUnitProcessingException( "Application root does not exist: " + root.toURL().toExternalForm() );
                }

                if (root.exists() && !root.isDirectory()) {
                    // Expand the referenced root if it's not a directory (ie
                    // .bacon archive)
                    final Closeable closable = VFS.mountZipExpanded( root, root, TempFileProviderService.provider() );
                    final MountHandle mountHandle = new MountHandle( closable );
                    appRoot = new ResourceRoot( root, mountHandle );

                } else {
                    appRoot = new ResourceRoot( root, null );
                }
                appRoot.putAttachment( Attachments.INDEX_RESOURCE_ROOT, false );
                unit.putAttachment( Attachments.DEPLOYMENT_ROOT, appRoot );
            }
            else {
                log.infof( "Rootless deployment detected: %s", unit.getName() );
                DeploymentUtils.markUnitAsRootless( unit );
            }
        }
        catch (Exception e) {
            throw new DeploymentUnitProcessingException( e );
        }
        unit.putAttachment( PorqueBoxMetaData.ATTACHMENT_KEY, metaData );

        PhpAppMetaData phpAppMetaData = new PhpAppMetaData( unit.getName() );
        phpAppMetaData.setRoot( rootFile );
        phpAppMetaData.setEnvironmentName( metaData.getApplicationEnvironment() );
        phpAppMetaData.attachTo( unit );
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    protected VirtualFile getFile(DeploymentUnit unit) throws DeploymentUnitProcessingException, IOException {
        List<VirtualFile> matches = new ArrayList<VirtualFile>();

        ResourceRoot resourceRoot = unit.getAttachment( Attachments.DEPLOYMENT_ROOT );
        VirtualFile root = resourceRoot.getRoot();

        if (this.baconFilter.accepts( root )) {
            return root;
        }

        matches = root.getChildren( this.baconFilter );

        if (matches.size() > 1) {
            throw new DeploymentUnitProcessingException( "Multiple application ini files found in " + root );
        }

        VirtualFile file = null;
        if (matches.size() == 1) {
            file = matches.get( 0 );
        }

        return file;
    }

    private VirtualFileFilter baconFilter = (new VirtualFileFilter() {
        public boolean accepts(VirtualFile file) {
            return file.getName().endsWith( "-bacon.ini" );
        }
    });

    private static final Logger log = Logger.getLogger( "org.porquebox.core" );
}
