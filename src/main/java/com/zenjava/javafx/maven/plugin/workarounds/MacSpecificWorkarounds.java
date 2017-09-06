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
package com.zenjava.javafx.maven.plugin.workarounds;

import com.oracle.tools.packager.Bundler;
import com.zenjava.javafx.maven.plugin.utils.BuildLogger;
import com.zenjava.javafx.maven.plugin.utils.MacAppBundlerWithAdditionalResources;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 * @author FibreFoX
 */
public class MacSpecificWorkarounds {

    protected BuildLogger logger;

    public MacSpecificWorkarounds(File nativeOutputDir, BuildLogger logger) {
        this.logger = logger;
    }

    protected BuildLogger getLog() {
        return logger;
    }

    public boolean isWorkaroundForNativeMacBundlerNeeded(File additionalBundlerResources) {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("os x");
        boolean hasBundlerResources = additionalBundlerResources != null && additionalBundlerResources.isDirectory() && additionalBundlerResources.exists();

        return isMac && hasBundlerResources;
    }

    public Bundler applyWorkaroundForNativeMacBundler(final Bundler b, String currentRunningBundlerID, Map<String, Object> params, File additionalBundlerResources) {
        String workaroundForNativeMacBundlerDoneMarker = "WorkaroundForNativeMacBundler.done";
        if( "mac.app".equals(currentRunningBundlerID) && !params.containsKey(workaroundForNativeMacBundlerDoneMarker) ){
            // 1) replace current running bundler with our own implementation
            Bundler specialMacBundler = new MacAppBundlerWithAdditionalResources();

            // 2) replace other bundlers using mac.app-bundler inside
            getLog().info("Setting replacement of the 'mac.app'-bundler.");
            params.put("mac.app.bundler", specialMacBundler);
            params.put(workaroundForNativeMacBundlerDoneMarker, true);
            Path specificFolder = additionalBundlerResources.toPath().resolve("mac.app");
            // check if there is a special folder, otherwise put all stuff here
            if( Files.exists(specificFolder) && Files.isDirectory(specificFolder) ){
                getLog().info("Using special 'mac.app' bundler-folder.");
                params.put(MacAppBundlerWithAdditionalResources.ADDITIONAL_BUNDLER_RESOURCES.getID(), specificFolder.toAbsolutePath().toFile());
            } else {
                params.put(MacAppBundlerWithAdditionalResources.ADDITIONAL_BUNDLER_RESOURCES.getID(), additionalBundlerResources);
            }

            return specialMacBundler;
        }
        return b;
    }

}
