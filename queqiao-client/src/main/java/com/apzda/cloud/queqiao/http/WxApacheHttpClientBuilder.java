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
package com.apzda.cloud.queqiao.http;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.common.util.http.apache.ApacheHttpClientBuilder;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class WxApacheHttpClientBuilder implements ApacheHttpClientBuilder {

	private final AtomicBoolean prepared = new AtomicBoolean(false);

	/**
	 * 获取链接的超时时间设置
	 * <p>
	 * 设置为零时不超时,一直等待. 设置为负数是使用系统默认设置(非3000ms的默认值,而是httpClient的默认设置).
	 * </p>
	 */
	private int connectionRequestTimeout = -1;

	/**
	 * 建立链接的超时时间,默认为5000ms.由于是在链接池获取链接,此设置应该并不起什么作用
	 * <p>
	 * 设置为零时不超时,一直等待. 设置为负数是使用系统默认设置(非上述的5000ms的默认值,而是httpclient的默认设置).
	 * </p>
	 */
	private int connectionTimeout = 5000;

	private int idleConnTimeout = 60000;

	/**
	 * 默认NIO的socket超时设置,默认5000ms.
	 */
	private int soTimeout = 5000;

	/**
	 * 检查空间链接的间隔周期,默认60000ms.
	 */
	private int checkWaitTime = 60000;

	/**
	 * 每路的最大链接数,默认10
	 */
	private int maxConnPerHost = 10;

	/**
	 * 最大总连接数,默认50
	 */
	private int maxTotalConn = 50;

	/**
	 * 自定义httpclient的User Agent
	 */
	private String userAgent;

	/**
	 * 自定义请求拦截器
	 */
	@Setter
	@Getter
	private List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();

	/**
	 * 自定义响应拦截器
	 */
	@Setter
	@Getter
	private List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();

	/**
	 * 自定义重试策略
	 */
	private HttpRequestRetryHandler httpRequestRetryHandler;

	/**
	 * 自定义KeepAlive策略
	 */
	private ConnectionKeepAliveStrategy connectionKeepAliveStrategy;

	private final HttpRequestRetryHandler defaultHttpRequestRetryHandler = (exception, executionCount,
			context) -> false;

	private SSLConnectionSocketFactory sslConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory();

	private final PlainConnectionSocketFactory plainConnectionSocketFactory = PlainConnectionSocketFactory
		.getSocketFactory();

	private String httpProxyHost;

	private int httpProxyPort;

	private String httpProxyUsername;

	private String httpProxyPassword;

	private PoolingHttpClientConnectionManager connectionManager;

	@Getter
	private DefaultApacheHttpClientBuilder.IdleConnectionMonitorThread idleConnectionMonitorThread;

	private volatile CloseableHttpClient closeableHttpClient;

	private WxApacheHttpClientBuilder() {
	}

	@Nonnull
	public static WxApacheHttpClientBuilder get() {
		val builder = new WxApacheHttpClientBuilder();
		builder.prepared.set(true);
		builder.userAgent = SingletonHolder.INSTANCE.userAgent;
		builder.httpRequestRetryHandler = SingletonHolder.INSTANCE.httpRequestRetryHandler;
		builder.connectionKeepAliveStrategy = SingletonHolder.INSTANCE.connectionKeepAliveStrategy;
		builder.httpProxyHost = SingletonHolder.INSTANCE.httpProxyHost;
		builder.httpProxyPort = SingletonHolder.INSTANCE.httpProxyPort;
		builder.httpProxyUsername = SingletonHolder.INSTANCE.httpProxyUsername;
		builder.httpProxyPassword = SingletonHolder.INSTANCE.httpProxyPassword;
		return builder;
	}

	public ApacheHttpClientBuilder newBuilder() {
		return get();
	}

	@Nonnull
	public static WxApacheHttpClientBuilder setupDefault(@Nonnull HttpClientProperties config) {
		synchronized (SingletonHolder.INSTANCE) {
			val instance = SingletonHolder.INSTANCE;
			instance.connectionRequestTimeout = config.getConnectionRequestTimeout();
			instance.connectionTimeout = config.getConnectionTimeout();
			instance.soTimeout = config.getSoTimeout();
			instance.idleConnTimeout = config.getIdleConnTimeout();
			instance.checkWaitTime = config.getCheckWaitTime();
			instance.maxConnPerHost = config.getMaxConnPerHost();
			instance.maxTotalConn = config.getMaxTotalConn();
			instance.userAgent = config.getUserAgent();
			return instance;
		}
	}

	@Override
	public ApacheHttpClientBuilder httpProxyHost(String httpProxyHost) {
		this.httpProxyHost = httpProxyHost;
		return this;
	}

	@Override
	public ApacheHttpClientBuilder httpProxyPort(int httpProxyPort) {
		this.httpProxyPort = httpProxyPort;
		return this;
	}

	@Override
	public ApacheHttpClientBuilder httpProxyUsername(String httpProxyUsername) {
		this.httpProxyUsername = httpProxyUsername;
		return this;
	}

	@Override
	public ApacheHttpClientBuilder httpProxyPassword(String httpProxyPassword) {
		this.httpProxyPassword = httpProxyPassword;
		return this;
	}

	@Override
	public ApacheHttpClientBuilder httpRequestRetryHandler(HttpRequestRetryHandler httpRequestRetryHandler) {
		this.httpRequestRetryHandler = httpRequestRetryHandler;
		return this;
	}

	@Override
	public ApacheHttpClientBuilder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		this.connectionKeepAliveStrategy = keepAliveStrategy;
		return this;
	}

	@Override
	public ApacheHttpClientBuilder sslConnectionSocketFactory(SSLConnectionSocketFactory sslConnectionSocketFactory) {
		this.sslConnectionSocketFactory = sslConnectionSocketFactory;
		return this;
	}

	@Override
	public CloseableHttpClient build() {
		if (!SingletonHolder.INSTANCE.prepared.get()) {
			SingletonHolder.INSTANCE.prepare();
		}

		if (closeableHttpClient == null) {
			synchronized (prepared) {
				if (closeableHttpClient == null) {
					val builder = buildHttpClientBuilder();
					closeableHttpClient = builder.build();
				}
			}
		}

		return this.closeableHttpClient;
	}

	private synchronized void prepare() {
		if (prepared.get()) {
			return;
		}
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
			.register("http", this.plainConnectionSocketFactory)
			.register("https", this.sslConnectionSocketFactory)
			.build();

		connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(this.maxTotalConn);
		connectionManager.setDefaultMaxPerRoute(this.maxConnPerHost);
		connectionManager
			.setDefaultSocketConfig(SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(this.soTimeout).build());

		idleConnectionMonitorThread = new DefaultApacheHttpClientBuilder.IdleConnectionMonitorThread(connectionManager,
				idleConnTimeout, this.checkWaitTime);
		idleConnectionMonitorThread.setDaemon(true);
		idleConnectionMonitorThread.start();

		prepared.set(true);
	}

	@Nonnull
	private HttpClientBuilder buildHttpClientBuilder() {
		HttpClientBuilder httpClientBuilder = HttpClients.custom()
			.setConnectionManager(SingletonHolder.INSTANCE.connectionManager)
			.setConnectionManagerShared(true)
			.setSSLSocketFactory(SingletonHolder.INSTANCE.sslConnectionSocketFactory)
			.setDefaultRequestConfig(RequestConfig.custom()
				.setSocketTimeout(SingletonHolder.INSTANCE.soTimeout)
				.setConnectTimeout(SingletonHolder.INSTANCE.connectionTimeout)
				.setConnectionRequestTimeout(SingletonHolder.INSTANCE.connectionRequestTimeout)
				.build());

		// 设置重试策略，没有则使用默认
		httpClientBuilder.setRetryHandler(httpRequestRetryHandler());

		// 设置KeepAliveStrategy，没有使用默认
		if (connectionKeepAliveStrategy() != null) {
			httpClientBuilder.setKeepAliveStrategy(connectionKeepAliveStrategy());
		}
		val proxyHost = httpProxyHost();
		val username = this.httpProxyUsername();
		if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(username)) {
			// 使用代理服务器 需要用户认证的代理服务器
			CredentialsProvider provider = new BasicCredentialsProvider();
			provider.setCredentials(new AuthScope(proxyHost, this.httpProxyPort()),
					new UsernamePasswordCredentials(username, this.httpProxyPassword()));
			httpClientBuilder.setDefaultCredentialsProvider(provider);
			httpClientBuilder.setProxy(new HttpHost(proxyHost, this.httpProxyPort()));
		}

		if (StringUtils.isNotBlank(userAgent)) {
			httpClientBuilder.setUserAgent(userAgent);
		}

		// 添加自定义的请求拦截器
		requestInterceptors.forEach(httpClientBuilder::addInterceptorFirst);

		// 添加自定义的响应拦截器
		responseInterceptors.forEach(httpClientBuilder::addInterceptorLast);

		return httpClientBuilder;
	}

	private HttpRequestRetryHandler httpRequestRetryHandler() {
		if (httpRequestRetryHandler == null) {
			return Objects.requireNonNullElse(SingletonHolder.INSTANCE.httpRequestRetryHandler,
					SingletonHolder.INSTANCE.defaultHttpRequestRetryHandler);
		}
		return httpRequestRetryHandler;
	}

	private ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		if (connectionKeepAliveStrategy == null) {
			return SingletonHolder.INSTANCE.connectionKeepAliveStrategy;
		}
		return connectionKeepAliveStrategy;
	}

	private String httpProxyHost() {
		if (StringUtils.isNotBlank(this.httpProxyHost)) {
			return this.httpProxyHost;
		}
		return SingletonHolder.INSTANCE.httpProxyHost;
	}

	private int httpProxyPort() {
		if (this.httpProxyPort > 0) {
			return this.httpProxyPort;
		}
		return SingletonHolder.INSTANCE.httpProxyPort;
	}

	private String httpProxyUsername() {
		if (StringUtils.isNotBlank(this.httpProxyUsername)) {
			return this.httpProxyUsername;
		}
		return SingletonHolder.INSTANCE.httpProxyUsername;
	}

	private String httpProxyPassword() {
		if (StringUtils.isNotBlank(this.httpProxyPassword)) {
			return this.httpProxyPassword;
		}
		return SingletonHolder.INSTANCE.httpProxyPassword;
	}

	private static class SingletonHolder {

		private static final WxApacheHttpClientBuilder INSTANCE = new WxApacheHttpClientBuilder();

	}

}
