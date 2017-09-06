package com.zenjava.javafx.maven.plugin.utils;

/**
 * To have a uniform implementation agnostic to the used build-tool, this is a small
 * and stupid dummy-class for logging. Any method here has an empty implementation.
 *
 * @author FibreFoX
 */
public abstract class BuildLogger {

    public void info(String message) {
        // NO-OP
    }

    public void info(String message, Throwable exception) {
        // NO-OP
    }

    public void info(Throwable exception) {
        // NO-OP
    }

    public void warn(String message) {
        // NO-OP
    }

    public void warn(String message, Throwable exception) {
        // NO-OP
    }

    public void warn(Throwable exception) {
        // NO-OP
    }

    public void error(String message) {
        // NO-OP
    }

    public void error(String message, Throwable exception) {
        // NO-OP
    }

    public void error(Throwable exception) {
        // NO-OP
    }

    public void debug(String message) {
        // NO-OP
    }

    public void debug(String message, Throwable exception) {
        // NO-OP
    }

    public void debug(Throwable exception) {
        // NO-OP
    }
}
