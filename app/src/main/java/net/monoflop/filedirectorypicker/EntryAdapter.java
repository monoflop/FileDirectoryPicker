package net.monoflop.filedirectorypicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder>
{
	private Context context;
	private List<FileDirectoryPickerDialog.Entry> entryList;
	private EntrySelectedCallback entrySelectedCallback;

	EntryAdapter(@NonNull Context context,
	             @NonNull List<FileDirectoryPickerDialog.Entry> entryList,
	             @NonNull EntrySelectedCallback entrySelectedCallback)
	{
		this.context = context;
		this.entryList = entryList;
		this.entrySelectedCallback = entrySelectedCallback;
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

		if(entry.getEntryType() == FileDirectoryPickerDialog.EntryType.Folder)
		{
			holder.entryImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_black_24dp));
			holder.entryCheckBox.setVisibility(View.VISIBLE);
		}
		else if(entry.getEntryType() == FileDirectoryPickerDialog.EntryType.File)
		{
			holder.entryImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
			holder.entryCheckBox.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.entryImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_black_24dp));
			holder.entryCheckBox.setVisibility(View.GONE);
		}

		holder.entryName.setText(entry.getName());
		holder.entryInfo.setText(entry.getInfo());

		holder.root.setOnClickListener((v) ->
		{
			if(entry.getEntryType() == FileDirectoryPickerDialog.EntryType.File)
			{
				holder.entryCheckBox.setChecked(!holder.entryCheckBox.isChecked());
				entrySelectedCallback.onEntrySelected(entry, holder.entryCheckBox.isChecked());
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
		@BindView(R.id.entryImage) ImageView entryImage;
		@BindView(R.id.entryName) TextView entryName;
		@BindView(R.id.entryInfo) TextView entryInfo;
		@BindView(R.id.entryCheckBox) CheckBox entryCheckBox;

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
