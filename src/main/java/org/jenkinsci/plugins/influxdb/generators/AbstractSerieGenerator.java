package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import org.influxdb.dto.Serie;

import java.util.List;

/**
 * Created by jrajala on 15.5.2015.
 */
public abstract class AbstractSerieGenerator implements  SerieGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    protected void addJenkinsProjectName(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_NAME);
        values.add(build.getProject().getName());
    }

    protected void addJenkinsBuildNumber(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_NUMBER);
        values.add(build.getNumber());
    }
}
