-----------------------------------------------------------------------------------
Project: OpenGTS - Open GPS Tracking System
URL    : http://www.opengts.org
File   : sampleData/README.txt
-----------------------------------------------------------------------------------

This directory contains the sample GPS tracking data that can be viewed on the demo
website: http://track.opengts.org/track/Track

After the OpenGTS database has been properly initialized, the sample data can be
loaded using the following steps:
1) cd to the OpenGTS installation directory.
2) Create the 'demo' account (without any password):
     > bin/admin.pl  Account -account=demo -nopass -create
   Or, on Windows:
     > bin\admin.bat Account -account:demo -nopass -create
3) Create the devices 'demo' and 'demo2':
     > bin/admin.pl  Device -account=demo -device=demo  -create
     > bin/admin.pl  Device -account=demo -device=demo2 -create
   Or, on Windows:
     > bin\admin.bat Device -account:demo -device:demo  -create
     > bin\admin.bat Device -account:demo -device:demo2 -create
4) Load the data for 'demo' and 'demo2':
     > bin/dbAdmin.pl   -load=EventData -dir=./sampleData
   Or, on Windows:
     > bin\dbConfig.bat -load:EventData -dir:./sampleData
5) Make sure that 'demo="true"' is set on the 'Domain' tag in the 'private.xml' file.
   Rebuild and redeploy the 'track.war' file if any changes were made to 'private.xml'.

Alternatively, the following Linux command may be executed to load the sample data
   bin/loadSampleData.sh

Any messages indicating that a column "will be dropped", can be ignored (these 
indicate optional columns that are not defined in the current EventData table).

At this point, if no errors occurred, you should be able to access the demo account
and view the 'demo' data through the OpenGTS web interface.

Note:
1) The date of all of the installed sample data occurs on March 12, 2010.  This 
   date for the "demo" account and "demo"/"demo2" devices is hardcoded in the class
   "org.opengts.db.tables.Account".  This will become the default date range
   when displaying these points on a map or in a report. 
2) If you wish to use the available sample data in an account other than "demo", you
   can use "sed" to replace the "demo" account name with another name with the 
   following example "sed" script:
     sed 's/^"demo",/"ACCOUNT",/g' < EventData.txt > EventData_ACCOUNT.txt
   Replace ACCOUNT with the specific account name you wish to have own the data.
   You will then need to move aside the existing "EventData.txt" file and rename
   "EventData_ACCOUNT.txt" to "EventData.txt" in order to use the above "-load"
   option to load the new sample data.  Make sure you first add the owning account
   and devices as described above, to prevent creating EventData records which are
   not owned by any account/device.

-----------------------------------------------------------------------------------
