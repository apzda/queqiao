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
package com.apzda.cloud.queqiao.broker.wechat.mp;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.queqiao.broker.AbstractHttpBroker;
import com.apzda.cloud.queqiao.broker.wechat.config.WxConfigProperties;
import com.apzda.cloud.queqiao.broker.wechat.config.WxMpExtraConfig;
import com.apzda.cloud.queqiao.broker.wechat.dto.WxAccessToken;
import com.apzda.cloud.queqiao.broker.wechat.http.WxCommonRequestWrapper;
import com.apzda.cloud.queqiao.broker.wechat.http.WxErrorResp;
import com.apzda.cloud.queqiao.broker.wechat.mp.handler.WxMpRetryHandler;
import com.apzda.cloud.queqiao.broker.wechat.mp.http.WxMpRequestWrapper;
import com.apzda.cloud.queqiao.config.BrokerConfig;
import com.apzda.cloud.queqiao.constrant.QueQiaoVals;
import com.apzda.cloud.queqiao.proxy.IRetryHandler;
import com.apzda.cloud.queqiao.storage.StringData;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.redis.WxRedisOps;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.config.WxMpHostConfig;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class WechatMpBroker extends AbstractHttpBroker {

	private static final String GET_ACCESS_TOKEN_URL = "/cgi-bin/token";

	private static final String GET_STABLE_ACCESS_TOKEN_URL = "/cgi-bin/stable_token";

	private final ScheduledExecutorService executor;

	private ScheduledFuture<?> refreshTask;

	private TempStorage storage;

	private IRetryHandler retryHandler;

	private WxMpExtraConfig extraConfig;

	private WxMpService wxMpService;

	public WechatMpBroker() {
		executor = Executors.newScheduledThreadPool(1);
	}

	@Override
	public boolean setup(@Nonnull BrokerConfig config, @Nonnull ApplicationContext context) {
		if (!super.setup(config, context)) {
			return false;
		}

		this.storage = context.getBean(TempStorage.class);
		val wxRedisOps = context.getBean(WxRedisOps.class);

		@SuppressWarnings("all")
		val conf = new WxMpRedisConfigImpl(wxRedisOps, "wxMp") {
			@Override
			public long getExpiresTime() {
				val expire = wxRedisOps.getExpire(getAccessTokenKey());
				if (expire == null) {
					return DateUtil.currentSeconds() - 1;
				}
				return DateUtil.currentSeconds() + expire;
			}
		};

		conf.setAppId(config.getAppId());
		conf.setSecret(config.getAppSecret());
		conf.setToken(config.getToken());
		val extra = config.getExtra();

		val extraProperties = context.getBean(WxConfigProperties.class);

		if (StringUtils.isNotBlank(extra) && extraProperties.getMp().containsKey(extra)) {
			extraConfig = extraProperties.getMp().get(extra);
			if (StringUtils.isNotBlank(extraConfig.getAesKey())) {
				conf.setAesKey(extraConfig.getAesKey());
			}
			conf.setUseStableAccessToken(extraConfig.isStableAccessToken());
		}

		wxMpService = new WxMpServiceImpl();
		wxMpService.setWxMpConfigStorage(conf);

		retryHandler = new WxMpRetryHandler(config, wxMpService);

		if (refreshTask != null) {
			refreshTask.cancel(true);
			refreshTask = null;
		}

		refreshTask = executor.scheduleWithFixedDelay(() -> {
			try {
				val expiredAt = wxMpService.getWxMpConfigStorage().getExpiresTime();
				val force = expiredAt - DateUtil.currentSeconds() <= 300;
				val accessToken = wxMpService.getAccessToken(force);
				log.debug("Refresh(force={}) accessToken[{}] successfully: {}", force, conf.getAppId(), accessToken);

				try {
					val at = storage.load(accessToken, StringData.class);
					if (at.isEmpty() || at.get().getExpireTime().toSeconds() <= 0) {
						val expiresTime = conf.getExpiresTime() - DateUtil.currentSeconds();
						storage.save(accessToken, new StringData().setExpireTime(Duration.ofSeconds(expiresTime * 2)));
					}
				}
				catch (Exception e) {
					log.warn("Can't save accessToken[{}] - {}", config.getAppId(), e.getMessage());
				}
			}
			catch (Exception e) {
				log.warn("Can not get accessToken[{}] - {}", config.getAppId(), e.getMessage());
			}
		}, 0, 60, TimeUnit.SECONDS);

		log.info("WechatMpBroker setup complete: {} - {}", config, extraConfig);
		return true;
	}

	@Override
	public void destroy() {
		if (refreshTask != null) {
			refreshTask.cancel(true);
		}
		executor.shutdown();
		log.info("WechatMpBroker executor destroyed");
	}

	@Nonnull
	@Override
	public ServerResponse onRequest(@Nonnull ServerRequest request) throws URISyntaxException {
		val query = new WxMpRequestWrapper(request);
		val uri = request.uri();
		val path = uri.getPath();
		if (GET_ACCESS_TOKEN_URL.equals(path) || GET_STABLE_ACCESS_TOKEN_URL.equals(path)) {
			return getAccessToken(query);
		}
		else {
			val response = checkAccessToken(query, path);
			if (response != null) {
				return response;
			}
			// 转发前准备
			val builder = UriComponentsBuilder.fromUri(uri).query(null);
			val realHost = request.headers().firstHeader(QueQiaoVals.WX_REAL_HOST_HEADER);

			final URI realUri;
			if ("apiHost".equals(realHost)) {
				realUri = UriComponentsBuilder.fromUriString(WxMpHostConfig.API_DEFAULT_HOST_URL).build().toUri();
			}
			else if ("mpHost".equals(realHost)) {
				realUri = UriComponentsBuilder.fromUriString(WxMpHostConfig.MP_DEFAULT_HOST_URL).build().toUri();
			}
			else if ("openHost".equals(realHost)) {
				realUri = UriComponentsBuilder.fromUriString(WxMpHostConfig.OPEN_DEFAULT_HOST_URL).build().toUri();
			}
			else {
				return ServerResponse.status(404).build();
			}

			val wxUri = builder.scheme(realUri.getScheme())
				.host(realUri.getHost())
				.port(realUri.getPort())
				.queryParams(query.queryParams())
				.build()
				.toUri();

			val req = ServerRequest.from(request)
				.params(Map::clear)
				.uri(wxUri)
				.attribute(QueQiaoVals.BROKER_REQUEST_WRAPPER, query)
				.build();

			try {
				return forward(req, retryHandler);
			}
			catch (Exception e) {
				return ServerResponse.status(500).body(e);
			}
		}
	}

	private ServerResponse checkAccessToken(WxMpRequestWrapper query, String uri) {
		var accessToken = query.getAccessToken();
		if (StringUtils.isBlank(accessToken) || !storage.exist(accessToken)) {
			// 不合法的 access_token
			return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(WxErrorResp.error(40014));
		}
		final String localAccessToken;
		try {
			localAccessToken = wxMpService.getAccessToken();
		}
		catch (WxErrorException e) {
			log.error("Can't get local accessToken[{}] to call({}) - {}", query.getAppid(), uri, e.getMessage(), e);
			val error = e.getError();
			return ServerResponse.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(error != null ? error : WxErrorResp.error(-1));
		}
		catch (Exception e) {
			log.error("Can't get local accessToken[{}] to call({}) - {}", query.getAppid(), uri, e.getMessage(), e);
			return ServerResponse.status(502).build();
		}

		if (!localAccessToken.equals(accessToken)) {
			query.getParams().replace("access_token", accessToken);
		}
		return null;
	}

	@Nonnull
	private ServerResponse getAccessToken(@Nonnull WxCommonRequestWrapper query) {
		try {
			if (!query.checkAuthentication(config)) {
				// 获取 access_token 时 AppSecret 错误
				return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(WxErrorResp.error(40001));
			}
			try {
				val realAccessToken = wxMpService.getAccessToken();
				val wxAccessToken = new WxAccessToken();
				wxAccessToken.setAccessToken(realAccessToken);
				val expiresTime = wxMpService.getWxMpConfigStorage().getExpiresTime();

				wxAccessToken.setExpiresIn((int) (expiresTime - DateUtil.currentSeconds()));

				return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(wxAccessToken);
			}
			catch (WxErrorException e) {
				log.error("Can't get access token[{}] - {}", query.getAppid(), e.getMessage(), e);
				val error = e.getError();
				return ServerResponse.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(error != null ? error : WxErrorResp.error(-1));
			}
			catch (Exception e) {
				log.error("Can't get access token[{}] - {}", query.getAppid(), e.getMessage(), e);
				return ServerResponse.status(502).build();
			}
		}
		catch (Exception e) {
			return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(WxErrorResp.error(-1));
		}
	}

	@Nonnull
	@Override
	public ServerResponse onCallback(@Nonnull ServerRequest request) {
		val method = request.method();
		val query = WxCommonRequestWrapper.from(request);
		val timestamp = query.getTimestamp();
		val nonce = query.getNonce();
		val signature = query.getSignature();
		if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
			return ServerResponse.status(403).body("Invalid Signature!");
		}
		if (method == HttpMethod.GET) {
			val echoStr = query.getEchoStr();
			if (StringUtils.isNotBlank(echoStr)) {
				return ServerResponse.ok().body(echoStr);
			}
			else {
				return ServerResponse.status(403).body("echostr is blank!");
			}
		}
		else if (method == HttpMethod.POST) {
			val openid = query.getOpenid();
			val encryptType = query.getEncryptType();
			val msgSignature = query.getMsgSignature();
			val requestBody = getRequestBody(request);
			log.debug("""
					 接收微信请求：[openid=[{}], [signature=[{}], encType=[{}], msgSignature=[{}],\
					 timestamp=[{}], nonce=[{}], requestBody=[
					{}
					]""", openid, signature, encryptType, msgSignature, timestamp, nonce, requestBody);
			final WxMpXmlMessage inMessage;
			if (encryptType == null) {
				inMessage = WxMpXmlMessage.fromXml(requestBody);
				log.debug("\n消息内容为：\n{} ", inMessage.toString());
			}
			else if ("aes".equalsIgnoreCase(encryptType)) {
				// aes加密的消息
				inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody, wxMpService.getWxMpConfigStorage(), timestamp,
						nonce, msgSignature);
				log.debug("\n消息解密后内容为：\n{} ", inMessage.toString());
			}
			else {
				return ServerResponse.status(HttpStatus.NO_CONTENT).body("");
			}
			// todo: 回调路由
			return ServerResponse.ok().body("success");
		}
		return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
	}

	@Nonnull
	private String getRequestBody(@Nonnull ServerRequest request) {
		try {
			return request.body(String.class);
		}
		catch (Exception e) {
			return "<xml>" + e.getMessage() + "</xml>";
		}
	}

}
