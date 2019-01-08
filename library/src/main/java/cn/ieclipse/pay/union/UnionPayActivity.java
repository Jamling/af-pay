package cn.ieclipse.pay.union;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Description
 *
 * @author Jamling
 */

public class UnionPayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (UnionPay.getInstance(this) != null) {
            UnionPay.getInstance(this).handleIntent(getIntent(), this);
        }
        else {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (UnionPay.getInstance(this) != null) {
            UnionPay.getInstance(this).handleIntent(intent, this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*************************************************
         * 步骤3：处理银联手机支付控件返回的支付结果
         ************************************************/
        UnionPay.getInstance(this).onResp(data);
        finish();
    }
}
