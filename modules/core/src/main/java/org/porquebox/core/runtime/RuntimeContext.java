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

package org.porquebox.core.runtime;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.logging.Logger;
import com.caucho.quercus.QuercusEngine;

public class RuntimeContext {

    public static QuercusEngine getCurrentRuntime() {
        // TODO: Maybe this will work?
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (cl != null) {
            WeakReference<QuercusEngine> ref = contexts.get( cl );
            if ( ref != null ) {
                 return ref.get();
            }
            cl = cl.getParent();
        }
        return null;
    }

    public static void registerRuntime(QuercusEngine php) {
        // TODO: Should I be using php.getClass().getClassLoader() ?
        contexts.put( Thread.currentThread().getContextClassLoader(), new WeakReference<QuercusEngine>(php) );
        return;
    }

    public static void deregisterRuntime(QuercusEngine php) {
        // TODO: Should I be using php.getClass().getClassLoader() ?
        contexts.remove( Thread.currentThread().getContextClassLoader() );
    }

    private static final Logger log = Logger.getLogger( "org.porquebox.core.runtime.context" );
    private static final Map<ClassLoader, WeakReference<QuercusEngine>> contexts = Collections.synchronizedMap( new WeakHashMap<ClassLoader, WeakReference<QuercusEngine>>() );
}
