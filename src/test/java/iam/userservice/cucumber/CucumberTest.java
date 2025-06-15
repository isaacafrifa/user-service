package iam.userservice.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Test runner for Cucumber tests.
 * This class configures Cucumber to run the feature files and step definitions.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"iam.userservice.cucumber.steps", "iam.userservice.cucumber.config"},
    plugin = {"pretty", "html:target/cucumber-reports"}
)
public class CucumberTest {
    // This class is empty. It's just a runner.
}