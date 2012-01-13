-----------------------------------------------------------------------------------
Project: OpenGTS - Open GPS Tracking System
URL    : http://www.opengts.org
File   : README.txt
-----------------------------------------------------------------------------------

Notes:
1) Please refer to the included "OpenGTS_Config.pdf" file for all installation and 
   configuration information for the OpenGTS system.
2) When updating to a newer version of OpenGTS, it is highly recommended that the 
   following command be run to update the database with the latest table/column 
   changes:
     $  bin/dbAdmin.pl -tables=ca
   Or, run the following if alternate key fields also need to be updated:
     $  bin/dbAdmin.pl -tables=cak
3) On Windows 7, Internet Explorer 8, if you receive the error 
      Error [jsMapInit:[object error]
   then you may need to enable "Active Scripting" in the security tab.
4) A listing of the database tables and fields can be found in the file "SCHEMA.txt"
   in the OpenGTS installation diretory.
5) Documentation is now available online at the following link:
     http://www.opengts.org/documentation.html
6) This open-source version of OpenGTS includes support for the following GPS 
   tracking devices:
     - Aspicore GSM Tracker (Nokia, Samsung, Sony Ericsson phones).
       [http://www.aspicore.com]
     - Sanav GC-101, MT-101, and CT-24 Personal Tracker (HTTP-based protocol).
       Sanav GX-101 Vehicle Tracker (HTTP-based protocol).
       [http://www.sanav.com]
     - V-Sun 3338 Personal Tracker.
     - Android App "GPS2OpenGTS"
       [http://www.androidzoom.com/android_applications/communication/gps2opengts_nfsk_download.html]
     - iCare G3300 Personal Tracker.
     - Most TK102/TK103 devices.
     - Xexun TK102.
     - ZhongShan SIPGEAR Technology Co, Ltd.
     - TAIP (Trimble ASCII Interface Protocol).
     - TrackStick GPS data logger.
     - "GPSMapper" capable phones.
     - "NetGPS" capable devices.
     - HP hw6965 Windows/CE phone (OpenDMTP compliant)
     - Mologogo capable phones
     - Various Boost Mobile phones (OpenDMTP compliant)

Please let us know if you find any issues with this release.

-----------------------------------------------------------------------------------

Visit the following link for additional downloadable documentation and add-ons for 
OpenGTS:
  http://www.opengts.org/documentation.html
  http://www.geotelematic.com/documentation.html

-----------------------------------------------------------------------------------

This package may contain one or more of the following source modules which contain 
copyrights from their respective authors.  Please review these source modules for 
their copyright and license information:
   war/track/js/mapstraction/mapstraction.js
   war/track/js/mapstraction/mapstraction-geocode.js
   war/track/js/mapstraction/labeledmarker.js
   war/track/js/sorttable/sorttable.js

-----------------------------------------------------------------------------------

Contributors:
  See the following link for a list of contributors:
    http://www.opengts.org/info.html

-----------------------------------------------------------------------------------

Sample data:
Additional documentation for installing sample 'demo' data into the database can be
found in the "README.txt" file in the "sampleData" directory at
    sampleData/README.txt

-----------------------------------------------------------------------------------

Boost Mobile Motorola phones:
For information regarding support for various Boost Mobile Motorola phones, please
see the document at 
    MotoDMTP/MotoDMTP.txt

-----------------------------------------------------------------------------------

Sanav GC-101 GPS Tracking Device Support:
Additional documentation for installing and configuring the GC-101 server within
OpenGTS can be found in the "README.txt" file in the "gc101" source directory at
   src/org/opengts/war/gc101/README.txt

See the following link for manufacturer's product information:
   http://www.sanav.com

-----------------------------------------------------------------------------------

TAIP (Trimble ASCII Interface Protocol) Support:
Support for TAIP (Trimble ASCII Interface Protocol) is included in this release.  
This server uses the raw-socket mode device communication server based on the example
'template' server.  The "taip.jar" server jar file can be built with the command:
   > ant taip
And can be started in the same manner that other servers are started using the
"runserver.pl" command as follows:
   > $GTS_HOME/bin/runserver.pl -s taip

-----------------------------------------------------------------------------------

ICare G3300 Personal GPS Tracking Device Support:
Support for the ICare G3300 GPS tracking device is included in this release.  This
server uses the raw-socket mode device communication server based on the example
'template' server.  The "icare.jar" server jar file can be built with the command:
   > ant icare
And can be started in the same manner that other servers are started using the
"runserver.pl" command as follows:
   > $GTS_HOME/bin/runserver.pl -s icare

-----------------------------------------------------------------------------------

Updating existing Geozone table entries 
(only required when upgrading from versions previous to v1.9.3):

If you have existing Geozone table entries, they will need to be updated to include
Bounding-Box information used to optimize Geozone lookups.  This update process is
done automatically with the following steps:

1) Update the new Geozone bounding-box table fields:
     > bin/dbAdmin.pl -tables=ca

2) For each account having Geozone entries, run the following command (replace "<accountID>" 
   with the appropriate account-IDs for each account which has Geozone entries):
     > bin/admin.pl Geozone -account=<accountID> -list -update

The update process should now be complete.

-----------------------------------------------------------------------------------

Runtime config file property key references
(for *.conf files, such as 'default.conf', 'common.conf', 'webapp.conf, ...):
- Previously defined property keys may be referenced by placing them in ${...} brackets,
  such as ${log.name}.  Environment variables may also be referenced in this manner, as in 
  ${GTS_HOME}, or ${CATALINA_HOME}.
- A default value may be specified for referenced property keys which have not been defined.   
  For instance ${OUTPUT_DIR=/tmp} would resolve to "/tmp", if the property OUTPUT_DIR is not 
  defined (as an environment variable, or otherwise), or will resolve to the value specified 
  on the OUTPUT_DIR property (or environment variable) if it is defined.
- Recursive propery key references may also be specified.  For instance, the specification
  ${THIS_DIR=${THAT_DIR=/tmp}} will first attempt to resolve the value for "THIS_DIR", if not
  found, then value of THAT_DIR will be returned.  If THAT_DIR is not defined, then finally
  the specification will resolve to "/tmp".
- Property reference specifications for which the property is not defined, and there is no
  specified default value will resolve to the literal "${var}" string.  That is, if "var"
  is not defined, then ${var} will resolve to the literal string "${var}".
- Property reference specifications are only resolved at the time a property value is 
  requested, not at the time the property key=value line is parsed (ie. lazy resolution).  
  For instance, in the property specification below, "var_a" will resolve to "test", even 
  though "var_b" is not defined until after "var_a":
    var_a=${var_b}
    var_b=test
  This means that if "var_b" should ever change, the resolved value of "var_a" will also
  change accordingly.  [ie. "var_a" will only be resolved at the time a call to an RTConfig
  property value retrieval method is called - such as RTConfig.getString("var_a")].
  Write-only (assignable) property keys (ie. %log, %include) are the only exception to this 
  rule.  Since their assigned behavior is executed at the time they are parsed ('%log' prints
  a log message, and '%include' includes a file), write-only property keys must be assigned
  values that can be fully resolved at the time their values are assigned.
- Property keys may be re-assigned.  Property keys take on their last assigned value.  For
  instance, the following will display "Hello", then display "World":
    test=Hello
    %log=${test}
    test=World
    %log=${test}
    
The following are reserved read-only constant property keys:
   %version        returns the current version, as in "2.3.4"
   %contextName    returns the context 'name' (name of the servlet context, or main class name)
   %contextPath    returns the context 'path' (path to the servlet context, or $GTS_HOME)
   %configURL      returns the URL for the loaded config file
   %hostName       returns the current host-name
   %hostIP         returns the current mail host IP address

The following are reserved write-only (assignable) constant property keys:
   %log            displays the specified value to the log output
   %include        includes the config at the value URL (URL must exist, error otherwise)
   %include?       includes the config at the value URL (URL may exist, otherwise ignored)
   (The included URL protocol must be one of "file" or "http".)
   
The following are available within a PrivateLabel session context:
   session.name    The PrivateLabel Domain name
   session.locale  The PrivateLabel Domain locale

-----------------------------------------------------------------------------------

All trade names listed above are trademarks of their respective companies.
OpenGTS and GTS Enterprise are not affiliated with any of the listed companies.

-----------------------------------------------------------------------------------

Contact Info:
Please feel free to contact us regarding questions on this package.

Thanks,
Martin D. Flynn
devstaff@opengts.org
