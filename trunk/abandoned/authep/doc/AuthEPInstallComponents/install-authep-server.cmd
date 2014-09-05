FIND /C /I "authep.nl" %WINDIR%\system32\drivers\etc\hosts
IF %ERRORLEVEL% NEQ 0 ECHO ^127.0.0.1				authep.nl>>%WINDIR%\system32\drivers\etc\hosts
FIND /C /I "www.authep.nl" %WINDIR%\system32\drivers\etc\hosts
IF %ERRORLEVEL% NEQ 0 ECHO ^127.0.0.1				www.authep.nl>>%WINDIR%\system32\drivers\etc\hosts

certmgr.exe -add -c nl.cer -s -r currentUser Root

cscript adsutil.vbs CREATE W3SVC/1/Root/authep-idp "IIsWebVirtualDir"
cscript adsutil.vbs appcreateinproc W3SVC/1/Root/authep-idp
cscript adsutil.vbs set W3SVC/1/Root/authep-idp/path "C:\AuthEP\software\IDP\IDPWebsite"

cscript adsutil.vbs CREATE W3SVC/1/Root/authep-rp "IIsWebVirtualDir"
cscript adsutil.vbs appcreateinproc W3SVC/1/Root/authep-rp
cscript adsutil.vbs set W3SVC/1/Root/authep-rp/path "C:\AuthEP\software\RP\RPWebsite"

cscript adsutil.vbs CREATE W3SVC/1/Root/authep-rp2 "IIsWebVirtualDir"
cscript adsutil.vbs appcreateinproc W3SVC/1/Root/authep-rp2
cscript adsutil.vbs set W3SVC/1/Root/authep-rp2/path "C:\AuthEP\software\RP\RP2Website"

httpcfg set ssl -i 0.0.0.0:7001 -h "e395499c27164df4fabfd8f2cfd589234bafc3a3"
pause



