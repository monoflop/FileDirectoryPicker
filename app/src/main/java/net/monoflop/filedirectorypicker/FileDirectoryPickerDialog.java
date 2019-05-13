package net.monoflop.filedirectorypicker;


import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

@SuppressWarnings("WeakerAccess")
public class FileDirectoryPickerDialog extends DialogFragment implements EntryAdapter.EntrySelectedCallback
{
	private static final String APP_TAG = "[FDP]";

	enum EntryType
	{
		None,
		File,
		Folder
	}

	@BindView(R.id.fileDirFolderImage) ImageView fileDirFolderImage;
	@BindView(R.id.fileDirTitle) TextView fileDirTitle;
	@BindView(R.id.fileDirPath) TextView fileDirPath;

	@BindView(R.id.structureRecycler) RecyclerView structureRecycler;

	private final Comparator<Entry> defaultSortingComperator = (o1, o2) -> o1.getName().compareTo(o2.getName());

	//Parameters
	private boolean enableExternalStorage;
	private boolean enableInternalStorage;
	private boolean preferExternalStorage;

	private boolean requestPermission;

	//Vars
	private File rootDirectory;
	private File currentRootDirectory;
	private List<Entry> entryList;
	private EntryAdapter entryAdapter;

	public FileDirectoryPickerDialog(){}

	@NonNull
	public static FileDirectoryPickerDialog newInstance(@NonNull Builder builder)
	{
		FileDirectoryPickerDialog fileDirectoryPickerDialog = new FileDirectoryPickerDialog();
		fileDirectoryPickerDialog.setArguments(builder.toBundle());
		return fileDirectoryPickerDialog;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//Read arguments from bundle:
		Bundle bundle = getArguments();
		if(bundle == null)
		{
			throw new IllegalArgumentException("FileDirectoryPickerDialog is missing a bundle");
		}

		enableExternalStorage = bundle.getBoolean("enableExternalStorage");
		enableInternalStorage = bundle.getBoolean("enableInternalStorage");
		preferExternalStorage = bundle.getBoolean("preferExternalStorage");

		requestPermission = bundle.getBoolean("requestPermission");

		entryList = new ArrayList<>();
		entryAdapter = new EntryAdapter(requireContext(), entryList, this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.dialog_filedir_picker, container, false);
		ButterKnife.bind(this, view);

		if(getDialog() != null && getDialog().getWindow() != null)
			getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		//Attach adapter and add item decoration
		structureRecycler.setAdapter(entryAdapter);
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
		structureRecycler.setLayoutManager(linearLayoutManager);
		DividerItemDecoration itemDecor = new DividerItemDecoration(requireContext(), RecyclerView.VERTICAL);
		structureRecycler.addItemDecoration(itemDecor);

		//Select root directory
		//Start with external storage
		if(preferExternalStorage)
		{

		}
		//Start with internal storage
		else
		{
			//@TODO
		}

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		getDialog().setOnKeyListener((dialog, keyCode, event) ->
		{
			if ((keyCode ==  KeyEvent.KEYCODE_BACK))
			{
				//This is the filter
				if (event.getAction() != KeyEvent.ACTION_DOWN)
					return true;
				else
				{
					if(!rootDirectory.equals(currentRootDirectory))
					{
						if(entryList.size() > 0
								&& entryList.get(0).getEntryType() == EntryType.None
								&& entryList.get(0).getPath() != null)
						{
							//Navigate back
							Log.d(APP_TAG, "Navigate back");
							File newRootDir = new File(entryList.get(0).getPath());
							File oldRootDir = currentRootDirectory;
							if(newRootDir.exists() && newRootDir.isDirectory())
							{
								currentRootDirectory = newRootDir;
								requireActivity().runOnUiThread(() ->
								{
									fileDirPath.setText(newRootDir.getAbsolutePath());
									loadFolderStructure(oldRootDir, newRootDir);
								});
							}
						}

						return true;
					}
					getDialog().dismiss();
					return false;
				}
			}
			else return false;
		});
	}

	@Override
	public void onEntrySelected(Entry entry, boolean selected)
	{
		//Add folder / file to list of selected entries.
		//@TODO
	}

	@Override
	public void onFolderClicked(Entry entry)
	{
		//Select new root folder
		File newRootDir = new File(entry.getPath());
		File oldRootDir = currentRootDirectory;
		if(newRootDir.exists() && newRootDir.isDirectory())
		{
			currentRootDirectory = newRootDir;
			requireActivity().runOnUiThread(() ->
			{
				fileDirPath.setText(newRootDir.getAbsolutePath());
				loadFolderStructure(oldRootDir, newRootDir);
				structureRecycler.scrollTo(0,0);
			});
		}
	}

	private void loadInternalStorage()
	{
		File internalStorageRoot = Environment.getExternalStorageDirectory();
		rootDirectory = internalStorageRoot;
		currentRootDirectory = internalStorageRoot;

		requireActivity().runOnUiThread(() ->
		{
			fileDirPath.setText(internalStorageRoot.getAbsolutePath());
			loadFolderStructure(internalStorageRoot, internalStorageRoot);
		});
	}

	private void loadExternalStorage()
	{
		if(requestPermission)
		{
			Dexter.withActivity(requireActivity())
					.withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					.withListener(new PermissionListener()
					{
						@Override
						public void onPermissionGranted(PermissionGrantedResponse response)
						{
							File externalStorageRoot = Environment.getExternalStorageDirectory();
							rootDirectory = externalStorageRoot;
							currentRootDirectory = externalStorageRoot;

							requireActivity().runOnUiThread(() ->
							{
								fileDirPath.setText(externalStorageRoot.getAbsolutePath());
								loadFolderStructure(externalStorageRoot, externalStorageRoot);
							});
						}

						@Override
						public void onPermissionDenied(PermissionDeniedResponse response)
						{
							Log.w(APP_TAG, "Permission denied.");
							if(getDialog() != null)
							{
								getDialog().dismiss();
							}
						}

						@Override
						public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token)
						{
							token.cancelPermissionRequest();
						}
					})
					.check();
		}
		else
		{
			if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PERMISSION_GRANTED)
			{
				File externalStorageRoot = Environment.getExternalStorageDirectory();
				rootDirectory = externalStorageRoot;
				currentRootDirectory = externalStorageRoot;

				requireActivity().runOnUiThread(() ->
				{
					fileDirPath.setText(externalStorageRoot.getAbsolutePath());
					loadFolderStructure(externalStorageRoot, externalStorageRoot);
				});
			}
		}
	}

	private void loadFolderStructure(@NonNull File oldRootDir, @NonNull File newRootDir)
	{
		if(newRootDir.canRead())
		{
			List<Entry> newFolders = new ArrayList<>();
			List<Entry> newFiles = new ArrayList<>();

			File[] subEntries = newRootDir.listFiles();
			for(File subEntry : subEntries)
			{
				if(subEntry.canRead())
				{
					Entry entry = new Entry();
					entry.setName(subEntry.getName());

					if(subEntry.isDirectory())
					{
						//Skip empty directories
						//if(subEntry.list().length == 0)continue;

						entry.setEntryType(EntryType.Folder);
						entry.setInfo("Directory");
					}

					if(subEntry.isFile())
					{
						entry.setEntryType(EntryType.File);
						entry.setInfo("Size: " + FileUtils.humanReadableByteCount(getResources().getConfiguration().locale, subEntry.length(), true));
					}

					entry.setPath(subEntry.getAbsolutePath());

					if(subEntry.isDirectory()) newFolders.add(entry);
					else if(subEntry.isFile()) newFiles.add(entry);
				}
			}

			Collections.sort(newFolders, defaultSortingComperator);
			Collections.sort(newFiles, defaultSortingComperator);

			entryList.clear();

			//Check if we are in the absolute root directory
			//else add the navigate back directory to the top
			if(!newRootDir.equals(rootDirectory))
			{
				Entry entry = new Entry();
				entry.setName("...");
				entry.setInfo("Go back");
				entry.setPath(oldRootDir.getAbsolutePath());
				entry.setEntryType(EntryType.None);
				entryList.add(entry);
			}

			entryList.addAll(newFolders);
			entryList.addAll(newFiles);
			entryAdapter.notifyDataSetChanged();
		}
	}

	public static class Entry
	{
		private EntryType entryType;
		private String name;
		private String info;

		private String path;

		public EntryType getEntryType()
		{
			return entryType;
		}

		public void setEntryType(@NonNull EntryType entryType)
		{
			this.entryType = entryType;
		}

		public String getName()
		{
			return name;
		}

		public void setName(@NonNull String name)
		{
			this.name = name;
		}

		public String getInfo()
		{
			return info;
		}

		public void setInfo(@NonNull String info)
		{
			this.info = info;
		}

		public String getPath()
		{
			return path;
		}

		public void setPath(@NonNull String path)
		{
			this.path = path;
		}
	}

	@SuppressWarnings("unused")
	public static class Builder
	{
		//Default values
		//Storage root
		private boolean enableExternalStorage = true;
		private boolean enableInternalStorage = true;
		private boolean preferExternalStorage = true;

		//Permission
		private boolean requestPermission = true;

		//
		private boolean selectFiles = true;
		private boolean selectFolders = true;

		public Builder(){}

		public Builder build()
		{
			if(!enableExternalStorage && !enableInternalStorage)
			{
				throw new IllegalArgumentException("You disabled all storage types.");
			}

			return this;
		}

		public Builder enableExternalStorage(boolean enable)
		{
			this.enableExternalStorage = enable;
			return this;
		}

		public Builder enableInternalStorage(boolean enable)
		{
			this.enableInternalStorage = enable;
			return this;
		}

		public Builder preferExternalStorage(boolean prefer)
		{
			this.preferExternalStorage = prefer;
			return this;
		}

		public Builder requestPermission(boolean request)
		{
			this.requestPermission = request;
			return this;
		}

		private Bundle toBundle()
		{
			Bundle bundle = new Bundle();
			bundle.putBoolean("enableExternalStorage", enableExternalStorage);
			bundle.putBoolean("enableInternalStorage", enableInternalStorage);
			bundle.putBoolean("preferExternalStorage", preferExternalStorage);

			bundle.putBoolean("requestPermission", requestPermission);
			return bundle;
		}
	}
}
