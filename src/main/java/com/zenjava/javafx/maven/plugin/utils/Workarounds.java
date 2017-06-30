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

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.zenjava.javafx.maven.plugin.NativeLauncher;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Danny Althoff
 */
public class Workarounds {

    private Log logger;
    private File nativeOutputDir;

    public Workarounds(File nativeOutputDir, Log logger) {
        this.logger = logger;
        this.nativeOutputDir = nativeOutputDir;
    }

    public Log getLog() {
        return logger;
    }

    public boolean isWorkaroundForBug185Needed(Map<String, Object> params) {
        return params.containsKey("jnlp.allPermisions") && Boolean.parseBoolean(String.valueOf(params.get("jnlp.allPermisions")));
    }

    public boolean isWorkaroundForBug205Needed() {
        return (JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40)) || JavaDetectionTools.IS_JAVA_9;
    }

}
