package org.chuniter.core.kernel.kernelunit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 根据OSGI运行文件config.ini提取BUNDLE jar文件。
 * 
 * @author youg
 * 
 */
public class CopyBundleJarsByIniFile {

	private String thisFilePath = "";

	public CopyBundleJarsByIniFile() {
		//URL classPathURL = this.getClass().getResource(
			//	"/" + this.getClass().getName().replace(".", "/") + ".class");
		//File file = new File(classPathURL.getFile());
		this.thisFilePath = "E:\\data\\bundle jars\\FIX\\";
		System.out.println(this.thisFilePath);
	}

	private void beginParse() throws IOException {
		File file = new File(
				"E:\\study\\java\\youg_project\\.metadata\\.plugins\\org.eclipse.pde.core\\FIXOSGI\\config.ini");
		Properties prop = new Properties();
		InputStream ins = null;
		try {
			ins = new FileInputStream(file);
			prop.load(ins);
			parseIniFileInfo(prop.get("osgi.bundles").toString());
			parseIniFileInfo(prop.get("osgi.framework").toString());
			saveBundleJarFileByPath(file.getAbsolutePath());
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			ins.close();
		}
	}

	private void parseIniFileInfo(String parseStr) throws IOException {
		try {
			String[] bundlePaths = parseStr.split(",");
			System.out.println(bundlePaths.length);
			for (int i = 0; i < bundlePaths.length; i++) {
				System.out.println(bundlePaths[i]);
				String bundleJarFilePath = parseBundlePath(bundlePaths[i]);
				if (!StringUtil.isValid(bundleJarFilePath))
					continue;
				saveBundleJarFileByPath(bundleJarFilePath);
			}
		} catch (IOException ioe) {
			throw ioe;
		}
	}

	private String parseBundlePath(String installBundlePath) {
		String[] oneBundleJarInfos = installBundlePath.split("file:");
		System.out.println(oneBundleJarInfos.length);
		for (int i = 0; i < oneBundleJarInfos.length; i++) {
			if (!StringUtil.isValid(oneBundleJarInfos[i])
					|| !(oneBundleJarInfos[i].contains(".jar")))
				continue;
			if(oneBundleJarInfos[i].indexOf("@") != -1)
				return (oneBundleJarInfos[i].substring(0,
						oneBundleJarInfos[i].indexOf("@")));
			return oneBundleJarInfos[i];
		}
		return "";
	}

	private void saveBundleJarFileByPath(String bundleJarPath)
			throws IOException {
		File bundleJarFile = new File(bundleJarPath);
		System.out.println(bundleJarFile.getName());
		File copyNewJarDir = new File("E:\\data\\bundle jars\\FIX\\");
		if (copyNewJarDir.exists()) {
			boolean ismakdir = copyNewJarDir.mkdirs();
			System.out.println(ismakdir);
		}
		File copyNewJarFile = new File("E:\\data\\bundle jars\\FIX\\"
				+ bundleJarFile.getName());

		BufferedInputStream bufferin = new BufferedInputStream(
				new FileInputStream(bundleJarFile));
		BufferedOutputStream bufferout = new BufferedOutputStream(
				new FileOutputStream(copyNewJarFile));

		try {
			byte[] bufferArray = new byte[1024 * 5];
			int len = 0;
			while ((len = bufferin.read(bufferArray)) != -1) {
				bufferout.write(bufferArray,0,len);
			}
			bufferout.flush();
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			bufferin.close();
			bufferout.close();
		}
	}

	public static void main(String[] args) throws IOException {
		CopyBundleJarsByIniFile inst = new CopyBundleJarsByIniFile();
		inst.beginParse();
		System.out.println("输出的Bundle jar文件在：" + inst.thisFilePath + " 中。");
	}
}
