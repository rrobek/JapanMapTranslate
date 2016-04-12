@echo off
rem #! /bin/bash
set HERE=%~dp0
set CP=%HERE%;%HERE%kakasi.jar;%HERE%commons-lang-2.6.jar;%HERE%kuromoji-core-0.9.0.jar;%HERE%kuromoji-ipadic-0.9.0.jar
set KDEF1=-Dkakasi.home=%HERE%
set KDEF2=-Dkakasi.itaijiDictionary.path=%HERE%dict\itaijidict
java -cp %CP% %KDEF1% %KDEF2%  JapanMapTranslate %*

