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

import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.handler.CallbackHandlerFunction;
import com.apzda.cloud.queqiao.handler.RelayHandlerFunction;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration
public class QueQiaoEndpoints {

	@Bean
	public RouterFunction<ServerResponse> relayRouterFunction(QueQiaoProperties properties) {
		val func = new RelayHandlerFunction();
		val headerName = StringUtils.defaultIfBlank(properties.getUpstreamHeader(), QueQiaoVals.UPSTREAM_HEADER);

		return RouterFunctions.route().path("/**", builder -> {
			builder.nest(request -> request.headers().asHttpHeaders().containsKey(headerName), b -> {
				b.GET(func).POST(func).DELETE(func).PUT(func).PATCH(func);
			}).build();
		}).build();
	};

	@Bean
	public RouterFunction<ServerResponse> callbackRouterFunction(QueQiaoProperties properties) {
		val func = new CallbackHandlerFunction();
		val headerName = StringUtils.defaultIfBlank(properties.getUpstreamHeader(), QueQiaoVals.UPSTREAM_HEADER);

		return RouterFunctions.route().path(properties.theCallbackPathPattern(), builder -> {
			builder.nest(request -> !request.headers().asHttpHeaders().containsKey(headerName), b -> {
				b.GET(func).POST(func);
			}).build();
		}).build();
	};

}
