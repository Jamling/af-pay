# af-pay
[![Build Status](https://travis-ci.org/Jamling/af-pay.svg?branch=master)](https://travis-ci.org/Jamling/af-pay)
[![GitHub release](https://img.shields.io/github/release/jamling/af-pay.svg?maxAge=3600)](https://github.com/Jamling/af-pay)
[![Bintray](https://img.shields.io/bintray/v/jamling/maven/af-pay.svg?maxAge=86400)](https://bintray.com/jamling/maven/af-pay)

Af-pay原为[QuickAF]（一个快速的Android开发框架）中的支付组件，现已抽取出来作为一个单独的Android支付库, 支持支付宝，微信支付，并且同时支持客户端下单与服务端下单。

## 引入
**注：如果不想引入支付依赖的jar，请使用`pure`分支或版本**

*`pure`版本不包含任何支付平台的相关jar包，如果您的工程中原来已经包含了如微信分享（libammsdk.jar）jar，则会出现冲突，导致打包失败。建议引入纯净版*

### 在Android中直接使用

- 引入完整版本，包含支付平台相关的jar包
```gradle
    dependencies {
        compile 'cn.ieclipse.af:af-pay:0.0.1'
    }
```
- 引入纯净版本
```gradle
    dependencies {
        compile 'cn.ieclipse.af:af-pay-pure:0.0.1'
    }
```

### 使用jitpack.io仓库

1，要工程根目录中的build.gradle中添加jitpack仓库

```gradle
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

2，在app模块中添加依赖

- 使用完整版本
```gradle
    dependencies {
        compile 'com.github.Jamling:af-pay:master-SNAPSHOT'
    }
```
- 使用`pure`版本
```gradle
    dependencies {
        compile 'com.github.Jamling:af-pay:pure-SNAPSHOT'
    }
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
        Wxpay wxpay = Wxpay.getInstance(activity);
        wxpay.setPayListener(new cn.ieclipse.pay.wxpay.Wxpay.PayListener() {
            @Override
            public void onPaySuccess(BaseResp resp) {
                showToast(activity, "支付成功：" + resp.errStr);
            }
            
            @Override
            public void onPayCanceled(BaseResp resp) {
                showToast(activity, "支付取消");
            }
            
            @Override
            public void onPayFailure(BaseResp resp) {
                showToast(activity, "支付失败：" + resp.errStr);
            }
        });
        if (!TextUtils.isEmpty(orderInfo)) {
            PayReq req = OrderInfoUtil.getPayReq(orderInfo);
            wxpay.pay(req);
        }
        else {
            Wxpay.DEBUG = true;
            Wxpay.Config.api_key = "";
            Wxpay.Config.app_id = "";
            Wxpay.Config.mch_id = "";
            Wxpay.Config.notify_url = "app/pay/wxpay_notify.do";
            
            Wxpay.DefaultOrderTask task = new Wxpay.DefaultOrderTask(wxpay);
            String trans_order_id = OrderInfoUtil2_0.genOutTradeNo();
            task.setParams(OrderInfoUtil.buildOrderParamMap(trans_order_id, "测试支付", "", "1", null, null, null));
            task.execute();
        }
    }
```

## 说明

- 因本demo未申请支付宝支付和微信支付，所以在demo中无法支付成功。
- 如果项目中已经包含或依赖的第三方库中已包含libammsdk.jar（微信sdk），在引入af-pay后，出现因jar版本不致导致编译不通过，建议引入`pure`分支版本。
- af-pay原来是包含在[QuickAF]中，建议使用[QuickAF]的同学们更新依赖。
- 日志tag为`pay_sdk`，可以设置`Wxpay.DEUBG = true`或`Alipay.DEUBG = true`来开启日志。

[QuickAF]: https://github.com/Jamling/QuickAF