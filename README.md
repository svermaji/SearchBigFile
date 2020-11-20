# SearchBigFile
Utility in Java to search big file (tested with 400MB and 1GB+ log/text file)

> * For complete codebase in one refer branch `complete-code`
> * For the feature of favourite buttons and tagging in files and searches refer branch `with-fav-in-recent`
> * This branch `master` is targeted to have separate modules as UI and Core
> * Below modules are the dependencies for this project
>   - Core-Parent module `https://github.com/svermaji/Core-Parent`
>   - Core module `https://github.com/svermaji/Core`
>   - UI module `https://github.com/svermaji/SwingUI`

## Author Information
* **Name:** Shailendra Verma
* **Email:** shailendravermag@gmail.com
* **Blogs:** https://sv-technical.blogspot.com/

## Test Results!!
 - File size `[1GB]`, type `[Text]`, lines processed `[13147027]` and pattern find `[1094]` times, time taken `[8sec]`
 - Attaching result with the name as `With BufferedReader`

## Description
* Program stores the last searched file and search string. 
* Added a button to change cases in search box - Upper, Lower, Title and Invert case. 
* Added all occurrences table to search occurrences quicker with highlighted text.  
    * No occurrences' message also present. 
    * All occurrences table is inside split pane to resize it any time. 
    * Multiple colors available for match, selected and selected background. 
    * Color combination can be seen in the tooltip of each menuitem. 
* Tabs present for help and results. 
* Help button text color changes to 6 different colors every 30sec. 
* After 'Read' operation is performed Search pattern box can be used to find any string in existing result. 
* At start of program detail help will be displayed. 
* Warning and Info indicator will change font every 10 min.  Font details will be in tooptip. 
* In file path and search text box auto-complete feature is supported. 
* In case of hang, program tries to stop forcefully after 50 sec. 
* Controls to increase/decrease font size. 
* Shows number of lines processed till time while searching or reading. 
* Controls to next/previous occurrences. 
* A warning indicator is shown and show the occurrences so far in case if there are too many matches. 
* An indicator showing present font size of file contents pane.
* File can be chosen by file chooser.
* Controls to go first or last line.
* ESC key can be used to hide inner-frame.
* Match case and whole word search options are available. 
* Search button action will result only matched strings lines with highlighted background. 
* Line number will be displayed in blue at the start of line to match with an original file. 
* Summary will be displayed in title bar with file size, time taken and number of occurrences matched.
* Another feature added to read last N (100-5000) lines of a file along with the highlight of search pattern  
* Recently searched files and search-patterns will be stored with shortcuts. 
* Additional filtering is provided to search among recent lists
* Application displays warning if search runs for too long or too many search occurrences 
* Tooltip provided for maximum controls.  
* Validations applied as and when needed.

#### Images of different stages of development/features since day 1
* Application Present Image<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-highlight.png)

* First cut<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image.png)

* Test result on 1GB file with Buffered Reader<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-test-result-BR.png)

* Test result on 1GB file improved<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-test-1gb-new.png)

* Test result on 1GB old<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-test-1gb.png)

* Information indicator<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-info.png)

* Warning indicator<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-warn.png)

* Error indicator<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-error.png)

* Auto complete<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-ac.png)

* Menu bar<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-bar.png)

* Help Tab<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-help.png)

* If no occurrence found<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-no-occr.png)

* Recent Menu<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-recent.png)

* Filter recent values<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-recent-filter.png)

* Highlight/Selected Colors tooltip sample<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-highlight-tooltips.png)
