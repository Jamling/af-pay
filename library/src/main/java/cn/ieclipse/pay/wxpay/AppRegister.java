package cn.ieclipse.pay.wxpay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class AppRegister extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Wxpay.DEBUG) {
            Wxpay.log("onReceive: " + intent);
        }
        final IWXAPI api = WXAPIFactory.createWXAPI(context, null);
        api.registerApp(Wxpay.Config.app_id);
    }
}
