# af-pay
[![Build Status](https://travis-ci.org/Jamling/af-pay.svg?branch=master)](https://travis-ci.org/Jamling/af-pay)
[![GitHub release](https://img.shields.io/github/release/jamling/af-pay.svg?maxAge=3600)](https://github.com/Jamling/af-pay)
[![Bintray](https://img.shields.io/bintray/v/jamling/maven/af-pay.svg?maxAge=86400)](https://bintray.com/jamling/maven/af-pay)
[![Jitpack](https://jitpack.io/v/Jamling/af-pay.svg)](https://jitpack.io/#Jamling/af-pay)

af-pay原为[QuickAF]（一个快速的Android开发框架）中的支付组件，现已抽取出来作为一个单独的Android支付库, 支持支付宝，微信支付，并且同时支持客户端下单与服务端下单。

0.2.0及其以上版本支付银联支付，如需使用银联支付，[请点击这里](unipay.md)。

新的版本（0.1.0及其以上）没有提供纯净版，如果您使用的是0.1以下版本，请移步[旧文档](README_old.md)

### 在Android中直接使用

- 引入完整版本，包含支付平台相关的jar包
```gradle
    dependencies {
        compile 'cn.ieclipse.af:af-pay:0.1.0'
    }
```

### 使用jitpack.io仓库

1，在工程根目录中的build.gradle中添加jitpack仓库

```gradle
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

2，在app模块中添加依赖

```gradle
    dependencies {
        compile 'com.github.Jamling:af-pay:master-SNAPSHOT'
    }
```

## 常见问题

因支付宝SDK的最低SDK版本改为了16，如果app模块最低仍为14，需要在AndroidManifest.xml中，添加以下声明

```xml
<uses-sdk tools:overrideLibrary="com.alipay.sdk,cn.ieclipse.pay"/>
```

## 示例代码
### 支付宝支付
客户端下单方式同时支持支付宝sdk v1和2，建议使用v2。服务端下单方式仅支持v2。如果申请了支付宝支付，可以修改相关配置，然后运行本demo，可以支付成功。
```java
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
```

### 微信支付
```java
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
        Wxpay.Config.api_key = "32位的字串";
        // APPID，在微信开放平台创建应用后生成
        Wxpay.Config.app_id = "wx...";
        // 商户ID，注册商户平台后生成
        Wxpay.Config.mch_id = "14...";
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
```

## 版本记录

### 0.2.0
- 支付银联支付 (aar sdk 3.5.4)
- 添加一些设计资源

### 0.1.0
- 使用支付宝最新的aar sdk 15.5.9
- 使用微信支付sdk 5.1.6

### 0.0.2
- 微信依赖改为`com.tencent.mm.opensdk:wechat-sdk-android-without-mta:+`

## 说明

- 因本demo未申请支付宝支付和微信支付，所以在示例中无法支付成功。
- af-pay原来是包含在[QuickAF]中，建议使用[QuickAF]的同学们更新依赖。
- 日志tag为`pay_sdk`，可以设置`Wxpay.DEUBG = true`或`Alipay.DEUBG = true`来开启日志。
- 更多请加入![QQ群: 629153672](http://dl.ieclipse.cn/screenshots/quickaf_group.png)

[QuickAF]: https://github.com/Jamling/QuickAF
