@echo off
rem #! /bin/bash
set HERE=%~dp0
set CP=%HERE%;%HERE%lib\commons-lang-2.6.jar;%HERE%lib\kuromoji-core-0.9.0.jar;%HERE%lib\kuromoji-ipadic-0.9.0.jar
java -cp %CP% %KDEF1% %KDEF2%  JapanMapTranslate %*

