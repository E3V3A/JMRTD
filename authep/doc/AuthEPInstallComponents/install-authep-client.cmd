FIND /C /I "authep.nl" %WINDIR%\system32\drivers\etc\hosts
IF %ERRORLEVEL% NEQ 0 ECHO ^195.169.17.114				authep.nl>>%WINDIR%\system32\drivers\etc\hosts
FIND /C /I "www.authep.nl" %WINDIR%\system32\drivers\etc\hosts
IF %ERRORLEVEL% NEQ 0 ECHO ^195.169.17.114				www.authep.nl>>%WINDIR%\system32\drivers\etc\hosts
copy .java.policy "%USERPROFILE%"
certmgr.exe -add -all -c www.authep.nl.cer -s Root
certmgr.exe -add -all -c www.authep.nl.cer -s -r localMachine Root
pause



