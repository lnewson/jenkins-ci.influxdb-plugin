package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import org.influxdb.dto.Point;

import java.util.HashMap;
import java.util.List;
import java.io.PrintStream;


/**
 * Created by jrajala on 15.5.2015.
 */
public abstract class AbstractSerieGenerator implements SerieGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    protected final AbstractBuild<?, ?> build;
    protected final PrintStream logger;

    public AbstractSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        this.build = build;
        this.logger = logger;
    }

    protected void addJenkinsProjectName(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_NAME);
        values.add(build.getProject().getName());
    }

    protected void addJenkinsBuildNumber(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_NUMBER);
        values.add(build.getNumber());
    }

    protected HashMap<String, Object> zipListsToMap(List<String> columns, List<Object> values) {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        for(int i=0; i<columns.size(); i++){
            fields.put(columns.get(i), values.get(i));
        }
        return fields;
    }
}
