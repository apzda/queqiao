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
package com.apzda.cloud.queqiao.broker.wechat.mp.http;

import com.apzda.cloud.queqiao.broker.wechat.http.WxCommonRequestWrapper;
import com.apzda.cloud.queqiao.config.BrokerConfig;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.mp.bean.WxMpStableAccessTokenRequest;
import me.chanjar.weixin.mp.util.json.WxMpGsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.function.ServerRequest;

import java.io.IOException;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class WxMpRequestWrapper extends WxCommonRequestWrapper {

	public WxMpRequestWrapper(@Nonnull ServerRequest request) {
		super(request);
	}

	@Override
	public <T> T getRequestBody(Class<T> clazz) throws IOException {
		return WxMpGsonBuilder.create().fromJson(getRequestBody(), clazz);
	}

	@Override
	public boolean checkAuthentication(BrokerConfig config) {
		if (super.checkAuthentication(config) && StringUtils.isNotBlank(getRequestBody())) {
			try {
				val dto = getRequestBody(WxMpStableAccessTokenRequest.class);
				val secret = dto.getSecret();
				val appid = dto.getAppid();
				if (!StringUtils.isAllBlank(secret, appid)) {
					return Objects.equals(secret, config.getAppSecret()) && Objects.equals(appid, config.getAppId());
				}
			}
			catch (IOException e) {
				log.error("Check request body failed", e);
				return false;
			}
		}
		return true;
	}

}
