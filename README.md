JapanMapTranslate Tool
===============================
JapanMapTranslate is a preprocessing tool for OpenStreetmap maps of Japan. It is particularly useful for OsmAnd.
It adds placenames (OSM tag `name:en`) for all roads or buildings that have a Japanese name (OSM tag name) but lack an English name.
These are transliterations (not actual translations) of the original names, i.e. the Japanese name
is written in Latin characters (or "romaji" as the Japanese would say).
By selecting English as language for the map description in OsmAnd, this enables average non-Japanese readers
to use the OpenStreetMaps of Japan. 
The following step-by-step guide shows how to create and use your own maps.  

Credits
------------------------------
Created by Florian Fischer (florianfischer@gmx.de)  
Based on Kuromoji, an open source Japanese morphological analyzer http://www.atilika.org/ and Kakasi, the Kanji-Kana Simple Inverter http://kakasi.namazu.org/  
Licensed under the GNU General Public License (GPL), see LICENSE.

Step 0
------------------------------
Download current OpenStreetMap data from Geofabrik (the `.osm.pbf` file for Japan):  
http://download.geofabrik.de/asia.html

Also, install a current Java runtime environment, if you haven't already:  
http://www.java.com/


Step 1
------------------------------
Convert the data to XML format, extracting a suitable area. 
(Unfortunately, OsmAnd cannot process a map that contains 
all of Japan.)  
Osmosis can be used for this task:   
http://wiki.openstreetmap.org/wiki/Osmosis

Some examples: 

    ..\OSMTools\osmosis-latest\bin\osmosis  --read-pbf file=japan-latest.osm.pbf  --bounding-box top=37.26 left=137.62 bottom=34.47 right=141.78 --write-xml file=japan-tokyo.osm

    ..\OSMTools\osmosis-latest\bin\osmosis  --read-pbf file=japan-latest.osm.pbf  --bounding-box top=37.61 left=135.02 bottom=33.41 right=137.62 --write-xml file=japan-osaka.osm

    ..\OSMTools\osmosis-latest\bin\osmosis  --read-pbf file=japan-latest.osm.pbf  --bounding-box top=35.82 left=130.808 bottom=33.81 right=135.02 --write-xml file=japan-hiroshima.osm

    ..\OSMTools\osmosis-latest\bin\osmosis  --read-pbf file=japan-latest.osm.pbf  --bounding-box top=33.966 left=129.34 bottom=30.94 right=132.122 --write-xml file=japan-kyushu.osm

    ..\OSMTools\osmosis-latest\bin\osmosis  --read-pbf file=japan-latest.osm.pbf  --bounding-box top=45.668 left=139.17 bottom=41.36 right=145.88 --write-xml file=japan-hokkaido.osm


Step 2
------------------------------
Annotate English names using JapanMapTranslate, example: 

    ..\OSMTools\JMTranslate\JapanMapTranslate japan-tokyo.osm

The result is an file named `japan-tokyo.osm.tr.osm` which contains Japanese *and* English 
place names. 


Step 3
------------------------------
Convert the data to the OsmAnd format. 
Use OsmAndMapCreator for this purpose.  
http://wiki.openstreetmap.org/wiki/OsmAndMapCreator

Start the program using OsmAndMapCreator.bat

Use 
*File -> Select Working directory... ->* select a drive with enough free space
*File -> Create OBF from OSM file...*

Select `japan-xxx.osm.tr.osm`

Press OK and wait, it typically takes hours.


Step 4
-------------------------------
Copy the resulting file to the Android device. 

For example, use a network-capable file manager, such as ES File Manager.

Put the map into the osmand data folder (you can check which it is under
*Settings -> General -> Misc -> Data* folder )

It seems that the map needs to be renamed once within OsmAnd's Map manager
(*Map management -> Local -> Select your file -> Rename*) 
before it is actually recognized by OsmAnd. 

Do not forget that you have to change the description language
(*Configure map -> Description language*) to English to see the transliterated place names. 

Also, as the base world map has not been created using JapanMapTranslate, you will 
need to zoom in a bit so that your local map is actually used. 
