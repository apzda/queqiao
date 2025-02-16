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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class CommonQuery {

	private final static String[] fields = new String[] { "signature", "timestamp", "nonce", "echostr", "openid",
			"encrypt_type", "msg_signature", "secret", "appid", "access_token" };

	private final Map<String, String> params = new HashMap<>();

	public CommonQuery(@Nonnull ServerRequest request) {
		for (String field : fields) {
			params.put(field, request.param(field).orElse(null));
		}
	}

	@Nonnull
	public static CommonQuery from(@Nonnull ServerRequest request) {
		return new CommonQuery(request);
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

	public boolean checkRequest(BrokerConfig config) {
		return true;
	}

}
