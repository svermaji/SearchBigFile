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
 - File size `[1GB]`, type `[Text]`, lines processed `[13147027]` and pattern find `[1094]` times, time taken `[8sec]`<br>
 - Attaching result with the name as `With BufferedReader`<br>

## Description
* Program stores the last searched file and search string. 
* Added a button to change cases in search box - Upper, Lower, Title and Invert case. 
* Added all occurrences table to search occurrences quicker with highlighted text.  
    * No occurrences' message also present. 
    * All occurrences tab;e is inside split pane to resize it any time. 
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

#### Images of different stages of development/features
Attaching screen shot of application:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image.png) 

Added on 11-Sep-2020. Attaching screen shot of application:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-recent-controls.png) 

Added on 13-Sep-2020. Attaching screen shot of application. Test Result `Test 1GB` :<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-test-1gb.png) 

Added on 14-Sep-2020. Attaching screen shot of application. Wider combo box list:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-wider-cb-list.png) 

Added on 15-Sep-2020. Attaching screen shot of application. Test Result `Test 1GB New` :<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-test-1gb-new.png) 

Added on 15-Sep-2020. Attaching screen shot of application. Read last N (500 to 5000) lines with highlight of pattern:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-N.png) 

Added on 18-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-recent-filter.png) 

Added on 18-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-warn-font.png) 

Added on 24-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-recent-favs.png) 

Added on 25-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-font-info.png) 

Added on 26-Sep-2020. Attaching screen shot of application. With BufferedReader:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-test-result-BR.png) 

Added on 29-Sep-2020. Attaching screen shot of application. With new info, warning and error message:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-info-new.png) 
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-warn-new.png) 
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-error-new.png) 

Added on 30-Sep-2020. Attaching screen shot of application. Added controls goto first line and last line:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-goto.png) 

Added on 3-Oct-2020. Attaching screen shot of application. Added controls goto next and previous occurrences:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-occr.png) 

Added on 12-Oct-2020. Attaching screen shot of application. Auto complete in text fields:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-ac.png) 

Added on 13-Oct-2020. Attaching screen shot of application. Help:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-help.png) 

Added on 14-Oct-2020. Attaching screen shot of application. Tabs for help and result.  The recent dropdown is now menu:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-tabs-and-menu.png) 

Added on 17-Oct-2020. Attaching screen shot of application. Tabs for help and result.  Find from results:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-find.png) 

Added on 21-Oct-2020. Attaching screen shot of application. Aa/W used for case-sensitive and word search:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-bar.png) 

Added on 26-Oct-2020. Attaching screen shot of application. New button to change case in search box (upper/lower/title/invert):<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-case.png) 

Added on 27-Oct-2020. Attaching screen shot of application. Added all occurrences table to search occurrences quicker:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-all-occrs.png) 
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-no-occr.png) 

Added on 4-Nov-2020. Attaching screen shot of application. Added split pane for all occurrences:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-images/app-image-split-pane.png) 
