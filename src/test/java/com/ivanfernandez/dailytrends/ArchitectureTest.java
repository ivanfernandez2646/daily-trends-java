package com.ivanfernandez.dailytrends;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@AnalyzeClasses(packages = "com.ivanfernandez.dailytrends")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_and_application_are_framework_free = noClasses()
            .that()
            .resideInAnyPackage("..contexts..domain..", "..contexts..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "org.bson..",
                    "com.mongodb..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "org.jsoup..",
                    "java.net..",
                    "org.springdoc..",
                    "io.swagger..")
            // No domain or application code exists until the feed domain is added.
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule stereotypes_only_in_apps = noClasses()
            .that()
            .resideOutsideOfPackage("..apps..")
            .should()
            .beAnnotatedWith(Service.class)
            .orShould()
            .beAnnotatedWith(Component.class)
            .orShould()
            .beAnnotatedWith(Repository.class);

    @ArchTest
    static final ArchRule tests_start_the_app_through_integration_test = noClasses()
            .that()
            .areNotAnnotations()
            .should()
            .beAnnotatedWith(SpringBootTest.class)
            .because("@IntegrationTest keeps the working-directory config/ out of the tests");
}
