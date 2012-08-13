package com.st.nyamnew.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.st.nyamnew.services.ConstantSynchronizationService;

public class SyncBroadcastReceier extends BroadcastReceiver {
	
	private String TAG ="SyncBroadcastReceier";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
	    Intent startServiceIntent = new Intent(context, ConstantSynchronizationService.class);
	    context.startService(startServiceIntent);
	}

}
