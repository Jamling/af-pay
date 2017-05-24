package cn.ieclipse.pay.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;

import java.util.Map;

import cn.ieclipse.pay.alipay.Alipay;
import cn.ieclipse.pay.alipay.OrderInfoUtil2_0;
import cn.ieclipse.pay.alipay.PayResult;
import cn.ieclipse.pay.wxpay.OrderInfoUtil;
import cn.ieclipse.pay.wxpay.Wxpay;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView tv1;
    private TextView tv2;
    private EditText et1;
    private Button tv3;
    private Button tv4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        et1 = (EditText) findViewById(R.id.et1);
        tv3 = (Button) findViewById(R.id.tv3);
        tv4 = (Button) findViewById(R.id.tv4);

        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
        tv4.setOnClickListener(this);

        setTitle("支付示例");
    }

    @Override
    public void onClick(View v) {
        if (v == tv1) {
            doAlipay(null);
        }
        else if (v == tv2) {
            doWxpay(null);
        }
        else if (v == tv3) {
            String info = et1.getText().toString().trim();
            doAlipay(info);
        }
        else if (v == tv4) {
            String info = et1.getText().toString().trim();
            doWxpay(info);
        }
    }

    private void doAlipay(String orderInfo) {

        final Activity activity = this;
        Alipay alipay = new Alipay(activity);
        alipay.setPayListener(new Alipay.PayListener() {
            @Override
            public void onPaySuccess(PayResult payResult) {
                showToast(activity, "支付成功");
            }

            @Override
            public void onPayWaiting(PayResult payResult) {
                showToast(activity, "支付结果确认中...");
            }

            @Override
            public void onPayCancel(PayResult payResult) {
                showToast(activity, "您已取消支付");
            }

            @Override
            public void onPayFailure(PayResult payResult) {
                showToast(activity, "支付失败\n" + payResult.getMemo());
            }
        });

        if (TextUtils.isEmpty(orderInfo)) {
            // set v1 config
            Alipay.DEBUG = true;
            Alipay.Config.appId = "";
            Alipay.Config.rsa_private = "";
            Alipay.Config.rsa_public = "";
            Alipay.Config.notify_url = "app/pay/alipay_notify.do";

            if (!alipay.check()) {
                showToast(activity, "缺少配置，无法支付");
                return;
            }

            String trans_order_id = OrderInfoUtil2_0.genOutTradeNo();
            Map<String, String> map = OrderInfoUtil2_0.buildOrderParamMap(trans_order_id, "测试支付", "测试商品1，测试商品2",
                String.valueOf(0.01f), null);
            orderInfo = OrderInfoUtil2_0.getOrderInfo(map);

            alipay.payV1(orderInfo);
        }
        else {
            alipay.payV2(orderInfo);
        }
    }

    private void doWxpay(String orderInfo) {
        final Activity activity = this;
        // 获取支付类
        Wxpay wxpay = Wxpay.getInstance(activity);
        // 设置支付回调监听
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
        // 这里是服务端下单，内容是统一下单返回的xml
        if (!TextUtils.isEmpty(orderInfo)) {
            PayReq req = OrderInfoUtil.getPayReq(orderInfo);
            wxpay.pay(req);
        }
        else { // 客户端下单
            Wxpay.DEBUG = true; // 开启日志
            // API密钥，在微信商户平台设置
            Wxpay.Config.api_key = "";
            // APPID，在微信开放平台创建应用后生成
            Wxpay.Config.app_id = "";
            // 商户ID，注册商户平台后生成
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
