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
		FileDirectoryPickerDialog directoryPickerDialog = FileDirectoryPickerDialog.newInstance(
				new FileDirectoryPickerDialog.Builder()
						.selectFiles(true)
						.selectFolders(false)
						.showEmptyFolders(false)
						.showAnimations(true)
						.customAnimation(R.style.DialogAnimation)
						.withResultListener((selectedFiles, selectedFolders) ->
						{
							Log.d("Debug", "Received " + selectedFiles.size() + " files and " + selectedFolders.size() + " folders");
						})
						.build());

		directoryPickerDialog.show(this.getSupportFragmentManager(), null);
	}
}
