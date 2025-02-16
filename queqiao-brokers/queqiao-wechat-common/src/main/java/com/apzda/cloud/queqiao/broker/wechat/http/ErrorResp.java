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
package com.apzda.cloud.queqiao.broker.wechat.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResp {

	private static final Map<Integer, String> errors = new HashMap<>() {
		{
			put(-1, "系统繁忙，此时请开发者稍候再试");
			put(0, "请求成功");
			put(40001, "获取 access_token 时 AppSecret 错误，或者 access_token 无效。请开发者认真比对 AppSecret 的正确性，或查看是否正在为恰当的公众号调用接口");
			put(40002, "不合法的凭证类型");
			put(40003, "不合法的 OpenID ，请开发者确认 OpenID （该用户）是否已关注公众号，或是否是其他公众号的 OpenID");
			put(40004, "不合法的媒体文件类型");
			put(40005, "不合法的文件类型");
			put(40006, "不合法的文件大小");
			put(40007, "不合法的媒体文件 id");
			put(40008, "不合法的消息类型");
			put(40009, "不合法的图片文件大小");
			put(40010, "不合法的语音文件大小");
			put(40011, "不合法的视频文件大小");
			put(40012, "不合法的缩略图文件大小");
			put(40013, "不合法的 AppID ，请开发者检查 AppID 的正确性，避免异常字符，注意大小写");
			put(40014, "不合法的 access_token ，请开发者认真比对 access_token 的有效性（如是否过期），或查看是否正在为恰当的公众号调用接口");
			put(40015, "不合法的菜单类型");
			put(40016, "不合法的按钮个数");
			put(40017, "不合法的按钮类型");
			put(40018, "不合法的按钮名字长度");
			put(40019, "不合法的按钮 KEY 长度");
			put(40020, "不合法的按钮 URL 长度");
			put(40021, "不合法的菜单版本号");
			put(40022, "不合法的子菜单级数");
			put(40023, "不合法的子菜单按钮个数");
			put(40024, "不合法的子菜单按钮类型");
			put(40025, "不合法的子菜单按钮名字长度");
			put(40026, "不合法的子菜单按钮 KEY 长度");
			put(40027, "不合法的子菜单按钮 URL 长度");
			put(40028, "不合法的自定义菜单使用用户");
			put(40029, "无效的 oauth_code");
			put(40030, "不合法的 refresh_token");
			put(40031, "不合法的 openid 列表");
			put(40032, "不合法的 openid 列表长度");
			put(40033, "不合法的请求字符，不能包含 \\uxxxx 格式的字符");
			put(40035, "不合法的参数");
			put(40038, "不合法的请求格式");
			put(40039, "不合法的 URL 长度");
			put(40048, "无效的url");
			put(40050, "不合法的分组 id");
			put(40051, "分组名字不合法");
			put(40060, "删除单篇图文时，指定的 article_idx 不合法");
			put(40117, "分组名字不合法");
			put(40118, "media_id 大小不合法");
			put(40119, "button 类型错误");
			put(40120, "子 button 类型错误");
			put(40121, "不合法的 media_id 类型");
			put(40125, "无效的appsecret");
			put(40132, "微信号不合法");
			put(40137, "不支持的图片格式");
			put(40155, "请勿添加其他公众号的主页链接");
			put(40163, "oauth_code已使用");
			put(40227, "标题为空");
			put(40243, "AppSecret已被冻结，请登录MP解冻后再次调用");
			put(40249, "不支持下发营销/推广类的消息内容");
			put(40250, "下发消息内容不规范（包含空值等），建议检查内容规范性后再下发");
			put(40251, "因历史违规导致平台限制账号调用上限，当前已到达下发上限");
			put(40252, "正在调用的模板下发的部分内容已进入平台审核流程，在审核完成前，相关内容暂时无法下发");
			put(41001, "缺少 access_token 参数");
			put(41002, "缺少 appid 参数");
			put(41003, "缺少 refresh_token 参数");
			put(41004, "缺少 secret 参数");
			put(41005, "缺少多媒体文件数据");
			put(41006, "缺少 media_id 参数");
			put(41007, "缺少子菜单数据");
			put(41008, "缺少 oauth code");
			put(41009, "缺少 openid");
			put(42001, "access_token 超时，请检查 access_token 的有效期，请参考基础支持 - 获取 access_token 中，对 access_token 的详细机制说明");
			put(42002, "refresh_token 超时");
			put(42003, "oauth_code 超时");
			put(42007, "用户修改微信密码， accesstoken 和 refreshtoken 失效，需要重新授权");
			put(42010, "相同 media_id 群发过快，请重试");
			put(43001, "需要 GET 请求");
			put(43002, "需要 POST 请求");
			put(43003, "需要 HTTPS 请求");
			put(43004, "需要接收者关注");
			put(43005, "需要好友关系");
			put(43019, "需要将接收者从黑名单中移除");
			put(43116, "该模板因滥用被滥用过多，已被限制下发");
			put(44001, "多媒体文件为空");
			put(44002, "POST 的数据包为空");
			put(44003, "图文消息内容为空");
			put(44004, "文本消息内容为空");
			put(44008, "音频内容审核失败");
			put(44009, "图文中的音频内容审核失败");
			put(45001, "多媒体文件大小超过限制");
			put(45002, "消息内容超过限制");
			put(45003, "标题字段超过限制");
			put(45004, "描述字段超过限制");
			put(45005, "链接字段超过限制");
			put(45006, "图片链接字段超过限制");
			put(45007, "语音播放时间超过限制");
			put(45008, "图文消息超过限制");
			put(45009, "接口调用超过限制");
			put(45010, "创建菜单个数超过限制");
			put(45011, "API 调用太频繁，请稍候再试");
			put(45015, "回复时间超过限制");
			put(45016, "系统分组，不允许修改");
			put(45017, "分组名字过长");
			put(45018, "分组数量超过上限");
			put(45047, "客服接口下行条数超过上限");
			put(45064, "创建菜单包含未关联的小程序");
			put(45065, "相同 clientmsgid 已存在群发记录，返回数据中带有已存在的群发任务的 msgid");
			put(45066, "相同 clientmsgid 重试速度过快，请间隔1分钟重试");
			put(45067, "clientmsgid 长度超过限制");
			put(45110, "作者字数超出限制");
			put(46001, "不存在媒体数据");
			put(46002, "不存在的菜单版本");
			put(46003, "不存在的菜单数据");
			put(46004, "不存在的用户");
			put(47001, "解析 JSON/XML 内容错误");
			put(47003, "参数值不符合限制要求，详情可参考参数值内容限制说明");
			put(48001, "api 功能未授权，请确认公众号已获得该接口，可以在公众平台官网 - 开发者中心页中查看接口权限");
			put(48002, "粉丝拒收消息（粉丝在公众号选项中，关闭了 “ 接收消息 ” ）");
			put(48004, "api 接口被封禁，请登录 mp.weixin.qq.com 查看详情");
			put(48005, "api 禁止删除被自动回复和自定义菜单引用的素材");
			put(48006, "api 禁止清零调用次数，因为清零次数达到上限");
			put(48008, "没有该类型消息的发送权限");
			put(48021, "自动保存的草稿无法预览/发送，请先手动保存草稿");
			put(50001, "用户未授权该 api");
			put(50002, "用户受限，可能是违规后接口被封禁");
			put(50005, "用户未关注公众号");
			put(53500, "发布功能被封禁");
			put(53501, "频繁请求发布");
			put(53502, "Publish ID 无效");
			put(53600, "Article ID 无效");
			put(61451, "参数错误 (invalid parameter)");
			put(61452, "无效客服账号 (invalid kf_account)");
			put(61453, "客服账号已存在 (kf_account exsited)");
			put(61454, "客服账号名长度超过限制 ( 仅允许 10 个英文字符，不包括 @ 及 @ 后的公众号的微信号 )(invalid   kf_acount length)");
			put(61455, "客服账号名包含非法字符 ( 仅允许英文 + 数字 )(illegal character in     kf_account)");
			put(61456, "客服账号个数超过限制 (10 个客服账号 )(kf_account count exceeded)");
			put(61457, "无效头像文件类型 (invalid   file type)");
			put(61450, "系统错误 (system error)");
			put(61500, "日期格式错误");
			put(63001, "部分参数为空");
			put(63002, "无效的签名");
			put(65301, "不存在此 menuid 对应的个性化菜单");
			put(65302, "没有相应的用户");
			put(65303, "没有默认菜单，不能创建个性化菜单");
			put(65304, "MatchRule 信息为空");
			put(65305, "个性化菜单数量受限");
			put(65306, "不支持个性化菜单的账号");
			put(65307, "个性化菜单信息为空");
			put(65308, "包含没有响应类型的 button");
			put(65309, "个性化菜单开关处于关闭状态");
			put(65310, "填写了省份或城市信息，国家信息不能为空");
			put(65311, "填写了城市信息，省份信息不能为空");
			put(65312, "不合法的国家信息");
			put(65313, "不合法的省份信息");
			put(65314, "不合法的城市信息");
			put(65316, "该公众号的菜单设置了过多的域名外跳（最多跳转到 3 个域名的链接）");
			put(65317, "不合法的 URL");
			put(87009, "无效的签名");
			put(9001001, "POST 数据参数不合法");
			put(9001002, "远端服务不可用");
			put(9001003, "Ticket 不合法");
			put(9001004, "获取摇周边用户信息失败");
			put(9001005, "获取商户信息失败");
			put(9001006, "获取 OpenID 失败");
			put(9001007, "上传文件缺失");
			put(9001008, "上传素材的文件类型不合法");
			put(9001009, "上传素材的文件尺寸不合法");
			put(9001010, "上传失败");
			put(9001020, "账号不合法");
			put(9001021, "已有设备激活率低于 50% ，不能新增设备");
			put(9001022, "设备申请数不合法，必须为大于 0 的数字");
			put(9001023, "已存在审核中的设备 ID 申请");
			put(9001024, "一次查询设备 ID 数量不能超过 50");
			put(9001025, "设备 ID 不合法");
			put(9001026, "页面 ID 不合法");
			put(9001027, "页面参数不合法");
			put(9001028, "一次删除页面 ID 数量不能超过 10");
			put(9001029, "页面已应用在设备中，请先解除应用关系再删除");
			put(9001030, "一次查询页面 ID 数量不能超过 50");
			put(9001031, "时间区间不合法");
			put(9001032, "保存设备与页面的绑定关系参数错误");
			put(9001033, "门店 ID 不合法");
			put(9001034, "设备备注信息过长");
			put(9001035, "设备申请参数不合法");
			put(9001036, "查询起始值 begin 不合法");
		}
	};

	private int errcode;

	private String errmsg;

	public ErrorResp(int errcode) {
		this.errcode = errcode;
		this.errmsg = errors.getOrDefault(errcode, "未知错误");
	}

	@Nonnull
	public static ErrorResp error(int errcode) {
		return new ErrorResp(errcode);
	}

}
