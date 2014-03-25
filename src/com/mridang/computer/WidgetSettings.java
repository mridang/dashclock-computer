package com.mridang.computer;

import java.security.MessageDigest;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/*
 * This class is the activity which contains the preferences
 */
@SuppressWarnings("deprecation")
public class WidgetSettings extends PreferenceActivity {

	/* The instance of the pdgProgress dialog */
	ProgressDialog pdgProgress;

	/*
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		getActionBar().setIcon(R.drawable.ic_dashclock);
		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	/*
	 * A preference value change listener that updates the preference's summary 
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference prePreference, Object objValue) {

			prePreference.setSummary(objValue.toString().isEmpty() ? prePreference.getSummary() : objValue.toString());
			return true;

		}

	};

	/*
	 * Binds a preference's summary to its value. More specifically, when the 
	 * preference's value is changed, its summary is updated to reflect the value.
	 */
	private static void bindPreferenceSummaryToValue(Preference prePreference) {

		prePreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		sBindPreferenceSummaryToValueListener.onPreferenceChange(prePreference,
				PreferenceManager
				.getDefaultSharedPreferences(prePreference.getContext())
				.getString(prePreference.getKey(), ""));

	}

	/*
	 * @see android.app.Activity#onPostCreate(android.os.Bundle)
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		bindPreferenceSummaryToValue(findPreference("hostname"));


	}

	/*
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {

		try {

			if (new AsyncTask<Void, Void, String>() {

				/*
				 * @see android.os.AsyncTask#doInBackground(Params[])
				 */
				@Override
				protected String doInBackground(Void... params) {

					try {

						SharedPreferences spePreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

						Log.d("WidgetSettings", "Checking if we've already registred with GCM");
						if (spePreferences.getString("token", "").isEmpty()) {

							Log.d("WidgetSettings", "Token not found. Getting a new GCM token and saving it.");
							String strToken = GoogleCloudMessaging.getInstance(getApplicationContext()).register("84581482730");

							Log.d("WidgetSettings", "Got token. " + strToken);
							spePreferences.edit().putString("token", strToken).commit();

						} 

						Log.d("WidgetSettings", "Registering token with backend");
						String strToken = spePreferences.getString("token", "");

						EditTextPreference edtName = (EditTextPreference) findPreference("hostname");
						String strName = edtName.getText();
						MessageDigest md5Digest = MessageDigest.getInstance("MD5");
						byte[] bytHash = md5Digest.digest(strName.getBytes());
						StringBuffer sbfHash = new StringBuffer();

						for (int i = 0; i < bytHash.length; ++i) {
							sbfHash.append(Integer.toHexString(bytHash[i] & 0xFF | 0x100).substring(1,3));
						}

						String strPath = "http://androprter.appspot.com/set/" + sbfHash.toString() +"/";
						Log.d("WidgetSettings", "Posting token to " + strPath);

						HttpClient dhcClient = new DefaultHttpClient();
						HttpPost posToken = new HttpPost(strPath);
						posToken.setEntity(new StringEntity(strToken));
						HttpResponse resToken = dhcClient.execute(posToken);

						Integer intToken = resToken.getStatusLine().getStatusCode();
						if (intToken != HttpStatus.SC_OK) {
							throw new HttpResponseException(intToken, "Server responded with code " + intToken);
						}

						Log.d("WidgetSettings", "Successfully registered token");
						return strToken;

					} catch (Exception e) {
						Log.e("WidgetSettings", "Error getting token", e);
						BugSenseHandler.sendException(e);
					}

					Toast.makeText(getApplicationContext(), R.string.toast, Toast.LENGTH_LONG).show();
					return null;

				}

			}.execute().get() != null) {
				super.onBackPressed();
			}

		} catch (Exception e) {
			Log.e("WidgetSettings", "Error getting token", e);
			BugSenseHandler.sendException(e);
		}

	}

}