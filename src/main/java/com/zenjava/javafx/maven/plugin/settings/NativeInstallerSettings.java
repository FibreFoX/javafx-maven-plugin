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

import com.zenjava.javafx.maven.plugin.settings.dto.FileAssociation;
import com.zenjava.javafx.maven.plugin.settings.dto.NativeLauncher;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public class NativeInstallerSettings {

    /**
     * Used as the 'id' of the application, and is used as the CFBundleDisplayName on Mac. See the official JavaFX
     * Packaging tools documentation for other information on this. Will be used as GUID on some installers too.
     */
    @Parameter
    protected String identifier;

    /**
     * The vendor of the application (i.e. you). This is required for some of the installation bundles and it's
     * recommended just to set it from the get-go to avoid problems. This will default to the project.organization.name
     * element in you POM if you have one.
     */
    @Parameter(required = true, property = "project.organization.name")
    protected String vendor;
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
     * <li>mac.appStore <i>(Creates a binary bundle ready for deployment into the Mac App Store)</i></li>
     * <li>exe <i>(Microsoft Windows EXE Installer, via InnoIDE)</i></li>
     * <li>msi <i>(Microsoft Windows MSI Installer, via WiX)</i></li>
     * <li>deb <i>(Linux Debian Bundle)</i></li>
     * <li>rpm <i>(Redhat Package Manager (RPM) bundler)</i></li>
     * <li>dmg <i>(Mac DMG Installer Bundle)</i></li>
     * <li>pkg <i>(Mac PKG Installer Bundle)</i></li>
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
     * The name of the JavaFX packaged executable to be built into the 'native/bundles' directory. By default this will
     * be the finalName as set in your project. Change this if you want something nicer. This also has effect on the
     * filename of icon-files, e.g. having 'NiceApp' as appName means you have to place that icon
     * at 'src/main/deploy/package/[os]/NiceApp.[icon-extension]' for having it picked up by the bundler.
     */
    @Parameter(property = "jfx.appName", defaultValue = "${project.build.finalName}")
    protected String appName;
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

    @Parameter(property = "jfx.secondaryLaunchers")
    protected List<NativeLauncher> secondaryLaunchers;

    /**
     * It is possible to create file associations when using native installers. When specified,
     * all file associations are bound to the main native launcher. There is no support for bunding
     * them to second launchers.
     * <p>
     * For more informatione, please see official information source: https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/javafx_ant_task_reference.html#CIAIDHBJ
     */
    @Parameter(property = "jfx.fileAssociations")
    private List<FileAssociation> fileAssociations;

    /**
     * When you need to add additional files to the base-folder of all bundlers (additional non-overriding files like
     * images, licenses or separated modules for encryption etc.) you can specify the source-folder here. All files
     * will be copied recursively. Please make sure to inform yourself about the details of the used bundler.
     */
    @Parameter(property = "jfx.additionalBundlerResources")
    protected File additionalBundlerResources;
    // ------
    /**
     * Set this to true if you would like your application to have a shortcut on the users desktop (or platform
     * equivalent) when it is installed.
     */
    @Parameter(defaultValue = "false")
    private boolean createShortcut = false;

    /**
     * Set this to true if you would like your application to have a link in the main system menu (or platform
     * equivalent) when it is installed.
     */
    @Parameter(defaultValue = "false")
    private boolean createMenu = false;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Map<String, String> getBundleArguments() {
        return bundleArguments;
    }

    public void setBundleArguments(Map<String, String> bundleArguments) {
        this.bundleArguments = bundleArguments;
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

    
    
    public boolean isCreateShortcut() {
        return createShortcut;
    }

    public void setCreateShortcut(boolean createShortcut) {
        this.createShortcut = createShortcut;
    }

    public boolean isCreateMenu() {
        return createMenu;
    }

    public void setCreateMenu(boolean createMenu) {
        this.createMenu = createMenu;
    }

    public List<FileAssociation> getFileAssociations() {
        return fileAssociations;
    }

    public void setFileAssociations(List<FileAssociation> fileAssociations) {
        this.fileAssociations = fileAssociations;
    }

}
