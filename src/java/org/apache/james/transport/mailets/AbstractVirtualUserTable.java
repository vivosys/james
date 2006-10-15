/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james.transport.mailets;

import org.apache.james.core.MailImpl;
import org.apache.james.util.VirtualUserTableUtil;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Provides an abstraction of common functionality needed for implementing
 * a Virtual User Table. Override the <code>mapRecipients</code> method to
 * map virtual recipients to real recipients.
 */
public abstract class AbstractVirtualUserTable extends GenericMailet
{
    static private final String MARKER = "org.apache.james.transport.mailets.AbstractVirtualUserTable.mapped";

    /**
     * Checks the recipient list of the email for user mappings.  Maps recipients as
     * appropriate, modifying the recipient list of the mail and sends mail to any new
     * non-local recipients.
     *
     * @param mail the mail to process
     */
    public void service(Mail mail) throws MessagingException
    {
        if (mail.getAttribute(MARKER) != null) {
            mail.removeAttribute(MARKER);
            return;
        }

        Collection recipientsToRemove = new HashSet();
        Collection recipientsToAddLocal = new ArrayList();
        Collection recipientsToAddForward = new ArrayList();

        Collection recipients = mail.getRecipients();
        Map recipientsMap = new HashMap(recipients.size());

        for (Iterator iter = recipients.iterator(); iter.hasNext(); ) {
            MailAddress address = (MailAddress)iter.next();

            // Assume all addresses are non-virtual at start
            recipientsMap.put(address, null);
        }

        mapRecipients(recipientsMap);

        for (Iterator iter = recipientsMap.keySet().iterator(); iter.hasNext(); ) {
            MailAddress source = (MailAddress)iter.next();
            String targetString = (String)recipientsMap.get(source);

            // Only non-null mappings are translated
            if(targetString != null) {
                if (targetString.startsWith("error:")) {
                    //Mark this source address as an address to remove from the recipient list
                    recipientsToRemove.add(source);
                    processDSN(mail, source, targetString);
                } else {
                    StringTokenizer tokenizer = new StringTokenizer(targetString, getSeparator(targetString));

                    while (tokenizer.hasMoreTokens()) {
                        String targetAddress = tokenizer.nextToken().trim();

                        // log("Attempting to map from " + source + " to " + targetAddress);

                        if (targetAddress.startsWith("regex:")) {
                            try {
                                targetAddress = VirtualUserTableUtil.regexMap(source, targetAddress);
                            } catch (MalformedPatternException e) {
                                log("Exception during regexMap processing: ", e);
                            }
                            if (targetAddress == null) continue;
                        }

                        try {
                            MailAddress target = (targetAddress.indexOf('@') < 0) ? new MailAddress(targetAddress, "localhost")
                                : new MailAddress(targetAddress);

                            //Mark this source address as an address to remove from the recipient list
                            recipientsToRemove.add(source);

                            // We need to separate local and remote
                            // recipients.  This is explained below.
                            if (getMailetContext().isLocalServer(target.getHost())) {
                                recipientsToAddLocal.add(target);
                            } else {
                                recipientsToAddForward.add(target);
                            }

                            StringBuffer buf = new StringBuffer().append("Translating virtual user ")
                                                                 .append(source)
                                                                 .append(" to ")
                                                                 .append(target);
                            log(buf.toString());

                        } catch (ParseException pe) {
                            //Don't map this address... there's an invalid address mapping here
                            StringBuffer exceptionBuffer =
                                new StringBuffer(128)
                                .append("There is an invalid map from ")
                                .append(source)
                                .append(" to ")
                                .append(targetAddress);
                            log(exceptionBuffer.toString());
                            continue;
                        }
                    }
                }
            }
        }

        // Remove mapped recipients
        recipients.removeAll(recipientsToRemove);

        // Add mapped recipients that are local
        recipients.addAll(recipientsToAddLocal);

        // We consider an address that we map to be, by definition, a
        // local address.  Therefore if we mapped to a remote address,
        // then we want to make sure that the mail can be relayed.
        // However, the original e-mail would typically be subjected to
        // relay testing.  By posting a new mail back through the
        // system, we have a locally generated mail, which will not be
        // subjected to relay testing.

        // Forward to mapped recipients that are remote
        if (recipientsToAddForward.size() != 0) {
            // Can't use this ... some mappings could lead to an infinite loop
            // getMailetContext().sendMail(mail.getSender(), recipientsToAddForward, mail.getMessage());

            // duplicates the Mail object, to be able to modify the new mail keeping the original untouched
            MailImpl newMail = new MailImpl(mail);
            try {
                try {
                    newMail.setRemoteAddr(java.net.InetAddress.getLocalHost().getHostAddress());
                    newMail.setRemoteHost(java.net.InetAddress.getLocalHost().getHostName());
                } catch (java.net.UnknownHostException _) {
                    newMail.setRemoteAddr("127.0.0.1");
                    newMail.setRemoteHost("localhost");
                }
                newMail.setRecipients(recipientsToAddForward);
                newMail.setAttribute(MARKER, Boolean.TRUE);
                getMailetContext().sendMail(newMail);
            } finally {
                newMail.dispose();
            }
        }

        // If there are no recipients left, Ghost the message
        if (recipients.size() == 0) {
            mail.setState(Mail.GHOST);
        }
    }

    /**
     * Override to map virtual recipients to real recipients, both local and non-local.
     * Each key in the provided map corresponds to a potential virtual recipient, stored as
     * a <code>MailAddress</code> object.
     * 
     * Translate virtual recipients to real recipients by mapping a string containing the
     * address of the real recipient as a value to a key. Leave the value <code>null<code>
     * if no mapping should be performed. Multiple recipients may be specified by delineating
     * the mapped string with commas, semi-colons or colons.
     * 
     * @param recipientsMap the mapping of virtual to real recipients, as 
     *    <code>MailAddress</code>es to <code>String</code>s.
     */
    protected abstract void mapRecipients(Map recipientsMap) throws MessagingException;
  
    /**
     * Sends the message for DSN processing
     *
     * @param mail the Mail instance being processed
     * @param address the MailAddress causing the DSN
     * @param error a String in the form "error:<code> <msg>"
     */
    private void processDSN(Mail mail, MailAddress address, String error) {
        // parse "error:<code> <msg>"
      int msgPos = error.indexOf(' ');
      try {
          Integer code = Integer.valueOf(error.substring("error:".length(),msgPos));
      } catch (NumberFormatException e) {
          log("Cannot send DSN.  Exception parsing DSN code from: " + error, e);
          return;
      }
      String msg = error.substring(msgPos + 1);
      // process bounce for "source" address
      try {
          getMailetContext().bounce(mail, error);
      }
      catch (MessagingException me) {
          log("Cannot send DSN.  Exception during DSN processing: ", me);
      }
  }

  /**
   * Returns the character used to delineate multiple addresses.
   * 
   * @param targetString the string to parse
   * @return the character to tokenize on
   */
  private String getSeparator(String targetString) {
      return (targetString.indexOf(',') > -1 ? "," : (targetString.indexOf(';') > -1 ? ";" : (targetString.indexOf("regex:") > -1? "" : ":" )));
  }

}
