package com.leapwork.leapwork_integration_for_bamboo.model;

public class InvalidSchedule
{
    private String name;
    private String stackTrace;

    public InvalidSchedule(String name, String stackTrace)
    {
        this.name = name;
        this.stackTrace = stackTrace;
    }

    public String getName() {
        return name;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
