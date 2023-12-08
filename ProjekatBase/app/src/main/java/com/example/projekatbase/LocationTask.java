package com.example.projekatbase;

import android.os.AsyncTask;
import android.util.Log;

public class LocationTask extends AsyncTask<PhoneStateListen, Void, Void> {

    @Override
    protected Void doInBackground(PhoneStateListen... psl) {

//        psl[0].getM().getTel().listen(psl[0], PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        Log.d("TAG", "doInBackground: ");
        psl[0].getM().locationUpdate();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
