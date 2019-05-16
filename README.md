# FileDirectoryPicker

Simple file and directory picker library for Android.

* Select file(s)
* Select folder(s)
* Filter file extensions

## Usage

Add jitpack.io in your root build.gradle

```
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

Add the dependency

```
dependencies {
    implementation 'com.github.monoflop:FileDirectoryPicker:1.0.0'
}
```

### Builder

| function                    | description               | default value |
| :--------------------------:| :-----------:             | :-----------: |
| requestPermission | Request READ_EXTERNAL_STORAGE permission | true |
| selectFiles | Allow the user to select files | true |
| selectFolders | Allow the user to select folders | true |
| singleFileMode | 'Select one file' mode | false |
| singleFolderMode | 'Select one folder' mode | false |
| showHidden | Show hidden files and folders | false |
| showEmptyFolders | Show empty folders | false |
| withResultListener | Result listener | null |
| withErrorListener | Error listener | null |
| filterFileEndings | Show only files with matching extension | null |
| customTitle | Custom title | null |
| showAnimations | Show window animation | true |
| customAnimation | Customize window animation with style. Overwrite android:windowEnterAnimation and android:windowExitAnimation. | null |
| showDirectoryInfo | Show folder size and file count | false |


```java
FileDirectoryPickerDialog directoryPickerDialog = FileDirectoryPickerDialog.newInstance(
new FileDirectoryPickerDialog.Builder()
  .withResultListener((selectedFiles, selectedFolders) ->
  {
    //Do something
  })
  .build());

directoryPickerDialog.show(getSupportFragmentManager(), null);
```

License
-------

    Copyright 2019 Philipp Kutsch

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
