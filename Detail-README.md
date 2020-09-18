# SearchBigFile
Utility in Java to search big file (tested with nearly 400MB and 1GB log file)

## Test Results IMPROVED!!<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147035]` and pattern find `[794]` times, time taken `[119sec]`<br>
 - Attaching result with the name as `Test 1GB New`<br>

## Test Results<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147035]` and pattern find `[794]` times, time taken `[167sec]`<br>
 - Attaching result with the name as `Test 1GB`<br>

## Recent Changes<br>
#### On 18-Sep-2020<br>
* UI improvement.
* Iconified few buttons and added functionality to use filter on recent items.
* Image attached as `app-recent-filter.png`  

#### On 15-Sep-2020<br>
* Greatly improved performance.
* Now added a new feature to read last 500 lines of a file along with the highlight of search pattern  
* Removed last 500 lines and give options till 5000 lines.  
* Changed labels and added tooltips to accommodate UI.  

#### On 14-Sep-2020<br>
* Threading used to draw search results in chunks.
* This redraw/repaint of JEditorPane improved alot, 1GB of data searched now in 119 seconds !!  

#### On 14-Sep-2020<br>
* Now combo boxes will be shown small initially but if list items are wider, then list will become wider to show it. 

#### On 13-Sep-2020<br>
* Performance and UI improvement enhanced
* New callable to update statistics and append result
* Warning if search runs for too long or too many search occurrences 

#### On 11-Sep-2020<br>
* Added the tooltip for combo box items

#### On 11-Sep-2020<br>
* Added recent searched files and recent searched text
* Max limit for recent is 20

#### On 01-Sep-2020<br>
Initial check in.

## Description<br>
Program will store last search file location and search string. <br>
Match case and whole word search options are available. <br>
Only search matched result will be displayed. <br>
Matched string will be highlighted with yellow background. <br>
Line number will be displayed as bold at the start of line to match with an original file. <br>
Summary will be displayed in title bar with file size, time taken and number of occurrences matched. <br>

#### Images<br>
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
