package org.jenkinsci.plugins.influxdb.metrics;

import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.influxdb.loggers.GraphiteLogger;
import org.jenkinsci.plugins.influxdb.Metric;
import org.jenkinsci.plugins.influxdb.Server;

import org.jenkinsci.plugins.influxdb.InfluxDbValidator;

/**
 * 
 * @author joachimrodrigues
 */
public abstract class AbstractMetric {

	protected InfluxDbValidator validator = new InfluxDbValidator();
	protected final AbstractBuild<?, ?> build;
	protected final PrintStream logger;
	protected final GraphiteLogger graphiteLogger;
	protected final String baseQueueName;

	/**
	 * 
	 * @param build
	 * @param logger
	 * @param graphiteLogger
	 */
	public AbstractMetric(AbstractBuild<?, ?> build, PrintStream logger, GraphiteLogger graphiteLogger, String baseQueueName) {
		this.build = build;
		this.logger = logger;
		this.graphiteLogger = graphiteLogger;
		this.baseQueueName = baseQueueName;
	}

	
	/**
	 * @param server
	 * @param metric
	 * @param value
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	protected void sendMetric(Server server, Metric metric, String value) throws
			IOException {
		logger.println("Trying to send metric to Graphite server : " + server.getHost() + ":" + server.getPort() + ", Metric name: " + metric.getName() +  " On queue : "
				+ metric.getQueueName() + " With value : " + value);

		/*
		if (server.getProtocol().equals("UDP")) {
			if (validator.isListening(server.getHost(), Integer.parseInt(server.getPort()))) {
				graphiteLogger.logToGraphite(server.getHost(), server.getPort(), getCheckedBaseQueueName() + metric.getFullQueueAndName(), value.trim(), server.getProtocol());
				logger.println("Metric: " + metric.getName() + " with value: "+ value + " correctly sent to " + server.getHost() + ":" + server.getPort()
					+ " on " +  getCheckedBaseQueueName() + metric.getFullQueueAndName() + "using UDP");
			}
		}
		else if (server.getProtocol().equals("TCP")) {
			if (validator.isListening(server.getHost(), Integer.parseInt(server.getPort()))) {
				graphiteLogger.logToGraphite(server.getHost(), server.getPort(),  getCheckedBaseQueueName() + metric.getFullQueueAndName(), value.trim(), server.getProtocol());
				logger.println("Metric: " + metric.getName() + " with value: "+ value + " correctly sent to " + server.getHost() + ":" + server.getPort()
					+ " on " +  getCheckedBaseQueueName() + metric.getFullQueueAndName());
			} else {
				logger.println("Metric: " + metric.getName() + " with value: "+ value + " failed when sent to " + server.getHost() + ":" + server.getPort()
					+ " on " +  getCheckedBaseQueueName() + metric.getFullQueueAndName());
			}
		}
		*/
	}
	
	/**
	 * Check if base queuename is null or whitespace and return empty, otherwise return the base queue name with a full stop appended
	 * @return base queue name
	 */
	protected String getCheckedBaseQueueName(){
	    if(StringUtils.isBlank(baseQueueName)){
	        return "";
	    }
	    return baseQueueName.concat(".");
	}

	/**
	 * 
	 * @param server
	 * @param metric
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public abstract void sendMetric(Server server, Metric... metric) throws IOException;
}
