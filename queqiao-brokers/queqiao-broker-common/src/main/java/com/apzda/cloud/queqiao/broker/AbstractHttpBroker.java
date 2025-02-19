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
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import com.apzda.cloud.queqiao.proxy.IHttpProxy;
import com.apzda.cloud.queqiao.proxy.IRetryHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class AbstractHttpBroker implements IBroker {

	protected String id;

	protected BrokerConfig config;

	protected IHttpProxy httpProxy;

	public boolean setup(String id, @Nonnull BrokerConfig config, @Nonnull ApplicationContext context) {
		this.id = id;
		this.config = config;
		this.httpProxy = context.getBean(IHttpProxy.class);
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

}
