package org.osc.core.server.installer.impl;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

class TestLogService implements LogService {

	List<String> logged = new LinkedList<>();

	@Override
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
    @Override
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		String levelStr;
		switch (level) {
		case LogService.LOG_DEBUG:
			levelStr = "DEBUG";
			break;
		case LogService.LOG_INFO:
			levelStr = "INFO";
			break;
		case LogService.LOG_WARNING:
			levelStr = "WARNING";
			break;
		case LogService.LOG_ERROR:
			levelStr = "ERROR";
			break;
		default:
			levelStr = "UNKNOWN";
		}
		String formatted = String.format("%s: %s %s", levelStr, message, exception != null ? exception : "");
		this.logged.add(formatted);
	}

}
