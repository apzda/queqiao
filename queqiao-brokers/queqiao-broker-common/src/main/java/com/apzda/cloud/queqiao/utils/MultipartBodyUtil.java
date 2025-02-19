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
package com.apzda.cloud.queqiao.utils;

import cn.hutool.core.net.URLEncodeUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Part;
import lombok.val;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class MultipartBodyUtil {

	@Nonnull
	public static MultipartBodyBuilder fromMap(@Nonnull MultiValueMap<String, ?> multiValueMap) {
		val builder = new MultipartBodyBuilder();

		for (val name : multiValueMap.keySet()) {
			val parts = multiValueMap.get(name);
			if (CollectionUtils.isEmpty(parts)) {
				continue;
			}
			for (val value : parts) {
				if (value instanceof Part part) {
					val contentType = part.getContentType();
					MediaType mediaType = null;
					if (contentType != null) {
						try {
							mediaType = MediaType.parseMediaType(contentType);
						}
						catch (Exception ignored) {
						}
					}
					builder.part(name, transform(part), mediaType);
				}
				else if (value instanceof File file) {
					transform(name, file, builder);
				}
				else if (value != null) {
					builder.part(name, value);
				}
			}
		}
		return builder;
	}

	public static void merge(@Nonnull MultiValueMap<String, ?> src,
			@Nonnull MultiValueMap<String, HttpEntity<?>> dest) {
		dest.putAll(fromMap(src).build());
	}

	@Nonnull
	static AsyncPart transform(@Nonnull Part part) {
		val httpHeaders = new HttpHeaders();
		val headerNames = part.getHeaderNames();
		if (!CollectionUtils.isEmpty(headerNames)) {
			headerNames.forEach(headerName -> httpHeaders.add(headerName, part.getHeader(headerName)));
		}
		return new AsyncPart(part.getName(), part, httpHeaders);
	}

	record AsyncPart(String name, Part part,
			HttpHeaders headers) implements org.springframework.http.codec.multipart.Part {
		@Override
		@Nonnull
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readInputStream(part::getInputStream, DefaultDataBufferFactory.sharedInstance, 1024);
		}
	}

	private static void transform(String name, @Nonnull File file, @Nonnull MultipartBodyBuilder builder) {
		val contentType = URLConnection.guessContentTypeFromName(file.getName());
		val header = String.format("form-data; name=\"%s\"; filename=\"%s\"", HtmlUtils.htmlEscape(name),
				URLEncodeUtil.encode(file.getName()));
		val fileInputStream = DataBufferUtils.readInputStream(() -> new FileInputStream(file),
				DefaultDataBufferFactory.sharedInstance, 1024);

		val headers = new HttpHeaders();
		headers.add("Content-Disposition", header);

		val part = new FilePart(name, headers, fileInputStream);

		builder.part(name, part, contentType == null ? null : MediaType.valueOf(contentType));
	}

	record FilePart(String name, HttpHeaders headers,
			Flux<DataBuffer> content) implements org.springframework.http.codec.multipart.Part {
	}

}
