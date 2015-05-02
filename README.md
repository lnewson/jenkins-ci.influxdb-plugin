# InfluxDb Jenkins Plugin

This plugin allows you to send the build metrics to InfluxDb servers to be used for analysis and radiators. Codebase
is forked from Jenkings Graphite Plugin and refactored to suit purpose.

Currently suppored metrics:
   - project_name
   - build_number
   - build_time
   - build_status_message
   - project_build_health
   - tests_failed
   - tests_skipped
   - tests_total
   - cobertura_package_coverage_rate
   - cobertura_class_coverage_rate
   - cobertura_line_coverage_rate
   - cobertura_branch_coverage_rate
   - cobertura_number_of_packages
   - cobertura_number_of_sourcefiles
   - cobertura_number_of_classes
   
Future plans:
   - Supporting Robot Framework plugin metrics
   - Getting plugin into Jenkins distribution
   
