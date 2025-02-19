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
package com.apzda.cloud.queqiao.wx.interceptor;

import com.apzda.cloud.queqiao.wx.WxClientMpProperties;
import com.apzda.cloud.queqiao.wx.WxConst;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class UpstreamInterceptor implements HttpRequestInterceptor {

	private final String id;

	private final WxClientMpProperties.MpConfig config;

	private final WxClientMpProperties properties;

	public UpstreamInterceptor(String id, WxClientMpProperties.MpConfig config, WxClientMpProperties properties) {
		this.id = id;
		this.config = config;
		this.properties = properties;
	}

	@Override
	public void process(HttpRequest httpRequest, HttpContext httpContext) {
		if (httpRequest instanceof HttpRequestWrapper wrapper) {
			var uri = wrapper.getRequestLine().getUri();
			var realHost = "";
			if (uri.startsWith(WxConst.WX_MP_PREFIX)) {
				uri = uri.substring(WxConst.WX_MP_PREFIX.length());
				realHost = "mpHost";
			}
			else if (uri.startsWith(WxConst.WX_API_PREFIX)) {
				uri = uri.substring(WxConst.WX_API_PREFIX.length());
				realHost = "apiHost";
			}
			else if (uri.startsWith(WxConst.WX_OPEN_PREFIX)) {
				uri = uri.substring(WxConst.WX_OPEN_PREFIX.length());
				realHost = "openHost";
			}
			else {
				log.warn("invalid uri: {}", uri);
				return;
			}
			try {
				wrapper.setURI(new URI(uri));
				wrapper.addHeader(WxConst.WX_REAL_HOST_HEADER, realHost);

				val upstreamHeader = properties.getUpstreamHeader();
				wrapper.addHeader(upstreamHeader, id);
			}
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
