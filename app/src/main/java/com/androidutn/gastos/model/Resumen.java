package com.androidutn.gastos.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by andres on 9/27/17.
 */

public class Resumen {
    private static final SimpleDateFormat MES_FORMAT = new SimpleDateFormat("yyyyMM", Locale.getDefault());

    private double totalIngresos;
    private double totalEgresos;

    public static String generarMesKey(Date fecha) {
        return MES_FORMAT.format(fecha);
    }

    public double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }
}
