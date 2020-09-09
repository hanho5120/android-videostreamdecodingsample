package com.dji.videostreamdecodingsample;

import android.app.Application;
import android.content.Context;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

public class MApplication extends Application {

    private static BaseProduct mProduct;

    public static synchronized BaseProduct getProductInstance()
    {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }



    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        com.secneo.sdk.Helper.install(MApplication.this);
    }
}
