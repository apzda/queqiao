/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.queqiao.autoconfig;

import com.apzda.cloud.queqiao.http.HttpClientProperties;
import com.apzda.cloud.queqiao.http.WxApacheHttpClientBuilder;
import com.apzda.cloud.queqiao.wx.WxClientMpProperties;
import com.apzda.cloud.queqiao.wx.WxConst;
import com.apzda.cloud.queqiao.wx.interceptor.UpstreamInterceptor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.common.redis.WxRedisOps;
import me.chanjar.weixin.common.util.http.apache.ApacheHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.WxMpHostConfig;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WxClientMpProperties.class)
@ConditionalOnClass(name = { "me.chanjar.weixin.mp.api.impl.WxMpServiceImpl", "org.apache.http.client.HttpClient" })
@Slf4j
public class QueQiaoMpAutoConfiguration {

	@Bean
	public WxMpService wxMpService(WxClientMpProperties properties, HttpClientProperties httpClientProperties,
			ApacheHttpClientBuilder wxApacheHttpClientBuilder, @Autowired(required = false) WxRedisOps wxRedisOps)
			throws URISyntaxException {
		final WxMpService service = new WxMpServiceImpl();
		val configs = properties.getAccount();
		if (configs == null) {
			log.warn("No config found");
			return service;
		}
		var host = properties.getHost();
		if (StringUtils.isBlank(host)) {
			throw new IllegalArgumentException("'weixin.mp.host' property cannot be blank");
		}
		host = StringUtils.strip(host, "/");
		try {
			new URIBuilder(host);
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(String.format("weixin.mp.host[%s] - %s", host, e.getMessage()));
		}
		val wxMpHostConfig = new WxMpHostConfig();
		wxMpHostConfig.setMpHost(String.format("%s%s", host, WxConst.WX_MP_PREFIX));
		wxMpHostConfig.setApiHost(String.format("%s%s", host, WxConst.WX_API_PREFIX));
		wxMpHostConfig.setOpenHost(String.format("%s%s", host, WxConst.WX_OPEN_PREFIX));

		service.setMultiConfigStorages(configs.entrySet().stream().map(mp -> {
			val id = mp.getKey();
			val config = mp.getValue();
			final WxMpDefaultConfigImpl configStorage;
			if (properties.isUseRedis() && wxRedisOps != null) {
				configStorage = new WxMpRedisConfigImpl(wxRedisOps, config.getAppId());
			}
			else {
				configStorage = new WxMpDefaultConfigImpl();
			}
			configStorage.setAppId(config.getAppId());
			configStorage.setSecret(config.getAppSecret());
			configStorage.setToken(config.getToken());
			configStorage.setAesKey(config.getAesKey());
			configStorage.setRetrySleepMillis((int) config.getRetryInterval().toMillis());
			configStorage.setMaxRetryTimes(config.getRetryTimes());
			configStorage.useStableAccessToken(config.isStableAccessToken());
			configStorage.setHostConfig(wxMpHostConfig);
			val builder = WxApacheHttpClientBuilder.get();
			builder.setRequestInterceptors(List.of(new UpstreamInterceptor(id, config, properties)));
			configStorage.setApacheHttpClientBuilder(builder);

			return configStorage;
		}).collect(Collectors.toMap(WxMpDefaultConfigImpl::getAppId, a -> a, (o, n) -> o)));
		return service;
	}

}
