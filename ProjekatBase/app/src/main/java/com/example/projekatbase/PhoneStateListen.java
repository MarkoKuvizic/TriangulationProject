package com.example.projekatbase;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

public class PhoneStateListen extends PhoneStateListener {

    public MainActivity m;
    private int t = 500;
    private LocationTask lt = new LocationTask();

    public PhoneStateListen(MainActivity m){
        super();
        this.m = m;
    }



    public void onSignalStrengthsChanged(SignalStrength ss) {

//        super.onSignalStrengthsChanged(ss);

        if(t < 0) {
            this.m.locationUpdate();
            t = 500;
        }else{
            t-=1;

        }
        this.lt.doInBackground(this.m.getPhoneSTL());

    }

    public MainActivity getM(){
        return this.m;
    }

}
