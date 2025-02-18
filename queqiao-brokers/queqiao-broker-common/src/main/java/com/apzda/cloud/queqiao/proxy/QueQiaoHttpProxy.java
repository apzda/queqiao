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
package com.apzda.cloud.queqiao.proxy;

import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.gtw.filter.HttpHeadersFilter;
import com.apzda.cloud.queqiao.config.HttpProxyConfig;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class QueQiaoHttpProxy implements IHttpProxy {

	private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

	private final WebClient client;

	private final HttpProxyConfig config;

	private volatile List<HttpHeadersFilter> headersFilters;

	@Override
	public ServerResponse handle(@Nonnull ServerRequest request, @Nullable IRetryHandler retry) {
		if (!request.attributes().containsKey(QueQiaoVals.BROKER_REQUEST_WRAPPER)) {
			request.attributes().put(QueQiaoVals.BROKER_REQUEST_WRAPPER, new HttpBrokerRequestWrapper(request));
		}

		return handle_(request, retry, 0);
	}

	@Nonnull
	@SneakyThrows
	private ServerResponse handle_(@Nonnull ServerRequest request, @Nullable IRetryHandler retry, int retried) {
		val context = GsvcContextHolder.getContext();
		context.setAttributes(RequestContextHolder.getRequestAttributes());

		val servletServerHttpRequest = new ServletServerHttpRequest(request.servletRequest());

		val filtered = HttpHeadersFilter.filterRequest(getHeadersFilters(), servletServerHttpRequest);
		filtered.remove(QueQiaoVals.UPSTREAM_HEADER);
		filtered.remove(QueQiaoVals.WX_REAL_HOST_HEADER);
		filtered.remove("X-Forwarded-Proto");
		filtered.remove("X-Forwarded-Host");
		filtered.remove("X-Forwarded-Port");
		filtered.remove("Host");

		val wrapper = HttpBrokerRequestWrapper.from(request);

		val requestBody = wrapper.getRequestBody();
		val multiPart = wrapper.getMultipartData();

		var proxyRequest = client.method(request.method()).uri(request.uri()).headers(headers -> {
			headers.add("X-Request-ID", GsvcContextHolder.getRequestId());
			headers.putAll(filtered);
			headers.remove(HttpHeaders.HOST);
		});

		WebClient.RequestHeadersSpec<?> requestHeadersSpec = proxyRequest.body(BodyInserters.empty());
		if (StringUtils.isNotBlank(requestBody)) {
			requestHeadersSpec = proxyRequest.body(BodyInserters.fromValue(requestBody));
		}
		else if (multiPart != null) {
			requestHeadersSpec = proxyRequest.body(BodyInserters.fromMultipartData(multiPart));
		}

		final ServerResponse serverResponse;
		if (retry == null) {
			serverResponse = doForward(requestHeadersSpec, request);
		}
		else {
			serverResponse = doForward(requestHeadersSpec, request, retry, retried);
		}

		return serverResponse;
	}

	@Nonnull
	@SuppressWarnings("all")
	private ServerResponse doForward(@Nonnull WebClient.RequestHeadersSpec<?> request,
			@Nonnull ServerRequest oriRequest) {
		val proxyResponse = request.exchangeToFlux(response -> {
			val responseStatus = response.statusCode();
			val httpHeaders = filterResponseHeaders(response.headers().asHttpHeaders(),
					new ServletServerHttpRequest(oriRequest.servletRequest()));
			val serverResponse = ServerResponse.status(responseStatus).headers(headers -> headers.addAll(httpHeaders));

			val dataBuffers = response.body(BodyExtractors.toDataBuffers()).toStream();
			val resp = serverResponse.build((req, res) -> {
				try (val writer = res.getOutputStream()) {
					dataBuffers.forEach(dataBuffer -> {
						try (val input = dataBuffer.asInputStream()) {
							writer.write(input.readAllBytes());
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				}
				return null;
			});

			return Flux.just(resp);
		});

		return ServerResponse.async(proxyResponse.elementAt(0), config.getReadTimeout());
	}

	@Nonnull
	private ServerResponse doForward(@Nonnull WebClient.RequestHeadersSpec<?> request, ServerRequest oriRequest,
			@Nonnull IRetryHandler retry, int retried) {
		try {
			val response = request.retrieve().toEntity(String.class).block(config.getReadTimeout());
			if (response == null) {
				log.warn("Can't get response from {}", oriRequest.uri());
				return ServerResponse.status(502).build();
			}
			val responseStatus = response.getStatusCode().value();

			if (!retry.exceedsMaxRetries(retried) && (retry.needRetryByHttpStatus(responseStatus)
					|| retry.needRetryByErrCode(oriRequest, response))) {
				try {
					TimeUnit.MILLISECONDS.sleep(retry.getRetryInterval().toMillis());
					log.warn("Retrying {}", oriRequest.uri());
					return handle_(retry.createRetryRequest(oriRequest), retry, retried + 1);
				}
				catch (InterruptedException e) {
					log.warn("Interrupted while waiting for retrying from {}", oriRequest.uri());
				}
			}
			// 头过滤
			val httpHeaders = filterResponseHeaders(response.getHeaders(),
					new ServletServerHttpRequest(oriRequest.servletRequest()));

			val resp = ServerResponse.status(responseStatus).headers(headers -> headers.addAll(httpHeaders));

			if (response.getBody() != null) {
				return resp.body(response.getBody());
			}
			else {
				return resp.build();
			}
		}
		catch (IllegalStateException ie) {
			if (ie.getCause() instanceof TimeoutException) {
				return ServerResponse.status(504).build();
			}
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return ServerResponse.status(502).build();
	}

	private List<HttpHeadersFilter> getHeadersFilters() {
		if (headersFilters == null) {
			headersFilters = headersFiltersProvider.getIfAvailable();
		}
		return headersFilters;
	}

	@Nonnull
	private HttpHeaders filterResponseHeaders(@Nonnull HttpHeaders responseHeaders,
			@Nonnull ServletServerHttpRequest request) {
		val headers = HttpHeadersFilter.filter(getHeadersFilters(), responseHeaders, request,
				HttpHeadersFilter.Type.RESPONSE);

		val httpHeaders = new HttpHeaders();
		httpHeaders.addAll(headers);

		if (httpHeaders.containsKey(HttpHeaders.TRANSFER_ENCODING)
				&& httpHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
			httpHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
		}

		return httpHeaders;
	}

}
