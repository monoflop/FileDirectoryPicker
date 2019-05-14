package net.monoflop.filedirectorypicker;


import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import butterknife.OnClick;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

//@TODO recursively select all files and folders if you select a folder
//@TODO add start folder
//@TODO add ability to restrict picker inside a folder
@SuppressWarnings("WeakerAccess")
public class FileDirectoryPickerDialog extends DialogFragment implements EntryAdapter.EntrySelectedCallback
{
	public static boolean DEBUG = true;
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

	@BindView(R.id.cancelButton) Button cancelButton;
	@BindView(R.id.selectButton) Button selectButton;

	private final Comparator<Entry> defaultSortingComperator = (o1, o2) -> o1.getName().compareTo(o2.getName());

	private boolean requestPermission;

	private boolean selectFiles;
	private boolean selectFolders;

	private boolean singleFileMode;
	private boolean singleFolderMode;

	private boolean showHidden;

	//Appearance
	private String customTitle;
	private boolean showAnimation;

	private PickerResultListener pickerResultListener;
	private PickerErrorListener pickerErrorListener;

	private String[] fileEndingFilter;

	//Vars
	private File rootDirectory;
	private File currentRootDirectory;
	private List<Entry> entryList;
	private EntryAdapter entryAdapter;

	private List<File> selectedFiles;
	private List<File> selectedFolders;

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

		requestPermission = bundle.getBoolean("requestPermission");
		selectFiles = bundle.getBoolean("selectFiles");
		selectFolders = bundle.getBoolean("selectFolders");
		singleFileMode = bundle.getBoolean("singleFileMode");
		singleFolderMode = bundle.getBoolean("singleFolderMode");
		showHidden = bundle.getBoolean("showHidden");

		if(bundle.containsKey("customTitle"))
			customTitle = bundle.getString("customTitle");

		showAnimation = bundle.getBoolean("showAnimation");

		if(bundle.containsKey("pickerResultListener"))
			pickerResultListener = bundle.getParcelable("pickerResultListener");

		if(bundle.containsKey("pickerErrorListener"))
			pickerErrorListener = bundle.getParcelable("pickerErrorListener");

		if(bundle.containsKey("fileEndingFilter"))
			fileEndingFilter = bundle.getStringArray("fileEndingFilter");


		EntryAdapter.ViewMode viewMode;
		if(singleFileMode) viewMode = EntryAdapter.ViewMode.FilesOnly;
		else if(singleFolderMode) viewMode = EntryAdapter.ViewMode.FoldersOnly;
		else
		{
			if(selectFiles && selectFolders)
			{
				viewMode = EntryAdapter.ViewMode.Default;
			}
			else
			{
				if(selectFiles)viewMode = EntryAdapter.ViewMode.FilesOnly;
				else viewMode = EntryAdapter.ViewMode.FoldersOnly;
			}
		}

		selectedFiles = new ArrayList<>();
		selectedFolders = new ArrayList<>();
		entryList = new ArrayList<>();
		entryAdapter = new EntryAdapter(requireContext(), entryList, this, viewMode);
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

		if(singleFileMode) fileDirTitle.setText("Select one File");
		else if(singleFolderMode) fileDirTitle.setText("Select one Folder");
		else
		{
			if(selectFiles && selectFolders)
			{
				fileDirTitle.setText("Select File(s) or Folder(s)");
				selectButton.setEnabled(false);
			}
			else
			{
				if(selectFiles)fileDirTitle.setText("Select File(s)");
				else fileDirTitle.setText("Select Folder(s)");
				selectButton.setEnabled(false);
			}
		}

		if(customTitle != null)
			fileDirTitle.setText(customTitle);

		//Select root directory
		new Thread(this::loadInternalStorage).start();

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
								&& entryList.get(0).getFile() != null)
						{
							//Navigate back
							File newRootDir = entryList.get(0).getFile();
							if(newRootDir.exists() && newRootDir.isDirectory())
							{
								currentRootDirectory = newRootDir;
								requireActivity().runOnUiThread(() ->
								{
									fileDirPath.setText(newRootDir.getAbsolutePath());
									loadFolderStructure(newRootDir);
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

	@OnClick(R.id.cancelButton)
	public void cancelButtonOnClick()
	{
		getDialog().dismiss();
	}

	@OnClick(R.id.selectButton)
	public void selectButtonOnClick()
	{
		getDialog().dismiss();
		if(pickerResultListener != null)
			pickerResultListener.onPickerResult(selectedFiles, selectedFolders);
	}

	@Override
	public void onEntrySelected(Entry entry, boolean selected)
	{
		File selectedFile = entry.getFile();

		//Single file and single folder mode
		if(singleFileMode || singleFolderMode)
		{
			//Allow only one file or folder to select
			if(selected && (selectedFiles.size() + selectedFolders.size()) == 0)
			{
				if (selectedFile.isFile())
				{
					selectedFiles.add(selectedFile);
				} else if (selectedFile.isDirectory())
				{
					selectedFolders.add(selectedFile);
				}
			}
			//Un select
			else if(!selected)
			{
				if (selectedFile.isFile())
				{
					selectedFiles.remove(selectedFile);
				} else if (selectedFile.isDirectory())
				{
					selectedFolders.remove(selectedFile);
				}
			}

			//Update select button
			if(selectedFiles.size() + selectedFolders.size() == 1)
			{
				selectButton.setEnabled(true);
			}
			else
			{
				selectButton.setEnabled(false);
			}
		}
		else
		{
			//Select new entry
			if (selected)
			{
				if (selectedFile.isFile())
				{
					selectedFiles.add(selectedFile);
				} else if (selectedFile.isDirectory())
				{
					selectedFolders.add(selectedFile);
				}
			}
			//Un select entry
			else
			{
				if (selectedFile.isFile())
				{
					selectedFiles.remove(selectedFile);
				} else if (selectedFile.isDirectory())
				{
					selectedFolders.remove(selectedFile);
				}
			}

			//Update select button
			int selectedFilesAndFolders = selectedFiles.size() + selectedFolders.size();
			if (selectedFilesAndFolders == 0)
			{
				selectButton.setEnabled(false);
				selectButton.setText("Select");
			} else
			{
				selectButton.setEnabled(true);
				selectButton.setText("Select (" + selectedFilesAndFolders + ")");
			}
		}
	}

	@Override
	public void onFolderClicked(Entry entry)
	{
		//Select new root folder
		File newRootDir = entry.getFile();
		if(newRootDir.exists() && newRootDir.isDirectory())
		{
			currentRootDirectory = newRootDir;
			requireActivity().runOnUiThread(() ->
			{
				fileDirPath.setText(newRootDir.getAbsolutePath());
				loadFolderStructure(newRootDir);
				structureRecycler.scrollTo(0,0);
			});
		}
	}


	private void loadInternalStorage()
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
								loadFolderStructure(externalStorageRoot);
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
					loadFolderStructure(externalStorageRoot);
				});
			}
		}
	}

	private void loadFolderStructure(@NonNull File newRootDir)
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
					//Skip hidden files and folders
					if(!showHidden && subEntry.isHidden())continue;

					Entry entry = new Entry();
					entry.setName(subEntry.getName());

					if(subEntry.isDirectory())
					{
						//Skip empty directories
						//if(subEntry.list().length == 0)continue;

						entry.setEntryType(EntryType.Folder);
						entry.setInfo("Directory");
						//@TODO maybe check dir size async
						//entry.setInfo("Size: " + FileUtils.humanReadableByteCount(getResources().getConfiguration().locale, FileUtils.getFolderSize(subEntry), true));
					}

					if(subEntry.isFile())
					{
						//Ignore files if we are in folder mode
						//if(singleFolderMode || (!selectFiles && selectFolders))continue;

						//Filter out unwanted files
						if(fileEndingFilter != null)
						{
							boolean filterFile = true;
							for(String fileEnding : fileEndingFilter)
							{
								if(subEntry.getName().endsWith("." + fileEnding))
								{
									filterFile = false;
									break;
								}
							}

							if(filterFile)continue;
						}

						entry.setEntryType(EntryType.File);
						String fileInfo = FileUtils.humanReadableByteCount(getResources().getConfiguration().locale, subEntry.length(), true);
						fileInfo += " | " + DateUtils.formatDateTime(requireContext(), subEntry.lastModified(), FORMAT_NUMERIC_DATE | FORMAT_SHOW_YEAR)
								+ " " + DateUtils.formatDateTime(requireContext(), subEntry.lastModified(), FORMAT_SHOW_TIME);
						entry.setInfo(fileInfo);
					}

					//entry.setPath(subEntry.getAbsolutePath());
					entry.setFile(subEntry);

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
				entry.setName("Navigate back");
				entry.setInfo("...");
				entry.setFile(newRootDir.getParentFile());
				//entry.setPath(newRootDir.getParentFile().getAbsolutePath());
				entry.setEntryType(EntryType.None);
				entryList.add(entry);
			}

			entryList.addAll(newFolders);
			entryList.addAll(newFiles);

			//Reselect selected files and folders
			for(Entry entry : entryList)
			{
				File entryFile = entry.getFile();
				if(entryFile.isDirectory())
				{
					for(File selectedFolder : selectedFolders)
					{
						if(selectedFolder.equals(entryFile))
						{
							entry.setSelected(true);
						}
					}
				}
				else if(entryFile.isFile())
				{
					for(File selectedFile : selectedFiles)
					{
						if(selectedFile.equals(entryFile))
						{
							entry.setSelected(true);
						}
					}
				}
			}

			entryAdapter.notifyDataSetChanged();
		}
	}

	public static class Entry
	{
		private EntryType entryType;
		private String name;
		private String info;
		private boolean selected = false;

		private File file;

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

		public File getFile()
		{
			return file;
		}

		public void setFile(File file)
		{
			this.file = file;
		}

		public boolean isSelected()
		{
			return selected;
		}

		public void setSelected(boolean selected)
		{
			this.selected = selected;
		}
	}

	@SuppressWarnings("unused")
	public static class Builder
	{
		//Default values
		//Permission
		private boolean requestPermission = true;

		private boolean selectFiles = true;
		private boolean selectFolders = true;

		private boolean singleFileMode = false;
		private boolean singleFolderMode = false;

		private boolean showHidden = false;

		//Appearance
		private String customTitle;
		private boolean showAnimation = true;

		private String[] fileEndingFilter;

		private PickerResultListener pickerResultListener;
		private PickerErrorListener pickerErrorListener;

		public Builder(){}

		public Builder build()
		{
			return this;
		}

		public Builder requestPermission(boolean request)
		{
			this.requestPermission = request;
			return this;
		}

		public Builder selectFiles(boolean enabled)
		{
			this.selectFiles = enabled;
			return this;
		}

		public Builder selectFolders(boolean enabled)
		{
			this.selectFolders = enabled;
			return this;
		}

		public Builder singleFileMode(boolean enabled)
		{
			this.singleFileMode = enabled;
			return this;
		}

		public Builder singleFolderMode(boolean enabled)
		{
			this.singleFolderMode = enabled;
			return this;
		}

		public Builder showHidden(boolean show)
		{
			this.showHidden = show;
			return this;
		}

		public Builder withResultListener(@NonNull PickerResultListener pickerResultListener)
		{
			this.pickerResultListener = pickerResultListener;
			return this;
		}

		public Builder withErrorListener(@NonNull PickerErrorListener pickerErrorListener)
		{
			this.pickerErrorListener = pickerErrorListener;
			return this;
		}

		public Builder filterFileEndings(@NonNull String... fileEndingFilter)
		{
			this.fileEndingFilter = fileEndingFilter;
			return this;
		}

		public Builder customTitle(@NonNull String title)
		{
			this.customTitle = title;
			return this;
		}

		public Builder showAnimations(boolean show)
		{
			this.showAnimation = show;
			return this;
		}

		private Bundle toBundle()
		{
			Bundle bundle = new Bundle();

			bundle.putBoolean("requestPermission", requestPermission);

			bundle.putBoolean("selectFiles", selectFiles);
			bundle.putBoolean("selectFolders", selectFolders);

			bundle.putBoolean("singleFileMode", singleFileMode);
			bundle.putBoolean("singleFolderMode", singleFolderMode);

			bundle.putBoolean("showHidden", showHidden);

			if(customTitle != null)
				bundle.putString("customTitle", customTitle);

			bundle.putBoolean("showAnimation", showAnimation);

			if(pickerResultListener != null)
				bundle.putParcelable("pickerResultListener", pickerResultListener);

			if(pickerErrorListener != null)
				bundle.putParcelable("pickerErrorListener", pickerErrorListener);

			if(fileEndingFilter != null)
				bundle.putStringArray("fileEndingFilter", fileEndingFilter);

			return bundle;
		}
	}
}
