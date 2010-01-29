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

package org.apache.james.socket.mina.filter;

import org.apache.james.api.protocol.Response;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

public abstract class AbstractResponseFilter extends IoFilterAdapter {

    public final void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Object obj = writeRequest.getMessage();
        if (obj != null && obj instanceof Response) {

            Response response = (Response) writeRequest.getMessage();

            processResponse(nextFilter, session, response);

            if (response.isEndSession()) {
                session.close(false);
            }

        } else {
            super.filterWrite(nextFilter, session, writeRequest);
        }

    }

    /**
     * Process the response
     * 
     * @param nextFilter
     * @param session
     * @param response
     */
    protected abstract void processResponse(NextFilter nextFilter, IoSession session, Response response);

    /**
     * Write the response to the client
     * 
     * @param nextFilter
     * @param session
     * @param response
     */
    protected void writeResponse(NextFilter nextFilter, IoSession session, String response) {
        nextFilter.filterWrite(session, new DefaultWriteRequest(response));
    }

}