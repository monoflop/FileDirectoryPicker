/*
 * Copyright 2019 Philipp Kutsch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.monoflop.filedirectorypicker;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused"})
final class FileUtils
{
	private FileUtils(){}

	/**
	 * Modified version of https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
	 * @param locale Respect local device settings in string.format
	 * @param bytes Size in bytes
	 * @param si Use si prefix (power of 2 or rounded values)
	 * @return Human readable byte count
	 */
	static String humanReadableByteCount(@NonNull Locale locale, long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format(locale,"%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Returns if the external storage is mounted.
	 *
	 * @return MEDIA_MOUNTED state of the external storage
	 */
	public static boolean isExternalStorageAvailable()
	{
		return android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
	}

	/**
	 * Recursive calculate the total size of all child files and folders.
	 *
	 * @param folder Root folder.
	 * @return Sum of all files and folders inside the root folder.
	 */
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


	/**
	 * Recursive calculate the total size and count of all child files and folders.
	 *
	 * @param folder Root folder.
	 * @return Size of the folder and number of child files and folders.
	 */
	static FolderInfo getFolderInfo(@NonNull File folder)
	{
		long size = 0;
		long fileCount = 0;

		File[] files = folder.listFiles();

		for(File file : files)
		{
			if(file.isFile())
			{
				size += file.length();
				fileCount++;
			}
			else
			{
				FolderInfo folderInfo = getFolderInfo(file);
				size += folderInfo.getSize();
				fileCount += folderInfo.getFileCount();
			}
		}

		return new FolderInfo(size, fileCount);
	}

	/**
	 * Class holds size and number of child files and folders.
	 */
	static class FolderInfo
	{
		private long size;
		private long fileCount;

		public FolderInfo(long size, long fileCount)
		{
			this.size = size;
			this.fileCount = fileCount;
		}

		public long getSize()
		{
			return size;
		}

		public void setSize(long size)
		{
			this.size = size;
		}

		public long getFileCount()
		{
			return fileCount;
		}

		public void setFileCount(long fileCount)
		{
			this.fileCount = fileCount;
		}
	}
}
