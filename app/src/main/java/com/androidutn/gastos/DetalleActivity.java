package com.androidutn.gastos;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.androidutn.gastos.model.Movimiento;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetalleActivity extends AppCompatActivity {

    public static final String EXTRA_MES = "mes";

    private static final SimpleDateFormat MES_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat MES_DB_FORMAT = new SimpleDateFormat("yyyyMM", Locale.getDefault());

    @BindView(R.id.mes_text) TextView mMes;

    @BindView(R.id.detalle_lista) RecyclerView mLista;
    private MovimientoAdapter adapter;

    private Calendar mesActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle);

        ButterKnife.bind(this);

        mLista.setLayoutManager(new LinearLayoutManager(this));
        mLista.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        adapter = new MovimientoAdapter();
        mLista.setAdapter(adapter);

        mesActual = Calendar.getInstance();

        long mes = getIntent().getLongExtra(EXTRA_MES, 0);
        if (mes != 0) {
            mesActual.setTimeInMillis(mes);
        }

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
        Calendar c = Calendar.getInstance();
        c.setTime(mesActual.getTime());

        c.set(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long inicioMes = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        long finMes = c.getTimeInMillis();

        FirebaseDatabase.getInstance().getReference("movimientos")
                .orderByChild("fecha").startAt(inicioMes).endAt(finMes).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Movimiento> movimientos = new ArrayList<>();
                for (DataSnapshot m : dataSnapshot.getChildren()) {
                    Movimiento mov = m.getValue(Movimiento.class);
                    movimientos.add(mov);
                }
                adapter.setDatos(movimientos);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
}
