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
import com.zenjava.javafx.maven.plugin.settings.FeatureSwitches;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.settings.KeystoreSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeAppSettings;
import com.zenjava.javafx.maven.plugin.settings.NativeInstallerSettings;
import com.zenjava.javafx.maven.plugin.settings.WorkaroundSwitches;
import com.zenjava.javafx.maven.plugin.utils.BuildLogger;
import com.zenjava.javafx.maven.plugin.workers.NativeAppWorker;
import com.zenjava.javafx.maven.plugin.workers.NativeInstallerWorker;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * EXPERIMENTAL
 */
@Mojo(name = "build-native-installer")
public class NativeInstallerMojo extends AbstractJfxMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( skip ){
            getLog().info("Skipping execution of NativeInstallerMojo");
            return;
        }
        // TODO do I need to call this?
        NativeAppWorker nativeAppWorker = new NativeAppWorker();
        nativeAppWorker.execute(baseSettings, new JfxJarSettings(), new NativeAppSettings(), new KeystoreSettings(), new FeatureSwitches(), new WorkaroundSwitches(), new BuildLogger() {
            @Override
            public void info(String message) {
                getLog().info(message);
            }
        });

        NativeInstallerWorker nativeInstallerWorker = new NativeInstallerWorker();
        nativeInstallerWorker.execute(baseSettings, new JfxJarSettings(), new NativeAppSettings(), new NativeInstallerSettings(), new KeystoreSettings(), new FeatureSwitches(), new WorkaroundSwitches(), new BuildLogger() {
            @Override
            public void info(String message) {
                getLog().info(message);
            }
        });
    }

}
