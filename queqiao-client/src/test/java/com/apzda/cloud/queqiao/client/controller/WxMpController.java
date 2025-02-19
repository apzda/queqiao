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

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplate;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/test/wx")
@RequiredArgsConstructor
public class WxMpController {

	private final WxMpService wxService;

	@Value("${weixin.mp.account.abc.app-id}")
	private String appId;

	@GetMapping("token")
	public String getAccessToken() throws WxErrorException {
		return wxService.switchoverTo(appId).getAccessToken();
	}

	@GetMapping("template")
	public List<WxMpTemplate> getMenu() throws WxErrorException {
		val ts = wxService.getTemplateMsgService();
		val allPrivateTemplate = ts.getAllPrivateTemplate();
		return allPrivateTemplate;
	}

	@GetMapping("send")
	public String send() throws WxErrorException {
		val ts = wxService.getTemplateMsgService();
		WxMpTemplateMessage msg = WxMpTemplateMessage.builder()
			.clientMsgId(String.valueOf(DateUtil.current()))
			.toUser("o7L3v7Ndtc5RDJGiCP01N0hjJPy8")
			.templateId("LheBvLu605JCqkbGiELQ8cPOb2aItwiOcmNOJJ_foiI")
			.data(List.of(new WxMpTemplateData("v1", "鹊桥")))
			.build();

		return ts.sendTemplateMsg(msg);
	}

}
