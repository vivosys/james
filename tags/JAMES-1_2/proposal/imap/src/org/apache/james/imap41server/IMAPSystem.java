/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;


/**
 * Interface for objects representing for an IMAP4rev1 messaging system. There
 * should be one instance of this class per instance of James.
 * <p>  An IMAP messaging system may span more than one server.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author Charles Benett <charles@benett1.demon.co.uk>
 * @version 0.1
 */

public interface IMAPSystem {


    public static final String IMAP_SYSTEM = "IMAP_SYSTEM";


    /**
     * Returns the token indicating a namespace.  Implementation dependent but
     * by convention, '#'.
     * Example: #news.org.apache vs #mail.org.apache
     */
    public String getNamespaceToken();


    /**
     * Returns the home server (server with user's INBOX) for specified user.
     * Enables Login Referrals per RFC2221. (Ie user attempts to login to a
     * server which is not their Home Server.)  The returned string must comply
     * with RFC2192, IMAP URL Scheme.
     *
     * @param username String representation of a user
     * @returns String holding an IMAP URL for the user's home server
     * @throws AuthenticationException if this System does not recognise
     * the user.
     */
    public String getHomeServer(String username)
	throws AuthenticationException;


    /**
     * Returns the character used as a mail hierarchy seperator in a given
     * namespace. A namespace must use the same seperator at all levels of
     * hierarchy.
     * <p>Recommendations (from rfc 2683) are period (US)/ full stop (Brit),
     * forward slash or backslash.
     *
     * @param namespace String identifying a namespace
     * @returns char, usually '.', '/', or '\'
     */
    public String getHierarchySeperator(String namespace);


    /**
     * Provides the set of namesapces a given user can access. Implementations
     * should but are not required to reveal all namespaces that a user can
     * access. Different namespaces may be handled by different
     * <code>IMAPHosts</code>
     *
     * @param username String identifying a user of this System
     * @returns String whose contents should be a space seperated triple 
     * <personal namespaces(s)> space <other users' namespace(s)> space
     * <shared namespace(s)>, per RFC2342
     */
    public String getNamespaces(String username);


    /**
     * Return UIDValidity for named mailbox. Implementations should track
     * existing and deleted folders.
     */
    public int getUIDValidity(String mailbox);


    /**
     * Returns an iterator over the collection of servers on which this user
     * has access. The collection should be unmodifiable.
     * Enable Mailbox Referrals - RFC 2193.
     *
     * @param username String identifying a user
     * @return iterator over a collection of strings
     */
    public Iterator getAccessibleServers(String username);

}
