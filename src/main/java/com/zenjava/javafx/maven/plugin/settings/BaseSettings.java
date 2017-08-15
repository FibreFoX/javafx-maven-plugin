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

import java.io.File;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public class BaseSettings {

    /**
     * Set this to true for skipping the execution.
     */
    @Parameter(name = "skip", property = "jfx.baseSettings.skip", defaultValue = "false")
    protected boolean skip = false;

    /**
     * Flag to turn off verbose logging. Set this to true if you are having problems and want more detailed information.
     *
     */
    @Parameter(name = "verbose", property = "jfx.baseSettings.verbose", defaultValue = "true")
    protected boolean verbose = true;

    /**
     * The 'app' output directory. This is where the base executable JavaFX jar is built into, along with any dependent
     * libraries (place in the 'lib' sub-directory). The resulting JAR in this directory will be ready for distribution,
     * including Pre-Loaders, signing, etc. This JAR will also be the one bundled into the other distribution bundles
     * (i.e. web or native) if you run the relevant commands for that.
     * <p>
     * This defaults to 'target/jfx/app' and in most cases there is no real need to change this.
     *
     */
    @Parameter(name = "outputDirectory", alias = "jfxAppOutputDir", property = "jfx.baseSettings.outputDirectory", defaultValue = "${project.build.directory}/jfx")
    protected File outputDirectory;

    /**
     * The directory contain deployment specific files, such as icons and splash screen images. This directory is added
     * to the classpath of the Mojo when it runs, so that any files within this directory are accessible to the
     * JavaFX packaging tools.
     * <p>
     * This defaults to src/main/deploy and typically this is good enough. Just put your deployment specific files in
     * this directory and they will be automatically picked up.
     * <p>
     * The most common usage for this is to provide platform specific icons for native bundles. In this case you need
     * to follow the convention of the JavaFX packaging tools to ensure your icons get picked up.
     *
     * <ul>
     * <li>for <b>windows</b> put an icon at src/main/deploy/package/windows/your-app-name.ico</li>
     * <li>for <b>mac</b> put an icon at src/main/deploy/package/macosx/your-app-name.icns</li>
     * <li>for <b>linux</b> put an icon at src/main/deploy/package/linux/your-app-name.png</li>
     * </ul>
     */
    @Parameter(name = "deployDir", property = "jfx.baseSettings.deployDir", defaultValue = "${project.basedir}/src/main/deploy")
    protected String deployDir;

    /**
     * Per default his plugin does not break the build if any bundler is failing. If you want
     * to fail the build and not just print a warning, please set this to true.
     *
     */
    @Parameter(property = "jfx.baseSettings.failOnError", defaultValue = "false")
    protected boolean failOnError = false;

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getDeployDir() {
        return deployDir;
    }

    public void setDeployDir(String deployDir) {
        this.deployDir = deployDir;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

}
