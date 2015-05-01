package org.jenkinsci.plugins.influxdb;

import hudson.tasks.test.AbstractTestResultAction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;
import org.jenkinsci.plugins.influxdb.loggers.GraphiteLogger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jenkinsci.plugins.influxdb.metrics.AbstractMetric;
import org.jenkinsci.plugins.influxdb.metrics.BuildDurationMetric;
import org.jenkinsci.plugins.influxdb.metrics.BuildFailedMetric;
import org.jenkinsci.plugins.influxdb.metrics.BuildSuccessfulMetric;
import org.jenkinsci.plugins.influxdb.metrics.CoberturaCodeCoverageMetric;
import org.jenkinsci.plugins.influxdb.metrics.FailTestsMetric;
import org.jenkinsci.plugins.influxdb.metrics.SkipTestsMetric;
import org.jenkinsci.plugins.influxdb.metrics.TotalTestsMetric;

import utils.MetricsEnum;

/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 * 
 */
public class InfluxDbPublisher extends Notifier {


    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";
    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";


    private String selectedIp;
    private String selectedMetric;
    private String serieName;

    /**
     *
     */
    private String protocol;

    private List<Metric> metrics = new ArrayList<Metric>();

    /**
     *
     */
    public InfluxDbPublisher() {
    }

    /**
     *
     */
    public InfluxDbPublisher(String ip, String metric, String protocol) {
        this.selectedIp = ip;
        this.selectedMetric = metric;
        this.protocol = protocol;
        System.out.println("IP: " + ip);
        System.out.println("Protocol: " + protocol);

    }
    
    

    public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setMetrics(List<Metric> metrics) {
		this.metrics = metrics;
	}

	/**
	 * 
	 * @return metrics
	 */
    public List<Metric> getMetrics() {
        return metrics;
    }

    /**
     * 
     * @return selectedIp
     */
    public String getSelectedIp() {
        String ipTemp = selectedIp;
        if (ipTemp == null) {
            Server[] servers = DESCRIPTOR.getServers();
            if (servers.length > 0) {
                ipTemp = servers[0].getHost();
            }
        }
        return ipTemp;
    }

    public String getSerieName() {
        return serieName;
    }

    public void setSerieName(String serieName) {
        this.serieName = serieName;
    }

    /**
     * 
     * @return selectedMetric
     */
    public String getSelectedMetric() {
        String metricTemp = selectedMetric;
        if (metricTemp == null) {
            Metric[] metrics = DESCRIPTOR.getMetrics();
            if (metrics.length > 0) {
                metricTemp = metrics[0].getName();
            }
        }
        return metricTemp;
    }

    /**
     * 
     * @param ip
     */
    public void setSelectedIp(String ip) {
        this.selectedIp = ip;
    }

    /**
     * 
     * @param metric
     */
    public void setSelectedMetric(String metric) {
        this.selectedMetric = metric;
    }

    /**
     * 
     * @return server
     */
    public Server getServer() {
        Server[] servers = DESCRIPTOR.getServers();
        if (selectedIp == null && servers.length > 0) {
            return servers[0];
        }
        for (Server server : servers) {
            if (server.getHost().equals(selectedIp)) {
                return server;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#prebuild(hudson.model.AbstractBuild
     * , hudson.model.BuildListener)
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.tasks.Publisher#needsToRunAfterFinalized()
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hudson.tasks.Notifier#getDescriptor()
     */
    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
     * , hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        Server server = getServer();
        Serie metricSerie = buildSerieFromBuild(build);
        InfluxDB influxDB = openInfluxDb(server);
        influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, metricSerie);

        /*
        if (build.getResult() == Result.ABORTED) {
            return true;
        }

        if (getServer() == null) {
            return false;
        }

        listener.getLogger().println("Connecting to " + getServer().getDescription());

        GraphiteLogger graphiteLogger = new GraphiteLogger(listener.getLogger());

        AbstractMetric metricSender = null;

        List<Metric> coberturaMetrics = null;

        for (Metric metric : metrics) {
            if (metric.name.equals(MetricsEnum.BUILD_DURATION.name())) {
                metricSender = new BuildDurationMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
                metricSender.sendMetric(getServer(), metric);
            }
            if (metric.name.equals(MetricsEnum.BUILD_FAILED.name())) {
                metricSender = new BuildFailedMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
                metricSender.sendMetric(getServer(), metric);
            }
            if (metric.name.equals(MetricsEnum.BUILD_SUCCESSFUL.name())) {
                metricSender = new BuildSuccessfulMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
                metricSender.sendMetric(getServer(), metric);
            }
            if (isCoberturaMetric(metric)) {
                if (!isCoberturaListInitialized(coberturaMetrics)) {
                    coberturaMetrics = new ArrayList<Metric>();
                }
                coberturaMetrics.add(metric);
            }
            // If a Freestyle Build has been configured (without publishing
            // JUnit XML Results) these will fail.
            // Added simple null check in for now to be safe.
            if (build.getTestResultAction() != null) {
                if (metric.name.equals(MetricsEnum.FAIL_TESTS.name())) {
                    metricSender = new FailTestsMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
                    metricSender.sendMetric(getServer(), metric);
                }
                if (metric.name.equals(MetricsEnum.SKIPED_TESTS.name())) {
                    metricSender = new SkipTestsMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
                    metricSender.sendMetric(getServer(), metric);
                }
                if (metric.name.equals(MetricsEnum.TOTAL_TESTS.name())) {
                    metricSender = new TotalTestsMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
                    metricSender.sendMetric(getServer(), metric);
                }
            }
        }
        if (isCoberturaListInitialized(coberturaMetrics)) {
            metricSender = new CoberturaCodeCoverageMetric(build, listener.getLogger(), graphiteLogger, DESCRIPTOR.getDatabaseName());
            metricSender.sendMetric(getServer(), coberturaMetrics.toArray(new Metric[coberturaMetrics.size()]));
        }
        */

        return true;
    }

    private Serie buildSerieFromBuild(AbstractBuild<?, ?> build) {
        Serie.Builder builder = new Serie.Builder(serieName);

        List<String> columnNames = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addProjectName(build, columnNames, values);
        addBuildNumber(build, columnNames, values);
        addBuildDuration(build, columnNames, values);
        addBuildStatusSummaryMesssage(build, columnNames, values);
        addProjectBuildHealth(build, columnNames, values);
        if(hasTestResults(build)) {
            addTestsFailed(build, columnNames, values);
            addTestsSkipped(build, columnNames, values);
            addTestsTotal(build, columnNames, values);
        }
        return builder.columns(columnNames.toArray(new String[columnNames.size()])).values(values.toArray()).build();

    }

    private void addProjectName(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_NAME);
        values.add(build.getProject().getName());
    }

    private void addBuildNumber(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_NUMBER);
        values.add(build.getNumber());
    }

    private void addBuildDuration(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_TIME);
        values.add(build.getDuration());
    }

    private void addBuildStatusSummaryMesssage(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_STATUS_MESSAGE);
        values.add(build.getBuildStatusSummary().message);
    }

    private void addProjectBuildHealth(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_BUILD_HEALTH);
        values.add(build.getProject().getBuildHealth().getScore());
    }

    private boolean hasTestResults(AbstractBuild<?, ?> build) {
        return build.getAction(AbstractTestResultAction.class) != null;
    }

    private void addTestsTotal(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getTotalCount());
        columnNames.add(TESTS_TOTAL);
    }

    private void addTestsFailed(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getFailCount());
        columnNames.add(TESTS_FAILED);
    }

    private void addTestsSkipped(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        values.add(build.getAction(AbstractTestResultAction.class).getSkipCount());
        columnNames.add(TESTS_SKIPPED);
    }

    private InfluxDB openInfluxDb(Server server) {
        return InfluxDBFactory.connect("http://" + server.getHost() + ":" + server.getPort(), server.getUser(), server.getPassword());
    }

}
