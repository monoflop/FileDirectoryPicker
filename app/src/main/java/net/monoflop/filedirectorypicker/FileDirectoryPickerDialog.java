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

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
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

@SuppressWarnings({"WeakerAccess", "unused"})
public class FileDirectoryPickerDialog extends DialogFragment implements EntryAdapter.EntrySelectedCallback
{
	public static final int ERROR_PERMISSION_DENIED = 1;

	enum EntryType
	{
		None,
		File,
		Folder
	}

	@BindView(R2.id.fileDirFolderImage) ImageView fileDirFolderImage;
	@BindView(R2.id.fileDirTitle) TextView fileDirTitle;
	@BindView(R2.id.fileDirPath) TextView fileDirPath;

	@BindView(R2.id.structureRecycler) RecyclerView structureRecycler;

	@BindView(R2.id.cancelButton) Button cancelButton;
	@BindView(R2.id.selectButton) Button selectButton;

	private final Comparator<Entry> defaultSortingComparator = (o1, o2) -> o1.getName().compareTo(o2.getName());

	private boolean requestPermission;

	private boolean selectFiles;
	private boolean selectFolders;

	private boolean singleFileMode;
	private boolean singleFolderMode;

	private boolean showHidden;
	private boolean showEmptyFolders;

	//Appearance
	private String customTitle;
	private boolean showAnimation;
	private int animationStyle;
	private boolean showDirectoryInfo;

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

	private HandlerThread handlerThread;
	private Handler utilityHandler;

	public FileDirectoryPickerDialog(){}

	@NonNull
	public static FileDirectoryPickerDialog newInstance(@NonNull Builder builder)
	{
		FileDirectoryPickerDialog fileDirectoryPickerDialog = new FileDirectoryPickerDialog();
		fileDirectoryPickerDialog.setArguments(builder.toBundle());
		return fileDirectoryPickerDialog;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if(showAnimation)
		{
			if (getDialog() != null && getDialog().getWindow() != null)
				getDialog().getWindow().getAttributes().windowAnimations = animationStyle;
		}
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
		showEmptyFolders = bundle.getBoolean("showEmptyFolders");

		if(bundle.containsKey("customTitle"))
			customTitle = bundle.getString("customTitle");

		showAnimation = bundle.getBoolean("showAnimation");
		animationStyle = bundle.getInt("animationStyle");
		showDirectoryInfo = bundle.getBoolean("showDirectoryInfo");

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
		structureRecycler.setItemAnimator(null);

		if(singleFileMode) fileDirTitle.setText(getString(R.string.picker_title_single_file));
		else if(singleFolderMode) fileDirTitle.setText(getString(R.string.picker_title_single_folder));
		else
		{
			if(selectFiles && selectFolders)
			{
				fileDirTitle.setText(getString(R.string.picker_title_files_and_folders));
				selectButton.setEnabled(false);
			}
			else
			{
				if(selectFiles)fileDirTitle.setText(getString(R.string.picker_title_files));
				else fileDirTitle.setText(getString(R.string.picker_title_folders));
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

		//Crawl directories async and show file count and folder size
		if(showDirectoryInfo)
		{
			//Start handler thread
			handlerThread = new HandlerThread("UtilityHandlerThread");
			handlerThread.start();
			Looper looper = handlerThread.getLooper();
			utilityHandler = new Handler(looper);
		}

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

	@Override
	public void onPause()
	{
		super.onPause();

		if(showDirectoryInfo)
		{
			utilityHandler.removeCallbacksAndMessages(null);
			handlerThread.quit();
		}
	}

	@OnClick(R2.id.cancelButton)
	public void cancelButtonOnClick()
	{
		getDialog().dismiss();
	}

	@OnClick(R2.id.selectButton)
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
				selectButton.setText(getString(R.string.button_select));
			} else
			{
				selectButton.setEnabled(true);
				selectButton.setText(String.format(getString(R.string.picker_title_select_n), selectedFilesAndFolders));
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
			if(showDirectoryInfo)
				utilityHandler.removeCallbacksAndMessages(null);
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
							if(getDialog() != null)
							{
								getDialog().dismiss();
							}

							if(pickerErrorListener != null)
								pickerErrorListener.onPickerError(ERROR_PERMISSION_DENIED);
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
			else
			{
				if(getDialog() != null)
				{
					getDialog().dismiss();
				}

				if(pickerErrorListener != null)
					pickerErrorListener.onPickerError(ERROR_PERMISSION_DENIED);
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
						if(!showEmptyFolders && subEntry.list().length == 0)continue;

						entry.setEntryType(EntryType.Folder);

						if(subEntry.list().length == 0)
							entry.setInfo("Empty Directory");
						else
							entry.setInfo("Directory");

						if(showDirectoryInfo)
						{
							utilityHandler.post(() ->
							{
								FileUtils.FolderInfo folderInfo = FileUtils.getFolderInfo(subEntry);
								entry.setInfo("Directory "
										+ folderInfo.getFileCount()
										+ " File(s) | "
										+ FileUtils.humanReadableByteCount(getResources().getConfiguration().locale,
										folderInfo.getSize(),
										true));
								if (entryList.indexOf(entry) != -1)
								{
									if (getActivity() != null)
									{
										getActivity().runOnUiThread(() ->
												entryAdapter.notifyItemChanged(entryList.indexOf(entry)));
									}
								}
							});
						}
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

			Collections.sort(newFolders, defaultSortingComparator);
			Collections.sort(newFiles, defaultSortingComparator);

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
		//Behavior
		private boolean requestPermission = true;
		private boolean selectFiles = true;
		private boolean selectFolders = true;
		private boolean singleFileMode = false;
		private boolean singleFolderMode = false;
		private boolean showHidden = false;
		private boolean showEmptyFolders = false;
		private String[] fileEndingFilter;

		//Appearance
		private String customTitle;
		private boolean showAnimation = true;
		private int animationStyle = R.style.DialogAnimation;
		private boolean showDirectoryInfo = false;

		//@TODO implement styleable dialog
		private boolean customColors = false;
		private int headerBackground;
		private int headerTitle;
		private int headerPath;
		private int headerImage;
		private int background;
		private int entryImage;
		private int entryName;
		private int entryInfo;
		private int button;

		//Listeners
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

		public Builder showEmptyFolders(boolean show)
		{
			this.showEmptyFolders = show;
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

		public Builder customAnimation(@StyleRes int style)
		{
			this.animationStyle = style;
			return this;
		}

		public Builder showDirectoryInfo(boolean show)
		{
			this.showDirectoryInfo = show;
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
			bundle.putBoolean("showEmptyFolders", showEmptyFolders);

			if(customTitle != null)
				bundle.putString("customTitle", customTitle);

			bundle.putBoolean("showAnimation", showAnimation);
			bundle.putInt("animationStyle", animationStyle);
			bundle.putBoolean("showDirectoryInfo", showDirectoryInfo);

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
