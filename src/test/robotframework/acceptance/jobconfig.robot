*** Settings ***
Test setup        Login to Jenkins
Test teardown     Close browser
Force tags        EPIC_JOB_CONFIG
Resource          jenkins.robot

*** Testcase ***
User can add InfluxDB publisher as post build action
   [Tags]                           TODO
   Go to global configuration
   Wait until page contains         InfluxDb Server

User can select which InfluxDB server is used for reporting metrics
   [Tags]                           TODO
   Go to global configuration
   Wait until page contains         InfluxDb Server
