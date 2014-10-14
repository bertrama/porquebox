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

import java.util.Map;

import org.jboss.as.naming.context.NamespaceContextSelector;
import com.caucho.quercus.QuercusEngine;

public class AbstractPhpComponent implements PhpComponent {

    public AbstractPhpComponent() {
    }

    public AbstractPhpComponent(Object component) {
        this.phpComponent = component;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Map<String, Object> getOptions() {
        return this.options;
    }

    public Object getOption(String name) {
        return this.options.get( name );
    }

    public void setPhpComponent(Object component) {
        this.phpComponent = component;
    }

    public Object getPhpComponent() {
        return this.phpComponent;
    }

    protected Object _callPhpMethod(Object target, String method, Object... args) {
        try {
            if (this.namespaceContextSelector != null) {
                NamespaceContextSelector.pushCurrentSelector( this.namespaceContextSelector );
            }
            return null; // RuntimeHelper.call( this.phpComponent.getRuntime(), target, method, args );
        } finally {
            if (this.namespaceContextSelector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    public Object _callPhpMethod(String method, Object... args) {
        return _callPhpMethod( this.phpComponent, method, args );
    }

    protected Object _callPhpMethodIfDefined(Object target, String method, Object... args) {
        try {
            if (this.namespaceContextSelector != null) {
                NamespaceContextSelector.pushCurrentSelector( this.namespaceContextSelector );
            }
            return null; // RuntimeHelper.callIfPossible( this.phpComponent.getRuntime(), target, method, args );
        } finally {
            if (this.namespaceContextSelector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }

        }
    }

    public Object _callPhpMethodIfDefined(String method, Object... args) {
        return _callPhpMethodIfDefined( this.phpComponent, method, args );
    }

    protected boolean _defined(Object target, String name) {
        try {
            if (this.namespaceContextSelector != null) {
                NamespaceContextSelector.pushCurrentSelector( this.namespaceContextSelector );
            }
            return false; // RuntimeHelper.defined( this.phpComponent.getRuntime(), target, name );
        } finally {
            if (this.namespaceContextSelector != null) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }

    public boolean _defined(String name) {
        return _defined( this.phpComponent, name );
    }

    protected QuercusEngine getRuntime() {
        return null; // this.phpComponent.getRuntime();
    }

    public void setNamespaceContextSelector(NamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }

    private Map<String, Object> options;
    private Object phpComponent;
    private NamespaceContextSelector namespaceContextSelector;
}
