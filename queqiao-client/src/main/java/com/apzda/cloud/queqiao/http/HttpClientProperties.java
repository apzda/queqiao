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
package com.apzda.cloud.queqiao.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@ConfigurationProperties(prefix = "weixin.http")
public class HttpClientProperties {

	private int connectionRequestTimeout = -1;

	private int connectionTimeout = 5000;

	private int soTimeout = 5000;

	private int idleConnTimeout = 60000;

	private int checkWaitTime = 60000;

	private int maxConnPerHost = 10;

	private int maxTotalConn = 50;

	private String userAgent;

	private String proxyHost;

	private int proxyPort;

}
