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
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.oracle.tools.packager.linux.LinuxDebBundler;
import com.oracle.tools.packager.linux.LinuxRpmBundler;
import com.oracle.tools.packager.windows.WinExeBundler;
import com.oracle.tools.packager.windows.WinMsiBundler;
import com.sun.javafx.tools.packager.PackagerException;
import com.zenjava.javafx.maven.plugin.settings.BaseSettings;
import com.zenjava.javafx.maven.plugin.settings.FeatureSwitches;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.settings.KeystoreSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeAppSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeInstallerSettings;
import com.zenjava.javafx.maven.plugin.settings.WorkaroundSwitches;
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

/**
 *
 * @author FibreFoX
 */
public class NativeInstallerWorker {

    public void execute(BaseSettings baseSettings, JfxJarSettings jfxJarSettings, NativeAppSettings nativeAppSettings, NativeInstallerSettings nativeInstallerSettings, KeystoreSettings keystoreSettings, FeatureSwitches featureSwitches, WorkaroundSwitches workaroundSwitches, BuildLogger buildLogger) throws MojoExecutionException {
        buildLogger.info("Building Native Launcher");

        WindowsSpecificWorkarounds windowsSpecificWorkarounds = new WindowsSpecificWorkarounds(nativeInstallerSettings.getNativeOutputDir(), buildLogger);
        LinuxSpecificWorkarounds linuxSpecificWorkarounds = new LinuxSpecificWorkarounds(nativeInstallerSettings.getNativeOutputDir(), buildLogger);
        MacSpecificWorkarounds macSpecificWorkarounds = new MacSpecificWorkarounds(nativeInstallerSettings.getNativeOutputDir(), buildLogger);
        GenericWorkarounds genericWorkarounds = new GenericWorkarounds(nativeInstallerSettings.getNativeOutputDir(), buildLogger);

        try{
            Map<String, ? super Object> params = new HashMap<>();

            // make bundlers doing verbose output (might not always be as verbose as expected)
            params.put(StandardBundlerParam.VERBOSE.getID(), baseSettings.isVerbose());

            Optional.ofNullable(nativeInstallerSettings.getIdentifier()).ifPresent(id -> {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
            });

            params.put(StandardBundlerParam.APP_NAME.getID(), nativeInstallerSettings.getAppName());
            params.put(StandardBundlerParam.VERSION.getID(), nativeInstallerSettings.getNativeReleaseVersion());
            // replace that value
            if( !featureSwitches.isSkipNativeVersionNumberSanitizing() && nativeInstallerSettings.getNativeReleaseVersion() != null ){
                params.put(StandardBundlerParam.VERSION.getID(), nativeInstallerSettings.getNativeReleaseVersion().replaceAll("[^\\d.]", ""));
            }
            params.put(StandardBundlerParam.VENDOR.getID(), nativeInstallerSettings.getVendor());
            params.put(StandardBundlerParam.SHORTCUT_HINT.getID(), nativeInstallerSettings.isCreateShortcut());
            params.put(StandardBundlerParam.MENU_HINT.getID(), nativeInstallerSettings.isCreateMenu());
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
            Optional.ofNullable(nativeInstallerSettings.getFileAssociations()).ifPresent(associations -> {
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
                if( !workaroundSwitches.isSkipNativeLauncherWorkaround167() ){
                    genericWorkarounds.applyWorkaround167(params);
                } else {
                    buildLogger.info("Skipped workaround for native launcher regarding cfg-file-format.");
                }
            }

            Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
            Collection<Bundler> loadedBundlers = bundlers.getBundlers();

            // TODO remove/rework this
            // makes it possible to kick out all default bundlers
//            if( onlyCustomBundlers ){
//                loadedBundlers.clear();
//            }
            // don't allow to overwrite existing bundler IDs
            List<String> existingBundlerIds = loadedBundlers.stream().map(existingBundler -> existingBundler.getID()).collect(Collectors.toList());

//            Optional.ofNullable(customBundlers).ifPresent(customBundlerList -> {
//                customBundlerList.stream().map(customBundlerClassName -> {
//                    try{
//                        Class<?> customBundlerClass = Class.forName(customBundlerClassName);
//                        Bundler newCustomBundler = (Bundler) customBundlerClass.newInstance();
//                        // if already existing (or already registered), skip this instance
//                        if( existingBundlerIds.contains(newCustomBundler.getID()) ){
//                            return null;
//                        }
//                        return newCustomBundler;
//                    } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex){
//                        buildLogger.warn("There was an exception while creating a new instance of custom bundler: " + customBundlerClassName, ex);
//                    }
//                    return null;
//                }).filter(customBundler -> customBundler != null).forEach(customBundler -> {
//                    if( onlyCustomBundlers ){
//                        loadedBundlers.add(customBundler);
//                    } else {
//                        bundlers.loadBundler(customBundler);
//                    }
//                });
//            });
            boolean foundBundler = false;

            // the new feature for only using custom bundlers made it necessary to check for empty bundlers list
            if( loadedBundlers.isEmpty() ){
                throw new MojoExecutionException("There were no bundlers registered. Please make sure to add your custom bundlers as dependency to the plugin.");
            }

            for( Bundler b : bundlers.getBundlers() ){
                String currentRunningBundlerID = b.getID();
                // sometimes we need to run this bundler, so do special check
                if( !shouldBundlerRun(nativeInstallerSettings.getBundler(), currentRunningBundlerID, params) ){
                    continue;
                }

                foundBundler = true;
                try{
                    if( macSpecificWorkarounds.isWorkaroundForNativeMacBundlerNeeded(nativeInstallerSettings.getAdditionalBundlerResources()) ){
                        if( !workaroundSwitches.isSkipMacBundlerWorkaround() ){
                            b = macSpecificWorkarounds.applyWorkaroundForNativeMacBundler(b, currentRunningBundlerID, params, nativeInstallerSettings.getAdditionalBundlerResources());
                        } else {
                            buildLogger.info("Skipping replacement of the 'mac.app'-bundler. Please make sure you know what you are doing!");
                        }
                    }

                    Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);

                    if( b.validate(paramsToBundleWith) ){

                        doPrepareBeforeBundling(baseSettings, nativeInstallerSettings, currentRunningBundlerID, paramsToBundleWith, workaroundSwitches, buildLogger);

                        // "jnlp bundler doesn't produce jnlp file and doesn't log any error/warning"
                        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/42
                        // the new jnlp-bundler does not work like other bundlers, you have to provide some bundleArguments-entry :(
                        if( "jnlp".equals(currentRunningBundlerID) && !paramsToBundleWith.containsKey("jnlp.outfile") ){
                            // do fail if JNLP-bundler has to run
                            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/238
                            if( baseSettings.isFailOnError() ){
                                throw new MojoExecutionException("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                            } else {
                                buildLogger.warn("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                                continue;
                            }
                        }

                        // DO BUNDLE HERE ;) and don't get confused about all the other stuff
                        b.execute(paramsToBundleWith, nativeInstallerSettings.getNativeOutputDir());

                        applyWorkaroundsAfterBundling(currentRunningBundlerID, params);
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
                    throw new MojoExecutionException("No bundler found for given id " + nativeInstallerSettings.getBundler() + ". Please check your configuration.");
                }
                buildLogger.warn("No bundler found for given id " + nativeInstallerSettings.getBundler() + ". Please check your configuration.");
            }
        } catch(RuntimeException e){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        } catch(PackagerException ex){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", ex);
        }
    }

    protected void checkAndWarnAboutSlowPerformance(String currentRunningBundlerID, NativeInstallerSettings nativeInstallerSettings, BuildLogger buildLogger) {
        // check if we need to inform the user about low performance even on SSD
        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/41
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") && "deb".equals(currentRunningBundlerID) ){
            AtomicBoolean needsWarningAboutSlowPerformance = new AtomicBoolean(false);
            nativeInstallerSettings.getNativeOutputDir().toPath().getFileSystem().getFileStores().forEach(store -> {
                if( "ext4".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
                if( "btrfs".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
            });
            if( needsWarningAboutSlowPerformance.get() ){
                buildLogger.info("This bundler might take some while longer than expected.");
                buildLogger.info("For details about this, please go to: https://wiki.debian.org/Teams/Dpkg/FAQ#Q:_Why_is_dpkg_so_slow_when_using_new_filesystems_such_as_btrfs_or_ext4.3F");
            }
        }
    }

    protected void doPrepareBeforeBundling(BaseSettings baseSettings, NativeInstallerSettings nativeInstallerSettings, String currentRunningBundlerID, Map<String, ? super Object> paramsToBundleWith, WorkaroundSwitches workaroundSwitches, BuildLogger buildLogger) {
        // copy all files every time a bundler runs, because they might cleanup their folders,
        // but user might have extend existing bundler using same foldername (which would end up deleted/cleaned up)
        // fixes "Make it possible to have additional resources for bundlers"
        // see https://github.com/FibreFoX/javafx-gradle-plugin/issues/38
        if( nativeInstallerSettings.getAdditionalBundlerResources() != null && nativeInstallerSettings.getAdditionalBundlerResources().exists() ){
            boolean skipCopyAdditionalBundlerResources = false;

            // keep previous behaviour
            Path additionalBundlerResourcesPath = nativeInstallerSettings.getAdditionalBundlerResources().toPath();
            Path resolvedBundlerFolder = additionalBundlerResourcesPath.resolve(currentRunningBundlerID);

            if( baseSettings.isVerbose() ){
                buildLogger.info("Additional bundler resources are specified, trying to copy all files into build root, using:" + nativeInstallerSettings.getAdditionalBundlerResources().getAbsolutePath());
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
                            sourceFolder = nativeInstallerSettings.getAdditionalBundlerResources().toPath();
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
        checkAndWarnAboutSlowPerformance(currentRunningBundlerID, nativeInstallerSettings, buildLogger);
    }

    protected void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }
}
