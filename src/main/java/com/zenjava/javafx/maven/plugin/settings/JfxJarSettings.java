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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;

/**
 *
 * @author FibreFoX
 */
public class JfxJarSettings {

    /**
     * Flag to switch on and off the compiling of CSS files to the binary format. In theory this has some minor
     * performance gains, but it's debatable whether you will notice them, and some people have experienced problems
     * with the resulting compiled files. Use at your own risk. By default this is false and CSS files are left in their
     * plain text format as they are found.
     *
     * @parameter property="jfx.css2bin" default-value=false
     */
    protected boolean css2bin;

    /**
     * A custom class that can act as a Pre-Loader for your app. The Pre-Loader is run before anything else and is
     * useful for showing splash screens or similar 'progress' style windows. For more information on Pre-Loaders, see
     * the official JavaFX packaging documentation.
     *
     * @parameter property="jfx.preLoader"
     */
    protected String preLoader;

    /**
     * Flag to switch on updating the existing jar created with maven. The jar to be updated is taken from
     * '${project.basedir}/target/${project.build.finalName}.jar'.
     * <p>
     * This makes all entries inside MANIFEST.MF being transfered to the jfx-jar.
     *
     * @parameter property="jfx.updateExistingJar" default-value=false
     */
    protected boolean updateExistingJar;

    /**
     * Set this to true if your app needs to break out of the standard web sandbox and do more powerful functions.
     * <p>
     * If you are using FXML you will need to set this value to true.
     *
     * @parameter property="jfx.allPermissions" default-value=false
     */
    protected boolean allPermissions;

    /**
     * To add custom manifest-entries, just add each entry/value-pair here.
     *
     * @parameter property="jfx.manifestAttributes"
     */
    protected Map<String, String> manifestAttributes;

    /**
     * For being able to use &lt;userJvmArgs&gt;, we have to copy some dependency when being used. To disable this feature an not having packager.jar
     * in your project, set this to false.
     * <p>
     * To get more information about, please check the documentation here: https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/jvm_options_api.html.
     *
     * @parameter property="jfx.addPackagerJar" default-value=true
     * @since 8.1.4
     */
    protected boolean addPackagerJar;

    /**
     * In the case you don't want some dependency landing in the generated lib-folder (e.g. complex maven-dependencies),
     * you now can manually exclude that dependency by added it's coordinates here.
     *
     * @parameter property="jfx.classpathExcludes"
     * @since 8.2.0
     */
    protected List<Dependency> classpathExcludes = new ArrayList<>();

    /**
     * Per default all listed classpath excludes are ment to be transivie, that means when any direct declared dependency
     * requires another dependency.
     * <p>
     * When having &lt;classpathExcludes&gt; contains any dependency, that dependency including all transitive dependencies
     * are filtered while processing lib-files, it's the default behaviour. In the rare case you have some very special setup,
     * and just want to exlude these dependency, but want to preserve all transitive dependencies going into the lib-folder,
     * this can be set via this property.
     * <p>
     * Set this to false when you want to have the direct declared dependency excluded from lib-file-processing.
     *
     * @parameter property="jfx.classpathExcludesTransient" default-value=true
     * @since 8.2.0
     */
    protected boolean classpathExcludesTransient;

    /**
     * When you need to add additional files to generated app-folder (e.g. README, license, third-party-tools, ...),
     * you can specify the source-folder here. All files will be copied recursively.
     *
     * @parameter property="jfx.additionalAppResources"
     */
    protected File additionalAppResources;

    /**
     * It is possible to copy all files specified by additionalAppResources into the created app-folder containing
     * your jfx-jar. This makes it possible to have external files (like native binaries) being available while
     * developing using the run-mojo.
     *
     * @parameter property="jfx.copyAdditionalAppResourcesToJar" default-value="false"
     */
    protected boolean copyAdditionalAppResourcesToJar = false;

    /**
     * To skip copying all dependencies, set this to true. Please note that all dependencies will be added to the
     * manifest-classpath as normal, only the copy-process gets skipped.
     *
     * @since 8.8.0
     *
     * @parameter property="jfx.skipCopyingDependencies"
     */
    protected boolean skipCopyingDependencies = false;

    /**
     * @since 8.8.0
     *
     * @parameter property="jfx.useLibFolderContentForManifestClasspath" default-value="false"
     */
    protected boolean useLibFolderContentForManifestClasspath = false;

    /**
     * @since 8.8.0
     *
     * @parameter property="jfx.fixedManifestClasspath" default-value=""
     */
    protected String fixedManifestClasspath = null;

    /**
     * @since 8.9.0
     *
     * @parameter property="jfx.attachAsZippedArtifact" default-value="false"
     */
    protected boolean attachAsZippedArtifact = false;

}
