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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track.page.devcmd;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;
import org.opengts.war.track.page.*;

import org.opengts.db.dmtp.*;

public class DeviceCmd_gtsdmtp
    implements DeviceCmdHandler
{

    // ------------------------------------------------------------------------
  
    /* device code */
    public static final String  DEVICE_CODE                 = DCServerFactory.OPENDMTP_NAME;

    // ------------------------------------------------------------------------

    public  static final String FORM_PROPERTY_EDIT          = "DevicePropEdit";

    // DeviceInfo commands
    public  static final String COMMAND_INFO_UPD_PROPS      = DeviceInfo.COMMAND_INFO_UPD_PROPS;

    // DeviceInfo parameters
    public  static final String PARM_COMMAND                = DeviceInfo.PARM_COMMAND;
    public  static final String PARM_DEVICE                 = DeviceInfo.PARM_DEVICE;
    public  static final String PARM_DEV_DESC               = DeviceInfo.PARM_DEV_DESC;

    // submit
    public  static final String PARM_SUBMIT_QUE             = DeviceInfo.PARM_SUBMIT_QUE;

    // buttons
    public  static final String PARM_BUTTON_CANCEL          = "d_btncan";
    public  static final String PARM_BUTTON_BACK            = "d_btnbak";

    // device properties
    public  static final String PARM_PROP_START_TYPE        = "p_startTyp";
    public  static final String PARM_PROP_START_DEF         = "p_startDef";
    public  static final String PARM_PROP_MOT_INTERV        = "p_motion";
    public  static final String PARM_PROP_STOP_TYPE         = "p_stopTyp";
    public  static final String PARM_PROP_STOP_INTERV       = "p_stopIntv";
    public  static final String PARM_PROP_DORM_INTERV       = "p_dormIntv";
    public  static final String PARM_PROP_DORM_COUNT        = "p_dormCnt";
    public  static final String PARM_PROP_EXCESS_SPEED      = "p_overSpeed";
  //public  static final String PARM_GET_PROPERT_STATE      = "p_getProps";

    // ------------------------------------------------------------------------
    
    private String              serverID    = DEVICE_CODE;

    /* DeviceCommand constructor */
    public DeviceCmd_gtsdmtp()
    {
        //
    }
    
    // ------------------------------------------------------------------------

    /* DCS id */
    public void setServerIDArg(String arg)
    {
        if (!StringTools.isBlank(arg)) {
            this.serverID = DEVICE_CODE + arg.trim();
        }
    }

    /* DCS id */
    public String getServerID()
    {
        return this.serverID;
    }

    /* DCS name */
    public String getServerDescription()
    {
        return DCServerFactory.getServerConfigDescription(this.getServerID());
    }
    
    // ------------------------------------------------------------------------

    /* true if this UI supports the specified device */
    public boolean deviceSupportsCommands(Device dev)
    {
        
        /* null device specified? */
        if (dev == null) {
            return false;
        }
        
        /* check serverID */
        String serverID = dev.getDeviceCode();
        if (StringTools.isBlank(serverID)) {
            return false;
        } else
        if (!serverID.equalsIgnoreCase(this.getServerID())) {
            return false;
        }
        
        /* check for DMTP support */
        if (!dev.getSupportsDMTP()) {
            Print.logWarn("Device with serverID '"+serverID+"' indicates 'supportsDMTP=false'");
            return false;
        } else {
            return true;
        }

    }
    
    // ------------------------------------------------------------------------

    /* write the command form html */
    public boolean writeCommandForm(PrintWriter out, RequestProperties reqState, Device selDev,
        String actionURL, boolean editProps)
        throws IOException
    {

        /* check for nulls */
        if ((out == null) || (reqState == null) || (selDev == null)) {
            return false;
        }
        
        /* supported device? */
        if (!this.deviceSupportsCommands(selDev)) {
            return false;
        }

        /* init */
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(DeviceCmd_gtsdmtp.class);
        String       selDevID    = selDev.getDeviceID();
        String       devTitles[] = reqState.getDeviceTitles();

        /* start of form */
        out.write("<form name='"+FORM_PROPERTY_EDIT+"' method='POST' action='"+actionURL+"' target='_self'>\n"); // target='_top'
        out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPD_PROPS+"'/>\n");

        /* Device fields */
        out.println("<table>");
        out.println(DeviceInfo.FormRow_TextField(PARM_DEVICE           , false    , i18n.getString("DeviceCmd_gtsdmtp.deviceID","{0} ID",devTitles)+":"        , selDevID, 30, 30));
        out.println(DeviceInfo.FormRow_TextField(PARM_DEV_DESC         , false    , i18n.getString("DeviceCmd_gtsdmtp.deviceDesc","{0} Description",devTitles) +":", (selDev!=null)?selDev.getDescription():"", 40, 40));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_START_TYPE  , editProps, i18n.getString("DeviceCmd_gtsdmtp.startType","Start Type")+":"             , "", 2, 2));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_START_DEF   , editProps, i18n.getString("DeviceCmd_gtsdmtp.startDefinition","Start Definition")+":" , "", 5, 5));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_MOT_INTERV  , editProps, i18n.getString("DeviceCmd_gtsdmtp.motionInterval","In-Motion Interval")+":", "", 5, 5));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_STOP_TYPE   , editProps, i18n.getString("DeviceCmd_gtsdmtp.stopType","Stop Type")+":"               , "", 2, 2));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_STOP_INTERV , editProps, i18n.getString("DeviceCmd_gtsdmtp.stopInterval","Stop Interval")+":"       , "", 5, 5));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_DORM_INTERV , editProps, i18n.getString("DeviceCmd_gtsdmtp.dormantInterval","Dormant Interval")+":" , "", 5, 5));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_DORM_COUNT  , editProps, i18n.getString("DeviceCmd_gtsdmtp.dormantCount","Dormant Count")+":"       , "", 5, 5));
        out.println(DeviceInfo.FormRow_TextField(PARM_PROP_EXCESS_SPEED, editProps, i18n.getString("DeviceCmd_gtsdmtp.excessSpeed","Excess Speed")+":"         , "", 5, 5));
        out.println("</table>");

        /* end of form */
        out.write("<hr style='margin-bottom:5px;'>\n");
        if (editProps) {
            out.write("<input type='submit' name='"+PARM_SUBMIT_QUE+"' value='"+i18n.getString("DeviceCmd_gtsdmtp.queue","Queue")+"'>\n");
            out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
            out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("DeviceCmd_gtsdmtp.cancel","Cancel")+"' onclick=\"javascript:openURL('"+actionURL+"','_self');\">\n");
        } else {
            out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("DeviceCmd_gtsdmtp.back","Back")+"' onclick=\"javascript:openURL('"+actionURL+"','_self');\">\n");
        }
        out.write("</form>\n");
        return true;

    }
    
    // ------------------------------------------------------------------------
    
    /* encode a PendingPacket.SetPropertyPacket, and add it to the property change list */
    private static boolean _addPropertyPacket(HttpServletRequest request, String reqAttr, int propCode, 
        java.util.List<PendingPacket.SetPropertyPacket> propList)
    {

        /* get new property value */
        String val = AttributeTools.getRequestString(request, reqAttr, "");
        if (StringTools.isBlank(val) || val.equals("?")) {
            return true;
        }

        /* get/update property state */
        /*
        if (reqAttr.equals(PARM_GET_PROPERT_STATE)) {
            // PropertyKey.PROP_MOTION_START_TYPE
            // PropertyKey.PROP_MOTION_START
            // PropertyKey.PROP_MOTION_IN_MOTION
            // PropertyKey.PROP_MOTION_STOP_TYPE
            // PropertyKey.PROP_MOTION_STOP
            // PropertyKey.PROP_MOTION_DORMANT_INTRVL
            // PropertyKey.PROP_MOTION_DORMANT_COUNT
            // PropertyKey.PROP_MOTION_EXCESS_SPEED
            return true;
        }
        */

        /* insert PendingPacket */
        PendingPacket.SetPropertyPacket spp = PendingPacket.createSetPropertyPacket(propCode, val);
        if (spp != null) {
            propList.add(spp);
            return true;
        } else {
            return false;
        }

    }

    // ------------------------------------------------------------------------

    /* update Device table with user entered information */
    public String handleDeviceCommands(RequestProperties reqState, Device selDev)
    {

        /* check for nulls */
        if ((reqState == null) || (selDev == null)) {
            return "Invalid 'queueDeviceProperties' parameters";
        }

        /* init */
        HttpServletRequest request     = reqState.getHttpServletRequest();
        PrivateLabel       privLabel   = reqState.getPrivateLabel();
        I18N               i18n        = privLabel.getI18N(DeviceCmd_gtsdmtp.class);
        String             devTitles[] = reqState.getDeviceTitles();

        /* supported device? */
        if (!this.deviceSupportsCommands(selDev)) {
            return i18n.getString("DeviceCmd_gtsdmtp.doesNotSupportDMTP","Device does not support DMTP");
        }

        /* retrieve user entered property changes */
        java.util.List<PendingPacket.SetPropertyPacket> propList = new Vector<PendingPacket.SetPropertyPacket>();
        // PropertyKey.PROP_MOTION_START_TYPE
        if (!_addPropertyPacket(request,PARM_PROP_START_TYPE,PropertyKey.PROP_MOTION_START_TYPE,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidStartType","Invalid 'Start Type' property");
        }
        // PropertyKey.PROP_MOTION_START
        if (!_addPropertyPacket(request,PARM_PROP_START_DEF,PropertyKey.PROP_MOTION_START,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidStartDefinition","Invalid 'Start Definition' property");
        }
        // PropertyKey.PROP_MOTION_IN_MOTION
        if (!_addPropertyPacket(request,PARM_PROP_MOT_INTERV,PropertyKey.PROP_MOTION_IN_MOTION,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidInMotion","Invalid 'In-Motion Interval' property");
        }
        // PropertyKey.PROP_MOTION_STOP_TYPE
        if (!_addPropertyPacket(request,PARM_PROP_STOP_TYPE,PropertyKey.PROP_MOTION_STOP_TYPE,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidStopType","Invalid 'Stop Type' property");
        }
        // PropertyKey.PROP_MOTION_STOP
        if (!_addPropertyPacket(request,PARM_PROP_STOP_INTERV,PropertyKey.PROP_MOTION_STOP,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidStopInterval","Invalid 'Stop Interval' property");
        }
        // PropertyKey.PROP_MOTION_DORMANT_INTRVL
        if (!_addPropertyPacket(request,PARM_PROP_DORM_INTERV,PropertyKey.PROP_MOTION_DORMANT_INTRVL,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidDormantInterval","Invalid 'Dormant Interval' property");
        }
        // PropertyKey.PROP_MOTION_DORMANT_COUNT
        if (!_addPropertyPacket(request,PARM_PROP_DORM_COUNT,PropertyKey.PROP_MOTION_DORMANT_COUNT,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidDormantCount","Invalid 'Dormant Count' property");
        }
        // PropertyKey.PROP_MOTION_EXCESS_SPEED
        if (!_addPropertyPacket(request,PARM_PROP_EXCESS_SPEED,PropertyKey.PROP_MOTION_EXCESS_SPEED,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.invalidExcessSpeed","Invalid 'Excess Speed' property");
        }
        
        // PARM_GET_PROPERT_STATE
        /*
        if (!_addPropertyPacket(request,PARM_GET_PROPERT_STATE,-1,propList)) {
            return i18n.getString("DeviceCmd_gtsdmtp.unsupportedSetUpdateProperties","Set/Update properties not supported");
        }
        */

        /* no properties defined? */
        if (propList.isEmpty()) {
            return i18n.getString("DeviceCmd_gtsdmtp.noPropertiesChanged","No property changes have been defined");
        }

        /* insert property changes */
        int queueCount = 0;
        try {
            for (Iterator i = propList.iterator(); i.hasNext();) {
                PendingPacket.SetPropertyPacket spp = (PendingPacket.SetPropertyPacket)i.next();
                if (PendingPacket.insertSetPropertyPacket(selDev, spp)) {
                    queueCount++;
                }
            }
        } catch (DBException dbe) {
            Print.logException("Inserting PendingPacket", dbe);
            return i18n.getString("DeviceCmd_gtsdmtp.errorPendingPacket","Internal PendingPacket error");
        }

        /* return result */
        if (queueCount == 0) {
            return i18n.getString("DeviceCmd_gtsdmtp.noPropertiesQueued","No property changes have been queued");
        } else {
            return i18n.getString("DeviceCmd_gtsdmtp.setPropertiesQueued","Requested property changes have been queued");
        }

    }

    // ------------------------------------------------------------------------

}

