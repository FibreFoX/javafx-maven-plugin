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

import com.zenjava.javafx.maven.plugin.settings.BaseSettings;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.settings.RunSettings;
import com.zenjava.javafx.maven.plugin.utils.JavaTools;
import com.zenjava.javafx.maven.plugin.utils.LoggerCall;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 * @author FibreFoX
 */
public class RunWorker {

    public void execute(BaseSettings baseSettings, JfxJarSettings jfxJarSettings, RunSettings runSettings, LoggerCall infoLogger) throws MojoExecutionException {
        infoLogger.log("Running JavaFX Application");

        List<String> command = new ArrayList<>();
        command.add(JavaTools.getExecutablePath(runSettings.isUseEnvironmentRelativeExecutables()) + "java");

        // might be useful for having a custom javassist or debugger integrated in this command
        Optional.ofNullable(runSettings.getJavaParameter()).ifPresent(parameter -> {
            if( !parameter.trim().isEmpty() ){
                command.add(parameter);
            }
        });

        command.add("-jar");
        command.add(jfxJarSettings.getJfxMainAppJarName());

        // it is possible to have jfx:run pass additional parameters
        // fixes https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/176
        Optional.ofNullable(runSettings.getAppParameter()).ifPresent(parameter -> {
            if( !parameter.trim().isEmpty() ){
                command.add(parameter);
            }
        });

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(jfxJarSettings.getOutputFolderName())
                    .command(command);

            if( baseSettings.isVerbose() ){
                infoLogger.log("Running command: " + String.join(" ", command));
            }

            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new MojoExecutionException("There was an exception while executing JavaFX Application. Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while executing JavaFX Application.", ex);
        }
    }

}
