package com.example.projekatbase;

import android.view.View;

public class ButtonListener implements View.OnClickListener
{

    MainActivity main;
    public ButtonListener(MainActivity main) {
        this.main = main;
    }

    @Override
    public void onClick(View v)
    {
        this.main.sumAvg[0] = 0.0;
        this.main.sumAvg[1] = 0.0;
        this.main.averages.clear();
        this.main.numOfAvgs = 0;
        this.main.locationUpdate();

    }

};