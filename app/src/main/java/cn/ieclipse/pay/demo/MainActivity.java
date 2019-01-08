package cn.ieclipse.pay.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;

import java.util.Map;

import cn.ieclipse.pay.alipay.Alipay;
import cn.ieclipse.pay.alipay.OrderInfoUtil2_0;
import cn.ieclipse.pay.alipay.PayResult;
import static cn.ieclipse.pay.demo.R.id.tv1;
import static cn.ieclipse.pay.demo.R.id.tv2;
import static cn.ieclipse.pay.demo.R.id.tv3;
import cn.ieclipse.pay.union.UnionPay;
import cn.ieclipse.pay.wxpay.OrderInfoUtil;
import cn.ieclipse.pay.wxpay.Wxpay;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView tv01;
    private TextView tv02;
    private TextView tv03;
    private EditText et1;
    private TextView tv11;
    private TextView tv12;
    private TextView tv13;
    private TextView btn1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv01 = findViewById(tv1);
        tv02 = findViewById(tv2);
        tv03 = findViewById(tv3);
        et1 = findViewById(R.id.et1);
        tv11 = findViewById(R.id.tv11);
        tv12 = findViewById(R.id.tv12);
        tv13 = findViewById(R.id.tv13);
        btn1 = findViewById(R.id.btn1);

        tv01.setOnClickListener(this);
        tv02.setOnClickListener(this);
        tv03.setOnClickListener(this);
        tv11.setOnClickListener(this);
        tv12.setOnClickListener(this);
        tv13.setOnClickListener(this);
        btn1.setOnClickListener(this);

        setTitle("支付示例");
    }

    @Override
    public void onClick(View v) {
        if (v == tv01) {
            doAlipay(null);
        }
        else if (v == tv02) {
            doWxpay(null);
        }
        else if (v == tv11) {
            String info = et1.getText().toString().trim();
            doAlipay(info);
        }
        else if (v == tv12) {
            String info = et1.getText().toString().trim();
            doWxpay(info);
        }
        else if (v == tv03) {
            String info = et1.getText().toString().trim();
            doUnionPay(null);
        }
        else if (v == tv13) {
            String info = et1.getText().toString().trim();
            doUnionPay(info);
        }
        else if (v == btn1) {
            startActivity(new Intent(this, ResourceActivity.class));
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

    private void doUnionPay(String orderInfo) {
        final Activity activity = this;
        UnionPay.DEBUG = true; // 开启日志
        final UnionPay unionPay = UnionPay.getInstance(activity);
        unionPay.setPayListener(new UnionPay.PayListener() {
            @Override
            public void onPaySuccess(cn.ieclipse.pay.union.PayResult payResult) {
                showToast(activity, "支付成功");
            }

            @Override
            public void onPayCancel(cn.ieclipse.pay.union.PayResult payResult) {
                showToast(activity, "支付取消");
            }

            @Override
            public void onPayFailure(cn.ieclipse.pay.union.PayResult payResult) {
                showToast(activity, "支付失败");
            }
        });
        if (!TextUtils.isEmpty(orderInfo)) {// 正式环境
            unionPay.pay(orderInfo, "00");
        }
        else {
            UnionPay.DefaultTnTask tnTask = new UnionPay.DefaultTnTask(unionPay) {
                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    unionPay.pay(result, "01"); // 测试
                }
            };
            tnTask.execute();
        }
    }

    private void showToast(Activity activity, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    //-->
    // 招商银行借记卡：6226090000000048
    // 手机号：18100000000
    // 密码：111101
    // 短信验证码：123456（先点获取验证码之后再输入）
    // 证件类型：01身份证
    // 证件号：510265790128303
    // 姓名：张三
    //-->

    //华夏银行贷记卡：6226388000000095
    // 手机号：18100000000
    // cvn2：248
    // 有效期：1219
    // 短信验证码：111111（先点获取验证码之后再输入）
    // 证件类型：01身份证
    // 证件号：510265790128303
    // 姓名：张三
}
