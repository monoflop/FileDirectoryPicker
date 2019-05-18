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

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Entry recyclerView adapter class.
 *
 * @author Philipp Kutsch
 */
class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder>
{
	enum ViewMode
	{
		Default, //Files and folders are shown and selectable
		FilesOnly, //Files and folders are shown but only files are selectable
		FoldersOnly //Only folders are shown and selectable
	}

	private Context context;
	private List<FileDirectoryPickerDialog.Entry> entryList;
	private EntrySelectedCallback entrySelectedCallback;
	private ViewMode viewMode;
	private FileDirectoryPickerDialog.CustomTheme customTheme;

	EntryAdapter(@NonNull Context context,
	             @NonNull List<FileDirectoryPickerDialog.Entry> entryList,
	             @NonNull EntrySelectedCallback entrySelectedCallback,
	             @Nullable ViewMode viewMode,
	             @Nullable FileDirectoryPickerDialog.CustomTheme customTheme)
	{
		this.context = context;
		this.entryList = entryList;
		this.entrySelectedCallback = entrySelectedCallback;

		this.viewMode = viewMode;
		if(this.viewMode == null)this.viewMode = ViewMode.Default;

		this.customTheme = customTheme;
	}

	@Override
	@NonNull
	public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		RelativeLayout layout;
		layout = (RelativeLayout) LayoutInflater.from(parent.getContext())
				.inflate(R.layout.element_entry, parent, false);

		return new EntryViewHolder(layout);
	}

	@Override
	public void onBindViewHolder(@NonNull EntryViewHolder holder, int position)
	{
		FileDirectoryPickerDialog.Entry entry = entryList.get(position);

		//Apply theme
		if(customTheme != null)
		{
			ImageViewCompat.setImageTintList(holder.entryImage, ColorStateList.valueOf(customTheme.getEntryImage()));
			holder.entryName.setTextColor(customTheme.getEntryName());
			holder.entryInfo.setTextColor(customTheme.getEntryInfo());
		}

		//Removes old listeners of recycled elements
		//Remove onCheckedChangeListener before setting the selected status.
		holder.entryCheckBox.setOnCheckedChangeListener(null);
		holder.entryCheckBox.setChecked(entry.isSelected());

		//Setup element based on the current view mode and entry type.
		if(entry.getEntryType() == FileDirectoryPickerDialog.Entry.EntryType.Folder)
		{
			holder.entryImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_black_24dp));
			if(viewMode == ViewMode.Default || viewMode == ViewMode.FoldersOnly)
				holder.entryCheckBox.setVisibility(View.VISIBLE);
			else
				holder.entryCheckBox.setVisibility(View.GONE);
		}
		else if(entry.getEntryType() == FileDirectoryPickerDialog.Entry.EntryType.File)
		{
			holder.entryImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));

			if(viewMode == ViewMode.Default || viewMode == ViewMode.FilesOnly)
				holder.entryCheckBox.setVisibility(View.VISIBLE);
			else
				holder.entryCheckBox.setVisibility(View.GONE);
		}
		else
		{
			holder.entryImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp));
			holder.entryCheckBox.setVisibility(View.GONE);
		}

		//Set name and info
		holder.entryName.setText(entry.getName());
		holder.entryInfo.setText(entry.getInfo());

		//Select entry if its a file otherwise navigate inside the folder.
		holder.root.setOnClickListener((v) ->
		{
			if(entry.getEntryType() == FileDirectoryPickerDialog.Entry.EntryType.File)
			{
				if(viewMode == ViewMode.Default || viewMode == ViewMode.FilesOnly)
				{
					holder.entryCheckBox.setChecked(!holder.entryCheckBox.isChecked());
				}
			}
			else
			{
				entrySelectedCallback.onFolderClicked(entry);
			}
		});

		holder.entryCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
				entrySelectedCallback.onEntrySelected(entry, isChecked));
	}

	@Override
	public int getItemCount()
	{
		return entryList.size();
	}

	static class EntryViewHolder extends RecyclerView.ViewHolder
	{
		@BindView(R2.id.entryImage) ImageView entryImage;
		@BindView(R2.id.entryName) TextView entryName;
		@BindView(R2.id.entryInfo) TextView entryInfo;
		@BindView(R2.id.entryCheckBox) CheckBox entryCheckBox;

		RelativeLayout root;

		private EntryViewHolder(RelativeLayout view)
		{
			super(view);
			root = view;
			ButterKnife.bind(this, view);
		}
	}

	public interface EntrySelectedCallback
	{
		void onEntrySelected(FileDirectoryPickerDialog.Entry entry, boolean selected);
		void onFolderClicked(FileDirectoryPickerDialog.Entry entry);
	}
}
