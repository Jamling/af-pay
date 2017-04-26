/*
 * Copyright (C) 2015-2017 QuickAF
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

import android.text.TextUtils;
import android.util.Xml;

import com.tencent.mm.sdk.modelpay.PayReq;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Description
 *
 * @author Jamling
 */
public abstract class OrderInfoUtil {

    /**
     * <xml>
     * <return_code><![CDATA[SUCCESS]]></return_code>
     * <return_msg><![CDATA[OK]]></return_msg>
     * <appid><![CDATA[wx2421b1c4370ec43b]]></appid>
     * <mch_id><![CDATA[10000100]]></mch_id>
     * <nonce_str><![CDATA[IITRi8Iabbblz1Jc]]></nonce_str>
     * <sign><![CDATA[7921E432F65EB8ED0CE9755F0E86D72F]]></sign>
     * <result_code><![CDATA[SUCCESS]]></result_code>
     * <prepay_id><![CDATA[wx201411101639507cbf6ffd8b0779950874]]></prepay_id>
     * <trade_type><![CDATA[APP]]></trade_type>
     * </xml>
     * 解析统一下单返回的xml报文
     *
     * @param content 统一下单返回的xml字符串
     *
     * @return 解析完的map
     */
    public static Map<String, String> parseXmlResponse(String content) {
        try {
            Map<String, String> xml = new HashMap<>();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(content));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {

                String nodeName = parser.getName();
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if ("xml".equals(nodeName) == false) {
                            // 实例化对象
                            xml.put(nodeName, parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                event = parser.next();
            }
            return xml;
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Get {@link com.tencent.mm.sdk.modelpay.PayReq} from order map result.
     *
     * @param result order result map
     *
     * @return PayReq
     */
    public static PayReq getPayReq(Map<String, String> result) {
        PayReq req = null;
        if (result != null && "SUCCESS".equals(result.get("result_code")) && "SUCCESS".equals(
            result.get("return_code"))) {
            req = new PayReq();
            req.appId = result.get("appid");
            req.nonceStr = result.get("nonce_str");
            req.partnerId = result.get("mch_id");
            req.packageValue = "Sign=WXPay";
            req.prepayId = result.get("prepay_id");
            req.timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            req.sign = result.get("sign");
            
            Map<String, String> sortedMap = new TreeMap<>();
            sortedMap.put("appid", req.appId);
            sortedMap.put("noncestr", req.nonceStr);
            sortedMap.put("partnerid", req.partnerId);
            sortedMap.put("prepayid", req.prepayId);
            sortedMap.put("timestamp", req.timeStamp);
            sortedMap.put("package", req.packageValue);
            String sign = OrderInfoUtil.genSign(sortedMap);
            if (Wxpay.DEBUG) {
                Wxpay.log("客户端支付签名：" + sign);
            }
            req.sign = sign;
        }
        return req;
    }

    /**
     * Get {@link com.tencent.mm.sdk.modelpay.PayReq} from order xml result.
     *
     * @param xmlResultContent order result xml content
     *
     * @return PayReq
     */
    public static PayReq getPayReq(String xmlResultContent) {
        Map<String, String> result = OrderInfoUtil.parseXmlResponse(xmlResultContent);
        return getPayReq(result);
    }
    
    /**
     * Sample :
     * <xml>
     * <appid>wx2421b1c4370ec43b</appid>
     * <attach>支付测试</attach>
     * <body>APP支付测试</body>
     * <mch_id>10000100</mch_id>
     * <nonce_str>1add1a30ac87aa2db72f57a2375d8fec</nonce_str>
     * <notify_url>http://wxpay.wxutil.com/pub_v2/pay/notify.v2.php</notify_url>
     * <out_trade_no>1415659990</out_trade_no>
     * <spbill_create_ip>14.23.150.211</spbill_create_ip>
     * <total_fee>1</total_fee>
     * <trade_type>APP</trade_type>
     * <sign>0CB01533B8C1EF103065174F50BCA001</sign>
     * </xml>
     *
     * @return request xml body
     */
    public static Map<String, String> buildOrderParamMap(String out_trade_no, String body, String detail, String fee,
                                                         String notify_url, String nonce_str, String ip) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("appid", Wxpay.Config.app_id);
        if (body.length() > 128) {
            map.put("body", body.substring(0, 128));
        }
        else {
            map.put("body", body);
        }
        if (!TextUtils.isEmpty(detail)) {
            map.put("detail", detail);
        }
        map.put("mch_id", Wxpay.Config.mch_id);
        map.put("nonce_str", TextUtils.isEmpty(nonce_str) ? OrderInfoUtil.genNonceStr() : nonce_str);
        map.put("notify_url", TextUtils.isEmpty(notify_url) ? Wxpay.Config.notify_url : notify_url);
        map.put("out_trade_no", out_trade_no);
        map.put("spbill_create_ip", TextUtils.isEmpty(ip) ? "127.0.0.1" : ip);
        map.put("total_fee", fee);
        map.put("trade_type", "APP");
        return map;
    }

    public static String genNonceStr() {
        String str = String.valueOf(new java.util.Random().nextDouble());
        return MD5.getMessageDigest(str.getBytes());
    }
    
    public static String genSign(Map<String, String> parameters) {
        // see https://pay.weixin.qq.com/wiki/tools/signverify/
        Map<String, String> sortedMap = new TreeMap<>();
        for (String key : parameters.keySet()) {
            sortedMap.put(key, parameters.get(key));
        }
        
        StringBuffer sb = new StringBuffer();
        Set<String> es = sortedMap.keySet();
        for (String k : es) {
            String v = sortedMap.get(k);
            if (!TextUtils.isEmpty(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        if (Wxpay.DEBUG) {
            Wxpay.log("生成字符串：" + sb.toString());
        }
        sb.append("key=" + Wxpay.Config.api_key);
        if (Wxpay.DEBUG) {
            Wxpay.log("连接商户key：" + sb.toString());
        }
        String sign = MD5.getMessageDigest(sb.toString().getBytes());
        if (Wxpay.DEBUG) {
            Wxpay.log("MD5并转成大写：" + sign);
        }
        return sign;
    }
}
