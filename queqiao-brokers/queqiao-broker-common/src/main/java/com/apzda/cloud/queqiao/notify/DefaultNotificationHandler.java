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
package com.apzda.cloud.queqiao.notify;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.apzda.cloud.queqiao.broker.IBroker;
import com.apzda.cloud.queqiao.config.NotificationConfig;
import com.apzda.cloud.queqiao.http.HttpBrokerRequestWrapper;
import com.apzda.cloud.queqiao.postman.IPostman;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationHandler implements INotificationHandler {

	private static final Express4Runner RUNNER = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

	private static final QLOptions QL_OPTIONS = QLOptions.builder().cache(false).build();

	private final IPostman postman;

	@Getter
	private final NotificationConfig config;

	@Override
	public boolean matches(Map<String, Object> context) {
		if (config.getFilter().isEmpty()) {
			return true;
		}

		for (val filter : config.getFilter()) {
			try {
				val value = RUNNER.execute(filter, context, QL_OPTIONS);

				if (value != null) {
					if (value instanceof Boolean) {
						return Boolean.TRUE.equals(value);
					}
					else if (value instanceof Collection<?> values) {
						return !CollectionUtils.isEmpty(values);
					}
					else if (value instanceof Object[] values) {
						return values.length > 0;
					}
					else if (value instanceof String strVal) {
						return StringUtils.isNotBlank(strVal) && !"false".equalsIgnoreCase(strVal);
					}
					return true;
				}
			}
			catch (Exception e) {
				log.error("""
						Parsing filter failed:
						[Expression]: {}
						[Context]:    {}
						[Exception]:  {}""", filter, context, e.getMessage());
			}
		}

		return false;
	}

	@Nullable
	@Override
	public ServerResponse notify(@Nonnull IBroker broker, @Nullable Object response, String body,
			@Nonnull ServerRequest request) {
		val context = new NotifyContext(broker, response, body, request, config.getReceipt(), config.getOptions());
		return notify(context, 0);
	}

	@Nullable
	ServerResponse notify(NotifyContext context, int retried) {
		try {
			HttpBrokerRequestWrapper.from(context.request()).setRequestBody(context.body());
			val response = postman.notify(context);
			if (response.statusCode().is5xxServerError()) {
				return doRetry(context, retried);
			}
			return response;
		}
		catch (Exception e) {
			return doRetry(context, retried);
		}
	}

	ServerResponse doRetry(NotifyContext context, int retried) {
		val retries = config.getRetries();
		if (retried > retries.size() - 1) {
			log.error("""
					Notification send failed:
					[Retried]: {}
					[Context]: {}
					""", retried, context);
			return null;
		}
		val duration = retries.get(retried);
		try {
			// 会卡死
			TimeUnit.MILLISECONDS.sleep(duration.toMillis());
		}
		catch (InterruptedException e) {
			log.error("Cannot Believe It - {}", e.getMessage());
			return null;
		}

		log.warn("Retrying({}) after {}ms - {}", retried + 1, duration.toMillis(), context.receipt());
		return notify(context, retried + 1);
	}

}
