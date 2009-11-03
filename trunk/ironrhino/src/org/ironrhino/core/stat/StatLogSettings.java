package org.ironrhino.core.stat;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ironrhino.core.util.AppInfo;

public class StatLogSettings {

	public static final String WRITETHREAD_NAME = "STATLOG-WRITE";

	public static final String ENCODING = "UTF-8";

	public static final String DATE_STYLE = "'.'yyyy-MM-dd";

	public static final String LAYOUT = "%m%n";

	public static final String TOKEN = "|";

	public static final String SEPARATOR = "_";

	public static final String FILE_DIRECTORY = "/logs/stat";

	public static final String STAT_LOG_FILE_NAME = "stat.log";

	public static final String SYSTEM_LOG_FILE_NAME = "system.log";

	private static final Log log = LogFactory.getLog(StatLogSettings.class);

	private static long intervalUnit = 60; // senconds

	private static long systemIntervalMultiple = 60; // system.log

	// SYSTEM_INTERVAL_MULTIPLE*INTERVAL_UNIT
	// senconds

	public static long getIntervalUnit() {
		return intervalUnit;
	}

	public static void setIntervalUnit(long intervalUnit) {
		if (intervalUnit > 0)
			StatLogSettings.intervalUnit = intervalUnit;
	}

	public static long getSystemIntervalMultiple() {
		return systemIntervalMultiple;
	}

	public static void setSystemIntervalMultiple(long systemIntervalMultiple) {
		if (systemIntervalMultiple > 0)
			StatLogSettings.systemIntervalMultiple = systemIntervalMultiple;
	}

	public static File getLogFileDirectory() {
		File dir = new File(AppInfo.getAppHome() + FILE_DIRECTORY);
		if (!dir.exists() && dir.mkdirs())
			log.error("mkdir error:" + dir.getAbsolutePath());
		return dir;
	}

	public static String getLogFile(String filename) {
		return new File(getLogFileDirectory(), AppInfo.getHostName()
				+ SEPARATOR + filename).getAbsolutePath();
	}

}
