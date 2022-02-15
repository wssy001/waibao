/**
 * Copyright 2015-现在 广州市领课网络科技有限公司
 */
package com.waibao.util.base;

import com.waibao.util.enums.*;
import com.waibao.util.tools.JSONUtil;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 控制基础类，所以controller都应该继承这个类
 * 
 * @author wujing
 */
public class BaseController  {

	public static final String TEXT_UTF8 = "text/html;charset=UTF-8";
	public static final String JSON_UTF8 = "application/json;charset=UTF-8";
	public static final String XML_UTF8 = "application/xml;charset=UTF-8";

	public static final int OK = 200;
	public static final int ER = 300;
	public static final int TO = 301;
	public static final boolean CLOSE = true;
	public static final boolean OPEN = false;

	@ModelAttribute
	public void enums(ModelMap modelMap) {
		modelMap.put("isPayEnums", IsPayEnum.values());
		modelMap.put("isPutawayEnums", IsPutawayEnum.values());
		modelMap.put("sexEnums", SexEnum.values());
	}

	/**
	 * 重定向
	 * 
	 * @param format
	 * @param arguments
	 * @return
	 */
	public static String redirect(String format, Object... arguments) {
		return new StringBuffer("redirect:").append(MessageFormat.format(format, arguments)).toString();
	}

	public static String getString(HttpServletRequest request) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(request.getInputStream());
			byte[] buf = new byte[request.getContentLength()];
			in.readFully(buf);
			return new String(buf, "UTF-8");
		} catch (Exception e) {
			return "";
		} finally {
			if (null != in) {
				try {
					in.close();// 关闭数据流
				} catch (IOException e) {
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static TreeMap<String, Object> getParamMap(HttpServletRequest request) {
		TreeMap<String, Object> paramMap = new TreeMap<>();
		Map<String, String[]> map = request.getParameterMap();
		for (String key : map.keySet()) {
			paramMap.put(key, map.get(key)[0]);
		}
		if (paramMap.isEmpty()) {
			return new TreeMap<>(JSONUtil.parseObject(getString(request), TreeMap.class));
		}
		return paramMap;
	}

	/**
	 * 成功提示，关闭当前对话框，并对tabid进行刷新
	 *
	 *            需要刷新的tabid或者dialogid
	 * 
	 * @return json字符串
	 */
	protected static String success(String targetId) {
		return bjui(OK, CLOSE, targetId, "操作成功");
	}

	/**
	 * 成功提示，关闭当前对话框，并对tabid进行刷新
	 *
	 *            需要刷新的tabid或者dialogid
	 * @param message
	 *            提示信息
	 * 
	 * @return json字符串
	 */
	protected static String success(String targetId, String message) {
		return bjui(OK, CLOSE, targetId, message);
	}

	/**
	 * 删除成功提示，不关闭当前对话框
	 *
	 *            需要刷新的tabid或者dialogid
	 * @return
	 */
	protected static String delete(String targetId) {
		return delete(targetId, "操作成功");
	}

	/**
	 * 删除成功提示，不关闭当前对话框
	 *
	 *            需要刷新的tabid或者dialogid
	 * @param message
	 *            提示信息
	 * @return
	 */
	protected static String delete(String targetId, String message) {
		return bjui(OK, OPEN, targetId, message);
	}

	/**
	 * 信息提示，不关闭当前对话框
	 *
	 *            需要刷新的tabid或者dialogid
	 * @param message
	 *            提示信息
	 * @return
	 */
	protected static String ties(String message) {
		return bjui(OK, OPEN, "", message);
	}

	/**
	 * 错误提示，不关闭当前对话框，自定义提示信息
	 * 
	 * @param message
	 *            提示信息
	 * @return
	 */
	protected static String error(String message) {
		return bjui(ER, OPEN, "", message);
	}

	/**
	 * 错误提示，不关闭当前对话框，自定义提示信息
	 *
	 *            提示信息
	 * @return
	 */
	protected static String error(BindingResult bindingResult) {
		StringBuilder sb = new StringBuilder();
		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			sb.append(fieldError.getDefaultMessage()).append("<br/>");
		}
		return error(sb.toString());
	}

	/**
	 */
	protected static String redirectForBJUI(String forward) {
		Bjui bj = new Bjui();
		bj.setStatusCode(OK);
		bj.setForward(forward);
		return JSONUtil.toJSONString(bj);
	}

	/**
	 */
	private static String bjui(int statusCode, Boolean closeCurrent, String targetId, String message) {
		Bjui bj = new Bjui();
		bj.setStatusCode(statusCode);
		bj.setCloseCurrent(closeCurrent);
		bj.setTabid(targetId);
		bj.setMessage(message);
		return JSONUtil.toJSONString(bj);
	}

}
