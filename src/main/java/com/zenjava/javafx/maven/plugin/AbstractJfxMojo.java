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
package com.zenjava.javafx.maven.plugin;

import com.zenjava.javafx.maven.plugin.settings.BaseSettings;
import com.zenjava.javafx.maven.plugin.settings.BuildToolSettings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public abstract class AbstractJfxMojo extends AbstractMojo {

    /**
     * Set this to true for skipping the execution.
     */
    @Parameter(name = "skip", property = "jfx.skip", defaultValue = "false")
    protected boolean skip = false;

    @Parameter
    protected BaseSettings baseSettings = new BaseSettings();

    @Parameter
    protected BuildToolSettings mavenSettings = new BuildToolSettings();

    public BaseSettings getBaseSettings() {
        return baseSettings;
    }

    public void setBaseSettings(BaseSettings baseSettings) {
        this.baseSettings = baseSettings;
    }

    public BuildToolSettings getMavenSettings() {
        return mavenSettings;
    }

    public void setMavenSettings(BuildToolSettings mavenSettings) {
        this.mavenSettings = mavenSettings;
    }

}
