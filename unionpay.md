# 银联支付

## 添加依赖库

```gradle
    dependencies {
        compile 'cn.ieclipse.aar-ref:unionpay:3.5.4'
    }
```
## 示例代码

类似支付宝支付，与支付宝不同的是，银联必须在服务端下单，然后通过服务端返回的TN来调起支付。

```java
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
```

## 测试账号

```
// 华夏银行贷记卡：6226388000000095
// 手机号：18100000000
// cvn2：248
// 有效期：1219
// 短信验证码：111111（先点获取验证码之后再输入）
// 证件类型：01身份证
// 证件号：510265790128303
// 姓名：张三
```
