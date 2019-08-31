package org.chuniter.core.kernel.kernelunit;

public class ClassUtil {

	public static <S> Class<S> fetchMethodFirstCallClass() throws ClassNotFoundException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stackTrace) {
            System.out.println(e.getClassName() + "\t" + e.getMethodName() + "\t" + e.getLineNumber());
        }
        StackTraceElement log = stackTrace[1];
        String tag = null;
        for (int i = 1; i < stackTrace.length; i++) {
            StackTraceElement e = stackTrace[i];
            if (!e.getClassName().equals(log.getClassName())) {
                tag = e.getClassName() + "." + e.getMethodName();
                break;
            }
        }
        if (tag == null) {
            tag = log.getClassName() + "." + log.getMethodName();

        }
        System.out.println(tag);
        return (Class<S>) Class.forName(log.getClassName());
    } 
}
