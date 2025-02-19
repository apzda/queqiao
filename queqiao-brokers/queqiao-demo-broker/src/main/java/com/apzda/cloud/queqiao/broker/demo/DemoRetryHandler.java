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

import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import com.apzda.cloud.queqiao.proxy.AbstractRetryHandler;
import com.apzda.cloud.queqiao.utils.MultipartBodyUtil;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.function.ServerRequest;

import java.io.File;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class DemoRetryHandler extends AbstractRetryHandler {

	public DemoRetryHandler(@Nonnull BrokerConfig config) {
		super(config);
	}

	@Override
	public ServerRequest createRetryRequest(@Nonnull ServerRequest request) {
		try {
			val wrapper = HttpBrokerRequestWrapper.from(request);
			val multipartData = wrapper.getMultipartData();
			if (multipartData != null) {
				multipartData.put("name", List.of(new HttpEntity<>("DemoBroker")));
				val map = new LinkedMultiValueMap<String, Object>();
				map.add("dockerFile", new File("./README.md"));
				MultipartBodyUtil.merge(map, multipartData);

				wrapper.setMultipartData(multipartData);
			}
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return request;
	}

	@Override
	public boolean needRetryByErrCode(@Nonnull ServerRequest request, ResponseEntity<String> response) {
		return true;
	}

}
