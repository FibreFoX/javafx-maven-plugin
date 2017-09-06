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

import com.zenjava.javafx.maven.plugin.AbstractJfxMojo;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeAppSettings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @deprecated will be dropped, please use "build-native-app" and "build-native-installer" goals
 */
@Mojo(name = "build-native")
@Deprecated
public class NativeMojo extends AbstractJfxMojo {

    @Parameter
    protected JfxJarSettings jfxJarSettings;

    @Parameter
    protected NativeAppSettings nativeAppSettings;

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
     * As it is possible to extend existing bundlers, you don't have to use your private
     * version of the javafx-maven-plugin. Just provide a list with the java-classes you
     * want to use, declare them as compile-depencendies and run `mvn jfx:native`
     * or by using maven lifecycle.
     * You have to implement the Bundler-interface (@see com.oracle.tools.packager.Bundler).
     */
    @Parameter(property = "jfx.customBundlers")
    protected List<String> customBundlers;


    /**
     * Per default his plugin does not break the build if any bundler is failing. If you want
     * to fail the build and not just print a warning, please set this to true.
     */
    @Parameter(property = "jfx.failOnError", defaultValue = "false")
    protected boolean failOnError = false;

    @Parameter(property = "jfx.onlyCustomBundlers", defaultValue = "false")
    protected boolean onlyCustomBundlers = false;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of Native Installers");
            return;
        }

        getLog().info("Building Native Installers");
        
        // TODO
        // call jfxJarBuilder, if requested ?
        // call build-native app (if not specified otherwise)
        // call build-native installer

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

        boolean runBundler = true;
        // Workaround for native installer bundle not creating working executable native launcher
        // (this is a comeback of issue 124)
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
        // do run application bundler and put the cfg-file to application resources
        /*
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
        * */
        return runBundler;
    }

    protected void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }
}
