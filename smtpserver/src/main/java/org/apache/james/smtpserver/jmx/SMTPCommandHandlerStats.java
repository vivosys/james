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
package org.apache.james.smtpserver.jmx;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.james.lifecycle.Disposable;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.socket.AbstractCommandHandlerStats;

/**
 * Expose statistics for {@link CommandHandler} via JMX
 *
 */
public class SMTPCommandHandlerStats extends AbstractCommandHandlerStats<SMTPResponse> implements SMTPCommandHandlerStatsMBean, Disposable {

    private AtomicLong temp = new AtomicLong(0);
    private AtomicLong perm = new AtomicLong(0);
    private AtomicLong ok  = new AtomicLong(0);
 
    public SMTPCommandHandlerStats(String jmxPath, String handlerName, String[] commands) throws NotCompliantMBeanException, MalformedObjectNameException, NullPointerException, InstanceAlreadyExistsException, MBeanRegistrationException {
        super(SMTPCommandHandlerStatsMBean.class, jmxPath, handlerName, commands);
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.CommandHandlerStatsMBean#getTemporaryError()
     */
    public long getTemporaryError() {
        return temp.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.CommandHandlerStatsMBean#getPermantError()
     */
    public long getPermantError() {
        return perm.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.CommandHandlerStatsMBean#getOk()
     */
    public long getOk() {
        return ok.get();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.AbstractCommandHandlerStats#incrementStats(org.apache.james.protocols.api.Response)
     */
    protected void incrementStats(SMTPResponse response) {
        try {
            String code = response.getRetCode();
            char c = code.charAt(0) ;
            if (c == '5') {
                perm.incrementAndGet();
            } else if (c == '4') {
                temp.incrementAndGet();
            } else if ( c == '2' || c == '3') {
                ok.incrementAndGet();
            } 
            
        } catch (NumberFormatException e) {
            // should never happen
        }        
    }

}
