/*
 * Copyright 2004-2006 Swen Kummer, Dustin Hass, Sven Jost, Grzegorz Grasza
 * modified by Yuan-Chu Tai
 * http://jxa.sourceforge.net/
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. Mobber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with mobber; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package net.sourceforge.jxa;

import javax.microedition.io.*;
import java.io.*;
import java.util.*;

/**
 * J2ME XMPP API Class
 * 
 * @author Swen Kummer, Dustin Hass, Sven Jost, Grzegorz Grasza
 * @version 4.0
 * @since 1.0
 */

public class Jxa extends Thread {

  final static boolean DEBUG = true;
        
	private final String host, port, username, password, myjid, server;
	private final boolean use_ssl;
	private String resource;
	private final int priority;

	private XmlReader reader;
	private XmlWriter writer;
	private InputStream is;
	private OutputStream os;

	private Vector listeners = new Vector();

	/**
	 * If you create this object all variables will be saved and the
	 * method {@link #run()} is started to log in on jabber server and
	 * listen to parse incomming xml stanzas. Use
	 * {@link #addListener(XmppListener xl)} to listen to events of this object.
	 * 
	 * @param host the hostname/ip of the jabber server
	 * @param port the port number of the jabber server
	 * @param username the username of the jabber account
	 * @param password the passwort of the jabber account
	 * @param resource a unique identifier of the used resource, for e.g. "mobile"
	 * @param priority the priority of the jabber session, defines on which
 	 * resource the messages arrive
	 */
/*	public Jxa(final String host, final String port, final String username, final String password, final String resource, final int priority) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.resource = resource;
		this.priority = priority;
		this.myjid = username + "@" + host;
		this.early_jabber = true;
		this.server = host;
		this.start();	
	}*/
	// jid must in the form "username@host"
	// to login Google Talk, set port to 5223 (NOT 5222 in their offical guide)	public Jxa(final String jid, final String password, final String resource, final int priority, final String server, final String port, final boolean use_ssl) {
		int i = jid.indexOf('@');
		this.host = jid.substring(i+1);
		this.port = port;
		this.username = jid.substring(0, i);
		this.password = password;
		this.resource = resource;
		this.priority = priority;
		this.myjid = jid;
		if (server == null)
			this.server = host;
		else
			this.server = server;
		this.use_ssl = use_ssl;
		//this.start();
	}

	/**
	 * The <code>run</code> method is called when {@link Jxa} object is
	 * created. It sets up the reader and writer, calls {@link #login()}
	 * methode and listens on the reader to parse incomming xml stanzas.
	 */
	public void run() {		
		try {
			if (!use_ssl) {
				final StreamConnection connection = (StreamConnection) Connector.open("socket://" + this.host + ":" + this.port);
				this.reader = new XmlReader(connection.openInputStream());
				this.writer = new XmlWriter(connection.openOutputStream());
			} else {
				final SecureConnection sc = (SecureConnection)Connector.open("ssl://" + this.server + ":" + this.port, Connector.READ_WRITE);
				//sc.setSocketOption(SocketConnection.DELAY, 1);
				//sc.setSocketOption(SocketConnection.LINGER, 0);
				is = sc.openInputStream();
				os = sc.openOutputStream();
				this.reader = new XmlReader(is);
				this.writer = new XmlWriter(os);
			}
		} catch (final Exception e) {
			java.lang.System.out.println(e);
			this.connectionFailed(e.toString());
			return;
		}
		
		java.lang.System.out.println("connected");
		/*for (Enumeration enu = listeners.elements(); enu.hasMoreElements();) {
      XmppListener xl = (XmppListener) enu.nextElement();
	    xl.onDebug("connected");
    }*/	
			
		// connected
		try {
			this.login();
			this.parse();
			//java.lang.System.out.println("done");
		} catch (final Exception e) {
			//e.printStackTrace();
			/*for (Enumeration enu = listeners.elements(); enu.hasMoreElements();) {
        XmppListener xl = (XmppListener) enu.nextElement();
        xl.onDebug(e.getMessage());
      }*/
      /*try {
          this.writer.close();
          this.reader.close();
      } catch (final IOException io) {
          io.printStackTrace();
      }*/			// hier entsteht der connection failed bug (Network Down)
			this.connectionFailed(e.toString());
		}
	}

	/**
	 * Add a {@link XmppListener} to listen for events.
	 *
	 * @param xl a XmppListener object
	 */
	public void addListener(final XmppListener xl) {
		if(!listeners.contains(xl)) listeners.addElement(xl);
	}

	/**
	 * Remove a {@link XmppListener} from this class.
	 *
	 * @param xl a XmppListener object
	 */
	public void removeListener(final XmppListener xl) {
		listeners.removeElement(xl);
	}

	/**
	 * Opens the connection with a stream-tag, queries authentication type and
	 * sends authentication data, which is username, password and resource.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void login() throws IOException {
		if (!use_ssl) {
			// start stream
			this.writer.startTag("stream:stream");
			this.writer.attribute("to", this.host);
			this.writer.attribute("xmlns", "jabber:client");
			this.writer.attribute("xmlns:stream", "http://etherx.jabber.org/streams");
			this.writer.flush();
			// log in
			this.writer.startTag("iq");
			this.writer.attribute("type", "set");
			this.writer.attribute("id", "auth");
			this.writer.startTag("query");
			this.writer.attribute("xmlns", "jabber:iq:auth");

			this.writer.startTag("username");
			this.writer.text(this.username);
			this.writer.endTag();
			this.writer.startTag("password");
			this.writer.text(this.password);
			this.writer.endTag();
			this.writer.startTag("resource");
			this.writer.text(this.resource);
			this.writer.endTag();

			this.writer.endTag(); // query
			this.writer.endTag(); // iq
			this.writer.flush();
		} else {
			String msg = "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' to='" + this.host + "' version='1.0'>";
			os.write(msg.getBytes());
			os.flush();
			do {
				reader.next();
			} while ((reader.getType() != XmlReader.END_TAG) || (!reader.getName().equals("stream:features")));

			java.lang.System.out.println("SASL phase1");
			/*for (Enumeration enu = listeners.elements(); enu.hasMoreElements();) {
	      XmppListener xl = (XmppListener) enu.nextElement();
		    xl.onDebug("SASL phase 1");
    	}*/	
    	
    	//int ghost = is.available();
    	//is.skip(ghost);
    	
			msg = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>";
			byte[] auth_msg = (username + "@" + host + "\0" + username + "\0" + password).getBytes();
			msg = msg + Base64.encode(auth_msg) + "</auth>";
			os.write(msg.getBytes());
			os.flush();			
			reader.next();
			if (reader.getName().equals("success")) {
				while (true) {			
						if ((reader.getType() == XmlReader.END_TAG) && reader.getName().equals("success")) break;
						reader.next();
				}
			} else {
				for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
					XmppListener xl = (XmppListener) e.nextElement();
					xl.onAuthFailed(reader.getName() + ", failed authentication");
				}
				return;
			}
			java.lang.System.out.println("SASL phase2");
			/*for (Enumeration enu = listeners.elements(); enu.hasMoreElements();) {
		    XmppListener xl = (XmppListener) enu.nextElement();
			  xl.onDebug("SASL phase 2");
	    }*/		
			msg = "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' to='" + this.host + "' version='1.0'>";
			os.write(msg.getBytes());
			os.flush();
			reader.next();
			while (true) {			
					if ((reader.getType() == XmlReader.END_TAG)  && reader.getName().equals("stream:features")) break;
					reader.next();
			}
			java.lang.System.out.println("SASL done");
			/*for (Enumeration enu = listeners.elements(); enu.hasMoreElements();) {
		    XmppListener xl = (XmppListener) enu.nextElement();
			  xl.onDebug("SASL done");
			}	*/	
			if (resource == null) 
				msg = "<iq type='set' id='res_binding'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/></iq>";
			else 
				msg = "<iq type='set' id='res_binding'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'><resource>" + resource + "</resource></bind></iq>";
			os.write(msg.getBytes());
			os.flush();
		}		
	}

	/**
	 * Closes the stream-tag and the {@link XmlWriter}.
	 */
	public void logoff() {
		try {
			this.writer.endTag();
			this.writer.flush();
			this.writer.close();
		} catch (final Exception e) {
			this.connectionFailed();
		}
	}

	/**
	 * Sends a message text to a known jid.
	 * 
	 * @param to the JID of the recipient
	 * @param msg the message itself
	 */
	public void sendMessage(final String to, final String msg) {
		try {
			this.writer.startTag("message");
			this.writer.attribute("type", "chat");
			this.writer.attribute("to", to);
			this.writer.startTag("body");
			this.writer.text(msg);
			this.writer.endTag();
			this.writer.endTag();
			this.writer.flush();
		} catch (final Exception e) {
			// e.printStackTrace();
			this.connectionFailed();
		}
	}

	/**
	 * Sends a presence stanza to a jid. This method can do various task but
	 * it's private, please use setStatus to set your status or explicit
         * subscription methods subscribe, unsubscribe, subscribed and
	 * unsubscribed to change subscriptions.
	 */
	private void sendPresence(final String to, final String type, final String show, final String status, final int priority) {
		try {
			this.writer.startTag("presence");
			if (type != null) {
				this.writer.attribute("type", type);
			}
			if (to != null) {
				this.writer.attribute("to", to);
			}
			if (show != null) {
				this.writer.startTag("show");
				this.writer.text(show);
				this.writer.endTag();
			}
			if (status != null) {
				this.writer.startTag("status");
				this.writer.text(status);
				this.writer.endTag();
			}
			if (priority != 0) {
				this.writer.startTag("priority");
				this.writer.text(Integer.toString(priority));
				this.writer.endTag();
			}
			this.writer.endTag(); // presence
			this.writer.flush();
		} catch (final Exception e) {
			// e.printStackTrace();
			this.connectionFailed();
		}
	}

	/**
	 * Sets your Jabber Status.
	 * 
	 * @param show is one of the following: <code>null</code>, chat, away,
	 *        dnd, xa, invisible
	 * @param status an extended text describing the actual status
	 * @param priority the priority number (5 should be default)
	 */
	public void setStatus(String show, String status, final int priority) {
		if (show.equals("")) {
			show = null;
		}
		if (status.equals("")) {
			status = null;
		}
		if (show.equals("invisible")) {
			this.sendPresence(null, "invisible", null, null, priority);
		} else {
			this.sendPresence(null, null, show, status, priority);
		}
	}

	/**
	 * Requesting a subscription.
	 * 
	 * @param to the jid you want to subscribe
	 */
	public void subscribe(final String to) {
		this.sendPresence(to, "subscribe", null, null, 0);
	}

	/**
	 * Remove a subscription.
	 * 
	 * @param to the jid you want to remove your subscription
	 */
	public void unsubscribe(final String to) {
		this.sendPresence(to, "unsubscribe", null, null, 0);
	}

	/**
	 * Approve a subscription request.
	 * 
	 * @param to the jid that sent you a subscription request
	 */
	public void subscribed(final String to) {
		this.sendPresence(to, "subscribed", null, null, 0);
	}

	/**
	 * Refuse/Reject a subscription request.
	 * 
	 * @param to the jid that sent you a subscription request
	 */
	public void unsubscribed(final String to) {
		this.sendPresence(to, "unsubscribed", null, null, 0);
	}

	/**
	 * Save a contact to roster. This means, a message is send to jabber
	 * server (which hosts your roster) to update the roster.
	 * 
	 * @param jid the jid of the contact
	 * @param name the nickname of the contact
	 * @param group the group of the contact
	 * @param subscription the subscription of the contact
	 */
	public void saveContact(final String jid, final String name, final Enumeration group, final String subscription) {
		try {
			this.writer.startTag("iq");
			this.writer.attribute("type", "set");
			this.writer.startTag("query");
			this.writer.attribute("xmlns", "jabber:iq:roster");
			this.writer.startTag("item");
			this.writer.attribute("jid", jid);
			if (name != null) {
				this.writer.attribute("name", name);
			}
			if (subscription != null) {
				this.writer.attribute("subscription", subscription);
			}
			if (group != null) {
				while (group.hasMoreElements()) {
					this.writer.startTag("group");
					this.writer.text((String) group.nextElement());
					this.writer.endTag(); // group
				}
			}
			this.writer.endTag(); // item
			this.writer.endTag(); // query
			this.writer.endTag(); // iq
			this.writer.flush();
		} catch (final Exception e) {
			// e.printStackTrace();
			this.connectionFailed();
		}
	}

	/**
	 * Sends a roster query.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void getRoster() throws IOException {
		this.writer.startTag("iq");
		this.writer.attribute("id", "roster");
		this.writer.attribute("type", "get");
		this.writer.startTag("query");
		this.writer.attribute("xmlns", "jabber:iq:roster");
		this.writer.endTag(); // query
		this.writer.endTag(); // iq
		this.writer.flush();
	}

	/**
	 * The main parse methode is parsing all types of XML stanzas
	 * <code>message</code>, <code>presence</code> and <code>iq</code>.
	 * Although ignores any other type of xml.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private void parse() throws IOException {
		if (DEBUG) java.lang.System.out.println("*debug* parsing");
		if (!use_ssl)
			this.reader.next(); // start tag
		while (this.reader.next() == XmlReader.START_TAG) {
			final String tmp = this.reader.getName();
			if (tmp.equals("message")) {
				this.parseMessage();
			} else if (tmp.equals("presence")) {
				this.parsePresence();
			} else if (tmp.equals("iq")) {
				this.parseIq();
			} else {
				this.parseIgnore();
			}
		}
		//java.lang.System.out.println("leave parse() " + reader.getName());
		this.reader.close();
	}

	/**
	 * This method parses all info/query stanzas, including authentication
	 * mechanism and roster. It also answers version queries.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private void parseIq() throws IOException {
		if (DEBUG) java.lang.System.out.println("*debug* paeseIq");
		String type = this.reader.getAttribute("type");
		final String id = this.reader.getAttribute("id");
		final String from = this.reader.getAttribute("from");
		if (type.equals("error")) {
			while (this.reader.next() == XmlReader.START_TAG) {
				// String name = reader.getName();
				if (this.reader.getName().equals("error")) {
					final String code = this.reader.getAttribute("code");
					for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
						XmppListener xl = (XmppListener) e.nextElement();
						xl.onAuthFailed(code + ": " + this.parseText());
					}
				} else {
					this.parseText();
				}
			}
		} else if (type.equals("result") && (id != null) && id.equals("res_binding")) {
			// authorized
			while (true) {				
				reader.next();				
				String tagname = reader.getName();
				if (tagname != null) {
					if ((reader.getType() == XmlReader.START_TAG) && tagname.equals("jid")) {
						reader.next();
						String rsp_jid = reader.getText();
						int i = rsp_jid.indexOf('/');
						this.resource = rsp_jid.substring(i+1);
						//java.lang.System.out.println(this.resource);
					} else if (tagname.equals("iq")) 
						break;
				}
			}
      for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
				XmppListener xl = (XmppListener) e.nextElement();
				xl.onAuth(this.resource);
			}
			this.sendPresence(null, null, null, null, this.priority);
		} else {
			//java.lang.System.out.println("contacts list");
			while (this.reader.next() == XmlReader.START_TAG) {
				if (this.reader.getName().equals("query")) {
					if (this.reader.getAttribute("xmlns").equals("jabber:iq:roster")) {
						while (this.reader.next() == XmlReader.START_TAG) {
							if (this.reader.getName().equals("item")) {
								type = this.reader.getAttribute("type");
                String jid = reader.getAttribute("jid");
                String name = reader.getAttribute("name");
                String subscription = reader.getAttribute("subscription");
                //newjid = (jid.indexOf('/') == -1) ? jid : jid.substring(0, jid.indexOf('/'));
								boolean check = true;
								//yctai
                /*for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
                	XmppListener xl = (XmppListener) e.nextElement();
                  xl.onContactRemoveEvent(newjid);
                }*/
								while (this.reader.next() == XmlReader.START_TAG) {
									if (this.reader.getName().equals("group")) {
                    for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
                      XmppListener xl = (XmppListener) e.nextElement();
                      xl.onContactEvent(jid, name, this.parseText(), subscription);
                    }
										check = false;
									} else {
										this.parseIgnore();
									}
								}
								//if (check && !subscription.equals("remove"))
								if (check) {
                  for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
                    XmppListener xl = (XmppListener) e.nextElement();
                    xl.onContactEvent(jid, name, "", subscription);
                  }
								}
							} else {
								this.parseIgnore();
							}
						}
						for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
		          XmppListener xl = (XmppListener) e.nextElement();
		          xl.onContactOverEvent();
            }
					} else if (this.reader.getAttribute("xmlns").equals("jabber:iq:version")) {
						while (this.reader.next() == XmlReader.START_TAG) {
							this.parseIgnore();
						}
						// reader.next();
						// send version
						this.writer.startTag("iq");
						this.writer.attribute("type", "result");
						this.writer.attribute("id", id);
						this.writer.attribute("to", from);
						this.writer.startTag("query");
						this.writer.attribute("xmlns", "jabber:iq:version");

						this.writer.startTag("name");
						this.writer.text("jxa");
						this.writer.endTag();
						this.writer.startTag("version");
						writer.text("1.0");
						this.writer.endTag();
						this.writer.startTag("os");
						this.writer.text("J2ME");
						this.writer.endTag();

						this.writer.endTag(); // query
						this.writer.endTag(); // iq
					} else {
						this.parseIgnore();
					}
				} else {
					this.parseIgnore();
				}
			}
		}
	}

	/**
	 * This method parses all presence stanzas, including subscription requests.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private void parsePresence() throws IOException {
		final String from = this.reader.getAttribute("from"), type = this.reader.getAttribute("type");
		String status = "", show = "";
		// int priority=-1;
		while (this.reader.next() == XmlReader.START_TAG) {
			final String tmp = this.reader.getName();
			if (tmp.equals("status")) {
				status = this.parseText();
			} else if (tmp.equals("show")) {
				show = this.parseText();
				// else if(tmp.equals("priority"))
				// priority = Integer.parseInt(parseText());
			} else {
				this.parseIgnore();
			}
		}

		if (DEBUG) java.lang.System.out.println("*debug* from,type,status,show:" + from + "," + type + "," + status + "," + show);

		//if ((type != null) && (type.equals("unavailable") || type.equals("unsubscribed") || type.equals("error"))) {
		if (type == null) {
			for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
		      XmppListener xl = (XmppListener) e.nextElement();		      xl.onStatusEvent(from, show, status);
		   }
		} else {
			if (type.equals("unsubscribed") || type.equals("error")) {
				for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
		      XmppListener xl = (XmppListener) e.nextElement();
		      xl.onUnsubscribeEvent(from);
				}
			} else if (type.equals("subscribe")) {
		    for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
		      XmppListener xl = (XmppListener) e.nextElement();
		      xl.onSubscribeEvent(from);
		    }
			} else if (type.equals("unavailable")) {
				//final String jid = (from.indexOf('/') == -1) ? from : from.substring(0, from.indexOf('/'));
		    for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
		      XmppListener xl = (XmppListener) e.nextElement();
		      //xl.onStatusEvent(jid, show, status);
		      xl.onStatusEvent(from, "na", status);
		    }
			}
		}
	}

	/**
	 * This method parses all incoming messages.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private void parseMessage() throws IOException {
		final String from = this.reader.getAttribute("from"), type = this.reader.getAttribute("type");
		String body = null, subject = null;
		while (this.reader.next() == XmlReader.START_TAG) {
			final String tmp = this.reader.getName();
			if (tmp.equals("body")) {
				body = this.parseText();
			} else if (tmp.equals("subject")) {
				subject = this.parseText();
			} else {
				this.parseIgnore();
			}
		}
		// (from, subject, body);
                for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
                        XmppListener xl = (XmppListener) e.nextElement();
                        xl.onMessageEvent((from.indexOf('/') == -1) ? from : from.substring(0, from.indexOf('/')), body);
                }
	}

	/**
	 * This method parses all text inside of xml start and end tags.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private String parseText() throws IOException {
		final String endTagName = this.reader.getName();
		final StringBuffer str = new StringBuffer("");
		int t = this.reader.next(); // omit start tag
		while (!endTagName.equals(this.reader.getName())) {
			if (t == XmlReader.TEXT) {
				str.append(this.reader.getText());
			}
			t = this.reader.next();
		}
		return str.toString();
	}

	/**
	 * This method doesn't parse tags it only let the reader go through unknown
	 * tags.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private void parseIgnore() throws IOException {
		int x;
		while ((x = this.reader.next()) != XmlReader.END_TAG) {
			if (x == XmlReader.START_TAG) {
				this.parseIgnore();
			}
		}
	}

	/**
	 * This method is used to be called on a parser or a connection error.
         * It tries to close the XML-Reader and XML-Writer one last time.
         *
	 */
	private void connectionFailed() {
    this.writer.close();
    this.reader.close();

		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
      XmppListener xl = (XmppListener) e.nextElement();
      xl.onConnFailed("");
		}
	}
	private void connectionFailed(final String msg) {
    this.writer.close();
    this.reader.close();

		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
      XmppListener xl = (XmppListener) e.nextElement();
      xl.onConnFailed(msg);
		}
	}

};
