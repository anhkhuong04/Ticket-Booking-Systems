package com.lak.moviebooking.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTests {

	private static final String ROOT_PACKAGE = "com.lak.moviebooking";
	private static final List<String> BUSINESS_MODULES = List.of(
			"identity",
			"authorization",
			"catalog",
			"cinema",
			"showtime",
			"reservation",
			"booking",
			"voucher",
			"payment",
			"refund",
			"ticketing",
			"notification",
			"reporting",
			"audit");
	private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages(ROOT_PACKAGE);

	@Test
	void modulesAreFreeOfCycles() {
		slices().matching(ROOT_PACKAGE + ".(*)..")
				.should().beFreeOfCycles()
				.check(PRODUCTION_CLASSES);
	}

	@Test
	void moduleInternalsAreNotAccessedAcrossBoundaries() {
		for (String module : BUSINESS_MODULES) {
			String modulePackage = ROOT_PACKAGE + "." + module;
			DescribedPredicate<JavaClass> internalType = resideInAnyPackage(
					modulePackage + ".api..",
					modulePackage + ".infrastructure..")
					.or(resideInAPackage(modulePackage + ".domain..")
							.and(resideInAPackage(modulePackage + ".domain.event..").negate()));

			noClasses().that().resideOutsideOfPackage(modulePackage + "..")
					.should().dependOnClassesThat(internalType)
					.allowEmptyShould(true)
					.check(PRODUCTION_CLASSES);
		}
	}

	@Test
	void layersOnlyDependInward() {
		noClasses().that().resideInAPackage("..domain..")
				.should().dependOnClassesThat().resideInAnyPackage(
						"org.springframework..",
						"jakarta.persistence..",
						"..api..",
						"..application..",
						"..infrastructure..")
				.allowEmptyShould(true)
				.check(PRODUCTION_CLASSES);

		noClasses().that().resideInAPackage("..application..")
				.should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..")
				.allowEmptyShould(true)
				.check(PRODUCTION_CLASSES);

		noClasses().that().resideInAPackage("..api..")
				.should().dependOnClassesThat().resideInAPackage("..infrastructure..")
				.allowEmptyShould(true)
				.check(PRODUCTION_CLASSES);
	}

	@Test
	void webControllersLiveInApiPackages() {
		classes().that().areAnnotatedWith(RestController.class)
				.should().resideInAPackage("..api..")
				.check(PRODUCTION_CLASSES);
	}

	@Test
	void applicationUsesConstructorInjection() {
		NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(PRODUCTION_CLASSES);
	}

}
