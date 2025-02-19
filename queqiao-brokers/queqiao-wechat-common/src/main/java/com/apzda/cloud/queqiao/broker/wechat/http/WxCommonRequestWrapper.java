/*
 * Copyright (C) 2025-2025 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.queqiao.broker.wechat.http;

import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@Getter
public class WxCommonRequestWrapper extends HttpBrokerRequestWrapper {

	@Getter(AccessLevel.PRIVATE)
	private final static String[] fields = new String[] { "signature", "timestamp", "nonce", "echostr", "openid",
			"encrypt_type", "msg_signature", "secret", "appid", "access_token" };

	protected final Map<String, String> params = new HashMap<>();

	public WxCommonRequestWrapper(@Nonnull ServerRequest request) {
		super(request);
		for (String field : fields) {
			request.param(field).ifPresent(value -> this.params.put(field, value));
		}
	}

	@Nonnull
	public static WxCommonRequestWrapper from(@Nonnull ServerRequest request) {
		val wrapper = request.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER).orElse(null);
		if (wrapper instanceof WxCommonRequestWrapper wxCommonRequestWrapper) {
			return wxCommonRequestWrapper;
		}

		val requestWrapper = new WxCommonRequestWrapper(request);
		request.attributes().put(QueQiaoVals.BROKER_REQUEST_WRAPPER, requestWrapper);
		return requestWrapper;
	}

	@Nullable
	public String getSignature() {
		return params.get("signature");
	}

	@Nullable
	public String getTimestamp() {
		return params.get("timestamp");
	}

	@Nullable
	public String getNonce() {
		return params.get("nonce");
	}

	@Nullable
	public String getEchoStr() {
		return params.get("echostr");
	}

	@Nullable
	public String getOpenid() {
		return params.get("openid");
	}

	@Nullable
	public String getEncryptType() {
		return params.get("encrypt_type");
	}

	@Nullable
	public String getMsgSignature() {
		return params.get("msg_signature");
	}

	@Nullable
	public String getSecret() {
		return params.get("secret");
	}

	@Nullable
	public String getAppid() {
		return params.get("appid");
	}

	public String getAccessToken() {
		return params.get("access_token");
	}

	@SuppressWarnings("all")
	public boolean checkAuthentication(BrokerConfig config) throws Exception {
		val secret = getSecret();
		val appid = getAppid();
		if (!StringUtils.isAllBlank(secret, appid)) {
			return Objects.equals(secret, config.getAppSecret()) && Objects.equals(appid, config.getAppId());
		}

		return true;
	}

	public MultiValueMap<String, String> queryParams() {
		val mParams = new LinkedMultiValueMap<String, String>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			mParams.add(entry.getKey(), entry.getValue());
		}
		return mParams;
	}

}
