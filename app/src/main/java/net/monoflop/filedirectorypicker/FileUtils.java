package net.monoflop.filedirectorypicker;

import java.util.Locale;

final class FileUtils
{
	private FileUtils(){}

	static String humanReadableByteCount(Locale locale, long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format(locale,"%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
