package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import org.influxdb.dto.Point;

import java.util.HashMap;
import java.util.List;
import java.io.PrintStream;
import java.util.Map;


public abstract class AbstractSerieGenerator implements SerieGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    protected final AbstractBuild<?, ?> build;
    protected final PrintStream logger;

    public AbstractSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        this.build = build;
        this.logger = logger;
    }

    protected void addJenkinsBaseInfo(Point.Builder pointBuilder) {
        if (build.getProject() instanceof hudson.matrix.MatrixConfiguration)
        {
            // make child project name the same as the parent project name
            pointBuilder.tag(PROJECT_NAME, build.getProject().getParent().getDisplayName());

            // Create a tag for each matrix axis, prefixed with "matrix_"
            Map<String,String> matrixVariables = build.getBuildVariables();
            for (Map.Entry<String, String> variable : matrixVariables.entrySet()) {
                pointBuilder.tag("matrix_" + variable.getKey(), variable.getValue());
            }
        }
        else {
            pointBuilder.tag(PROJECT_NAME, build.getProject().getName());
        }

        pointBuilder.field(BUILD_NUMBER, build.getNumber());
    }
}
