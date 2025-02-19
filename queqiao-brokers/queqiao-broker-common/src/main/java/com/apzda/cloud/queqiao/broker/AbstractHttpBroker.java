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
package com.apzda.cloud.queqiao.broker;

import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.config.NotificationConfig;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import com.apzda.cloud.queqiao.notify.DefaultNotificationHandler;
import com.apzda.cloud.queqiao.notify.INotificationHandler;
import com.apzda.cloud.queqiao.postman.IPostman;
import com.apzda.cloud.queqiao.proxy.IHttpProxy;
import com.apzda.cloud.queqiao.proxy.IRetryHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public abstract class AbstractHttpBroker implements IBroker {

	protected final List<INotificationHandler> syncHandlers = new ArrayList<>();

	protected final List<INotificationHandler> asyncHandlers = new ArrayList<>();

	protected String id;

	protected BrokerConfig config;

	protected IHttpProxy httpProxy;

	public boolean setup(String id, @Nonnull BrokerConfig config, @Nonnull ApplicationContext context) {
		this.id = id;
		this.config = config;
		this.httpProxy = context.getBean(IHttpProxy.class);
		val postmanContainer = new HashMap<String, IPostman>();
		context.getBeanProvider(IPostman.class).stream().forEach(postman -> {
			postmanContainer.put(postman.getId(), postman);
		});
		// 构建通知器
		val notifications = config.getNotifications();
		for (NotificationConfig notification : notifications) {
			val postman = notification.getPostman();
			if (StringUtils.isBlank(postman)) {
				throw new IllegalArgumentException(
						String.format("Notification postman of broker[%s] is null or empty: \n", id) + notification);
			}
			try {
				val postmanBean = postmanContainer.get(postman);
				if (postmanBean == null) {
					throw new IllegalArgumentException(
							String.format("Notification postman of broker[%s] not found: \n", id) + notification);
				}
				val handler = new DefaultNotificationHandler(postmanBean, notification);
				if (postmanBean.isSync()) {
					syncHandlers.add(handler);
				}
				else {
					asyncHandlers.add(handler);
				}
			}
			catch (Exception e) {
				throw new IllegalArgumentException(
						String.format("Notification postman[%s] of broker[%s] not found: \n", postman, id)
								+ notification);
			}
		}
		return true;
	}

	@Nonnull
	protected ServerRequest changeTarget(@Nonnull ServerRequest request) {
		val builder = UriComponentsBuilder.fromUri(request.uri());
		val host = UriComponentsBuilder.fromUriString(config.getHost()).build();

		builder.scheme(host.getScheme()).host(host.getHost()).port(host.getPort());

		if (host.getPath() != null && !"/".equals(host.getPath())) {
			builder.replacePath(
					String.format("%s%s", host.getPath(), StringUtils.defaultIfBlank(request.uri().getPath(), "")));
		}

		val wrapper = new HttpBrokerRequestWrapper(request);

		return ServerRequest.from(request)
			.uri(builder.build().toUri())
			.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER, wrapper)
			.build();
	}

	@Nonnull
	protected ServerResponse forward(ServerRequest request, @Nullable IRetryHandler retry) {
		return httpProxy.handle(request, retry);
	}

	@Nonnull
	protected ServerResponse forward(ServerRequest request) {
		return forward(request, null);
	}

	@Nullable
	protected ServerResponse notify(@Nullable Object response, String body, @Nonnull ServerRequest request) {
		val context = new HashMap<String, Object>(4);
		context.put("response", response);
		context.put("content", body);
		context.put("params", request.params().toSingleValueMap());
		context.put("headers", request.headers().asHttpHeaders().toSingleValueMap());
		val atomicInteger = new AtomicInteger(0);
		// async first
		for (val asyncHandler : asyncHandlers) {
			try {
				if (asyncHandler.matches(context)) {
					atomicInteger.incrementAndGet();
					CompletableFuture.runAsync(() -> {
						asyncHandler.notify(this, response, body, request);
					});
				}
			}
			catch (Exception e) {
				log.warn("Async handler failed", e);
			}
		}

		for (INotificationHandler syncHandler : syncHandlers) {
			if (syncHandler.matches(context)) {
				atomicInteger.incrementAndGet();
				return syncHandler.notify(this, response, body, request);
			}
		}
		if (atomicInteger.get() == 0) {
			log.warn("""
					No Postman found for broker[{}]
					[Config]: {}
					[Context]: {}
					""", id, config.getNotifications(), context);
		}

		return ServerResponse.ok().build();
	}

}
