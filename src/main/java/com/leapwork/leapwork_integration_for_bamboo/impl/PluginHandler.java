package com.leapwork.leapwork_integration_for_bamboo.impl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.utils.SystemProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leapwork.leapwork_integration_for_bamboo.model.Failure;
import com.leapwork.leapwork_integration_for_bamboo.model.InvalidSchedule;
import com.leapwork.leapwork_integration_for_bamboo.model.LeapworkRun;
import com.leapwork.leapwork_integration_for_bamboo.model.RunCollection;
import com.leapwork.leapwork_integration_for_bamboo.model.RunItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PluginHandler {
	private static PluginHandler pluginHandler = null;

	public static final String scheduleSeparatorRegex = "\r\n|\n|\\s+,\\s+|,\\s+|\\s+,|,";
	private static final String variableSeparatorRegex = "\\s+:\\s+|:\\s+|\\s+:|:";
	private static final String pathSeparatorRegex = "\\\\|\\/|\\|";
	private static final String STRING_EMPTY = "";

	private PluginHandler() {
	}

	public static PluginHandler getInstance() {
		if (pluginHandler == null) {
			pluginHandler = new PluginHandler();
		}

		return pluginHandler;
	}

	public String getScheduleVariablesRequestPart(String rawScheduleVariables, final BuildLogger buildLogger) {
		if (Utils.isBlank(rawScheduleVariables)) {
			return STRING_EMPTY;
		}

		LinkedHashMap<String, String> variables = new LinkedHashMap<>();
		String[] rawSplittedKeyValuePairs = rawScheduleVariables.split(scheduleSeparatorRegex);
		for (String rawKeyValuePair : rawSplittedKeyValuePairs) {
			String[] splittedKeyAndValue = rawKeyValuePair.split(variableSeparatorRegex);
			if (splittedKeyAndValue.length < 2) {
				buildLogger.addBuildLogEntry(String.format(Messages.INVALID_SCHEDULE_VARIABLE, rawKeyValuePair));
				continue;
			}
			String key = splittedKeyAndValue[0];
			String value = splittedKeyAndValue[1];
			if (Utils.isBlank(key) || Utils.isBlank(value)) {
				buildLogger.addBuildLogEntry(String.format(Messages.INVALID_SCHEDULE_VARIABLE, rawKeyValuePair));
				continue;
			}
			if (!Utils.tryAddToMap(variables, key, value)) {
				buildLogger.addBuildLogEntry(String.format(Messages.SCHEDULE_VARIABLE_KEY_DUPLICATE, rawKeyValuePair));
			}
		}
		if (variables.isEmpty()) {
			return STRING_EMPTY;
		}

		String prefix = "?";
		StringBuilder stringBuilder = new StringBuilder();
		for (Map.Entry<String, String> variable : variables.entrySet()) {
			stringBuilder.append(prefix).append(variable.getKey()).append("=").append(variable.getValue());
			prefix = "&";
		}

		String variableRequestPart = stringBuilder.toString();
		buildLogger.addBuildLogEntry(String.format(Messages.SCHEDULE_VARIABLE_REQUEST_PART, variableRequestPart));
		return variableRequestPart;
	}

	public ArrayList<String> getRawScheduleList(String rawScheduleIds, String rawScheduleTitles) {
		ArrayList<String> rawScheduleList = new ArrayList<>();

		String[] schidsArray = rawScheduleIds.split(scheduleSeparatorRegex);
		String[] testsArray = rawScheduleTitles.split(scheduleSeparatorRegex);

		rawScheduleList.addAll(Arrays.asList(schidsArray));
		rawScheduleList.addAll(Arrays.asList(testsArray));
		rawScheduleList.removeIf(sch -> sch.trim().length() == 0);

		return rawScheduleList;
	}

	public int getTimeDelay(String rawTimeDelay, final BuildLogger buildLogger) {
		int defaultTimeDelay = 3;
		try {
			if (!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay)) {
				return Integer.parseInt(rawTimeDelay);
			}

			buildLogger.addBuildLogEntry(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID, defaultTimeDelay));
			return defaultTimeDelay;
		} catch (Exception e) {
			buildLogger.addErrorLogEntry(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID, defaultTimeDelay));
			return defaultTimeDelay;
		}
	}

	public boolean isDoneStatusAsSuccess(String doneStatusAs) {
		return doneStatusAs.contentEquals("Success");
	}

	public String getControllerApiHttpAdderess(String hostname, boolean isHttpsEnabled, String rawPort,
			final BuildLogger buildLogger) {
		StringBuilder stringBuilder = new StringBuilder();
		int port = getPortNumber(rawPort, isHttpsEnabled, buildLogger);
		if (isHttpsEnabled) {
			stringBuilder.append("https://").append(hostname).append(":").append(port);
		} else {
			stringBuilder.append("http://").append(hostname).append(":").append(port);
		}

		return stringBuilder.toString();
	}

	private int getPortNumber(String rawPortStr, boolean isHttpsEnabled, final BuildLogger buildLogger) {
		int defaultPortNumber;
		int defaultHttpPortNumber = 9001;
		int defaultHttpsPortNumber = 9002;
		try {
			if (!rawPortStr.isEmpty() || !"".equals(rawPortStr)) {
				return Integer.parseInt(rawPortStr);
			}

			defaultPortNumber = isHttpsEnabled ? defaultHttpsPortNumber : defaultHttpPortNumber;
			buildLogger.addErrorLogEntry(String.format(Messages.PORT_NUMBER_IS_INVALID, defaultPortNumber));
			return defaultPortNumber;
		} catch (Exception e) {
			defaultPortNumber = isHttpsEnabled ? defaultHttpsPortNumber : defaultHttpPortNumber;
			buildLogger.addErrorLogEntry(String.format(Messages.PORT_NUMBER_IS_INVALID, defaultPortNumber));
			return defaultPortNumber;
		}
	}

	public LinkedHashMap<UUID, String> getSchedulesIdTitleHashMap(String accessKey, String controllerApiHttpAddress,
			ArrayList<String> rawScheduleList, final BuildLogger buildLogger, ArrayList<InvalidSchedule> invalidSchedules)
			throws Exception {

		LinkedHashMap<UUID, String> schedulesIdTitleHashMap = new LinkedHashMap<>();
		ArrayList<String> tempRawScheduleList = new ArrayList<>(rawScheduleList);
		String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, controllerApiHttpAddress);
		Collections.sort(rawScheduleList);
		try {
			SimpleHttpResponse response = executeRequest("GET", scheduleListUri, accessKey, null);

			switch (response.getStatusCode()) {
			case 200:
				JsonParser parser = new JsonParser();
				JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

				for (String rawSchedule : rawScheduleList) {
					boolean successfullyMapped = false;
					for (JsonElement jsonScheduleElement : jsonScheduleList) {
						JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

						if (jsonSchedule.get("Type").getAsString().contentEquals("TemporaryScheduleInfo")) {
							continue;
						}

						UUID id = Utils.defaultUuidIfNull(jsonSchedule.get("Id"), UUID.randomUUID());
						String title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");
						boolean isEnabled = Utils.defaultBooleanIfNull(jsonSchedule.get("IsEnabled"), false);

						if (id.toString().contentEquals(rawSchedule)) {
							if (!schedulesIdTitleHashMap.containsValue(title) && tempRawScheduleList.contains(id.toString())) {
								tempRawScheduleList.remove(id.toString());
								tempRawScheduleList.remove(title);
								if (isEnabled) {
									schedulesIdTitleHashMap.put(id, title);
									buildLogger.addBuildLogEntry(
											String.format(Messages.SCHEDULE_DETECTED, title, rawSchedule));
								} else {
									invalidSchedules.add(new InvalidSchedule(rawSchedule,
											String.format(Messages.SCHEDULE_DISABLED, title, id)));
									buildLogger.addBuildLogEntry(String.format(Messages.SCHEDULE_DISABLED, title, id));
								}
							}

							successfullyMapped = true;
						}

						if (title.contentEquals(rawSchedule)) {
							if (!schedulesIdTitleHashMap.containsKey(id) && tempRawScheduleList.contains(title)) {
								tempRawScheduleList.remove(id.toString());
								tempRawScheduleList.remove(title);
								if (isEnabled) {
									schedulesIdTitleHashMap.put(id, title);
									buildLogger.addBuildLogEntry(
											String.format(Messages.SCHEDULE_DETECTED, title, rawSchedule));
								} else {
									invalidSchedules.add(new InvalidSchedule(rawSchedule,
											String.format(Messages.SCHEDULE_DISABLED, title, id)));
								}
							}

							successfullyMapped = true;
						}
					}

					if (!successfullyMapped) {
						invalidSchedules.add(new InvalidSchedule(rawSchedule, Messages.NO_SUCH_SCHEDULE));
					}
				}
				break;

			case 401:
				StringBuilder errorMessage401 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
				OnFailedToGetScheduleTitleIdMap(null, errorMessage401.toString(), buildLogger);

			case 500:
				StringBuilder errorMessage500 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage500, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
				OnFailedToGetScheduleTitleIdMap(null, errorMessage500.toString(), buildLogger);

			default:
				StringBuilder errorMessage = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				OnFailedToGetScheduleTitleIdMap(null, errorMessage.toString(), buildLogger);
			}
		} catch (ConnectException | UnknownHostException e) {
			String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
			OnFailedToGetScheduleTitleIdMap(e, connectionErrorMessage, buildLogger);
		} catch (IOException e) {
			String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
			OnFailedToGetScheduleTitleIdMap(e, ioExceptionMessage, buildLogger);
		}

		return schedulesIdTitleHashMap;
	}

	private static HashMap<UUID, String> OnFailedToGetScheduleTitleIdMap(Exception e, String errorMessage,
			final BuildLogger buildLogger) throws Exception {
		buildLogger.addErrorLogEntry(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
		if (errorMessage != null && !errorMessage.isEmpty()) {
			buildLogger.addErrorLogEntry(errorMessage);
		} else {
			errorMessage = Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT;
		}
		if (e == null) {
			e = new Exception(errorMessage);
		}
		throw e;
	}

	public UUID runSchedule(String controllerApiHttpAddress, String accessKey, UUID scheduleId, String scheduleTitle,
			final BuildLogger buildLogger, LeapworkRun run, String scheduleVariablesRequestPart) throws Exception {

		String uri = String.format(Messages.RUN_SCHEDULE_URI, controllerApiHttpAddress, scheduleId.toString(),
				scheduleVariablesRequestPart);
		try {
			SimpleHttpResponse response = executeRequest("PUT", uri, accessKey, STRING_EMPTY);

			switch (response.getStatusCode()) {
			case 200:
				String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, scheduleTitle, scheduleId);
				buildLogger.addBuildLogEntry(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
				buildLogger.addBuildLogEntry(successMessage);
				JsonParser parser = new JsonParser();
				JsonObject jsonRunObject = parser.parse(response.getResponseBody()).getAsJsonObject();
				JsonElement jsonRunId = jsonRunObject.get("RunId");
				String runIdStr = Utils.defaultStringIfNull(jsonRunId);
				return UUID.fromString(runIdStr);

			case 400:
				StringBuilder errorMessage400 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage400, Messages.INVALID_VARIABLE_KEY_NAME);
				return OnScheduleRunFailure(errorMessage400, run, scheduleId, buildLogger);

			case 401:
				StringBuilder errorMessage401 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
				return OnScheduleRunFailure(errorMessage401, run, scheduleId, buildLogger);

			case 404:
				StringBuilder errorMessage404 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage404, String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
				return OnScheduleRunFailure(errorMessage404, run, scheduleId, buildLogger);

			case 446:
				StringBuilder errorMessage446 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage446, Messages.NO_DISK_SPACE);
				return OnScheduleRunFailure(errorMessage446, run, scheduleId, buildLogger);

			case 455:
				StringBuilder errorMessage455 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage455, Messages.DATABASE_NOT_RESPONDING);
				return OnScheduleRunFailure(errorMessage455, run, scheduleId, buildLogger);

			case 500:
				StringBuilder errorMessage500 = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				appendLine(errorMessage500, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
				return OnScheduleRunFailure(errorMessage500, run, scheduleId, buildLogger);

			default:
				StringBuilder errorMessage = new StringBuilder(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				return OnScheduleRunFailure(errorMessage, run, scheduleId, buildLogger);
			}
		} catch (ConnectException | UnknownHostException e) {
			return OnScheduleRunConnectionFailure(e, buildLogger);
		}
	}

	private static UUID OnScheduleRunFailure(StringBuilder errorMessage, LeapworkRun failedRun, UUID scheduleId,
			BuildLogger buildLogger) {
		buildLogger.addErrorLogEntry(
				String.format(Messages.SCHEDULE_RUN_FAILURE, failedRun.getScheduleTitle(), scheduleId.toString()));
		buildLogger.addErrorLogEntry(errorMessage.toString());
		failedRun.setError(errorMessage.toString());
		failedRun.incErrors();
		return null;
	}

	private static UUID OnScheduleRunConnectionFailure(Exception e, BuildLogger buildLogger) {
		buildLogger.addErrorLogEntry(String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getMessage()));
		return null;
	}

	public boolean stopRun(String controllerApiHttpAddress, UUID runId, String scheduleTitle, String accessKey,
			final BuildLogger buildLogger) {
		boolean isSuccessfullyStopped = false;

		buildLogger.addErrorLogEntry(String.format(Messages.STOPPING_RUN, scheduleTitle, runId));
		String uri = String.format(Messages.STOP_RUN_URI, controllerApiHttpAddress, runId.toString());
		try {
			SimpleHttpResponse response = executeRequest("PUT", uri, accessKey, STRING_EMPTY);

			switch (response.getStatusCode()) {
			case 200:
				JsonParser parser = new JsonParser();
				JsonObject jsonStopRunObject = parser.parse(response.getResponseBody()).getAsJsonObject();
				JsonElement jsonStopSuccessfull = jsonStopRunObject.get("OperationCompleted");
				isSuccessfullyStopped = Utils.defaultBooleanIfNull(jsonStopSuccessfull, false);
				if (isSuccessfullyStopped) {
					buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_SUCCESS, scheduleTitle, runId.toString()));
				} else {
					buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));
				}
				break;

			case 401:
				buildLogger.addErrorLogEntry(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				buildLogger.addErrorLogEntry(Messages.INVALID_ACCESS_KEY);
				buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));

			case 404:
				buildLogger.addErrorLogEntry(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				buildLogger.addErrorLogEntry(String.format(Messages.NO_SUCH_RUN_WAS_FOUND, runId, scheduleTitle));
				buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));

			case 446:
				buildLogger.addErrorLogEntry(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				buildLogger.addErrorLogEntry(Messages.NO_DISK_SPACE);
				buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));

			case 455:
				buildLogger.addErrorLogEntry(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				buildLogger.addErrorLogEntry(Messages.DATABASE_NOT_RESPONDING);
				buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));

			case 500:
				buildLogger.addErrorLogEntry(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				buildLogger.addErrorLogEntry(Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
				buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));
			default:
				buildLogger.addErrorLogEntry(
						String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
				buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));
			}
		} catch (Exception e) {
			buildLogger.addErrorLogEntry(String.format(Messages.STOP_RUN_FAIL, scheduleTitle, runId.toString()));
			buildLogger.addErrorLogEntry(e.getMessage());
		}
		return isSuccessfullyStopped;
	}

	public String getCorrectName(String reportFileName) {
		if (reportFileName.contains("/") || reportFileName.contains("\\") || reportFileName.contains("|")) {
			String[] ar = reportFileName.split(pathSeparatorRegex);
			String buff = ar[ar.length - 1];

			if (!buff.endsWith(".xml")) {
				buff += ".xml";
			}

			reportFileName = buff;
		} else {
			if (reportFileName.isEmpty() || "".equals(reportFileName)) {
				reportFileName = "report.xml";
			}

			if (!reportFileName.contains(".xml")) {
				reportFileName += ".xml";
			}
		}
		return reportFileName;
	}

	public File createJUnitReport(TaskContext taskContext, String reportFileName, final BuildLogger buildLogger,
			RunCollection buildResult) throws Exception {
		String bambooWorkspacePath = taskContext.getWorkingDirectory().getAbsolutePath();
		String junitReportFilePath = Paths.get(bambooWorkspacePath, reportFileName).toString();
		buildLogger.addBuildLogEntry(String.format(Messages.BAMBOO_WORKSPACE_VARIABLE, bambooWorkspacePath));

		try {
			File reportFile = new File(junitReportFilePath);
			if (!reportFile.exists()) {
				reportFile.createNewFile();
			}

			reportFile.setLastModified(
					reportFile.lastModified() - SystemProperty.FS_TIMESTAMP_RESOLUTION_MS.getTypedValue() - 2000);

			String xml = buildJUnitXml(buildResult);
			try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()), false, "UTF-8")) {
				out.print(xml);
			}
			return reportFile;
		} catch (FileNotFoundException e) {
			buildLogger.addErrorLogEntry(Messages.REPORT_FILE_NOT_FOUND);
			throw new Exception(e);
		} catch (IOException e) {
			buildLogger.addErrorLogEntry(Messages.REPORT_FILE_CREATION_FAILURE);
			throw new Exception(e);
		}
	}

	private String buildJUnitXml(RunCollection buildResult) {
		String lineSeparator = System.getProperty("line.separator");
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(lineSeparator);
		xml.append("<testsuites");
		appendAttribute(xml, "total", Integer.toString(buildResult.getTotalTests()));
		appendAttribute(xml, "tests", Integer.toString(buildResult.getPassedTests()));
		appendAttribute(xml, "failures", Integer.toString(buildResult.getFailedTests()));
		appendAttribute(xml, "errors", Integer.toString(buildResult.getErrors()));
		appendAttribute(xml, "disabled", Integer.toString(buildResult.getDisabled()));
		appendAttribute(xml, "time", Double.toString(buildResult.getTotalTime()));
		xml.append(">");

		for (LeapworkRun run : buildResult.leapworkRuns) {
			xml.append(lineSeparator).append("  <testsuite");
			appendAttribute(xml, "name", run.getScheduleTitle());
			appendAttribute(xml, "runId", run.getRunId() == null ? null : run.getRunId().toString());
			appendAttribute(xml, "schId", run.getScheduleId());
			appendAttribute(xml, "status", run.getStatus());
			appendAttribute(xml, "tests", Integer.toString(run.getTotal()));
			appendAttribute(xml, "passed", Integer.toString(run.getPassed()));
			appendAttribute(xml, "failures", Integer.toString(run.getFailed()));
			appendAttribute(xml, "errors", Integer.toString(run.getErrors()));
			appendAttribute(xml, "error", run.getError());
			appendAttribute(xml, "time", Double.toString(run.getTime()));
			appendAttribute(xml, "id", Integer.toString(run.getId()));
			xml.append(">");

			for (RunItem runItem : run.runItems) {
				xml.append(lineSeparator).append("    <testcase");
				appendAttribute(xml, "name", runItem.getCaseName());
				appendAttribute(xml, "status", runItem.getCaseStatus());
				appendAttribute(xml, "time", Double.toString(runItem.getElapsedTime()));
				appendAttribute(xml, "classname", runItem.getClassname());

				Failure failure = runItem.failure;
				if (failure != null && isFailureStatus(runItem.getCaseStatus(), false)) {
					xml.append(">").append(lineSeparator).append("      <failure");
					appendAttribute(xml, "message", failure.getMessage());
					appendAttribute(xml, "type", failure.getType());
					xml.append(" />").append(lineSeparator).append("    </testcase>");
				} else {
					xml.append(" />");
				}
			}

			xml.append(lineSeparator).append("  </testsuite>");
		}

		xml.append(lineSeparator).append("</testsuites>").append(lineSeparator);
		return xml.toString();
	}

	private void appendAttribute(StringBuilder stringBuilder, String name, String value) {
		if (value == null) {
			return;
		}

		stringBuilder.append(" ").append(name).append("=\"").append(escapeXml(value)).append("\"");
	}

	private String escapeXml(String value) {
		if (value == null) {
			return "";
		}

		return value.replace("&", "&amp;")
				.replace("\"", "&quot;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	public String getRunStatus(String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {
		String uri = String.format(Messages.GET_RUN_STATUS_URI, controllerApiHttpAddress, runId.toString());
		SimpleHttpResponse response = executeRequest("GET", uri, accessKey, null);

		switch (response.getStatusCode()) {
		case 200:
			JsonParser parser = new JsonParser();
			JsonObject runStatusObject = parser.parse(response.getResponseBody()).getAsJsonObject();
			JsonElement jsonRunStatus = runStatusObject.get("Status");
			return Utils.defaultStringIfNull(jsonRunStatus, "Queued");

		case 401:
			StringBuilder errorMessage401 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
			throw new Exception(errorMessage401.toString());

		case 404:
			StringBuilder errorMessage404 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage404, String.format(Messages.NO_SUCH_RUN, runId));
			throw new Exception(errorMessage404.toString());

		case 455:
			StringBuilder errorMessage455 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage455, Messages.DATABASE_NOT_RESPONDING);
			throw new Exception(errorMessage455.toString());

		case 500:
			StringBuilder errorMessage500 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage500, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
			throw new Exception(errorMessage500.toString());

		default:
			String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(),
					response.getStatusText());
			throw new Exception(errorMessage);
		}
	}

	public List<UUID> getRunRunItems(String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {
		String uri = String.format(Messages.GET_RUN_ITEMS_IDS_URI, controllerApiHttpAddress, runId.toString());
		SimpleHttpResponse response = executeRequest("GET", uri, accessKey, null);

		switch (response.getStatusCode()) {
		case 200:
			JsonParser parser = new JsonParser();
			JsonObject jsonRunItemsObject = parser.parse(response.getResponseBody()).getAsJsonObject();
			JsonElement jsonRunItemsElement = jsonRunItemsObject.get("RunItemIds");

			List<UUID> runItems = new ArrayList<>();
			if (jsonRunItemsElement != null) {
				JsonArray jsonRunItems = jsonRunItemsElement.getAsJsonArray();
				for (int i = 0; i < jsonRunItems.size(); i++) {
					runItems.add(UUID.fromString(jsonRunItems.get(i).getAsString()));
				}
			}

			return runItems;

		case 401:
			StringBuilder errorMessage401 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
			throw new Exception(errorMessage401.toString());

		case 404:
			StringBuilder errorMessage404 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage404, String.format(Messages.NO_SUCH_RUN, runId));
			throw new Exception(errorMessage404.toString());

		case 446:
			StringBuilder errorMessage446 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage446, Messages.NO_DISK_SPACE);
			throw new Exception(errorMessage446.toString());

		case 455:
			StringBuilder errorMessage455 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage455, Messages.DATABASE_NOT_RESPONDING);
			throw new Exception(errorMessage455.toString());

		case 500:
			StringBuilder errorMessage500 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage500, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
			throw new Exception(errorMessage500.toString());

		default:
			String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(),
					response.getStatusText());
			throw new Exception(errorMessage);
		}
	}

	public RunItem getRunItem(String controllerApiHttpAddress, String accessKey, UUID runItemId, String scheduleTitle,
			boolean doneStatusAsSuccess, boolean writePassedKeyframes, final BuildLogger buildLogger) throws Exception {
		String uri = String.format(Messages.GET_RUN_ITEM_URI, controllerApiHttpAddress, runItemId.toString());
		SimpleHttpResponse response = executeRequest("GET", uri, accessKey, null);

		switch (response.getStatusCode()) {
		case 200:
			JsonParser parser = new JsonParser();
			JsonObject jsonRunItem = parser.parse(response.getResponseBody()).getAsJsonObject();

			JsonObject flowInfo = jsonRunItem.get("FlowInfo").getAsJsonObject();
			String flowTitle = Utils.defaultStringIfNull(flowInfo.get("FlowTitle"));
			String flowStatus = Utils.defaultStringIfNull(flowInfo.get("Status"), "NoStatus");

			JsonObject environmentInfo = jsonRunItem.get("AgentInfo").getAsJsonObject();
			String environmentTitle = Utils.defaultStringIfNull(environmentInfo.get("AgentTitle"));

			double milliseconds = Utils.defaultDoubleIfNull(jsonRunItem.get("ElapsedMilliseconds"), 0);
			RunItem runItem = new RunItem(flowTitle, flowStatus, milliseconds, scheduleTitle);

			if (flowStatus.contentEquals("Initializing") || flowStatus.contentEquals("Connecting")
					|| flowStatus.contentEquals("Connected") || flowStatus.contentEquals("Running")
					|| flowStatus.contentEquals("IsProcessing") || flowStatus.contentEquals("NoStatus")
					|| (flowStatus.contentEquals("Passed") && !writePassedKeyframes)
					|| (flowStatus.contentEquals("Done") && doneStatusAsSuccess && !writePassedKeyframes)) {
				return runItem;
			}

			Failure keyframes = getRunItemKeyFrames(controllerApiHttpAddress, accessKey, runItemId, runItem,
					scheduleTitle, environmentTitle, buildLogger);
			runItem.failure = keyframes;
			return runItem;

		case 401:
			StringBuilder errorMessage401 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
			throw new Exception(errorMessage401.toString());

		case 404:
			StringBuilder errorMessage404 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage404, String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
			throw new Exception(errorMessage404.toString());

		case 446:
			StringBuilder errorMessage446 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage446, Messages.NO_DISK_SPACE);
			throw new Exception(errorMessage446.toString());

		case 455:
			StringBuilder errorMessage455 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage455, Messages.DATABASE_NOT_RESPONDING);
			throw new Exception(errorMessage455.toString());

		case 500:
			StringBuilder errorMessage500 = new StringBuilder(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			appendLine(errorMessage500, Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
			throw new Exception(errorMessage500.toString());

		default:
			String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(),
					response.getStatusText());
			throw new Exception(errorMessage);
		}
	}

	public Failure getRunItemKeyFrames(String controllerApiHttpAddress, String accessKey, UUID runItemId,
			RunItem runItem, String scheduleTitle, String environmentTitle, final BuildLogger buildLogger)
			throws Exception {
		String uri = String.format(Messages.GET_RUN_ITEM_KEYFRAMES_URI, controllerApiHttpAddress, runItemId.toString());
		SimpleHttpResponse response = executeRequest("GET", uri, accessKey, null);

		switch (response.getStatusCode()) {
		case 200:
			JsonArray jsonKeyframes = TryParseKeyframeJson(response.getResponseBody(), buildLogger);

			if (jsonKeyframes != null) {
				buildLogger.addBuildLogEntry(Messages.CASE_CONSOLE_LOG_SEPARATOR);
				buildLogger.addBuildLogEntry(String.format(Messages.CASE_INFORMATION, runItem.getCaseName(),
						runItem.getCaseStatus(), runItem.getElapsedTime()));
				StringBuilder fullKeyframes = new StringBuilder();

				for (JsonElement jsonKeyFrame : jsonKeyframes) {
					String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "Trace");
					if (!level.contentEquals("") && !level.contentEquals("Trace")) {
						String keyFrameTimeStamp = jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsJsonObject()
								.get("Value").getAsString();
						String keyFrameLogMessage = jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString();
						String keyFrame = String.format(Messages.CASE_STACKTRACE_FORMAT, keyFrameTimeStamp,
								keyFrameLogMessage);
						buildLogger.addBuildLogEntry(keyFrame);
						fullKeyframes.append(keyFrame).append("&#xA;");
					}
				}

				fullKeyframes.append("Environment: ").append(environmentTitle).append("&#xA;");
				fullKeyframes.append("Schedule: ").append(scheduleTitle);
				buildLogger.addBuildLogEntry("Environment: " + environmentTitle);
				buildLogger.addBuildLogEntry("Schedule: " + scheduleTitle);

				return new Failure(fullKeyframes.toString());
			}

			buildLogger.addBuildLogEntry(String.format(Messages.FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY));
			return new Failure(Messages.FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY);

		case 401:
			buildLogger.addErrorLogEntry(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			buildLogger.addErrorLogEntry(Messages.INVALID_ACCESS_KEY);
			break;
		case 404:
			buildLogger.addErrorLogEntry(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			buildLogger.addErrorLogEntry(String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
			break;
		case 446:
			buildLogger.addErrorLogEntry(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			buildLogger.addErrorLogEntry(Messages.NO_DISK_SPACE);
			break;
		case 455:
			buildLogger.addErrorLogEntry(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			buildLogger.addErrorLogEntry(Messages.DATABASE_NOT_RESPONDING);
			break;
		case 500:
			buildLogger.addErrorLogEntry(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			buildLogger.addErrorLogEntry(Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
			break;
		default:
			buildLogger.addErrorLogEntry(
					String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
			break;
		}
		return null;
	}

	private static JsonArray TryParseKeyframeJson(String response, BuildLogger buildLogger) {
		JsonParser parser = new JsonParser();
		try {
			return parser.parse(response).getAsJsonArray();
		} catch (Exception e) {
			buildLogger.addBuildLogEntry(e.getMessage());
			return null;
		}
	}

	private void appendLine(StringBuilder stringBuilder, String line) {
		if (stringBuilder != null) {
			stringBuilder.append(System.getProperty("line.separator"));
			stringBuilder.append(line);
		}
	}

	public String correctRepeatedTitles(HashMap<String, Integer> repeatedNameMapCounter, String title) {
		if (repeatedNameMapCounter.containsKey(title)) {
			Integer number = repeatedNameMapCounter.get(title);
			title = String.format("%1$s(%2$d)", title, number);
			number++;
			return title;
		}

		repeatedNameMapCounter.put(title, Integer.valueOf(0));
		return title;
	}

	public boolean isLeapworkWritePassedFlowKeyFrames(boolean leapworkWritePassedFlowKeyFrames) {
		return leapworkWritePassedFlowKeyFrames;
	}

	private boolean isFailureStatus(String status, boolean doneStatusAsSuccess) {
		return status.contentEquals("Failed") || status.contentEquals("Error") || status.contentEquals("Inconclusive")
				|| status.contentEquals("Timeout") || status.contentEquals("Cancelled")
				|| (status.contentEquals("Done") && !doneStatusAsSuccess);
	}

	private SimpleHttpResponse executeRequest(String method, String requestUrl, String accessKey, String requestBody)
			throws IOException {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(requestUrl).openConnection();
			connection.setRequestMethod(method);
			connection.setRequestProperty("AccessKey", accessKey);
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);

			if (requestBody != null) {
				byte[] requestBytes = requestBody.getBytes(StandardCharsets.UTF_8);
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				connection.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
				connection.getOutputStream().write(requestBytes);
			}

			int statusCode = connection.getResponseCode();
			String statusText = connection.getResponseMessage();
			InputStream responseStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
			String responseBody = responseStream == null ? STRING_EMPTY : readStream(responseStream);
			return new SimpleHttpResponse(statusCode, statusText, responseBody);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private String readStream(InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			StringBuilder responseBody = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				responseBody.append(line);
			}
			return responseBody.toString();
		}
	}

	static final class SimpleHttpResponse {
		private final int statusCode;
		private final String statusText;
		private final String responseBody;

		SimpleHttpResponse(int statusCode, String statusText, String responseBody) {
			this.statusCode = statusCode;
			this.statusText = statusText;
			this.responseBody = responseBody;
		}

		int getStatusCode() {
			return statusCode;
		}

		String getStatusText() {
			return statusText;
		}

		String getResponseBody() {
			return responseBody;
		}
	}
}
