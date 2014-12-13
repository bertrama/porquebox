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

package org.porquebox.core;
import java.util.HashMap;
import java.util.Map;

import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.projectodd.polyglot.core.AsyncService;

import com.caucho.quercus.script.QuercusScriptEngineFactory;
import com.caucho.quercus.script.QuercusScriptEngine;
import com.caucho.quercus.QuercusContext;


public class GlobalPhp extends AsyncService<GlobalPhp> implements GlobalPhpMBean  {

    @Override
    public GlobalPhp getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void startAsync(final StartContext context) throws Exception {
        this.factory = new QuercusScriptEngineFactory();
        // TODO: find the quercus API for setting the classloader.  Maybe this?
        //this.factory.setClassLoader( getClass().getClassLoader() );
        this.runtime = (QuercusScriptEngine) factory.getScriptEngine();
        this.runtime.getQuercus().setCompileClassLoader(getClass().getClassLoader());
    }

    @Override
    public void stop(StopContext context) {
        // TODO: figure out what to do on the stop request.  Maybe this?
        // this.runtime.tearDown( false );
        this.runtime.getQuercus().close();
    }

    public Object evaluate(String script) throws Exception {
        while (this.runtime == null) {
            Thread.sleep( 50 );
        }
        // TODO: Evaluate using a RuntimeHelper like TB does?
        //return RuntimeHelper.evalScriptlet( this.runtime, script, false );
        return this.runtime.eval(script);
    }

    public String evaluateToString(String script) throws Exception {
        Object result = evaluate( script );
        if (result == null) {
            return null;
        }

        return result.toString();
    }
    private QuercusScriptEngineFactory factory;
    private QuercusScriptEngine runtime;
}



