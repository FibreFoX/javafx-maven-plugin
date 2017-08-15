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
package com.zenjava.javafx.maven.plugin.mojo.lifecycle;

import com.oracle.tools.packager.Log;
import com.zenjava.javafx.maven.plugin.utils.JavaTools;
import com.sun.javafx.tools.packager.CreateJarParams;
import com.sun.javafx.tools.packager.PackagerLib;
import com.zenjava.javafx.maven.plugin.AbstractJfxMojo;
import com.zenjava.javafx.maven.plugin.settings.JfxJarSettings;
import com.zenjava.javafx.maven.plugin.utils.FileHelper;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "build-jar",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PACKAGE
)
public class JarMojo extends AbstractJfxMojo {

    @Parameter(name = "jfxJarSettings")
    protected JfxJarSettings jfxJarSettings = null;

    /**
     * Will be set when having goal "build-jar" within package-phase and calling "jfx:jar" or "jfx:native" from CLI. Internal usage only.
     */
    @Parameter(name = "jfxCallFromCLI", defaultValue = "false")
    protected boolean jfxCallFromCLI = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of JavaFX JAR for application");
            return;
        }

        if( baseSettings.isSkip() ){
            getLog().info("Skipping execution of JarMojo MOJO.");
            return;
        }

        getLog().info("Building JavaFX JAR for application");

        Build build = mavenSettings.getProject().getBuild();

        CreateJarParams createJarParams = new CreateJarParams();
        createJarParams.setOutdir(jfxJarSettings.getOutputFolderName());

        // check if we got some filename ending with ".jar" (found this while checking issue 128)
        if( !jfxJarSettings.getJfxMainAppJarName().toLowerCase().endsWith(".jar") ){
            getLog().error("Please provide a proper value for <jfxMainAppJarName>! It has to end with \".jar\".");
            return;
        }

        createJarParams.setOutfile(jfxJarSettings.getJfxMainAppJarName());
        createJarParams.setApplicationClass(jfxJarSettings.getMainClass());
        createJarParams.setCss2bin(jfxJarSettings.isCss2bin());
        createJarParams.setPreloader(jfxJarSettings.getPreLoader());

        if( jfxJarSettings.getManifestAttributes() == null ){
            jfxJarSettings.setManifestAttributes(new HashMap<>());
        }
        createJarParams.setManifestAttrs(jfxJarSettings.getManifestAttributes());

        StringBuilder classpath = new StringBuilder();
        File libDir = new File(jfxJarSettings.getOutputFolderName(), jfxJarSettings.getLibFolderName());
        if( !libDir.exists() && !libDir.mkdirs() ){
            throw new MojoExecutionException("Unable to create app lib dir: " + libDir);
        }

        if( jfxJarSettings.isUpdateExistingJar() ){
            File potentialExistingFile = new File(build.getDirectory() + File.separator + build.getFinalName() + ".jar");
            if( !potentialExistingFile.exists() ){
                throw new MojoExecutionException("Could not update existing jar-file, because it does not exist. Please make sure this file gets created or exists, or set updateExistingJar to false.");
            }
            createJarParams.addResource(null, potentialExistingFile);
        } else {
            File potentialExistingGeneratedClasses = new File(build.getOutputDirectory());
            // make sure folder exists, it is possible to have just some bootstraping "-jfx.jar"
            if( !potentialExistingGeneratedClasses.exists() ){
                getLog().warn("There were no classes build, this might be a problem of your project, if its not, just ignore this message. Continuing creating JavaFX JAR...");
                potentialExistingGeneratedClasses.mkdirs();
            }
            createJarParams.addResource(potentialExistingGeneratedClasses, "");
        }

        try{
            if( checkIfJavaIsHavingPackagerJar() ){
                getLog().debug("Check if packager.jar needs to be added");
                if( jfxJarSettings.isAddPackagerJar() && !jfxJarSettings.isSkipCopyingDependencies() ){
                    getLog().debug("Searching for packager.jar ...");
                    String targetPackagerJarPath = jfxJarSettings.getLibFolderName() + File.separator + "packager.jar";
                    for( Dependency dependency : mavenSettings.getProject().getDependencies() ){
                        // check only system-scoped
                        if( "system".equalsIgnoreCase(dependency.getScope()) ){
                            File packagerJarFile = new File(dependency.getSystemPath());
                            String packagerJarFilePathString = packagerJarFile.toPath().normalize().toString();
                            if( packagerJarFile.exists() && packagerJarFilePathString.endsWith(targetPackagerJarPath) ){
                                getLog().debug(String.format("Including packager.jar from system-scope: %s", packagerJarFilePathString));
                                File dest = new File(libDir, packagerJarFile.getName());
                                if( !dest.exists() ){
                                    Files.copy(packagerJarFile.toPath(), dest.toPath());
                                }
                                // this is for INSIDE the manifes-file, so always use "/"
                                classpath.append(jfxJarSettings.getLibFolderName()).append("/").append(packagerJarFile.getName()).append(" ");
                            }
                        }
                    }
                } else {
                    getLog().debug("No packager.jar will be added");
                }
            } else {
                if( jfxJarSettings.isAddPackagerJar() ){
                    getLog().warn("Skipped checking for packager.jar. Please install at least Java 1.8u40 for using this feature.");
                }
            }
            List<String> brokenArtifacts = new ArrayList<>();
            mavenSettings.getProject().getArtifacts().stream().filter(artifact -> {
                // filter all unreadable, non-file artifacts
                File artifactFile = artifact.getFile();
                return artifactFile.isFile() && artifactFile.canRead();
            }).filter(artifact -> {
                if( jfxJarSettings.getClasspathExcludes().isEmpty() ){
                    return true;
                }
                boolean isListedInList = isListedInExclusionList(artifact);
                return !isListedInList;
            }).forEach(artifact -> {
                File artifactFile = artifact.getFile();
                getLog().debug(String.format("Including classpath element: %s", artifactFile.getAbsolutePath()));
                File dest = new File(libDir, artifactFile.getName());
                if( !dest.exists() ){
                    try{
                        if( !jfxJarSettings.isSkipCopyingDependencies() ){
                            Files.copy(artifactFile.toPath(), dest.toPath());
                        } else {
                            getLog().info(String.format("Skipped copying classpath element: %s", artifactFile.getAbsolutePath()));
                        }
                    } catch(IOException ex){
                        getLog().warn(String.format("Couldn't read from file %s", artifactFile.getAbsolutePath()));
                        getLog().debug(ex);
                        brokenArtifacts.add(artifactFile.getAbsolutePath());
                    }
                }
                classpath.append(jfxJarSettings.getLibFolderName()).append("/").append(artifactFile.getName()).append(" ");
            });
            if( !brokenArtifacts.isEmpty() ){
                throw new MojoExecutionException("Error copying dependencies for application");
            }
        } catch(IOException e){
            throw new MojoExecutionException("Error copying dependency for application", e);
        }

        if( jfxJarSettings.isUseLibFolderContentForManifestClasspath() ){
            StringBuilder scannedClasspath = new StringBuilder();
            try{
                Files.walkFileTree(libDir.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        scannedClasspath.append(jfxJarSettings.getLibFolderName().replace("\\", "/")).append("/").append(libDir.toPath().relativize(file).toString().replace("\\", "/")).append(" ");
                        return super.visitFile(file, attrs);
                    }
                });
            } catch(IOException ioex){
                getLog().warn("Got problem while scanning lib-folder", ioex);
            }
            createJarParams.setClasspath(scannedClasspath.toString());
        } else {
            createJarParams.setClasspath(classpath.toString());
        }

        Optional.ofNullable(jfxJarSettings.getFixedManifestClasspath()).ifPresent(manifestClasspath -> {
            if( manifestClasspath.trim().isEmpty() ){
                return;
            }
            createJarParams.setClasspath(manifestClasspath);

            if( jfxJarSettings.isUseLibFolderContentForManifestClasspath() ){
                getLog().warn("You specified to use the content of the lib-folder AND specified a fixed classpath. The fixed classpath will get taken.");
            }
        });

        // https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/manifest.html#JSDPG896
        // http://docs.oracle.com/javase/8/docs/technotes/guides/javaws/developersguide/syntax.html#security
        if( jfxJarSettings.isAllPermissions() ){
            jfxJarSettings.getManifestAttributes().put("Permissions", "all-permissions");
        }

        try{
            JavaTools.addFolderToClassloader(baseSettings.getDeployDir());

            Log.setLogger(new Log.Logger(baseSettings.isVerbose()));

            new PackagerLib().packageAsJar(createJarParams);
        } catch(Exception e){
            throw new MojoExecutionException("Unable to build JFX JAR for application", e);
        }

        if( jfxJarSettings.isCopyAdditionalAppResourcesToJar() ){
            Optional.ofNullable(jfxJarSettings.getAdditionalAppResources())
                    .filter(File::exists)
                    .ifPresent(appResources -> {
                        getLog().info("Copying additional app ressources...");

                        try{
                            Path targetFolder = jfxJarSettings.getOutputFolderName().toPath();
                            Path sourceFolder = appResources.toPath();
                            new FileHelper().copyRecursive(sourceFolder, targetFolder, getLog());
                        } catch(IOException e){
                            getLog().warn("Couldn't copy additional application resource-file(s).", e);
                        }
                    });
        }

        // cleanup
        if( libDir.list().length == 0 ){
            // remove lib-folder, when nothing ended up there
            libDir.delete();
        }

        if( jfxJarSettings.isAttachAsZippedArtifact() ){
            // create ZIPPED version first
            File zipFileTarget = new File(build.getDirectory() + File.separator + build.getFinalName() + "-jfx-app.zip");
            try{
                new FileHelper().pack(jfxJarSettings.getOutputFolderName().toPath(), zipFileTarget.toPath());
                // attach to project artifacts
                mavenSettings.getProjectHelper().attachArtifact(mavenSettings.getProject(), "zip", "jfx-app", zipFileTarget);
            } catch(IOException ex){
                // TODO handle
                Logger.getLogger(JarMojo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected boolean checkIfJavaIsHavingPackagerJar() {
        if( JavaTools.IS_JAVA_8 && JavaTools.isAtLeastOracleJavaUpdateVersion(40) ){
            return true;
        }
        if( JavaTools.IS_JAVA_9 ){ // NOSONAR
            return true;
        }
        return false;
    }

    protected boolean isListedInExclusionList(Artifact artifact) {
        return jfxJarSettings.getClasspathExcludes().stream().filter(dependency -> {
            // we are checking for "groupID:artifactId:" because we don't care about versions nor types (jar, war, source, ...)
            String dependencyTrailIdentifier = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":";

            // when not transitive, look at the artifact information
            if( !jfxJarSettings.isClasspathExcludesTransient() ){
                return dependencyTrailIdentifier.startsWith(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":");
            }

            // when transitive, look at the trail
            return artifact.getDependencyTrail().stream().anyMatch((dependencyTrail) -> (dependencyTrail.startsWith(dependencyTrailIdentifier)));
        }).count() > 0;
    }
}
