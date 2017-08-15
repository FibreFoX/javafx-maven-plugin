package com.zenjava.javafx.maven.plugin;

import com.zenjava.javafx.maven.plugin.settings.BaseSettings;
import com.zenjava.javafx.maven.plugin.settings.MavenSettings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public abstract class AbstractJfxMojo extends AbstractMojo {

    @Parameter
    protected BaseSettings baseSettings = new BaseSettings();

    @Parameter
    protected MavenSettings mavenSettings = new MavenSettings();

    public BaseSettings getBaseSettings() {
        return baseSettings;
    }

    public void setBaseSettings(BaseSettings baseSettings) {
        this.baseSettings = baseSettings;
    }

    public MavenSettings getMavenSettings() {
        return mavenSettings;
    }

    public void setMavenSettings(MavenSettings mavenSettings) {
        this.mavenSettings = mavenSettings;
    }

}
