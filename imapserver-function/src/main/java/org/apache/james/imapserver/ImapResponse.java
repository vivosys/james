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

package org.apache.james.imapserver;

import java.io.OutputStream;
import java.io.PrintWriter;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.util.InternetPrintWriter;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ImapResponse  extends AbstractLogEnabled implements ImapConstants {
    
    private PrintWriter writer;
    private String tag = UNTAGGED;

    public ImapResponse( OutputStream output )
    {
        this.writer = new InternetPrintWriter( output, true );
    }

    public void setTag( String tag )
    {
        this.tag = tag;
    }

    /**
     * Writes a standard tagged OK response on completion of a command.
     * Response is writen as:
     * <pre>     a01 OK COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     */
    public void commandComplete( ImapCommand command )
    {
        commandComplete( command, null );
    }

    /**
     * Writes a standard tagged OK response on completion of a command,
     * with a response code (eg READ-WRITE)
     * Response is writen as:
     * <pre>     a01 OK [responseCode] COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     * @param responseCode A string response code to send to the client.
     */
    public void commandComplete( ImapCommand command, String responseCode )
    {
        tag();
        message( OK );
        responseCode( responseCode );
        commandName( command );
        message( "completed." );
        end();
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 NO COMMAND_NAME failed. <reason></pre>
     *
     * @param command The ImapCommand which failed.
     * @param reason A message describing why the command failed.
     */
    public void commandFailed( ImapCommand command, String reason )
    {
        commandFailed( command, null, reason );
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 NO [responseCode] COMMAND_NAME failed. <reason></pre>
     *
     * @param command The ImapCommand which failed.
     * @param responseCode The Imap response code to send.
     * @param reason A message describing why the command failed.
     */
    public void commandFailed( ImapCommand command,
                               String responseCode,
                               String reason )
    {
        tag();
        message( NO );
        responseCode( responseCode );
        commandName( command );
        message( "failed." );
        message( reason );
        end();
        final Logger logger = getLogger();
        if (logger.isInfoEnabled()) {
            logger.info("COMMAND FAILED [" + responseCode + "] - " + reason);
        }
    }

    /**
     * Writes a standard BAD response on command error, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 BAD <message></pre>
     *
     * @param message The descriptive error message.
     */
    public void commandError( String message )
    {
        tag();
        message( BAD );
        message( message );
        end();
        final Logger logger = getLogger();
        if (logger.isInfoEnabled()) {
            logger.info("ERROR - " + message); 
        }
    }

    /**
     * Writes a standard untagged BAD response, together with a descriptive message.
     */
    public void badResponse( String message )
    {
        untagged();
        message( BAD );
        message( message );
        end();
        final Logger logger = getLogger(); 
        if (logger.isInfoEnabled()) { 
            logger.info("BAD - " + message); 
        }
    }

    /**
     * Writes an untagged OK response, with the supplied response code,
     * and an optional message.
     * @param responseCode The response code, included in [].
     * @param message The message to follow the []
     */
    public void okResponse( String responseCode, String message )
    {
        untagged();
        message( OK );
        responseCode( responseCode );
        message( message );
        end();
    }

    public void flagsResponse( Flags flags )
    {
        untagged();
        message( "FLAGS" );
        message( MessageFlags.format(flags) );
        end();
    }

    public void existsResponse( int count )
    {
        untagged();
        message( count );
        message( "EXISTS" );
        end();
    }

    public void recentResponse( int count )
    {
        untagged();
        message( count );
        message( "RECENT" );
        end();
    }

    public void expungeResponse( int msn )
    {
        untagged();
        message( msn );
        message( "EXPUNGE" );
        end();
    }

    public void fetchResponse( int msn, String msgData )
    {
        untagged();
        message( msn );
        message( "FETCH" );
        message( "(" + msgData + ")" );
        end();
    }

    public void commandResponse( ImapCommand command, String message )
    {
        untagged();
        commandName( command );
        message( message );
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * request tag.
     *
     * @param message The message to write to the client.
     */
    public void taggedResponse( String message )
    {
        tag();
        message( message );
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * untagged marker "*".
     *
     * @param message The message to write to the client.
     */
    public void untaggedResponse( String message )
    {
        untagged();
        message( message );
        end();
    }
    
    public void byeResponse( String message ) {
        untaggedResponse(BYE + SP + message);
    }

    private void untagged()
    {
        writer.print( UNTAGGED );
    }

    private void tag()
    {
        writer.print( tag );
    }

    private void commandName( ImapCommand command )
    {
        String name = command.getName();
        writer.print( SP );
        writer.print( name );
    }

    private void message( String message )
    {
        if ( message != null ) {
            writer.print( SP );
            writer.print( message );
        }
    }

    private void message( int number )
    {
        writer.print( SP );
        writer.print( number );
    }

    private void responseCode( String responseCode )
    {
        if ( responseCode != null ) {
            writer.print( " [" );
            writer.print( responseCode );
            writer.print( "]" );
        }
    }

    private void end()
    {
        writer.println();
        writer.flush();
    }

    public void permanentFlagsResponse(Flags flags) {
        untagged();
        message(OK);
        responseCode("PERMANENTFLAGS " + MessageFlags.format(flags));
        end();
    }
}