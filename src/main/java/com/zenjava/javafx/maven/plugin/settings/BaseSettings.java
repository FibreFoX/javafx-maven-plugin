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

/**
 *
 * @author FibreFoX
 */
public class BaseSettings {

    /**
     * Flag to turn on verbose logging. Set this to true if you are having problems and want more detailed information.
     *
     * @parameter property="jfx.verbose" default-value="false"
     */
    protected Boolean verbose;

    /**
     * The 'app' output directory. This is where the base executable JavaFX jar is built into, along with any dependent
     * libraries (place in the 'lib' sub-directory). The resulting JAR in this directory will be ready for distribution,
     * including Pre-Loaders, signing, etc. This JAR will also be the one bundled into the other distribution bundles
     * (i.e. web or native) if you run the relevant commands for that.
     * <p>
     * This defaults to 'target/jfx/app' and in most cases there is no real need to change this.
     *
     * @parameter property="jfx.jfxAppOutputDir" default-value="${project.build.directory}/jfx/app"
     */
    protected File jfxAppOutputDir;

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
     * </ul>
     *
     * @parameter property="jfx.deployDir" default-value="${project.basedir}/src/main/deploy"
     */
    protected String deployDir;

    /**
     * Set this to true for skipping the execution.
     *
     * @parameter property="jfx.skip" default-value="false"
     */
    protected boolean skip;
}
