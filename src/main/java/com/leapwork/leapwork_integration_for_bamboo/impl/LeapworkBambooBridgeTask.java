package com.leapwork.leapwork_integration_for_bamboo.impl;

import com.leapwork.leapwork_integration_for_bamboo.model.*;
import com.atlassian.annotations.PublicApi;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.build.test.junit.JunitTestReportCollector;
import com.atlassian.bamboo.task.*;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.ning.http.client.AsyncHttpClient;
import com.sun.javafx.scene.layout.region.Margins;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.aspectj.bridge.AbortException;

@Scanned
public class LeapworkBambooBridgeTask implements TaskType {
	TaskResult result;

	private static PluginHandler pluginHandler = PluginHandler.getInstance();

	@PublicApi
	private final TestCollationService testCollationService;

	@Autowired
    public LeapworkBambooBridgeTask(@ComponentImport TestCollationService testCollationService)
    {
        this.testCollationService = testCollationService;
    }

	@Override
	public TaskResult execute(final TaskContext taskContext) throws TaskException {
		final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);

		final BuildLogger buildLogger = taskContext.getBuildLogger();

		// get fields value
		final String leapworkHostname = taskContext.getConfigurationMap().get("leapworkHostname");
		final String leapworkPort = taskContext.getConfigurationMap().get("leapworkPort");
		final String leapworkAccessKey = taskContext.getConfigurationMap().get("leapworkAccessKey");
		final String leapworkDelay = taskContext.getConfigurationMap().get("leapworkDelay");
		final String leapworkDoneStatusAs = taskContext.getConfigurationMap().get("leapworkDoneStatusAs");
		final String leapworkReport = pluginHandler
				.getCorrectName(taskContext.getConfigurationMap().get("leapworkReport"));
		final String leapworkSchIds = taskContext.getConfigurationMap().get("leapworkSchIds");
		final String leapworkSchNames = taskContext.getConfigurationMap().get("leapworkSchNames");
		final String leapworkAutoReport = taskContext.getConfigurationMap().get("leapworkAutoReport");
		final String leapworkScheduleVariables = taskContext.getConfigurationMap().get("leapworkScheduleVariables");
		final boolean leapworkWritePassedFlowKeyFrames = Boolean
				.parseBoolean(taskContext.getConfigurationMap().get("leapworkWritePassedFlowKeyFrames"));

		buildLogger.addBuildLogEntry(String.format(Messages.AUTO_REPORT_ENABLED, leapworkAutoReport));

		ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
		final LinkedHashMap<String, Integer> repeatedNameMapCounter = new LinkedHashMap<>();

		ArrayList<String> rawScheduleList = pluginHandler.getRawScheduleList(leapworkSchIds, leapworkSchNames);
		printPluginInputs(leapworkHostname, leapworkPort, leapworkDelay, leapworkDoneStatusAs, leapworkReport,
				leapworkSchNames, leapworkSchIds, leapworkScheduleVariables,
				String.valueOf(leapworkWritePassedFlowKeyFrames), buildLogger);

		String controllerApiHttpAddress = pluginHandler.getControllerApiHttpAdderess(leapworkHostname, leapworkPort,
				buildLogger);

		int timeDelay = pluginHandler.getTimeDelay(leapworkDelay, buildLogger);
		boolean isAutoReportEnabled = Utils.defaultBooleanIfNull(leapworkAutoReport, false);
		boolean isDoneStatusAsSuccess = pluginHandler.isDoneStatusAsSuccess(leapworkDoneStatusAs);
		boolean writePassedKeyframes = pluginHandler
				.isLeapworkWritePassedFlowKeyFrames(leapworkWritePassedFlowKeyFrames);

		String scheduleVariablesRequestPart = pluginHandler.getScheduleVariablesRequestPart(leapworkScheduleVariables,
				buildLogger);

		AsyncHttpClient mainClient = new AsyncHttpClient();
		try {
			// Get schedule titles (or/and ids in case of pipeline)
			LinkedHashMap<UUID, String> schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(mainClient,
					leapworkAccessKey, controllerApiHttpAddress, rawScheduleList, buildLogger, invalidSchedules);
			rawScheduleList.clear();// don't need that anymore

			if (schedulesIdTitleHashMap.isEmpty()) {
				throw new Exception(Messages.NO_SCHEDULES);
			}

			List<UUID> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());
			LinkedHashMap<UUID, LeapworkRun> resultsMap = new LinkedHashMap<>();

			ListIterator<UUID> iter = schIdsList.listIterator();
			while (iter.hasNext()) {

				UUID schId = iter.next();
				String schTitle = schedulesIdTitleHashMap.get(schId);
				LeapworkRun run = new LeapworkRun(schId.toString(), schTitle);

				UUID runId = pluginHandler.runSchedule(mainClient, controllerApiHttpAddress, leapworkAccessKey, schId,
						schTitle, buildLogger, run, scheduleVariablesRequestPart);
				if (runId != null) {
					resultsMap.put(runId, run);
					CollectScheduleRunResults(controllerApiHttpAddress, leapworkAccessKey, runId, schTitle, timeDelay,
							isDoneStatusAsSuccess, writePassedKeyframes, run, buildLogger, repeatedNameMapCounter);
				} else
					resultsMap.put(UUID.randomUUID(), run);

				iter.remove();
			}

			schIdsList.clear();
			schedulesIdTitleHashMap.clear();
			RunCollection buildResult = new RunCollection();

			if (invalidSchedules.size() > 0) {
				buildLogger.addBuildLogEntry(Messages.INVALID_SCHEDULES);

				for (InvalidSchedule invalidSchedule : invalidSchedules) {
					buildLogger.addBuildLogEntry(
							String.format("%1$s: %2$s", invalidSchedule.getName(), invalidSchedule.getStackTrace()));
					LeapworkRun notFoundSchedule = new LeapworkRun(invalidSchedule.getName());
					RunItem invalidRunItem = new RunItem("Error", "Error", 0, invalidSchedule.getStackTrace(),
							invalidSchedule.getName());
					notFoundSchedule.runItems.add(invalidRunItem);
					notFoundSchedule.setError(invalidSchedule.getStackTrace());
					buildResult.leapworkRuns.add(notFoundSchedule);
				}

			}

			List<LeapworkRun> resultRuns = new ArrayList<>(resultsMap.values());
			buildLogger.addBuildLogEntry(Messages.TOTAL_SEPARATOR);
			for (LeapworkRun run : resultRuns) {
				buildResult.leapworkRuns.add(run);

				buildResult.addFailedTests(run.getFailed());
				buildResult.addPassedTests(run.getPassed());
				buildResult.addErrors(run.getErrors());
				run.setTotal(run.getPassed() + run.getFailed());
				buildResult.addTotalTime(run.getTime());
				buildLogger.addBuildLogEntry(
						String.format(Messages.SCHEDULE_TITLE, run.getScheduleTitle(), run.getScheduleId()));
				buildLogger.addBuildLogEntry(String.format(Messages.CASES_PASSED, run.getPassed()));
				buildLogger.addBuildLogEntry(String.format(Messages.CASES_FAILED, run.getFailed()));
				buildLogger.addBuildLogEntry(String.format(Messages.CASES_ERRORED, run.getErrors()));
			}
			buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

			buildLogger.addBuildLogEntry(Messages.TOTAL_SEPARATOR);
			buildLogger.addBuildLogEntry(String.format(Messages.TOTAL_CASES_PASSED, buildResult.getPassedTests()));
			buildLogger.addBuildLogEntry(String.format(Messages.TOTAL_CASES_FAILED, buildResult.getFailedTests()));
			buildLogger.addBuildLogEntry(String.format(Messages.TOTAL_CASES_ERROR, buildResult.getErrors()));

			File reportFile = pluginHandler.createJUnitReport(taskContext, leapworkReport, buildLogger, buildResult);

			if (isAutoReportEnabled) {
				testCollationService.collateTestResults(taskContext, leapworkReport, new JunitTestReportCollector(),
						true);
				result = taskResultBuilder.checkTestFailures().build();
			} else {
				if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0 || invalidSchedules.size() > 0) {
					if (buildResult.getErrors() > 0)
						buildLogger.addBuildLogEntry(Messages.ERROR_NOTIFICATION);

					result = taskResultBuilder.failed().build();
					buildLogger.addBuildLogEntry("FAILURE");
				} else {
					result = taskResultBuilder.success().build();
					buildLogger.addBuildLogEntry("SUCCESS");
				}
			}

			buildLogger.addBuildLogEntry(Messages.PLUGIN_SUCCESSFUL_FINISH);

		} catch (AbortException | InterruptedException e) {
			buildLogger.addErrorLogEntry("ABORTED");
			buildLogger.addErrorLogEntry(Messages.PLUGIN_ERROR_FINISH);
			result = taskResultBuilder.failedWithError().build();
		} catch (Exception e) {
			buildLogger.addErrorLogEntry(Messages.PLUGIN_ERROR_FINISH);
			buildLogger.addErrorLogEntry(e.getMessage());
			buildLogger.addErrorLogEntry(Messages.PLEASE_CONTACT_SUPPORT);
			buildLogger.addErrorLogEntry("FAILURE");
			result = taskResultBuilder.failedWithError().build();
		} finally {
			mainClient.close();
			return result;
		}
	}

	private static void CollectScheduleRunResults(String controllerApiHttpAddress, String accessKey, UUID runId,
			String scheduleName, int timeDelay, boolean isDoneStatusAsSuccess, boolean writePassedKeyframes,
			LeapworkRun resultRun, final BuildLogger buildLogger, LinkedHashMap<String, Integer> repeatedNameMapCounter)
			throws InterruptedException {
		List<UUID> runItemsId = new ArrayList<>();
		Object waiter = new Object();
		// get statuses
		AsyncHttpClient client = new AsyncHttpClient();
		try {
			boolean isStillRunning = true;

			do {
				synchronized (waiter) // ---------
				{
					waiter.wait(timeDelay * 1000);// Time delay
				}

				List<UUID> executedRunItems = pluginHandler.getRunRunItems(client, controllerApiHttpAddress, accessKey,
						runId);
				executedRunItems.removeAll(runItemsId); // left only new

				for (ListIterator<UUID> iter = executedRunItems.listIterator(); iter.hasNext();) {
					UUID runItemId = iter.next();
					RunItem runItem = pluginHandler.getRunItem(client, controllerApiHttpAddress, accessKey, runItemId,
							scheduleName, isDoneStatusAsSuccess, writePassedKeyframes, buildLogger);

					String status = runItem.getCaseStatus();

					resultRun.addTime(runItem.getElapsedTime());
					switch (status) {
					case "NoStatus":
					case "Initializing":
					case "Connecting":
					case "Connected":
					case "Running":
					case "IsProcessing":
						iter.remove();
						break;
					case "Passed":

						runItem.setCaseName(
								pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
						resultRun.incPassed();
						resultRun.runItems.add(runItem);
						resultRun.incTotal();
						break;
					case "Failed":

						runItem.setCaseName(
								pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
						resultRun.incFailed();
						resultRun.runItems.add(runItem);
						resultRun.incTotal();
						break;
					case "Error":
					case "Inconclusive":
					case "Timeout":
					case "Cancelled":
						runItem.setCaseName(
								pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
						resultRun.incErrors();
						resultRun.runItems.add(runItem);
						resultRun.incTotal();
						break;
					case "Done":
						runItem.setCaseName(
								pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
						resultRun.runItems.add(runItem);
						if (isDoneStatusAsSuccess)
							resultRun.incPassed();
						else
							resultRun.incFailed();
						resultRun.incTotal();
						break;

					}

				}

				runItemsId.addAll(executedRunItems);

				String runStatus = pluginHandler.getRunStatus(client, controllerApiHttpAddress, accessKey, runId);
				if (runStatus.contentEquals("Finished")) {
					List<UUID> allExecutedRunItems = pluginHandler.getRunRunItems(client, controllerApiHttpAddress,
							accessKey, runId);
					if (allExecutedRunItems.size() > 0 && allExecutedRunItems.size() <= runItemsId.size())// todo ==
						isStillRunning = false;
				}

				if (isStillRunning)
					buildLogger.addBuildLogEntry(
							String.format("The schedule status is already '%1$s' - wait a minute...", runStatus));
			} while (isStillRunning);

		} catch (InterruptedException e) {
			Lock lock = new ReentrantLock();
			lock.lock();
			try {
				String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
				buildLogger.addErrorLogEntry(interruptedExceptionMessage);
				RunItem invalidItem = new RunItem("Aborted run", "Cancelled", 0, e.getMessage(), scheduleName);
				pluginHandler.stopRun(controllerApiHttpAddress, runId, scheduleName, accessKey, buildLogger);
				resultRun.incErrors();
				resultRun.runItems.add(invalidItem);
			} finally {
				lock.unlock();
				throw e;
			}
		} catch (Exception e) {
			buildLogger.addErrorLogEntry(e.getMessage());
			RunItem invalidItem = new RunItem("Invalid run", "Error", 0, e.getMessage(), scheduleName);
			resultRun.incErrors();
			resultRun.runItems.add(invalidItem);
		} finally {
			client.close();
		}
	}

	private void printPluginInputs(String leapworkHostname, String leapworkPort, String leapworkDelay,
			String leapworkDoneStatusAs, String leapworkReport, String leapworkSchNames, String leapworkSchIds,
			String leapworkScheduleVariables, String leapworkWritePassedFlowKeyFrames, final BuildLogger buildLogger) {
		String[] Names = leapworkSchNames.split(PluginHandler.scheduleSeparatorRegex);
		String[] Ids = leapworkSchIds.split(PluginHandler.scheduleSeparatorRegex);

		buildLogger.addBuildLogEntry(Messages.INPUT_VALUES_MESSAGE);
		buildLogger.addBuildLogEntry(Messages.CASE_CONSOLE_LOG_SEPARATOR);
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_HOSTNAME_VALUE, leapworkHostname));
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_PORT_VALUE, leapworkPort));
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_REPORT_VALUE, leapworkReport));
		buildLogger.addBuildLogEntry(Messages.INPUT_SCHEDULE_NAMES_VALUE);
		buildLogger.addBuildLogEntry(Arrays.toString(Names));
		if (!leapworkSchIds.trim().isEmpty()) {
			buildLogger.addBuildLogEntry(Messages.INPUT_SCHEDULE_IDS_VALUE);
			buildLogger.addBuildLogEntry(Arrays.toString(Ids));
		}
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_DELAY_VALUE, leapworkDelay));
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_DONE_VALUE, leapworkDoneStatusAs));
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_WRITE_PASSED, leapworkWritePassedFlowKeyFrames));
		buildLogger.addBuildLogEntry(String.format(Messages.INPUT_VARIABLES, leapworkScheduleVariables));

	}

}
