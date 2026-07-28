package com.leapwork.leapwork_integration_for_bamboo.model;

import java.util.ArrayList;
import java.util.UUID;

public class LeapworkRun
{
    private String scheduleTitle;

    private String scheduleId;

    private String status;

    private UUID runId;

    private int total;

    private int passed ;

    private int failed ;

    private int errors ;

    private String error;

    private double time ;

    private int id;

    public ArrayList<RunItem> runItems;


    public LeapworkRun() { runItems = new ArrayList<RunItem>(); }

    public LeapworkRun(String title)
    {
        runItems = new ArrayList<RunItem>();
        scheduleTitle = title;
        failed = 0;
        passed = 0;
        errors = 0;
        time = 0;
    }
    public LeapworkRun(String schid, String title)
    {
        runItems = new ArrayList<RunItem>();
        scheduleId = schid;
        scheduleTitle = title;
        failed = 0;
        passed = 0;
        errors = 0;
    }

    public String getScheduleTitle()            { return scheduleTitle;}
    public void   setScheduleTitle(String name) { scheduleTitle = name; }

    public UUID getRunId()               { return  runId;}
    public void   setRunId(UUID id)   { runId = id; }

    public String getScheduleId()               { return  scheduleId;}
    public void   setScheduleId(String schId)   { scheduleId = schId; }

    public String getStatus()                   { return status; }
    public void   setStatus(String status)      { this.status = status; }

    public int    getTotal()                    { return total; }
    public void   setTotal(int tests)           { total = tests; }
    public void   incTotal()                    { this.total++; }

    public int    getPassed()                   { return passed; }
    public void   setPassed(int passed)         { this.passed = passed; }
    public void   incPassed()                   { passed++; }
    public void   addPassed(int passed)         { this.passed += passed; }

    public int    getFailed()                   { return failed; }
    public void   setFailed(int failures)       { failed = failures; }
    public void   incFailed()                   { failed++; }
    public void   addFailed(int failures)       { this.failed += failures; }

    public int    getErrors()                   { return errors; }
    public void   setErrors(int errors)         { this.errors = errors; }
    public void   incErrors()                   { errors++; }

    public String getError()                    { return error; }
    public void   setError(String error)
    {
        if(this.error == null)
            this.error = error.replace("\n","&#xA;");
        else
            this.error += ("\n" + error).replace("\n","&#xA;");
    }

    public double getTime()                     { return time; }
    public void   setTime(double time)          { this.time = time; }
    public void   addTime(double time)          { this.time += time; }

    public int    getId()                       { return id; }
    public void   setId(int id)                 { this.id = id; }

}
