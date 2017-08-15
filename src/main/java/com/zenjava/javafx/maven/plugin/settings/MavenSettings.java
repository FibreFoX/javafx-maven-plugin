package com.zenjava.javafx.maven.plugin.settings;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 *
 * @author FibreFoX
 */
public class MavenSettings {

    /**
     * The Maven Project Object
     */
    @Parameter(name = "project", property = "project", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Used for attaching the generated bundles to the project.
     *
     */
    @Component
    protected MavenProjectHelper projectHelper;

    public MavenProject getProject() {
        return project;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

}
