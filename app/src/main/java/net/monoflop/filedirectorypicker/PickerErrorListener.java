package net.monoflop.filedirectorypicker;

import android.os.Parcel;
import android.os.Parcelable;

public interface PickerErrorListener extends Parcelable
{
	void onPickerError(int error);

	default int describeContents() { return 0; }
	default void writeToParcel(Parcel dest, int flags) { }
}
