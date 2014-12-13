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

package org.porquebox.web.component.processors;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;
import org.projectodd.polyglot.core.util.DeploymentUtils;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.component.ComponentEval;
import org.porquebox.core.component.processors.ComponentResolverHelper;
import org.porquebox.web.as.WebServices;
import org.porquebox.web.component.PhpApplicationComponent;
import org.porquebox.web.php.PhpMetaData;

public class PhpApplicationComponentResolverInstaller implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentUtils.isUnitRootless( unit )) {
            return;
        }

        ServiceName serviceName = WebServices.phpApplicationComponentResolver(unit);
        PhpMetaData phpMetaData = unit.getAttachment(PhpMetaData.ATTACHMENT_KEY);

        if (phpMetaData == null) {
            return;
        }

        ResourceRoot resourceRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = resourceRoot.getRoot();

        ComponentEval instantiator = new ComponentEval();

        try {
            String execFile = phpMetaData.getExecScriptFile(root.getPhysicalFile()).getAbsolutePath();
            instantiator.setCode(getCode(phpMetaData.getExecScript(root.getPhysicalFile()), execFile));
            instantiator.setLocation(execFile);

            ComponentResolverHelper helper = new ComponentResolverHelper(phaseContext, serviceName);

            helper
                    .initializeInstantiator(instantiator)
                    .initializeResolver(PhpApplicationComponent.class, null, false, false) // Let Rack / Rails handle reloading for the web stack
                    .installService(Mode.ON_DEMAND);

        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    protected String getCode(String execScript, String execFile) {
        StringBuilder code = new StringBuilder();
        code.append("require_once('" + execFile + "');");
    /*
        code.append("require %q(rack)\n");
        if (execScript != null) {
            code.append("execScript = %q(" + execScript + ")\n");
        } else {
            code.append("execScript = File.read(%q(" + execFile + "))\n");
        }
        code.append("eval(%Q(Rack::Builder.new{\n");
        code.append("#{execScript}");
        code.append("\n}.to_app), TOPLEVEL_BINDING, %q(");
        code.append(execFile);
        code.append("), 0)");
    */
        return code.toString();
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger("org.porquebox.web.component");
}
