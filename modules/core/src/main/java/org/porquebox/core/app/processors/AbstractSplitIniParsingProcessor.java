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

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;
import org.projectodd.polyglot.core.processors.AbstractParsingProcessor;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.projectodd.polyglot.core.util.DeprecationLogger;
import org.porquebox.core.PorqueBoxMetaData;
import org.porquebox.core.app.PhpAppMetaData;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

/**
 * Abstract deployer base-class supporting <code>porquebox.ini</code> sectional
 * parsing.
 * 
 * <p>
 * For a given subsystem 'foo', a porquebox.ini section named 'foo:' can
 * configure it or optionally (deprecated) a file named foo.ini.
 * </p>
 * 
 * @author Bob McWhirter
 */
public abstract class AbstractSplitIniParsingProcessor extends AbstractParsingProcessor {

    /** Name of the section within porquebox.ini. */
    private String sectionName;

    /** Optional file-name for NAME.ini parsing separate from porquebox.ini. */
    private String fileName;

    /** Does this deploy unit support a standalone *.ini descriptor? */
    private boolean supportsStandalone = true;

    private boolean standaloneDeprecated = true;

    /** Does this deploy support a *-<name>.ini format? */
    private boolean supportsSuffix = false;

    /** Is the app root required for this deploy unit? **/
    private boolean supportsRootless = false;

    private static final Logger log = Logger.getLogger( "org.porquebox.core" );

    public static void logDeprecation(DeploymentUnit unit, String message) {
        DeprecationLogger.getLogger( unit ).append( message );
    }

    public AbstractSplitIniParsingProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        ResourceRoot resourceRoot = unit.getAttachment( Attachments.DEPLOYMENT_ROOT );
        PorqueBoxMetaData globalMetaData = unit.getAttachment( PorqueBoxMetaData.ATTACHMENT_KEY );

        Object data = null;

        if (globalMetaData != null) {
            data = globalMetaData.getSection( getSectionName() );
        }

        VirtualFile root = resourceRoot.getRoot();
        if (data == null && isSupportsStandalone()) {
            VirtualFile metaDataFile = getMetaDataFile( root, getFileName() );

            if ((metaDataFile == null || !metaDataFile.exists()) && this.supportsSuffix) {
                List<VirtualFile> matches = getMetaDataFileBySuffix( root, "-" + getFileName() );
                if (!matches.isEmpty()) {
                    if (matches.size() > 1) {
                        log.warn( "Multiple matches: " + matches );
                    }
                    metaDataFile = matches.get( 0 );
                }
            }

            if ((metaDataFile != null) && metaDataFile.exists()) {
                if (!metaDataFile.equals( root ) && this.standaloneDeprecated) {
                    logDeprecation( unit, "Usage of " + getFileName() + " is deprecated.  Please use porquebox.ini." );
                }
                try {
                   //TODO import Configuration into data.
                   Configuration conf = new HierarchicalINIConfiguration(metaDataFile.asFileURL());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException( "Error processing yaml: ", e );
                }
            }
        } else {

            // If data has been specified for this section, and the deployment
            // is rootless,
            // but rootlessness is not supported, then error out.
            PhpAppMetaData phpMetaData = unit.getAttachment( PhpAppMetaData.ATTACHMENT_KEY );
            if (data != null && !isSupportsRootless() && phpMetaData != null && DeploymentUtils.isUnitRootless( unit )) {
                throw new DeploymentUnitProcessingException( String.format(
                        "Error processing deployment %s: The section %s requires an app root to be specified, but none has been provided.",
                        unit.getName(), getSectionName() ) );
            }
        }

        if (data == null) {
            return;
        }

        try {
            parse( unit, data );
        } catch (DeploymentUnitProcessingException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeploymentUnitProcessingException( e );
        }
    }

    public String getFileName() {
        if (this.fileName != null) {
            return this.fileName;
        }

        return getSectionName() + ".ini";
    }

    public String getSectionName() {
        return this.sectionName;
    }

    public boolean isSupportsRootless() {
        return supportsRootless;
    }

    public boolean isStandaloneDeprecated() {
        return this.standaloneDeprecated;
    }

    public boolean isSupportsStandalone() {
        return this.supportsStandalone;
    }

    public boolean isSupportsSuffix() {
        return this.supportsSuffix;
    }

    protected abstract void parse(DeploymentUnit unit, Object data) throws Exception;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setSupportsRootless(boolean supportsRootless) {
        this.supportsRootless = supportsRootless;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public void setStandaloneDeprecated(boolean deprecated) {
        this.standaloneDeprecated = deprecated;
    }

    public void setSupportsStandalone(boolean supports) {
        this.supportsStandalone = supports;
    }

    public void setSupportsSuffix(boolean supports) {
        this.supportsSuffix = supports;
    }

}
