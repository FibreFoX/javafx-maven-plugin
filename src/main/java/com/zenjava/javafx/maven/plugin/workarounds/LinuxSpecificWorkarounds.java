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
package com.zenjava.javafx.maven.plugin.workarounds;

import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.zenjava.javafx.maven.plugin.settings.dto.NativeLauncher;
import com.zenjava.javafx.maven.plugin.utils.BuildLogger;
import com.zenjava.javafx.maven.plugin.utils.JavaTools;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author FibreFoX
 */
public class LinuxSpecificWorkarounds {

    public static final String CONFIG_FILE_EXTENSION = ".cfg";

    public static final String CFG_WORKAROUND_MARKER = "cfgWorkaroundMarker";
    public static final String CFG_WORKAROUND_DONE_MARKER = CFG_WORKAROUND_MARKER + ".done";

    protected BuildLogger logger;
    protected File nativeOutputDir;

    public LinuxSpecificWorkarounds(File nativeOutputDir, BuildLogger logger) {
        this.logger = logger;
        this.nativeOutputDir = nativeOutputDir;
    }

    protected BuildLogger getLog() {
        return logger;
    }

    public boolean isWorkaroundForCfgFileNameNeeded() {
        return JavaTools.IS_JAVA_8 && JavaTools.isAtLeastOracleJavaUpdateVersion(40) || JavaTools.IS_JAVA_9;
    }

    public void applyWorkaroundForCfgFileName(String appName, List<NativeLauncher> secondaryLaunchers) {
        // apply on main launcher
        applyNativeLauncherWorkaround(appName);

        // check on secondary launchers too
        if( secondaryLaunchers != null && !secondaryLaunchers.isEmpty() ){
            secondaryLaunchers.stream().map(launcher -> {
                return launcher.getAppName();
            }).filter(launcherAppName -> {
                // check appName containing any dots (which is the bug)
                return launcherAppName.contains(".");
            }).forEach(launcherAppname -> {
                applyNativeLauncherWorkaround(launcherAppname);
            });
        }
    }

    protected void applyNativeLauncherWorkaround(String appName) {
        // check appName containing any dots
        boolean needsWorkaround = appName.contains(".");
        if( !needsWorkaround ){
            return;
        }
        // rename .cfg-file (makes it able to create running applications again, even within installer)
        String newConfigFileName = appName.substring(0, appName.lastIndexOf("."));
        Path appPath = nativeOutputDir.toPath().resolve(appName).resolve("app");
        Path oldConfigFile = appPath.resolve(appName + CONFIG_FILE_EXTENSION);
        try{
            Files.move(oldConfigFile, appPath.resolve(newConfigFileName + CONFIG_FILE_EXTENSION), StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException ex){
            getLog().warn("Couldn't rename configfile. Please see issue #124 of the javafx-maven-plugin for further details.", ex);
        }
    }

    /**
     * Get generated, fixed cfg-files and push them to app-resources-list.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
     *
     * @param appName
     * @param secondaryLaunchers
     * @param params
     */
    public void applyWorkaroundForCfgFileNameInsideInstallers(String appName, List<NativeLauncher> secondaryLaunchers, Map<String, Object> params) {
        // to workaround, we are gathering the fixed versions of the previous executed "app-bundler"
        // and assume they all are existing
        Set<File> filenameFixedConfigFiles = new HashSet<>();

        // get cfg-file of main native launcher
        Path appPath = nativeOutputDir.toPath().resolve(appName).resolve("app").toAbsolutePath();
        if( appName.contains(".") ){
            String newConfigFileName = appName.substring(0, appName.lastIndexOf("."));
            File mainAppNameCfgFile = appPath.resolve(newConfigFileName + CONFIG_FILE_EXTENSION).toFile();
            if( mainAppNameCfgFile.exists() ){
                getLog().info("Found main native application configuration file (" + mainAppNameCfgFile.toString() + ").");
            }
            filenameFixedConfigFiles.add(mainAppNameCfgFile);
        }

        // when having secondary native launchers, we need their cfg-files too
        Optional.ofNullable(secondaryLaunchers).ifPresent(launchers -> {
            launchers.stream().map(launcher -> {
                return launcher.getAppName();
            }).forEach(secondaryLauncherAppName -> {
                if( secondaryLauncherAppName.contains(".") ){
                    String newSecondaryLauncherConfigFileName = secondaryLauncherAppName.substring(0, secondaryLauncherAppName.lastIndexOf("."));
                    filenameFixedConfigFiles.add(appPath.resolve(newSecondaryLauncherConfigFileName + CONFIG_FILE_EXTENSION).toFile());
                }
            });
        });

        if( filenameFixedConfigFiles.isEmpty() ){
            // it wasn't required to apply this workaround
            getLog().info("No workaround for native launcher issue 205 needed. Continuing.");
            return;
        }
        getLog().info("Applying workaround for native launcher issue 205 by modifying application resources.");

        // since 1.8.0_60 there exists some APP_RESOURCES_LIST, which contains multiple RelativeFileSet-instances
        // this is the more easy way ;)
        List<RelativeFileSet> appResourcesList = new ArrayList<>();
        RelativeFileSet appResources = StandardBundlerParam.APP_RESOURCES.fetchFrom(params);
        // original application resources
        appResourcesList.add(appResources);
        // additional filename-fixed cfg-files
        appResourcesList.add(new RelativeFileSet(appPath.toFile(), filenameFixedConfigFiles));

        // special workaround when having some jdk before update 60
        if( JavaTools.IS_JAVA_8 && !JavaTools.isAtLeastOracleJavaUpdateVersion(60) ){
            try{
                // pre-update60 did not contain any list of RelativeFileSets, which requires to rework APP_RESOURCES :/
                Path tempResourcesDirectory = Files.createTempDirectory("jfxmp-workaround205-").toAbsolutePath();
                File tempResourcesDirAsFile = tempResourcesDirectory.toFile();
                getLog().info("Modifying application resources for native launcher issue 205 by copying into temporary folder (" + tempResourcesDirAsFile.toString() + ").");
                for( RelativeFileSet sources : appResourcesList ){
                    File baseDir = sources.getBaseDirectory();
                    for( String fname : appResources.getIncludedFiles() ){
                        IOUtils.copyFile(new File(baseDir, fname), new File(tempResourcesDirAsFile, fname));
                    }
                }

                // might not work for gradle, but maven does not hold up any JVM ;)
                // might rework this later into cleanup-phase
                tempResourcesDirAsFile.deleteOnExit();

                // generate new RelativeFileSet with fixed cfg-file
                Set<File> fixedResourceFiles = new HashSet<>();
                try(Stream<Path> walkstream = Files.walk(tempResourcesDirectory)){
                    walkstream.
                            map(p -> p.toFile())
                            .filter(File::isFile)
                            .filter(File::canRead)
                            .forEach(f -> {
                                getLog().info(String.format("Add %s file to application resources.", f));
                                fixedResourceFiles.add(f);
                            });
                } catch(IOException ignored){
                    // NO-OP
                }
                params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(tempResourcesDirAsFile, fixedResourceFiles));
            } catch(IOException ex){
                getLog().warn(ex);
            }
            return;
        }
        /*
         * Backward-compatibility note:
         * When using JDK 1.8.0u51 on travis-ci it would results into "cannot find symbol: variable APP_RESOURCES_LIST"!
         *
         * To solve this, we are using some hard-coded map-key :/ (please no hacky workaround via reflections .. urgh)
         */
        params.put(StandardBundlerParam.APP_RESOURCES.getID() + "List", appResourcesList);
    }

    public boolean isWorkaroundForBug205Needed() {
        return (JavaTools.IS_JAVA_8 && JavaTools.isAtLeastOracleJavaUpdateVersion(40)) || JavaTools.IS_JAVA_9;
    }
}
