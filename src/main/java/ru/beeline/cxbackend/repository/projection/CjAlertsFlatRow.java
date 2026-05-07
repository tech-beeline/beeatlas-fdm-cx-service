package ru.beeline.cxbackend.repository.projection;

public interface CjAlertsFlatRow {
    Long getCjId();
    String getCjName();
    String getCjUniqueIdent();
    String getCjDashboardLink();

    Long getBiId();
    String getBiUniqueIdent();
    String getBiName();
    String getBiDescr();

    Integer getBsIdStepType();
    String getBsUniqueIdent();
    String getBsName();
    Float getBsLatency();
    Float getBsRps();
    Float getBsErrorRate();
    Integer getBsId();
}

