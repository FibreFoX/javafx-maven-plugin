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

import com.zenjava.javafx.maven.plugin.utils.FileHelper;
import com.zenjava.javafx.maven.plugin.utils.JavaTools;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author FibreFoX
 */
public class GenericWorkarounds {

    protected static final String JNLP_JAR_PATTERN = "(.*)href=(\".*?\")(.*)size=(\".*?\")(.*)";
    protected FileHelper fileHelper = new FileHelper();

    protected Log logger;
    protected File nativeOutputDir;

    public GenericWorkarounds(File nativeOutputDir, Log logger) {
        this.logger = logger;
        this.nativeOutputDir = nativeOutputDir;
    }

    protected Log getLog() {
        return logger;
    }

    public boolean isWorkaroundForBug167Needed() {
        // this has been fixed and made available since 1.8.0u92:
        // http://www.oracle.com/technetwork/java/javase/2col/8u92-bugfixes-2949473.html
        return JavaTools.IS_JAVA_8 && JavaTools.isAtLeastOracleJavaUpdateVersion(60) && !JavaTools.isAtLeastOracleJavaUpdateVersion(92);
    }

    public boolean isWorkaroundForBug185Needed(Map<String, Object> params) {
        return params.containsKey("jnlp.allPermisions") && Boolean.parseBoolean(String.valueOf(params.get("jnlp.allPermisions")));
    }

    public List<File> getGeneratedJNLPFiles() {
        List<File> generatedFiles = new ArrayList<>();

        // try-ressource, because walking on files is lazy, resulting in file-handler left open otherwise
        try(Stream<Path> walkstream = Files.walk(nativeOutputDir.toPath())){
            walkstream.forEach(fileEntry -> {
                File possibleJNLPFile = fileEntry.toFile();
                String fileName = possibleJNLPFile.getName();
                if( fileName.endsWith(".jnlp") ){
                    generatedFiles.add(possibleJNLPFile);
                }
            });
        } catch(IOException ignored){
            // NO-OP
        }

        return generatedFiles;
    }

    public void fixPathsInsideJNLPFiles() {
        List<File> generatedJNLPFiles = getGeneratedJNLPFiles();
        Pattern pattern = Pattern.compile(JNLP_JAR_PATTERN);
        generatedJNLPFiles.forEach(file -> {
            try{
                List<String> allLines = Files.readAllLines(file.toPath());
                List<String> newLines = new ArrayList<>();
                allLines.stream().forEach(line -> {
                    if( line.matches(JNLP_JAR_PATTERN) ){
                        // get jar-file
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rawJarName = matcher.group(2);
                        // replace \ with /
                        newLines.add(line.replace(rawJarName, rawJarName.replaceAll("\\\\", "\\/")));
                    } else {
                        newLines.add(line);
                    }
                });
                Files.write(file.toPath(), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            } catch(IOException ignored){
                // NO-OP
            }
        });
    }

    public void fixFileSizesWithinGeneratedJNLPFiles() {
        // after signing, we have to adjust sizes, because they have changed (since they are modified with the signature)
        List<String> jarFiles = getJARFilesFromJNLPFiles();
        Map<String, Long> newFileSizes = fileHelper.getFileSizes(nativeOutputDir, jarFiles);
        List<File> generatedJNLPFiles = getGeneratedJNLPFiles();
        Pattern pattern = Pattern.compile(JNLP_JAR_PATTERN);
        generatedJNLPFiles.forEach(file -> {
            try{
                List<String> allLines = Files.readAllLines(file.toPath());
                List<String> newLines = new ArrayList<>();
                allLines.stream().forEach(line -> {
                    if( line.matches(JNLP_JAR_PATTERN) ){
                        // get jar-file
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rawJarName = matcher.group(2);
                        String jarName = rawJarName.substring(1, rawJarName.length() - 1);
                        if( newFileSizes.containsKey(jarName) ){
                            // replace old size with new one
                            newLines.add(line.replace(matcher.group(4), "\"" + newFileSizes.get(jarName) + "\""));
                        } else {
                            newLines.add(line);
                        }
                    } else {
                        newLines.add(line);
                    }
                });
                Files.write(file.toPath(), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            } catch(IOException ignored){
                // NO-OP
            }
        });
    }

    public List<String> getJARFilesFromJNLPFiles() {
        List<String> jarFiles = new ArrayList<>();
        getGeneratedJNLPFiles().stream().map(jnlpFile -> jnlpFile.toPath()).forEach(jnlpPath -> {
            try{
                List<String> allLines = Files.readAllLines(jnlpPath);
                allLines.stream().filter(line -> line.trim().startsWith("<jar href=")).forEach(line -> {
                    String jarFile = line.replaceAll(JNLP_JAR_PATTERN, "$2");
                    jarFiles.add(jarFile.substring(1, jarFile.length() - 1));
                });
            } catch(IOException ignored){
                // NO-OP
            }
        });
        return jarFiles;
    }

    public void applyWorkaround185(boolean skipSizeRecalculationForJNLP185) {
        if( !skipSizeRecalculationForJNLP185 ){
            getLog().info("Fixing sizes of JAR files within JNLP-files");
            fixFileSizesWithinGeneratedJNLPFiles();
        } else {
            getLog().info("Skipped fixing sizes of JAR files within JNLP-files");
        }
    }

    public void applyWorkaround167(Map<String, Object> params) {
        if( params.containsKey("runtime") ){
            getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding cfg-file-format");
            // the problem is com.oracle.tools.packager.windows.WinAppBundler within createLauncherForEntryPoint-Method
            // it does NOT respect runtime-setting while calling "writeCfgFile"-method of com.oracle.tools.packager.AbstractImageBundler
            // since newer java versions (they added possability to have INI-file-format of generated cfg-file, since 1.8.0_60).
            // Because we want to have backward-compatibility within java 8, we will use parameter-name as hardcoded string!
            // Our workaround: use prop-file-format
            params.put("launcher-cfg-format", "prop");
        }
    }

}
