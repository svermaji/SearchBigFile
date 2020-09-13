# SearchBigFile
Utility in Java to search big file (tested with nearly 400MB)

## Test Results<br>
 - File size `[1GB]`, type `[Text]`, lines processed `[13147035]` and pattern find `[794]` times, time taken `[167sec]`<br>
 - Attachine result with name as `Test 1GB`<br>

## Recent Changes<br>
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
