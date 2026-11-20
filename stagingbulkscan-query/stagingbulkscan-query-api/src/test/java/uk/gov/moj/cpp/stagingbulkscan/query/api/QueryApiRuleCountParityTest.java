package uk.gov.moj.cpp.stagingbulkscan.query.api;

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
 * The query API has no access-control test at all, so a kbase that loaded no rules would go unnoticed. Pin exactly
 * which rules the QUERY_API kbase loads. The Drools engine moves from 7.69 to 10.1 in the J25 upgrade; the DRL is
 * unchanged.
 */
class QueryApiRuleCountParityTest {

    @Test
    void bscM04_queryApiKbaseShouldLoadAllSevenAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("QUERY_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Query - API - stagingbulkscan.get-all-documents-by-status",
                "Query - API - stagingbulkscan.get-scan-document",
                "Query - API - stagingbulkscan.get-scan-document-by-id",
                "Query - API - stagingbulkscan.get-scan-envelope-document-by-ids",
                "Query - API - stagingbulkscan.get-thumbnail-content",
                "Query - API - stagingbulkscan.get-scan-document-status",
                "Query - API - stagingbulkscan.get-documents-for-deletion"));
    }
}
