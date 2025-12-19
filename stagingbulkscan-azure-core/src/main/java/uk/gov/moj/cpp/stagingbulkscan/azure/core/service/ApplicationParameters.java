package uk.gov.moj.cpp.stagingbulkscan.azure.core.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ApplicationParameters {

    @Inject
    @Value(key = "storageConnectionString")
    private String storageConnectionString;

    @Inject
    @Value(key = "scanManagerStorageConnectionString")
    private String scanManagerStorageConnectionString;

    @Inject
    @Value(key = "azureScanManagerContainerName")
    private String azureScanManagerContainerName;

    @Inject
    @Value(key = "scanManagerContainerName")
    private String scanManagerContainerName;

    @Inject
    @Value(key = "stagingBulkScanEventProcessorSchedulerIntervalMillis")
    private String stagingBulkScanEventProcessorSchedulerIntervalMillis;

    @Inject
    @Value(key = "deleteAfterActionedDays")
    private String deleteAfterActionedDays;

    public String getStorageConnectionString() {
        return storageConnectionString;
    }

    public String getAzureScanManagerContainerName() {
        return azureScanManagerContainerName;
    }

    public String getStagingBulkScanEventProcessorSchedulerIntervalMillis() {
        return stagingBulkScanEventProcessorSchedulerIntervalMillis;
    }

    public String getDeleteAfterActionedDays() {
        return deleteAfterActionedDays;
    }

    public String getScanManagerStorageConnectionString() {
        return scanManagerStorageConnectionString;
    }

    public String getScanManagerContainerName() {
        return scanManagerContainerName;
    }

}
