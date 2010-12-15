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
package org.apache.james.vut.jpa;

import java.util.HashMap;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.vut.jpa.JPAVirtualUserTable;
import org.apache.james.vut.jpa.model.JPAVirtualUser;
import org.apache.james.vut.lib.AbstractVirtualUserTable;
import org.apache.james.vut.lib.AbstractVirtualUserTableTest;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * Test the JPA Virtual User Table implementation.
 */
public class JPAVirtualUserTableTest extends AbstractVirtualUserTableTest {
    
    /**
     * The OpenJPA Entity Manager used for the tests.
     */
    private OpenJPAEntityManagerFactory factory;

    /**
     * The properties for the OpenJPA Entity Manager.
     */
    private HashMap<String, String> properties;
    
    @Override
    protected void setUp() throws Exception {

        // Use a memory database.
        properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.put("openjpa.ConnectionURL", "jdbc:h2:target/users/db");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=" + JPAVirtualUser.class.getName() +")");
        
        factory = OpenJPAPersistence.getEntityManagerFactory(properties);

        super.setUp();
        
    }

    /**
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#getVirtualUserTable()
     */
    protected AbstractVirtualUserTable getVirtualUserTable() throws Exception {
        JPAVirtualUserTable virtualUserTable = new JPAVirtualUserTable();
        virtualUserTable.setLog(new SimpleLog("MockLog"));
        virtualUserTable.setEntityManagerFactory(factory);
        DefaultConfigurationBuilder defaultConfiguration = new DefaultConfigurationBuilder();
        virtualUserTable.configure(defaultConfiguration);
        return virtualUserTable;
    }    
    
    /**
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#addMapping(java.lang.String, java.lang.String, java.lang.String, int)
     */
    protected boolean addMapping(String user, String domain, String mapping, int type) {
        if (type == ERROR_TYPE) {
            return virtualUserTable.addErrorMapping(user, domain, mapping);
        } else if (type == REGEX_TYPE) {
            return virtualUserTable.addRegexMapping(user, domain, mapping);
        } else if (type == ADDRESS_TYPE) {
            return virtualUserTable.addAddressMapping(user, domain, mapping);
        } else if (type == ALIASDOMAIN_TYPE) {
            return virtualUserTable.addAliasDomainMapping(domain, mapping);
        } else {
            return false;
        }
    }

    /**
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#removeMapping(java.lang.String, java.lang.String, java.lang.String, int)
     */
    protected boolean removeMapping(String user, String domain, String mapping, int type) {
        if (type == ERROR_TYPE) {
            return virtualUserTable.removeErrorMapping(user, domain, mapping);
        } else if (type == REGEX_TYPE) {
            return virtualUserTable.removeRegexMapping(user, domain, mapping);
        } else if (type == ADDRESS_TYPE) {
            return virtualUserTable.removeAddressMapping(user, domain, mapping);
        } else if (type == ALIASDOMAIN_TYPE) {
            return virtualUserTable.removeAliasDomainMapping(domain, mapping);
        } else {
            return false;
        }
    }
    
}
