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
package com.apzda.cloud.queqiao.handler;

import com.apzda.cloud.queqiao.broker.BrokerManager;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import static com.apzda.cloud.queqiao.constrant.QueQiaoVals.UPSTREAM_HEADER;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class RelayHandlerFunction implements HandlerFunction<ServerResponse> {

	@Override
	@Nonnull
	public ServerResponse handle(@Nonnull ServerRequest request) throws Exception {
		val upstreams = request.headers().header(UPSTREAM_HEADER);
		if (CollectionUtils.isEmpty(upstreams)) {
			return ServerResponse.status(404).build();
		}
		val upstream = upstreams.get(0);
		log.debug("Received request to upstream : {}", upstream);
		try {
			request.attributes().put(QueQiaoVals.BROKER_REQUEST_WRAPPER, HttpBrokerRequestWrapper.from(request));
			request.attributes()
				.put(QueQiaoVals.CONTENT_CACHING_REQUEST_WRAPPER,
						new ContentCachingRequestWrapper(request.servletRequest()));
			val broker = BrokerManager.getBroker(upstream);
			return broker.onRequest(request);
		}
		catch (Exception e) {
			log.error("Cannot handle: {}\nparams={}", request.uri(), request.params(), e);
			return ServerResponse.status(404).build();
		}
	}

}
