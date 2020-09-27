# SearchBigFile
Utility in Java to search big file (tested with 400MB and 1GB+ log/text file)

> * For feature of favourite buttons in recent is in branch `with-fav-in-recent`
> * Branch `master` is targeted to remove favourite feature in recent searches

## Test Results!!<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147027]` and pattern find `[1094]` times, time taken `[8sec]`<br>
 - Attaching result with the name as `With BufferedReader`<br>

## Description<br>
* Program stores the last searched file and search string. 
* Controls to increase/decrease font size. 
* Shows number of lines processed till time while searching or reading. 
* A warning indicator is shown and show the occurrences so war in case if there are too many matches. 
* An indicator showing present font size of file contents pane.
* ESC key can be used to hide inner-frame.
* Match case and whole word search options are available. 
* Search button action will result only matched strings lines with highlighted background. 
* Recent files/searches can be tagged for quick searching with space at end in brackets `FILE (TAG)`.
* Any recent files/searches starting with `*` will be added as favourite button `(max 5)`, ex. `*FILE (TAG)`.
* Line number will be displayed as bold at the start of line to match with an original file. 
* Summary will be displayed in title bar with file size, time taken and number of occurrences matched.
* Another feature added to read last N (200-5000) lines of a file along with the highlight of search pattern  
* Recently searched files and search-patterns will be stored. 
* Additional filtering is provided to search among recent lists
* Application displays warning if search runs for too long or too many search occurrences 
* Tooltip provided for maximum controls.  
* Validations applied as and when needed.

#### Images of different stages of development/features<br>
Attaching screen shot of application:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image.png) 

Added on 11-Sep-2020. Attaching screen shot of application:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image-recent-controls.png) 

Added on 13-Sep-2020. Attaching screen shot of application. Test Result `Test 1GB` :<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-test-1gb.png) 

Added on 14-Sep-2020. Attaching screen shot of application. Wider combo box list:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image-wider-cb-list.png) 

Added on 15-Sep-2020. Attaching screen shot of application. Test Result `Test 1GB New` :<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-test-1gb-new.png) 

Added on 15-Sep-2020. Attaching screen shot of application. Read last N (500 to 5000) lines with highlight of pattern:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-N.png) 

Added on 18-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-recent-filter.png) 

Added on 18-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image-warn-font.png) 

Added on 24-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image-recent-favs.png) 

Added on 25-Sep-2020. Attaching screen shot of application. Recent items filter:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image-font-info.png) 

Added on 26-Sep-2020. Attaching screen shot of application. With BufferedReader:<br>
![Image of Yaktocat](https://github.com/svermaji/SearchBigFile/blob/master/app-image-test-result-BR.png) 
