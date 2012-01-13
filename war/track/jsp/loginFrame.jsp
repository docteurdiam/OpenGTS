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

<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>
<!-- jsp/loginFrame.jsp: <gts:var>${version} [${privateLabelName}]</gts:var>
  =======================================================================================
-->

<!-- Head -->
<head>
  <gts:var>
  <!-- meta tags -->
  <meta name="author" content="GeoTelematic Solutions, Inc."/>
  <meta http-equiv="content-type" content='text/html; charset=UTF-8'/>
  <meta http-equiv="cache-control" content='no-cache'/>
  <meta http-equiv="pragma" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta name="copyright" content="${copyright}"/>
  <meta name="robots" content="none"/>

  <!-- page title -->
  <title>${pageTitle}</title>
  </gts:var>
</head>

<!-- Frame -->
<frameset cols="1" rows="1" frameborder="0" framespacing="0" border="0" bordercolor="0">
    <gts:var>
    <frame name="GTSContent" src="${loginURL}"
       scrolling="auto" frameborder="no" noresize="noresize" 
       marginheight="0" marginwidth="0" />
    <noframes> 
       <!-- -->
       <body>
         Frames are not supported by this browser.<br/>
         Please continue to the login page by clicking on the following link:<br/>
         <br/>
         <a href="${loginURL}" target="_top">Continue to GPS Tracking Login</a>
       </body> 
       <!-- -->
    </noframes>
    </gts:var>
</frameset>
<!-- -->

</html>
