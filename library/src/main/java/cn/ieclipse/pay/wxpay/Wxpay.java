/*
 * Copyright (C) 2015-2017 HongTu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ieclipse.pay.wxpay;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mm.sdk.constants.Build;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.modelpay.PayResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Description
 *
 * @author Jamling
 */
public class Wxpay {
    public static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    public static boolean DEBUG = false;
    public static final String TAG = "pay_sdk";
    private Activity context;
    private IWXAPI mWXApi;
    private static Wxpay instance;

    public static void log(String msg) {
        Log.v(TAG, msg);
    }

    private Wxpay(Activity context) {
        this.context = context;
        if (mWXApi == null) {
            mWXApi = WXAPIFactory.createWXAPI(context, Config.app_id);
            mWXApi.registerApp(Config.app_id);
        }
    }

    public static Wxpay getInstance(Activity context) {
        if (instance == null) {
            instance = new Wxpay(context);
        }
        return instance;
    }

    boolean handleIntent(Intent intent, IWXAPIEventHandler eventHandler) {
        return mWXApi.handleIntent(intent, eventHandler);
    }

    public boolean isSupportPay() {
        return mWXApi.isWXAppInstalled() && mWXApi.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
    }

    public void pay(PayReq req) {
        if (isSupportPay()) {
            mWXApi.sendReq(req);
        }
        else {
            if (payListener != null ) {
                PayResp resp = new PayResp();
                resp.errCode = BaseResp.ErrCode.ERR_UNSUPPORT;
                payListener.onPayFailure(resp);
            }
        }
    }

    public void pay(String xml) {
        if (!TextUtils.isEmpty(xml)) {
            PayReq req = OrderInfoUtil.getPayReq(xml);
            pay(req);
        }
    }

    public void onResp(BaseResp baseResp) {
        int code = baseResp.errCode;
        if (payListener != null) {
            if (code == BaseResp.ErrCode.ERR_OK) {
                payListener.onPaySuccess(baseResp);
            }
            else if (code == BaseResp.ErrCode.ERR_USER_CANCEL) {
                payListener.onPayCanceled(baseResp);
            }
            else {
                payListener.onPayFailure(baseResp);
            }
        }
    }

    private PayListener payListener;

    public void setPayListener(PayListener payListener) {
        this.payListener = payListener;
    }

    public interface PayListener {
        void onPaySuccess(BaseResp resp);

        void onPayCanceled(BaseResp resp);

        void onPayFailure(BaseResp resp);
    }

    public static class Config {
        /**
         * 微信appid
         */
        public static String app_id;
        /**
         * 商户号
         */
        public static String mch_id;
        /**
         * API密钥，在商户平台设置
         */
        public static String api_key;

        /**
         * 服务器异步通知页面路径
         */
        public static String notify_url;
    }

    public static class DefaultOrderTask extends AbsUnifiedOrderTask {

        private ProgressDialog dialog;
        private Wxpay wxpay;

        public DefaultOrderTask(Wxpay wxpay) {
            this.wxpay = wxpay;
        }

        @Override
        protected void onPreExecute() {
            try {
                dialog = ProgressDialog.show(wxpay.context, "提示", "正在下单，请稍候...");
            } catch (Exception e) {
                Wxpay.log("弹出下单提示框失败\n" + e.toString());
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (dialog != null) {
                dialog.dismiss();
            }

            if (getPayReq() != null) {
                wxpay.pay(getPayReq());
            }
            else {
                Toast.makeText(wxpay.context, "下单失败！", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static abstract class AbsUnifiedOrderTask extends AsyncTask<Void, Void, Void> {

        private PayReq req;
        private Map<String, String> params;

        public PayReq getPayReq() {
            return req;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // see https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=9_1
            String url = UNIFIED_ORDER_URL;// 统一下单
            String body = getRequest(this.params, false);
            if (DEBUG) {
                Wxpay.log("下单请求xml为：\n" + body);
            }
            String content = getResponse(url, body);
            if (DEBUG) {
                Wxpay.log("下单响应xml为：\n" + content);
            }
            req = OrderInfoUtil.getPayReq(content);
            return null;
        }

        protected String getRequest(Map<String, String> params, boolean signed) {
            if (!signed) {
                params.put("sign", OrderInfoUtil.genSign(params));
            }
            return map2xmlStr(params);
        }

        protected String map2xmlStr(Map<String, String> params) {
            StringBuffer sb = new StringBuffer();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<xml>");
            Set es = params.entrySet();
            Iterator it = es.iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if ("attach".equalsIgnoreCase(key) || "body".equalsIgnoreCase(key) || "detail".equalsIgnoreCase(key)) {
                    sb.append("<" + key + ">" + "<![CDATA[" + value + "]]></" + key + ">");
                }
                else {
                    sb.append("<" + key + ">" + value + "</" + key + ">");
                }
            }
            sb.append("</xml>");
            String xml = "";
            try {
                xml = new String(sb.toString().getBytes(), "ISO8859-1");
            } catch (Exception e) {
                if (Wxpay.DEBUG) {
                    Wxpay.log(e.getMessage());
                }
            }
            return xml;
        }

        //-----------> Response

        protected String getResponse(String url, String body) {
            byte[] buf = Util.httpPost(url, body);
            return new String(buf);
        }
    }
}
