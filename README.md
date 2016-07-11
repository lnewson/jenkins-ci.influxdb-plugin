# InfluxDB Jenkins Plugin

This plugin allows you to send the build metrics to [InfluxDB](https://influxdata.com/) time series database, to be used for analysis and in radiators. One option is to use Grafana (http://grafana.org) for visualizing the data. 

Codebase is forked from Jenkings Graphite Plugin and refactored to suit purpose.

Plugin has been tested with InfluxDB version v0.11, v0.12 and v0.13. Does not support InfluxDB versions prior v0.9.

Supported metrics:
   - Jenkins base report
   - Cobertura code coverage metrics
   - Robot Framework plugin metrics

## Jenkins base report (InfluxDB measurement __jenkins__)

Generates InfluxDB serie per project (project_name)

Supported metrics:
   - project_name
   - build_number
   - build_duration
   - build_result
   - build_result_ordinal
   - build_status_message
   - project_build_stability
   - project_build_health
   - last_successful_build
   - last_stable_build
   - tests_failed
   - tests_skipped
   - tests_total

## Cobertura code coverage metrics (InfluxDB measurement __cobertura__)

Requires Jenkins Cobertura plugin. Generates InfluxDB serie per project (project_name).

Supported metrics:
   - project_name
   - build_number
   - cobertura_package_coverage_rate
   - cobertura_number_of_packages
   - cobertura_class_coverage_rate
   - cobertura_number_of_classes
   - cobertura_line_coverage_rate
   - cobertura_number_of_lines
   - cobertura_sourcefile_coverage_rate
   - cobertura_number_of_sourcefiles
   - cobertura_condition_coverage_rate
   - cobertura_number_of_conditions
   - cobertura_method_coverage_rate
   - cobertura_number_of_methods

## Robot Framework plugin metrics (InfluxDB measurement __robotframework__)

Requires Jenkins Robot Framework plugin. Generates several InfluxDB series. 
   - summary - per project (__project_name__)
   - tag - per __tag__
   - suite - per __suite__
   - testcase - per __testcase__

###Summary data (*project_name*.rf)
   - project_name
   - build_number
   - duration
   - cases_failed
   - cases_passed
   - cases_total
   - pass_percentage
   - critical_failed
   - critical_passed
   - critical_total
   - critical_pass_percentage
   - serie
   - suite
   - suites
   - tag
   - tag_list
   - testcase_name

##Future plans:
   - Getting plugin into Jenkins distribution
