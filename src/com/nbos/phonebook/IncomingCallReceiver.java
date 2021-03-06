package com.nbos.phonebook;

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


public class IncomingCallReceiver extends BroadcastReceiver {

	private Context mContext;
	private Intent mIntent;
	String tag = "CallReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		mIntent = intent;
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int events = PhoneStateListener.LISTEN_CALL_STATE;
		tm.listen(phoneStateListener, events);
	}

	private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			String callState = "UNKNOWN";
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				callState = "IDLE";
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				try {
					String groups = Db.getGroupNamesFromPhoneNumber(incomingNumber, mContext);
					Log.i(tag, "groups is: "+groups);
					if(groups == null) break;
					Toast.makeText(mContext, "Phonebooks: " + groups, Toast.LENGTH_LONG).show();
				} catch(Exception e){}
				// -- check international call or not.
				/*if (incomingNumber.startsWith("00")) {
					Toast.makeText(mContext,"International Call- " + incomingNumber,Toast.LENGTH_LONG).show();
					callState = "International - Ringing (" + incomingNumber+ ")";
				} else {
					Toast.makeText(mContext, "Local Call - " + incomingNumber, Toast.LENGTH_LONG).show();
					callState = "Local - Ringing (" + incomingNumber + ")";
				}*/
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				String dialingNumber = mIntent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				/*if(dialingNumber == null) break;
				if (dialingNumber.startsWith("00")) {
					Toast.makeText(mContext,"International - " + dialingNumber,Toast.LENGTH_LONG).show();
					callState = "International - Dialing (" + dialingNumber+ ")";
				} else {
					Toast.makeText(mContext, "Local Call - " + dialingNumber,Toast.LENGTH_LONG).show();
					callState = "Local - Dialing (" + dialingNumber + ")";
				}*/
				break;
			}
			Log.i(">>>Broadcast", "onCallStateChanged " + callState);
			super.onCallStateChanged(state, incomingNumber);
		}
	};
}
