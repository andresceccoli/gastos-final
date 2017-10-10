package com.androidutn.gastos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.androidutn.gastos.model.Resumen;
import com.androidutn.gastos.model.ResumenCategoria;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.PieChartView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.mes_text) TextView mMes;
    @BindView(R.id.egresos_text) TextView mEgresos;
    @BindView(R.id.ingresos_text) TextView mIngresos;
    @BindView(R.id.saldo_text) TextView mSaldo;
    @BindView(R.id.grafico_egresos) PieChartView mGraficoEgresos;
    @BindView(R.id.grafico_ingresos) PieChartView mGraficoIngresos;

    private static final SimpleDateFormat MES_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat MES_DB_FORMAT = new SimpleDateFormat("yyyyMM", Locale.getDefault());
    private Calendar mesActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        mGraficoEgresos.setCircleFillRatio(0.7f);
        mGraficoIngresos.setCircleFillRatio(0.7f);

        mesActual = Calendar.getInstance();
        mostrarMes();
    }

    @Override
    protected void onStart() {
        super.onStart();

        cargarValores();
    }

    private void mostrarMes() {
        mMes.setText(MES_FORMAT.format(mesActual.getTime()));
    }

    private void cargarValores() {
        FirebaseDatabase.getInstance().getReference("resumen").child(MES_DB_FORMAT.format(mesActual.getTime()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Resumen resumen = dataSnapshot.getValue(Resumen.class);
                        if (resumen != null) {
                            mEgresos.setText(NumberFormat.getCurrencyInstance().format(resumen.getTotalEgresos()));
                            mIngresos.setText(NumberFormat.getCurrencyInstance().format(resumen.getTotalIngresos()));
                            mSaldo.setText(NumberFormat.getCurrencyInstance().format(resumen.getTotalIngresos() - resumen.getTotalEgresos()));
                        } else {
                            mEgresos.setText("--");
                            mIngresos.setText("--");
                            mSaldo.setText("--");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        FirebaseDatabase.getInstance().getReference("resumen_egresos").child(MES_DB_FORMAT.format(mesActual.getTime()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mostrarGrafico(mGraficoEgresos, dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        FirebaseDatabase.getInstance().getReference("resumen_ingresos").child(MES_DB_FORMAT.format(mesActual.getTime()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mostrarGrafico(mGraficoIngresos, dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void mostrarGrafico(PieChartView grafico, DataSnapshot dataSnapshot) {
        List<SliceValue> values = new ArrayList<>();
        for (DataSnapshot cat : dataSnapshot.getChildren()) {
            ResumenCategoria res = cat.getValue(ResumenCategoria.class);
            values.add(new SliceValue((float) res.getTotal(), ChartUtils.nextColor()).setLabel(res.getNombre()));
        }
        PieChartData data = new PieChartData(values);
        data.setHasLabels(true).setHasLabelsOutside(true).setHasLabelsOnlyForSelected(false);
        grafico.setPieChartData(data);
    }

    @OnClick(R.id.mes_anterior)
    public void onMesAnterior() {
        mesActual.add(Calendar.MONTH, -1);
        mostrarMes();
        cargarValores();
    }

    @OnClick(R.id.mes_siguiente)
    public void onMesSiguiente() {
        mesActual.add(Calendar.MONTH, 1);
        mostrarMes();
        cargarValores();
    }

    @OnClick(R.id.nuevo_ingreso)
    public void onNuevoIngreso() {
        Intent i = new Intent(this, MovimientoActivity.class);
        i.putExtra(MovimientoActivity.EXTRA_INGRESO, true);
        startActivity(i);
    }

    @OnClick(R.id.nuevo_egreso)
    public void onNuevoEgreso() {
        Intent i = new Intent(this, MovimientoActivity.class);
        i.putExtra(MovimientoActivity.EXTRA_INGRESO, false);
        startActivity(i);
    }

    @OnClick({R.id.resumen_mes, R.id.egresos_mes, R.id.ingresos_mes})
    public void onDetalleMes() {
        Intent i = new Intent(this, DetalleActivity.class);
        i.putExtra(DetalleActivity.EXTRA_MES, mesActual.getTimeInMillis());
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
