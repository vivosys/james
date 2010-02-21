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

package org.apache.james.transport.camel;

import org.apache.camel.impl.DefaultMessage;
import org.apache.mailet.Mail;

/**
 * Message for Mails
 *
 */
public class MailMessage extends DefaultMessage{
    public final static String STATE = "mailstate";

    private Mail mail;
    public MailMessage(Mail mail) {
        this.mail = mail;
    }
    
    
    @Override
    protected Object createBody() {
        return mail;
    }


    @Override
    public Object getHeader(String name) {
        if (STATE.equals(name)) {
            return mail.getState();
        }
        return super.getHeader(name);
    }

    
}