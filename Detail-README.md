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
#### On 29-Sep-2021
* Update color and borders.
* Font/Colors will change only if window is active.

#### On 28-Sep-2021
* Now highlighted text color will also change.  Implementation changed from HighlightPainter to SimpleAttributeSet.
* Update pom version.

#### On 26-Sep-2021
* Application can be colored with highlight color.

#### On 24-Sep-2021
* Theme color comes on hover of tool buttons.

#### On 16-Sep-2021
* Drag n Drop file to set it in search text field.
* Use F3/Shift-F3 to search text.
* User can press Ctrl+F from anywhere to go to search box and hit enter to make Search.


#### On 13-Sep-2021
* Added old export cleaup button and display time to each action in information  

#### On 07-Sep-2021
* Added export button to transfer results into a timestamp based file  

#### On 21-Nov-2020
* Settings menu added 
* Now font and highlight color will be changed automatically if settings are ON  

#### On 20-Nov-2020
* Updated with better picture of colors for highlighting and selected text 

#### On 19-Nov-2020
* Different colors available for highlight and selected text.
* Sample can be see as tooltip 

#### On 16-Nov-2020
* Html highlighting removed and used Swing Highlighter class. 
* Now find functionality will also highlight new results. 

#### On 4-Nov-2020
* Added split pane for all occurrences. 

#### On 31-Oct-2020
* Added shortcuts to recent list of files/searches. 

#### On 29-Oct-2020
* Added all occurrences table to search occurrences quicker with highlighted text. 

#### On 26-Oct-2020
* Added all occurrences table to search occurrences quicker. 
* Added buttons to change cases in search box - Upper, Lower, Title and Invert case. 

#### On 21-Oct-2020
* Aa/W used for case-sensitive and word search.

#### On 17-Oct-2020
* After 'Read' operation is performed Search pattern box can be used to find any string in existing result. 

#### On 15-Oct-2020
* Help button text color changes to 6 different colors every 30sec.  

#### On 14-Oct-2020
* Tabs added for Help and Result. 
* TitledBorder removed. 

#### On 13-Oct-2020
* Help will be displayed at start of application. 
* Help button also added. 

#### On 12-Oct-2020
* Now auto complete feature is available in search file and search pattern text fields. 

#### On 3-Oct-2020
* Added controls to go on next or previous occurrences of search result. 
* Warning and Info indicator will change font every 10 min.  Font details will be in tooptip. 

#### On 30-Sep-2020
* Added controls to go on first or last line of results. 
* Open file dialog added with `...` 

#### On 29-Sep-2020
* In case of hang, program tries to stop forcefully after 50 sec. 
* The warning indicator is now revamped and made more prominent. 

#### On 26-Sep-2020
* ESC key to hide inner-frame for recent searches.
* Names are in separate files now.
* Configs are now in the file.
* UI and Core modules are used that has logic for logger, Utils and SwingUI.

#### On 25-Sep-2020
* Added indicator showing present font size of file contents pane.

#### On 24-Sep-2020
* Bug fixes, performance improvement, icon change.
* Now recent files/searches can be tagged for quick searching.
* Any recent files/searches starting with `*` will be added as favourite button `(max 5)`.

#### On 23-Sep-2020
* Controls to increase/decrease font size.
* A warning indicator is shown and show the occurrences so war in case if there are too many matches.
* Image attached as `app-image-warn-font.png`  

#### On 18-Sep-2020
* UI improvement.
* Iconified few buttons and added functionality to use filter on recent items.
* Image attached as `app-recent-filter.png`  

#### On 15-Sep-2020
* Greatly improved performance.
* Now added a new feature to read last 500 lines of a file along with the highlight of search pattern  
* Removed last 500 lines and give options till 5000 lines.  
* Changed labels and added tooltips to accommodate UI.  

#### On 14-Sep-2020
* Threading used to draw search results in chunks.
* This redraw/repaint of JEditorPane improved alot, 1GB of data searched now in 119 seconds !!  

#### On 14-Sep-2020
* Now combo boxes will be shown small initially but if list items are wider, then list will become wider to show it. 

#### On 13-Sep-2020
* Performance and UI improvement enhanced
* New callable to update statistics and append result
* Warning if search runs for too long or too many search occurrences 

#### On 11-Sep-2020
* Added the tooltip for combo box items

#### On 11-Sep-2020
* Added recent searched files and recent searched text
* Max limit for recent is 20

#### On 01-Sep-2020
Initial check in.

## Description
Program will store last search file location and search string. <br>
Match case and whole word search options are available. <br>
Only search matched result will be displayed. <br>
Matched string will be highlighted with yellow background. <br>
Line number will be displayed as bold at the start of line to match with an original file. <br>
Summary will be displayed in title bar with file size, time taken and number of occurrences matched. <br>
