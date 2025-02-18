package com.apzda.cloud.queqiao.wx;

import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * wechat mp properties
 *
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@Data
@ConfigurationProperties(prefix = "weixin.mp")
public class WxClientMpProperties {

	private String host;

	private String upstreamHeader = QueQiaoVals.UPSTREAM_HEADER;

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