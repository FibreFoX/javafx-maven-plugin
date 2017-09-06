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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Danny Althoff
 */
public class JavaTools {

    public static final boolean IS_JAVA_8 = isJavaVersion(8);
    public static final boolean IS_JAVA_9 = !IS_JAVA_8 && isJavaVersion(9) || isJavaVersion(9, true);

    public static final String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase();
    public static final boolean IS_WINDOWS = OPERATING_SYSTEM.contains("win");
    public static final boolean IS_LINUX = OPERATING_SYSTEM.contains("nix") || OPERATING_SYSTEM.contains("nux");
    public static final boolean IS_MAC = OPERATING_SYSTEM.contains("mac");

    public static boolean isJavaVersion(int oracleJavaVersion, boolean noVersionOne) {
        String javaVersion = System.getProperty("java.version");
        if( noVersionOne ){
            return javaVersion.startsWith(String.valueOf(oracleJavaVersion));
        }
        return javaVersion.startsWith("1." + oracleJavaVersion);
    }

    public static boolean isJavaVersion(int oracleJavaVersion) {
        return isJavaVersion(oracleJavaVersion, false);
    }

    public static boolean isAtLeastOracleJavaUpdateVersion(int updateNumber) {
        String javaVersion = System.getProperty("java.version");
        String[] javaVersionSplitted = javaVersion.split("_");
        if( javaVersionSplitted.length <= 1 ){
            return false;
        }
        String javaUpdateVersionRaw = javaVersionSplitted[1];
        // issue #159 NumberFormatException on openjdk (the reported Java version is "1.8.0_45-internal")
        String javaUpdateVersion = javaUpdateVersionRaw.replaceAll("[^\\d]", "");
        return Integer.parseInt(javaUpdateVersion, 10) >= updateNumber;
    }

    public static String getExecutablePath(boolean useEnvironmentRelativeExecutables) {
        if( useEnvironmentRelativeExecutables ){
            return "";
        }

        String jrePath = System.getProperty("java.home");
        if( IS_JAVA_9 ){
            return jrePath + File.separator + "bin" + File.separator;
        }
        return jrePath + File.separator + ".." + File.separator + "bin" + File.separator;
    }

    public static void addFolderToClassloader(String folderLocation) throws Exception {
        if( folderLocation == null || folderLocation.trim().isEmpty() ){
            return;
        }

        URLClassLoader sysloader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try{
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysloader, new File(folderLocation).toURI().toURL());
        } catch(NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new Exception("Error, could not add URL to system classloader", ex);
        }
    }
}
