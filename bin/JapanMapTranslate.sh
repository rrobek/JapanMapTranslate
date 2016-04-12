#! /bin/bash
HERE=`dirname $0`
CP=$HERE:$HERE/lib/commons-lang-2.6.jar:$HERE/lib/kuromoji-core-0.9.0.jar:$HERE/lib/kuromoji-ipadic-0.9.0.jar
java -cp $CP  JapanMapTranslate $*

