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
package com.zenjava.javafx.maven.plugin.mojo.cli;

import com.zenjava.javafx.maven.plugin.AbstractJfxToolsMojo;
import com.zenjava.javafx.maven.plugin.settings.RunSettings;
import com.zenjava.javafx.maven.plugin.workers.RunWorker;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @goal run
 * @execute goal="jar"
 */
public class RunMojo extends AbstractJfxToolsMojo {

    /**
     * This got moved to jfx.runSettings.javaParameter
     *
     * @deprecated
     * @parameter property="jfx.runJavaParameter"
     */
    @Deprecated
    protected String runJavaParameter = null;

    /**
     * This got moved to jfx.runSettings.appParameter
     *
     * @deprecated
     * @parameter property="jfx.runAppParameter"
     */
    @Deprecated
    protected String runAppParameter = null;

    /**
     * @parameter
     */
    protected RunSettings runSettings = null;

    protected RunWorker runWorker = new RunWorker();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( skip ){
            getLog().info("Skipping execution of RunMojo MOJO.");
            return;
        }

        // propagate old parameters to new settings object
        if( runSettings == null ){
            runSettings = new RunSettings();
            runSettings.setAppParameter(runAppParameter);
            runSettings.setJavaParameter(runJavaParameter);
        }

        runWorker.execute(runSettings);
    }
}
