# SearchBigFile
Utility in Java to search big file (tested with nearly 400MB and 1GB log file)

## Test Results with BufferedReader!!<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147027]` and pattern find `[1094]` times, time taken `[8sec]`<br>
 - Attaching result with the name as `With BufferedReader`<br>
 - Analysing some line difference due to line endings<br>

## Test Results IMPROVED!!<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147035]` and pattern find `[794]` times, time taken `[119sec]`<br>
 - Attaching result with the name as `Test 1GB New`<br>

## Test Results<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147035]` and pattern find `[794]` times, time taken `[167sec]`<br>
 - Attaching result with the name as `Test 1GB`<br>

## Recent Changes<br>
#### On 13-Oct-2020<br>
* Help will be displayed at start of application. 
* Help button also added. 

#### On 12-Oct-2020<br>
* Now auto complete feature is available in search file and search pattern text fields. 

#### On 3-Oct-2020<br>
* Added controls to go on next or previous occurrences of search result. 
* Warning and Info indicator will change font every 10 min.  Font details will be in tooptip. 

#### On 30-Sep-2020<br>
* Added controls to go on first or last line of results. 
* Open file dialog added with `...` 

#### On 29-Sep-2020<br>
* In case of hang, program tries to stop forcefully after 50 sec. 
* The warning indicator is now revamped and made more prominent. 

#### On 26-Sep-2020<br>
* ESC key to hide inner-frame for recent searches.
* Names are in separate files now.
* Configs are now in the file.
* UI and Core modules are used that has logic for logger, Utils and SwingUI.

#### On 25-Sep-2020<br>
* Added indicator showing present font size of file contents pane.

#### On 24-Sep-2020<br>
* Bug fixes, performance improvement, icon change.
* Now recent files/searches can be tagged for quick searching.
* Any recent files/searches starting with `*` will be added as favourite button `(max 5)`.

#### On 23-Sep-2020<br>
* Controls to increase/decrease font size.
* A warning indicator is shown and show the occurrences so war in case if there are too many matches.
* Image attached as `app-image-warn-font.png`  

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
