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
import org.jenkinsci.plugins.influxdb.generators.JenkinsBaseSerieGenerator;
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
        InfluxDB influxDB = openInfluxDb(server);

        JenkinsBaseSerieGenerator jGenerator = new JenkinsBaseSerieGenerator(build);
        influxDB.write(server.getDatabaseName(), TimeUnit.MILLISECONDS, jGenerator.generate());

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

    private InfluxDB openInfluxDb(Server server) {
        return InfluxDBFactory.connect("http://" + server.getHost() + ":" + server.getPort(), server.getUser(), server.getPassword());
    }



}
