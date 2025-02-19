/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
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
import jakarta.annotation.Nonnull;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface IBroker {

	default boolean setup(@Nonnull BrokerConfig config, @Nonnull ApplicationContext context) {
		return true;
	}

	default void destroy() {
	}

	@Nonnull
	ServerResponse onRequest(@Nonnull ServerRequest request);

	@Nonnull
	ServerResponse onCallback(@Nonnull ServerRequest request);

}
