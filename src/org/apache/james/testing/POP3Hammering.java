package org.apache.james.testing;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

/**
 * Program that can be run multiple times to recreate the
 * "stuck file" issue in Windows.
 *
 * @author Prasanna Uppaladadium <prasanna@vayusphere.com>
 */
public class POP3Hammering {

    private String mailHost;
    private String user;
    private String password;
    private Properties prop = new Properties();

    private static final String body = "Test message number: ";

    private int iter;

    public JamesTest(String host, String user, String password) {
		this.mailHost = host;
		this.user = user;
		this.password = password;
		iter = 0;
		prop.put("java.smtp.host", mailHost);
    }

    void sendMail() {
		try {
			Session session = Session.getDefaultInstance(prop, null);
			// Transport transport = session.getTransport("smtp");
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("prasanna@localhost"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress("prasanna@localhost"));
			msg.setContent(body + ++iter, "text/plain");
			Transport.send(msg);
			// transport.close();
			System.out.println("Sent message : " + msg.getContent() +
			  " from: " + msg.getFrom()[0] + " To: " +
			  msg.getAllRecipients()[0]);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(0);
		}
    }

    void receiveMail(boolean delete) {
		try {
			Session session = Session.getDefaultInstance(prop, null);
			Store store = session.getStore("pop3");
			store.connect(mailHost, user, password);

			Folder folder = store.getFolder("INBOX");

			if(folder == null || !folder.exists()) {
				System.out.println("This folder does not exist.");
				return;
			}

			folder.open(Folder.READ_WRITE);

			Message[] msgs = folder.getMessages();
			System.out.println("Received " + msgs.length + " messages for " + user);
			Message msg = msgs[0];
			System.out.println("From: " + msg.getFrom()[0].toString());
			System.out.println("To: " + msg.getRecipients(Message.RecipientType.TO)[0]);
			System.out.println("-------------------");
			System.out.println(msg.getContent().toString());

			if(delete) {
				msg.setFlag(Flags.Flag.DELETED, true);
				System.out.println("Deleted.");
			}
			folder.close(true);
			store.close();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }

    public static void main(String[] args) throws Throwable {
		JamesTest jt = new JamesTest(args[0], args[1], args[2]);
		jt.sendMail();
		jt.sendMail();

		jt.receiveMail(true);
		jt.receiveMail(true);
    }
}
