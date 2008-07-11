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

package org.apache.james.imapserver.handler.session;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.commons.collections.IteratorUtils;
import org.apache.james.imapserver.ImapRequestHandler;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionImpl;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.TestConstants;
import org.apache.james.imapserver.client.Command;
import org.apache.james.imapserver.mock.MockImapHandler;
import org.apache.james.imapserver.mock.MockImapHandlerConfigurationData;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.MessageRangeImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.manager.MailboxExpression;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.test.mock.avalon.MockLogger;
import org.jmock.MockObjectTestCase;

public abstract class AbstractSessionTest extends MockObjectTestCase implements TestConstants {
    
    MailboxManager mailboxManager;
    
    private ImapRequestHandler handler;
    private ImapSession session;
    private MailboxSession mailboxSession;
    
    int counter=0;
    
    public AbstractSessionTest() {
    }
    
    public void setUp() throws Exception
    {
        
        MockImapHandlerConfigurationData theConfigData = new MockImapHandlerConfigurationData();
        MockImapHandlerConfigurationData.reset();
        session = new ImapSessionImpl(theConfigData.getMailboxManagerProvider(),
                theConfigData.getUsersRepository(), new MockImapHandler(),
                HOST_NAME, HOST_ADDRESS);
        ((LogEnabled)session).enableLogging(new MockLogger());
        handler = new ImapRequestHandler();
        handler.enableLogging(new MockLogger());
        mailboxManager=theConfigData.getMailboxManagerProvider().getMailboxManager();
        mailboxSession = mailboxManager.createSession();
    }
    
    void createFolders(String[] folders) throws MailboxManagerException {
        for (int i=0; i<folders.length; i++) {
            mailboxManager.createMailbox(folders[i]);
        }
    }
    
    public void appendMessagesClosed(String folder,MimeMessage[] msgs) throws MailboxManagerException, MessagingException {
        Mailbox mailbox=getMailbox(folder);
        for (int i = 0; i < msgs.length; i++) {
            msgs[i].setFlags(new Flags(Flags.Flag.RECENT), true);
            mailbox.appendMessage(msgs[i],new Date(),FetchGroupImpl.MINIMAL, mailboxSession);
            
        }
    }

    public long[] addUIDMessagesOpen(String folder,MimeMessage[] msgs) throws MailboxManagerException, MessagingException {
        Mailbox mailbox=getMailbox(folder);
        long[] uids=new long[msgs.length];
        for (int i = 0; i < msgs.length; i++) {
            msgs[i].setFlags(new Flags(Flags.Flag.RECENT), false);
            uids[i]=mailbox.appendMessage(msgs[i],new Date(),FetchGroupImpl.MINIMAL, mailboxSession).getUid();
        }
        return uids;
    }
    public long getUidValidity(String folder) throws MailboxManagerException {
        Mailbox mailbox=getMailbox(folder);
        long uidv=mailbox.getUidValidity(mailboxSession);
        return uidv;
    }
    
    
    public long getUidNext(String folder) throws MailboxManagerException {
        Mailbox mailbox=getMailbox(folder);
        long uidNext=mailbox.getUidNext(mailboxSession);
        return uidNext;
    }
    
    public MimeMessage[] getMessages(String folder) throws MailboxManagerException {
        Mailbox mailbox=getMailbox(folder);
        Iterator iterator =mailbox.getMessages(MessageRangeImpl.all(),FetchGroupImpl.MIME_MESSAGE, mailboxSession);
        List messages = IteratorUtils.toList(iterator);
        MimeMessage[] mms=new MimeMessage[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            mms[i]=((MessageResult) messages.get(i)).getMimeMessage();
        }
        return mms;
    }
    
    public long[] getUids(String folder) throws MailboxManagerException {
        Mailbox mailbox=getMailbox(folder);
        Iterator iterator = mailbox.getMessages(MessageRangeImpl.all(),FetchGroupImpl.MINIMAL, mailboxSession);
        List messages = IteratorUtils.toList(iterator);
        long[] uids=new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            uids[i]=((MessageResult)messages.get(i)).getUid();
        }
        return uids;
    }
    
    public boolean folderExists(String folder) throws MailboxManagerException {
        return mailboxManager.existsMailbox(folder);
    }
    
    public String[] getFolderNames() throws MailboxManagerException {
        ListResult[] listResults=mailboxManager.list(new MailboxExpression("","*", '*', '%'));
        String[] names=new String[listResults.length];
        for (int i = 0; i < listResults.length; i++) {
            names[i]=listResults[i].getName();
        }
        return names;
    }
    
    public void verifyFolderList(String[] expected,String[] found) {
        Set expectedSet=new HashSet(Arrays.asList(expected));
        Set foundSet=new HashSet(Arrays.asList(found));
        Set missing=new HashSet(expectedSet);
        missing.removeAll(foundSet);
        Set notExpected=new HashSet(foundSet);
        notExpected.removeAll(expectedSet);
        assertEquals("Missing folders :"+missing,0,missing.size());
        assertEquals("Not expected folders :"+notExpected,0,notExpected.size());
    }
    
    public boolean isOpen(String folder)  throws MessagingException {
        // TODO implement this
        return false;
    }
    public void useFolder(String folder) {
        
    }
    public void freeFolder(String folder) {
        
    }
    
    public void deleteAll(String folder) throws MailboxManagerException {
        Mailbox mailbox=getMailbox(folder);
        mailbox.setFlags(new Flags(Flag.DELETED),true,false,MessageRangeImpl.all(), FetchGroupImpl.MINIMAL, mailboxSession);
        mailbox.expunge(MessageRangeImpl.all(),FetchGroupImpl.MINIMAL, mailboxSession);
    }
    public BufferedReader handleRequestReader(String s) throws ProtocolException
    {
        return new BufferedReader(new StringReader(handleRequest(s)));
    }

    public String handleRequest(String s) throws ProtocolException
    {
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        //System.out.println("IN :" + s);
        handler.handleRequest(is, os, session);
        String out = os.toString();
        //System.out.println("OUT:" + out);
        return out;
        
    }
    protected void verify(final String command, final Set requiredResponseSet, final String requiredStatusResponse) throws ProtocolException, IOException, MessagingException {
        Command c=new Command() {
            public List getExpectedResponseList() throws MessagingException, IOException {
                return new LinkedList(requiredResponseSet);
            }
            public String getExpectedStatusResponse() {
                return requiredStatusResponse;
            }
            public String getCommand() {
                return command;
            }
        };
        verifyCommand(c);
    }
    
    protected void verifyCommand(Command command) throws ProtocolException, IOException, MessagingException {
        
        BufferedReader br=handleRequestReader((++counter)+" "+command.getCommand()+"\n");
        Set requiredResponseSet=new HashSet(command.getExpectedResponseList());
        List rec=new ArrayList();
        String line;
        while ((line=br.readLine())!=null) {
             rec.add(line);
        }
        assertFalse("nothing received",rec.isEmpty());
        String statusResponse=(String)rec.get(rec.size()-1);
        rec.remove(rec.size()-1);
        Set responseSet=new HashSet(rec);
        responseSet.removeAll(requiredResponseSet);
        requiredResponseSet.removeAll(new HashSet(rec));
//        if (responseSet.size()>0) {
//            System.out.println("Not awaitet responses: "+responseSet);
//        }
//        if (requiredResponseSet.size()>0) {
//            System.out.println("Missed responses: "+requiredResponseSet);
//        }
        assertEquals("Missed responses: "+requiredResponseSet,0,requiredResponseSet.size());
        assertEquals("Not awaitet responses: "+responseSet,0,responseSet.size());
        assertEquals("Status respons differs",counter+" "+command.getExpectedStatusResponse(),statusResponse);
    }
    
    protected void  verifyCommandOrdered(Command command)  throws ProtocolException, IOException, MessagingException
    {
        BufferedReader br=handleRequestReader((++counter)+" "+command.getCommand()+"\n");
        
        int c=0;
        for (Iterator it = command.getExpectedResponseList().iterator(); it.hasNext();) {
            c++;
            String expectedResponse = (String) it.next();
//          System.out.println("Expected: "+expectedResponse);
            String[] expectedLines=expectedResponse.split("\r\n");
            for (int i = 0; i < expectedLines.length; i++) {
                String readLine=br.readLine();
                assertNotNull("Unexpected end of response (response "+c+" line "+i+")",readLine);
                assertEquals("Unexpected response line (response "+c+" line "+i+")",expectedLines[i],readLine);             
            }
        }
        
        String statusResponse=(String)br.readLine();;
        assertEquals("Status response differs",counter+" "+command.getExpectedStatusResponse(),statusResponse);
        String notExpected=br.readLine();
        assertNull("did expect eof",notExpected);
    }
    
    public void setFlags(String mailboxName,long fromUid,long toUid,Flags flags, boolean value, boolean replace) throws MailboxManagerException {
        Mailbox mailbox=getMailbox(mailboxName);
        mailbox.setFlags(flags, value, replace, MessageRangeImpl.uidRange(fromUid, toUid), FetchGroupImpl.MINIMAL, mailboxSession);
    }
    private Mailbox getMailbox(String mailboxName) throws MailboxManagerException {
        int[] neededSets = new int[] {MessageRange.TYPE_UID};
        int neededResults= FetchGroup.MIME_MESSAGE + FetchGroup.FLAGS;
        Mailbox mailboxSession= mailboxManager.getMailbox(mailboxName, false);
        return mailboxSession;
    }
}