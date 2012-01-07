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
package org.apache.james.pop3server.core;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;
import org.apache.james.protocols.lib.POP3BeforeSMTPHelper;
import org.apache.james.protocols.pop3.POP3Response;
import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.core.PassCmdHandler;
import org.apache.james.protocols.pop3.mailbox.MailboxFactory;

/**
 * {@link PassCmdHandler} which also handles POP3 Before SMTP
 * 
 */
public class JamesPassCmdHandler implements CommandHandler<POP3Session> {

    private PassCmdHandler handler;

    public JamesPassCmdHandler() {
    }

    @Resource(name = "mailboxfactory")
    public void setMailboxFactory(MailboxFactory factory) {
        this.handler = new PassCmdHandler(factory);
    }

    public Response onCommand(POP3Session session, Request request) {
        Response response =  handler.onCommand(session, request);
        if (POP3Response.OK_RESPONSE.equals(response.getRetCode())) {
            POP3BeforeSMTPHelper.addIPAddress(session.getRemoteAddress().getAddress().toString());
        }
        return response;
    }

    @Override
    public Collection<String> getImplCommands() {
        return handler.getImplCommands();
    }

}
