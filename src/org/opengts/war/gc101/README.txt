-----------------------------------------------------------------------------------
Project: OpenGTS - Open GPS Tracking System
URL    : http://www.opengts.org
File   : README.txt
-----------------------------------------------------------------------------------
Note:  This GC101 communication server may also support the following devices:
    - Sanav GX-101
    - Sanav MT-101
    - Sanav CT-24
    - V-SUN 3338 Personal Tracker
-----------------------------------------------------------------------------------

This document will discribe how to install/configure the "gc101.war" device parsing
module for the Sanav GC-101 personal GPS tracking device.  Please first read the
OpenGTS installation/configuration manual for examples on installing other servlets
and device parsing modules/servers.

You can find out more about this particular device from the Sanav web-site at:
   http://www.sanav.com/gps_tracking/GC-101.htm
   http://www.sanav.com/gps_tracking/GX-101.htm
   
This document assumes that Apache Tomcat and MySQL have been properly installed and 
configured, and that the rest of the OpenGTS project has been installed, configured,
and is functional.  Please refer to the OpenGTS installation/configuration document
for additional information regarding the installation and configuration of the 
required system services.

For the purpose of the document, we will assume that OpenGTS was installed at the
directory location "/usr/local/OpenGTS_1.2.3".  Please adjust this name accordingly 
to match your particular OpenGTS installation directory.

-----------------------------------------------------------------------------------
Building/Installing the "gc101.war" file:

To build the "gc101.war" file, 'cd' to the OpenGTS installation directory and compile
the gc101 server as follows:
   /zzz> cd /usr/local/OpenGTS_1.2.3
   /usr/local/OpenGTS_1.2.3> ant gc101
   
The target "gc101" is a wrapper for ant targets "gc101.compile" and "gc101.war".  
The target "gc101.compile" compiles all necessary classes and configuration files into 
the build directory "/usr/local/OpenGTS_1.2.3/build/gc101".  The target "gc101.war" 
then creates the 'web archive' file "/usr/local/OpenGTS_1.2.3/build/gc101.war".

If you wish, you can also make temporary changes to the "webapp.conf" file in the 
build directory "/usr/local/OpenGTS_1.2.3/build/gc101/WEB-INF/", then re-create the 
"gc101.war" file using the ant command "ant gc101.war".   Currently the only
configurable item specific to the GC101 module is the minimum acceptable speed.
Speeds below the set threshold (default is currently 4 kph), will be assumed to be
stopped, and the speed/heading values will be set to '0'.  This minimum spped
can be configured in the 'webapp.conf' file by adding a line similar to the following:
  # --- GC-101 minimum acceptable speed (in KPH)
  gc101.minimumSpeedKPH=10

Install the "gc101.war" file per the Apache Tomcat installation/configuration 
instructions.  Typically, this means copying the "gc101.war" file to the directory 
"$CATALINA_HOME/webapps/.".

In order for the GPS location information to reach the "gc101.war" servlet, it will
need to be installed on an Internet accessible server (please make sure you have
the proper Internet securities and firewalls in place).  For the purposes of this
document, we will assume that your Apache Tomcat server is accessible through the 
domain name and URL "http://www.example.com:8080/".  Then when the "gc101.war" file 
is installed, it will be accessible through the following URL:
   http://www.example.com:8080/gc101/Data

-----------------------------------------------------------------------------------
Creating a Device record for your GC-101 device.

First create an Account per the instructions found in the OpenGTS installation/
configuration document.  For purposes of thie example, we will assume that the
account name is "myaccount" and the device name will be "gc101".

You will also need to know the IMEI # of the GC-101 device, which you should be
able to obtain from the device itself.  For the purposes of this example, we will
assume that the IMEI # is "123423002212345".

To create the device record enter the following command:
   /usr/local/OpenGTS_1.2.3> bin/admin.pl Device -account=myaccount -device=gc101 -create

Edit the new record to set the UniqueID for the GC-101 device:
   /usr/local/OpenGTS_1.2.3> bin/admin.pl Device -account=myaccount -device=gc101 -edit

You will see a screen similar to the following:
    ...
    -----------------------------------------
    Key: myaccount,gc101
    -----------------------------------------
     0) Unique ID                           : ""
     1) Group ID                            : ""
    ...
    Enter field number [or 'save','exit']:
    
Enter "0", then hit return (to edit the "Unique ID" field):
    ....
    -----------------------------------------
    Field: uniqueID
    Title: Unique ID
    Type : class java.lang.String
    Value: 
    Enter new value:

Enter the value "gc101_123423002212345" (which is the prefix "gc101_" followed
by the IMEI# of the device), then press return:
    ...
    -----------------------------------------
    Key: myaccount,gc101
    -----------------------------------------
     0) Unique ID                           : "gc101_123423002212345"
     1) Group ID                            : ""
    ...
    Enter field number [or 'save','exit']:

Enter "save", then press return:
    ...
    Record saved
    ...
    /usr/local/OpenGTS_1.2.3> 

-----------------------------------------------------------------------------------
Configuring your GC-101 device.

The GC-101 device must be configured through SMS messages sent to the device.  Please
refer to your GC-101 documentation for instructions on how to perform these steps.

To have the GC-101 send GPS location information to your 'gc101.war' servlet,
configure the GC-101 to send data to the URL:
  http://www.example.com/gc101/Data
(Change "www.example.com" to your own server host name)

You can monitor the Apache Tomcat log files at "$CATALINA_HOME/logs/catalina.out", or
the log file at "$GTS_HOME/logs/w-gc101.log", to see the incoming connections from the 
GC-101 device.  If all has been configured properly, the GC-101 should send its GPS 
information on the scheduled periodic basis to your server, which will install the data 
into your EventData table (assuming that the database and "webapps.conf" have been 
properly configured and initialized.

-----------------------------------------------------------------------------------
