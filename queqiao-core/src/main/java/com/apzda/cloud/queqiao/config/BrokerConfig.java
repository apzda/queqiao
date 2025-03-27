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

import com.apzda.cloud.queqiao.broker.IBroker;
import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
public class BrokerConfig {

	/**
	 * 上游接口地址
	 */
	private String host;

	/**
	 * 代理人
	 */
	private Class<? extends IBroker> broker;

	private String appId;

	private String account;

	private String appKey;

	private String appSecret;

	private String token;

	// 额外配置ID
	private String extra;

	// 重试错误码
	private List<String> retryErrCodes = new ArrayList<>();

	// 重试HTTP响应码
	private List<Integer> retryHttpCodes = new ArrayList<>();

	// 重试次数
	private int retryTimes = 3;

	/**
	 * 重试间隔
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration retryInterval = Duration.ofSeconds(1);

	/**
	 * 通知(回调)配置
	 */
	private List<NotificationConfig> notifications = new ArrayList<>();

}
