package cn.ieclipse.pay.wxsample;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;

import org.json.JSONObject;

import cn.ieclipse.pay.alipay.OrderInfoUtil2_0;
import cn.ieclipse.pay.wxpay.HttpsUtils;
import cn.ieclipse.pay.wxpay.OrderInfoUtil;
import cn.ieclipse.pay.wxpay.Wxpay;

public class MainActivity extends Activity implements View.OnClickListener {

    TextView btn1;
    TextView btn2;
    TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn1 = findViewById(R.id.btn1);
        btn1.setOnClickListener(this);

        btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(this);
        tv1 = findViewById(R.id.tv1);
    }

    @Override
    public void onClick(View v) {
        if (v == btn1) {
            doOrder();
        }
        else if (v == btn2) {
            goPay();
        }
    }

    private void doOrder() {
        Wxpay.AbsUnifiedOrderTask task = new Wxpay.AbsUnifiedOrderTask() {
            private String jsonstring;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    byte[] bytes = HttpsUtils.post(Wxpay.TEST_ORDER_URL, "");
                    if (bytes != null) {
                        jsonstring = new String(bytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                tv1.setText(jsonstring);
                //doWxpay(req);
                if (TextUtils.isEmpty(jsonstring)) {
                    showToast(MainActivity.this, "下单失败，请查看日志输出");
                }
            }
        };
        task.execute();
    }

    private void goPay() {
        if (TextUtils.isEmpty(tv1.getText())) {
            showToast(this, "请先下单");
            return;
        }
        try {
            JSONObject json = new JSONObject(tv1.getText().toString());
            PayReq req = new PayReq();
            req.appId = "wxf8b4f85f3a794e77";
            req.appId = json.getString("appid");
            req.partnerId = json.getString("partnerid");
            req.prepayId = json.getString("prepayid");
            req.nonceStr = json.getString("noncestr");
            req.timeStamp = json.getString("timestamp");
            req.packageValue = json.getString("package");
            req.sign = json.getString("sign");
            doWxpay(req);
        } catch (Exception e) {
            showToast(this, e.toString());
        }
    }

    private void doWxpay(PayReq req) {
        final Activity activity = this;
        Wxpay.DEBUG = true; // 开启日志
        Wxpay.init(getApplicationContext(), req.appId, Wxpay.Config.checkSignature);
        // step 2: 获取支付类
        Wxpay wxpay = Wxpay.getInstance(activity);
        // step 3: 设置支付回调监听
        wxpay.setPayListener(new Wxpay.PayListener() {
            @Override
            public void onPaySuccess(BaseResp resp) {
                showToast(activity, "支付成功");
            }

            @Override
            public void onPayCanceled(BaseResp resp) {
                showToast(activity, "支付取消");
            }

            @Override
            public void onPayFailure(BaseResp resp) {
                showToast(activity, "支付失败");
            }
        });
        wxpay.pay(req);
    }

    private void doWxpay(String orderInfo) {
        final Activity activity = this;
        Wxpay.DEBUG = true; // 开启日志
        Wxpay.Config.checkSignature = false; // 0.0.3版本，微信SDK将此参数默认值改为了true，不过本sdk仍然使用默认的false
        Wxpay.Config.app_id = "";
        // step 1: 初始化, 推荐在Application#onCreate()方法中初始化
        Wxpay.init(getApplicationContext(), Wxpay.Config.app_id, Wxpay.Config.checkSignature);

        // step 2: 获取支付类
        Wxpay wxpay = Wxpay.getInstance(activity);
        // step 3: 设置支付回调监听
        wxpay.setPayListener(new Wxpay.PayListener() {
            @Override
            public void onPaySuccess(BaseResp resp) {
                showToast(activity, "支付成功");
            }

            @Override
            public void onPayCanceled(BaseResp resp) {
                showToast(activity, "支付取消");
            }

            @Override
            public void onPayFailure(BaseResp resp) {
                showToast(activity, "支付失败");
            }
        });
        // step 4: 调用支付
        // 这里是服务端下单，内容是统一下单返回的xml
        if (!TextUtils.isEmpty(orderInfo)) {
            PayReq req = OrderInfoUtil.getPayReq(orderInfo);
            wxpay.pay(req);
            // TODO 如果服务端下单，但是返回的是json，需要自己生成一个PayReq对象。
        }
        else { // 客户端下单，需要补全Wxpay.Config中的其它参数
            // API密钥，在微信商户平台设置
            Wxpay.Config.api_key = "";
            // APPID，在微信开放平台创建应用后生成
            Wxpay.Config.app_id = "";
            // 商户ID，注册商户平台后生成
            Wxpay.Config.mch_id = "";
            Wxpay.Config.api_key = "";
            Wxpay.Config.app_id = "";
            Wxpay.Config.mch_id = "";
            // 支付结果异步通知接口，由后台开发提供
            Wxpay.Config.notify_url = "http://www.ieclipse.cn/app/pay/wxpay_notify.do";
            // 创建统一下单异步任务
            Wxpay.DefaultOrderTask task = new Wxpay.DefaultOrderTask(wxpay);
            // 这个商户订单号，由后台返回，在这里随便生成一个
            String outTradeNo = OrderInfoUtil2_0.genOutTradeNo();
            // 设置统一下单的请求参数
            task.setParams(OrderInfoUtil.buildOrderParamMap(outTradeNo, "测试支付", "", "1", null, null, null));
            task.execute();
        }
    }

    private void showToast(Activity activity, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
