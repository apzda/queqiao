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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j

public class HttpBrokerRequestWrapper {

	private final ContentCachingRequestWrapper requestWrapper;

	@Setter
	private volatile String requestBody;

	public HttpBrokerRequestWrapper(@Nonnull ServerRequest request) {
		requestWrapper = (ContentCachingRequestWrapper) request.attribute(QueQiaoVals.CONTENT_CACHING_REQUEST_WRAPPER)
			.orElse(new ContentCachingRequestWrapper(request.servletRequest()));
	}

	public String getRequestBody() {
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
						throw new RuntimeException(e);
					}
				}
			}
		}
		return requestBody;
	}

	public <T> T getRequestBody(Class<T> clazz) throws IOException {
		return ResponseUtils.OBJECT_MAPPER.readValue(getRequestBody(), clazz);
	}

}
