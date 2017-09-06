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
package com.zenjava.javafx.maven.plugin.settings;

import java.io.File;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author FibreFoX
 */
public class KeystoreSettings {

    /**
     * The location of the keystore. If not set, this will default to src/main/deploy/kesytore.jks which is usually fine
     * to use for most cases.
     */
    @Parameter(property = "jfx.keyStore", defaultValue = "src/main/deploy/keystore.jks")
    protected File keyStore;

    /**
     * The alias to use when accessing the keystore. This will default to "myalias".
     */
    @Parameter(property = "jfx.keyStoreAlias", defaultValue = "myalias")
    protected String keyStoreAlias;

    /**
     * The password to use when accessing the keystore. This will default to "password".
     */
    @Parameter(property = "jfx.keyStorePassword", defaultValue = "password")
    protected String keyStorePassword;

    /**
     * The password to use when accessing the key within the keystore. If not set, this will default to
     * keyStorePassword.
     */
    @Parameter(property = "jfx.keyPassword")
    protected String keyPassword;

    /**
     * The type of KeyStore being used. This defaults to "jks", which is the normal one.
     */
    @Parameter(property = "jfx.keyStoreType", defaultValue = "jks")
    protected String keyStoreType;

    public File getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(File keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

}
