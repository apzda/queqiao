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
package com.apzda.cloud.queqiao.broker.demo;

import com.apzda.cloud.queqiao.broker.AbstractHttpBroker;
import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.proxy.IRetryHandler;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class DemoBroker extends AbstractHttpBroker {

	private IRetryHandler retryHandler;

	@Override
	public boolean setup(String id, @Nonnull BrokerConfig config, @Nonnull ApplicationContext context) {
		if (!super.setup(id, config, context)) {
			return false;
		}
		retryHandler = new DemoRetryHandler(config);
		return true;
	}

	@Nonnull
	@Override
	public ServerResponse onRequest(@Nonnull ServerRequest request) {
		val serverRequest = changeTarget(request);

		val uri = UriComponentsBuilder.fromUriString(serverRequest.uri().toString())
			.replacePath("/_" + serverRequest.uri().getPath())
			.build()
			.toUri();

		val req = ServerRequest.from(serverRequest).uri(uri).build();
		return forward(req, retryHandler);
	}

	@Nonnull
	@Override
	public ServerResponse onCallback(@Nonnull ServerRequest request) {
		return ServerResponse.ok().body("Demo");
	}

}
