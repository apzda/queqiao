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
package com.apzda.cloud.queqiao.proxy;

import com.apzda.cloud.queqiao.config.BrokerConfig;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import java.time.Duration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class AbstractRetryHandler implements IRetryHandler {

	protected final BrokerConfig config;

	protected final int maxRetries;

	public AbstractRetryHandler(@Nonnull BrokerConfig config) {
		this.config = config;
		maxRetries = config.getRetryTimes();

	}

	@Override
	public Duration getRetryInterval() {
		return config.getRetryInterval();
	}

	@Override
	public boolean exceedsMaxRetries(int retried) {
		return retried >= maxRetries;
	}

	@Override
	public boolean needRetryByHttpStatus(int status) {
		val retryHttpCodes = config.getRetryHttpCodes();
		if (!CollectionUtils.isEmpty(retryHttpCodes)) {
			return retryHttpCodes.contains(status);
		}

		return false;
	}

	@Override
	public boolean needRenotify(ResponseEntity<String> response) {
		return false;
	}

}
