*** Settings ***
Test setup        Login to Jenkins
Test teardown     Close browser
Force tags        EPIC_GLOBAL_CONFIG
Resource          jenkins.robot

Library    Selenium2Library

*** Testcase ***
Install Plugin
   [Tags]                           RELEASED
   Install InfluxDB Plugin

User can add new InfluxDB Server in global configuration
   [Tags]                           WIP
   Go to global configuration
   Wait until page contains         InfluxDb Server

User can change setting of InfluxDB Server in global configuration
   [Tags]                           TODO
   Go to global configuration
   Wait until page contains         InfluxDb Server

User can delete existing InfluxDB Server in global configuration
   [Tags]                           TODO
   Go to global configuration
   Wait until page contains         InfluxDb Server



