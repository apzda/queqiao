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
package com.apzda.cloud.queqiao.broker.wechat.autoconfig;

import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.queqiao.broker.wechat.common.WxInfraOps;
import com.apzda.cloud.queqiao.broker.wechat.config.WxConfigProperties;
import me.chanjar.weixin.common.redis.WxRedisOps;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = "com.apzda.cloud.gsvc.autoconfigure.GsvcAutoConfiguration")
@EnableConfigurationProperties(WxConfigProperties.class)
public class WxCommonAutoConfiguration {

	@Bean
	@ConditionalOnBean(TempStorage.class)
	WxRedisOps wxRedisOps(TempStorage tempStorage) {
		return new WxInfraOps(tempStorage);
	}

}
