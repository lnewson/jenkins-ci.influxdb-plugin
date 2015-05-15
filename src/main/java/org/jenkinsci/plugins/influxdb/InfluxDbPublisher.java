package org.jenkinsci.plugins.influxdb;

import hudson.plugins.robot.RobotBuildAction;
import hudson.tasks.test.AbstractTestResultAction;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.jenkinsci.plugins.influxdb.generators.CoberturaSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.RobotFrameworkSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.SerieGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


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
    private String serieName;

    private String protocol;


    public InfluxDbPublisher() {
    }

    public InfluxDbPublisher(String ip, String metric, String protocol) {
        this.selectedIp = ip;
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

    public void setSelectedIp(String ip) {
        this.selectedIp = ip;
    }

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

        CoberturaSerieGenerator cbGenerator = new CoberturaSerieGenerator(build);
        if(cbGenerator.hasReport()) {
            influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, cbGenerator.generate());
        }

        SerieGenerator rfGenerator = new RobotFrameworkSerieGenerator(build);
        if(rfGenerator.hasReport()) {
            influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, rfGenerator.generate());
        }

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
