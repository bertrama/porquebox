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

package org.porquebox.core.component;

import java.util.HashMap;
import java.util.Map;

import com.caucho.quercus.QuercusEngine;
//import org.jruby.javasupport.JavaEmbedUtils;
//import org.jruby.runtime.builtin.IRubyObject;

public class ComponentRegistry {

    public static ComponentRegistry getRegistryFor(QuercusEngine runtime) {
        // TODO: Implement the php component registry.
        // IRubyObject rubyRegistry = runtime.getObject().getConstant( PORQUEBOX_COMPONENT_REGISTRY );
        // return (ComponentRegistry) JavaEmbedUtils.rubyToJava( rubyRegistry );
        return new ComponentRegistry(runtime);
    }

    public static void createRegistryFor(QuercusEngine runtime) {
        // TODO: Implement the php component registry.
        // ComponentRegistry javaRegistry = new ComponentRegistry( runtime );
        // IRubyObject rubyRegistry = JavaEmbedUtils.javaToRuby( runtime, javaRegistry );
        // runtime.getObject().setConstant( PORQUEBOX_COMPONENT_REGISTRY, rubyRegistry );
    }

    private ComponentRegistry(QuercusEngine runtime) {
        this.runtime = runtime;
    }

    // TODO: Find the right class to use instead of object.
    public Object lookup(String componentName) {
        return this.registry.get( componentName );
    }

    public void register(String componentName, Object phpComponent) {
        // TODO: Find the right class to use instead of object.
        // if (phpComponent.getRuntime() != this.runtime) {
        //    throw new IllegalArgumentException( "Component/runtime mismatch" );
        //}

        this.registry.put( componentName, phpComponent );
    }

    private QuercusEngine runtime;
    // TODO: Find the right class to use instead of object.
    private Map<String, Object> registry = new HashMap<String, Object>();

    private static final String PORQUEBOX_COMPONENT_REGISTRY = "PORQUEBOX_COMPONENT_REGISTRY";
}
