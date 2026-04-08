package com.leapwork.leapwork_integration_for_bamboo.impl;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.util.TextProviderUtils;
import org.jetbrains.annotations.NotNull; //import com.atlassian.util.concurrent.NotNull;
import java.util.ArrayList;
import java.util.Map;

public class LeapworkBambooBridgeTaskConfigurator extends AbstractTaskConfigurator {

    private TextProviderUtils textProvider;

    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @NotNull final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put("leapworkHostname", params.getString("leapworkHostname"));
        config.put("leapworkHttps", Boolean.toString(params.getBoolean("leapworkHttps")));
        config.put("leapworkPort", params.getString("leapworkPort"));
        config.put("leapworkAccessKey", params.getString("leapworkAccessKey"));
        config.put("leapworkDelay", params.getString("leapworkDelay"));
        config.put("leapworkDoneStatusAs", params.getString("leapworkDoneStatusAs"));
        config.put("leapworkReport", params.getString("leapworkReport"));
        config.put("leapworkAutoReport", Boolean.toString(params.getBoolean("leapworkAutoReport")));
        config.put("leapworkSchNames", params.getString("leapworkSchNames"));
        config.put("leapworkSchIds", params.getString("leapworkSchIds"));
        config.put("leapworkScheduleVariables", params.getString("leapworkScheduleVariables"));
        config.put("leapworkWritePassedFlowKeyFrames", params.getString("leapworkWritePassedFlowKeyFrames"));

        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);

        context.put("leapworkPort", "9001");
        context.put("leapworkDelay", "3");
        context.put("leapworkReport","report.xml");
        context.put("leapworkAutoReport", Boolean.toString(false));


        ArrayList<String> statuses = new ArrayList<String>();
        statuses.add("Failed");
        statuses.add("Success");
        context.put("statuses",statuses);

    }
    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);

        Map<String, String> config = taskDefinition.getConfiguration();
        context.put("leapworkHostname", config.get("leapworkHostname"));
        context.put("leapworkHttps", Boolean.valueOf(config.get("leapworkHttps")));
        context.put("leapworkPort",config.get("leapworkPort"));
        context.put("leapworkAccessKey", config.get("leapworkAccessKey"));
        context.put("leapworkDelay", config.get("leapworkDelay"));
        context.put("leapworkDoneStatusAs", config.get("leapworkDoneStatusAs"));
        context.put("leapworkReport", config.get("leapworkReport"));
        context.put("leapworkAutoReport", Boolean.valueOf(config.get("leapworkAutoReport")));
        context.put("leapworkSchNames", config.get("leapworkSchNames"));
        context.put("leapworkSchIds", config.get("leapworkSchIds")); // for debug only!
        context.put("leapworkScheduleVariables", config.get("leapworkScheduleVariables"));
        context.put("leapworkWritePassedFlowKeyFrames", config.get("leapworkWritePassedFlowKeyFrames"));

        ArrayList<String> statuses = new ArrayList<String>();
        statuses.add("Failed");
        statuses.add("Success");
        context.put("statuses",statuses);

    }
}
