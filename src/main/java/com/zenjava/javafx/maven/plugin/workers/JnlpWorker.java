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
package com.zenjava.javafx.maven.plugin.workers;

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.PackagerException;
import com.zenjava.javafx.maven.plugin.settings.BaseSettings;
import com.zenjava.javafx.maven.plugin.utils.BuildLogger;
import com.zenjava.javafx.maven.plugin.utils.FileHelper;
import com.zenjava.javafx.maven.plugin.workarounds.GenericWorkarounds;
import com.zenjava.javafx.maven.plugin.workarounds.LinuxSpecificWorkarounds;
import com.zenjava.javafx.maven.plugin.workarounds.MacSpecificWorkarounds;
import com.zenjava.javafx.maven.plugin.workarounds.WindowsSpecificWorkarounds;
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
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author FibreFoX
 */
public class JnlpWorker {
/*
    public void execute(BaseSettings baseSettings, BuildLogger buildLogger) {
        if( baseSettings.isSkip() ){
            buildLogger.info("Skipping execution of NativeMojo MOJO.");
            return;
        }

        buildLogger.info("Building Native Launcher");

        WindowsSpecificWorkarounds windowsSpecificWorkarounds = new WindowsSpecificWorkarounds(nativeOutputDir, buildLogger);
        LinuxSpecificWorkarounds linuxSpecificWorkarounds = new LinuxSpecificWorkarounds(nativeOutputDir, buildLogger);
        MacSpecificWorkarounds macSpecificWorkarounds = new MacSpecificWorkarounds(nativeOutputDir, buildLogger);
        GenericWorkarounds genericWorkarounds = new GenericWorkarounds(nativeOutputDir, buildLogger);

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
                    new FileHelper().copyRecursive(sourceFolder, targetFolder, buildLogger);
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
*/
}
