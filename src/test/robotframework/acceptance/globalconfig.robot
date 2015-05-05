*** Settings ***
Library    Selenium2Library

*** Testcase ***
Test case 1
   Open browser                     ${server}    ${browser}
   Input text                       id=j_username    ${user}
   Input text                       name=j_password    ${password}
   Click element                    id=yui-gen1-button
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
   [Teardown]                       Close browser
