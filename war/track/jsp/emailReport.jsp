<%@ taglib uri="./Track" prefix="gts" %>
<%@ page isELIgnored="true" contentType="text/html; charset=UTF-8" %>
<%
//response.setContentType("text/html; charset=UTF-8");
//response.setCharacterEncoding("UTF-8");
response.setHeader("CACHE-CONTROL", "NO-CACHE");
response.setHeader("PRAGMA"       , "NO-CACHE");
response.setDateHeader("EXPIRES"  , 0         );
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<!-- <!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> -->
<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>
<!-- jsp/emailReport.jsp: <gts:var>${version} [${privateLabelName}]</gts:var>
  =======================================================================================
  Copyright(C) 2007-2011 GeoTelematic Solutions, Inc., All rights reserved
  Project: OpenGTS - Open GPS Tracking System [http://www.opengts.org]
  =======================================================================================
-->

<!-- Head -->
<head>

  <!-- meta tags -->
  <gts:var>
  <meta name="author" content="GeoTelematic Solutions, Inc."/>
  <meta http-equiv="content-type" content='text/html; charset=UTF-8'/>
  <meta http-equiv="cache-control" content='no-cache'/>
  <meta http-equiv="pragma" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta name="copyright" content="${copyright}"/>
  <meta name="robots" content="none"/>
  </gts:var>

  <!-- page title -->
  <gts:var>
  <title>${pageTitle}</title>
  </gts:var>
  
  <!-- default style -->
  <gts:track section="cssfile" arg="General.css"/>
  <gts:track section="cssfile" arg="Controls.css"/>

  <!-- custom style -->
  <gts:track section="stylesheet"/>

  <!-- local style -->
  <style type="text/css">
    TD.titleText {
      font-family: Arial;
      font-size: 18pt;
      font-weight: bold; 
      text-align: center;
      color: #000000;
    }
  </style>

</head>

<!-- ======================================================================================= -->

<body>

<table border='0' cellspacing='0' cellpadding='0' width='100%' height='100%'>
<tbody>

  <!-- Begin Page header/navigation ======================================== -->
  <tr>
  <td width="100%">
    <table width="100%" border="0" cellpadding="0" cellspacing="0" bgcolor="#EFF7FF">
    <tbody>
    <tr>
      <gts:var>
      <td class="titleText" valign="center">
        ${pageTitle}<br>
      </td>
      </gts:var>
    </tr>
    </tbody>
    </table>
  </td>
  </tr>
  <tr>
  <td>
     <table width="100%" border="0" cellpadding="0" cellspacing="0">
     <tbody>
     <tr>
       <td class="navBar" nowrap align="left">&nbsp;<gts:var ifKey="isLoggedIn" value="true"><i>${i18n.Account}:</i> ${accountDesc} (${userDesc})</gts:var></td>
       <td class="navBar" nowrap align="right" width="100%">&nbsp;</td>
     </tr>
     </tbody>
     </table>
  </td>
  </tr>
  <!-- End Page header/navigation ======================================== -->

  <!-- Begin Page contents ======================================== -->
  <tr height="100%">
  <td>
    <table width="100%" height="100%" border="0" cellpadding="0" cellspacing="0">
    <tbody>
    <tr>
        <td valign='top' align='center' height='100%'>
           <table class="<gts:track section='content.class.table'/>" cellspacing='0' cellpadding='0' border='0'>
           <tbody>
              <!-- The MenuBar is display iff 'content.class.table' is "contentTable"/"contentMapTable" -->
              <tr height='100%'>
                <td class="<gts:track section='content.class.cell'/>">
                    <gts:track section="content.body"/>
                </td>
              </tr>
              <tr>
                <td id="<gts:track section='content.id.message'/>" class="<gts:track section='content.class.message'/>">
                    <gts:track section="content.message"/>
                </td>
              </tr>
           </tbody>
           </table>
        </td>
    </tr>
    </tbody>
    </table>
  </td>
  </tr>
  <!-- End Page contents ======================================== -->

  <!-- Begin Page footer ======================================== -->
  <tr>
    <td style="font-size: 7pt; border-bottom: 1px solid #888888;">&nbsp;</td>
  </tr>
  <tr>
  <td>
    <table class="copyrightFooter" width="100%" border="0" cellpadding="0" cellspacing="0">
    <tbody>
    <tr>
      <td style="padding: 0px 0px 2px 5px;">
        &nbsp;
      </td>
      <td width="100%">
         <gts:var>${copyright}</gts:var>
      </td>
      <td nowrap style="padding-bottom: 2px;">
         <span style="font-size: 7pt; font-style: oblique; color: #888888;"><gts:var ifTrue="login.showGTSVersion">${version}</gts:var></span>&nbsp;&nbsp;
         <gts:var ifTrue="login.showPiLink"><a style="font-size: 11pt; text-decoration: none;" href="${login.piLink=http://www.opengts.org}" target="_blank">&pi;</a>&nbsp;</gts:var>
      </td>
    </tr>
    </tbody>
    </table>
  </td>
  </tr>
  <!-- End Page footer ======================================== -->

</tbody>
</table>
</body>

<!-- ======================================================================================= -->

</html>
