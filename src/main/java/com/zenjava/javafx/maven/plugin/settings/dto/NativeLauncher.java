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
package com.zenjava.javafx.maven.plugin.settings.dto;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Data transfer object for configuring secondary native launchers. These fields are all like when calling 'mvn jfx:native'.
 *
 * @author Danny Althoff
 */
public class NativeLauncher {

    /**
     * This has to be different than original appname, as all existing parameter are copied and this would be overwritten.
     */
    @Parameter(required = true)
    private String appName = null;

    @Parameter
    private String mainClass = null;

    @Parameter
    private File jfxMainAppJarName = null;

    @Parameter
    private Map<String, String> jvmProperties = null;

    @Parameter
    private List<String> jvmArgs = null;

    @Parameter
    private Map<String, String> userJvmArgs = null;

    @Parameter(defaultValue = "1.0")
    private String nativeReleaseVersion = "1.0";

    @Parameter(defaultValue = "false")
    private boolean needShortcut = false;

    @Parameter(defaultValue = "false")
    private boolean needMenu;

    @Parameter
    private String vendor = null;

    @Parameter
    private String identifier = null;

    /**
     * To override default generated classpath, set this to your wanted value.
     */
    @Parameter
    private String classpath = null;

    @Parameter
    private List<String> launcherArguments = null;

    public String getMainClass() {
        return mainClass;
    }

    public File getJfxMainAppJarName() {
        return jfxMainAppJarName;
    }

    public String getAppName() {
        return appName;
    }

    public Map<String, String> getJvmProperties() {
        return jvmProperties;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public Map<String, String> getUserJvmArgs() {
        return userJvmArgs;
    }

    public String getNativeReleaseVersion() {
        return nativeReleaseVersion;
    }

    public boolean isNeedShortcut() {
        return needShortcut;
    }

    public boolean isNeedMenu() {
        return needMenu;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public List<String> getLauncherArguments() {
        return launcherArguments;
    }

    public void setLauncherArguments(List<String> launcherArguments) {
        this.launcherArguments = launcherArguments;
    }

}
