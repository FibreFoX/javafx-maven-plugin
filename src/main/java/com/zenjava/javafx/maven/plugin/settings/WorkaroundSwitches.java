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
public class WorkaroundSwitches {

    /**
     * Since Java version 1.8.0 Update 40 the native launcher for linux was changed and includes a bug
     * while searching for the generated configfile. This results in wrong ouput like this:
     * <pre>
     * client-1.1 No main class specified
     * client-1.1 Failed to launch JVM
     * </pre>
     * <p>
     * Scenario (which would work on windows):
     * <p>
     * <ul>
     * <li>generated launcher: i-am.working.1.2.0-SNAPSHOT</li>
     * <li>launcher-algorithm extracts the "extension" (a concept not known in linux-space for executables) and now searches for i-am.working.1.2.cfg</li>
     * </ul>
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
     */
    @Parameter(property = "jfx.skipNativeLauncherWorkaround124", defaultValue = "false")
    protected boolean skipNativeLauncherWorkaround124;

    /**
     * Since Java version 1.8.0 Update 60 the native launcher configuration for windows was changed
     * and includes a bug: the file-format before was "property-file", now it's "INI-file" per default,
     * but the runtime-configuration isn't honored like in property-files.
     * This workaround enforces the property-file-format.
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
     */
    @Parameter(property = "jfx.skipNativeLauncherWorkaround167", defaultValue = "false")
    protected boolean skipNativeLauncherWorkaround167;

    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was presented and includes
     * a bug while generating relative file-references when building on windows.
     * <p>
     * Change this to "true" when you don't want this workaround.
     */
    @Parameter(property = "jfx.skipJNLPRessourcePathWorkaround182")
    protected boolean skipJNLPRessourcePathWorkaround182;

    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was introduced,
     * but lacks the ability to sign jar-files by passing some flag. We are signing the files in the
     * case of having "jnlp" as bundler. The MOJO with the goal "build-web" was deprecated in favor
     * of that new bundler (mostly because the old one does not fit the bundler-list strategy).
     * <p>
     * Change this to "true" when you don't want signing jar-files.
     */
    @Parameter(property = "jfx.skipSigningJarFilesJNLP185", defaultValue = "false")
    protected boolean skipSigningJarFilesJNLP185;

    /**
     * After signing is done, the sizes inside generated JNLP-files still point to unsigned jar-file sizes,
     * so we have to fix these sizes to be correct. This sizes-fix even lacks in the old web-MOJO.
     * <p>
     * Change this to "true" when you don't want to recalculate sizes of jar-files.
     */
    @Parameter(property = "jfx.skipSizeRecalculationForJNLP185", defaultValue = "false")
    protected boolean skipSizeRecalculationForJNLP185;

    /**
     * Same problem as workaround for bug 124 for native launchers, but this time regarding
     * created native installers, where the workaround didn't apply.
     * <p>
     * Change this to "true" when you don't want this workaround.
     * <p>
     * Requires skipNativeLauncherWorkaround124 to be false.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
     */
    @Parameter(property = "jfx.skipNativeLauncherWorkaround205", defaultValue = "false")
    protected boolean skipNativeLauncherWorkaround205;

    @Parameter(property = "jfx.skipMacBundlerWorkaround", defaultValue = "false")
    protected boolean skipMacBundlerWorkaround = false;

    public boolean isSkipNativeLauncherWorkaround124() {
        return skipNativeLauncherWorkaround124;
    }

    public void setSkipNativeLauncherWorkaround124(boolean skipNativeLauncherWorkaround124) {
        this.skipNativeLauncherWorkaround124 = skipNativeLauncherWorkaround124;
    }

    public boolean isSkipNativeLauncherWorkaround167() {
        return skipNativeLauncherWorkaround167;
    }

    public void setSkipNativeLauncherWorkaround167(boolean skipNativeLauncherWorkaround167) {
        this.skipNativeLauncherWorkaround167 = skipNativeLauncherWorkaround167;
    }

    public boolean isSkipJNLPRessourcePathWorkaround182() {
        return skipJNLPRessourcePathWorkaround182;
    }

    public void setSkipJNLPRessourcePathWorkaround182(boolean skipJNLPRessourcePathWorkaround182) {
        this.skipJNLPRessourcePathWorkaround182 = skipJNLPRessourcePathWorkaround182;
    }

    public boolean isSkipSigningJarFilesJNLP185() {
        return skipSigningJarFilesJNLP185;
    }

    public void setSkipSigningJarFilesJNLP185(boolean skipSigningJarFilesJNLP185) {
        this.skipSigningJarFilesJNLP185 = skipSigningJarFilesJNLP185;
    }

    public boolean isSkipSizeRecalculationForJNLP185() {
        return skipSizeRecalculationForJNLP185;
    }

    public void setSkipSizeRecalculationForJNLP185(boolean skipSizeRecalculationForJNLP185) {
        this.skipSizeRecalculationForJNLP185 = skipSizeRecalculationForJNLP185;
    }

    public boolean isSkipNativeLauncherWorkaround205() {
        return skipNativeLauncherWorkaround205;
    }

    public void setSkipNativeLauncherWorkaround205(boolean skipNativeLauncherWorkaround205) {
        this.skipNativeLauncherWorkaround205 = skipNativeLauncherWorkaround205;
    }

    public boolean isSkipMacBundlerWorkaround() {
        return skipMacBundlerWorkaround;
    }

    public void setSkipMacBundlerWorkaround(boolean skipMacBundlerWorkaround) {
        this.skipMacBundlerWorkaround = skipMacBundlerWorkaround;
    }

}
