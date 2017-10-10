package com.androidutn.gastos;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.androidutn.gastos.model.Categoria;
import com.androidutn.gastos.model.Movimiento;
import com.androidutn.gastos.model.Resumen;
import com.androidutn.gastos.model.ResumenCategoria;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.blackbox_vision.datetimepickeredittext.view.DatePickerInputEditText;
import io.blackbox_vision.datetimepickeredittext.view.TimePickerInputEditText;

public class MovimientoActivity extends AppCompatActivity {

    public static final String EXTRA_INGRESO = "ingreso";

    @BindView(R.id.mov_categoria) Spinner mCategoria;
    @BindView(R.id.mov_monto_il) TextInputLayout mMontoInput;
    @BindView(R.id.mov_descripcion_il) TextInputLayout mDescripcionInput;
    @BindView(R.id.mov_fecha_il) TextInputLayout mFechaInput;
    @BindView(R.id.mov_fecha) DatePickerInputEditText mFecha;
    @BindView(R.id.mov_hora) TimePickerInputEditText mHora;

    private boolean ingreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movimiento);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ButterKnife.bind(this);

        Calendar c = Calendar.getInstance();
        mFecha.onDateSet(null, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE));
        mHora.onTimeSet(null, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));

        ingreso = getIntent().getBooleanExtra(EXTRA_INGRESO, false);

        getSupportActionBar().setTitle(ingreso ? R.string.nuevo_ingreso : R.string.nuevo_egreso);

        mFecha.setManager(getSupportFragmentManager());
        mHora.setManager(getSupportFragmentManager());

        cargarCategorias();
    }

    private void cargarCategorias() {
        FirebaseDatabase.getInstance().getReference(ingreso ? "categorias_ingresos" : "categorias_egresos")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Categoria> categorias = new ArrayList<>();
                        for (DataSnapshot cat : dataSnapshot.getChildren()) {
                            Categoria categoria = cat.getValue(Categoria.class);
                            categorias.add(categoria);
                        }

                        mCategoria.setAdapter(new ArrayAdapter<>(MovimientoActivity.this, android.R.layout.simple_list_item_1, categorias));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @OnClick(R.id.aceptar)
    public void onAceptar() {
        if (TextUtils.isEmpty(mMontoInput.getEditText().getText())) {
            mMontoInput.setErrorEnabled(true);
            mMontoInput.setError(getString(R.string.monto_error));
            return;
        } else {
            mMontoInput.setErrorEnabled(false);
            mMontoInput.setError(null);
        }

        final Categoria categoria = (Categoria) mCategoria.getSelectedItem();
        final Movimiento mov = new Movimiento();
        mov.setCategoriaKey(categoria.getId());
        mov.setCategoriaNombre(categoria.getNombre());
        mov.setDescripcion(mDescripcionInput.getEditText().getText().toString());
        mov.setIngreso(ingreso);
        mov.setMonto(Double.parseDouble(mMontoInput.getEditText().getText().toString()));

        final Calendar c = mFecha.getDate();
        c.set(Calendar.HOUR_OF_DAY, mHora.getTime().get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, mHora.getTime().get(Calendar.MINUTE));
        mov.setFecha(c.getTimeInMillis());
        mov.setFechaRev(-mov.getFecha());

        DatabaseReference movDb = FirebaseDatabase.getInstance().getReference("movimientos");
        String key = movDb.push().getKey();
        movDb.child(key).setValue(mov, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                actualizarResumen(c, mov, categoria);
            }
        });
    }

    private void actualizarResumen(Calendar c, final Movimiento mov, final Categoria categoria) {
        final String mesKey = Resumen.generarMesKey(c.getTime());
        final DatabaseReference resumenDb = FirebaseDatabase.getInstance().getReference("resumen");
        resumenDb.child(mesKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Resumen resumen = dataSnapshot.getValue(Resumen.class);
                if (resumen == null)
                    resumen = new Resumen();
                if (ingreso)
                    resumen.setTotalIngresos(resumen.getTotalIngresos() + mov.getMonto());
                else
                    resumen.setTotalEgresos(resumen.getTotalEgresos() + mov.getMonto());

                resumenDb.child(mesKey).setValue(resumen, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        actualizarResumenCategorias(mesKey, categoria, mov);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void actualizarResumenCategorias(final String mesKey, final Categoria categoria, final Movimiento mov) {
        final DatabaseReference resumenCatDb = FirebaseDatabase.getInstance().getReference(ingreso ? "resumen_ingresos" : "resumen_egresos");
        resumenCatDb.child(mesKey).child(categoria.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ResumenCategoria resumenCat = dataSnapshot.getValue(ResumenCategoria.class);
                if (resumenCat == null)
                    resumenCat = new ResumenCategoria();

                resumenCat.setNombre(categoria.getNombre());
                resumenCat.setTotal(resumenCat.getTotal() + mov.getMonto());

                resumenCatDb.child(mesKey).child(categoria.getId()).setValue(resumenCat, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        finish();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @OnClick(R.id.cancelar)
    public void onCancelar() {
        finish();
    }

    @OnClick(R.id.nueva_categoria)
    public void onNuevaCategoria() {
        final EditText nombre = new EditText(this);
        nombre.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nombre.setHint(R.string.nombre);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.nueva_categoria).setView(nombre).setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!TextUtils.isEmpty(nombre.getText())) {
                    crearCategoria(nombre.getText().toString());
                    dialogInterface.dismiss();
                }
            }
        }).setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).show();
    }

    private void crearCategoria(String nombre) {
        Categoria cat = new Categoria();
        cat.setNombre(nombre);

        DatabaseReference categorias = FirebaseDatabase.getInstance().getReference(ingreso ? "categorias_ingresos" : "categorias_egresos");
        String id = categorias.push().getKey();
        cat.setId(id);

        categorias.child(id).setValue(cat, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                cargarCategorias();
            }
        });
    }

}
