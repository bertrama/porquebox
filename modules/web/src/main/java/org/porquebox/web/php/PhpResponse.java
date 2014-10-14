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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.caucho.quercus.servlet.api.QuercusHttpServletResponseImpl;
import com.caucho.quercus.servlet.api.QuercusHttpServletRequestImpl;
import com.caucho.quercus.QuercusEngine;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.env.Env;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.StringWriter;
import org.porquebox.web.component.PhpApplicationComponent;
import org.porquebox.core.component.ComponentEval;
import org.jboss.logging.Logger;
import java.io.PrintWriter;
import java.io.IOException;

public class PhpResponse {

    private PhpEnvironment phpEnv;
    private PhpApplicationComponent phpComponent;

    public PhpResponse(PhpEnvironment phpEnv, PhpApplicationComponent phpComponent) {
        this.phpEnv = phpEnv;
        this.phpComponent = phpComponent;
    }

    public void respond(HttpServletResponse response) {
        QuercusEngine runtime = phpEnv.getEngine();
        QuercusContext ctx = runtime.getQuercus();
        ComponentEval component = (ComponentEval) phpComponent.getPhpComponent();

        try {
          QuercusPage page   =  ctx.parse(new FilePath(component.getLocation()));
          StringWriter string = new StringWriter();
          WriteStream stream = string.openWrite();
          HttpServletRequest request = phpEnv.getRequest();
          QuercusHttpServletRequestImpl  quercusRequest  = new QuercusHttpServletRequestImpl(request);
          QuercusHttpServletResponseImpl quercusResponse = new QuercusHttpServletResponseImpl(response);
          Env env = ctx.createEnv(page, stream, quercusRequest, quercusResponse);
          env.start();
          env.execute();
          response = quercusResponse.toResponse(HttpServletResponse.class);
          PrintWriter out = response.getWriter();
          out.print(string.getString());
        }
        catch (IOException e) {
        }
    }
    private static final Logger log = Logger.getLogger( "org.porquebox.web.php" );
}
