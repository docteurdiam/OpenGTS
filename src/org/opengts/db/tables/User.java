// ----------------------------------------------------------------------------
// Copyright 2007-2011, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/03/30  Martin D. Flynn
//     -Moved to "org.opengts.db.tables"
//  2007/06/13  Martin D. Flynn
//     -Added BLANK_PASSWORD to explicitly support blank passwords
//  2007/06/14  Martin D. Flynn
//     -Fixed 'isAuthorizedDevice' to return true if no 'deviceGroup' has been specified.
//  2007/07/14  Martin D. Flynn
//     -Added "-nopass" & "-password" options to command-line administration.
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//  2007/11/28  Martin D. Flynn
//     -Added '-editall' command-line option to display all fields.
//  2008/07/21  Martin D. Flynn
//     -Fixed problem preventing device groups from being set properly.
//  2008/08/15  Martin D. Flynn
//     -Explicitly write DeviceGroup "all" to authorized device group list when 
//      DBConfig.DEFAULT_DEVICE_AUTHORIZATION is 'false'.
//     -Added static methods 'getAdminUserID()' and 'isAdminUser(...)'
//  2008/09/01  Martin D. Flynn
//     -Added 'FLD_firstLoginPageID'
//  2008/10/16  Martin D. Flynn
//     -Changed 'getUsersForContactEmail' to return a list of 'User' objects.
//     -Changed unspecified 'gender' text from "Unknown" to "n/a" (not applicable)
//     -Added fields 'FLD_preferredDeviceID', 'FLD_roleID'
//  2011/03/08  Martin D. Flynn
//     -Added FLD_notifyEmail
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;

public class User
    extends UserRecord<User>
    implements UserInformation
{

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String  OPTCOLS_AddressFieldInfo            = "startupInit.User.AddressFieldInfo";

    // ------------------------------------------------------------------------
    // Administrator user name

    public static final String  USER_ADMIN = "admin";

    /**
    *** Gets the defined "admin" user id
    *** @return The defined "admin" user id
    **/
    public static String getAdminUserID()
    {
        return USER_ADMIN;
    }

    /**
    *** Returns true if specified user is and "admin" user
    *** @param userID  The userID to test
    *** @return True if the specified is an "admin" user
    **/
    public static boolean isAdminUser(String userID)
    {
        if (StringTools.isBlank(userID)) {
            return false; // must be explicit
        } else {
            return User.getAdminUserID().equals(userID);
        }
    }

    /**
    *** Returns true if specified user is and "admin" user
    *** @param user  The user to test
    *** @return True if the specified is an "admin" user
    **/
    public static boolean isAdminUser(User user)
    {
        if (user == null) {
            return true; // null user is considered an 'admin'
        } else {
            return User.getAdminUserID().equalsIgnoreCase(user.getUserID());
        }
    }
    
    /**
    *** Gets the account/user name for the specified user. <br>
    *** (typically used for debug/logging purposes)
    *** @param user  The user for which the account/user name is returned
    *** @return The account/user id/name
    **/
    public static String getUserName(User user)
    {
        if (user == null) {
            return "null";
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            sb.append(user.getAccountID());
            sb.append("/");
            sb.append(user.getUserID());
            sb.append("] ");
            sb.append(user.getDescription());
            return sb.toString().trim();
        }
    }

    // ------------------------------------------------------------------------
    // Blank password
    
    public static final String  BLANK_PASSWORD                  = Account.BLANK_PASSWORD;

    // ------------------------------------------------------------------------
    // Timezones
    
    public static final String  DEFAULT_TIMEZONE                = Account.DEFAULT_TIMEZONE;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // User type enum

    public enum UserType implements EnumTools.StringLocale, EnumTools.IntValue {
        TYPE_000    (  0, I18N.getString(User.class,"User.type.type000"  ,"Type000"  )), // default
        TYPE_001    (  1, I18N.getString(User.class,"User.type.type001"  ,"Type001"  )),
        TYPE_002    (  2, I18N.getString(User.class,"User.type.type002"  ,"Type002"  )),
        TYPE_003    (  3, I18N.getString(User.class,"User.type.type003"  ,"Type003"  )),
        TYPE_010    ( 10, I18N.getString(User.class,"User.type.type010"  ,"Type010"  )),
        TYPE_011    ( 11, I18N.getString(User.class,"User.type.type011"  ,"Type011"  )),
        TYPE_020    ( 20, I18N.getString(User.class,"User.type.type020"  ,"Type020"  )),
        TYPE_021    ( 21, I18N.getString(User.class,"User.type.type021"  ,"Type021"  )),
        TYPE_030    ( 30, I18N.getString(User.class,"User.type.type030"  ,"Type030"  )),
        TYPE_031    ( 31, I18N.getString(User.class,"User.type.type031"  ,"Type031"  )),
        TEMPORARY   (900, I18N.getString(User.class,"User.type.temporary","Temporary")),
        SYSTEM      (999, I18N.getString(User.class,"User.type.system"   ,"System"   ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        UserType(int v, I18N.Text a)                { vv=v; aa=a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(TYPE_000); }
        public boolean isTemporary()                { return this.equals(TEMPORARY); }
        public boolean isSystem()                   { return this.equals(SYSTEM); }
        public boolean isType(int type)             { return this.getIntValue() == type; }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Gender enum

    public enum Gender implements EnumTools.StringLocale, EnumTools.IntValue {
        UNKNOWN     (0, I18N.getString(User.class,"User.gender.notSpecified","n/a"    )),
        MALE        (1, I18N.getString(User.class,"User.gender.male"        ,"Male"   )),
        FEMALE      (2, I18N.getString(User.class,"User.gender.female"      ,"Female" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        Gender(int v, I18N.Text a)                  { vv=v; aa=a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    };

    /**
    *** Returns the defined Gender for the specified user.
    *** @param u  The user from which the Gender will be obtained.  
    ***           If null, the default Gender will be returned.
    *** @return The Gender
    **/
    public static Gender getGender(User u)
    {
        return (u != null)? EnumTools.getValueOf(Gender.class,u.getGender()) : EnumTools.getDefault(Gender.class);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME                  = "User";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_userType                 = "userType";
    public static final String FLD_roleID                   = RoleAcl.FLD_roleID; 
    public static final String FLD_password                 = Account.FLD_password;
    public static final String FLD_gender                   = "gender";
    public static final String FLD_notifyEmail              = Account.FLD_notifyEmail;
    public static final String FLD_contactName              = Account.FLD_contactName;
    public static final String FLD_contactPhone             = Account.FLD_contactPhone;
    public static final String FLD_contactEmail             = Account.FLD_contactEmail;
    public static final String FLD_timeZone                 = Account.FLD_timeZone;
    public static final String FLD_speedUnits               = Account.FLD_speedUnits;
    public static final String FLD_distanceUnits            = Account.FLD_distanceUnits;
    public static final String FLD_firstLoginPageID         = "firstLoginPageID";       // first page viewed at login
    public static final String FLD_preferredDeviceID        = "preferredDeviceID";      // preferred device ID
    public static final String FLD_maxAccessLevel           = "maxAccessLevel";
    public static final String FLD_passwdQueryTime          = "passwdQueryTime";
    public static final String FLD_lastLoginTime            = "lastLoginTime";
    private static DBField FieldInfo[] = {
        // Key fields
        newField_accountID(true),
        newField_userID(true),
        // User fields
        new DBField(FLD_userType            , Integer.TYPE  , DBField.TYPE_UINT16      , "User Type"                 , "edit=2"),
        new DBField(FLD_roleID              , String.class  , DBField.TYPE_ROLE_ID()   , "User Role"                 , "edit=2 altkey=role"),
        new DBField(FLD_password            , String.class  , DBField.TYPE_STRING(32)  , "Password"                  , "edit=2 editor=password"),
        new DBField(FLD_gender              , Integer.class , DBField.TYPE_UINT8       , "Gender"                    , "edit=2 enum=User$Gender"),
        new DBField(FLD_notifyEmail         , String.class  , DBField.TYPE_EMAIL_LIST(), "Notification EMail Address", "edit=2"),
        new DBField(FLD_contactName         , String.class  , DBField.TYPE_STRING(64)  , "Contact Name"              , "edit=2 utf8=true"),
        new DBField(FLD_contactPhone        , String.class  , DBField.TYPE_STRING(32)  , "Contact Phone"             , "edit=2"),
        new DBField(FLD_contactEmail        , String.class  , DBField.TYPE_STRING(64)  , "Contact EMail Address"     , "edit=2 altkey=email"),
        new DBField(FLD_timeZone            , String.class  , DBField.TYPE_STRING(32)  , "Time Zone"                 , "edit=2 editor=timeZone"),
      //new DBField(FLD_speedUnits          , Integer.TYPE  , DBField.TYPE_UINT8       , "Speed Units"               , "edit=2 enum=Account$SpeedUnits"),
      //new DBField(FLD_distanceUnits       , Integer.TYPE  , DBField.TYPE_UINT8       , "Distance Units"            , "edit=2 enum=Account$DistanceUnits"),
        new DBField(FLD_firstLoginPageID    , String.class  , DBField.TYPE_STRING(24)  , "First Login Page ID"       , "edit=2"),
        new DBField(FLD_preferredDeviceID   , String.class  , DBField.TYPE_DEV_ID()    , "Preferred Device ID"       , "edit=2"),
        new DBField(FLD_maxAccessLevel      , Integer.TYPE  , DBField.TYPE_UINT16      , "Maximum Access Level"      , "edit=2 enum=AclEntry$AccessLevel"),
        new DBField(FLD_passwdQueryTime     , Long.TYPE     , DBField.TYPE_UINT32      , "Last Password Query Time"  , "format=time"),
        new DBField(FLD_lastLoginTime       , Long.TYPE     , DBField.TYPE_UINT32      , "Last Login Time"           , "format=time"),
        // Common fields
        newField_isActive(),
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };
    
    // Address fields
    // startupInit.User.AddressFieldInfo=true
    public static final String FLD_addressLine1             = "addressLine1";           // address line 1
    public static final String FLD_addressLine2             = "addressLine2";           // address line 2
    public static final String FLD_addressLine3             = "addressLine3";           // address line 3
    public static final String FLD_addressCity              = "addressCity";            // address city
    public static final String FLD_addressState             = "addressState";           // address state/province
    public static final String FLD_addressPostalCode        = "addressPostalCode";      // address postal code
    public static final String FLD_addressCountry           = "addressCountry";         // address country
    public static final DBField AddressFieldInfo[] = {
        new DBField(FLD_addressLine1        , String.class  , DBField.TYPE_STRING(70)  , "Address Line 1"            , "edit=2 utf8=true"),
        new DBField(FLD_addressLine2        , String.class  , DBField.TYPE_STRING(70)  , "Address Line 2"            , "edit=2 utf8=true"),
        new DBField(FLD_addressLine3        , String.class  , DBField.TYPE_STRING(70)  , "Address Line 3"            , "edit=2 utf8=true"),
        new DBField(FLD_addressCity         , String.class  , DBField.TYPE_STRING(50)  , "Address City"              , "edit=2 utf8=true"),
        new DBField(FLD_addressState        , String.class  , DBField.TYPE_STRING(50)  , "Address State/Province"    , "edit=2 utf8=true"),
        new DBField(FLD_addressPostalCode   , String.class  , DBField.TYPE_STRING(20)  , "Address Postal Code"       , "edit=2 utf8=true"),
        new DBField(FLD_addressCountry      , String.class  , DBField.TYPE_STRING(20)  , "Address Country"           , "edit=2 utf8=true"),
    };

    /* key class */
    public static class Key
        extends UserKey<User>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String userId) {
            super.setFieldValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setFieldValue(FLD_userID   , ((userId != null)? userId.toLowerCase() : ""));
        }
        public DBFactory<User> getFactory() {
            return User.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<User> factory = null;
    public static DBFactory<User> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                User.TABLE_NAME(), 
                User.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                User.class, 
                User.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public User()
    {
        super();
    }

    /* database record */
    public User(User.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(User.class, loc);
        return i18n.getString("User.description", 
            "This table defines " +
            "Account specific Users."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the user type */
    public int getUserType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_userType);
        return (v != null)? v.intValue() : 0;
    }

    /* set the user type */
    public void setUserType(int v)
    {
        this.setFieldValue(FLD_userType, ((v >= 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    /* gets the defined Role, or null if no role was defined */
    private Role userRole = null;
    public Role getRole()
    {
        if ((this.userRole == null) && !StringTools.isBlank(this.getRoleID())) {
            try {
                this.userRole = Role.getRole(this.getAccountID(), this.getRoleID());
                if (this.userRole != null) {
                    if (this.hasAccount() && !this.userRole.isSystemAdminRole()) {
                        // Only set the Role account if not a SystemAdmin Role.
                        this.userRole.setAccount(this.getAccount());
                    }
                } else {
                    Print.logError("User Role not found: %s/%s [user=%s]", this.getAccountID(), this.getRoleID(), this.getUserID());
                    return null;
                }
            } catch (DBException dbe) {
                Print.logException("Error retrieving User Role: " + this.getAccountID() + "/" + this.getRoleID(), dbe);
                return null;
            }
        }
        return this.userRole; // may be null
    }

    /* get the user role id */
    public String getRoleID()
    {
        String v = (String)this.getFieldValue(FLD_roleID);
        return StringTools.trim(v);
    }

    /* set the user role id */
    public void setRoleID(String v)
    {
        this.setFieldValue(FLD_roleID, StringTools.trim(v));
        this.userRole = null;
    }

    // ------------------------------------------------------------------------

    /* get the password of this user */
    public String getPassword()
    {
        String p = (String)this.getFieldValue(FLD_password);
        return (p != null)? p : "";
    }

    /* get the password of this account */
    public String getEncodedPassword()
    {
        return this.getPassword();
    }

    public void setPassword(String p)
    {
        this.setFieldValue(FLD_password, ((p != null)? p : ""));
    }

    /* set the password for this user */
    public void setEncodedPassword(String p)
    {
        this.setPassword(p);
    }

    // --------

    /* get decoded password */
    public String getDecodedPassword()
    {
        String pass = Account.decodePassword(this.getEncodedPassword());
        return pass;
    }

    /* encode and set password */
    public void setDecodedPassword(String enteredPass)
    {
        this.setEncodedPassword(Account.encodePassword(enteredPass));
    }

    // --------

    /* reset the password */
    // does not save the record!
    public String resetPassword()
    {
        String pass = Account.createRandomPassword(Account.TEMP_PASSWORD_LENGTH);
        this.setDecodedPassword(pass);
        return pass; // record not yet saved!
    }

    public boolean checkPassword(String enteredPass)
    {
        return Account.checkPassword(enteredPass, this.getEncodedPassword());
    }

    // ------------------------------------------------------------------------

    /* get the gender of the user */
    public int getGender()
    {
        Integer v = (Integer)this.getFieldValue(FLD_gender);
        return (v != null)? v.intValue() : EnumTools.getDefault(Gender.class).getIntValue();
    }

    /* set the gender */
    public void setGender(int v)
    {
        this.setFieldValue(FLD_gender, EnumTools.getValueOf(Gender.class,v).getIntValue());
    }

    /* set the gender */
    public void setGender(Gender v)
    {
        this.setFieldValue(FLD_gender, EnumTools.getValueOf(Gender.class,v).getIntValue());
    }

    /* set the string representation of the gender */
    public void setGender(String v, Locale locale)
    {
        this.setFieldValue(FLD_gender, EnumTools.getValueOf(Gender.class,v,locale).getIntValue());
    }

    // ------------------------------------------------------------------------

    /* return the notification email address for this account */
    public String getNotifyEmail()
    {
        String v = (String)this.getFieldValue(FLD_notifyEmail);
        return StringTools.trim(v);
    }

    /* set the notification email address for this account */
    public void setNotifyEmail(String v)
    {
        this.setFieldValue(FLD_notifyEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact name of this user */
    public String getContactName()
    {
        String v = (String)this.getFieldValue(FLD_contactName);
        return StringTools.trim(v);
    }

    public void setContactName(String v)
    {
        this.setFieldValue(FLD_contactName, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact phone of this user */
    public String getContactPhone()
    {
        String v = (String)this.getFieldValue(FLD_contactPhone);
        return StringTools.trim(v);
    }

    public void setContactPhone(String v)
    {
        this.setFieldValue(FLD_contactPhone, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact email of this user */
    public String getContactEmail()
    {
        String v = (String)this.getFieldValue(FLD_contactEmail);
        return StringTools.trim(v);
    }

    public void setContactEmail(String v)
    {
        this.setFieldValue(FLD_contactEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    private TimeZone timeZone = null;

    /* get the TimeZone instance for this user */
    // return the specified default if no timezone have been specified
    public TimeZone getTimeZone(TimeZone dft)
    {
        if (this.timeZone == null) {
            this.timeZone = DateTime.getTimeZone(this.getTimeZone(), null);
            if (this.timeZone == null) {
                this.timeZone = (dft != null)? dft : DateTime.getGMTTimeZone();
            }
        }
        return this.timeZone;
    }

    /* get time zone for this user */
    public String getTimeZone()
    {
        String v = (String)this.getFieldValue(FLD_timeZone);
        return StringTools.isBlank(v)? DEFAULT_TIMEZONE : v.trim();
    }

    public void setTimeZone(String v)
    {
        String tz = StringTools.isBlank(v)? DEFAULT_TIMEZONE : v.trim();
        // validate timezone value?
        this.setFieldValue(FLD_timeZone, tz);
    }
    
    /* return current DateTime (relative the User TimeZone) */
    public DateTime getCurrentDateTime()
    {
        return new DateTime(this.getTimeZone(null));
    }

    // ------------------------------------------------------------------------

    /* get the speed-units for this account */
    public int getSpeedUnits()
    {
        Integer v = (Integer)this.getFieldValue(FLD_speedUnits);
        return (v != null)? v.intValue() : EnumTools.getDefault(Account.SpeedUnits.class).getIntValue();
    }

    /* set the speed-units */
    public void setSpeedUnits(int v)
    {
        this.setFieldValue(FLD_speedUnits, EnumTools.getValueOf(Account.SpeedUnits.class,v).getIntValue());
    }

    /* set the speed-units */
    public void setSpeedUnits(Account.SpeedUnits v)
    {
        this.setFieldValue(FLD_speedUnits, EnumTools.getValueOf(Account.SpeedUnits.class,v).getIntValue());
    }

    /* set the string representation of the speed-units */
    public void setSpeedUnits(String v, Locale locale)
    {
        this.setFieldValue(FLD_speedUnits, EnumTools.getValueOf(Account.SpeedUnits.class,v,locale).getIntValue());
    }

    /* return a formatted speed string */
    public String getSpeedString(double speedKPH, boolean inclUnits, Locale locale)
    {
        return this.getSpeedString(speedKPH, "0", null, inclUnits, locale);
    }

    /* return a formatted speed string */
    public String getSpeedString(double speedKPH, String format, boolean inclUnits, Locale locale)
    {
        return this.getSpeedString(speedKPH, format, null, inclUnits, locale);
    }

    /* return a formatted speed string */
    public String getSpeedString(double speedKPH, String format, Account.SpeedUnits speedUnitsEnum, boolean inclUnits, Locale locale)
    {
        if (speedUnitsEnum == null) { speedUnitsEnum = Account.getSpeedUnits(this); }
        double speed = speedUnitsEnum.convertFromKPH(speedKPH);
        String speedFmt = StringTools.format(speed, format);
        if (speed <= 0.0) {
            return speedFmt;
        } else {
            if (inclUnits) {
                return speedFmt + " " + speedUnitsEnum.toString(locale);
            } else {
                return speedFmt;
            }
        }
    }

    // ------------------------------------------------------------------------

    /* get the distance units for this account */
    public int getDistanceUnits()
    {
        Integer v = (Integer)this.getFieldValue(FLD_distanceUnits);
        return (v != null)? v.intValue() : EnumTools.getDefault(Account.DistanceUnits.class).getIntValue();
    }

    /* set the distance units */
    public void setDistanceUnits(int v)
    {
        this.setFieldValue(FLD_distanceUnits, EnumTools.getValueOf(Account.DistanceUnits.class,v).getIntValue());
    }

    /* set the distance units */
    public void setDistanceUnits(Account.DistanceUnits v)
    {
        this.setFieldValue(FLD_distanceUnits, EnumTools.getValueOf(Account.DistanceUnits.class,v).getIntValue());
    }

    /* set the string representation of the distance units */
    public void setDistanceUnits(String v, Locale locale)
    {
        this.setFieldValue(FLD_distanceUnits, EnumTools.getValueOf(Account.DistanceUnits.class,v,locale).getIntValue());
    }

    /* return a formatted distance string */
    public String getDistanceString(double distKM, boolean inclUnits, Locale locale)
    {
        Account.DistanceUnits units = Account.getDistanceUnits(this);
        String distUnitsStr = units.toString(locale);
        double dist         = units.convertFromKM(distKM);
        String distStr      = StringTools.format(dist, "0");
        return inclUnits? (distStr + " " + distUnitsStr) : distStr;
    }

    // ------------------------------------------------------------------------

    /* get default login page ID */
    public String getFirstLoginPageID()
    {
        String v = (String)this.getFieldValue(FLD_firstLoginPageID);
        return StringTools.trim(v);
    }

    public void setFirstLoginPageID(String v)
    {
        this.setFieldValue(FLD_firstLoginPageID, StringTools.trim(v));
    }
    
    public boolean hasFirstLoginPageID()
    {
        return !StringTools.isBlank(this.getFirstLoginPageID());
    }

    // ------------------------------------------------------------------------

    /* get preferred device ID */
    public String getPreferredDeviceID()
    {
        String v = (String)this.getFieldValue(FLD_preferredDeviceID);
        return StringTools.trim(v);
    }

    public void setPreferredDeviceID(String v)
    {
        this.setFieldValue(FLD_preferredDeviceID, StringTools.trim(v));
    }
    
    public boolean hasPreferredDeviceID()
    {
        return !StringTools.isBlank(this.getPreferredDeviceID());
    }

    // ------------------------------------------------------------------------

    /* get maximum access level */
    public int getMaxAccessLevel()
    {
        if (this.isAdminUser()) {
            // admin user is never restricted
            return AccessLevel.ALL.getIntValue();
        } else {
            Integer v = (Integer)this.getFieldValue(FLD_maxAccessLevel);
            if (v != null) {
                int aclLevel = v.intValue();
                if ((aclLevel < 0) || (aclLevel == AccessLevel.NONE.getIntValue())) {
                    // default to ALL, if invalid/undefined
                    return AccessLevel.ALL.getIntValue();
                } else
                if (aclLevel > AccessLevel.ALL.getIntValue()) {
                    // cannot me more than ALL
                    return AccessLevel.ALL.getIntValue();
                } else {
                    // defined maximum access level
                    return aclLevel;
                }
            } else {
                // default to ALL, if undefined
                return AccessLevel.ALL.getIntValue();
            }
        }
    }

    public void setMaxAccessLevel(int v)
    {
        int accessLevel = EnumTools.getValueOf(AccessLevel.class,v).getIntValue();
        this.setFieldValue(FLD_maxAccessLevel, accessLevel);
    }

    public void setMaxAccessLevel(String v)
    {
        int accessLevel = EnumTools.getValueOf(AccessLevel.class,v).getIntValue();
        this.setFieldValue(FLD_maxAccessLevel, accessLevel);
    }

    public void setMaxAccessLevel(AccessLevel v)
    {
        int accessLevel = (v != null)? v.getIntValue() : AccessLevel.ALL.getIntValue();
        this.setFieldValue(FLD_maxAccessLevel, accessLevel);
    }

    // ------------------------------------------------------------------------

    /* return time of last password query */
    public long getPasswdQueryTime()
    {
        Long v = (Long)this.getFieldValue(FLD_passwdQueryTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setPasswdQueryTime(long v)
    {
        this.setFieldValue(FLD_passwdQueryTime, v);
    }

    // ------------------------------------------------------------------------

    /* last user login time */
    public long getLastLoginTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastLoginTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setLastLoginTime(long v)
    {
        this.setFieldValue(FLD_lastLoginTime, v);
    }

    // ------------------------------------------------------------------------

    public String getAddressLine1()
    {
        String v = (String)this.getFieldValue(FLD_addressLine1);
        return StringTools.trim(v);
    }

    public String getAddressLine2()
    {
        String v = (String)this.getFieldValue(FLD_addressLine2);
        return StringTools.trim(v);
    }
    
    public String getAddressLine3()
    {
        String v = (String)this.getFieldValue(FLD_addressLine3);
        return StringTools.trim(v);
    }
    
    public String[] getAddressLines()
    {
        return new String[] {
            this.getAddressLine1(),
            this.getAddressLine2(),
            this.getAddressLine3()
        };
    }
    
    public String getAddressCity()
    {
        String v = (String)this.getFieldValue(FLD_addressCity);
        return StringTools.trim(v);
    }
    
    public String getAddressState()
    {
        String v = (String)this.getFieldValue(FLD_addressState);
        return StringTools.trim(v);
    }
   
    public String getAddressPostalCode()
    {
        String v = (String)this.getFieldValue(FLD_addressPostalCode);
        return StringTools.trim(v);
    }
   
    public String getAddressCountry()
    {
        String v = (String)this.getFieldValue(FLD_addressCountry);
        return StringTools.trim(v);
    }

    public void setAddressLine1(String v)
    {
        this.setFieldValue(FLD_addressLine1, StringTools.trim(v));
    }

    public void setAddressLine2(String v)
    {
        this.setFieldValue(FLD_addressLine2, StringTools.trim(v));
    }

    public void setAddressLine3(String v)
    {
        this.setFieldValue(FLD_addressLine3, StringTools.trim(v));
    }
    
    public void setAddressLines(String lines[])
    {
        if ((lines != null) && (lines.length > 0)) {
            int n = 0;
            while ((n < lines.length) && ((lines[n] == null) || lines[n].trim().equals(""))) { n++; }
            this.setAddressLine1((n < lines.length)? lines[n++].trim() : "");
            while ((n < lines.length) && ((lines[n] == null) || lines[n].trim().equals(""))) { n++; }
            this.setAddressLine2((n < lines.length)? lines[n++].trim() : "");
            while ((n < lines.length) && ((lines[n] == null) || lines[n].trim().equals(""))) { n++; }
            this.setAddressLine3((n < lines.length)? lines[n++].trim() : "");
        } else {
            this.setAddressLine1("");
            this.setAddressLine2("");
            this.setAddressLine3("");
        }
    }

    public void setAddressCity(String v)
    {
        this.setFieldValue(FLD_addressCity, StringTools.trim(v));
    }

    public void setAddressState(String v)
    {
        this.setFieldValue(FLD_addressState, StringTools.trim(v));
    }

    public void setAddressPostalCode(String v)
    {
        this.setFieldValue(FLD_addressPostalCode, StringTools.trim(v));
    }

    public void setAddressCountry(String v)
    {
        this.setFieldValue(FLD_addressCountry, StringTools.trim(v));
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setIsActive(true);
        if (this.isAdminUser()) {
            this.setDescription("Administrator");
            this.setEncodedPassword(this.getAccount().getEncodedPassword());
        } else {
            this.setDescription("New User");
        }
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /* return true if this user is the admin user */
    public boolean isAdminUser()
    {
        return User.isAdminUser(this.getUserID());
    }

    /* return default device authorization */
    public boolean getDefaultDeviceAuthorization()
    {
        if (this.isAdminUser()) {
            // authorized for "ALL" devices
            return true;
        } else {
            // check for "ALL" device authroization
            return DBConfig.GetDefaultDeviceAuthorization(this.getAccountID());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/user */
    protected static DBSelect _getGroupListSelect(String acctId, String userId, long limit)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null user */
        if (StringTools.isBlank(userId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM GroupList WHERE ((accountID='acct') and (userID='user')) ORDER BY groupID
        DBSelect<GroupList> dsel = new DBSelect<GroupList>(GroupList.getFactory());
        dsel.setSelectedFields(GroupList.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(GroupList.FLD_accountID,acctId),
                    dwh.EQ(GroupList.FLD_userID   ,userId)
                )
            )
        );
        dsel.setOrderByFields(GroupList.FLD_groupID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED GROUPS) */
    public static java.util.List<String> getGroupsForUser(String acctId, String userId)
        throws DBException
    {
        return User.getGroupsForUser(acctId, userId, -1L);
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED GROUPS) */
    public static java.util.List<String> getGroupsForUser(String acctId, String userId, long limit)
        throws DBException
    {

        /* valid account/groupId? */
        if (StringTools.isBlank(acctId)) {
            return null;
        } else
        if (StringTools.isBlank(userId)) {
            return null;
        }

        /* get db selector */
        DBSelect dsel = User._getGroupListSelect(acctId, userId, limit);
        if (dsel == null) {
            return null;
        }

        /* read devices for account */
        java.util.List<String> grpList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String grpId = rs.getString(GroupList.FLD_groupID);
                grpList.add(grpId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting User DeviceGroup List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return grpList;

    }

    /* return authorized device groups */
    private java.util.List<String> deviceGroupList = null;
    public java.util.List<String> getDeviceGroups(boolean refresh)
        throws DBException
    {
        if ((this.deviceGroupList == null) || refresh) {
            this.deviceGroupList = User.getGroupsForUser(this.getAccountID(), this.getUserID(), -1L);
        }
        return this.deviceGroupList;
    }

    /* return true if this User is authorized for 'all' groups/devices */
    public boolean isDeviceGroupAll()
        throws DBException
    {
        java.util.List<String> groups = this.getDeviceGroups(false/*refresh*/);
        if (ListTools.isEmpty(groups)) {
            return this.getDefaultDeviceAuthorization();
        } else {
            for (String groupID : groups) {
                if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) { 
                    return true; 
                }
            }
            return false;
        }
    }

    public boolean setDeviceGroups(String groupList[])
    {
        return this._setDeviceGroups(ListTools.toIterator(groupList));
    }

    public boolean setDeviceGroups(java.util.List<String> groupList)
    {
        return this._setDeviceGroups(ListTools.toIterator(groupList));
    }

    protected boolean _setDeviceGroups(Iterator<String> groupListIter)
    {
        String accountID = this.getAccountID();
        String userID    = this.getUserID();
        this.deviceGroupList = null;

        /* delete all existing DeviceGroup entries from the GroupList table for this User */
        // [DELETE FROM GroupList WHERE accountID='account' AND userID='user']
        try {
            DBRecordKey<GroupList> grpListKey = new GroupList.Key();
            grpListKey.setFieldValue(GroupList.FLD_accountID, accountID);
            grpListKey.setFieldValue(GroupList.FLD_userID   , userID);
            DBDelete ddel = new DBDelete(GroupList.getFactory());
            ddel.setWhere(grpListKey.getWhereClause(DBWhere.KEY_PARTIAL_FIRST));
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDefaultConnection();
                dbc.executeUpdate(ddel.toString());
            } finally {
                DBConnection.release(dbc);
            }
        } catch (Throwable th) { // DBException, SQLException
            Print.logException("Error deleting existing DeviceGroup entries from the User GroupList table", th);
            return false;
        }

        /* add new entries */
        if (groupListIter != null) {

            /* check groups other than ALL or blank */
            boolean all = false;
            int grpCount = 0;
            Collection<String> addGroups = new HashSet<String>();
            for (;groupListIter.hasNext();) {
                String groupID = groupListIter.next();
                if (DeviceGroup.DEVICE_GROUP_ALL.equalsIgnoreCase(groupID)) {
                    all = true;
                    addGroups.clear();
                    break;
                } else
                if (DeviceGroup.DEVICE_GROUP_NONE.equalsIgnoreCase(groupID)) {
                    // skip this reserver group id
                } else
                if (StringTools.isBlank(groupID)) {
                    // skip blank group ids
                } else {
                    try {
                        if (DeviceGroup.exists(accountID,groupID)) {
                            grpCount++;
                            addGroups.add(groupID);
                        } else {
                            Print.logError("DeviceGroup does not exist: %s/%s", accountID, groupID);
                        }
                    } catch (DBException dbe) {
                        Print.logException("Error creating new DeviceGroup entries in the User GroupList table", dbe);
                        return false;
                    }
                }
            }

            /* add groupIDs in list */
            if (all) {
                if (!this.getDefaultDeviceAuthorization()) {
                    // if the default device authorization is false, we do explicitly specify that this user
                    // has authority to view "ALL" devices, otherwise he will not athority to view any device.
                    try {
                        GroupList groupListItem = GroupList.getGroupList(this, DeviceGroup.DEVICE_GROUP_ALL, true);
                        groupListItem.save();
                    } catch (DBException dbe) {
                        Print.logException("Error creating new DeviceGroup entries in the User GroupList table", dbe);
                        return false;
                    }
                }
            } else
            if (!ListTools.isEmpty(addGroups)) {
                try {
                    for (String groupID : addGroups) {
                        GroupList groupListItem = GroupList.getGroupList(this, groupID, true);
                        groupListItem.save();
                    }
                } catch (DBException dbe) {
                    Print.logException("Error creating new DeviceGroup entries in the User GroupList table", dbe);
                    return false;
                }
            }

        }
        
        /* success */
        return true;

    }

    public void addDeviceGroup(String groupID)
        throws DBException
    {
        if (!StringTools.isBlank(groupID)) {
            String accountID = this.getAccountID();
            if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL) || DeviceGroup.exists(accountID,groupID)) {
                this.deviceGroupList = null;
                if (!GroupList.exists(accountID,this.getUserID(),groupID)) {
                    GroupList groupListItem = GroupList.getGroupList(this, groupID, true);
                    groupListItem.save();
                } else {
                    // already exists (quietly ignore)
                }
            } else {
                Print.logError("DeviceGroup does not exist: %s/%s", accountID, groupID);
            }
        }
    }

    public void removeDeviceGroup(String groupID)
        throws DBException
    {
        if (!StringTools.isBlank(groupID)) {
            this.deviceGroupList = null;
            GroupList.Key grpListKey = new GroupList.Key(this.getAccountID(), this.getUserID(), groupID);
            grpListKey.delete(true);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* add all user authorized devices to the internal device map */
    public static OrderedSet<String> getAuthorizedDeviceIDs(User user, Account account, boolean inclInactv)
        throws DBException
    {
        if (user != null) {
            return user.getAuthorizedDeviceIDs(inclInactv);
        } else
        if (account != null) {
            return Device.getDeviceIDsForAccount(account.getAccountID(), null, inclInactv);
        } else {
            return new OrderedSet<String>();
        }
    }

    /* add all user authorized devices to the internal device map */
    public static OrderedSet<String> getAuthorizedDeviceIDs(User user, String accountID, boolean inclInactv)
        throws DBException
    {
        if (user != null) {
            return user.getAuthorizedDeviceIDs(inclInactv);
        } else
        if (accountID != null) {
            return Device.getDeviceIDsForAccount(accountID, null, inclInactv);
        } else {
            return new OrderedSet<String>();
        }
    }

    /* get all authorized devices for this user */
    protected OrderedSet<String> getAuthorizedDeviceIDs(boolean inclInactv)
        throws DBException
    {
        java.util.List<String> groupList = this.getDeviceGroups(true/*refresh*/);
        if (!ListTools.isEmpty(groupList)) {
            // The user is authorized to all Devices in the listed groups (thus "User" can be null)
            OrderedSet<String> list = new OrderedSet<String>();
            for (String groupID : groupList) {
                OrderedSet<String> d = DeviceGroup.getDeviceIDsForGroup(this.getAccountID(), groupID, null/*User*/, inclInactv);
                ListTools.toList((java.util.List<String>)d, list);
            }
            return list;
        } else {
            // no explicit defined groups, get all authorized devices
            if (this.getDefaultDeviceAuthorization()) {
                // all devices are authorized
                return Device.getDeviceIDsForAccount(this.getAccountID(), null, inclInactv, -1L);
            } else {
                // no devices are authorized
                return new OrderedSet<String>();
            }
        }
    }

    /* return ture if specified device is authorized for this User */
    public boolean isAuthorizedDevice(String deviceID)
        throws DBException
    {
        if (StringTools.isBlank(deviceID)) {
            return false;
        } else {
            java.util.List<String> groupList = this.getDeviceGroups(false/*refresh*/);
            if (ListTools.isEmpty(groupList)) {
                return this.getDefaultDeviceAuthorization();
            } else {
                for (String groupID : groupList) {
                    // authorized if the device exists in the DeviceGroup (DeviceList)
                    if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                        // always authorized for group 'all'
                        return true;
                    } else
                    if (DeviceGroup.exists(this.getAccountID(), groupID, deviceID)) {
                        return true;
                    }
                }
                // does not exist in any authorized group
                Print.logInfo("Not authorized device for user '%s': %s", this.getUserID(), deviceID);
                return false;
            }
        }
    }

    /* get the preferred/first authorized device for this user */
    public String getDefaultDeviceID(boolean inclInactv)
        throws DBException
    {

        /* first check preferred device */
        if (this.hasPreferredDeviceID()) {
            String devID = this.getPreferredDeviceID();
            try {
                if (Device.exists(this.getAccountID(),devID) && this.isAuthorizedDevice(devID)) {
                    return devID;
                }
            } catch (DBException dbe) {
                // 'Device.exists' error, ignore
            }
            // device does not exist, or not authorized for preferred device
        }

        /* check for first authorized device */
        java.util.List<String> groupList = User.getGroupsForUser(this.getAccountID(), this.getUserID(), 1L);
        if (ListTools.isEmpty(groupList)) {
            // no defined groups
            if (this.getDefaultDeviceAuthorization()) {
                // all devices are authorized, return first device
                OrderedSet<String> d = Device.getDeviceIDsForAccount(this.getAccountID(), null, inclInactv, 1);
                return !ListTools.isEmpty(d)? d.get(0) : null;
            } else {
                // no devices are authorized
                return null;
            }
        } else {
            String groupID = groupList.get(0);
            OrderedSet<String> d = DeviceGroup.getDeviceIDsForGroup(this.getAccountID(), groupID, null, inclInactv, 1L);
            return !ListTools.isEmpty(d)? d.get(0) : null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return list of all currently defined ACLs for this User */
    // does not return null
    public String[] getAclsForUser()
        throws DBException
    {
        String acctID = this.getAccountID();
        
        /* read ACLs for user */
        java.util.List<String> aclList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
        
            /* select */
            // DBSelect: SELECT aclID FROM UserAcl WHERE (accountID='acct') AND (userID='user') ORDER BY aclID
            DBSelect<UserAcl> dsel = new DBSelect<UserAcl>(UserAcl.getFactory());
            dsel.setSelectedFields(UserAcl.FLD_aclID);
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(UserAcl.FLD_accountID,this.getAccountID()),
                    dwh.EQ(UserAcl.FLD_userID,this.getUserID())
                )
            ));
            dsel.setOrderByFields(UserAcl.FLD_aclID);
    
            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String aclId = rs.getString(UserAcl.FLD_aclID);
                aclList.add(aclId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting User ACL List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return aclList.toArray(new String[aclList.size()]);

    }

    // ------------------------------------------------------------------------

    /* to String value */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getUserID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if the specified user exists */
    public static boolean exists(String acctID, String userID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (userID != null)) {
            User.Key userKey = new User.Key(acctID, userID);
            return userKey.exists();
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /* Return specified user (may return null) */
    public static User getUser(Account account, String userId)
        throws DBException
    {
        if ((account != null) && (userId != null)) {
            User.Key userKey = new User.Key(account.getAccountID(), userId);
            if (userKey.exists()) {
                User user = userKey.getDBRecord(true);
                user.setAccount(account);
                return user;
            } else {
                return null;
            }
        } else {
            throw new DBException("Account or UserID is null");
        }
    }

    /* Return specified user, create if specified (does not return null) */
    public static User getUser(Account account, String userId, boolean create)
        throws DBException
    {
        
        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctId = account.getAccountID();

        /* user-id specified? */
        if ((userId == null) || userId.equals("")) {
            throw new DBNotFoundException("User-ID not specified.");
        }

        /* get/create user */
        User user = null;
        User.Key userKey = new User.Key(acctId, userId);
        if (!userKey.exists()) { // may throw DBException
            if (create) {
                user = userKey.getDBRecord();
                user.setAccount(account);
                user.setCreationDefaultValues();
                return user; // not yet saved!
            } else {
                throw new DBNotFoundException("User-ID does not exists '" + userKey + "'");
            }
        } else
        if (create) {
            // we've been asked to create the user, and it already exists
            throw new DBAlreadyExistsException("User-ID already exists '" + userKey + "'");
        } else {
            user = User.getUser(account, userId); // may throw DBException
            if (user == null) {
                throw new DBException("Unable to read existing User-ID '" + userKey + "'");
            }
            return user;
        }

    }

    /* Create specified user.  Return null if user already exists */
    public static User createNewUser(Account account, String userID, String contactEmail, String passwd)
        throws DBException
    {
        if ((account != null) && (userID != null) && !userID.equals("")) {
            // create user record (not yet saved)
            User user = User.getUser(account, userID, true); // does not return null
            // set contact email address
            if (contactEmail != null) {
                user.setContactEmail(contactEmail);
            }
            // set password
            if (passwd != null) {
                user.setDecodedPassword(passwd);
            }
            // save
            user.save();
            return user;
        } else {
            throw new DBNotFoundException("Invalid Account/UserID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return list of all Users owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getUsersForAccount(String acctId)
        throws DBException
    {
        return User.getUsersForAccount(acctId, -1);
    }

    /* return list of all Users owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getUsersForAccount(String acctId, int userType)
        throws DBException
    {

        /* invalid account */
        if ((acctId == null) || acctId.equals("")) {
            return new String[0];
        }

        /* select */
        // DBSelect: SELECT userID FROM User WHERE (accountID='acct') ORDER BY userID
        DBSelect<User> dsel = new DBSelect<User>(User.getFactory());
        dsel.setSelectedFields(User.FLD_userID);
        DBWhere dwh = dsel.createDBWhere();
        //dsel.setWhere(dwh.WHERE_(dwh.EQ(User.FLD_accountID,acctId)));
        dwh.append(dwh.EQ(User.FLD_accountID,acctId));
        if (userType >= 0) {
            // AND (userType=0)
            dwh.append(dwh.AND_(dwh.EQ(User.FLD_userType,userType)));
        }
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(User.FLD_userID);

        /* select */
        return User.getUserIDs(dsel);
        
    }
    
    /* return list of all Users owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getUserIDs(DBSelect<User> dsel)
        throws DBException
    {

        /* invalid selection */
        if (dsel == null) {
            return new String[0];
        }
        dsel.setSelectedFields(User.FLD_userID);

        /* read users for account */
        java.util.List<String> userList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String userId = rs.getString(User.FLD_userID);
                userList.add(userId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account User List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return userList.toArray(new String[userList.size()]);

    }

    // ------------------------------------------------------------------------

    /* return the first user which specifies this email address as the contact email */
    public static User getUserForContactEmail(String acctId, String emailAddr)
        throws DBException
    {
        java.util.List<User> userList = User.getUsersForContactEmail(acctId, emailAddr);
        return !ListTools.isEmpty(userList)? userList.get(0) : null;
    }

    /* return all users which list this email address as the contact email */
    public static java.util.List<User> getUsersForContactEmail(String acctId, String emailAddr)
        throws DBException
    {
        java.util.List<User> userList = new Vector<User>();

        /* invalid account? */
        boolean acctIdBlank = StringTools.isBlank(acctId);
        //if (acctIdBlank) {
        //    return userList;
        //}

        /* EMailAddress specified? */
        if (StringTools.isBlank(emailAddr)) {
            return userList; // empty list
        }

        /* read users for contact email */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT userID FROM User WHERE [(accountID='account') AND] (contactEmail='email')
            DBSelect<User> dsel = new DBSelect<User>(User.getFactory());
            //dsel.setSelectedFields(User.FLD_accountID,User.FLD_userID);
            DBWhere dwh = dsel.createDBWhere();
            if (acctIdBlank) {
                dwh.append(
                    dwh.EQ(User.FLD_contactEmail,emailAddr)
                );
            } else {
                dwh.append(dwh.AND(
                    dwh.EQ(User.FLD_accountID   ,acctId),
                    dwh.EQ(User.FLD_contactEmail,emailAddr)
                ));
            }
            dsel.setWhere(dwh.WHERE(dwh.toString()));
            dsel.setOrderByFields(User.FLD_userID);
            // Note: The index on the column FLD_contactEmail is not unique

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String aid = rs.getString(User.FLD_accountID);
                String uid = rs.getString(User.FLD_userID);
                User user = new User(new User.Key(aid, uid));
                user.setAllFieldValues(rs);
                userList.add(user);
            }

        } catch (SQLException sqe) {
            throw new DBException("Get User ContactEmail", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        return userList;
    }

    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/user */
    protected static DBSelect<User> _getUsersForRoleSelect(String acctID, String roleID, long limit)
    {

        /* invalid accountID? */
        if (StringTools.isBlank(acctID)) {
            return null;
        }

        /* invalid roleID? */
        if (StringTools.isBlank(roleID)) {
            return null;
        }

        /* select */
        // DBSelect: SELECT accountID,userID FROM User WHERE (accountID='account') AND (roleID='role')
        DBSelect<User> dsel = new DBSelect<User>(User.getFactory());
        dsel.setSelectedFields(User.FLD_accountID,User.FLD_userID);
        DBWhere dwh = dsel.createDBWhere();
        dwh.append(dwh.AND(
            dwh.EQ(User.FLD_accountID, acctID),
            dwh.EQ(User.FLD_roleID   , roleID)
        ));
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(User.FLD_userID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return true if there are any users that reference the specified role */
    public static boolean hasUserIDsForRole(String acctID, String roleID)
        throws DBException
    {
        return !ListTools.isEmpty(User.getUserIDsForRole(acctID, roleID, 1L));
    }

    /* return all user IDs for the specified role ID */
    public static java.util.List<String> getUserIDsForRole(String acctID, String roleID)
        throws DBException
    {
        return User.getUserIDsForRole(acctID, roleID, -1L);
    }

    /* return all user IDs for the specified role ID */
    public static long countUserIDsForRole(String acctID, String roleID)
        throws DBException
    {

        /* valid account/roleId? */
        if (StringTools.isBlank(acctID)) {
            return 0L;
        } else
        if (StringTools.isBlank(roleID)) {
            return 0L;
        }

        /* get db selector */
        DBSelect<User> dsel = User._getUsersForRoleSelect(acctID, roleID, -1);
        if (dsel == null) {
            return 0L;
        }

        /* count users */
        long recordCount = 0L;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            recordCount = DBRecord.getRecordCount(dsel);
        } finally {
            DBProvider.unlockTables();
        }
        return recordCount;

    }

    /* return all user IDs for the specified role ID */
    public static java.util.List<String> getUserIDsForRole(String acctID, String roleID, long limit)
        throws DBException
    {
        java.util.List<String> userList = new Vector<String>();

        /* valid account/roleId? */
        if (StringTools.isBlank(acctID)) {
            return null;
        } else
        if (StringTools.isBlank(roleID)) {
            return null;
        }

        /* get db selector */
        DBSelect<User> dsel = User._getUsersForRoleSelect(acctID, roleID, limit);
        if (dsel == null) {
            return null;
        }

        /* read users for roleID */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String aid = rs.getString(User.FLD_accountID);
                String uid = rs.getString(User.FLD_userID);
                userList.add(uid);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Users for Role", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        return userList;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = new String[] { "account" , "acct"  , "a" };
    private static final String ARG_USER[]      = new String[] { "user"    , "usr"   , "u" };
    private static final String ARG_EMAIL[]     = new String[] { "email"             };
    private static final String ARG_CREATE[]    = new String[] { "create"  , "cr"    };
    private static final String ARG_NOPASS[]    = new String[] { "nopass"            };
    private static final String ARG_PASSWORD[]  = new String[] { "password", "passwd", "pass" };
    private static final String ARG_EDIT[]      = new String[] { "edit"    , "ed"    };
    private static final String ARG_EDITALL[]   = new String[] { "editall" , "eda"   };
    private static final String ARG_DELETE[]    = new String[] { "delete"  , "purge" };
    private static final String ARG_LIST[]      = new String[] { "list"              };
    private static final String ARG_TEST[]      = new String[] { "test"              };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + User.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns User");
        Print.logInfo("  -user=<id>      User ID to create/edit");
        Print.logInfo("  -create         Create a new User");
        Print.logInfo("  -edit           Edit an existing (or newly created) User");
        Print.logInfo("  -delete         Delete specified User");
        Print.logInfo("  -list           List Users for Account");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID       = RTConfig.getString(ARG_ACCOUNT, "");
        String userID       = RTConfig.getString(ARG_USER   , "");
        String contactEmail = RTConfig.getString(ARG_EMAIL  , "");
        boolean listUsers   = RTConfig.getBoolean(ARG_LIST  , false);
        
        /* option count */
        int opts = 0;

        /* account-id specified? */
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may return DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* list */
        if (listUsers) {
            opts++;
            try {
                Print.logInfo("Account: " + acctID);
                String userList[] = User.getUsersForAccount(acctID);
                for (int i = 0; i < userList.length; i++) {
                    Print.logInfo("  User: " + userList[i]);
                }
            } catch (DBException dbe) {
                Print.logError("Error listing Users: " + acctID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }
        
        // the following require a "-user" specification

        /* user-id specified? */
        if ((userID == null) || userID.equals("")) {
            Print.logError("User-ID not specified.");
            usage();
        }

        /* user exists? */
        boolean userExists = false;
        try {
            userExists = User.exists(acctID, userID);
        } catch (DBException dbe) {
            Print.logError("Error determining if User exists: " + acctID + "," + userID);
            System.exit(99);
        }
        
        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !userID.equals("")) {
            opts++;
            if (!userExists) {
                Print.logWarn("User does not exist: " + acctID + "/" + userID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                User.Key userKey = new User.Key(acctID, userID);
                userKey.delete(true); // also delete dependencies
                Print.logInfo("User deleted: " + acctID + "/" + userID);
            } catch (DBException dbe) {
                Print.logError("Error deleting User: " + acctID + "/" + userID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (userExists) {
                Print.logWarn("User already exists: " + acctID + "/" + userID);
            } else {
                try {
                    String passwd = null;
                    if (RTConfig.getBoolean(ARG_NOPASS,false)) {
                        passwd = BLANK_PASSWORD;
                    } else
                    if (RTConfig.hasProperty(ARG_PASSWORD)) {
                        passwd = RTConfig.getString(ARG_PASSWORD,"");
                    }
                    User.createNewUser(acct, userID, contactEmail, passwd);
                    Print.logInfo("Created User-ID: " + acctID + "/" + userID);
                } catch (DBException dbe) {
                    Print.logError("Error creating User: " + acctID + "/" + userID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!userExists) {
                Print.logError("User does not exist: " + acctID + "/" + userID);
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    User user = User.getUser(acct, userID, false); // may throw DBException
                    DBEdit editor = new DBEdit(user);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing User: " + acctID + "/" + userID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* test */
        if (RTConfig.hasProperty(ARG_TEST)) {
            int test = RTConfig.getInt(ARG_TEST,0);
            opts++;
            if (!userExists) {
                Print.logError("User does not exist: " + acctID + "/" + userID);
            } else {
                String dg[] = new String[] { "G1", "G2", "G3", "G4", "G5" };
                switch (test) {
                    case 1: dg = new String[] { "G6", "G7", "G8", "G9", "GA" }; break;
                    case 2: dg = new String[] { "G4", "G5", "G6", "G7", "G8" }; break;
                }
                try {
                    User user = User.getUser(acct, userID, false); // may throw DBException
                    java.util.List<String> groupList = user.getDeviceGroups(true/*refresh*/);
                    for (String gid : groupList) { Print.sysPrintln("Old DeviceGroup: " + gid); }
                    user.setDeviceGroups((String[])null);
                    Print.sysPrintln("1) Is 'mobile' Authorized Device?: " + user.isAuthorizedDevice("mobile"));
                    user.setDeviceGroups(dg);
                    groupList = user.getDeviceGroups(true/*refresh*/);
                    for (String gid : groupList) { Print.sysPrintln("New DeviceGroup: " + gid); }
                    Print.sysPrintln("2) Is 'mobile' Authorized Device?: " + user.isAuthorizedDevice("mobile"));
                } catch (DBException dbe) {
                    Print.logError("Error testing User: " + acctID + "/" + userID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

    // ------------------------------------------------------------------------

}
