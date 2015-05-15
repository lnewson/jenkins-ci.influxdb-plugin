package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import org.influxdb.dto.Serie;

/**
 * Created by jrajala on 15.5.2015.
 */
public interface SerieGenerator {

    public boolean hasReport();

    public Serie[] generate();

}
