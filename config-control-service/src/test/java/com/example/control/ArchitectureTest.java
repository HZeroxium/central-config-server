package com.example.control;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests to enforce architectural constraints and prevent circular dependencies.
 * <p>
 * These tests ensure that:
 * 1. No circular dependencies exist between packages
 * 2. Services don't depend on other services in the same layer
 * 3. Command handlers depend only on ports and query services
 * 4. Query services depend only on ports
 * 5. Layered architecture is respected
 * </p>
 */
@AnalyzeClasses(packages = "com.example.control", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    /**
     * Test that there are no circular dependencies between packages.
     */
    @ArchTest
    static final ArchRule noCycles = slices()
            .matching("com.example.control.(*)..")
            .should().beFreeOfCycles();

    /**
     * Test that application services don't depend on other application services.
     * This prevents circular dependencies at the service layer.
     */
    @ArchTest
    static final ArchRule servicesShouldNotDependOnOtherServices = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().dependOnClassesThat().resideInAPackage("..application.service..");

    /**
     * Test that command handlers depend only on ports, query services, and standard libraries.
     */
    @ArchTest
    static final ArchRule commandHandlersShouldOnlyDependOnPortsAndQueries = classes()
            .that().resideInAPackage("..application.command..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..domain.port..",
                    "..application.query..",
                    "..domain..",
                    "java..",
                    "lombok..",
                    "org.springframework..",
                    "org.slf4j.."
            );

    /**
     * Test that query services depend only on ports and standard libraries.
     */
    @ArchTest
    static final ArchRule queryServicesShouldOnlyDependOnPorts = classes()
            .that().resideInAPackage("..application.query..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..domain.port..",
                    "..domain..",
                    "java..",
                    "lombok..",
                    "org.springframework..",
                    "org.slf4j.."
            );

    /**
     * Test that event listeners depend only on ports and domain events.
     */
    @ArchTest
    static final ArchRule eventListenersShouldOnlyDependOnPortsAndEvents = classes()
            .that().resideInAPackage("..application.event..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..domain.port..",
                    "..domain.event..",
                    "..domain..",
                    "java..",
                    "lombok..",
                    "org.springframework..",
                    "org.slf4j.."
            );

    /**
     * Test that domain objects don't depend on application layer.
     */
    @ArchTest
    static final ArchRule domainObjectsShouldNotDependOnApplication = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..");

    /**
     * Test that infrastructure adapters don't depend on application layer.
     */
    @ArchTest
    static final ArchRule infrastructureShouldNotDependOnApplication = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..application..");

    /**
     * Test layered architecture compliance.
     */
    @ArchTest
    static final ArchRule layeredArchitectureRule = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Web").definedBy("..web..")
            .layer("Config").definedBy("..config..")

            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Web", "Config")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Config")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Web", "Config")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Config").mayNotBeAccessedByAnyLayer();

    /**
     * Test that controllers only depend on application services and DTOs.
     */
    @ArchTest
    static final ArchRule controllersShouldOnlyDependOnServicesAndDtos = classes()
            .that().resideInAPackage("..web..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..application.service..",
                    "..application.query..",
                    "..domain.object..",
                    "..domain.criteria..",
                    "..domain.id..",
                    "java..",
                    "lombok..",
                    "org.springframework..",
                    "org.slf4j..",
                    "jakarta.validation..",
                    "com.fasterxml.jackson.."
            );
}
