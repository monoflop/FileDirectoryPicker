package net.monoflop.filedirectorypicker;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

public interface PickerResultListener extends Parcelable
{
	void onPickerResult(@NonNull List<File> selectedFiles, @NonNull List<File> selectedFolders);

	default int describeContents() { return 0; }
	default void writeToParcel(Parcel dest, int flags) { }
}
