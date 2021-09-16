# Leapwork Integration
This is LEAPWORK plugin for Bamboo

# More Details
LEAPWORK is a mighty automation testing system and now it can be used for running [smoke, functional, acceptance] tests, generating reports and a lot more in Bamboo. You can easily configure integration directly in Bamboo enjoying UI friendly configuration page with easy connection and test suites selection.

# Features:
 - Setup and test LEAPWORK connection in few clicks
 - Run automated tests in your Bamboo build tasks
 - Automatically receive test results
 - Build status based tests results
 - Generate a xml report file in JUnit format
 - Write tests trace to build output log
 - Smart UI
 
# Installing
- Use atlassian-sdk maven 8.0.7
- Command: atlas-package 
- Or simply install jar-file from the "target" folder: Manage apps -> Upload app -> Choose that jar-file -> Press Upload

# Update 3.0.0
- For LEAPWORK version 2018.2
- Removed "Access key" info from console log.
- Fixed bug when schedules are executing in non-ordered way.
- Now it is possible to insert a list of schedules to "Schedule Names" text box. List of names must be new line or comma separated.
Be noticed that by clicking on any checkbox with schedule to select, using "Select Schedules" button, all non-existing or disabled schedules will be removed from "Schedule names" text box.
- Added "Schedule variables" non-mandatory field. Schedule variables must be listed in "key : value" way and separated by new line or comma.
Be noticed that all the schedules will be run with this list of variables.
- Boolean field "leapworkWritePassedFlowKeyFrames" is not mandatory anymore.
- Uses new Leapwork v3 API, API v2 is not supported

# Instruction
1. Add Build "Leapwork Integration" to your job.
2. Enter your LEAPWORK controller hostname or IP-address something like "win10-agent20" or "localhost".
3. Enter your LEAPWORK controller API port, by default it is 9001.
4. Enter JUnit report file name. This file will be created at your job's working directory. If there is an xml file with the same name, it will be overwritten. By default it is "report.xml".
5. Enter time delay in seconds. When schedule is run, plugin will wait this time before trying to get schedule state. If schedule is still running, plugin will wait this time again. By default this value is 5 seconds.
6. Select how plugin should set "Done" status value: to Success or Failed.
7. Press button "Select Schedules" to get a list of all available schedules. Select schedules you want to run.
8. Add Post-Build "Publish JUnit test result report" to your job. Enter JUnit report file name. It MUST be the same you've entered before!
9. Run your job and get results. Enjoy!

# Screenshots
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/highlight1.png)
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/highlight2.png)
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/highlight3.png)
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/screen1.png)
