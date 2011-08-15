/*
 * Copyright (C) 2010-2011 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms.connector.discotel;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * AsyncTask to manage IO to discotel.com API.
 * 
 * @author flx
 */
public class ConnectorDiscotel extends Connector {
	/** Tag for output. */
	private static final String TAG = "discotel";

	/** Discoplus URL: login. */
	private static final String URL_DP_LOGIN = // .
	"https://service.discoplus.de/frei/LOGIN";
	/** Destination for login. */
	private static final String LOGIN_DP_DEST = "/discoplus/index3.php";
	/** Discoplus URL: send. */
	private static final String URL_DP_SEND = // .
	"https://service.discoplus.de/discoplus/sms-neu.php";

	/** Check for login. */
	private static final String CHECK_DP_LOGIN = "sms-neu.php";
	/** Check for balance. */
	private static final String CHECK_DP_BALANCE1 = "prepaid Guthaben:";
	/** Check for balance. */
	private static final String CHECK_DP_BALANCE2 = " kostenlose SMS ";
	/** Check for sent. */
	private static final String CHECK_DP_SENT = "Ihre SMS wurde versendet";

	/** Discotel URL: login. */
	private static final String URL_DT_LOGIN = // .
	"https://lounge.discoplus.de/login";
	/** Discotel URL: send. */
	private static final String URL_DT_SEND = // .
	"https://lounge.discoplus.de/sms_service";
	/** Check for login. */
	private static final String CHECK_DT_LOGIN = "Ihre Kundennummer lautet";
	/** Check for balance. */
	private static final String CHECK_DT_BALANCE1 = "nnen in diesem Monat noch";
	/** Check for sent. */
	private static final String CHECK_DT_SENT = // .
	"Ihre SMS wurde erfolgreich versendet";
	/** Max length for discotel sms. */
	private static final int MAXLENGTH = 101;

	/** Number of vars pushed at login. */
	private static final int NUM_VARS_LOGIN = 4;
	/** Number of vars pushed at send. */
	private static final int NUM_VARS_SEND = 6;

	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U; "
			+ "Windows NT 5.1; ko; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3 "
			+ "(.NET CLR 3.5.30729)";

	/** Used encoding. */
	private static final String ENCODING = "ISO-8859-15";

	/** Trusted SSL certificates. */
	private static final String[] TRUSTED_CERTS = { // .
	"d6:5c:29:a8:68:dd:c7:72:a1:11:af:5d:6a:84:ad:fc:4f:f0:5b:dc" };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_discotel_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
				context.getString(R.string.connector_discotel_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("discotel", c.getName(),
				SubConnectorSpec.FEATURE_NONE);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Login to service.discoplus.de.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @throws IOException
	 *             IOException
	 */
	private void dpDoLogin(final Context context, // .
			final ConnectorCommand command) throws IOException {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);

		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(NUM_VARS_LOGIN);
		postData.add(new BasicNameValuePair("destination", LOGIN_DP_DEST));
		String genlogin = Utils.getSender(context, command.getDefSender());
		Log.d(TAG, "genlogin:  " + genlogin);
		genlogin = Utils.international2national(command.getDefPrefix(),
				genlogin);
		Log.d(TAG, "genlogin:  " + genlogin);
		postData.add(new BasicNameValuePair("credential_0", genlogin));
		Log.d(TAG, "genlogin:  " + genlogin);
		postData.add(new BasicNameValuePair("credential_1", p.getString(
				Preferences.PREFS_PASSWORD, "")));

		HttpResponse response = Utils.getHttpClient(URL_DP_LOGIN, null, // .
				postData, TARGET_AGENT, null, ENCODING, Preferences
						.getTrustAll(p), TRUSTED_CERTS);
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String htmlText = Utils.stream2str(response.getEntity().getContent());
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		if (!htmlText.contains(CHECK_DP_LOGIN)) {
			Utils.clearCookies();
			throw new WebSMSException(context, R.string.error_pw);
		}

		// update balance
		String balance = "";
		int i = htmlText.indexOf(CHECK_DP_BALANCE1);
		if (i > 0) {
			htmlText = htmlText.substring(i, htmlText.indexOf(" EUR", i));
			Log.d(TAG, htmlText);
			htmlText = htmlText.substring(htmlText.lastIndexOf(">") + 1);
			balance = htmlText + "\u20ac";
			Log.d(TAG, "balance: " + balance);
		}

		// update free balance
		response = Utils.getHttpClient(URL_DP_SEND, null, null, TARGET_AGENT,
				null, ENCODING, Preferences.getTrustAll(p), TRUSTED_CERTS);

		resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			Utils.clearCookies();
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		htmlText = Utils.stream2str(response.getEntity().getContent());
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		i = htmlText.indexOf(CHECK_DP_BALANCE2);
		if (i < 0) {
			Utils.clearCookies();
			throw new WebSMSException(context, R.string.error);
		}
		htmlText = htmlText.substring(1, i);
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");
		i = htmlText.lastIndexOf(">");
		Log.d(TAG, "i: " + i);
		++i;
		Log.d(TAG, "i: " + i);
		final int j = htmlText.lastIndexOf(" ");
		Log.d(TAG, "j: " + j);
		if (!TextUtils.isEmpty(balance)) {
			balance += "/";
		}
		if (j < i) {
			balance += htmlText.substring(i);
		} else {
			balance += htmlText.substring(i, j);
		}

		Log.d(TAG, "balance: " + balance);
		this.getSpec(context).setBalance(balance);
	}

	/**
	 * Send text via service.discoplus.de.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @throws IOException
	 *             IOException
	 */
	private void dpSendText(final Context context,
			final ConnectorCommand command) throws IOException {
		final int cc = Utils.getCookieCount();
		if (cc == 0) {
			this.dpDoLogin(context, command);
		}
		String number = Utils.national2international(command.getDefPrefix(),
				Utils.getRecipientsNumber(command.getRecipients()[0]));
		Log.d(TAG, "number: " + number);
		final String prefix = number.substring(0, 6);
		Log.d(TAG, "prefix: " + prefix);
		number = number.substring(prefix.length());
		Log.d(TAG, "number: " + number);

		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(NUM_VARS_SEND);
		postData.add(new BasicNameValuePair("vorwahl", prefix));
		postData.add(new BasicNameValuePair("nummer", number));
		postData.add(new BasicNameValuePair("TEXT", command.getText()));

		postData.add(new BasicNameValuePair("SMSanzahl", "1"));
		postData.add(new BasicNameValuePair("ZEICHEN", "619"));

		postData.add(new BasicNameValuePair("sende", "Absenden"));
		postData.add(new BasicNameValuePair("action", ""));
		postData.add(new BasicNameValuePair("unteraction", "abschicken"));

		HttpResponse response = Utils.getHttpClient(Utils.httpGetParams(
				URL_DP_SEND, postData, ENCODING), null, null, TARGET_AGENT,
				URL_DP_SEND, ENCODING, Preferences.getTrustAll(context),
				TRUSTED_CERTS);
		postData = null;
		final int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String htmlText = Utils.stream2str(response.getEntity().getContent());
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		final int i = htmlText.indexOf(CHECK_DP_SENT);
		if (i < 0) {
			Log.e(TAG, "failed to send message, response following:");
			Log.e(TAG, htmlText);
			throw new WebSMSException(context, R.string.error);
		}
	}

	/**
	 * Login to lounge.discoplus.de.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @throws IOException
	 *             IOException
	 */
	private void dtDoLogin(final Context context, // .
			final ConnectorCommand command) throws IOException {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);

		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(NUM_VARS_LOGIN);
		postData.add(new BasicNameValuePair("do", "login"));
		postData.add(new BasicNameValuePair("goto", ""));
		postData.add(new BasicNameValuePair("loginname", p.getString(
				Preferences.PREFS_USERNAME, "")));
		postData.add(new BasicNameValuePair("password", p.getString(
				Preferences.PREFS_PASSWORD, "")));

		HttpResponse response = Utils.getHttpClient(URL_DT_LOGIN, null, // .
				postData, TARGET_AGENT, URL_DT_LOGIN, ENCODING, Preferences
						.getTrustAll(p));
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String htmlText = Utils.stream2str(response.getEntity().getContent());
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		if (!htmlText.contains(CHECK_DT_LOGIN)) {
			Utils.clearCookies();
			throw new WebSMSException(context, R.string.error_pw);
		}

		// update balance
		String balance = "";
		int i = htmlText.indexOf(CHECK_DT_BALANCE1);
		if (i > 0) {
			htmlText = htmlText.substring(i, htmlText.indexOf("SMS", i));
			Log.d(TAG, htmlText);
			htmlText = htmlText.substring(htmlText.indexOf(">") + 1);
			balance = htmlText.substring(0, htmlText.indexOf("<"));
			Log.d(TAG, "balance: " + balance);
		}

		Log.d(TAG, "balance: " + balance);
		this.getSpec(context).setBalance(balance);
	}

	/**
	 * Send text via service.discoplus.de.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @throws IOException
	 *             IOException
	 */
	private void dtSendText(final Context context,
			final ConnectorCommand command) throws IOException {
		if (command.getText().length() > MAXLENGTH) {
			throw new WebSMSException(context, R.string.error_length_101);
		}
		final int cc = Utils.getCookieCount();
		if (cc == 0) {
			this.dtDoLogin(context, command);
		}
		String number = Utils.national2international(command.getDefPrefix(),
				Utils.getRecipientsNumber(command.getRecipients()[0]));
		Log.d(TAG, "number: " + number);

		String sender = Utils.getSender(context, command.getDefSender());
		Log.d(TAG, "sender:  " + sender);

		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(NUM_VARS_SEND);
		postData.add(new BasicNameValuePair("submit_sms", "SMS versenden"));
		postData.add(new BasicNameValuePair("confirm_agb", "1"));
		postData.add(new BasicNameValuePair("sender", sender));
		postData.add(new BasicNameValuePair("message", command.getText()));
		postData.add(new BasicNameValuePair("receiver", Utils
				.cleanRecipient(command.getRecipients()[0])));

		HttpResponse response = Utils.getHttpClient(URL_DT_SEND, null,
				postData, TARGET_AGENT, URL_DT_SEND, ENCODING, Preferences
						.getTrustAll(context));
		postData = null;
		final int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String htmlText = Utils.stream2str(response.getEntity().getContent());
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		final int i = htmlText.indexOf(CHECK_DT_SENT);
		if (i < 0) {
			Log.e(TAG, "failed to send message, response following:");
			Log.e(TAG, htmlText);
			System.out.println(htmlText);
			throw new WebSMSException(context, R.string.error);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent) {
		try {
			String url = Preferences.getService(context);
			if (url.contains("service.discoplus.de")) {
				this.dpDoLogin(context, new ConnectorCommand(intent));
			} else {
				this.dtDoLogin(context, new ConnectorCommand(intent));
			}
		} catch (IOException e) {
			Log.e(TAG, "login failed", e);
			throw new WebSMSException(e.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent) {
		try {
			String url = Preferences.getService(context);
			if (url.contains("service.discoplus.de")) {
				this.dpSendText(context, new ConnectorCommand(intent));
			} else {
				this.dtSendText(context, new ConnectorCommand(intent));
			}
		} catch (IOException e) {
			Log.e(TAG, "send failed", e);
			throw new WebSMSException(e.toString());
		}
	}
}
