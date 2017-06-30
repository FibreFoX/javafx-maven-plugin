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

/**
 *
 * @author FibreFoX
 */
public class FeatureSwitches {

    protected boolean useEnvironmentRelativeExecutables;
    protected boolean noBlobSigning;
    protected boolean failOnError = false;
    protected boolean onlyCustomBundlers = false;
    protected boolean skipNativeVersionNumberSanitizing = false;
    protected boolean skipMainClassScanning = false;
    protected boolean skipKeyStoreChecking = false;
    protected boolean skipKeypassWhileSigning = false;
}
