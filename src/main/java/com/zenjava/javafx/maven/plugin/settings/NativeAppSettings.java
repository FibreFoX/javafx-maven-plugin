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

import com.zenjava.javafx.maven.plugin.settings.dto.NativeLauncher;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public class NativeAppSettings {

    /**
     * Used as the 'id' of the application, and is used as the CFBundleDisplayName on Mac. See the official JavaFX
     * Packaging tools documentation for other information on this. Will be used as GUID on some installers too.
     */
    @Parameter
    protected String identifier;
    /**
     *
     * The output directory that the native bundles are to be built into. This will be the base directory only as the
     * JavaFX packaging tools use sub-directories that can't be customised. Generally just have a rummage through the
     * sub-directories until you find what you are looking for.
     * <p>
     * This defaults to 'target/jfx/native' and the interesting files are usually under 'bundles'.
     */
    @Parameter(property = "jfx.nativeOutputDir", defaultValue = "${project.build.directory}/jfx/native")
    protected File nativeOutputDir;

    /**
     * Specify the used bundler found by selected bundleType. May not be installed your OS and will fail in that case.
     *
     * <p>
     * By default this will be set to 'ALL', depending on your installed OS following values are possible for installers:
     * <p>
     * <ul>
     * <li>windows.app <i>(Creates only Windows Executable, does not bundle into Installer)</i></li>
     * <li>linux.app <i>(Creates only Linux Executable, does not bundle into Installer)</i></li>
     * <li>mac.app <i>(Creates only Mac Executable, does not bundle into Installer)</i></li>
     * <li>mac.appStore <i>(Creates a binary bundle ready for deployment into the Mac App Store)</i></li>
     * </ul>
     *
     * <p>
     * For a full list of available bundlers on your system, call 'mvn jfx:list-bundlers' inside your project.
     */
    @Parameter(property = "jfx.bundler", defaultValue = "ALL")
    protected String bundler;

    /**
     * Properties passed to the Java Virtual Machine when the application is started (i.e. these properties are system
     * properties of the JVM bundled in the native distribution and used to run the application once installed).
     */
    @Parameter(property = "jfx.jvmProperties")
    protected Map<String, String> jvmProperties;

    /**
     * JVM Flags to be passed into the JVM at invocation time. These are the arguments to the left of the main class
     * name when launching Java on the command line. For example:
     * <pre>
     *     &lt;jvmArgs&gt;
     *         &lt;jvmArg&gt;-Xmx8G&lt;/jvmArg&gt;
     *     &lt;/jvmArgs&gt;
     * </pre>
     */
    @Parameter(property = "jfx.jvmArgs")
    protected List<String> jvmArgs;

    /**
     * Optional command line arguments passed to the application when it is started. These will be included in the
     * native bundle that is generated and will be accessible via the main(String[] args) method on the main class that
     * is launched at runtime.
     * <p>
     * These options are user overridable for the value part of the entry via user preferences. The key and the value
     * are concated without a joining character when invoking the JVM.
     */
    @Parameter(property = "jfx.userJvmArgs")
    protected Map<String, String> userJvmArgs;

    /**
     * You can specify arguments that gonna be passed when calling your application.
     */
    @Parameter(property = "jfx.launcherArguments")
    protected List<String> launcherArguments;

    /**
     * The release version as passed to the native installer. It would be nice to just use the project's version number
     * but this must be a fairly traditional version string (like '1.34.5') with only numeric characters and dot
     * separators, otherwise the JFX packaging tools bomb out. We default to 1.0 in case you can't be bothered to set
     * a version and don't really care. On Mac this is used for "CFBundleVersion".
     * Normally all non-number signs and dots are removed from the value, which can be disabled
     * by setting 'skipNativeVersionNumberSanitizing' to true.
     */
    @Parameter(property = "jfx.nativeReleaseVersion", defaultValue = "1.0.0")
    protected String nativeReleaseVersion;

    /**
     * A list of bundler arguments. The particular keys and the meaning of their values are dependent on the bundler
     * that is reading the arguments. Any argument not recognized by a bundler is silently ignored, so that arguments
     * that are specific to a specific bundler (for example, a Mac OS X Code signing key name) can be configured and
     * ignored by bundlers that don't use the particular argument.
     * <p>
     * To disable creating native bundles with JRE in it, just add "&lt;runtime /&gt;" to bundleArguments.
     * <p>
     * If there are bundle arguments that override other fields in the configuration, then it is an execution error.
     */
    @Parameter(property = "jfx.bundleArguments")
    protected Map<String, String> bundleArguments;

    /**
     * The name of the JavaFX packaged executable to be built into the 'native/bundles' directory. By default this will
     * be the finalName as set in your project. Change this if you want something nicer. This also has effect on the
     * filename of icon-files, e.g. having 'NiceApp' as appName means you have to place that icon
     * at 'src/main/deploy/package/[os]/NiceApp.[icon-extension]' for having it picked up by the bundler.
     */
    @Parameter(property = "jfx.appName", defaultValue = "${project.build.finalName}")
    protected String appName;

    @Parameter(property = "jfx.secondaryLaunchers")
    protected List<NativeLauncher> secondaryLaunchers;

    /**
     * When you need to add additional files to the base-folder of all bundlers (additional non-overriding files like
     * images, licenses or separated modules for encryption etc.) you can specify the source-folder here. All files
     * will be copied recursively. Please make sure to inform yourself about the details of the used bundler.
     */
    @Parameter(property = "jfx.additionalBundlerResources")
    protected File additionalBundlerResources;

    /**
     * Since it it possible to sign created jar-files using jarsigner, it might be required to
     * add some special parameters for calling it (like -tsa and -tsacert). Just add them to this
     * list to have them being applied.
     */
    @Parameter(property = "jfx.additionalJarsignerParameters")
    protected List<String> additionalJarsignerParameters = new ArrayList<>();

    // ------
    /**
     * All commands executed by this Maven-plugin will be done using the current available commands
     * of your maven-execution environment. It is possible to call Maven with a different version of Java,
     * so these calls might be wrong. To use the executables of the JDK used for running this maven-plugin,
     * please set this to false. You might need this in the case you installed multiple versions of Java.
     *
     * The default is to use environment relative executables.
     */
    @Parameter(property = "jfx.nativeAppSettings.useEnvironmentRelativeExecutables", defaultValue = "true")
    protected boolean useEnvironmentRelativeExecutables = true;

    public boolean isUseEnvironmentRelativeExecutables() {
        return useEnvironmentRelativeExecutables;
    }

    public void setUseEnvironmentRelativeExecutables(boolean useEnvironmentRelativeExecutables) {
        this.useEnvironmentRelativeExecutables = useEnvironmentRelativeExecutables;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public File getNativeOutputDir() {
        return nativeOutputDir;
    }

    public void setNativeOutputDir(File nativeOutputDir) {
        this.nativeOutputDir = nativeOutputDir;
    }

    public String getBundler() {
        return bundler;
    }

    public void setBundler(String bundler) {
        this.bundler = bundler;
    }

    public Map<String, String> getJvmProperties() {
        return jvmProperties;
    }

    public void setJvmProperties(Map<String, String> jvmProperties) {
        this.jvmProperties = jvmProperties;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public Map<String, String> getUserJvmArgs() {
        return userJvmArgs;
    }

    public void setUserJvmArgs(Map<String, String> userJvmArgs) {
        this.userJvmArgs = userJvmArgs;
    }

    public List<String> getLauncherArguments() {
        return launcherArguments;
    }

    public void setLauncherArguments(List<String> launcherArguments) {
        this.launcherArguments = launcherArguments;
    }

    public String getNativeReleaseVersion() {
        return nativeReleaseVersion;
    }

    public void setNativeReleaseVersion(String nativeReleaseVersion) {
        this.nativeReleaseVersion = nativeReleaseVersion;
    }

    public Map<String, String> getBundleArguments() {
        return bundleArguments;
    }

    public void setBundleArguments(Map<String, String> bundleArguments) {
        this.bundleArguments = bundleArguments;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<NativeLauncher> getSecondaryLaunchers() {
        return secondaryLaunchers;
    }

    public void setSecondaryLaunchers(List<NativeLauncher> secondaryLaunchers) {
        this.secondaryLaunchers = secondaryLaunchers;
    }

    public File getAdditionalBundlerResources() {
        return additionalBundlerResources;
    }

    public void setAdditionalBundlerResources(File additionalBundlerResources) {
        this.additionalBundlerResources = additionalBundlerResources;
    }

    public List<String> getAdditionalJarsignerParameters() {
        return additionalJarsignerParameters;
    }

    public void setAdditionalJarsignerParameters(List<String> additionalJarsignerParameters) {
        this.additionalJarsignerParameters = additionalJarsignerParameters;
    }

    
}
