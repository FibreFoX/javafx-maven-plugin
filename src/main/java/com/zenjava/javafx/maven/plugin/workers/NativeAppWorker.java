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
import com.zenjava.javafx.maven.plugin.settings.BaseSettings;
import com.zenjava.javafx.maven.plugin.settings.FeatureSwitches;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.settings.KeystoreSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeAppSettings;
import com.zenjava.javafx.maven.plugin.settings.WorkaroundSwitches;
import com.zenjava.javafx.maven.plugin.utils.BuildLogger;
import com.zenjava.javafx.maven.plugin.utils.FileHelper;
import com.zenjava.javafx.maven.plugin.utils.JavaTools;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author FibreFoX
 */
public class NativeAppWorker {

    public void execute(BaseSettings baseSettings, JfxJarSettings jfxJarSettings, NativeAppSettings nativeAppSettings, KeystoreSettings keystoreSettings, FeatureSwitches featureSwitches, WorkaroundSwitches workaroundSwitches, BuildLogger buildLogger) throws MojoExecutionException, MojoFailureException {
        buildLogger.info("Building Native Application Bundle");

        // first we will get all the existing bundlers
        Bundlers bundlers = Bundlers.createBundlersInstance(Thread.currentThread().getContextClassLoader()); // JDK 9 uses ServiceLoader to detect, so pass the current classloader
        Collection<Bundler> loadedBundlers = bundlers.getBundlers();

        // don't allow to overwrite existing bundler IDs
        List<String> existingBundlerIds = loadedBundlers.stream().map(existingBundler -> existingBundler.getID()).collect(Collectors.toList());

        // check if selected bundler OS is current OS
        String bundlerForCurrentOS = null;
        if( JavaTools.IS_WINDOWS ){
            bundlerForCurrentOS = "windows.app";
        }
        if( JavaTools.IS_LINUX ){
            bundlerForCurrentOS = "linux.app";
        }
        if( JavaTools.IS_MAC ){
            bundlerForCurrentOS = "mac.app";
        }
        if( bundlerForCurrentOS == null ){
            // TODO cant detect current OS
            return;
        }
        if( !existingBundlerIds.contains(bundlerForCurrentOS) ){
            // TODO there is no bundler :(
            return;
        }

        // makes it possible to provide settings for ALL systems, but only execute possible ones
        if( !("*".equals(nativeAppSettings.getBundler()) || bundlerForCurrentOS.equals(nativeAppSettings.getBundler())) ){
            buildLogger.info("Skipping build, because bundler did not match");
            return;
        }

        // check if the bundler for the current os was specified
        WindowsSpecificWorkarounds windowsSpecificWorkarounds = new WindowsSpecificWorkarounds(nativeAppSettings.getNativeOutputDir(), buildLogger);
        LinuxSpecificWorkarounds linuxSpecificWorkarounds = new LinuxSpecificWorkarounds(nativeAppSettings.getNativeOutputDir(), buildLogger);
        MacSpecificWorkarounds macSpecificWorkarounds = new MacSpecificWorkarounds(nativeAppSettings.getNativeOutputDir(), buildLogger);
        GenericWorkarounds genericWorkarounds = new GenericWorkarounds(nativeAppSettings.getNativeOutputDir(), buildLogger);

        try{
            Map<String, ? super Object> params = new HashMap<>();

            // make bundlers doing verbose output (might not always be as verbose as expected)
            params.put(StandardBundlerParam.VERBOSE.getID(), baseSettings.isVerbose());

            Optional.ofNullable(nativeAppSettings.getIdentifier()).ifPresent(id -> {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
            });

            params.put(StandardBundlerParam.APP_NAME.getID(), nativeAppSettings.getAppName());

            // version is used for CFBundleVersion on mac
            params.put(StandardBundlerParam.VERSION.getID(), nativeAppSettings.getNativeReleaseVersion());
            // replace that value
            if( !featureSwitches.isSkipNativeVersionNumberSanitizing() && nativeAppSettings.getNativeReleaseVersion() != null ){
                params.put(StandardBundlerParam.VERSION.getID(), nativeAppSettings.getNativeReleaseVersion().replaceAll("[^\\d.]", ""));
            }

            params.put(StandardBundlerParam.MAIN_CLASS.getID(), jfxJarSettings.getMainClass());

            Optional.ofNullable(nativeAppSettings.getJvmProperties()).ifPresent(jvmProps -> {
                params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
            });
            Optional.ofNullable(nativeAppSettings.getJvmArgs()).ifPresent(jvmOptions -> {
                params.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
            });
            Optional.ofNullable(nativeAppSettings.getUserJvmArgs()).ifPresent(userJvmOptions -> {
                params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
            });
            Optional.ofNullable(nativeAppSettings.getLauncherArguments()).ifPresent(arguments -> {
                params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
            });

            // bugfix for #83 (by copying additional resources to /target/jfx/app folder)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/83
            Optional.ofNullable(jfxJarSettings.getAdditionalAppResources()).filter(File::exists).ifPresent(appResources -> {
                try{
                    Path targetFolder = jfxJarSettings.getOutputFolderName().toPath();
                    Path sourceFolder = appResources.toPath();
                    new FileHelper().copyRecursive(sourceFolder, targetFolder, buildLogger);
                } catch(IOException e){
                    buildLogger.warn(e);
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
                            buildLogger.info(String.format("Add %s file to application resources.", f));
                            resourceFiles.add(f);
                        });
            } catch(IOException e){
                buildLogger.warn(e);
            }
            params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(jfxJarSettings.getOutputFolderName(), resourceFiles));

            // check for misconfiguration
            Collection<String> duplicateKeys = new HashSet<>();
            Optional.ofNullable(nativeAppSettings.getBundleArguments()).ifPresent(bArguments -> {
                duplicateKeys.addAll(params.keySet());
                duplicateKeys.retainAll(bArguments.keySet());
                params.putAll(bArguments);
            });

            if( !duplicateKeys.isEmpty() ){
                throw new MojoExecutionException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
            }

            if( !featureSwitches.isSkipMainClassScanning() ){
                boolean mainClassInsideResourceJarFile = resourceFiles.stream().filter(resourceFile -> resourceFile.toString().endsWith(".jar")).filter(resourceJarFile -> isClassInsideJarFile(jfxJarSettings.getMainClass(), resourceJarFile)).findFirst().isPresent();
                if( !mainClassInsideResourceJarFile ){
                    // warn user about missing class-file
                    buildLogger.warn(String.format("Class with name %s was not found inside provided jar files!! JavaFX-application might not be working !!", jfxJarSettings.getMainClass()));
                }
            }

            // check for secondary launcher misconfiguration (their appName requires to be different as this would overwrite primary launcher)
            Collection<String> launcherNames = new ArrayList<>();
            launcherNames.add(nativeAppSettings.getAppName());
            final AtomicBoolean nullLauncherNameFound = new AtomicBoolean(false);
            // check "no launcher names" and gather all names
            Optional.ofNullable(nativeAppSettings.getSecondaryLaunchers()).filter(list -> !list.isEmpty()).ifPresent(launchers -> {
                buildLogger.info("Adding configuration for secondary native launcher");
                nullLauncherNameFound.set(launchers.stream().anyMatch(launcher -> launcher.getAppName() == null));
                if( !nullLauncherNameFound.get() ){
                    launcherNames.addAll(launchers.stream().map(launcher -> launcher.getAppName()).collect(Collectors.toList()));

                    // assume we have valid entry here
                    params.put(StandardBundlerParam.SECONDARY_LAUNCHERS.getID(), launchers.stream().map(launcher -> {
                        buildLogger.info("Adding secondary launcher: " + launcher.getAppName());
                        Map<String, Object> secondaryLauncher = new HashMap<>();
                        addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncher);
                        // TODO add native release version sanitizing
                        addToMapWhenNotNull(launcher.getNativeReleaseVersion(), StandardBundlerParam.VERSION.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getIdentifier(), StandardBundlerParam.IDENTIFIER.getID(), secondaryLauncher);

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
                if( !workaroundSwitches.isSkipNativeLauncherWorkaround167() ){
                    genericWorkarounds.applyWorkaround167(params);
                } else {
                    buildLogger.info("Skipped workaround for native launcher regarding cfg-file-format.");
                }
            }
            /*
             * Optional.ofNullable(nativeAppSettings.getCustomBundlers()).ifPresent(customBundlerList -> {
             * customBundlerList.stream().map(customBundlerClassName -> {
             * try{
             * Class<?> customBundlerClass = Class.forName(customBundlerClassName);
             * Bundler newCustomBundler = (Bundler) customBundlerClass.newInstance();
             * // if already existing (or already registered), skip this instance
             * if( existingBundlerIds.contains(newCustomBundler.getID()) ){
             * return null;
             * }
             * return newCustomBundler;
             * } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex){
             * getLog().warn("There was an exception while creating a new instance of custom bundler: " + customBundlerClassName, ex);
             * }
             * return null;
             * }).filter(customBundler -> customBundler != null).forEach(customBundler -> {
             * if( onlyCustomBundlers ){
             * loadedBundlers.add(customBundler);
             * } else {
             * bundlers.loadBundler(customBundler);
             * }
             * });
             * });
             * */

            boolean foundBundler = false;

            // the new feature for only using custom bundlers made it necessary to check for empty bundlers list
            if( loadedBundlers.isEmpty() ){
                throw new MojoExecutionException("There were no bundlers registered. Please make sure to add your custom bundlers as dependency to the plugin.");
            }

            for( Bundler b : bundlers.getBundlers() ){
                String currentRunningBundlerID = b.getID();
                // sometimes we need to run this bundler, so do special check
                if( !shouldBundlerRun(nativeAppSettings.getBundler(), currentRunningBundlerID, params) ){
                    continue;
                }

                foundBundler = true;
                try{
                    if( macSpecificWorkarounds.isWorkaroundForNativeMacBundlerNeeded(nativeAppSettings.getAdditionalBundlerResources()) ){
                        if( !workaroundSwitches.isSkipMacBundlerWorkaround() ){
                            b = macSpecificWorkarounds.applyWorkaroundForNativeMacBundler(b, currentRunningBundlerID, params, nativeAppSettings.getAdditionalBundlerResources());
                        } else {
                            buildLogger.info("Skipping replacement of the 'mac.app'-bundler. Please make sure you know what you are doing!");
                        }
                    }

                    Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);

                    if( b.validate(paramsToBundleWith) ){

                        doPrepareBeforeBundling(baseSettings, nativeAppSettings, workaroundSwitches, currentRunningBundlerID, paramsToBundleWith, buildLogger);

                        // DO BUNDLE HERE ;) and don't get confused about all the other stuff
                        b.execute(paramsToBundleWith, nativeAppSettings.getNativeOutputDir());

                        applyWorkaroundsAfterBundling(genericWorkarounds, linuxSpecificWorkarounds, baseSettings, jfxJarSettings, nativeAppSettings, keystoreSettings, featureSwitches, workaroundSwitches, currentRunningBundlerID, params, buildLogger);
                    }
                } catch(UnsupportedPlatformException e){
                    // quietly ignored
                } catch(ConfigException e){
                    if( baseSettings.isFailOnError() ){
                        throw new MojoExecutionException("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                    } else {
                        buildLogger.info("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                    }

                }
            }
            if( !foundBundler ){
                if( baseSettings.isFailOnError() ){
                    throw new MojoExecutionException("No bundler found for given id " + nativeAppSettings.getBundler() + ". Please check your configuration.");
                }
                buildLogger.warn("No bundler found for given id " + nativeAppSettings.getBundler() + ". Please check your configuration.");
            }
        } catch(RuntimeException e){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        } catch(PackagerException ex){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", ex);
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

    protected void signJar(BaseSettings baseSettings, KeystoreSettings keystoreSettings, NativeAppSettings nativeAppSettings, FeatureSwitches featureSwitches, File jarFile, BuildLogger buildLogger) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(JavaTools.getExecutablePath(nativeAppSettings.isUseEnvironmentRelativeExecutables()) + "jarsigner");

        // check is required for non-file keystores, see #291
        AtomicBoolean containsKeystore = new AtomicBoolean(false);

        Optional.ofNullable(nativeAppSettings.getAdditionalJarsignerParameters()).ifPresent(jarsignerParameters -> {
            containsKeystore.set(jarsignerParameters.stream().filter(jarsignerParameter -> "-keystore".equalsIgnoreCase(jarsignerParameter.trim())).count() > 0);
            command.addAll(jarsignerParameters);
        });
        command.add("-strict");

        if( !containsKeystore.get() ){
            command.add("-keystore");
            // might be null, because check might be skipped
            if( keystoreSettings.getKeyStore() != null ){
                command.add(keystoreSettings.getKeyStore().getAbsolutePath());
            }
        }
        command.add("-storepass");
        command.add(keystoreSettings.getKeyStorePassword());
        if( !featureSwitches.isSkipKeypassWhileSigning() ){
            command.add("-keypass");
            command.add(keystoreSettings.getKeyPassword());
        }
        command.add(jarFile.getAbsolutePath());
        command.add(keystoreSettings.getKeyStoreAlias());

        if( baseSettings.isVerbose() ){
            command.add("-verbose");
        }

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(mavenSettings.getProject().getBasedir())
                    .command(command);

            if( baseSettings.isVerbose() ){
                buildLogger.info("Running command: " + String.join(" ", command));
            }

            buildLogger.info("Signing JAR files for jnlp bundle using jarsigner-method");
            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new MojoExecutionException("Signing jar using jarsigner wasn't successful! Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while signing jar-file: " + jarFile.getAbsolutePath(), ex);
        }
    }

    protected void signJarFilesUsingBlobSigning(GenericWorkarounds genericWorkarounds, BaseSettings baseSettings, JfxJarSettings jfxJarSettings, NativeAppSettings nativeAppSettings, KeystoreSettings keystoreSettings, FeatureSwitches featureSwitches, BuildLogger buildLogger) throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration(keystoreSettings, featureSwitches, buildLogger);

        SignJarParams signJarParams = new SignJarParams();
        signJarParams.setVerbose(baseSettings.isVerbose());
        signJarParams.setKeyStore(keystoreSettings.getKeyStore());
        signJarParams.setAlias(keystoreSettings.getKeyStoreAlias());
        signJarParams.setStorePass(keystoreSettings.getKeyStorePassword());
        signJarParams.setKeyPass(keystoreSettings.getKeyPassword());
        signJarParams.setStoreType(keystoreSettings.getKeyStoreType());

        signJarParams.addResource(nativeAppSettings.getNativeOutputDir(), jfxJarSettings.getJfxMainAppJarName());

        // add all gathered jar-files as resources so be signed
        genericWorkarounds.getJARFilesFromJNLPFiles().forEach(jarFile -> signJarParams.addResource(nativeAppSettings.getNativeOutputDir(), jarFile));

        buildLogger.info("Signing JAR files for jnlp bundle using BLOB-method");
        try{
            JavaTools.addFolderToClassloader(baseSettings.getDeployDir());
        } catch(Exception e){
            throw new MojoExecutionException("Unable to sign JFX JAR", e);
        }
        Log.setLogger(new Log.Logger(baseSettings.isVerbose()));
        new PackagerLib().signJar(signJarParams);
    }

    protected void signJarFiles(GenericWorkarounds genericWorkarounds, BaseSettings baseSettings, NativeAppSettings nativeAppSettings, KeystoreSettings keystoreSettings, FeatureSwitches featureSwitches, BuildLogger buildLogger) throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration(keystoreSettings, featureSwitches, buildLogger);

        AtomicReference<MojoExecutionException> exception = new AtomicReference<>();
        genericWorkarounds.getJARFilesFromJNLPFiles().stream().map(relativeJarFilePath -> new File(nativeAppSettings.getNativeOutputDir(), relativeJarFilePath)).forEach(jarFile -> {
            try{
                // only sign when there wasn't already some problem
                if( exception.get() == null ){
                    signJar(baseSettings, keystoreSettings, nativeAppSettings, featureSwitches, jarFile.getAbsoluteFile(), buildLogger);
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

    protected void checkSigningConfiguration(KeystoreSettings keystoreSettings, FeatureSwitches featureSwitches, BuildLogger buildLogger) throws MojoFailureException {
        if( featureSwitches.isSkipKeyStoreChecking() ){
            buildLogger.info("Skipped checking if keystore exists.");
        } else {
            if( !keystoreSettings.getKeyStore().exists() ){
                throw new MojoFailureException("Keystore does not exist, use 'jfx:generate-key-store' command to make one (expected at: " + keystoreSettings.getKeyStore() + ")");
            }
        }

        if( keystoreSettings.getKeyStoreAlias() == null || keystoreSettings.getKeyStoreAlias().isEmpty() ){
            throw new MojoFailureException("A 'keyStoreAlias' is required for signing JARs");
        }

        if( keystoreSettings.getKeyStorePassword() == null || keystoreSettings.getKeyStorePassword().isEmpty() ){
            throw new MojoFailureException("A 'keyStorePassword' is required for signing JARs");
        }

        // fallback
        if( keystoreSettings.getKeyPassword() == null ){
            keystoreSettings.setKeyPassword(keystoreSettings.getKeyStorePassword());
        }
    }

    protected void applyWorkaroundsAfterBundling(GenericWorkarounds genericWorkarounds, LinuxSpecificWorkarounds linuxSpecificWorkarounds, BaseSettings baseSettings, JfxJarSettings jfxJarSettings, NativeAppSettings nativeAppSettings, KeystoreSettings keystoreSettings, FeatureSwitches featureSwitches, WorkaroundSwitches workaroundSwitches, String currentRunningBundlerID, Map<String, ? super Object> params, BuildLogger buildLogger) throws PackagerException, MojoFailureException, MojoExecutionException {
        // Workaround for "Native package for Ubuntu doesn't work"
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
        // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
        if( linuxSpecificWorkarounds.isWorkaroundForCfgFileNameNeeded() ){
            if( "linux.app".equals(currentRunningBundlerID) ){
                buildLogger.info("Applying workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s).");
                if( !workaroundSwitches.isSkipNativeLauncherWorkaround124() ){
                    linuxSpecificWorkarounds.applyWorkaroundForCfgFileName(nativeAppSettings.getAppName(), nativeAppSettings.getSecondaryLaunchers());
                    // only apply workaround for issue 205 when having workaround for issue 124 active
                    if( Boolean.parseBoolean(String.valueOf(params.get(LinuxSpecificWorkarounds.CFG_WORKAROUND_MARKER))) && !Boolean.parseBoolean((String) params.get(LinuxSpecificWorkarounds.CFG_WORKAROUND_DONE_MARKER)) ){
                        buildLogger.info("Preparing workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s) inside native linux installers.");
                        linuxSpecificWorkarounds.applyWorkaroundForCfgFileNameInsideInstallers(nativeAppSettings.getAppName(), nativeAppSettings.getSecondaryLaunchers(), params);
                        params.put(LinuxSpecificWorkarounds.CFG_WORKAROUND_DONE_MARKER, "true");
                    }
                } else {
                    buildLogger.info("Skipped workaround for native linux launcher(s).");
                }
            }
        }

        if( "jnlp".equals(currentRunningBundlerID) ){
            if( workaroundSwitches.isSkipJNLPRessourcePathWorkaround182() ){
                // Workaround for "JNLP-generation: path for dependency-lib on windows with backslash"
                // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/182
                buildLogger.info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding jar-path inside generated JNLP-files.");
                if( !workaroundSwitches.isSkipJNLPRessourcePathWorkaround182() ){
                    genericWorkarounds.fixPathsInsideJNLPFiles();
                } else {
                    buildLogger.info("Skipped workaround for jar-paths jar-path inside generated JNLP-files.");
                }
            }

            // Do sign generated jar-files by calling the packager (this might change in the future,
            // hopefully when oracle reworked the process inside the JNLP-bundler)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/185
            if( genericWorkarounds.isWorkaroundForBug185Needed(params) ){
                buildLogger.info("Signing jar-files referenced inside generated JNLP-files.");
                if( !workaroundSwitches.isSkipSigningJarFilesJNLP185() ){
                    // JavaFX signing using BLOB method will get dropped on JDK 9: "blob signing is going away in JDK9. "
                    // https://bugs.openjdk.java.net/browse/JDK-8088866?focusedCommentId=13889898#comment-13889898
                    if( !featureSwitches.isNoBlobSigning() ){
                        buildLogger.info("Signing jar-files using BLOB method.");
                        signJarFilesUsingBlobSigning(genericWorkarounds, baseSettings, jfxJarSettings, nativeAppSettings, keystoreSettings, featureSwitches, buildLogger);
                    } else {
                        buildLogger.info("Signing jar-files using jarsigner.");
                        signJarFiles(genericWorkarounds, baseSettings, nativeAppSettings, keystoreSettings, featureSwitches, buildLogger);
                    }
                    genericWorkarounds.applyWorkaround185(workaroundSwitches.isSkipSizeRecalculationForJNLP185());
                } else {
                    buildLogger.info("Skipped signing jar-files referenced inside JNLP-files.");
                }
            }
        }
    }

    protected void doPrepareBeforeBundling(BaseSettings baseSettings, NativeAppSettings nativeAppSettings, WorkaroundSwitches workaroundSwitches, String currentRunningBundlerID, Map<String, ? super Object> paramsToBundleWith, BuildLogger buildLogger) {
        // copy all files every time a bundler runs, because they might cleanup their folders,
        // but user might have extend existing bundler using same foldername (which would end up deleted/cleaned up)
        // fixes "Make it possible to have additional resources for bundlers"
        // see https://github.com/FibreFoX/javafx-gradle-plugin/issues/38
        if( nativeAppSettings.getAdditionalBundlerResources() != null && nativeAppSettings.getAdditionalBundlerResources().exists() ){
            boolean skipCopyAdditionalBundlerResources = false;

            // keep previous behaviour
            Path additionalBundlerResourcesPath = nativeAppSettings.getAdditionalBundlerResources().toPath();
            Path resolvedBundlerFolder = additionalBundlerResourcesPath.resolve(currentRunningBundlerID);

            if( baseSettings.isVerbose() ){
                buildLogger.info("Additional bundler resources are specified, trying to copy all files into build root, using:" + nativeAppSettings.getAdditionalBundlerResources().getAbsolutePath());
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
                    if( workaroundSwitches.isSkipMacBundlerWorkaround() ){
                        buildLogger.warn("The bundler with ID 'mac.app' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                    }
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "mac.daemon":
                    // this bundler just deletes everything ... it has no bundlerRoot
                    buildLogger.warn("The bundler with ID 'mac.daemon' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
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
                        buildLogger.info("Found additional bundler resources for bundler " + currentRunningBundlerID);
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
                            if( workaroundSwitches.isSkipMacBundlerWorkaround() ){
                                buildLogger.warn("The bundler with ID 'mac.appStore' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "dmg":
                            // custom mac bundler might be used
                            if( workaroundSwitches.isSkipMacBundlerWorkaround() ){
                                buildLogger.warn("The bundler with ID 'dmg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "pkg":
                            // custom mac bundler might be used
                            if( workaroundSwitches.isSkipMacBundlerWorkaround() ){
                                buildLogger.warn("The bundler with ID 'pkg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
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
                            buildLogger.warn("Unknown bundler-ID found, copying from root of additionalBundlerResources into IMAGES_ROOT.");
                            sourceFolder = nativeAppSettings.getAdditionalBundlerResources().toPath();
                            break;
                    }
                } else {
                    if( baseSettings.isVerbose() ){
                        buildLogger.info("No additional bundler resources for bundler " + currentRunningBundlerID + " were found, copying all files instead.");
                    }
                }
                if( !skipCopyAdditionalBundlerResources ){
                    try{
                        if( baseSettings.isVerbose() ){
                            buildLogger.info("Copying additional bundler resources into: " + targetFolder.toFile().getAbsolutePath());
                        }
                        new FileHelper().copyRecursive(sourceFolder, targetFolder, buildLogger);
                    } catch(IOException e){
                        buildLogger.warn("Couldn't copy additional bundler resource-file(s).", e);
                    }
                }
            }
        }
    }

    protected void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

}
