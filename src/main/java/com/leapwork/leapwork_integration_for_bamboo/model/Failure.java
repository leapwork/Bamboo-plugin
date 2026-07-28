package com.leapwork.leapwork_integration_for_bamboo.model;

public class Failure
{
    private String message;

    private String type;

    public Failure() { }
    public Failure(String stacktrace)
    {
        message = stacktrace;
        type = "Stack trace";
    }

    public String getMessage()              { return message; }
    public void   setMessage(String message){ this.message = message; }

    public String getType()                 { return type; }
    public void   setType(String type)      { this.type = type;}

}
