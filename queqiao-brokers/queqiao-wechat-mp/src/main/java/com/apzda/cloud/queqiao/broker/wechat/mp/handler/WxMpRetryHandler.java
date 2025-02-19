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
package com.apzda.cloud.queqiao.broker.wechat.mp.handler;

import com.apzda.cloud.queqiao.broker.wechat.http.WxCommonRequestWrapper;
import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.proxy.AbstractRetryHandler;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class WxMpRetryHandler extends AbstractRetryHandler {

	private final List<String> retryErrCodes;

	private final WxMpService wxMpService;

	public WxMpRetryHandler(BrokerConfig config, WxMpService wxMpService) {
		super(config);
		this.retryErrCodes = config.getRetryErrCodes();
		if (retryErrCodes.isEmpty()) {
			retryErrCodes.add("42001");
			retryErrCodes.add("40014");
		}
		this.wxMpService = wxMpService;
	}

	@Override
	@Nonnull
	public ServerRequest createRetryRequest(@Nonnull ServerRequest request) {
		val commonQuery = request.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER);
		if (commonQuery.isPresent()) {
			val requestWrapper = (WxCommonRequestWrapper) commonQuery.get();

			val builder = UriComponentsBuilder.fromUri(request.uri()).query(null);
			// 主要更新accessToken
			val wxUri = builder.queryParams(requestWrapper.queryParams()).build().toUri();

			return ServerRequest.from(request).params(Map::clear).uri(wxUri).build();
		}
		return request;
	}

	@Override
	public boolean needRetryByErrCode(@Nonnull ServerRequest request, @Nonnull ResponseEntity<String> response) {
		val wxError = WxError.fromJson(response.getBody());
		val errorCode = wxError.getErrorCode();
		if (errorCode == 0) {
			return false;
		}

		if (retryErrCodes.contains(String.valueOf(errorCode))) {
			val commonQuery = request.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER);
			if (commonQuery.isPresent()) {
				val query = (WxCommonRequestWrapper) commonQuery.get();
				try {
					if (errorCode == 42001 || errorCode == 40014 || errorCode == 40001) {
						val accessToken = wxMpService.getAccessToken(true);
						query.getParams().put("access_token", accessToken);
					}
					return true;
				}
				catch (WxErrorException e) {
					log.warn("Stop Retry since can't refresh accessToken[{}] - {}", config.getAppId(), e.getError());
					return false;
				}
			}
		}

		return false;
	}

}
