package iam.userservice.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Test runner for Cucumber tests.
 * This class configures Cucumber to run the feature files and step definitions.
 * Uses JUnit 5 platform for running tests.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "iam.userservice.cucumber.steps,iam.userservice.cucumber.config")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports,summary") // summary enables detailed Cucumber execution output summary
public class CucumberTest {
    // This class is empty. It's just a runner.
}
