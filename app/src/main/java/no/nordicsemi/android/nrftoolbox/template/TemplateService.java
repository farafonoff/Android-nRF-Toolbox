/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrftoolbox.template;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.nrftoolbox.FeaturesActivity;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.ToolboxApplication;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService;
import no.nordicsemi.android.nrftoolbox.profile.LoggableBleManager;

public class TemplateService extends BleProfileService implements TemplateManagerCallbacks {
	public static final String BROADCAST_TEMPLATE_MEASUREMENT = "no.nordicsemi.android.nrftoolbox.template.BROADCAST_MEASUREMENT";
	public static final String EXTRA_DATA = "no.nordicsemi.android.nrftoolbox.template.EXTRA_DATA";

	public static final String BROADCAST_BATTERY_LEVEL = "no.nordicsemi.android.nrftoolbox.BROADCAST_BATTERY_LEVEL";
	public static final String EXTRA_BATTERY_LEVEL = "no.nordicsemi.android.nrftoolbox.EXTRA_BATTERY_LEVEL";

	private final static String ACTION_DISCONNECT = "no.nordicsemi.android.nrftoolbox.template.ACTION_DISCONNECT";

	private final static int NOTIFICATION_ID = 864;
	private final static int OPEN_ACTIVITY_REQ = 0;
	private final static int DISCONNECT_REQ = 1;

	private TemplateManager mManager;

	private static BluetoothDevice msDevice;

	private final LocalBinder mBinder = new TemplateBinder();

	PhoneBroadcastReceiver mReciever = new PhoneBroadcastReceiver();

	/**
	 * This local binder is an interface for the bound activity to operate with the sensor.
	 */
	class TemplateBinder extends LocalBinder {
		// TODO Define service API that may be used by a bound Activity

		/**
		 * Sends some important data to the device.
		 *
		 * @param parameter some parameter.
		 */
		public void performAction(final String parameter) {
			TemplateService.this.
			 mManager.performAction(parameter);
		}

		public void notifyCall(final String callerId, int times) {
			TemplateService.this.notifyCall(callerId, times);
			//mManager.notifyCall(callerId);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		 int original = super.onStartCommand(intent, flags, startId);
		 String notifyType = intent.getStringExtra("notify");
		 String notifyContent = intent.getStringExtra("notifyContent");
		 if ("call".equals(notifyType)) {
			 final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ToolboxApplication.CONNECTED_DEVICE_CHANNEL);
			 builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.template_notification_connected_message, getDeviceName()));
			 builder.setSmallIcon(R.drawable.ic_stat_notify_template);
			 //builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
			 //builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.template_notification_action_disconnect), disconnectAction));
			 final Notification notification = builder.build();
			 startForeground(1, notification);
		 	notifyCall(notifyContent, 2);
		 	return START_STICKY;
		 }
		 return original;
	}

	String getFromPhonebook(Context context, String number) {
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor c = context.getContentResolver().query(lookupUri, new String[]{ContactsContract.Data.DISPLAY_NAME},null,null,null);
		try {
			if(c.moveToFirst()) {
				String displayName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				//displayName = c.getString(0);
				String ContactName = displayName;
				Toast.makeText(context, ContactName, Toast.LENGTH_LONG).show();
				return ContactName;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			c.close();
		}
		return null;
	}

	ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	void notifyCall(final String callerId, int times) {
		String displayName = getFromPhonebook(this, callerId);
		String text = BrtlUtils.transliterate(displayName == null?callerId:displayName);
		if (text.length() > 11) {
			text = text.substring(0, 11);
		}
		final String notifyText = text;
		mManager.notifyCall(notifyText);
		if (times > 1) {
			ScheduledFuture handle = executorService.scheduleAtFixedRate(() -> mManager.notifyCall(notifyText), 5,5, TimeUnit.SECONDS);
			Runnable canceller = () -> handle.cancel(false);
			executorService.schedule(canceller, 5*times, TimeUnit.SECONDS);
		}
	}

	@Override
	protected LocalBinder getBinder() {
		return mBinder;
	}

	@Override
	protected LoggableBleManager<TemplateManagerCallbacks> initializeManager() {
		return mManager = new TemplateManager(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		final IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_DISCONNECT);
		registerReceiver(mDisconnectActionBroadcastReceiver, filter);

		IntentFilter phoneFilter = new IntentFilter();
		phoneFilter.addAction("android.intent.action.PHONE_STATE");
		registerReceiver(mReciever, phoneFilter);
		/*TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(new CallStateListener(), PhoneStateListener.LISTEN_CALL_STATE);*/
	}

	@Override
	public void onDeviceReady(@NonNull BluetoothDevice device) {
		super.onDeviceReady(device);
		msDevice = device;
	}

	@Override
	public void onDestroy() {
		// when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
		cancelNotification();
		unregisterReceiver(mDisconnectActionBroadcastReceiver);
		unregisterReceiver(mReciever);

		super.onDestroy();
	}

	@Override
	protected void onRebind() {
		// when the activity rebinds to the service, remove the notification
		cancelNotification();
	}

	@Override
	protected void onUnbind() {
		// when the activity closes we need to show the notification that user is connected to the sensor
		createNotification(R.string.template_notification_connected_message, 0);
	}

	@Override
	public void onSampleValueReceived(@NonNull final BluetoothDevice device, final int value) {
		final Intent broadcast = new Intent(BROADCAST_TEMPLATE_MEASUREMENT);
		broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
		broadcast.putExtra(EXTRA_DATA, value);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

		if (!mBound) {
			// Here we may update the notification to display the current value.
			// TODO modify the notification here
		}
	}

	@Override
	public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int batteryLevel) {

	}

	/**
	 * Creates the notification.
	 *
	 * @param messageResId message resource id. The message must have one String parameter,<br />
	 *                     f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
	 * @param defaults     signals that will be used to notify the user
	 */
	private void createNotification(final int messageResId, final int defaults) {
		final Intent parentIntent = new Intent(this, FeaturesActivity.class);
		parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final Intent targetIntent = new Intent(this, TemplateActivity.class);

		final Intent disconnect = new Intent(ACTION_DISCONNECT);
		final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);

		// both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
		final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[]{parentIntent, targetIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ToolboxApplication.CONNECTED_DEVICE_CHANNEL);
		builder.setContentIntent(pendingIntent);
		builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(messageResId, getDeviceName()));
		builder.setSmallIcon(R.drawable.ic_stat_notify_template);
		builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
		builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.template_notification_action_disconnect), disconnectAction));

		final Notification notification = builder.build();
		final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(NOTIFICATION_ID, notification);
	}

	/**
	 * Cancels the existing notification. If there is no active notification this method does nothing
	 */
	private void cancelNotification() {
		final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}

	/**
	 * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
	 */
	private final BroadcastReceiver mDisconnectActionBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			Logger.i(getLogSession(), "[Notification] Disconnect action pressed");
			if (isConnected())
				getBinder().disconnect();
			else
				stopSelf();
		}
	};

	@Override
	protected boolean shouldAutoConnect() {
		return true;
	}

	/*(private class CallStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					// called when someone is ringing to this phone

					Toast.makeText(TemplateService.this,
							"Incoming: "+incomingNumber,
							Toast.LENGTH_LONG).show();
					break;
			}
		}
	}*/
}
