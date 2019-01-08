package cn.ieclipse.pay.union;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.unionpay.UPPayAssistEx;

import cn.ieclipse.pay.wxpay.HttpsUtils;

/**
 * Description
 *
 * @author Jamling
 */

public class UnionPay {
    public static final String TEST_TN_URL = "http://101.231.204.84:8091/sim/getacptn";
    public static boolean DEBUG = false;
    public static final String TAG = "pay_sdk";
    private static UnionPay instance;
    private Context context;
    private String spId;
    private String provider;
    private String serverMode;
    private String tn;

    public static void log(String msg) {
        Log.v(TAG, msg);
    }

    private UnionPay(Context context) {
        this.context = context;
    }

    public static UnionPay getInstance(Context context) {
        if (instance == null) {
            instance = new UnionPay(context);
        }
        return instance;
    }

    public void pay(String tn, String serverMode) {
        pay(null, null, tn, serverMode);
    }

    public void pay(String spId, String sysProvider, String tn, String serverMode) {
        try {
            Class.forName("com.unionpay.UPPayAssistEx");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未集成银联支付SDK，请参考https://github.com/Jamling/af-pay 说明文档");
        }
        boolean check = "00".equals(serverMode) || "01".equals(serverMode);
        if (!check) {
            throw new IllegalArgumentException("serverMode参数错误");
        }
        this.spId = spId;
        this.provider = sysProvider;
        this.tn = tn;
        this.serverMode = serverMode;
        context.startActivity(new Intent(context, UnionPayActivity.class));
    }

    void handleIntent(Intent intent, UnionPayActivity unionPayActivity) {
        UPPayAssistEx.startPay(unionPayActivity, spId, provider, tn, serverMode);
    }

    void onResp(Intent data) {
        PayResult result = new PayResult(data);
        if (DEBUG) {
            log("支付结果：" + result);
        }
        if (payListener != null) {
            if ("success".equals(result.getStatus())) {
                payListener.onPaySuccess(result);
            }
            else if ("cancel".equals(result.getStatus())) {
                payListener.onPayCancel(result);
            }
            else {
                payListener.onPayFailure(result);
            }
        }
    }

    private PayListener payListener;

    public void setPayListener(PayListener payListener) {
        this.payListener = payListener;
    }

    public interface PayListener {
        /**
         * 支付成功
         *
         * @param payResult
         */
        void onPaySuccess(PayResult payResult);

        /**
         * 支付取消
         *
         * @param payResult
         */
        void onPayCancel(PayResult payResult);

        /**
         * 支付失败
         *
         * @param payResult
         */
        void onPayFailure(PayResult payResult);
    }

    /**
     * 仅供测试环境下使用，获取测试的TN
     */
    public static class DefaultTnTask extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog;
        private UnionPay unionPay;

        public DefaultTnTask(UnionPay unionPay) {
            this.unionPay = unionPay;
        }

        @Override
        protected void onPreExecute() {
            try {
                dialog = ProgressDialog.show(unionPay.context, "提示", "正在下单，请稍候...");
            } catch (Exception e) {
                UnionPay.log("弹出下单提示框失败\n" + e.toString());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (dialog != null) {
                dialog.dismiss();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (DEBUG) {
                log("获取TN url: " + TEST_TN_URL);
            }
            byte[] buf = HttpsUtils.post(TEST_TN_URL, "");
            if (buf == null) {
                return "";
            }
            String tn = new String(buf);
            if (DEBUG) {
                log("返回TN: " + tn);
            }
            return tn;
        }
    }
}
