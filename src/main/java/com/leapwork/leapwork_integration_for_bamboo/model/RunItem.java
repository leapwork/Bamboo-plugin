package com.leapwork.leapwork_integration_for_bamboo.model;

public class RunItem
{

    private String caseName;

    private String caseStatus;

    private double elapsedTime ;

    private String classname;
    public Failure failure;
    public RunItem() { }

    public RunItem(String caseTitle, String caseStatus, double elapsed, String schedule)
    {
        caseName = caseTitle;
        this.caseStatus = caseStatus;
        elapsedTime = elapsed;
        classname = schedule;
        failure = null;
    }

    public RunItem(String caseTitle, String caseStatus, double elapsed, String stacktrace, String schedule)
    {
        caseName = caseTitle;
        this.caseStatus = caseStatus;
        elapsedTime = elapsed;
        failure = new Failure(stacktrace);
        classname = schedule;
    }

    public String getCaseName()                  { return  caseName; }
    public void   setCaseName(String name)       { caseName = name; }

    public String getCaseStatus()                { return  caseStatus;}
    public void   setCaseStatus(String status)    { caseStatus = status; }

    public double getElapsedTime()               { return elapsedTime; }
    public void   setElapsedTime(double time)    { elapsedTime = time; }

    public String getClassname()                 { return classname; }
    public void   setClassname(String classname) { this.classname = classname; }
}
