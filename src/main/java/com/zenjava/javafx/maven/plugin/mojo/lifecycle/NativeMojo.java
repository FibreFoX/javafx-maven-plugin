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
package com.zenjava.javafx.maven.plugin.mojo.lifecycle;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.oracle.tools.packager.linux.LinuxDebBundler;
import com.oracle.tools.packager.linux.LinuxRpmBundler;
import com.oracle.tools.packager.windows.WinExeBundler;
import com.oracle.tools.packager.windows.WinMsiBundler;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.PackagerLib;
import com.sun.javafx.tools.packager.SignJarParams;
import com.zenjava.javafx.maven.plugin.AbstractJfxMojo;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeAppSettings;
import com.zenjava.javafx.maven.plugin.settings.dto.FileAssociation;
import com.zenjava.javafx.maven.plugin.settings.dto.NativeLauncher;
import com.zenjava.javafx.maven.plugin.utils.FileHelper;
import com.zenjava.javafx.maven.plugin.utils.JavaTools;
import com.zenjava.javafx.maven.plugin.workarounds.GenericWorkarounds;
import com.zenjava.javafx.maven.plugin.workarounds.LinuxSpecificWorkarounds;
import com.zenjava.javafx.maven.plugin.workarounds.MacSpecificWorkarounds;
import com.zenjava.javafx.maven.plugin.workarounds.WindowsSpecificWorkarounds;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "build-native")
public class NativeMojo extends AbstractJfxMojo {

    @Parameter
    protected JfxJarSettings jfxJarSettings;

    @Parameter
    protected NativeAppSettings nativeAppSettings;

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
     * <li>windows.app <i>(Creates only Windows Executable, does not bundle into Installer)</i></li>
     * <li>linux.app <i>(Creates only Linux Executable, does not bundle into Installer)</i></li>
     * <li>mac.app <i>(Creates only Mac Executable, does not bundle into Installer)</i></li>
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
     * a version and don't really care.
     * Normally all non-number signs and dots are removed from the value, which can be disabled
     * by setting 'skipNativeVersionNumberSanitizing' to true.
     */
    @Parameter(property = "jfx.nativeReleaseVersion", defaultValue = "1.0")
    protected String nativeReleaseVersion;

    /**
     * Set this to true if you would like your application to have a shortcut on the users desktop (or platform
     * equivalent) when it is installed.
     */
    @Parameter(property = "jfx.needShortcut", defaultValue = "false")
    protected boolean needShortcut;

    /**
     * Set this to true if you would like your application to have a link in the main system menu (or platform
     * equivalent) when it is installed.
     */
    @Parameter(property = "jfx.needMenu", defaultValue = "false")
    protected boolean needMenu;

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

    /**
     * Will be set when having goal "build-native" within package-phase and calling "jfx:native" from CLI. Internal usage only.
     */
    @Parameter(defaultValue = "false")
    protected boolean jfxCallFromCLI;

    /**
     * When you need to add additional files to generated app-folder (e.g. README, license, third-party-tools, ...),
     * you can specify the source-folder here. All files will be copied recursively.
     */
    @Parameter(property = "jfx.additionalAppResources")
    protected File additionalAppResources;

    /**
     * When you need to add additional files to the base-folder of all bundlers (additional non-overriding files like
     * images, licenses or separated modules for encryption etc.) you can specify the source-folder here. All files
     * will be copied recursively. Please make sure to inform yourself about the details of the used bundler.
     */
    @Parameter(property = "jfx.additionalBundlerResources")
    protected File additionalBundlerResources;

    /**
     * Since Java version 1.8.0 Update 40 the native launcher for linux was changed and includes a bug
     * while searching for the generated configfile. This results in wrong ouput like this:
     * <pre>
     * client-1.1 No main class specified
     * client-1.1 Failed to launch JVM
     * </pre>
     * <p>
     * Scenario (which would work on windows):
     * <p>
     * <ul>
     * <li>generated launcher: i-am.working.1.2.0-SNAPSHOT</li>
     * <li>launcher-algorithm extracts the "extension" (a concept not known in linux-space for executables) and now searches for i-am.working.1.2.cfg</li>
     * </ul>
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
     */
    @Parameter(property = "jfx.skipNativeLauncherWorkaround124", defaultValue = "false")
    protected boolean skipNativeLauncherWorkaround124;

    @Parameter(property = "jfx.secondaryLaunchers")
    protected List<NativeLauncher> secondaryLaunchers;

    /**
     * Since Java version 1.8.0 Update 60 the native launcher configuration for windows was changed
     * and includes a bug: the file-format before was "property-file", now it's "INI-file" per default,
     * but the runtime-configuration isn't honored like in property-files.
     * This workaround enforces the property-file-format.
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
     */
    @Parameter(property = "jfx.skipNativeLauncherWorkaround167", defaultValue = "false")
    protected boolean skipNativeLauncherWorkaround167;

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
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was presented and includes
     * a bug while generating relative file-references when building on windows.
     * <p>
     * Change this to "true" when you don't want this workaround.
     */
    @Parameter(property = "jfx.skipJNLPRessourcePathWorkaround182")
    protected boolean skipJNLPRessourcePathWorkaround182;

    /**
     * The location of the keystore. If not set, this will default to src/main/deploy/kesytore.jks which is usually fine
     * to use for most cases.
     */
    @Parameter(property = "jfx.keyStore", defaultValue = "src/main/deploy/keystore.jks")
    protected File keyStore;

    /**
     * The alias to use when accessing the keystore. This will default to "myalias".
     */
    @Parameter(property = "jfx.keyStoreAlias", defaultValue = "myalias")
    protected String keyStoreAlias;

    /**
     * The password to use when accessing the keystore. This will default to "password".
     */
    @Parameter(property = "jfx.keyStorePassword", defaultValue = "password")
    protected String keyStorePassword;

    /**
     * The password to use when accessing the key within the keystore. If not set, this will default to
     * keyStorePassword.
     */
    @Parameter(property = "jfx.keyPassword")
    protected String keyPassword;

    /**
     * The type of KeyStore being used. This defaults to "jks", which is the normal one.
     */
    @Parameter(property = "jfx.keyStoreType", defaultValue = "jks")
    protected String keyStoreType;

    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was introduced,
     * but lacks the ability to sign jar-files by passing some flag. We are signing the files in the
     * case of having "jnlp" as bundler. The MOJO with the goal "build-web" was deprecated in favor
     * of that new bundler (mostly because the old one does not fit the bundler-list strategy).
     * <p>
     * Change this to "true" when you don't want signing jar-files.
     */
    @Parameter(property = "jfx.skipSigningJarFilesJNLP185", defaultValue = "false")
    protected boolean skipSigningJarFilesJNLP185;

    /**
     * After signing is done, the sizes inside generated JNLP-files still point to unsigned jar-file sizes,
     * so we have to fix these sizes to be correct. This sizes-fix even lacks in the old web-MOJO.
     * <p>
     * Change this to "true" when you don't want to recalculate sizes of jar-files.
     */
    @Parameter(property = "jfx.skipSizeRecalculationForJNLP185", defaultValue = "false")
    protected boolean skipSizeRecalculationForJNLP185;

    /**
     * JavaFX introduced a new way for signing jar-files, which was called "BLOB signing".
     * <p>
     * The tool "jarsigner" is not able to verify that signature and webstart doesn't
     * accept that format either. For not having to call jarsigner yourself, set this to "true"
     * for having your jar-files getting signed when generating JNLP-files.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/190
     */
    @Parameter(property = "jfx.noBlobSigning", defaultValue = "false")
    protected boolean noBlobSigning;

    /**
     * As it is possible to extend existing bundlers, you don't have to use your private
     * version of the javafx-maven-plugin. Just provide a list with the java-classes you
     * want to use, declare them as compile-depencendies and run `mvn jfx:native`
     * or by using maven lifecycle.
     * You have to implement the Bundler-interface (@see com.oracle.tools.packager.Bundler).
     */
    @Parameter(property = "jfx.customBundlers")
    protected List<String> customBundlers;

    /**
     * Same problem as workaround for bug 124 for native launchers, but this time regarding
     * created native installers, where the workaround didn't apply.
     * <p>
     * Change this to "true" when you don't want this workaround.
     * <p>
     * Requires skipNativeLauncherWorkaround124 to be false.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
     */
    @Parameter(property = "jfx.skipNativeLauncherWorkaround205", defaultValue = "false")
    protected boolean skipNativeLauncherWorkaround205;

    @Parameter(property = "jfx.skipMacBundlerWorkaround", defaultValue = "false")
    protected boolean skipMacBundlerWorkaround = false;

    /**
     * Per default his plugin does not break the build if any bundler is failing. If you want
     * to fail the build and not just print a warning, please set this to true.
     */
    @Parameter(property = "jfx.failOnError", defaultValue = "false")
    protected boolean failOnError = false;

    @Parameter(property = "jfx.onlyCustomBundlers", defaultValue = "false")
    protected boolean onlyCustomBundlers = false;

    /**
     * If you don't want to create a JNLP-bundle, set this to true to avoid that ugly warning
     * in the build-log.
     */
    @Parameter(property = "jfx.skipJNLP", defaultValue = "false")
    protected boolean skipJNLP = false;

    /**
     * Most bundlers do not like dashes or anything than digits and dots as version number,
     * therefor we remove all "non-digit"- and "non-dot"-chars. Most use-case is when having
     * some "1.0.0-SNAPSHOT" as version-string. If you do know what you are doing, you can set
     * this to true for skipping the removal of the "evil" chars.
     *
     * @since 8.8.0
     */
    @Parameter(property = "jfx.skipNativeVersionNumberSanitizing", defaultValue = "false")
    protected boolean skipNativeVersionNumberSanitizing = false;

    /**
     * Since it it possible to sign created jar-files using jarsigner, it might be required to
     * add some special parameters for calling it (like -tsa and -tsacert). Just add them to this
     * list to have them being applied.
     */
    @Parameter(property = "jfx.additionalJarsignerParameters")
    protected List<String> additionalJarsignerParameters = new ArrayList<>();

    /**
     * Set this to true, to not scan for the specified main class inside the generated/copied jar-files.
     * <p>
     * Check only works for the main launcher, any secondary launchers are not checked.
     */
    @Parameter(property = "jfx.skipMainClassScanning", defaultValue = "false")
    protected boolean skipMainClassScanning = false;

    /**
     * Set this to true to disable the file-existence check on the keystore.
     */
    @Parameter(property = "jfx.skipKeyStoreChecking", defaultValue = "false")
    protected boolean skipKeyStoreChecking = false;

    /**
     * Set this to true to remove "-keypass"-part while signing via jarsigner.
     */
    @Parameter(property = "jfx.skipKeypassWhileSigning", defaultValue = "false")
    protected boolean skipKeypassWhileSigning = false;

    protected WindowsSpecificWorkarounds windowsSpecificWorkarounds = null;
    protected LinuxSpecificWorkarounds linuxSpecificWorkarounds = null;
    protected MacSpecificWorkarounds macSpecificWorkarounds = null;
    protected GenericWorkarounds genericWorkarounds = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of Native Installers");
            return;
        }

        if( baseSettings.isSkip() ){
            getLog().info("Skipping execution of NativeMojo MOJO.");
            return;
        }

        getLog().info("Building Native Installers");

        windowsSpecificWorkarounds = new WindowsSpecificWorkarounds(nativeOutputDir, getLog());
        linuxSpecificWorkarounds = new LinuxSpecificWorkarounds(nativeOutputDir, getLog());
        macSpecificWorkarounds = new MacSpecificWorkarounds(nativeOutputDir, getLog());
        genericWorkarounds = new GenericWorkarounds(nativeOutputDir, getLog());

        try{
            Map<String, ? super Object> params = new HashMap<>();

            // make bundlers doing verbose output (might not always be as verbose as expected)
            params.put(StandardBundlerParam.VERBOSE.getID(), baseSettings.isVerbose());

            Optional.ofNullable(identifier).ifPresent(id -> {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
            });

            params.put(StandardBundlerParam.APP_NAME.getID(), appName);
            params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion);
            // replace that value
            if( !skipNativeVersionNumberSanitizing && nativeReleaseVersion != null ){
                params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion.replaceAll("[^\\d.]", ""));
            }
            params.put(StandardBundlerParam.VENDOR.getID(), vendor);
            params.put(StandardBundlerParam.SHORTCUT_HINT.getID(), needShortcut);
            params.put(StandardBundlerParam.MENU_HINT.getID(), needMenu);
            params.put(StandardBundlerParam.MAIN_CLASS.getID(), jfxJarSettings.getMainClass());

            Optional.ofNullable(jvmProperties).ifPresent(jvmProps -> {
                params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
            });
            Optional.ofNullable(jvmArgs).ifPresent(jvmOptions -> {
                params.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
            });
            Optional.ofNullable(userJvmArgs).ifPresent(userJvmOptions -> {
                params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
            });
            Optional.ofNullable(launcherArguments).ifPresent(arguments -> {
                params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
            });

            // bugfix for #83 (by copying additional resources to /target/jfx/app folder)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/83
            Optional.ofNullable(additionalAppResources).filter(File::exists).ifPresent(appResources -> {
                try{
                    Path targetFolder = jfxJarSettings.getOutputFolderName().toPath();
                    Path sourceFolder = appResources.toPath();
                    new FileHelper().copyRecursive(sourceFolder, targetFolder, getLog());
                } catch(IOException e){
                    getLog().warn(e);
                }
            });

            // gather all files for our application bundle
            Set<File> resourceFiles = new HashSet<>();
            try{
                Files.walk(jfxJarSettings.getOutputFolderName().toPath())
                        .map(p -> p.toFile())
                        .filter(File::isFile)
                        .filter(File::canRead)
                        .forEach(f -> {
                            getLog().info(String.format("Add %s file to application resources.", f));
                            resourceFiles.add(f);
                        });
            } catch(IOException e){
                getLog().warn(e);
            }
            params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(jfxJarSettings.getOutputFolderName(), resourceFiles));

            // check for misconfiguration
            Collection<String> duplicateKeys = new HashSet<>();
            Optional.ofNullable(bundleArguments).ifPresent(bArguments -> {
                duplicateKeys.addAll(params.keySet());
                duplicateKeys.retainAll(bArguments.keySet());
                params.putAll(bArguments);
            });

            if( !duplicateKeys.isEmpty() ){
                throw new MojoExecutionException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
            }

            if( !skipMainClassScanning ){
                boolean mainClassInsideResourceJarFile = resourceFiles.stream().filter(resourceFile -> resourceFile.toString().endsWith(".jar")).filter(resourceJarFile -> isClassInsideJarFile(jfxJarSettings.getMainClass(), resourceJarFile)).findFirst().isPresent();
                if( !mainClassInsideResourceJarFile ){
                    // warn user about missing class-file
                    getLog().warn(String.format("Class with name %s was not found inside provided jar files!! JavaFX-application might not be working !!", jfxJarSettings.getMainClass()));
                }
            }

            // check for secondary launcher misconfiguration (their appName requires to be different as this would overwrite primary launcher)
            Collection<String> launcherNames = new ArrayList<>();
            launcherNames.add(appName);
            final AtomicBoolean nullLauncherNameFound = new AtomicBoolean(false);
            // check "no launcher names" and gather all names
            Optional.ofNullable(secondaryLaunchers).filter(list -> !list.isEmpty()).ifPresent(launchers -> {
                getLog().info("Adding configuration for secondary native launcher");
                nullLauncherNameFound.set(launchers.stream().anyMatch(launcher -> launcher.getAppName() == null));
                if( !nullLauncherNameFound.get() ){
                    launcherNames.addAll(launchers.stream().map(launcher -> launcher.getAppName()).collect(Collectors.toList()));

                    // assume we have valid entry here
                    params.put(StandardBundlerParam.SECONDARY_LAUNCHERS.getID(), launchers.stream().map(launcher -> {
                        getLog().info("Adding secondary launcher: " + launcher.getAppName());
                        Map<String, Object> secondaryLauncher = new HashMap<>();
                        addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getNativeReleaseVersion(), StandardBundlerParam.VERSION.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getVendor(), StandardBundlerParam.VENDOR.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getIdentifier(), StandardBundlerParam.IDENTIFIER.getID(), secondaryLauncher);

                        addToMapWhenNotNull(launcher.isNeedMenu(), StandardBundlerParam.MENU_HINT.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.isNeedShortcut(), StandardBundlerParam.SHORTCUT_HINT.getID(), secondaryLauncher);

                        // as we can set another JAR-file, this might be completly different
                        addToMapWhenNotNull(launcher.getClasspath(), StandardBundlerParam.CLASSPATH.getID(), secondaryLauncher);

                        Optional.ofNullable(launcher.getJvmArgs()).ifPresent(jvmOptions -> {
                            secondaryLauncher.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
                        });
                        Optional.ofNullable(launcher.getJvmProperties()).ifPresent(jvmProps -> {
                            secondaryLauncher.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
                        });
                        Optional.ofNullable(launcher.getUserJvmArgs()).ifPresent(userJvmOptions -> {
                            secondaryLauncher.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
                        });
                        Optional.ofNullable(launcher.getLauncherArguments()).ifPresent(arguments -> {
                            secondaryLauncher.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
                        });
                        return secondaryLauncher;
                    }).collect(Collectors.toList()));
                }
            });

            // check "no launcher names"
            if( nullLauncherNameFound.get() ){
                throw new MojoExecutionException("Not all secondary launchers have been configured properly.");
            }
            // check "duplicate launcher names"
            Set<String> duplicateLauncherNamesCheckSet = new HashSet<>();
            launcherNames.stream().forEach(launcherName -> duplicateLauncherNamesCheckSet.add(launcherName));
            if( duplicateLauncherNamesCheckSet.size() != launcherNames.size() ){
                throw new MojoExecutionException("Secondary launcher needs to have different name, please adjust appName inside your configuration.");
            }

            // check and prepare for file-associations (might not be present on all bundlers)
            Optional.ofNullable(fileAssociations).ifPresent(associations -> {
                final List<Map<String, ? super Object>> allAssociations = new ArrayList<>();
                associations.stream().forEach(association -> {
                    Map<String, ? super Object> settings = new HashMap<>();
                    settings.put(StandardBundlerParam.FA_DESCRIPTION.getID(), association.getDescription());
                    settings.put(StandardBundlerParam.FA_ICON.getID(), association.getIcon());
                    settings.put(StandardBundlerParam.FA_EXTENSIONS.getID(), association.getExtensions());
                    settings.put(StandardBundlerParam.FA_CONTENT_TYPE.getID(), association.getContentType());
                    allAssociations.add(settings);
                });
                params.put(StandardBundlerParam.FILE_ASSOCIATIONS.getID(), allAssociations);
            });

            // bugfix for "bundler not being able to produce native bundle without JRE on windows"
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
            if( genericWorkarounds.isWorkaroundForBug167Needed() ){
                if( !skipNativeLauncherWorkaround167 ){
                    genericWorkarounds.applyWorkaround167(params);
                } else {
                    getLog().info("Skipped workaround for native launcher regarding cfg-file-format.");
                }
            }

            Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
            Collection<Bundler> loadedBundlers = bundlers.getBundlers();

            // makes it possible to kick out all default bundlers
            if( onlyCustomBundlers ){
                loadedBundlers.clear();
            }

            // don't allow to overwrite existing bundler IDs
            List<String> existingBundlerIds = loadedBundlers.stream().map(existingBundler -> existingBundler.getID()).collect(Collectors.toList());

            Optional.ofNullable(customBundlers).ifPresent(customBundlerList -> {
                customBundlerList.stream().map(customBundlerClassName -> {
                    try{
                        Class<?> customBundlerClass = Class.forName(customBundlerClassName);
                        Bundler newCustomBundler = (Bundler) customBundlerClass.newInstance();
                        // if already existing (or already registered), skip this instance
                        if( existingBundlerIds.contains(newCustomBundler.getID()) ){
                            return null;
                        }
                        return newCustomBundler;
                    } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex){
                        getLog().warn("There was an exception while creating a new instance of custom bundler: " + customBundlerClassName, ex);
                    }
                    return null;
                }).filter(customBundler -> customBundler != null).forEach(customBundler -> {
                    if( onlyCustomBundlers ){
                        loadedBundlers.add(customBundler);
                    } else {
                        bundlers.loadBundler(customBundler);
                    }
                });
            });

            boolean foundBundler = false;

            // the new feature for only using custom bundlers made it necessary to check for empty bundlers list
            if( loadedBundlers.isEmpty() ){
                throw new MojoExecutionException("There were no bundlers registered. Please make sure to add your custom bundlers as dependency to the plugin.");
            }

            for( Bundler b : bundlers.getBundlers() ){
                String currentRunningBundlerID = b.getID();
                // sometimes we need to run this bundler, so do special check
                if( !shouldBundlerRun(bundler, currentRunningBundlerID, params) ){
                    continue;
                }

                foundBundler = true;
                try{
                    if( macSpecificWorkarounds.isWorkaroundForNativeMacBundlerNeeded(additionalBundlerResources) ){
                        if( !skipMacBundlerWorkaround ){
                            b = macSpecificWorkarounds.applyWorkaroundForNativeMacBundler(b, currentRunningBundlerID, params, additionalBundlerResources);
                        } else {
                            getLog().info("Skipping replacement of the 'mac.app'-bundler. Please make sure you know what you are doing!");
                        }
                    }

                    Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);

                    if( b.validate(paramsToBundleWith) ){

                        doPrepareBeforeBundling(currentRunningBundlerID, paramsToBundleWith);

                        // "jnlp bundler doesn't produce jnlp file and doesn't log any error/warning"
                        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/42
                        // the new jnlp-bundler does not work like other bundlers, you have to provide some bundleArguments-entry :(
                        if( "jnlp".equals(currentRunningBundlerID) && !paramsToBundleWith.containsKey("jnlp.outfile") ){
                            // do fail if JNLP-bundler has to run
                            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/238
                            if( failOnError ){
                                throw new MojoExecutionException("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                            } else {
                                getLog().warn("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                                continue;
                            }
                        }

                        // DO BUNDLE HERE ;) and don't get confused about all the other stuff
                        b.execute(paramsToBundleWith, nativeOutputDir);

                        applyWorkaroundsAfterBundling(currentRunningBundlerID, params);
                    }
                } catch(UnsupportedPlatformException e){
                    // quietly ignored
                } catch(ConfigException e){
                    if( failOnError ){
                        throw new MojoExecutionException("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                    } else {
                        getLog().info("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                    }

                }
            }
            if( !foundBundler ){
                if( failOnError ){
                    throw new MojoExecutionException("No bundler found for given id " + bundler + ". Please check your configuration.");
                }
                getLog().warn("No bundler found for given id " + bundler + ". Please check your configuration.");
            }
        } catch(RuntimeException e){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        } catch(PackagerException ex){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", ex);
        }
    }

    protected void applyWorkaroundsAfterBundling(String currentRunningBundlerID, Map<String, ? super Object> params) throws PackagerException, MojoFailureException, MojoExecutionException {
        // Workaround for "Native package for Ubuntu doesn't work"
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
        // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
        if( linuxSpecificWorkarounds.isWorkaroundForCfgFileNameNeeded() ){
            if( "linux.app".equals(currentRunningBundlerID) ){
                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s).");
                if( !skipNativeLauncherWorkaround124 ){
                    linuxSpecificWorkarounds.applyWorkaroundForCfgFileName(appName, secondaryLaunchers);
                    // only apply workaround for issue 205 when having workaround for issue 124 active
                    if( Boolean.parseBoolean(String.valueOf(params.get(LinuxSpecificWorkarounds.CFG_WORKAROUND_MARKER))) && !Boolean.parseBoolean((String) params.get(LinuxSpecificWorkarounds.CFG_WORKAROUND_DONE_MARKER)) ){
                        getLog().info("Preparing workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s) inside native linux installers.");
                        linuxSpecificWorkarounds.applyWorkaroundForCfgFileNameInsideInstallers(appName, secondaryLaunchers, params);
                        params.put(LinuxSpecificWorkarounds.CFG_WORKAROUND_DONE_MARKER, "true");
                    }
                } else {
                    getLog().info("Skipped workaround for native linux launcher(s).");
                }
            }
        }

        if( "jnlp".equals(currentRunningBundlerID) ){
            if( windowsSpecificWorkarounds.isWorkaroundForBug182Needed() ){
                // Workaround for "JNLP-generation: path for dependency-lib on windows with backslash"
                // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/182
                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding jar-path inside generated JNLP-files.");
                if( !skipJNLPRessourcePathWorkaround182 ){
                    genericWorkarounds.fixPathsInsideJNLPFiles();
                } else {
                    getLog().info("Skipped workaround for jar-paths jar-path inside generated JNLP-files.");
                }
            }

            // Do sign generated jar-files by calling the packager (this might change in the future,
            // hopefully when oracle reworked the process inside the JNLP-bundler)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/185
            if( genericWorkarounds.isWorkaroundForBug185Needed(params) ){
                getLog().info("Signing jar-files referenced inside generated JNLP-files.");
                if( !skipSigningJarFilesJNLP185 ){
                    // JavaFX signing using BLOB method will get dropped on JDK 9: "blob signing is going away in JDK9. "
                    // https://bugs.openjdk.java.net/browse/JDK-8088866?focusedCommentId=13889898#comment-13889898
                    if( !noBlobSigning ){
                        getLog().info("Signing jar-files using BLOB method.");
                        signJarFilesUsingBlobSigning();
                    } else {
                        getLog().info("Signing jar-files using jarsigner.");
                        signJarFiles();
                    }
                    genericWorkarounds.applyWorkaround185(skipSizeRecalculationForJNLP185);
                } else {
                    getLog().info("Skipped signing jar-files referenced inside JNLP-files.");
                }
            }
        }
    }

    protected void doPrepareBeforeBundling(String currentRunningBundlerID, Map<String, ? super Object> paramsToBundleWith) {
        // copy all files every time a bundler runs, because they might cleanup their folders,
        // but user might have extend existing bundler using same foldername (which would end up deleted/cleaned up)
        // fixes "Make it possible to have additional resources for bundlers"
        // see https://github.com/FibreFoX/javafx-gradle-plugin/issues/38
        if( additionalBundlerResources != null && additionalBundlerResources.exists() ){
            boolean skipCopyAdditionalBundlerResources = false;

            // keep previous behaviour
            Path additionalBundlerResourcesPath = additionalBundlerResources.toPath();
            Path resolvedBundlerFolder = additionalBundlerResourcesPath.resolve(currentRunningBundlerID);

            if( baseSettings.isVerbose() ){
                getLog().info("Additional bundler resources are specified, trying to copy all files into build root, using:" + additionalBundlerResources.getAbsolutePath());
            }

            File bundlerImageRoot = AbstractBundler.IMAGES_ROOT.fetchFrom(paramsToBundleWith);
            Path targetFolder = bundlerImageRoot.toPath();
            Path sourceFolder = additionalBundlerResourcesPath;

            // only do copy-stuff when having non-image bundlers
            switch(currentRunningBundlerID) {
                case "windows.app":
                    // no copy required, as we already have "additionalAppResources"
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "windows.service":
                    // no copy required, as we already have "additionalAppResources"
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "mac.app":
                    // custom mac bundler might be used
                    if( skipMacBundlerWorkaround ){
                        getLog().warn("The bundler with ID 'mac.app' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                    }
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "mac.daemon":
                    // this bundler just deletes everything ... it has no bundlerRoot
                    getLog().warn("The bundler with ID 'mac.daemon' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "linux.app":
                    // no copy required, as we already have "additionalAppResources"
                    skipCopyAdditionalBundlerResources = true;
                    break;
            }

            if( !skipCopyAdditionalBundlerResources ){
                // new behaviour, use bundler-name as folder-name
                if( Files.exists(resolvedBundlerFolder) ){
                    if( baseSettings.isVerbose() ){
                        getLog().info("Found additional bundler resources for bundler " + currentRunningBundlerID);
                    }
                    sourceFolder = resolvedBundlerFolder;
                    // change behaviour to have more control for all bundlers being inside JDK
                    switch(currentRunningBundlerID) {
                        case "exe":
                            File exeBundlerFolder = WinExeBundler.EXE_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = exeBundlerFolder.toPath();
                            break;
                        case "msi":
                            File msiBundlerFolder = WinMsiBundler.MSI_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = msiBundlerFolder.toPath();
                            break;
                        case "mac.appStore":
                            // custom mac bundler might be used
                            if( skipMacBundlerWorkaround ){
                                getLog().warn("The bundler with ID 'mac.appStore' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "dmg":
                            // custom mac bundler might be used
                            if( skipMacBundlerWorkaround ){
                                getLog().warn("The bundler with ID 'dmg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "pkg":
                            // custom mac bundler might be used
                            if( skipMacBundlerWorkaround ){
                                getLog().warn("The bundler with ID 'pkg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "deb":
                            File linuxDebBundlerFolder = LinuxDebBundler.DEB_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = linuxDebBundlerFolder.toPath();
                            break;
                        case "rpm":
                            File linuxRpmBundlerFolder = LinuxRpmBundler.RPM_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = linuxRpmBundlerFolder.toPath();
                            break;
                        default:
                            // we may have custom bundler ;)
                            getLog().warn("Unknown bundler-ID found, copying from root of additionalBundlerResources into IMAGES_ROOT.");
                            sourceFolder = additionalBundlerResources.toPath();
                            break;
                    }
                } else {
                    if( baseSettings.isVerbose() ){
                        getLog().info("No additional bundler resources for bundler " + currentRunningBundlerID + " were found, copying all files instead.");
                    }
                }
                if( !skipCopyAdditionalBundlerResources ){
                    try{
                        if( baseSettings.isVerbose() ){
                            getLog().info("Copying additional bundler resources into: " + targetFolder.toFile().getAbsolutePath());
                        }
                        new FileHelper().copyRecursive(sourceFolder, targetFolder, getLog());
                    } catch(IOException e){
                        getLog().warn("Couldn't copy additional bundler resource-file(s).", e);
                    }
                }
            }
        }
        checkAndWarnAboutSlowPerformance(currentRunningBundlerID);
    }

    protected void checkAndWarnAboutSlowPerformance(String currentRunningBundlerID) {
        // check if we need to inform the user about low performance even on SSD
        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/41
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") && "deb".equals(currentRunningBundlerID) ){
            AtomicBoolean needsWarningAboutSlowPerformance = new AtomicBoolean(false);
            nativeOutputDir.toPath().getFileSystem().getFileStores().forEach(store -> {
                if( "ext4".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
                if( "btrfs".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
            });
            if( needsWarningAboutSlowPerformance.get() ){
                getLog().info("This bundler might take some while longer than expected.");
                getLog().info("For details about this, please go to: https://wiki.debian.org/Teams/Dpkg/FAQ#Q:_Why_is_dpkg_so_slow_when_using_new_filesystems_such_as_btrfs_or_ext4.3F");
            }
        }
    }

    /*
     * Sometimes we need to work with some bundler, even if it wasn't requested. This happens when one bundler was selected and we need
     * to work with the outcome of some image-bundler (because that JDK-bundler is faulty).
     */
    protected boolean shouldBundlerRun(String requestedBundler, String currentRunningBundlerID, Map<String, ? super Object> params) {
        if( requestedBundler != null && !"ALL".equalsIgnoreCase(requestedBundler) && !requestedBundler.equalsIgnoreCase(currentRunningBundlerID) ){
            // this is not the specified bundler
            return false;
        }

        if( skipJNLP && "jnlp".equalsIgnoreCase(currentRunningBundlerID) ){
            getLog().info("Skipped JNLP-bundling as requested.");
            return false;
        }

        boolean runBundler = true;
        // Workaround for native installer bundle not creating working executable native launcher
        // (this is a comeback of issue 124)
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
        // do run application bundler and put the cfg-file to application resources
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") ){
            if( linuxSpecificWorkarounds.isWorkaroundForBug205Needed() ){
                // check if special conditions for this are met (not jnlp, but not linux.app too, because another workaround already works)
                if( !"jnlp".equalsIgnoreCase(requestedBundler) && !"linux.app".equalsIgnoreCase(requestedBundler) && "linux.app".equalsIgnoreCase(currentRunningBundlerID) ){
                    if( !skipNativeLauncherWorkaround205 ){
                        getLog().info("Detected linux application bundler ('linux.app') needs to run before installer bundlers are executed.");
                        runBundler = true;
                        params.put(LinuxSpecificWorkarounds.CFG_WORKAROUND_MARKER, "true");
                    } else {
                        getLog().info("Skipped workaround for native linux installer bundlers.");
                    }
                }
            }
        }
        return runBundler;
    }

    protected void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

    protected void signJarFilesUsingBlobSigning() throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration();

        SignJarParams signJarParams = new SignJarParams();
        signJarParams.setVerbose(baseSettings.isVerbose());
        signJarParams.setKeyStore(keyStore);
        signJarParams.setAlias(keyStoreAlias);
        signJarParams.setStorePass(keyStorePassword);
        signJarParams.setKeyPass(keyPassword);
        signJarParams.setStoreType(keyStoreType);

        signJarParams.addResource(nativeOutputDir, jfxJarSettings.getJfxMainAppJarName());

        // add all gathered jar-files as resources so be signed
        genericWorkarounds.getJARFilesFromJNLPFiles().forEach(jarFile -> signJarParams.addResource(nativeOutputDir, jarFile));

        getLog().info("Signing JAR files for jnlp bundle using BLOB-method");
        try{
            JavaTools.addFolderToClassloader(baseSettings.getDeployDir());
        } catch(Exception e){
            throw new MojoExecutionException("Unable to sign JFX JAR", e);
        }
        Log.setLogger(new Log.Logger(baseSettings.isVerbose()));
        new PackagerLib().signJar(signJarParams);
    }

    protected void signJarFiles() throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration();

        AtomicReference<MojoExecutionException> exception = new AtomicReference<>();
        genericWorkarounds.getJARFilesFromJNLPFiles().stream().map(relativeJarFilePath -> new File(nativeOutputDir, relativeJarFilePath)).forEach(jarFile -> {
            try{
                // only sign when there wasn't already some problem
                if( exception.get() == null ){
                    signJar(jarFile.getAbsoluteFile());
                }
            } catch(MojoExecutionException ex){
                // rethrow later (same trick is done inside apache-tomee project ;D)
                exception.set(ex);
            }
        });
        if( exception.get() != null ){
            throw exception.get();
        }
    }

    protected void checkSigningConfiguration() throws MojoFailureException {
        if( skipKeyStoreChecking ){
            getLog().info("Skipped checking if keystore exists.");
        } else {
            if( !keyStore.exists() ){
                throw new MojoFailureException("Keystore does not exist, use 'jfx:generate-key-store' command to make one (expected at: " + keyStore + ")");
            }
        }

        if( keyStoreAlias == null || keyStoreAlias.isEmpty() ){
            throw new MojoFailureException("A 'keyStoreAlias' is required for signing JARs");
        }

        if( keyStorePassword == null || keyStorePassword.isEmpty() ){
            throw new MojoFailureException("A 'keyStorePassword' is required for signing JARs");
        }

        // fallback
        if( keyPassword == null ){
            keyPassword = keyStorePassword;
        }
    }

    protected void signJar(File jarFile) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(JavaTools.getExecutablePath(nativeAppSettings.isUseEnvironmentRelativeExecutables()) + "jarsigner");

        // check is required for non-file keystores, see #291
        AtomicBoolean containsKeystore = new AtomicBoolean(false);

        Optional.ofNullable(additionalJarsignerParameters).ifPresent(jarsignerParameters -> {
            containsKeystore.set(jarsignerParameters.stream().filter(jarsignerParameter -> "-keystore".equalsIgnoreCase(jarsignerParameter.trim())).count() > 0);
            command.addAll(jarsignerParameters);
        });
        command.add("-strict");

        if( !containsKeystore.get() ){
            command.add("-keystore");
            // might be null, because check might be skipped
            if( keyStore != null ){
                command.add(keyStore.getAbsolutePath());
            }
        }
        command.add("-storepass");
        command.add(keyStorePassword);
        if( !skipKeypassWhileSigning ){
            command.add("-keypass");
            command.add(keyPassword);
        }
        command.add(jarFile.getAbsolutePath());
        command.add(keyStoreAlias);

        if( baseSettings.isVerbose() ){
            command.add("-verbose");
        }

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(mavenSettings.getProject().getBasedir())
                    .command(command);

            if( baseSettings.isVerbose() ){
                getLog().info("Running command: " + String.join(" ", command));
            }

            getLog().info("Signing JAR files for jnlp bundle using jarsigner-method");
            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new MojoExecutionException("Signing jar using jarsigner wasn't successful! Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while signing jar-file: " + jarFile.getAbsolutePath(), ex);
        }
    }

    protected boolean isClassInsideJarFile(String classname, File jarFile) {
        String requestedJarEntryName = classname.replace(".", "/") + ".class";
        try{
            JarFile jarFileToSearchIn = new JarFile(jarFile, false, JarFile.OPEN_READ);
            return jarFileToSearchIn.stream().parallel().filter(jarEntry -> {
                return jarEntry.getName().equals(requestedJarEntryName);
            }).findAny().isPresent();
        } catch(IOException ex){
            // NO-OP
        }
        return false;
    }
}
