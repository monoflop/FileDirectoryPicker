package net.monoflop.filedirectorypicker;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

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
						.build());

		directoryPickerDialog.show(this.getSupportFragmentManager(), null);
	}
}
