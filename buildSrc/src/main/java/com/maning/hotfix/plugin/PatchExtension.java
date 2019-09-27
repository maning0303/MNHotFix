package com.maning.hotfix.plugin;

/**
 * 配置参数
 */
public class PatchExtension {

    boolean debugOn;
    String output;
    String applicationName;


    public PatchExtension() {
        debugOn = false;
    }

    public void setDebugOn(boolean debugOn) {
        this.debugOn = debugOn;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public String toString() {
        return "PatchExtension{" +
                "debugOn=" + debugOn +
                ", output='" + output + '\'' +
                ", applicationName='" + applicationName + '\'' +
                '}';
    }
}
