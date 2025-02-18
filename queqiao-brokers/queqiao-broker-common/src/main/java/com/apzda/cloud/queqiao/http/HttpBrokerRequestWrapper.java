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

import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.google.common.base.Joiner;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class HttpBrokerRequestWrapper {

	private final ContentCachingRequestWrapper requestWrapper;

	private final ServerRequest delegate;

	private final HttpBrokerRequestWrapper parent;

	@Getter
	private final boolean multipart;

	private volatile String requestBody;

	private volatile MultiValueMap<String, HttpEntity<?>> multipartData;

	public HttpBrokerRequestWrapper(@Nonnull ServerRequest request) {
		val httpServletRequest = request.servletRequest();
		val attr = request.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER);
		if (attr.isPresent()) {
			val wrapper = (HttpBrokerRequestWrapper) attr.get();
			delegate = wrapper.delegate;
			multipart = wrapper.multipart;
			requestBody = wrapper.requestBody;
			multipartData = wrapper.multipartData;
			requestWrapper = wrapper.requestWrapper;
			parent = wrapper.parent;
		}
		else {
			this.delegate = request;
			this.multipart = StringUtils.startsWithIgnoreCase(httpServletRequest.getContentType(), "multipart/");
			val cachingWrapper = request.attribute(QueQiaoVals.CONTENT_CACHING_REQUEST_WRAPPER);
			if (cachingWrapper.isEmpty()) {
				requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
				request.attributes().put(QueQiaoVals.CONTENT_CACHING_REQUEST_WRAPPER, requestWrapper);
			}
			else {
				requestWrapper = (ContentCachingRequestWrapper) cachingWrapper.get();
			}

			parent = null;
		}
	}

	@Nonnull
	public static HttpBrokerRequestWrapper from(@Nonnull ServerRequest request) {
		val attr = request.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER);
		if (attr.isPresent()) {
			return (HttpBrokerRequestWrapper) attr.get();
		}

		val wrapper = new HttpBrokerRequestWrapper(request);
		request.attributes().put(QueQiaoVals.BROKER_REQUEST_WRAPPER, wrapper);

		return wrapper;
	}

	public String getRequestBody() throws IOException {
		if (parent != null) {
			return parent.getRequestBody();
		}
		if (multipart) {
			return null;
		}
		if (requestBody != null) {
			return requestBody;
		}
		if (requestWrapper.getContentLength() == 0) {
			return null;
		}

		if (requestBody == null) {
			synchronized (requestWrapper) {
				if (requestBody == null) {
					try {
						if (requestWrapper.getInputStream().isFinished()) {
							requestBody = StringUtils.defaultIfBlank(requestWrapper.getContentAsString(),
									StringUtils.EMPTY);
						}
						else {
							val body = Joiner.on(System.lineSeparator())
								.join(requestWrapper.getReader().lines().toList());
							requestBody = StringUtils.defaultIfBlank(body, StringUtils.EMPTY);
						}
					}
					catch (IOException e) {
						log.warn("Failed to read request body - {} - {}", requestWrapper.getRequestURI(),
								e.getMessage());
						throw e;
					}
				}
			}
		}

		return requestBody;
	}

	public void setRequestBody(@Nonnull String requestBody) {
		if (parent != null) {
			parent.requestBody = requestBody;
		}
		else {
			this.requestBody = requestBody;
		}
	}

	public MultiValueMap<String, HttpEntity<?>> getMultipartData() throws IOException {
		if (parent != null) {
			return parent.getMultipartData();
		}
		if (!multipart) {
			return null;
		}
		if (multipartData == null) {
			synchronized (delegate) {
				if (multipartData == null) {
					try {
						multipartData = new LinkedMultiValueMap<>();
						val data = delegate.multipartData();
						if (!CollectionUtils.isEmpty(data)) {
							val builder = generateMultipartFormData(data);
							multipartData = builder.build();
						}
					}
					catch (IOException e) {
						throw e;
					}
					catch (Exception e) {
						throw new IOException(e.getMessage(), e);
					}
				}
			}
		}
		return multipartData;
	}

	public void setMultipartData(@Nonnull MultiValueMap<String, HttpEntity<?>> multipartData) {
		if (parent != null) {
			parent.multipartData = multipartData;
		}
		else {
			this.multipartData = multipartData;
		}
	}

	public <T> T getRequestBody(Class<T> clazz) throws IOException {
		return ResponseUtils.OBJECT_MAPPER.readValue(getRequestBody(), clazz);
	}

	@Nonnull
	private MultipartBodyBuilder generateMultipartFormData(@Nonnull MultiValueMap<String, Part> multiValueMap) {
		val builder = new MultipartBodyBuilder();

		for (val name : multiValueMap.keySet()) {
			val parts = multiValueMap.get(name);
			if (CollectionUtils.isEmpty(parts)) {
				continue;
			}
			for (val part : parts) {
				val contentType = part.getContentType();
				MediaType mediaType = null;
				if (contentType != null) {
					try {
						mediaType = MediaType.parseMediaType(contentType);
					}
					catch (Exception ignored) {
					}
				}
				builder.part(name, decode(part), mediaType);
			}
		}
		return builder;
	}

	@Nonnull
	private Object decode(@Nonnull Part part) {
		val httpHeaders = new HttpHeaders();
		val headerNames = part.getHeaderNames();
		if (!CollectionUtils.isEmpty(headerNames)) {
			headerNames.forEach(headerName -> httpHeaders.add(headerName, part.getHeader(headerName)));
		}
		return new AsyncPart(part.getName(), part, httpHeaders);
	}

	record AsyncPart(String name, Part part,
			HttpHeaders headers) implements org.springframework.http.codec.multipart.Part {
		@Override
		@Nonnull
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readInputStream(part::getInputStream, DefaultDataBufferFactory.sharedInstance, 1024);
		}
	}

}
