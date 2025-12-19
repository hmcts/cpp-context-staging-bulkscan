package uk.gov.moj.cpp.stagingbulkscan.it;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.stagingbulkscan.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.cpp.stagingbulkscan.utils.RestPollerWithDefaults.DELAY_IN_MILLIS;
import static uk.gov.moj.cpp.stagingbulkscan.utils.RestPollerWithDefaults.INTERVAL_IN_MILLIS;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WireMockStubUtils.setupAsSystemUser;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JmsResourceManagementExtension.class)
public abstract class BaseIntegrationTest {

    public static final UUID USER_ID_SYSTEM_USER = randomUUID();
    public static final UUID USER_ID_CROWN_COURT_USER = randomUUID();
    public static final UUID STAGING_BULKSCAN_SYSTEM_USER_ID = randomUUID();
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final UUID USER_ID_VALUE = randomUUID();
    public static final Properties ENDPOINT_PROPERTIES = new Properties();
    public static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());

    static {
        setDefaultPollDelay(DELAY_IN_MILLIS, TimeUnit.MILLISECONDS);
        setDefaultPollInterval(INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
        configureFor(HOST, 8080);

    }

    @BeforeEach
    public void setup() {
        resetAllRequests();
        reset();
        stubPingFor("usersgroups-service");
        setupAsSystemUser(USER_ID_SYSTEM_USER);
        stubForUserDetails(USER_ID_SYSTEM_USER, "ALL");
        setupAsSystemUser(STAGING_BULKSCAN_SYSTEM_USER_ID);
        stubForUserDetails(STAGING_BULKSCAN_SYSTEM_USER_ID, "ALL");
    }
}