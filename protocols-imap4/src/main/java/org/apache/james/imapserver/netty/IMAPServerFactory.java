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
package org.apache.james.imapserver.netty;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.main.DefaultImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.encode.main.DefaultLocalizer;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.*;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.SimpleMailboxMetaData;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer;
import org.apache.james.protocols.lib.netty.AbstractServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

public class IMAPServerFactory extends AbstractServerFactory {

    private FileSystem fileSystem;
    private ImapDecoder decoder;
    private ImapEncoder encoder;
    private ImapProcessor processor;


    @Resource(name = "filesystem")
    public final void setFileSystem(FileSystem filesystem) {
        this.fileSystem = filesystem;
    }

    @Resource(name = "imapDecoder")
    public void setImapDecoder(ImapDecoder decoder) {
        this.decoder = decoder;
    }

    @Resource(name = "imapEncoder")
    public void setImapEncoder(ImapEncoder encoder) {
        this.encoder = encoder;
    }

    @Resource(name = "imapProcessor")
    public void setImapProcessor(ImapProcessor processor) {
        this.processor = processor;
    }

    protected IMAPServer createServer() {
        return new IMAPServer();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<AbstractConfigurableAsyncServer> createServers(Logger log, HierarchicalConfiguration config) throws Exception {
        List<AbstractConfigurableAsyncServer> servers = new ArrayList<AbstractConfigurableAsyncServer>();
        List<HierarchicalConfiguration> configs = config.configurationsAt("imapserver");

        for (HierarchicalConfiguration serverConfig : configs) {
            IMAPServer server = createServer();
            server.setLog(log);
            server.setFileSystem(fileSystem);
            server.setImapDecoder(decoder);
            server.setImapEncoder(encoder);
            server.setImapProcessor(processor);
            server.configure(serverConfig);
            servers.add(server);
        }

        return servers;
    }

    public static void main(String[] args) throws Exception {

        IMAPServerFactory f = new IMAPServerFactory();
        IMAPServer s = f.createServer();
        s.setLog(LoggerFactory.getLogger("Foo"));
        final HierarchicalConfiguration config = new HierarchicalConfiguration();
        config.setProperty("bind", "0.0.0.0:1443");
        s.configure(config);
        final DefaultImapDecoder imapDecoder = (DefaultImapDecoder) DefaultImapDecoderFactory.createDecoder();
        s.setImapDecoder(imapDecoder);
        s.setImapProcessor(DefaultImapProcessorFactory.createDefaultProcessor(new MailboxManager() {
                @Override
                public char getDelimiter() {
                    return 0;
                }

                @Override
                public MessageManager getMailbox(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
                    return new StoreMessageManager<>();
                }

                @Override
                public void createMailbox(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
                }

                @Override
                public void deleteMailbox(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
                }

                @Override
                public void renameMailbox(MailboxPath mailboxPath, MailboxPath mailboxPath1, MailboxSession mailboxSession)
                    throws MailboxException {
                }

                @Override
                public List<MessageRange> copyMessages(MessageRange longs, MailboxPath mailboxPath, MailboxPath mailboxPath1,
                    MailboxSession mailboxSession) throws MailboxException {
                    return null;
                }

                @Override
                public List<MailboxMetaData> search(MailboxQuery mailboxQuery, MailboxSession mailboxSession)
                    throws MailboxException {
                    List<MailboxMetaData> metaData = new ArrayList<MailboxMetaData>();
                    metaData.add(new SimpleMailboxMetaData(new MailboxPath(".", "raman", "path"), '.'));
                    return metaData;
                }

                @Override
                public boolean mailboxExists(MailboxPath mailboxPath, MailboxSession mailboxSession) throws MailboxException {
                    return false;
                }

                @Override
                public MailboxSession createSystemSession(String s, Logger logger)
                    throws BadCredentialsException, MailboxException {
                    return null;
                }

                @Override
                public MailboxSession login(String s, String s1, Logger logger) throws BadCredentialsException, MailboxException {
                    System.out.printf("Doing login s = %s, s1 = %s, ", s, s1);
                    return new MailboxSession() {
                        @Override
                        public SessionType getType() {
                            return null;
                        }

                        @Override
                        public long getSessionId() {
                            return 0;
                        }

                        @Override
                        public boolean isOpen() {
                            return false;
                        }

                        @Override
                        public void close() {
                        }

                        @Override
                        public Logger getLog() {
                            return null;
                        }

                        @Override
                        public User getUser() {
                            return new User() {
                                @Override
                                public String getUserName() {
                                    return "raman";
                                }

                                @Override
                                public String getPassword() {
                                    return "raman";
                                }

                                @Override
                                public List<Locale> getLocalePreferences() {
                                    return null;
                                }
                            };
                        }

                        @Override
                        public String getPersonalSpace() {
                            return null;
                        }

                        @Override
                        public String getOtherUsersSpace() {
                            return null;
                        }

                        @Override
                        public Collection<String> getSharedSpaces() {
                            return null;
                        }

                        @Override
                        public Map<Object, Object> getAttributes() {
                            return null;
                        }

                        @Override
                        public char getPathDelimiter() {
                            return 0;
                        }
                    };
                }

                @Override
                public void logout(MailboxSession mailboxSession, boolean b) throws MailboxException {
                }

                @Override
                public List<MailboxPath> list(MailboxSession mailboxSession) throws MailboxException {
                    return null;
                }

                @Override
                public void addListener(MailboxPath mailboxPath, MailboxListener mailboxListener, MailboxSession mailboxSession)
                    throws MailboxException {
                }

                @Override
                public void removeListener(MailboxPath mailboxPath, MailboxListener mailboxListener,
                    MailboxSession mailboxSession) throws MailboxException {
                }

                @Override
                public void addGlobalListener(MailboxListener mailboxListener, MailboxSession mailboxSession)
                    throws MailboxException {
                }

                @Override
                public void removeGlobalListener(MailboxListener mailboxListener, MailboxSession mailboxSession)
                    throws MailboxException {
                }

                @Override
                public void startProcessingRequest(MailboxSession mailboxSession) {
                }

                @Override
                public void endProcessingRequest(MailboxSession mailboxSession) {
                }
            }, new SubscriptionManager() {
                @Override
                public void subscribe(MailboxSession mailboxSession, String s) throws SubscriptionException {
                }

                @Override
                public Collection<String> subscriptions(MailboxSession mailboxSession) throws SubscriptionException {
                    List<String> s = new ArrayList<String>();
                    s.add("box1");
                    s.add("box2");
                    s.add("box3");
                    return s;
                }

                @Override
                public void unsubscribe(MailboxSession mailboxSession, String s) throws SubscriptionException {
                }

                @Override
                public void startProcessingRequest(MailboxSession mailboxSession) {
                }

                @Override
                public void endProcessingRequest(MailboxSession mailboxSession) {
                }
            }
        ));
        s.setImapEncoder(DefaultImapEncoderFactory.createDefaultEncoder(new DefaultLocalizer(), true));

        s.init();

    }


}
