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
package com.zenjava.javafx.maven.plugin;

/**
 *
 * @author FibreFoX
 */
public class FeatureSwitches {

    /**
     * All commands executed by this Maven-plugin will be done using the current available commands
     * of your maven-execution environment. It is possible to call Maven with a different version of Java,
     * so these calls might be wrong. To use the executables of the JDK used for running this maven-plugin,
     * please set this to false. You might need this in the case you installed multiple versions of Java.
     *
     * The default is to use environment relative executables.
     *
     * @parameter property="jfx.feature.useEnvironmentRelativeExecutables" default-value="true"
     */
    protected boolean useEnvironmentRelativeExecutables = true;
    protected boolean noBlobSigning;
    protected boolean failOnError = false;
    protected boolean onlyCustomBundlers = false;
    protected boolean skipNativeVersionNumberSanitizing = false;
    protected boolean skipMainClassScanning = false;
    protected boolean skipKeyStoreChecking = false;
    protected boolean skipKeypassWhileSigning = false;
}
