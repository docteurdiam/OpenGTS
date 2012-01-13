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
//  2009/06/01  Martin D. Flynn
//     -Extracted from SendMail
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.activation.*; // since Java 6

import javax.mail.*;
import javax.mail.internet.*;

public class SendMailArgs
{

    // ------------------------------------------------------------------------

    public static final boolean USE_AUTHENTICATOR   = true;

    public static final String SSL_FACTORY          = "javax.net.ssl.SSLSocketFactory";

    // ------------------------------------------------------------------------

    /** 
    *** Filters and returns the base email address from the specified String.<br>
    *** For example, if the String "Jones&lt;jones@example.com&gt;" is passed to this
    *** method, then the value "jones@example.com" will be returned.
    *** @param addr The email address to filter.
    *** @return  The filtered email address, or null if the specified email address is invalid.
    **/
    public static String parseEMailAddress(String addr)
    {
        if (addr != null) {
            try {
                InternetAddress ia = new InternetAddress(addr, true);
                return ia.getAddress();
            } catch (Throwable ae) { // AddressException
                // drop through
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Internal method to send email
    *** @param args  The email arguments
    *** @return True if the email was sent, false otherwise
    **/
    public static boolean send(SendMail.Args args)
    {
        String from        = args.getFrom();
        String to[]        = args.getTo();
        String cc[]        = args.getCc();
        String bcc[]       = args.getBcc();
        String subject     = args.getSubject();
        String msgBody     = args.getBody();
        Properties headers = args.getHeaders();
        SendMail.Attachment attach  = args.getAttachment();

        /* SMTP properties */
        // http://www.j2ee.me/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html
        // mail.smtp.host (String)
        // mail.smtp.port (int)
        // mail.smtp.user (String)
        // mail.smtp.auth (boolean)
        // mail.smtp.connectiontimeout (int)  [miliseconds]
        // mail.smtp.timeout (int)  [miliseconds]
        // mail.smtp.socketFactory.class (String)
        // mail.smtp.socketFactory.port (int)
        // mail.smtp.socketFactory.fallback (boolean)
        // mail.smtp.starttls.enable (boolean)
        // mail.smtp.sendpartial (boolean)
        Properties props = new Properties();

        // Debug
        if (RTConfig.getBoolean(RTKey.SMTP_DEBUG)) {
            props.put("mail.debug", "true");
            Print.logDebug("SendMail debug mode");
        }

        // SMTP host:port
        final String smtpHost = RTConfig.getString(RTKey.SMTP_SERVER_HOST);
        final int    smtpPort = RTConfig.getInt(RTKey.SMTP_SERVER_PORT,25);
        if (StringTools.isBlank(smtpHost) || smtpHost.endsWith("example.com")) {
            Print.logError("Null/Invalid SMTP host, not sending email");
            return false;
        } else
        if (smtpPort <= 0) {
            Print.logError("Invalid SMTP port, not sending email");
            return false;
        }
        props.put("mail.smtp.host"                          , smtpHost);
        props.put("mail.smtp.port"                          , String.valueOf(smtpPort));
        props.put("mail.smtp.connectiontimeout"             , "60000");
        props.put("mail.smtp.timeout"                       , "60000");
      //props.put("mail.smtp.auth"                          , "true");
      //props.put("mail.smtp.auth.mechanisms"               , "LOGIN PLAIN DIGEST-MD5 NTLM");
      
        // The following can be used as a replacement for the value returned by
        // "InetAddress.getLocalHost().getHostName()".
      //props.put("mail.smtp.localhost"                     , "mydomain.example.com");

        // SSL
        String enableSSL = RTConfig.getString(RTKey.SMTP_ENABLE_SSL,"false").toLowerCase();
        if (enableSSL.equals("only") || enableSSL.equals("true")) {
            props.put("mail.smtp.socketFactory.port"        , String.valueOf(smtpPort));
            props.put("mail.smtp.socketFactory.class"       , SSL_FACTORY);
            props.put("mail.smtp.socketFactory.fallback"    , "false");
          //props.put("mail.smtp.socketFactory.fallback"    , "true");
            if (enableSSL.equals("only")) {
                props.put("mail.smtp.ssl.enable"            , "true");
                props.put("mail.smtp.ssl.socketFactory.port", String.valueOf(smtpPort));
            }
        }
        
        // TLS
        String enableTLS = RTConfig.getString(RTKey.SMTP_ENABLE_TLS,"false").toLowerCase();
        if (enableTLS.equals("only") || enableTLS.equals("true")) {
            props.put("mail.smtp.starttls.required"         , "true");
            props.put("mail.smtp.starttls.enable"           , "true");
        }

        /* SMTP Authenticator */
        javax.mail.Authenticator auth = null;
        final String smtpUser = StringTools.trim(RTConfig.getString(RTKey.SMTP_SERVER_USER));
        final String smtpPass = RTConfig.getString(RTKey.SMTP_SERVER_PASSWORD);
        if (USE_AUTHENTICATOR && !StringTools.isBlank(smtpUser)) {
            auth = new javax.mail.Authenticator() {
                public javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(smtpUser, smtpPass);
                }
            };
            props.put("mail.smtp.user", smtpUser);
            props.put("mail.smtp.auth", "true"); // SSL
        }

        /* SMTP Session */
        //props.list(System.out);
        Session session = Session.getInstance(props, auth);
        
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            
            InternetAddress toAddr[]  = _convertRecipients(to);
            InternetAddress ccAddr[]  = _convertRecipients(cc);
            InternetAddress bccAddr[] = _convertRecipients(bcc);
            if ((toAddr != null) && (toAddr.length > 0)) {
                for (Iterator i = headers.keySet().iterator(); i.hasNext();) {
                    String k = (String)i.next();
                    String v = headers.getProperty(k);
                    if (v != null) {
                        msg.setHeader(k, v);
                    }
                }
                msg.setRecipients(Message.RecipientType.TO , toAddr);
                msg.setRecipients(Message.RecipientType.CC , ccAddr);
                msg.setRecipients(Message.RecipientType.BCC, bccAddr);
                msg.setSubject(subject);
                msg.setSentDate(new Date());
                if ((attach != null) && (attach.getSize() > 0)) {
                    Multipart multipart = new MimeMultipart();
                    if ((msgBody != null) && !msgBody.equals("")) {
                        BodyPart textBodyPart = new MimeBodyPart();
                        textBodyPart.setText(msgBody);
                        multipart.addBodyPart(textBodyPart);
                    }
                    // add attachment
                    BodyPart attachBodyPart = new MimeBodyPart();
                    DataSource source = new ByteArrayDataSource(attach.getName(), attach.getType(), attach.getBytes());
                    attachBodyPart.setDataHandler(new DataHandler(source));
                    attachBodyPart.setFileName(source.getName());
                    multipart.addBodyPart(attachBodyPart);
                    // set content 
                    msg.setContent(multipart);
                } else {
                    msg.setText(msgBody); // setContent(msgBody, CONTENT_TYPE_PLAIN);
                }

                /* send email */
                msg.saveChanges(); // implicit with send()
                if (!USE_AUTHENTICATOR && !StringTools.isBlank(smtpUser)) {
                    Transport transport = session.getTransport("smtp");
                    transport.connect(smtpHost, smtpUser, (smtpPass!=null?smtpPass:""));
                    transport.sendMessage(msg, msg.getAllRecipients());
                    transport.close();
                } else {
                    Transport.send(msg);
                }
                Print.logDebug("Email sent ...");

                return true;
            } else {
                return false;
            }

        } catch (MessagingException me) {
            
            Print.logStackTrace("Unable to send email [host="+smtpHost+"; port="+smtpPort+"]", me);
            for (Exception ex = me; ex != null;) {
                if (ex instanceof SendFailedException) {
                    SendFailedException sfex = (SendFailedException)ex;
                    _printAddresses("Invalid:"     , sfex.getInvalidAddresses());
                    _printAddresses("Valid Unsent:", sfex.getValidUnsentAddresses());
                    _printAddresses("Valid Sent:"  , sfex.getValidSentAddresses());
                }
                ex = (ex instanceof MessagingException)? ((MessagingException)ex).getNextException() : null;
            }

            return false;

        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the list of String email addresses to instances of 'InternetAddress'
    *** @param to  The array of email addresses
    *** @return An array of InternetAddress instances
    *** @throws AddressException if any of the specified email addresses are invalid
    **/
    private static InternetAddress[] _convertRecipients(String to[])
        throws AddressException
    {
        java.util.List<InternetAddress> inetAddr = new Vector<InternetAddress>();
        for (int i = 0; i < to.length; i++) {
            String t = (to[i] != null)? to[i].trim() : "";
            if (!t.equals("")) { 
                try {
                    inetAddr.add(new InternetAddress(t)); 
                } catch (AddressException ae) {
                    Print.logStackTrace("Address: " + t + " (skipped)", ae);
                }
            }
        }
        return inetAddr.toArray(new InternetAddress[inetAddr.size()]);
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the list of email addresses (debug purposes only)
    **/
    private static void _printAddresses(String msg, Address addr[])
    {
        if (addr != null) {
            Print.logInfo(msg);
            for (int i = 0; i < addr.length; i++) {
                Print.logInfo("    " + addr[i]);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** ByteArrayDataSource class
    **/
    private static class ByteArrayDataSource
        implements DataSource
    {
        private String name   = null;
        private String type   = null;
        private Object source = null;
        private ByteArrayDataSource(String name, String type, Object src) {
            this.name   = name;
            this.type   = type;
            this.source = src;
        }
        public ByteArrayDataSource(String name, byte src[]) {
            this(name, null, src);
        }
        public ByteArrayDataSource(String name, String type, byte src[]) {
            this(name, type, (Object)src);
        }
        public ByteArrayDataSource(String name, String src) {
            this(name, null, src);
        }
        public ByteArrayDataSource(String name, String type, String src) {
            this(name, type, (Object)src);
        }
        public String getName() {
            return (this.name != null)? this.name : "";
        }
        public String getContentType() {
            if (this.type != null) {
                return this.type;
            } else 
            if (this.getName().toLowerCase().endsWith(".csv")) {
                return HTMLTools.MIME_CSV();
            } else 
            if (this.getName().toLowerCase().endsWith(".gif")) {
                return HTMLTools.MIME_GIF();
            } else 
            if (this.getName().toLowerCase().endsWith(".png")) {
                return HTMLTools.MIME_PNG();
            } else
            if (this.source instanceof byte[]) {
                return SendMail.DefaultContentType((byte[])this.source);
            } else
            if (this.source instanceof ByteArrayOutputStream) {
                return SendMail.DefaultContentType(((ByteArrayOutputStream)this.source).toByteArray());
            } else {
                return HTMLTools.MIME_PLAIN();
            }
        }
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.toByteArray());
        }
        public OutputStream getOutputStream() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte b[] = this.toByteArray();
            if ((b != null) && (b.length > 0)) {
                out.write(b, 0, b.length);
            }
            this.source = out;
            return (ByteArrayOutputStream)this.source;
        }
        private byte[] toByteArray() {
            if (this.source == null) {
                return new byte[0];
            } else
            if (this.source instanceof byte[]) {
                return (byte[])this.source;
            } else
            if (this.source instanceof ByteArrayOutputStream) {
                return ((ByteArrayOutputStream)this.source).toByteArray();
            } else {
                return StringTools.getBytes(this.source.toString());
            }
        }
    }                               

}
