/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.twitter.util;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringPool;

import java.text.Format;

import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Shinn Lok
 */
public class DefaultTimelineProcessor implements TimelineProcessor {

	public JSONArray getUserTimelineJSONArray(
		String twitterScreenName, long sinceId) {

		Http.Options options = new Http.Options();

		options.addHeader(HttpHeaders.USER_AGENT, getRandomUserAgent());
		options.setLocation(_URL + twitterScreenName);

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		try {
			String html = HttpUtil.URLtoString(options);

			jsonArray = doGetUserTimelineJSONArray(html, sinceId, jsonArray);
		}
		catch (Exception e) {
			_log.error(e, e);
		}

		return jsonArray;
	}

	protected JSONArray doGetUserTimelineJSONArray(
		String html, long sinceId, JSONArray jsonArray) {

		Matcher matcher = _pattern.matcher(html);

		if (!matcher.find() ||
			(sinceId >= GetterUtil.getLong(matcher.group(1)))) {

			return jsonArray;
		}

		JSONObject statusJSON = JSONFactoryUtil.createJSONObject();

		Date createDate = new Date(GetterUtil.getLong(matcher.group(3)) * 1000);

		statusJSON.put("created_at", _dateFormat.format(createDate));
		statusJSON.put("id", matcher.group(1));
		statusJSON.put("text", HtmlUtil.extractText(matcher.group(4)));

		JSONObject userJSON = JSONFactoryUtil.createJSONObject();

		userJSON.put("id", matcher.group(2));

		statusJSON.put("user", userJSON);

		jsonArray.put(statusJSON);

		StringBuffer sb = new StringBuffer();

		matcher.appendReplacement(sb, StringPool.BLANK);

		matcher.appendTail(sb);

		return doGetUserTimelineJSONArray(sb.toString(), sinceId, jsonArray);
	}

	protected String getRandomUserAgent() {
		Random random = new Random();

		String userAgent = _USER_AGENTS[random.nextInt(_USER_AGENTS.length)];

		return userAgent;
	}

	private static final String _URL = "https://www.twitter.com/";

	private static final String[] _USER_AGENTS = new String[] {
		"Mozilla/1.22 (compatible; MSIE 10.0; Windows 3.1)",
		"Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 5.2)",
		"Mozilla/5.0 (Windows NT 5.1; rv:5.0.1) Gecko/20100101 Firefox/5.0.1",
		"Mozilla/5.0 (Windows NT 5.1; rv:13.0) Gecko/20100101 Firefox/13.0.1",
		"Mozilla/5.0 (Windows NT 5.1; rv:17.0) Gecko/20100101 Firefox/17.0",
		"Opera/9.80 (X11; Linux i686; U; hu) Presto/2.9.168 Version/11.50",
		"Opera/12.80 (Windows NT 5.1; U; en) Presto/2.10.289 Version/12.02"
	};

	private static Log _log = LogFactoryUtil.getLog(
		DefaultTimelineProcessor.class);

	private static Format _dateFormat =
		FastDateFormatFactoryUtil.getSimpleDateFormat(
			"EEE MMM d hh:mm:ss Z yyyy", Locale.US);

	private static Pattern _pattern = Pattern.compile(
		"data-item-id=\"([0-9]+)\".*?data-user-id=\"([0-9]+)\".*?data-time=\"" +
		"([0-9]+)\".*?<p class=\"js-tweet-text\">(.*?)</p>", Pattern.DOTALL);

}