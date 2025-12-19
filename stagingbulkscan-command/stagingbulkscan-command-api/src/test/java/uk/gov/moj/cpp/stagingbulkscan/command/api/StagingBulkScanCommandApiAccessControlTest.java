package uk.gov.moj.cpp.stagingbulkscan.command.api;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class StagingBulkScanCommandApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_NAME_REGISTER_SCAN_ENVELOPE = "stagingbulkscan.register-scan-envelope";
    private static final String ACTION_NAME_MARK_AS_ACTION = "stagingbulkscan.mark-as-action";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public StagingBulkScanCommandApiAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, this.userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToRegisterScanEnvelope() {
        final Action action = createActionFor(ACTION_NAME_REGISTER_SCAN_ENVELOPE);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "System Users"))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToMarkDocumentAsActioned() {
        final Action action = createActionFor(ACTION_NAME_MARK_AS_ACTION);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Court Administrators", "Crown Court Admin","System Users"))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }
}