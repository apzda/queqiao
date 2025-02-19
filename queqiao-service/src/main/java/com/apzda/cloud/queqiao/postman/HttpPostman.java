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
package com.apzda.cloud.queqiao.postman;

import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import com.apzda.cloud.queqiao.notify.NotifyContext;
import com.apzda.cloud.queqiao.proxy.IHttpProxy;
import com.apzda.cloud.queqiao.proxy.IRetryHandler;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class HttpPostman implements IPostman {

	private final boolean sync;

	private final IHttpProxy httpProxy;

	public HttpPostman(boolean sync, IHttpProxy httpProxy) {
		this.sync = sync;
		this.httpProxy = httpProxy;
	}

	@Override
	public String getId() {
		return this.sync ? "http" : "asyncHttp";
	}

	@Nonnull
	@Override
	public ServerResponse notify(@Nonnull NotifyContext context) {
		val request = context.request();
		val receipt = UriComponentsBuilder.fromUriString(context.receipt()).build();
		val oriUri = request.uri();
		val builder = UriComponentsBuilder.fromUri(oriUri);
		val path = oriUri.getPath();
		val prefix = receipt.getPath();
		val uri = builder.scheme(receipt.getScheme()).host(receipt.getHost()).port(receipt.getPort());

		if (StringUtils.isNotBlank(prefix)) {
			if (StringUtils.isNotBlank(path)) {
				uri.replacePath(prefix);
			}
			else {
				uri.path(prefix);
			}
		}

		val req = ServerRequest.from(request).uri(uri.build().toUri());
		HttpBrokerRequestWrapper.from(request).setRequestBody(context.body());

		val retryHandler = new RetryHandlerWrapper(context.broker().getRetryHandler());

		return httpProxy.handle(req.build(), retryHandler);
	}

	@Override
	public boolean isSync() {
		return sync;
	}

	static class RetryHandlerWrapper implements IRetryHandler {

		private final IRetryHandler parent;

		RetryHandlerWrapper(IRetryHandler parent) {
			this.parent = parent;
		}

		@Override
		public ServerRequest createRetryRequest(@Nonnull ServerRequest request) {
			return null;
		}

		@Override
		public boolean exceedsMaxRetries(int retried) {
			return false;
		}

		@Override
		public boolean needRetryByHttpStatus(int status) {
			return false;
		}

		@Override
		public boolean needRetryByErrCode(@Nonnull ServerRequest request, ResponseEntity<String> response) {
			if (parent != null && parent.needRenotify(response)) {
				throw new IllegalStateException("");
			}
			return false;
		}

		@Override
		public boolean needRenotify(ResponseEntity<String> response) {
			return false;
		}

		@Override
		public Duration getRetryInterval() {
			return Duration.ZERO;
		}

	}

}
