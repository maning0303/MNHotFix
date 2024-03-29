package com.maning.hotfix.plugin;

import com.android.build.gradle.AppExtension;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * 生成补丁包
 */
public class PatchGenerator {

    private File patchFile;
    private File patchClassFile;
    private String buildToolsVersion;
    private Project project;
    private Map<String, String> oldHexs;
    private JarOutputStream jarOutputStream;

    public PatchGenerator(Project project, File outputDir, File hexFile) {
        this.project = project;
        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        buildToolsVersion = android.getBuildToolsVersion();
        // 需要打包补丁的类的jar包
        patchClassFile = new File(outputDir, "patchClass.jar");
        // 用dx打包后的jar包
        patchFile = new File(outputDir, "patch.jar");
        try {
            if (!this.patchFile.exists()) {
                this.patchFile.createNewFile();
            }
            if (!this.patchClassFile.exists()) {
                this.patchClassFile.createNewFile();
            }
        } catch (Exception e) {

        }
        if (hexFile.exists()) {
            oldHexs = Utils.readHex(hexFile);
        }
    }

    public void checkClass(String className, String hex, byte[] byteCode) {
        if (Utils.isEmpty(oldHexs)) {
            return;
        }
        String oldHex = oldHexs.get(className);
        System.out.println(oldHex + "=>" + hex);
        // 缓存不存在并且不相等，需要进入补丁包
        if (oldHex == null || !oldHex.equals(hex)) {
            JarOutputStream output = getOutput();
            try {
                output.putNextEntry(new JarEntry(className));
                output.write(byteCode);
                output.closeEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private JarOutputStream getOutput() {
        if (jarOutputStream == null) {
            try {
                jarOutputStream = new JarOutputStream(new FileOutputStream(patchClassFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jarOutputStream;
    }

    /**
     * 生成补丁包
     *
     * @throws Exception
     */
    public void generate() throws Exception {
        if (!patchClassFile.exists()) {
            return;
        }
        JarOutputStream output = getOutput();
        output.close();
        Properties properties = new Properties();
        File localProps = project.getRootProject().file("local.properties");
        //因为dx命令在 sdk中，获得sdk目录
        String sdkDir;
        if (localProps.exists()) {
            properties.load(new FileInputStream(localProps));
            sdkDir = properties.getProperty("sdk.dir");
        } else {
            sdkDir = System.getenv("ANDROID_HOME");
        }
        //windows使用 dx.bat命令,linux/mac使用 dx命令
        String cmd;
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            // 执行：dx --dex --output=output.jar input.jar
            String dxPath = sdkDir + "/build-tools/" + buildToolsVersion + "/dx.bat";
            String patch = "--output=" + patchFile.getAbsolutePath();
            cmd = dxPath + " --dex " + patch + " " + patchClassFile.getAbsolutePath();
        } else {
            // 执行：java -jar dx.jar --dex --output=output.jar input.jar
            String dxPath = sdkDir + "/build-tools/" + buildToolsVersion + "/lib/dx.jar";
            String patch = "--output=" + patchFile.getAbsolutePath();
            cmd = "java -jar " + dxPath + " --dex " + patch + " " + patchClassFile.getAbsolutePath();
        }
        System.out.println(cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        //命令执行失败
        if (process.exitValue() == 0) {
            project.getLogger().error("patch creat success : " + patchFile);
        } else {
            project.getLogger().error("no patch");
        }
    }
}
