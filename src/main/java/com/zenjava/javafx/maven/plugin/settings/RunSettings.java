/*
 * Copyright 2012 Daniel Zwolenski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zenjava.javafx.maven.plugin.settings;

/**
 *
 * @author FibreFoX
 */
public class RunSettings {

    /**
     * Developing and debugging javafx applications can be difficult, so a lot of
     * tools exists, that need to be injected into the JVM via special parameter
     * (e.g. javassist). To have this being part of the command used to start the
     * application by this MOJO, just set all your parameters here.
     *
     * @parameter property="jfx.runSettings.javaParameter"
     */
    protected String javaParameter = null;

    /**
     * While developing, you might need some arguments for your application passed
     * to your execution. To have them being part of the command used to start the
     * application by this MOJO, just set all your parameters here.
     *
     * This fixes issue #176.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/176
     * @parameter property="jfx.runSettings.appParameter"
     */
    protected String appParameter = null;

    /**
     * All commands executed by this Maven-plugin will be done using the current available commands
     * of your maven-execution environment. It is possible to call Maven with a different version of Java,
     * so these calls might be wrong. To use the executables of the JDK used for running this maven-plugin,
     * please set this to false. You might need this in the case you installed multiple versions of Java.
     *
     * The default is to use environment relative executables.
     *
     * @parameter property="jfx.runSettings.useEnvironmentRelativeExecutables" default-value="true"
     */
    protected boolean useEnvironmentRelativeExecutables;

    public String getJavaParameter() {
        return javaParameter;
    }

    public void setJavaParameter(String javaParameter) {
        this.javaParameter = javaParameter;
    }

    public String getAppParameter() {
        return appParameter;
    }

    public void setAppParameter(String appParameter) {
        this.appParameter = appParameter;
    }

    public boolean isUseEnvironmentRelativeExecutables() {
        return useEnvironmentRelativeExecutables;
    }

    public void setUseEnvironmentRelativeExecutables(boolean useEnvironmentRelativeExecutables) {
        this.useEnvironmentRelativeExecutables = useEnvironmentRelativeExecutables;
    }

}
