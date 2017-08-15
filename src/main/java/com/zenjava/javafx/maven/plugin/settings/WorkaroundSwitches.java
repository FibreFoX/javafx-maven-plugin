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

    @Parameter
    protected boolean skipNativeLauncherWorkaround124;
    @Parameter
    protected boolean skipNativeLauncherWorkaround167;
    @Parameter
    protected boolean skipJNLPRessourcePathWorkaround182;
    @Parameter
    protected boolean skipSigningJarFilesJNLP185;
    @Parameter
    protected boolean skipSizeRecalculationForJNLP185;
    @Parameter
    protected boolean skipNativeLauncherWorkaround205;
    @Parameter
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
