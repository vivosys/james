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



package org.apache.james.mailrepository.file;

import org.apache.james.core.MimeMessageSource;
import org.apache.james.repository.StreamRepository;

import java.io.IOException;
import java.io.InputStream;

public class MimeMessageStreamRepositorySource extends MimeMessageSource {

    //Define how to get to the data
    
    /**
     * The stream repository used by this data source.
     */
    StreamRepository sr = null;

    /**
     * The name of the repository
     */
    String repositoryName = null;

    /**
     * The key for the particular stream in the stream repository
     * to be used by this data source.
     */
    String key = null;

    private long size = -1;

    /**
     * Main constructor
     * 
     * @param sr The stream repository used by this data source.
     * @param repositoryName The name of the repository
     * @param key The key for the particular stream in the stream repository to be used by this data source.
     */
    public MimeMessageStreamRepositorySource(StreamRepository sr, String repositoryName, String key) {
        super();

        this.sr = sr;
        this.repositoryName = repositoryName;
        this.key = key;
    }

    /**
     * Returns a unique String ID that represents the location from where 
     * this source is loaded.  This will be used to identify where the data 
     * is, primarily to avoid situations where this data would get overwritten.
     *
     * @return the String ID
     */
    public String getSourceId() {
        StringBuffer sourceIdBuffer =
            new StringBuffer(128)
                    .append(repositoryName)
                    .append("/")
                    .append(key);
        return sourceIdBuffer.toString();
    }

    /**
     * Return the InputStream
     * 
     * @return stream return the InputStream
     */
    public InputStream getInputStream() throws IOException {
        return sr.get(key);
    }

    /**
     * @see org.apache.james.core.MimeMessageSource#getMessageSize()
     */
    public long getMessageSize() throws IOException {
        if (size == -1) {
            if (sr instanceof org.apache.james.filepair.FilePersistentStreamRepository) {
                size = ((org.apache.james.filepair.FilePersistentStreamRepository) sr).getSize(key);
            } else size = super.getMessageSize();
        }
        return size;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.core.MimeMessageSource#disposeSource()
     */
    protected void disposeSource() {
        // nothing todo here
    }
}
