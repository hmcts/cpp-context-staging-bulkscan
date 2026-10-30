package uk.gov.moj.cpp.bulkscan.azure.function;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.BlobClientProvider;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * J17/J25 parity characterisation (J17 golden master), BSC-M01.
 *
 * This module is packaged as a separate Azure Function App (fa-*-scan-processor, Windows, Functions v4). Its runtime
 * javaVersion and JAVA_VERSION app setting come from java.se.version, which is 17 on main and 25 after the upgrade,
 * and the classes are compiled for that version. It also bundles stagingbulkscan-azure-core and the framework jars
 * (utilities-core, messaging-core), which are compiled for Java 25 (major version 69) on 25.104.x. A Function App can
 * only load these classes if it runs that Java runtime. The J25 upgrade moves the Function App to Java 25, so pin the
 * J25 class-file level (major version 69). The Function App's runtime must move to Java 25 in the same release as
 * this jar.
 */
class FunctionAppRuntimeCompatibilityParityTest {

    private static final int JAVA_25_CLASS_FILE_MAJOR_VERSION = 69;

    @ParameterizedTest
    @ValueSource(classes = {
            PoliceEmailExtractorFunction.class,
            BulkScanInboxProcessor.class,
            BlobClientProvider.class,
            JsonObjects.class,
            StringToJsonObjectConverter.class})
    void bscM01_classesShouldTargetTheJava25FunctionAppRuntime(final Class<?> type) throws IOException {
        assertThat(type.getName(), classFileMajorVersion(type), is(JAVA_25_CLASS_FILE_MAJOR_VERSION));
    }

    private static int classFileMajorVersion(final Class<?> type) throws IOException {
        try (InputStream classFile = type.getResourceAsStream(type.getSimpleName() + ".class");
             DataInputStream data = new DataInputStream(classFile)) {
            data.readInt();
            data.readUnsignedShort();
            return data.readUnsignedShort();
        }
    }
}
