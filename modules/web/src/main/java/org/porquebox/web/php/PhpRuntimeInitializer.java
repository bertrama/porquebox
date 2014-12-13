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

package org.porquebox.web.php;

import java.io.IOException;

import org.jboss.logging.Logger;
import com.caucho.quercus.QuercusEngine;
import org.porquebox.core.app.PhpAppMetaData;
import org.porquebox.core.runtime.BaseRuntimeInitializer;
import org.porquebox.core.runtime.RuntimeInitializer;

/**
 * {@link RuntimeInitializer} for Php applications.
 *
 * @author Bob McWhirter <bmcwhirt@redhat.com>
 */
public class PhpRuntimeInitializer extends BaseRuntimeInitializer {


    public PhpRuntimeInitializer(PhpAppMetaData phpAppMetaData) {
        super( phpAppMetaData );
        this.phpAppMetaData = phpAppMetaData;
    }

    public String getPhpEnv() {
        return getPhpAppMetaData().getEnvironmentName();
    }

    @Override
    public void initialize(QuercusEngine engine, String runtimeContext) throws Exception {
        setRuntimeType( engine, "php" );
        super.initialize( engine, runtimeContext );
    }

    protected void setRuntimeType(QuercusEngine engine, String type) {
        // Set an environment variable like below.
        // RuntimeHelper.evalScriptlet( engine, "ENV['PORQUEBOX_APP_TYPE'] ||= '" + type + "'" );
    }

    /**
     * Create the initializer script.
     *
     * @return The initializer script.
     */
    protected String getInitializerScript() {
        StringBuilder script = new StringBuilder();
        String phpEnv = getPhpAppMetaData().getEnvironmentName();
        String contextPath = this.phpAppMetaData.getContextPath();
        String phpRootPath = null;

        try {
            phpRootPath = getPhpAppMetaData().getRoot().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (phpRootPath.endsWith( "/" )) {
            phpRootPath = phpRootPath.substring( 0, phpRootPath.length() - 1 );
        }

/*
        script.append( "RACK_ROOT=%q(" + phpRootPath + ")\n" );
        script.append( "RACK_ENV=%q(" + phpEnv + ")\n" );
        script.append( "PORQUEBOX_EXEC_CONTEXT=%q(" + contextPath + ")\n" );
        script.append( "ENV['RACK_ROOT']=%q(" + phpRootPath + ")\n" );
        script.append( "ENV['RACK_ENV']=%q(" + rackEnv + ")\n" );

        // only set if not root context
        if (contextPath != null && contextPath.length() > 1) {
            // context path should always start with a "/"
            if (!contextPath.startsWith( "/" )) {
                contextPath = "/" + contextPath;
            }
            script.append( "ENV['RAILS_RELATIVE_URL_ROOT']=%q(" + contextPath + ")\n" );
            script.append( "ENV['RACK_BASE_URI']=%q(" + contextPath + ")\n" );
        }

*/
        return script.toString();
    }

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( "org.porquebox.web.php" );

    protected PhpAppMetaData phpAppMetaData;

}
