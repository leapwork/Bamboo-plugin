package com.leapwork.leapwork_integration_for_bamboo.model;

import java.util.ArrayList;

public class RunCollection
{
    private int totalTests = 0;

    private int passedTests = 0;

    private int failedTests = 0;

    private int errors = 0;

    private int disabled = 0;

    private double totalTime = 0;
    public ArrayList<LeapworkRun> leapworkRuns;


    public RunCollection()
    {
        leapworkRuns = new ArrayList<LeapworkRun>();

        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        errors = 0;
        disabled = 0;
        totalTime = 0;
    }

    public int    getTotalTests()               { return  totalTests; }
    public void   setTotalTests(int total)      { totalTests = total; }

    public int    getPassedTests()              { return passedTests; }
    public void   setPassedTests(int tests)     { passedTests = tests; }
    public void   addPassedTests (int tests)    { passedTests += tests; }

    public int    getFailedTests()              { return failedTests; }
    public void   setFailedTests(int failures)  { failedTests = failures; }
    public void   addFailedTests (int failures) { failedTests += failures; }

    public int    getErrors()                   { return  errors; }
    public void   setErrors(int errors)         { this.errors = errors; }
    public void   addErrors (int errors)        { this.errors += errors; }

    public int    getDisabled()                 { return disabled; }
    public void   setDisabled(int disabled)     { this.disabled = disabled; }

    public double getTotalTime()                { return totalTime; }
    public void   setTotalTime(double time)     { totalTime = time; }
    public void   addTotalTime (double time)    { totalTime += time; }
}
