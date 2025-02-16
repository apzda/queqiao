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
package com.apzda.cloud.queqiao.broker.wechat.common;

import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.queqiao.storage.StringData;
import jakarta.annotation.PreDestroy;
import lombok.val;
import me.chanjar.weixin.common.redis.WxRedisOps;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class WxInfraOps implements WxRedisOps {

	private final TempStorage storage;

	private final Map<String, Lock> locks = new HashMap<>();

	public WxInfraOps(TempStorage tempStorage) {
		this.storage = tempStorage;
	}

	@Override
	public String getValue(String s) {
		return storage.load(s, StringData.class).map(StringData::toString).orElse(null);
	}

	@Override
	public void setValue(String s, String s1, int i, TimeUnit timeUnit) {
		val data = new StringData().setData(s1).setExpireTime(Duration.of(i, timeUnit.toChronoUnit()));
		try {
			storage.save(s, data);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Long getExpire(String s) {
		return storage.getDuration(s).getSeconds();
	}

	@Override
	public void expire(String s, int i, TimeUnit timeUnit) {
		storage.expire(s, Duration.ofSeconds(timeUnit.toSeconds(i)));
	}

	@Override
	public Lock getLock(String s) {
		return locks.computeIfAbsent(s, storage::getLock);
	}

	@PreDestroy
	public void destroy() {
		locks.forEach((k, v) -> {
			storage.deleteLock(k);
		});
	}

}
