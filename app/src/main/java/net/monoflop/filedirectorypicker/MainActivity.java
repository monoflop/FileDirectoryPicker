package net.monoflop.filedirectorypicker;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
{
	@BindView(R.id.dialogTestButton) Button dialogTestButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
	}

	@OnClick(R.id.dialogTestButton)
	public void dialogTestButtonOnClick()
	{
		FileDirectoryPickerDialog directoryPickerDialog
				= FileDirectoryPickerDialog.newInstance(
				new FileDirectoryPickerDialog.Builder()
						.selectFiles(true)
						.selectFolders(false)
						//.customTitle("Select opt key")
						//.filterFileEndings("otpkey")
						.withResultListener((selectedFiles, selectedFolders) ->
						{
							Log.d("Debug", "Received " + selectedFiles.size() + " files and " + selectedFolders.size() + " folders");
						})
						.build());

		directoryPickerDialog.show(this.getSupportFragmentManager(), null);
	}
}
