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
package com.apzda.cloud.queqiao.client.controller;

import com.apzda.cloud.queqiao.wx.WxClientMpProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/cb")
@RequiredArgsConstructor
@Slf4j
public class CallbackHandler {

	private final WxMpService wxMpService;

	private final WxClientMpProperties properties;

	@PostMapping(value = "/{account}/**", produces = "application/xml; charset=UTF-8")
	public String callback(@PathVariable("account") String account, @RequestBody String requestBody,
			@RequestParam("signature") String signature, @RequestParam("timestamp") String timestamp,
			@RequestParam("nonce") String nonce, @RequestHeader(value = "Host", required = false) String host,
			@RequestParam(name = "encrypt_type", required = false) String encType,
			@RequestParam(name = "msg_signature", required = false) String msgSignature, HttpServletRequest request) {
		log.error("Host: {}", new ServletServerHttpRequest(request).getHeaders().asSingleValueMap());
		val mpConfig = properties.getAccount().get(account);
		if (mpConfig == null) {
			return "success";
		}
		val wxService = wxMpService.switchoverTo(mpConfig.getAppId());
		if (!wxService.checkSignature(timestamp, nonce, signature)) {
			throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
		}
		String out = "";
		if (encType == null) {
			// 明文传输的消息
			WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);

			WxMpXmlOutMessage outMessage = WxMpXmlOutMessage.TEXT()
				.content("你好: " + inMessage.getContent())
				.fromUser(inMessage.getToUser())
				.toUser(inMessage.getFromUser())
				.build();

			out = outMessage.toXml();
		}
		else if ("aes".equalsIgnoreCase(encType)) {
			// aes加密的消息
			WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody, wxService.getWxMpConfigStorage(),
					timestamp, nonce, msgSignature);
			log.debug("\n消息解密后内容为：\n{} ", inMessage.toString());

			WxMpXmlOutMessage outMessage = WxMpXmlOutMessage.TEXT()
				.content("你好: " + inMessage.getFromUser())
				.fromUser(inMessage.getToUser())
				.toUser(inMessage.getFromUser())
				.build();

			out = outMessage.toEncryptedXml(wxService.getWxMpConfigStorage());
		}

		log.debug("\n组装回复信息：{}", out);
		return out;
	}

}
