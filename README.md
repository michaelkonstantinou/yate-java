# YATE - Official Java/Kotlin implementation

> Yet Another TEst generator

**YATE** is a research project for generating unit tests using LLMs. Its
approach is to incrementally provide more meaningful content when repairing the LLM's response

The repository contains the core implementation of YATE. For the plugin, please check the yate-jk-plugin repository

## How to use

> **USE YATE-JK-PLUGIN repository to run experiments**

Further information coming soon...

### Relevant paper

[YATE: The Role of Test Repair in LLM-Based Unit Test Generation](https://arxiv.org/abs/2507.18316)

If this repository or the linked paper helped you in anyway, please make sure to cite it


## Requirements

For detailed setup instructions of .env, see the [Environment Setup Guide](./SETUP_ENV.md).

> YATE is a research project, not an enterprise application. If your setup is not supported, please raise an issue and help us improve this tool

The following libraries must be included in the **project under test**, in order for YATE to work

- Testing framework of your choice with mocking library. Current known supported frameworks
  - Junit5 + Mockito
  - TestNG + Mockito
- Surefire reports
  - Used to analyze non-passing tests
- Pi-Test for mutation score
- Jacoco for coverage

The following dependencies have been tried, and we know that work

```xml
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>4.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <version>5.9.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.vintage</groupId>
        <artifactId>junit-vintage-engine</artifactId>
        <version>5.9.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-inline</artifactId>
        <version>4.8.1</version>
        <scope>test</scope>
    </dependency>
```

The following plugins have been tried, and we know that work
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>2.22.2</version>
    <configuration>
        <properties>
            <property>
                <name>listener</name>
                <value>com.gzoltar.internal.core.listeners.JUnitListener</value>
            </property>
        </properties>
    </configuration>
</plugin>
<plugin>
<groupId>org.pitest</groupId>
<artifactId>pitest-maven</artifactId>
<version>1.18.2</version>
<configuration>
    <threads>16</threads>
</configuration>
<dependencies>
    <dependency>
        <groupId>org.pitest</groupId>
        <artifactId>pitest-junit5-plugin</artifactId>
        <version>1.2.2</version>
    </dependency>
</dependencies>
</plugin>
<plugin>
<groupId>org.jacoco</groupId>
<artifactId>jacoco-maven-plugin</artifactId>
<version>0.8.13</version>
<executions>
    <execution>
        <goals>
            <goal>prepare-agent</goal>
        </goals>
    </execution>
    <!-- attached to Maven test phase -->
    <execution>
        <id>report</id>
        <phase>test</phase>
        <goals>
            <goal>report</goal>
        </goals>
    </execution>
</executions>
</plugin>
```