package com.techsupport.ci;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * GitHub Actions CI Workflow Configuration Tests
 *
 * <p>Validates the CI workflow configuration to ensure: - Workflow file exists and is valid YAML -
 * Triggers are configured for push/PR to main - Java 21 setup with correct distribution - Gradle
 * caching is enabled - Required build steps are present - Artifact publishing is configured
 *
 * <p>Acceptance Criteria Coverage: - AC-1.2.1: Workflow triggers and Java setup - AC-1.2.2: Build
 * goals and artifact publishing - AC-1.2.3: SonarCloud integration - AC-1.2.4: Gradle dependency
 * caching
 */
class CIWorkflowConfigTest {

  private static final Path WORKFLOW_FILE = Paths.get(".github/workflows/ci.yml");
  private static final Path SONAR_PROPERTIES = Paths.get("sonar-project.properties");
  private static final Path BUILD_GRADLE = Paths.get("build.gradle");

  /** AC-1.2.1: Verify workflow file exists and is valid YAML */
  @Test
  void workflowFileShouldExistAndBeValidYaml() throws IOException {
    assertThat(Files.exists(WORKFLOW_FILE))
        .as("CI workflow file should exist at .github/workflows/ci.yml")
        .isTrue();

    String content = Files.readString(WORKFLOW_FILE);
    assertThat(content).isNotEmpty();

    // Parse as YAML to ensure it is valid
    Yaml yaml = new Yaml();
    Object parsed = yaml.load(content);
    assertThat(parsed).isNotNull();
  }

  /** AC-1.2.1: Verify triggers are configured for push and pull_request to main */
  @Test
  void workflowShouldTriggerOnPushAndPullRequest() throws IOException {
    String content = Files.readString(WORKFLOW_FILE);

    assertThat(content)
        .as("Workflow should trigger on push to main")
        .contains("push:")
        .contains("branches: [main]");

    assertThat(content)
        .as("Workflow should trigger on pull_request to main")
        .contains("pull_request:");
  }

  /** AC-1.2.1: Verify Java 21 setup with Temurin distribution */
  @Test
  void workflowShouldSetupJava21WithTemurin() throws IOException {
    String content = Files.readString(WORKFLOW_FILE);

    assertThat(content)
        .as("Workflow should use actions/setup-java@v4")
        .contains("actions/setup-java@v4");

    assertThat(content)
        .as("Workflow should configure Java 21")
        .containsPattern("java-version:\\s*'?21'?");

    assertThat(content)
        .as("Workflow should use Temurin distribution")
        .contains("distribution: 'temurin'");
  }

  /** AC-1.2.4: Verify Gradle caching is configured */
  @Test
  void workflowShouldUseGradleCaching() throws IOException {
    String content = Files.readString(WORKFLOW_FILE);

    assertThat(content)
        .as("Workflow should use gradle/actions/setup-gradle")
        .contains("gradle/actions/setup-gradle@v4");
  }

  /** AC-1.2.2: Verify build and test steps are present */
  @Test
  void workflowShouldIncludeBuildAndTestSteps() throws IOException {
    String content = Files.readString(WORKFLOW_FILE);

    assertThat(content).as("Workflow should run spotlessCheck").contains("./gradlew spotlessCheck");

    assertThat(content).as("Workflow should run gradle build").contains("./gradlew clean build");

    assertThat(content)
        .as("Workflow should generate JaCoCo report")
        .contains("./gradlew jacocoTestReport");
  }

  /** AC-1.2.2: Verify JaCoCo artifact publishing */
  @Test
  void workflowShouldPublishJaCoCoArtifacts() throws IOException {
    String content = Files.readString(WORKFLOW_FILE);

    assertThat(content)
        .as("Workflow should upload JaCoCo reports")
        .contains("actions/upload-artifact@v4")
        .contains("jacoco-report")
        .contains("build/reports/jacoco/test/");

    assertThat(content).as("Workflow should set artifact retention").contains("retention-days: 30");
  }

  /**
   * AC-1.2.3: SonarCloud integration removed from CI workflow
   * SonarCloud uses automatic scheduled scans every 10 days instead
   */

  /** AC-1.2.3: Verify sonar-project.properties exists and is configured */
  @Test
  void sonarPropertiesFileShouldExistAndBeConfigured() throws IOException {
    assertThat(Files.exists(SONAR_PROPERTIES)).as("sonar-project.properties should exist").isTrue();

    String content = Files.readString(SONAR_PROPERTIES);

    assertThat(content)
        .as("Sonar properties should define project key")
        .contains("sonar.projectKey=tech-support");

    assertThat(content)
        .as("Sonar properties should point to SonarCloud")
        .contains("sonar.host.url=https://sonarcloud.io");

    assertThat(content)
        .as("Sonar properties should configure JaCoCo XML report path")
        .contains("sonar.coverage.jacoco.xmlReportPaths")
        .contains("jacocoTestReport.xml");
  }

  /** AC-1.2.2, AC-1.2.3: Verify build.gradle has sonarqube plugin */
  @Test
  void buildGradleShouldHaveSonarQubePlugin() throws IOException {
    assertThat(Files.exists(BUILD_GRADLE)).as("build.gradle should exist").isTrue();

    String content = Files.readString(BUILD_GRADLE);

    assertThat(content)
        .as("build.gradle should include sonarqube plugin")
        .contains("id 'org.sonarqube' version '6.0.1.5171'");
  }

  /** AC-1.2.4: Verify Gradle build cache is configured */
  @Test
  void workflowShouldUseBuildCache() throws IOException {
    String content = Files.readString(WORKFLOW_FILE);

    assertThat(content).as("Workflow should use Gradle build cache").contains("--build-cache");
  }

  /** Integration test: Verify all required workflow steps are present in order */
  @Test
  void workflowShouldHaveAllStepsInCorrectOrder() throws IOException {
    List<String> lines = Files.readAllLines(WORKFLOW_FILE);

    // Find indices of key steps
    int checkoutIdx = findStepIndex(lines, "Checkout code");
    int javaSetupIdx = findStepIndex(lines, "Set up Java");
    int gradleSetupIdx = findStepIndex(lines, "Setup Gradle");
    int spotlessIdx = findStepIndex(lines, "spotlessCheck");
    int buildIdx = findStepIndex(lines, "clean build");
    int jacocoIdx = findStepIndex(lines, "jacocoTestReport");
    int uploadIdx = findStepIndex(lines, "upload-artifact");

    // Verify order
    assertThat(checkoutIdx).as("Checkout should be first").isLessThan(javaSetupIdx);
    assertThat(javaSetupIdx)
        .as("Java setup should come before Gradle setup")
        .isLessThan(gradleSetupIdx);
    assertThat(gradleSetupIdx)
        .as("Gradle setup should come before spotless check")
        .isLessThan(spotlessIdx);
    assertThat(spotlessIdx).as("Spotless check should come before build").isLessThan(buildIdx);
    assertThat(buildIdx).as("Build should come before JaCoCo report").isLessThan(jacocoIdx);
    assertThat(jacocoIdx)
        .as("JaCoCo report should come before artifact upload")
        .isLessThan(uploadIdx);
  }

  private int findStepIndex(List<String> lines, String keyword) {
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains(keyword)) {
        return i;
      }
    }
    return -1;
  }
}
