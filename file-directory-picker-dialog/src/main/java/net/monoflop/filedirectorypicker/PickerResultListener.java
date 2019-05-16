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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

/**
 * FileDirectoryPickerDialog result listener.
 *
 * @author Philipp Kutsch
 */
public interface PickerResultListener extends Parcelable
{
	void onPickerResult(@NonNull List<File> selectedFiles, @NonNull List<File> selectedFolders);

	default int describeContents() { return 0; }
	default void writeToParcel(Parcel dest, int flags) { }
}
