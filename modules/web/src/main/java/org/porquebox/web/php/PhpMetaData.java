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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.projectodd.polyglot.web.WebApplicationMetaData;

public class PhpMetaData extends WebApplicationMetaData {

    public static final AttachmentKey<PhpMetaData> ATTACHMENT_KEY = AttachmentKey.create(PhpMetaData.class);
    
    public PhpMetaData() {

    }

    @Override
    public void attachTo(DeploymentUnit unit) {
        super.attachTo( unit );
        unit.putAttachment( ATTACHMENT_KEY, this );
    }
    
    public void setExecScript(String execScript) {
        this.execScript = execScript;
    }

    public String getExecScript(File root) throws IOException {
        return this.execScript;
    }

    public void setExecScriptLocation(String execScriptLocation) {
        this.execScriptLocation = execScriptLocation;
    }

    public String getExecScriptLocation() {
        return this.execScriptLocation;
    }

    public File getExecScriptFile(File root) {
        if (this.execScriptLocation == null) {
            return null;
        }

        if (this.execScriptLocation.startsWith( "/" ) || execScriptLocation.matches( "^[A-Za-z]:.*" )) {
            return new File( execScriptLocation );
        } else {
            return new File( root, execScriptLocation );
        }
    }

    public void setPhpRuntimePoolName(String phpRuntimePoolName) {
        this.phpRuntimePoolName = phpRuntimePoolName;
    }

    public String getPhpRuntimePoolName() {
        return this.phpRuntimePoolName;
    }

    public void setPhpApplicationFactoryName(String phpApplicationFactoryName) {
        this.phpApplicationFactoryName = phpApplicationFactoryName;
    }

    public String getPhpApplicationFactoryName() {
        return this.phpApplicationFactoryName;
    }

    public void setPhpApplicationPoolName(String phpApplicationPoolName) {
        this.phpApplicationPoolName = phpApplicationPoolName;
    }

    public String getPhpApplicationPoolName() {
        return this.phpApplicationPoolName;
    }

    public String toString() {
        return "[PhpApplicationMetaData:" + System.identityHashCode( this ) + "\n  execScriptLocation=" + this.execScriptLocation + "\n  execScript="
                + this.execScript + "\n  host=" + getHosts() + "\n  context=" + getContextPath() + "\n  static=" + getStaticPathPrefix() + "]";
    }

    public String setExecute(String str) {
      return execute = str;
    }

    public String setExclude(String str) {
      return exclude = str;
    }

    public String getExecute() {
      return execute;
    }

    public String getExclude() {
      return exclude;
    }

    private String execScript;
    private String execScriptLocation = "index.php";

    private String phpRuntimePoolName;
    private String phpApplicationFactoryName;
    private String phpApplicationPoolName;
    private String execute;
    private String exclude;
}
