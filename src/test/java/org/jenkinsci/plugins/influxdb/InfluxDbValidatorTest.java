package org.jenkinsci.plugins.influxdb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jrajala on 1.5.2015.
 */
public class InfluxDbValidatorTest {

    InfluxDbValidator validator;
    @Before
    public void setupTest() {
        validator = new InfluxDbValidator();
    }

    @Test
    public void shouldAllowValidPortNumbers() {
        for(int i = 1; i <=65535; i++) {
            Assert.assertTrue(validator.validatePortFormat("" + i));
        }
    }

    @Test
    public void shouldNotAllowPortsOusideOfValidRange() {
        Assert.assertFalse(validator.validatePortFormat(""+0));
        Assert.assertFalse(validator.validatePortFormat(""+65536));
    }

    @Test
    public void shouldNotAcceptAnythingThatDoesNotParseAsNumber() {
        Assert.assertFalse(validator.validatePortFormat("adsf"));
    }

    @Test
    public void shouldCheckIsHostPresent() {
        Assert.assertFalse(validator.isHostPresent(null));
        Assert.assertFalse(validator.isHostPresent(""));
        Assert.assertTrue(validator.isHostPresent("hostname"));
    }

    @Test
    public void shouldCheckIsPortPresent() {
        Assert.assertFalse(validator.isPortPresent(null));
        Assert.assertFalse(validator.isPortPresent(""));
        Assert.assertTrue(validator.isPortPresent("1000"));
    }

    @Test
    public void shouldCheckIsDescriptionPresent() {
        Assert.assertFalse(validator.isDescriptionPresent(null));
        Assert.assertFalse(validator.isDescriptionPresent(""));
        Assert.assertTrue(validator.isDescriptionPresent("description"));
    }

    @Test
    public void shouldNotAllowTooLongDescription() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0 ; i <= 100; i++) {
            builder.append("a");
        }
        String description = builder.toString();
        Assert.assertTrue(description.length() > 100);
        Assert.assertTrue(validator.isDescriptionTooLong(description));
    }

    @Test
    public void shouldAllowDescriptionWhichIsNotTooLong() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0 ; i <= 99; i++) {
            builder.append("a");
        }
        String description = builder.toString();
        Assert.assertTrue(description.length() == 100);
        Assert.assertFalse(validator.isDescriptionTooLong(description));
    }

    @Test
    public void shouldCheckIsDatabaseNamePresent() {
        Assert.assertFalse(validator.isDatabaseNamePresent(null));
        Assert.assertFalse(validator.isDatabaseNamePresent(""));
        Assert.assertTrue(validator.isDatabaseNamePresent("database"));
    }
}

