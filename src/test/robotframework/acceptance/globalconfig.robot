*** Settings ***
Test setup    Login to Jenkins
Test teardown   Close browser

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
   [Tags]                           WIP
   Go to global configuration
   Wait until page contains         InfluxDb Server

User can delete existing InfluxDB Server in global configuration
   [Tags]                           WIP
   Go to global configuration
   Wait until page contains         InfluxDb Server

User can add InfluxDB publisher as post build action
   [Tags]                           WIP
   Go to global configuration
   Wait until page contains         InfluxDb Server

User can select which InfluxDB server is used for reportint metrics
   [Tags]                           WIP
   Go to global configuration
   Wait until page contains         InfluxDb Server


*** Keywords ***
Login to Jenkins
   Open browser                     ${server}    ${browser}
   Input text                       id=j_username    ${user}
   Input text                       name=j_password    ${password}
   Click element                    id=yui-gen1-button

Install InfluxDB Plugin
   Go to                            ${server}/pluginManager/advanced
   Choose file                      name=name    ${pluginfile}
   Click element                    id=yui-gen3-button
   Wait Until Element Is Visible    id=scheduleRestartCheckbox
   Select checkbox                  id=scheduleRestartCheckbox
   Sleep                            5
   Go to                            ${server}
   Wait Until page contains         Please wait while Jenkins is restarting
   Sleep    30
   Go to                            ${server}

Go to global configuration
    Go to                           ${server}/configure
