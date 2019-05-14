package net.monoflop.filedirectorypicker;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused"})
final class FileUtils
{
	private FileUtils(){}

	static String humanReadableByteCount(@NonNull Locale locale, long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format(locale,"%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	static long getFolderSize(@NonNull File folder)
	{
		long size = 0;
		File[] files = folder.listFiles();

		for(File file : files)
		{
			if(file.isFile())
			{
				size += file.length();
			}
			else
			{
				size += getFolderSize(file);
			}
		}
		return size;
	}
}
