$sourcePath = "F:\mtr-nte\build\MTR-ANTE-1.0.0+1.18.2.jar"
$destinationPath = "F:\mc\forge\mods"
$executablePath = "F:\mc\Plain Craft Launcher 2"

Copy-Item -Path "$sourcePath" -Destination "$destinationPath" -Recurse -Force
Start-Process -FilePath "$executablePath"
#.\run.ps1
#./gradlew build -PbuildVersion="1.18.2"
