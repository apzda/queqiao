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

import com.apzda.cloud.queqiao.core.IBroker;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
public class BrokerConfig {

	/**
	 * 接口地址
	 */
	private String api;

	/**
	 * 代理人ID
	 */
	private Class<? extends IBroker> broker;

	private String appId;

	private String account;

	private String appKey;

	private String appSecret;

	private String token;

	private String extra;

	private Map<String, Environment> environment = new HashMap<>();

}
