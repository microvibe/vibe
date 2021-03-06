package io.microvibe.booster.txn.alipay.impl;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayOpenAuthTokenAppRequest;
import com.alipay.api.response.AlipayOpenAuthTokenAppResponse;
import io.microvibe.booster.commons.spring.ApplicationContextHolder;
import io.microvibe.booster.commons.utils.StringUtils;
import io.microvibe.booster.core.api.ApiException;
import io.microvibe.booster.core.api.annotation.ApiName;
import io.microvibe.booster.core.api.model.BodyModel;
import io.microvibe.booster.core.api.model.RequestData;
import io.microvibe.booster.core.api.model.ResponseData;
import io.microvibe.booster.pay.alipay.config.AlipayConfig;
import io.microvibe.booster.pay.alipay.entity.PayAlipayAppConfig;
import io.microvibe.booster.pay.alipay.service.PayAlipayAppConfigService;
import io.microvibe.booster.txn.alipay.AbstractAlipayApiService;
import io.microvibe.booster.txn.alipay.AlipayTxnCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 使用app_auth_code换取app_auth_token
 *
 * @author Qt
 * @since Aug 26, 2018
 */
@Component
@ApiName(AlipayTxnCode.PAY_ALIPAY_OPEN_AUTH_TOKEN_APP)
@Slf4j
public class Alipay0004 extends AbstractAlipayApiService {

	/**
	 * <pre>
	 * 第一步：创建应用
	 * 第二步：配置密钥
	 * 第三步：应用授权URL拼装
	 *   拼接规则:
	 *     https://openauth.alipay.com/oauth2/appToAppAuth.htm?app_id=2015101400446982&redirect_uri=http%3A%2F%2Fexample.com
	 *
	 *     参数           参数名称            类型       必填       描述               范例
	 *     app_id        开发者应用的AppId    String     是      开发者应用的AppId      2015101400446982
	 *     redirect_uri  回调页面            String     是     参数需要UrlEncode       http%3A%2F%2Fexample.com
	 *
	 *   TIPS： 授权链接中配置的redirect_uri内容需要与应用中配置的授权回调地址完全一样，否则无法正常授权。
	 *
	 * 第四步：获取app_auth_code
	 *   商户授权成功后，pc或者钱包客户端会跳转至开发者定义的回调页面（即redirect_uri参数对应的url），
	 *   在回调页面请求中会带上当次授权的授权码app_auth_code和开发者的app_id
	 *
	 * 第五步：使用app_auth_code换取app_auth_token
	 *   接口名称：alipay.open.auth.token.app
	 *
	 *   开发者通过app_auth_code可以换取app_auth_token、授权商户的userId以及授权商户AppId。
	 *
	 *   注意:  应用授权的app_auth_code唯一的；
	 *         app_auth_code使用一次后失效，一天（从生成app_auth_code开始的24小时）未被使用自动过期；
	 *         app_auth_token永久有效。
	 *
	 *    开发者代替商户发起请求时，POST公共请求参数中的app_id应填写开发者的app_id；
	 *    如果业务参数biz_content中需要AppId，则应填写商户的AppId。
	 *
	 *
	 * </pre>
	 *
	 * @return
	 */
	@Override
	public ResponseData doExecute(RequestData requestData) {
		try {
			BodyModel requestBody = requestData.getBody();
			String app_id = StringUtils.trimToNull(requestBody.getString("app_id"));
			String app_auth_code = StringUtils.trimToNull(requestBody.getString("app_auth_code"));

			AlipayConfig alipayConfig = AlipayConfig.get();


			// 使用app_auth_code换取app_auth_token
			AlipayClient alipayClient = alipayService.createAlipayClient(alipayConfig);
			AlipayOpenAuthTokenAppRequest alipayRequest = new AlipayOpenAuthTokenAppRequest();
			alipayRequest.setBizContent("{" +
				"    \"grant_type\":\"authorization_code\"," +
				"    \"code\":\"" + app_auth_code + "\"" +
				"  }");
			AlipayOpenAuthTokenAppResponse alipayResponse = alipayClient.execute(alipayRequest);
			String appAuthToken = alipayResponse.getAppAuthToken();//商户授权令牌,通过该令牌来帮助商户发起请求，完成业务
			String authAppId = alipayResponse.getAuthAppId();//授权商户的AppId（如果有服务窗，则为服务窗的AppId）
			String userId = alipayResponse.getUserId();//授权商户的ID/授权者的PID
			String appRefreshToken = alipayResponse.getAppRefreshToken(); //刷新令牌，老的app_auth_token从刷新开始10分钟内可继续使用

			log.info("保存令牌信息");
			log.info("appAuthToken: {}", appAuthToken);
			log.info("authAppId: {}", authAppId);
			log.info("userId: {}", userId);
			log.info("appRefreshToken: {}", appRefreshToken);

			if (!alipayResponse.isSuccess()) {
				throw new ApiException(alipayResponse.getCode() + ":" + alipayResponse.getMsg());
			}

			PayAlipayAppConfigService payAlipayAppConfigService = ApplicationContextHolder.getBean(PayAlipayAppConfigService.class);

			PayAlipayAppConfig entity = payAlipayAppConfigService.getByConfigIdAndAppId(alipayConfig.config_id, authAppId);
			if (entity != null) {
				entity.setValid(false);
				payAlipayAppConfigService.updateSelective(entity);
			}
			entity = new PayAlipayAppConfig();
			entity.setValid(true);
			entity.setAuthAppId(authAppId);
			entity.setAuthUserId(userId);
			entity.setAppAuthCode(app_auth_code);
			entity.setAppRefreshToken(appRefreshToken);
			entity.setAppAuthToken(appAuthToken);
			entity.setAlipayConfigId(alipayConfig.config_id);
			entity.setIdentity(UUID.randomUUID().toString());
			payAlipayAppConfigService.insert(entity);


			ResponseData responseData = requestData.buildResponse();
			responseData.setBody("appAuthToken", appAuthToken);
			responseData.setBody("authAppId", authAppId);
			responseData.setBody("userId", userId);
			responseData.setBody("appRefreshToken", appRefreshToken);
			return responseData;
		} catch (Exception e) {
			throw new ApiException(e);
		}
	}
}
