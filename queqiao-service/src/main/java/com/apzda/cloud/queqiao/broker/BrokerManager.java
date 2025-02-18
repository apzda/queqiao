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
package com.apzda.cloud.queqiao.broker;

import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.config.QueQiaoProperties;
import com.apzda.cloud.queqiao.exception.BrokerNotFoundException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public abstract class BrokerManager implements ApplicationContextAware, ApplicationListener<ApplicationReadyEvent> {

	private static final Map<String, IBroker> brokers = new ConcurrentHashMap<>();

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
		BrokerManager.applicationContext = applicationContext;
	}

	@Nonnull
	public static IBroker getBroker(@Nonnull String upstream) throws BrokerNotFoundException {
		val broker = brokers.get(upstream);
		if (broker == null) {
			throw new BrokerNotFoundException(upstream);
		}
		return broker;
	}

	public static void reload() {
		if (applicationContext != null) {
			log.info("Reloading Broker");
			val properties = applicationContext.getBean(QueQiaoProperties.class);
			val newBrokers = new ArrayList<String>(properties.getBroker().size());
			for (Map.Entry<String, BrokerConfig> brokerCfg : properties.getBroker().entrySet()) {
				val brokerId = brokerCfg.getKey();
				val config = brokerCfg.getValue();
				newBrokers.add(brokerId);
				brokers.compute(brokerId, (s, broker) -> {
					try {
						if (broker == null) {
							broker = BeanUtils.instantiateClass(config.getBroker());
						}
						try {
							broker.setup(config, applicationContext);
							log.info("Broker setup successfully: {} - {}", brokerId,
									properties.theCallbackPath(brokerId));
							return broker;
						}
						catch (Exception e) {
							log.error("Broker setup failed: {}", brokerId, e);
							broker.destroy();
							log.info("Broker destroyed for setup failed: {}", brokerId);
						}
					}
					catch (Exception e) {
						log.error("Cannot destroy broker: {}", brokerId, e);
					}
					return null;
				});
			}

			for (val brokerId : brokers.keySet()) {
				if (!newBrokers.contains(brokerId)) {
					val broker = brokers.remove(brokerId);
					if (broker != null) {
						try {
							broker.destroy();
							log.info("Broker destroyed: {}", brokerId);
						}
						catch (Exception e) {
							log.error("Cannot destroy broker: {}", brokerId, e);
						}
					}
				}
			}

			log.info("Broker reloaded: {}", newBrokers);
		}
	}

	@Override
	public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
		reload();
		log.info("Broker manager started");
	}

	@PreDestroy
	public void destroy() {
		for (val brokerConfig : brokers.entrySet()) {
			val brokerId = brokerConfig.getKey();
			val broker = brokerConfig.getValue();
			if (broker == null) {
				continue;
			}
			try {
				broker.destroy();
				log.info("Broker destroyed: {}", brokerId);
			}
			catch (Exception e) {
				log.error("Cannot destroy broker: {}", brokerId, e);
			}
		}
		log.info("Broker manager stopped");
	}

}
