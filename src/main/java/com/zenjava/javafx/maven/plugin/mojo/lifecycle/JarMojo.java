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
import com.zenjava.javafx.maven.plugin.workers.JfxJarWorker;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "build-jar",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PACKAGE
)
public class JarMojo extends AbstractJfxMojo {

    @Parameter(name = "jfxJarSettings")
    protected JfxJarSettings jfxJarSettings = null;

    /**
     * Will be set when having goal "build-jar" within package-phase and calling "jfx:jar" or "jfx:native" from CLI. Internal usage only.
     */
    @Parameter(name = "jfxCallFromCLI", defaultValue = "false")
    protected boolean jfxCallFromCLI = false;

    protected JfxJarWorker jfxJarWorker = new JfxJarWorker();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of JavaFX JAR for application");
            return;
        }
        jfxJarWorker.execute(baseSettings, mavenSettings, jfxJarSettings, (message) -> {
            getLog().info(message);
        }, (message) -> {
            getLog().warn(message);
        }, (message) -> {
            getLog().error(message);
        }, (message) -> {
            getLog().debug(message);
        });
    }

}
