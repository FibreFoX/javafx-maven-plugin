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
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public class JfxJarSettings {

    /**
     * The name of the JavaFX packaged JAR to be built into the 'app' directory. By default this will be the finalName
     * as set in your project with a '-jfx' suffix. Change this if you want something nicer. Note, that changing this
     * value does not affect the regular old, non-JFX modified JAR (built in the 'target' directory).
     */
    @Parameter(property = "jfx.jfxMainAppJarName", defaultValue = "${project.build.finalName}-jfx.jar")
    protected String jfxMainAppJarName = "";

    /**
     * The 'app' output directory. This is where the base executable JavaFX jar is built into, along with any dependent
     * libraries (place in the 'lib' sub-directory). The resulting JAR in this directory will be ready for distribution,
     * including Pre-Loaders, signing, etc. This JAR will also be the one bundled into the other distribution bundles
     * (i.e. web or native) if you run the relevant commands for that.
     * <p>
     * This defaults to 'target/jfx/app' and in most cases there is no real need to change this.
     */
    @Parameter(property = "jfx.jfxAppOutputDir", defaultValue = "${project.build.directory}/jfx/app")
    protected File outputFolderName;

    /**
     * The main JavaFX application class that acts as the entry point to the JavaFX application.
     */
    @Parameter(property = "jfx.mainClass", required = true)
    protected String mainClass = "";

    /**
     * Flag to switch on and off the compiling of CSS files to the binary format. In theory this has some minor
     * performance gains, but it's debatable whether you will notice them, and some people have experienced problems
     * with the resulting compiled files. Use at your own risk. By default this is false and CSS files are left in their
     * plain text format as they are found.
     */
    @Parameter(property = "jfx.css2bin", defaultValue = "false")
    protected boolean css2bin;

    /**
     * A custom class that can act as a Pre-Loader for your app. The Pre-Loader is run before anything else and is
     * useful for showing splash screens or similar 'progress' style windows. For more information on Pre-Loaders, see
     * the official JavaFX packaging documentation.
     */
    @Parameter(property = "jfx.preLoader")
    protected String preLoader;

    /**
     * Flag to switch on updating the existing jar created with maven. The jar to be updated is taken from
     * '${project.basedir}/target/${project.build.finalName}.jar'.
     * <p>
     * This makes all entries inside MANIFEST.MF being transfered to the jfx-jar.
     */
    @Parameter(property = "jfx.updateExistingJar", defaultValue = "false")
    protected boolean updateExistingJar;

    /**
     * Set this to true if your app needs to break out of the standard web sandbox and do more powerful functions.
     * <p>
     * If you are using FXML you will need to set this value to true.
     */
    @Parameter(property = "jfx.allPermissions", defaultValue = "false")
    protected boolean allPermissions = false;

    /**
     * To add custom manifest-entries, just add each entry/value-pair here.
     */
    @Parameter(property = "jfx.manifestAttributes")
    protected Map<String, String> manifestAttributes;

    /**
     * For being able to use &lt;userJvmArgs&gt;, we have to copy some dependency when being used. To disable this feature an not having packager.jar
     * in your project, set this to false.
     * <p>
     * To get more information about, please check the documentation here: https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/jvm_options_api.html.
     *
     * @since 8.1.4
     */
    @Parameter(property = "jfx.addPackagerJar", defaultValue = "true")
    protected boolean addPackagerJar = true;

    /**
     * In the case you don't want some dependency landing in the generated lib-folder (e.g. complex maven-dependencies),
     * you now can manually exclude that dependency by added it's coordinates here.
     *
     * @since 8.2.0
     */
    @Parameter(property = "jfx.classpathExcludes")
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
     * @since 8.2.0
     */
    @Parameter(property = "jfx.classpathExcludesTransient", defaultValue = "true")
    protected boolean classpathExcludesTransient = true;

    /**
     * When you need to add additional files to generated app-folder (e.g. README, license, third-party-tools, ...),
     * you can specify the source-folder here. All files will be copied recursively.
     */
    @Parameter(property = "jfx.additionalAppResources")
    protected File additionalAppResources;

    /**
     * It is possible to copy all files specified by additionalAppResources into the created app-folder containing
     * your jfx-jar. This makes it possible to have external files (like native binaries) being available while
     * developing using the run-mojo.
     */
    @Parameter(property = "jfx.copyAdditionalAppResourcesToJar", defaultValue = "false")
    protected boolean copyAdditionalAppResourcesToJar = false;

    /**
     * To skip copying all dependencies, set this to true. Please note that all dependencies will be added to the
     * manifest-classpath as normal, only the copy-process gets skipped.
     *
     * @since 8.8.0
     */
    @Parameter(property = "jfx.skipCopyingDependencies", defaultValue = "false")
    protected boolean skipCopyingDependencies = false;

    /**
     * @since 8.8.0
     */
    @Parameter(property = "jfx.useLibFolderContentForManifestClasspath", defaultValue = "false")
    protected boolean useLibFolderContentForManifestClasspath = false;

    /**
     * @since 8.8.0
     */
    @Parameter(property = "jfx.fixedManifestClasspath", defaultValue = "")
    protected String fixedManifestClasspath = "";

    /**
     * @since 9.0.0
     */
    @Parameter(property = "jfx.attachAsZippedArtifact", defaultValue = "false")
    protected boolean attachAsZippedArtifact = false;

    /**
     * All dependencies are copied to a separated folder, which can be changed.
     *
     * @since 8.8.0
     */
    @Parameter(property = "jfx.libFolderName", defaultValue = "lib")
    protected String libFolderName;

    public String getJfxMainAppJarName() {
        return jfxMainAppJarName;
    }

    public void setJfxMainAppJarName(String jfxMainAppJarName) {
        this.jfxMainAppJarName = jfxMainAppJarName;
    }

    public File getOutputFolderName() {
        return outputFolderName;
    }

    public void setOutputFolderName(File outputFolderName) {
        this.outputFolderName = outputFolderName;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public boolean isCss2bin() {
        return css2bin;
    }

    public void setCss2bin(boolean css2bin) {
        this.css2bin = css2bin;
    }

    public String getPreLoader() {
        return preLoader;
    }

    public void setPreLoader(String preLoader) {
        this.preLoader = preLoader;
    }

    public boolean isUpdateExistingJar() {
        return updateExistingJar;
    }

    public void setUpdateExistingJar(boolean updateExistingJar) {
        this.updateExistingJar = updateExistingJar;
    }

    public boolean isAllPermissions() {
        return allPermissions;
    }

    public void setAllPermissions(boolean allPermissions) {
        this.allPermissions = allPermissions;
    }

    public Map<String, String> getManifestAttributes() {
        return manifestAttributes;
    }

    public void setManifestAttributes(Map<String, String> manifestAttributes) {
        this.manifestAttributes = manifestAttributes;
    }

    public boolean isAddPackagerJar() {
        return addPackagerJar;
    }

    public void setAddPackagerJar(boolean addPackagerJar) {
        this.addPackagerJar = addPackagerJar;
    }

    public List<Dependency> getClasspathExcludes() {
        return classpathExcludes;
    }

    public void setClasspathExcludes(List<Dependency> classpathExcludes) {
        this.classpathExcludes = classpathExcludes;
    }

    public boolean isClasspathExcludesTransient() {
        return classpathExcludesTransient;
    }

    public void setClasspathExcludesTransient(boolean classpathExcludesTransient) {
        this.classpathExcludesTransient = classpathExcludesTransient;
    }

    public File getAdditionalAppResources() {
        return additionalAppResources;
    }

    public void setAdditionalAppResources(File additionalAppResources) {
        this.additionalAppResources = additionalAppResources;
    }

    public boolean isCopyAdditionalAppResourcesToJar() {
        return copyAdditionalAppResourcesToJar;
    }

    public void setCopyAdditionalAppResourcesToJar(boolean copyAdditionalAppResourcesToJar) {
        this.copyAdditionalAppResourcesToJar = copyAdditionalAppResourcesToJar;
    }

    public boolean isSkipCopyingDependencies() {
        return skipCopyingDependencies;
    }

    public void setSkipCopyingDependencies(boolean skipCopyingDependencies) {
        this.skipCopyingDependencies = skipCopyingDependencies;
    }

    public boolean isUseLibFolderContentForManifestClasspath() {
        return useLibFolderContentForManifestClasspath;
    }

    public void setUseLibFolderContentForManifestClasspath(boolean useLibFolderContentForManifestClasspath) {
        this.useLibFolderContentForManifestClasspath = useLibFolderContentForManifestClasspath;
    }

    public String getFixedManifestClasspath() {
        return fixedManifestClasspath;
    }

    public void setFixedManifestClasspath(String fixedManifestClasspath) {
        this.fixedManifestClasspath = fixedManifestClasspath;
    }

    public boolean isAttachAsZippedArtifact() {
        return attachAsZippedArtifact;
    }

    public void setAttachAsZippedArtifact(boolean attachAsZippedArtifact) {
        this.attachAsZippedArtifact = attachAsZippedArtifact;
    }

    public String getLibFolderName() {
        return libFolderName;
    }

    public void setLibFolderName(String libFolderName) {
        this.libFolderName = libFolderName;
    }

}
