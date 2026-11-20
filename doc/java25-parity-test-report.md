# Java 25 parity test report — staging-bulkscan

**Branch:** `dev/java25-upgrade` (`65fae31`), tests on `dev/java25-upgrade-java17-parity`.
**Baseline:** Java 17 `main` (`6656abe`), tests on `dev/CIMD-4112-parity-with-j25`.
**Jira:** CIMD-4112. **Date:** 2026-10-16. **Updated:** 2026-10-29 — dispositions applied; every finding is fixed or accepted and pinned (see *Resolution*).

## What this is

These are characterisation ("golden master") tests, following the team guide *Parity Testing Java17 → Java25*. The same scenarios and assertions run against the Java 17 `main` branch and against this Java 25 branch:
- On **Java 17 every test passes**. That records how the service behaved before the upgrade.
- On **Java 25 a failing parity test is a behaviour change introduced by the upgrade.** Each one is either fixed in production code or explicitly accepted. When a change is accepted, the Java 25 test is changed to pin the new behaviour.

Only the test harness and the `javax`/`jakarta` imports differ between the two copies. The one production change is the BSC-P01 fix in `ScanDocumentRepository`.

Apart from the viewstore repositories, every Java change in the upgrade is an import rename (`javax.*` → `jakarta.*`). No expected/golden test file, JSON schema, RAML, DRL or subscription file changed, and the 99 generated sources are identical apart from imports. The behaviour changes below come from the persistence rewrite (DeltaSpike → JPA), library versions (RESTEasy, Drools, Jackson) and the Java 25 bytecode level.

## Results

| Run | Tests | Failed | Notes |
|---|---|---|---|
| Java 17 `main` + parity tests, unit (JDK 17) | 242 | **0** | |
| Java 17 `main` + parity tests, integration (WildFly 26) | 24 | **0** | `StagingBulkScanRegisterIT` 21, `DocumentDeletionQueryIT` 3 |
| Java 25 this branch, unit (JDK 25), before resolution | 244 | **10** | all 10 are parity tests; every other test passes |
| Java 25 this branch, unit (JDK 25), after resolution (2026-10-29) | 245 | **0** | one test added for BSC-M02. `DeletingActionedDocumentsSchedulerTest#shouldComputeCorrectCutoffDateInRequestPayload` (not a parity test) was also fixed: it built its bounds in the local zone while the scheduler uses UTC, so it failed for 10 days after each daylight-saving change, on Java 17 as well. It now uses UTC |
| Java 25 this branch, integration (WildFly 40) | 24 | **0** | same classes and counts as Java 17; no finding needed an integration test |

## Behaviour changes found (tests failed on Java 25 before resolution)

The test names below are the original ones; see *Resolution* for the J25 names and what each now pins.

| ID | Severity | What changed | Test (failing on J25) | Recommendation |
|---|---|---|---|---|
| BSC-M01 | **Critical** | `stagingbulkscan-azure-functions` is packaged as its own Function App (`fa-*-scan-processor`, Windows, Functions v4). On Java 17 everything it loads is Java 17 bytecode (class version 61). On Java 25 the module itself, `stagingbulkscan-azure-core`, `utilities-core`, `messaging-core` (previously pinned to 8.0.4) and the framework jars are all Java 25 bytecode (69). A Java 17 or 21 worker fails with `UnsupportedClassVersionError` on the first zip event or police email. This repo builds the Function App but never deploys it, so the worker's Java version is set elsewhere. | `azure-functions` `FunctionAppRuntimeCompatibilityParityTest` (5) | **Fix.** Either build this module (and azure-core and its framework dependencies) for Java 17, or confirm with the platform team that the Function App can run on a worker that loads Java 25 bytecode. |
| BSC-P01 | Medium if misconfigured | `ScanDocumentRepository.findDocumentsEligibleForDeletion(…, maxResults)`. DeltaSpike's `@MaxResults` only limits the query when the value is above 0. The J25 repository calls `setMaxResults(maxResults)` unconditionally. With a `deletionBatchSize` of 0, J17 returns every eligible document and J25 returns none, so nothing is ever deleted. With a negative value, J25 throws `IllegalArgumentException`. The default is 50000, so this only bites on misconfiguration. | `viewstore-persistence` `ViewstoreRepositoryParityTest#bscP01_…` (1) | **Fix:** `if (maxResults > 0) query.setMaxResults(maxResults)`. |
| BSC-M02 | Medium (low exposure) | The helper code is unchanged, but the RESTEasy multipart provider moved from 3.15 to 7.0. In the `add material` multipart upload to stagingprosecutors, the `material` part's filename is UTF-8 on J17 and has non-ASCII characters replaced by `?` on J25 (`fé 1.pdf` becomes `f? 1.pdf`). The part order also changes. Vendor and generated file names are normally ASCII. | `azure-functions` `StagingProsecutorCommandHelperWireParityTest` (1) | **Accept or fix.** Accept if file names are always ASCII. Otherwise set the filename encoding on the part, or send an RFC 5987 `filename*`. |
| BSC-P02 | Medium, environment-dependent | On J17 the framework `EntityManagerProducer`, which the DeltaSpike repositories used, set the JVM default time zone to UTC. J25 repositories use `@PersistenceContext` and never call it. The four `ZonedDateTime` columns (`vendor_received_date`, `status_updated_date`, `deleted_date`, `extracted_date`) are `timestamp without time zone`. If the JVM is not in UTC, stored values, the deletion cut-off comparison and API dates shift by the offset. The local WildFly 40 image runs with `-Duser.timezone=UTC`, so there is no impact there. | `viewstore-persistence` `ViewstoreRepositoryParityTest#bscP02_…` (1) | **Verify** that every environment's image sets UTC, then **Accept**, as for casefile PCF-10 and SPI-P02. Otherwise fix by setting UTC explicitly. |
| BSC-P04 | Low | Two documents with the same key in one envelope still fail on both, but J17 throws a JPA `EntityExistsException` (a `PersistenceException`), while J25 throws `IllegalStateException` ("Multiple representations of the same entity"). That may change how the framework classifies the event error. | `viewstore-persistence` `ViewstoreRepositoryParityTest#bscP04_…` (1) | **Accept.** Both fail loudly. Pin the new exception in the J25 test. |
| BSC-P03 | Low | `save()` was DeltaSpike's "persist if new, else merge", which returns the same managed instance for a new entity. The J25 `save()` always merges and returns a managed copy, leaving the argument detached. No production caller uses the return value or changes the argument after `save`. | `viewstore-persistence` `ViewstoreRepositoryParityTest#bscP03_…` (1) | **Accept.** Pin the new contract in the J25 test. |

## Resolution

Settled the same way as cpp-context-staging-prosecutors-spi (CIMD-4111). Where the difference comes from the platform and no caller depends on the old behaviour, the J25 test now pins the J25 behaviour. Where a consumer would see the difference, production code is fixed.

| ID | Decision | Change | J25 test now |
|---|---|---|---|
| BSC-M01 | **Fixed:** the Function App moves to Java 25 in the same release | none needed in this repo: `stagingbulkscan-azure-functions/pom.xml` already sets `java.version` to `${java.se.version}`, so the compiler `release`, the plugin `javaVersion` and the `JAVA_VERSION` app setting are all 25 | `FunctionAppRuntimeCompatibilityParityTest#bscM01_classesShouldTargetTheJava25FunctionAppRuntime` pins class-file version 69 for the two functions, `BlobClientProvider`, `JsonObjects` and `StringToJsonObjectConverter` (5/5 pass on JDK 25) |
| BSC-P01 | **Fixed** | `ScanDocumentRepository.findDocumentsEligibleForDeletion` calls `setMaxResults` only when `maxResults > 0`, as DeltaSpike `@MaxResults` did | `bscP01_findDocumentsEligibleForDeletionShouldIgnoreAMaxResultsOfZeroOrLess` is unchanged and now passes: 0 and -1 return every eligible document, 1 returns one |
| BSC-M02 | **Accepted:** vendor file names are normally ASCII, and ASCII names are sent unchanged | none | `StagingProsecutorCommandHelperWireParityTest#bscM02_materialPartShouldCarryAnAsciiFileNameUnchanged` and `#bscM02_materialPartShouldReplaceANonAsciiFileNameCharacterWithAQuestionMark` (`fé 1.pdf` is sent as `f? 1.pdf`) |
| BSC-P02 | **Accepted:** the server runs in UTC, as SPI-P02 | none | `bscP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged` |
| BSC-P03 | **Accepted**, as SPI-P03 | none | `bscP03_saveOfNewEnvelopeShouldReturnAManagedEntityWithTheSameFields` (`samePropertyValuesAs`, ignoring the document collection, which `merge()` copies; that collection still has one document, and the returned entity is managed) |
| BSC-P04 | **Accepted:** both still fail loudly | none | `bscP04_duplicateDocumentKeyInOneEnvelopeShouldFailWithAnIllegalStateException` |

Follow-ups that come with these decisions:
- **BSC-M01:** the deployed `fa-*-scan-processor` Function App, and its `JAVA_VERSION` setting wherever infrastructure manages it, must move to Java 25 in the same release as this jar. Confirm that Azure Functions offers Java 25 on Windows for the plan.
- **BSC-M02:** if a vendor can send non-ASCII file names, the material stored in stagingprosecutors gets `?` in place of those characters. Fixing it would mean sending an RFC 5987 `filename*` on the part.
- **BSC-P02:** every environment's WildFly must run in UTC (`-Duser.timezone=UTC`, as the local WildFly 40 image does).
- **BSC-P04:** a duplicate document key now surfaces as `IllegalStateException` rather than a `PersistenceException`; check that the framework's event error handling treats it the same way.

## Checked and unchanged (tests pass on both)

| ID | Area | Test |
|---|---|---|
| BSC-M04 (BC-20) | Access-control kbases load the same rules: COMMAND_API 2, QUERY_API 7. The query API had no access-control test before this. | `command-api` `CommandApiRuleCountParityTest`, `query-api` `QueryApiRuleCountParityTest` |

Also checked by review or probe, with no test needed:
- **Persistence and queries:**
  - BC-01/02: only `findBy(PK)`, which returns null on both.
  - BC-04: the only primitive is `deleted`, NOT NULL in Liquibase.
  - BC-05: no `!= null` guards.
  - BC-06: associations are eager.
  - The JPQL queries behave identically, including ordering and `and`/`or` precedence.
  - Enum and UUID mappings and the `ScanSnapshotKey` composite key are unchanged.
- **Libraries and output:**
  - JSON-P (glassfish → Parsson): 14 edge-case probes gave identical results, so BC-11 is not triggered.
  - The notification and reference-data HTTP calls are identical on the wire.
  - The police-email metadata JSON is byte-identical.
  - The `DeletingActionedDocumentsScheduler` timer only changed its imports, and its cut-off format is the same (no BC-08).
- **Configuration:**
  - The healthchecks, the `beans.xml` discovery mode and the `ApplicationParameters` keys are unchanged.
  - BC-07: `liquibase.hub.mode` was removed.

## Informational (no test)

- **BSC-M03:** the register-scan-envelope POST body has the same JSON, with keys in a different order. Jackson 2.18+ orders properties by constructor parameter. This only matters if a consumer compares bytes.
- **BSC-M05:** the MI contract tests (`mireportdata-stagingbulkscan-event-listener:tests`) are skipped on J25, because that jar is still `javax`. They ran on J17. Re-enable them once MI publishes a jakarta test jar.
- **BSC-P05:** the persistence context is now scoped to the transaction rather than the request. There is no visible effect, because `ScanEnvelope.associatedScanDocuments` and `ScanDocument.scanEnvelope` are eager. Making either lazy would break `getDocumentResponse` and `getThumbnailResponse` on J25 only.

## How to run

```bash
# Java 25 unit tests (this branch): all pass
JAVA_HOME=<jdk-25> mvn -o clean verify -Denforcer.skipMoJLatestInterfacesRule=true

# Integration tests: cpp-developers-docker on java-25 (or java-17 for the baseline)
JAVA_HOME=<jdk-25> CPP_DOCKER_DIR=<cpp-developers-docker> \
  MAVEN_ARGS="-o -Denforcer.skipMoJLatestInterfacesRule=true" ./runIntegrationTests.sh
```

Run the same tests on `dev/CIMD-4112-parity-with-j25` with JDK 17 to see the baseline pass.

## Notes for maintainers

- When a fix lands, the matching parity test should start passing. Keep the tests as regression guards.
- `StagingProsecutorCommandHelperWireParityTest` builds a real RESTEasy client against a local `HttpServer`. The J17 copy needs empty `javax/servlet/LocalStrings.properties` test resources, because the `javaee-api` stub jar lacks them; the J25 copy does not.
- The J17 viewstore harness (HSQLDialect over H2) cannot load a UUID-keyed entity after the persistence context is cleared, so the parity tests never clear it.
- `runIntegrationTests.sh` waits only 90 seconds for the WAR to answer `ping`. On WildFly 26 the deploy can take longer, and the script then exits 0 having run no ITs. Check the IT count, not just the exit code.
