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
package com.zenjava.javafx.maven.plugin.settings;

import org.apache.maven.plugins.annotations.Parameter;

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
     */
    @Parameter(property = "jfx.feature.useEnvironmentRelativeExecutables", defaultValue = "true")
    protected boolean useEnvironmentRelativeExecutables = true;
    @Parameter
    protected boolean noBlobSigning;
    @Parameter
    protected boolean onlyCustomBundlers = false;
    @Parameter
    protected boolean skipNativeVersionNumberSanitizing = false;
    @Parameter
    protected boolean skipMainClassScanning = false;
    @Parameter
    protected boolean skipKeyStoreChecking = false;
    @Parameter
    protected boolean skipKeypassWhileSigning = false;

    public boolean isUseEnvironmentRelativeExecutables() {
        return useEnvironmentRelativeExecutables;
    }

    public void setUseEnvironmentRelativeExecutables(boolean useEnvironmentRelativeExecutables) {
        this.useEnvironmentRelativeExecutables = useEnvironmentRelativeExecutables;
    }

    public boolean isNoBlobSigning() {
        return noBlobSigning;
    }

    public void setNoBlobSigning(boolean noBlobSigning) {
        this.noBlobSigning = noBlobSigning;
    }

    public boolean isOnlyCustomBundlers() {
        return onlyCustomBundlers;
    }

    public void setOnlyCustomBundlers(boolean onlyCustomBundlers) {
        this.onlyCustomBundlers = onlyCustomBundlers;
    }

    public boolean isSkipNativeVersionNumberSanitizing() {
        return skipNativeVersionNumberSanitizing;
    }

    public void setSkipNativeVersionNumberSanitizing(boolean skipNativeVersionNumberSanitizing) {
        this.skipNativeVersionNumberSanitizing = skipNativeVersionNumberSanitizing;
    }

    public boolean isSkipMainClassScanning() {
        return skipMainClassScanning;
    }

    public void setSkipMainClassScanning(boolean skipMainClassScanning) {
        this.skipMainClassScanning = skipMainClassScanning;
    }

    public boolean isSkipKeyStoreChecking() {
        return skipKeyStoreChecking;
    }

    public void setSkipKeyStoreChecking(boolean skipKeyStoreChecking) {
        this.skipKeyStoreChecking = skipKeyStoreChecking;
    }

    public boolean isSkipKeypassWhileSigning() {
        return skipKeypassWhileSigning;
    }

    public void setSkipKeypassWhileSigning(boolean skipKeypassWhileSigning) {
        this.skipKeypassWhileSigning = skipKeypassWhileSigning;
    }

}
