package com.mridang.computer;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

/*
 * This class is the main class that provides the widget
 */
public class ComputerWidget extends DashClockExtension {

	/* This is the instance of the receiver that deals with received messages */
	private MessageReceiver objMessageReceiver;
	/* This is the instance of the thread that keeps track of connected clients */
	private Thread thrPeriodicTicker;

	/*
	 * This class is the receiver for getting the received messages
	 */
	private class MessageReceiver extends BroadcastReceiver {

		/*
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context ctxContext, Intent ittIntent) {

			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(ctxContext);
			String strType = gcm.getMessageType(ittIntent);
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(strType)) {

				showInformation(ittIntent.getExtras());

			} 

		}

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		super.onInitialize(booReconnect);

		if (objMessageReceiver != null) {

			try {

				Log.d("ComputerWidget", "Unregistering any existing status receivers");
				unregisterReceiver(objMessageReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		IntentFilter intentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
		intentFilter.addCategory("com.mridang.computer");

		objMessageReceiver = new MessageReceiver();
		registerReceiver(objMessageReceiver, intentFilter);
		Log.d("ComputerWidget", "Registered the status receiver");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("ComputerWidget", "Created");
		BugSenseHandler.initAndStartSession(this, getString(R.string.bugsense));

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@SuppressLint("DefaultLocale")
	protected void showInformation(Bundle bndBundle) {

		Log.d("ComputerWidget", "Checking the message recived");
		final ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(true);

		try {

			if (bndBundle.getString("event").equalsIgnoreCase("logon")) {

				Log.d("ComputerWidget", bndBundle.getString("username") + " logged on at " + bndBundle.getString("machine"));

				edtInformation.visible(true);
				edtInformation.expandedBody(bndBundle.getString("machine"));
				edtInformation.expandedTitle(getString(R.string.logon, bndBundle.getString("username").toLowerCase()));

				if (thrPeriodicTicker != null) {

					Log.d("ComputerWidget", "Stop the existing periodic checker");
					thrPeriodicTicker.interrupt();

				}

				if (thrPeriodicTicker == null) {

					Log.d("ComputerWidget", "Making a little vibration vibes");
					((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);

				}

				Log.d("ComputerWidget", "Starting a new periodic checker");
				thrPeriodicTicker = new Thread() {

					public void run () {

						try {

							Thread.sleep(120000);

							Log.d("ComputerWidget", "Computer is probably off since no event was received.");

							edtInformation.visible(false);
							publishUpdate(edtInformation);

							((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);									

						}catch (InterruptedException e) {
							Log.d("ComputerWidget", "Stopping the periodic checker.");
							return;
						} catch (Exception e) {
							Log.e("ComputerWidget", "Encountered an error", e);
							BugSenseHandler.sendException(e);
						}

					}

				};

				thrPeriodicTicker.start();

			} else if (bndBundle.getString("event").equalsIgnoreCase("lock")) {

				Log.d("ComputerWidget", bndBundle.getString("username") + " locked the computer " + bndBundle.getString("machine"));

				edtInformation.visible(true);
				edtInformation.expandedBody(bndBundle.getString("machine"));
				edtInformation.expandedTitle(getString(R.string.lock, bndBundle.getString("username").toLowerCase()));

				if (thrPeriodicTicker != null) {

					Log.d("ComputerWidget", "Stop the existing periodic checker");
					thrPeriodicTicker.interrupt();

				}

				Log.d("ComputerWidget", "Making a little vibration vibes");
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);

				Log.d("ComputerWidget", "Starting a new periodic checker");
				thrPeriodicTicker = new Thread() {

					public void run () {

						try {

							Thread.sleep(120000);

							Log.d("ComputerWidget", "User has been notified of lock so hide message");

							edtInformation.visible(false);
							publishUpdate(edtInformation);

						}catch (InterruptedException e) {
							Log.d("ComputerWidget", "Stopping the periodic checker.");
							return;
						} catch (Exception e) {
							Log.e("ComputerWidget", "Encountered an error", e);
							BugSenseHandler.sendException(e);
						}

					}

				};

				thrPeriodicTicker.start();

			} else if (bndBundle.getString("event").equalsIgnoreCase("unlock")) {

				Log.d("ComputerWidget", bndBundle.getString("username") + " unlocked the computer " + bndBundle.getString("machine"));

				edtInformation.visible(true);
				edtInformation.expandedBody(bndBundle.getString("machine"));
				edtInformation.expandedTitle(getString(R.string.unlock, bndBundle.getString("username").toLowerCase()));

				if (thrPeriodicTicker != null) {

					Log.d("ComputerWidget", "Stop the existing periodic checker");
					thrPeriodicTicker.interrupt();

				}

				Log.d("ComputerWidget", "Making a little vibration vibes");
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);

				Log.d("ComputerWidget", "Starting a new periodic checker");
				thrPeriodicTicker = new Thread() {

					public void run () {

						try {

							Thread.sleep(120000);

							Log.d("ComputerWidget", "User has been notified of unlock so hide message");

							edtInformation.visible(false);
							publishUpdate(edtInformation);

						}catch (InterruptedException e) {
							Log.d("ComputerWidget", "Stopping the periodic checker.");
							return;
						} catch (Exception e) {
							Log.e("ComputerWidget", "Encountered an error", e);
							BugSenseHandler.sendException(e);
						}

					}

				};

				thrPeriodicTicker.start();

			} else if (bndBundle.getString("event").equalsIgnoreCase("logoff")) {

				Log.d("ComputerWidget", bndBundle.getString("username") + " logged off at " + bndBundle.getString("machine"));

				edtInformation.visible(true);
				edtInformation.expandedBody(bndBundle.getString("machine"));
				edtInformation.expandedTitle(getString(R.string.logoff, bndBundle.getString("username").toLowerCase()));

				if (thrPeriodicTicker != null) {

					Log.d("ComputerWidget", "Stop the existing periodic checker");
					thrPeriodicTicker.interrupt();

				}

				Log.d("ComputerWidget", "Making a little vibration vibes");
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);

				Log.d("ComputerWidget", "Starting a new periodic checker");
				thrPeriodicTicker = new Thread() {

					public void run () {

						try {

							Thread.sleep(120000);

							Log.d("ComputerWidget", "User has been notified of logoff so hide message");

							edtInformation.visible(false);
							publishUpdate(edtInformation);

						}catch (InterruptedException e) {
							Log.d("ComputerWidget", "Stopping the periodic checker.");
							return;
						} catch (Exception e) {
							Log.e("ComputerWidget", "Encountered an error", e);
							BugSenseHandler.sendException(e);
						}

					}

				};

				thrPeriodicTicker.start();

			} else {

				Log.d("ComputerWidget", bndBundle.getString("username") + " is active on " + bndBundle.getString("machine"));

				edtInformation.visible(true);
				edtInformation.expandedBody(bndBundle.getString("machine"));
				edtInformation.expandedTitle(getString(R.string.active, bndBundle.getString("username").toLowerCase()));

				if (thrPeriodicTicker != null) {

					Log.d("ComputerWidget", "Stop the existing periodic checker");
					thrPeriodicTicker.interrupt();

				}

				Log.d("ComputerWidget", "Starting a new periodic checker");
				thrPeriodicTicker = new Thread() {

					public void run () {

						try {

							Thread.sleep(120000);

							Log.d("ComputerWidget", "Computer is probably off since no event was received.");

							edtInformation.visible(false);
							publishUpdate(edtInformation);

							((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);									

						}catch (InterruptedException e) {
							Log.d("ComputerWidget", "Stopping the periodic checker.");
							return;
						} catch (Exception e) {
							Log.e("ComputerWidget", "Encountered an error", e);
							BugSenseHandler.sendException(e);
						}

					}

				};

				thrPeriodicTicker.start();

			}

			if (new Random().nextInt(5) == 0) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);
					if (mgrPackages.checkSignatures(getPackageName(), "com.mridang.donate") != PackageManager.SIGNATURE_MATCH) {

						throw new NameNotFoundException("Mismatched signatures");

					}

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
					Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
					String strPackage;

					for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

						strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0); 

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation.expandedBody("Thank you for using " + intExtensions + " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(false);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e("ComputerWidget", "Encountered an error", e);
			BugSenseHandler.sendException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("ComputerWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();

		if (objMessageReceiver != null) {

			try {

				Log.d("ComputerWidget", "Unregistered the status receiver");
				unregisterReceiver(objMessageReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		Log.d("ComputerWidget", "Destroyed");
		BugSenseHandler.closeSession(this);

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData(int)
	 */
	@SuppressLint("DefaultLocale")
	@Override
	protected void onUpdateData(int intReason) {

		setUpdateWhenScreenOn(true);

		SharedPreferences spePreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		Log.d("ComputerWidget", "Checking if the widget is configured");
		if (spePreferences.getString("hostname", "").isEmpty()) {

			Log.d("ComputerWidget", "Hostname hasn't been configured");
			Toast.makeText(getApplicationContext(), getString(R.string.unconfigured), Toast.LENGTH_LONG).show();

		} else {

			try {

				String strToken = GoogleCloudMessaging.getInstance(getApplicationContext()).register("84581482730");
				Log.d("ComputerWidget", "Checking the status of the new token: " + strToken);
				if (spePreferences.getString("token", "").isEmpty()) {

					Log.d("ComputerWidget", "Not registered, saving the token");
					spePreferences.edit().putString("token", strToken).commit();

				} else {

					Log.d("ComputerWidget", "Token changed, saving the new token");
					spePreferences.edit().putString("token", strToken).commit();

				}

				String strName = spePreferences.getString("hostname", "").toUpperCase();
				Log.d("ComputerWidget", "Hostname is " + strName);

				MessageDigest md5Digest = MessageDigest.getInstance("MD5");
				byte[] bytHash = md5Digest.digest(strName.getBytes());
				StringBuffer sbfHash = new StringBuffer();

				for (int i = 0; i < bytHash.length; ++i) {
					sbfHash.append(Integer.toHexString(bytHash[i] & 0xFF | 0x100).substring(1,3));
				}

				String strPath = "http://androprter.appspot.com/set/token" + sbfHash.toString() +"/";
				Log.d("ComputerWidget", "Posting token to " + strPath);
				AsyncHttpClient ascClient = new AsyncHttpClient();
				ascClient.setTimeout(5000);
				ascClient.post(getApplicationContext(), 
						strPath, 
						new StringEntity(strToken, "UTF-8"), "text/plain", new AsyncHttpResponseHandler() {

					@Override
					public void onSuccess(String strResponse) {
						Log.d("ComputerWidget", "Posted the token successfully");
					}

					@Override
					public void onFailure(int intCode, Header[] arrHeaders, byte[] arrBytes, Throwable errError) {
						Log.w("ComputerWidget", "Error posting token due to code " + intCode);
					}

				});

			} catch (IOException e) {
				Log.e("ComputerWidget", "Error getting token", e);

			} catch (Exception e) {
				Log.e("ComputerWidget", "Unknown error occurred", e);
				BugSenseHandler.sendException(e);
			} 

		}

	}

}