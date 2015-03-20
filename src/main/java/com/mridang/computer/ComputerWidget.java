package com.mridang.computer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.acra.ACRA;
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.security.MessageDigest;

/*
 * This class is the main class that provides the widget
 */
public class ComputerWidget extends ImprovedExtension {

	/**
	 * The handler class that hides the notification after a few seconds
	 */
	private class HeartbeatHandler extends Handler {

		/**
		 * Simple constructor to initialize the initial value of the previous
		 */
		public HeartbeatHandler(Looper looLooper) {
			super(looLooper);
		}

		/**
		 * Handler method that that acts an and expiration checker that upon expiry simple hides the
		 * dashclock notification.
		 */
		@Override
		public void handleMessage(Message msgMessage) {

			try {

				Log.d(getTag(), "User has been notified of logoff so hide message");
				ExtensionData edtInformation = new ExtensionData();
				edtInformation.visible(false);
				doUpdate(edtInformation);

			} catch (Exception e) {
				Log.e(ComputerWidget.this.getTag(), "Error hiding the notification", e);
			}

		}

	}

	/**
	 * The instance of the manager of the notification services
	 */
	private static NotificationManager mgrNotifications;
	/**
	 * The instance of the notification builder to rebuild the notification
	 */
	private static NotificationCompat.Builder notBuilder;
	/**
	 * This is the instance of the thread that keeps track of connected clients
	 */
	private HeartbeatHandler hndWaiter;

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hardware.ImprovedExtension#getIntents()
	 */
	@Override
	protected IntentFilter getIntents() {

		IntentFilter itfIntents = new IntentFilter();
		itfIntents.addAction("com.google.android.c2dm.intent.RECEIVE");
		itfIntents.addCategory("com.mridang.computer");
		return itfIntents;

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hardware.ImprovedExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hardware.ImprovedExtension#getUris()
	 */
	@Override
	protected String[] getUris() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hardware.ImprovedExtension#onInitialize(java.lang.Boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		super.onInitialize(booReconnect);
		mgrNotifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		notBuilder = new NotificationCompat.Builder(this);
		notBuilder = notBuilder.setSmallIcon(R.drawable.ic_dashclock);
		notBuilder = notBuilder.setOngoing(false);
		notBuilder = notBuilder.setShowWhen(true);
		notBuilder = notBuilder.setOnlyAlertOnce(false);
		notBuilder = notBuilder.setPriority(Integer.MAX_VALUE);
		notBuilder = notBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		notBuilder = notBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);

		Looper.prepare();
		hndWaiter = new HeartbeatHandler(Looper.myLooper());
		Looper.loop();

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hardware.ImprovedExtension#onDestroy()
	 */
	@Override
	public void onDestroy() {

		mgrNotifications.cancel(115);
		super.onDestroy();

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.hardware.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {

		ExtensionData edtInformation = new ExtensionData();
		try {

			Bundle bndBundle = ittIntent.getExtras();
			String strUsername = bndBundle.getString("username").toLowerCase();
			String strMachine = bndBundle.getString("machine");
			notBuilder = notBuilder.setWhen(System.currentTimeMillis());
			notBuilder = notBuilder.setContentTitle(strMachine);
			edtInformation.visible(true);
			edtInformation.expandedBody(strMachine);

			if (bndBundle.getString("event").equalsIgnoreCase("logon")) {

				Log.d(getTag(), strUsername + " logged on at " + strMachine);
				edtInformation.expandedTitle(getString(R.string.logon, strUsername));
				notBuilder = notBuilder.setContentText(edtInformation.expandedTitle());
				mgrNotifications.notify(115, notBuilder.build());

			} else if (bndBundle.getString("event").equalsIgnoreCase("lock")) {

				Log.d(getTag(), strUsername + " locked the computer " + strMachine);
				edtInformation.expandedTitle(getString(R.string.lock, strUsername));
				notBuilder = notBuilder.setContentText(edtInformation.expandedTitle());
				mgrNotifications.notify(115, notBuilder.build());

			} else if (bndBundle.getString("event").equalsIgnoreCase("unlock")) {

				Log.d(getTag(), strUsername + " unlocked the computer " + strMachine);
				edtInformation.expandedTitle(getString(R.string.unlock, strUsername));
				notBuilder = notBuilder.setContentText(edtInformation.expandedTitle());
				mgrNotifications.notify(115, notBuilder.build());

			} else if (bndBundle.getString("event").equalsIgnoreCase("logoff")) {

				Log.d(getTag(), strUsername + " logged off at " + strMachine);
				edtInformation.expandedTitle(getString(R.string.logoff, strUsername));
				notBuilder = notBuilder.setContentText(edtInformation.expandedTitle());
				mgrNotifications.notify(115, notBuilder.build());

			} else {

				Log.v(getTag(), strUsername + " is active on " + strMachine);
				edtInformation.expandedTitle(getString(R.string.active, strUsername));

			}

			hndWaiter.removeMessages(1);
			hndWaiter.sendEmptyMessageDelayed(1, 120000);

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e(getTag(), "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		doUpdate(edtInformation);

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		setUpdateWhenScreenOn(true);

		Log.d(getTag(), "Checking if the widget is configured");
		if (getString("hostname", "").isEmpty()) {

			Log.d(getTag(), "Hostname hasn't been configured");
			Toast.makeText(getApplicationContext(), getString(R.string.unconfigured), Toast.LENGTH_LONG).show();

		} else {

			try {

				String strToken = GoogleCloudMessaging.getInstance(getApplicationContext()).register("84581482730");
				Log.d(getTag(), "Checking the status of the new token: " + strToken);
				if (getString("token", "").isEmpty()) {

					Log.d(getTag(), "Not registered, saving the token");
					getEditor().putString("token", strToken).commit();

				} else {

					Log.d(getTag(), "Token changed, saving the new token");
					getEditor().putString("token", strToken).commit();

				}

				String strName = getString("hostname", "").toUpperCase();
				Log.d(getTag(), "Hostname is " + strName);

				MessageDigest md5Digest = MessageDigest.getInstance("MD5");
				byte[] bytHash = md5Digest.digest(strName.getBytes());
				StringBuilder sbfHash = new StringBuilder();

				for (byte aBytHash : bytHash) {
					sbfHash.append(Integer.toHexString(aBytHash & 0xFF | 0x100).substring(1, 3));
				}

				String strPath = "http://androprter.appspot.com/set/token" + sbfHash.toString() + "/";
				Log.d(getTag(), "Posting token to " + strPath);
				AsyncHttpClient ascClient = new AsyncHttpClient();
				ascClient.setTimeout(5000);
				ascClient.post(getApplicationContext(), strPath, new StringEntity(strToken, "UTF-8"), "text/plain",
						new AsyncHttpResponseHandler() {

							@Override
							public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
								Log.d(getTag(), "Posted the token successfully");
							}

							@Override
							public void onFailure(int intCode, Header[] arrHeaders, byte[] arrBytes, Throwable errError) {
								Log.w(getTag(), "Error posting token due to code " + intCode);
							}

						});

			} catch (IOException e) {
				Log.e(getTag(), "Error getting token", e);

			} catch (Exception e) {
				Log.e(getTag(), "Unknown error occurred", e);
				ACRA.getErrorReporter().handleSilentException(e);
			}

		}

	}

}