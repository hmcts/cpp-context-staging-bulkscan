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
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * J17/J25 parity characterisation (J17 golden master), BSC-M01.
 *
 * This module is packaged as a separate Azure Function App (fa-*-scan-processor, Windows, Functions v4) whose Java
 * worker is Java 17. Everything it loads must therefore be Java 17 bytecode (class-file major version 61) or older.
 * On the J25 branch the module, stagingbulkscan-azure-core and the framework jars it bundles are compiled for
 * Java 25 (major version 69), so a Java 17 (or 21) worker fails with UnsupportedClassVersionError.
 */
class FunctionAppRuntimeCompatibilityParityTest {

    private static final int JAVA_17_CLASS_FILE_MAJOR_VERSION = 61;

    @ParameterizedTest
    @ValueSource(classes = {
            PoliceEmailExtractorFunction.class,
            BulkScanInboxProcessor.class,
            BlobClientProvider.class,
            JsonObjects.class,
            StringToJsonObjectConverter.class})
    void bscM01_classesShouldBeLoadableByTheJava17FunctionAppRuntime(final Class<?> type) throws IOException {
        assertThat(type.getName(), classFileMajorVersion(type), lessThanOrEqualTo(JAVA_17_CLASS_FILE_MAJOR_VERSION));
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
