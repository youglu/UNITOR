package org.chuniter.core.kernel.kernelunit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Stack;

public class HiGirl {
	/**
	 * 动态执行一段代码(生成文件->编译->执行)
	 * 
	 * @author kingfish
	 * @version 1.0
	 */

	private String fileName = "Test.java";
	private String className = "Test.class";

	public HiGirl() {
		String dir = System.getProperty("java.io.tmpdir")+"/";
		fileName=dir+fileName;
		className=dir+className;
		File f = new File(fileName);
		if (f.exists())
			f.delete();

		f = new File(className);
		if (f.exists())
			f.delete();
	}

	/**
	 * 创建java文件
	 */
	public void createJavaFile(String body) {
		String head = "public class Test{  public static void runCode(){";

		String end = "}}";
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileName));
			dos.writeBytes(head);
			dos.writeBytes(body);
			dos.writeBytes(end);
			dos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 编译
	 */
	public int makeJavaFile() {
		int ret = 0;
		try {
			Runtime rt = Runtime.getRuntime();
			Process ps = rt.exec(" javac " + fileName);
			ps.waitFor();
			byte[] out = new byte[1024];
			DataInputStream dos = new DataInputStream(ps.getInputStream());
			dos.read(out);
			String s = new String(out);
			if (s.indexOf("Exception") > 0) {
				ret = -1;
			}
		} catch (Exception e) {
			ret = -1;
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * 反射执行
	 */
	public void run() {
		try {
			Class.forName("Test").getMethod("runCode", new Class[] {})
					.invoke(null, new Object[] {});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 测试
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String cmd = "System.out.println(\"usage:Java TestRun int i=1; System.out.println(i+100);\");";
		if (args.length >= 1) {
			cmd = args[0];
		}
		cmd = "int i=1; System.out.println(i+100);";
		HiGirl t = new HiGirl();
		t.createJavaFile(cmd);
		if (t.makeJavaFile() == 0) {
			//Class cc = loadclassfromfile(t.className);
			t.run();
		}
	}
	
	private static final Class<?> loadclassfromfile(String cn) throws Exception{
		// 设置class文件所在根路径  
		// 例如/usr/java/classes下有一个test.App类，则/usr/java/classes即这个类的根路径，而.class文件的实际位置是/usr/java/classes/test/App.class  
		File clazzPath = new File(cn);  
		  
		// 记录加载.class文件的数量  
		int clazzCount = 0;  
		  
		if (clazzPath.exists() && clazzPath.isDirectory()) {  
		    // 获取路径长度  
		    int clazzPathLen = clazzPath.getAbsolutePath().length() + 1;  
		  
		    Stack<File> stack = new Stack<File>();  
		    stack.push(clazzPath);
		  
		    // 遍历类路径  
		    while (stack.isEmpty() == false) {  
		        File path = stack.pop();  
		        File[] classFiles = path.listFiles(new FileFilter() {  
		            public boolean accept(File pathname) {  
		                return pathname.isDirectory() || pathname.getName().endsWith(".class");  
		            }  
		        });  
		        for (File subFile : classFiles) {  
		            if (subFile.isDirectory()) {  
		                stack.push(subFile);  
		            } else {  
		                if (clazzCount++ == 0) {  
		                    Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);  
		                    boolean accessible = method.isAccessible();  
		                    try {  
		                        if (accessible == false) {  
		                            method.setAccessible(true);  
		                        }  
		                        // 设置类加载器  
		                        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();  
		                        // 将当前类路径加入到类加载器中  
		                        method.invoke(classLoader, clazzPath.toURI().toURL());  
		                    } finally {  
		                        method.setAccessible(accessible);  
		                    }  
		                }  
		                // 文件名称  
		                String className = subFile.getAbsolutePath();  
		                className = className.substring(clazzPathLen, className.length() - 6);  
		                className = className.replace(File.separatorChar, '.');  
		                // 加载Class类  
		                Class<?> c = Class.forName(className);  
		                System.out.println("读取应用程序类文件[class={}]"+className+"  "+c);
		                return c;
		            }  
		        }  
		    }  
		} 
		return null;
	}
}
