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
package com.apzda.cloud.queqiao.wx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@ConfigurationProperties(prefix = "weixin.mp")
public class WxClientMpProperties {

	private String host;

	private String upstreamHeader = WxConst.UPSTREAM_HEADER;

	private boolean useRedis;

	private final Map<String, MpConfig> account = new HashMap<>();

	@Data
	public static class MpConfig {

		/**
		 * 设置微信公众号的appid
		 */
		private String appId;

		/**
		 * 设置微信公众号的app secret
		 */
		private String appSecret;

		/**
		 * 设置微信公众号的token
		 */
		private String token;

		/**
		 * 设置微信公众号的EncodingAESKey
		 */
		private String aesKey;

		/**
		 * 使用stableAccessToken
		 */
		private boolean stableAccessToken;

		/**
		 * 微信返回-1时，重试次数
		 */
		private int retryTimes = 5;

		/**
		 * 重试间隔
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration retryInterval = Duration.ofSeconds(1);

	}

}