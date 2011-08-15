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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import de.ub0r.android.websms.connector.common.ConnectorPreferenceActivity;
import de.ub0r.android.websms.connector.common.Log;

/**
 * Preferences.
 * 
 * @author flx
 */
public final class Preferences extends ConnectorPreferenceActivity implements
		OnPreferenceChangeListener {
	/** Preference key: enabled. */
	static final String PREFS_ENABLED = "enable_discotel";
	/** Preference's name: user's login. */
	static final String PREFS_USERNAME = "username_discotel";
	/** Preference's name: user's password. */
	static final String PREFS_PASSWORD = "password_discotel";
	/** Preference's name: etelon service. */
	static final String PREFS_SERVICE = "etelon_service";
	/** Preference's name: ignore SSL errors. */
	private static final String PREFS_IGNORE_SSL_ERRORS = "ignore_ssl_error";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
		Preference pref = this.findPreference(PREFS_SERVICE);
		pref.setOnPreferenceChangeListener(this);
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (p.getBoolean(PREFS_ENABLED, false)) {
			this.onPreferenceChange(pref, Preferences.getService(p));
		} else {
			this.findPreference(PREFS_USERNAME).setEnabled(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPreferenceChange(final Preference preference,
			final Object newValue) {
		Log.d("dicotel.prefs", "key: " + preference.getKey());
		Log.d("dicotel.prefs", "val: " + newValue);
		if (preference.getKey().equals(PREFS_SERVICE)) {
			String s = (String) newValue;
			this.findPreference(PREFS_USERNAME).setEnabled(
					!s.contains("service.discoplus.de"));
			return true;
		}
		return false;
	}

	/**
	 * Get selected service.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return service
	 */
	static String getService(final Context context) {
		return getService(PreferenceManager
				.getDefaultSharedPreferences(context));
	}

	/**
	 * Get selected service.
	 * 
	 * @param p
	 *            {@link SharedPreferences}
	 * @return service
	 */
	static String getService(final SharedPreferences p) {
		return p.getString(Preferences.PREFS_SERVICE, "service.discoplus.de");
	}

	/**
	 * Trust all SSL certificates?
	 * 
	 * @param context
	 *            {@link Context}
	 * @return true, for trusting all certificates
	 */
	static boolean getTrustAll(final Context context) {
		return getTrustAll(PreferenceManager
				.getDefaultSharedPreferences(context));
	}

	/**
	 * Trust all SSL certificates?
	 * 
	 * @param p
	 *            {@link SharedPreferences}
	 * @return true, for trusting all certificates
	 */
	static boolean getTrustAll(final SharedPreferences p) {
		return p.getBoolean(PREFS_IGNORE_SSL_ERRORS, false);
	}
}
