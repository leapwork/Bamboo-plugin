# Leapwork Integration
This is Leapwork plugin for Bamboo

# More Details
Leapwork is a mighty automation testing system and now it can be used for running [smoke, functional, acceptance] tests, generating reports and a lot more in Bamboo. You can easily configure integration directly in Bamboo enjoying UI friendly configuration page with easy connection and test suites selection.

# Features:
 - Setup and test Leapwork connection in few clicks
 - Run automated tests in your Bamboo build tasks
 - Automatically receive test results
 - Build status based tests results
 - Generate a xml report file in JUnit format
 - Write tests trace to build output log
 - Smart UI
 
# Installing
- Use atlassian-sdk maven 8.0.7
- Command: atlas-package 
- Or download jar file of OBR file from Releases section.

# Instruction
1. Add Build "Leapwork Integration" to your job.
2. Enter your Leapwork controller hostname or IP-address something like "win10-agent20" or "localhost".
3. Enter your Leapwork controller API port, by default it is 9001.
4. Enter JUnit report file name. This file will be created at your job's working directory. If there is an xml file with the same name, it will be overwritten. By default it is "report.xml".
5. Enter time delay in seconds. When schedule is run, plugin will wait this time before trying to get schedule state. If schedule is still running, plugin will wait this time again. By default this value is 5 seconds.
6. Select how plugin should set "Done" status value: to Success or Failed.
7. Press button "Select Schedules" to get a list of all available schedules. Select schedules you want to run.
8. Add Post-Build "Publish JUnit test result report" to your job. Enter JUnit report file name. It MUST be the same you've entered before!
9. Run your job and get results. Enjoy!

# Screenshots
![ScreenShot](https://github.com/leapwork/Bamboo-plugin/blob/master/src/main/resources/images/image1.png)
![ScreenShot](https://github.com/leapwork/Bamboo-plugin/blob/master/src/main/resources/images/image2.png)
![ScreenShot](https://github.com/leapwork/Bamboo-plugin/blob/master/src/main/resources/images/image3.png)

