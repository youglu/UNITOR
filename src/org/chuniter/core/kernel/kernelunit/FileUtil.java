package org.chuniter.core.kernel.kernelunit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public abstract class FileUtil
{
	public static byte[] getBytesFromFile(File file) {
		try {
			InputStream fin = new FileInputStream(file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (fin.read(buffer) > -1) {
				out.write(buffer);
			}
			return out.toByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static byte[] getBytesFromFile(InputStream fin) {
		try { 
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (fin.read(buffer) > -1) {
				out.write(buffer);
			}
			return out.toByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

  public static InputStream getInputStreamFromBytes(byte[] b)
  {
    return new ByteArrayInputStream(b);
  }
}