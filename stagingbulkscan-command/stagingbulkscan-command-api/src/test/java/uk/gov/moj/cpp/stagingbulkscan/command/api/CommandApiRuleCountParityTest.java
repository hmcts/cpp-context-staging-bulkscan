package uk.gov.moj.cpp.stagingbulkscan.command.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * J17/J25 parity characterisation (J17 golden master), BSC-M04 / BC-20.
 *
 * Deny assertions in access-control tests also pass when a kbase loads no rules at all, so pin exactly which rules
 * the COMMAND_API kbase loads. The Drools engine moves from 7.69 to 10.1 in the J25 upgrade; the DRL is unchanged.
 */
class CommandApiRuleCountParityTest {

    @Test
    void bscM04_commandApiKbaseShouldLoadBothAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("COMMAND_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Command - API - Action - stagingbulkscan.register-scan-envelope",
                "Command - API - Action - stagingbulkscan.mark-as-action"));
    }
}
