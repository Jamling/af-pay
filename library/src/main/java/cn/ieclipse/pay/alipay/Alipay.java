/*
 * Copyright (C) 2015-2016 QuickAF
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
package cn.ieclipse.pay.alipay;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alipay.sdk.app.PayTask;

import java.util.Map;

import static cn.ieclipse.pay.alipay.Alipay.Config.appId;
import static cn.ieclipse.pay.alipay.Alipay.Config.partner;
import static cn.ieclipse.pay.alipay.Alipay.Config.rsa2_private;
import static cn.ieclipse.pay.alipay.Alipay.Config.rsa_private;
import static cn.ieclipse.pay.alipay.Alipay.Config.rsa_public;
import static cn.ieclipse.pay.alipay.Alipay.Config.seller;
import cn.ieclipse.pay.wxpay.Wxpay;

public class Alipay {
    public static final int VERSION_1 = 1;
    public static final int VERSION_2 = 2;
    public static final String TAG = Wxpay.TAG;
    public static boolean DEBUG = false;
    private Activity context;
    private int version;
    private PayListener mPayListener;
    private AuthListener mAuthListener;

    public Alipay(Activity activity, int version) {
        this.context = activity;
        this.version = version;
    }

    public Alipay(Activity activity) {
        this(activity, VERSION_2);
    }

    public static void log(String msg) {
        Log.v(TAG, msg);
    }

    public interface PayListener {
        /**
         * 支付成功
         *
         * @param payResult
         */
        void onPaySuccess(PayResult payResult);

        /**
         * 支付等待中...
         *
         * @param payResult
         */
        void onPayWaiting(PayResult payResult);

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

    public interface AuthListener {
        void onAuthSuccess(AuthResult result);

        void onAuthFailure(AuthResult result);
    }

    /**
     * add pay call back listener
     *
     * @param listener
     */
    public void setPayListener(PayListener listener) {
        mPayListener = listener;
    }

    public void setAuthListener(AuthListener authListener) {
        this.mAuthListener = authListener;
    }

    @SuppressWarnings("unchecked")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Config.SDK_PAY_FLAG: {
                    PayResult payResult;
                    if (msg.arg1 == VERSION_1) {
                        payResult = new PayResult((String) msg.obj);
                    }
                    else {
                        payResult = new PayResult((Map<String, String>) msg.obj);
                    }
                    /**
                     对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    // 可以拿支付宝公钥验签
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();

                    if (DEBUG) {
                        Alipay.log("支付结果:" + payResult);
                    }
                    if (mPayListener != null) {
                        // 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
                        if (TextUtils.equals(resultStatus, "9000")) {
                            mPayListener.onPaySuccess(payResult);
                        }
                        else {
                            // 判断resultStatus 为非“9000”则代表可能支付失败
                            // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                            if (TextUtils.equals(resultStatus, "8000")) {
                                //支付结果确认中
                                mPayListener.onPayWaiting(payResult);
                            }
                            else if (TextUtils.equals(resultStatus, "6001")) {
                                // 其他值就可以判断为支付失败，包括用户主动取消支付，或者系统返回的错误
                                mPayListener.onPayCancel(payResult);
                            }
                            else {
                                mPayListener.onPayFailure(payResult);
                            }
                        }
                    }
                    break;
                }
                case Config.SDK_AUTH_FLAG: {
                    AuthResult authResult = new AuthResult((Map<String, String>) msg.obj, true);
                    String resultStatus = authResult.getResultStatus();
                    // 判断resultStatus 为“9000”且result_code
                    // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
                    if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
                        // 获取alipay_open_id，调支付时作为参数extern_token 的value
                        // 传入，则支付账户为该授权账户
                        if (DEBUG) {
                            Alipay.log("授权成功\n" + String.format("authCode:%s", authResult.getAuthCode()));
                        }
                    }
                    else {
                        // 其他状态值则为授权失败
                        Alipay.log("授权失败\n" + String.format("authCode:%s", authResult.getAuthCode()));
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * call alipay sdk pay. 调用SDK支付
     */
    public void payV1(final String payInfo) {
        Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                // 构造PayTask 对象
                PayTask alipay = new PayTask(context);
                // 调用支付接口，获取支付结果
                String result = alipay.pay(payInfo, true);// true 在调起支付页面之前显示进度条

                Message msg = new Message();
                msg.what = Config.SDK_PAY_FLAG;
                msg.arg1 = VERSION_1;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    public void payV2(final String orderInfo) {
        Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                PayTask alipay = new PayTask(context);
                Map<String, String> result = alipay.payV2(orderInfo, true);

                Message msg = new Message();
                msg.what = Config.SDK_PAY_FLAG;
                msg.arg1 = VERSION_2;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    /**
     * check whether the device has authentication alipay account.
     * 查询终端设备是否存在支付宝认证账户
     */
//    public void check() {
//        Runnable checkRunnable = new Runnable() {
//
//            @Override
//            public void run() {
//                // 构造PayTask 对象
//                PayTask payTask = new PayTask(context);
//                // 调用查询接口，获取查询结果
//                boolean isExist = payTask.checkAccountIfExist();
//
//                Message msg = new Message();
//                msg.what = AlipayConfig.SDK_CHECK_FLAG;
//                msg.obj = isExist;
//                mHandler.sendMessage(msg);
//            }
//        };
//
//        Thread checkThread = new Thread(checkRunnable);
//        checkThread.start();
//    }
    public static boolean isRSA2() {
        String rsa = rsa2_private;
        boolean rsa2 = false;
        if (!TextUtils.isEmpty(rsa2_private)) {
            rsa = rsa2_private;
            rsa2 = true;
        }
        return rsa2;
    }

    public boolean check() {
        if (version == VERSION_2) {
            return checkV2();
        }
        else {
            return checkV1();
        }
    }

    private static boolean checkV1() {
        return !TextUtils.isEmpty(seller) && !TextUtils.isEmpty(rsa_public) && !TextUtils.isEmpty(partner) && !(
            TextUtils.isEmpty(rsa2_private) && TextUtils.isEmpty(rsa2_private));
    }

    private static boolean checkV2() {
        return !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(rsa_public) && !TextUtils.isEmpty(partner) && !(
            TextUtils.isEmpty(rsa2_private) && TextUtils.isEmpty(rsa2_private));
    }

    public static String sign(String content) {
        boolean rsa2 = isRSA2();
        String privateKey = rsa2 ? rsa2_private : rsa_private;
        return SignUtils.sign(content, privateKey, rsa2);
    }

    public static class Config {

        public static final int SDK_PAY_FLAG = 1;

        public static final int SDK_AUTH_FLAG = 2;

        /**
         * 商户PID
         */
        public static String partner = "2088202379311396";

        /**
         * 2.0 新增，需在支付宝开放平台注册应用
         */
        public static String appId = "";

        /**
         * 商户收款账号
         */
        public static String seller = "";
        /** 商户私钥，pkcs8格式 */
        /** 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个 */
        /** 如果商户两个都设置了，优先使用 RSA2_PRIVATE */
        /** RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议使用 RSA2_PRIVATE */
        /** 获取 RSA2_PRIVATE，建议使用支付宝提供的公私钥生成工具生成， */
        /** 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1 */
        /**
         * 商户私钥，pkcs8格式
         */
        public static String rsa_private = "";

        /**
         * 2.0新增，推荐使用RSA2 2048位加密
         */
        public static String rsa2_private = "";

        /**
         * 支付宝公钥
         */
        public static String rsa_public = "";
        /**
         * 服务器异步通知页面路径
         */
        public static String notify_url;
    }
}
