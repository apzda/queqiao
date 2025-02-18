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
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@ConfigurationProperties(prefix = "apzda.cloud.queqiao")
@Data
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class QueQiaoProperties {

	private String url = "http://localhost";

	private String callbackPath = "";

	private String upstreamHeader = QueQiaoVals.UPSTREAM_HEADER;

	private Map<String, BrokerConfig> broker = new HashMap<>();

	private HttpProxyConfig proxy = new HttpProxyConfig();

	public String theCallbackPath(String upstream) {
		var cb = callbackPath;
		if (StringUtils.isNotBlank(cb)) {
			cb = "/" + StringUtils.strip(cb, "/");
		}
		var host = StringUtils.stripEnd(url, "/");

		return String.format("%s%s/%s/", host, cb, upstream);
	}

	public String theCallbackPathPattern() {
		var cb = callbackPath;
		if (StringUtils.isNotBlank(cb)) {
			cb = "/" + StringUtils.strip(cb, "/");
		}
		return String.format("%s/{upstream:[0-9a-z_]+}/**", cb);
	}

}
