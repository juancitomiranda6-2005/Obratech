package com.obratech.service;

public final class ProjectRiskPolicy {

    public static final int CRONOGRAMA_WEIGHT = 40;
    public static final int EQUIPOS_WEIGHT = 25;
    public static final int CLIMA_WEIGHT = 20;
    public static final int PAGOS_APROBACIONES_WEIGHT = 15;
    public static final double HIGH_RAIN_PROBABILITY = 50.0;
    public static final double MEDIUM_RISK_MAX = 60.0;
    public static final double LOW_RISK_MAX = 30.0;
    public static final int STALE_DAYS = 7;

    private ProjectRiskPolicy() {}
}
