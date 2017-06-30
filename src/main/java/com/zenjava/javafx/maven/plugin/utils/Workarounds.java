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
package com.zenjava.javafx.maven.plugin.utils;

import java.io.File;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Danny Althoff
 */
public class Workarounds {

    protected Log logger;

    public Workarounds(File nativeOutputDir, Log logger) {
        this.logger = logger;
    }

    protected Log getLog() {
        return logger;
    }

    public boolean isWorkaroundForBug205Needed() {
        return (JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40)) || JavaDetectionTools.IS_JAVA_9;
    }

}
