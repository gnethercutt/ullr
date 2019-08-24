package org.nethercutt.ullr.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigUtils {
    public static int getIntProperty(String propName, int defaultValue) {
        try {
            String value = System.getenv(propName);
            return (value != null) ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException nfe) {
            log.error("Invalid value {} for integer property {}", System.getenv(propName), propName);
            return defaultValue;
        }
    }

    public static String getStringProperty(String propName, String defaultValue) {
        try {
            String value = System.getenv(propName);
            return (value != null) ? value : defaultValue;
        } catch (NumberFormatException nfe) {
            log.error("Invalid value {} for integer property {}", System.getenv(propName), propName);
            return defaultValue;
        }
    }
}