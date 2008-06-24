package org.ironrhino.common.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionUtils {
	public static String getStackTraceAsString(Throwable t) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os, true, "UTF-8");
			t.printStackTrace(ps);
			ps.flush();
			ps.close();
			String s = os.toString("UTF-8");
			os.flush();
			os.close();
			return s;
		} catch (Exception e) {
			return t.getCause().toString();
		}
	}
}
