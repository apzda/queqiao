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
package com.apzda.cloud.queqiao.config;

import com.apzda.cloud.gsvc.gtw.filter.HttpHeadersFilter;
import com.apzda.cloud.queqiao.broker.BrokerManager;
import com.apzda.cloud.queqiao.proxy.IHttpProxy;
import com.apzda.cloud.queqiao.proxy.QueQiaoHttpProxy;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QueQiaoProperties.class)
public class QueQiaoSrvConfiguration {

	@Bean
	@ConditionalOnMissingBean
	static BrokerManager brokerManager() {
		return new BrokerManager() {
		};
	}

	@Bean
	@Qualifier("queqiaoWebClient")
	WebClient queqiaoWebClient(WebClient.Builder builder, QueQiaoProperties properties) {
		val proxy = properties.getProxy();
		val readTimeout = proxy.getReadTimeout();
		val writeTimeout = proxy.getWriteTimeout();
		val connectTimeout = proxy.getConnectTimeout();

		val httpClient = HttpClient.create()
			.followRedirect(false)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.doOnConnected(conn -> {
				if (readTimeout.toMillis() > 0) {
					conn.addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS));
				}
				if (writeTimeout.toMillis() > 0) {
					conn.addHandlerLast(new WriteTimeoutHandler(writeTimeout.toMillis(), TimeUnit.MILLISECONDS));
				}
			});

		if (connectTimeout.toMillis() > 0) {
			httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
		}

		return builder.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}

	@Bean
	@ConditionalOnMissingBean
	IHttpProxy queqiaoHttpProxy(ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
			@Qualifier("queqiaoWebClient") WebClient queqiaoWebClient, QueQiaoProperties properties) {
		return new QueQiaoHttpProxy(headersFiltersProvider, queqiaoWebClient, properties.getProxy());
	}

}
