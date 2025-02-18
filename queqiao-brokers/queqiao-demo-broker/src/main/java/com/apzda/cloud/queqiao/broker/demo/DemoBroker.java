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
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URISyntaxException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class DemoBroker extends AbstractHttpBroker {

	@Nonnull
	@Override
	public ServerResponse onRequest(@Nonnull ServerRequest request) throws URISyntaxException {
		val serverRequest = changeTarget(request);

		val uri = UriComponentsBuilder.fromUriString(serverRequest.uri().toString())
			.replacePath("/_" + serverRequest.uri().getPath())
			.build()
			.toUri();
		serverRequest.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER).orElse(serverRequest);
		val req = ServerRequest.from(serverRequest).uri(uri).build();
		return forward(req);
	}

	@Nonnull
	@Override
	public ServerResponse onCallback(@Nonnull ServerRequest request) {
		return ServerResponse.ok().body("Demo");
	}

}
