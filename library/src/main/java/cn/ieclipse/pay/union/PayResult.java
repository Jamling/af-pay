package cn.ieclipse.pay.union;

import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Description
 *
 * @author Jamling
 */

public class PayResult {
    private String status = "Unknown";
    private String result;
    private String sign;
    private String data;

    PayResult(Intent data) {
        if (data == null) {
            return;
        }
        status = data.getExtras().getString("pay_result").toLowerCase();
        if ("success".equals(status)) {

            // 如果想对结果数据验签，可使用下面这段代码，但建议不验签，直接去商户后台查询交易结果
            // result_data结构见c）result_data参数说明
            if (data.hasExtra("result_data")) {
                result = data.getExtras().getString("result_data");
                try {
                    JSONObject resultJson = new JSONObject(result);
                    this.sign = resultJson.getString("sign");
                    this.data = resultJson.getString("data");
                } catch (JSONException e) {
                }
            }
            // 结果result_data为成功时，去商户后台查询一下再展示成功
        }
    }

    /**
     * 获取签名的原始数据
     * 当支付成功时，data不为空
     *
     * @return 支付成功时的签名数据
     */
    public String getData() {
        return data;
    }

    /**
     * 签名后做Base64的数据
     * 当支付成功时，sign不为空
     *
     * @return 支付成功时的签名
     */
    public String getSign() {
        return sign;
    }

    /**
     * 签名后做Base64的数据
     * 当支付成功时，result不为空，result为一段json
     *
     * @return 支付成功结果
     */
    public String getResult() {
        return result;
    }

    /**
     * 返回支付结果状态
     *
     * @return 支付结果状态
     */
    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "status={" + status + "};result={" + result + "}";
    }
}
