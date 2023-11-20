package sv.edu.catolica.timetrack.Class;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.C;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import sv.edu.catolica.timetrack.R;
import sv.edu.catolica.timetrack.Interfaces.OnDialogCloseListener;

public class AddNewTask extends BottomSheetDialogFragment {
    public static final String TAG = "AddNewTask";

    private TextView setDueDate;
    private EditText mTaskEdit;
    private Spinner mSpTipo;
    private String opcionElegida = "";
    private Button mSaveBtn;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String usuarioId;
    private Context context;
    private String dueDate = "";
    private Timestamp limitDate; // timestamp de firebase equivalente a la fecha elegida por el datepickerdialog
    private Timestamp limitDateUpdate;
    private String id = "";
    private String dueDateUpdate = "";

    public static AddNewTask newInstance() {
        return new AddNewTask();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_new_task, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDueDate = view.findViewById(R.id.tv_set_due);
        mTaskEdit = view.findViewById(R.id.et_task);
        mSpTipo = view.findViewById(R.id.spTipo);
        mSaveBtn = view.findViewById(R.id.save_btn);

        // Array para popular el spinner
        List<String> actividadesTipos = new ArrayList<>();
        actividadesTipos.add("Examen escrito");
        actividadesTipos.add("Guía de estudios");
        actividadesTipos.add("Exposición");
        actividadesTipos.add("Actividad práctica");
        actividadesTipos.add("Otras actividades");

        // Se creay un adaptador para el spinner y se elige el layout a utilizar
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, actividadesTipos);


        // Obteniendo instancia de Firestore
        firestore = FirebaseFirestore.getInstance();


        // para identificar si se pretende actualizar elementos en firestore
        boolean isUpdate = false;
        final Bundle bundle = getArguments();
        if (bundle != null) {
            isUpdate = true;
            String task = bundle.getString("task");
            id = bundle.getString("id");
            dueDateUpdate = bundle.getString("due");
            opcionElegida = bundle.getString("type");
            // Reconstruyendo el timestamp
            long limitDateSec = bundle.getLong("limitDateSec");
            int limitDateNano = bundle.getInt("limitDateNano");
            limitDateUpdate = new Timestamp(limitDateSec, limitDateNano);

            mSpTipo.setSelection(actividadesTipos.indexOf(opcionElegida));
            mTaskEdit.setText(task);
            setDueDate.setText("Agendado para: " + dueDateUpdate);

//            selectedOptionUpdate = actividadesTipos.get()

//          TODO: Descomentar esto antes de version final
//            if (task.length() > 0) {  // validacion para no actualizar el mismo texto
//                mSaveBtn.setEnabled(false);
//                mSaveBtn.setBackgroundColor(Color.GRAY);
//            }
        }

        // Especifica el layout a utilizar cuando se despliega el listado de opciones
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Aplica el adapter al spinner
        mSpTipo.setAdapter(adapterSpinner);

        // Captura el valor seleccionado del Spinner
        boolean finalIsUpdate1 = isUpdate;
        mSpTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                opcionElegida = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {  }
        });

        // Comprueba que no se intente crear una tarea con el titulo vacio
        mTaskEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence c, int i, int i1, int i2) {
                if(c.toString().trim().equals("")) {
                    mSaveBtn.setEnabled(false);
                    mSaveBtn.setBackgroundColor(Color.GRAY);
                }
                else {
                    if (!dueDate.isEmpty()) {


                        mSaveBtn.setEnabled(true);
                        mSaveBtn.setBackgroundColor(getResources().getColor(R.color.lavender));
                    }
//                    mSaveBtn.setEnabled(true);
//                    mSaveBtn.setBackgroundColor(getResources().getColor(R.color.lavender));
                }
//                if (!dueDate.isEmpty()) {
//                    mSaveBtn.setEnabled(true);
//                    mSaveBtn.setBackgroundColor(getResources().getColor(R.color.lavender));
//                }
            }

            @Override
            public void afterTextChanged(Editable editable) {  }
        });

        // para poder hacer actualizaciones
        boolean finalIsUpdate = isUpdate;

        // actualizando la fecha
        if (finalIsUpdate) {
            dueDate = dueDateUpdate;
            limitDate = limitDateUpdate;
        }

        setDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();

                int MONTH = calendar.get(Calendar.MONTH);
                int YEAR = calendar.get(Calendar.YEAR);
                int DATE = calendar.get(Calendar.DATE);

                DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month += 1;
                        setDueDate.setText(dayOfMonth + "/" + month + "/" + year);
                        dueDate = dayOfMonth + "/" + month + "/" + year;

                        // habilitando el boton de aniadir
                        if (!mTaskEdit.getText().toString().isEmpty()) {
                            mSaveBtn.setEnabled(true);
                            mSaveBtn.setBackgroundColor(requireActivity().getResources().getColor(R.color.lavender));
                        }

                        // Capturan y convirtiendo la fecha seleccionada en un valor timestamp de firestore
                        calendar.set(year, month - 1, dayOfMonth);
                        limitDate = new Timestamp(calendar.getTime());
                    }
                }, YEAR, MONTH, DATE);

                datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

                datePickerDialog.show();
            }
        });

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // obteniendo el id del usuario para identificar su db
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                usuarioId = currentUser.getUid();

                String task = mTaskEdit.getText().toString();

                if (finalIsUpdate) { // para actualizar la tarea
                    firestore.collection(usuarioId).document(id).update(
                            "task", task,
                            "due", dueDate,
                            "type", opcionElegida,
                            "limitDate", limitDate
                    );
                    Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                } else {  // para crear la tarea
                    if (task.isEmpty()) {
                        Toast.makeText(context, "No se permiten actividades en blanco", Toast.LENGTH_SHORT).show();
                    } else {
                        // generando un entero del instante, a manera de id, para identificar su respectiva notificacion AlarmManager
                        // Obtener la fecha y hora actual del sistema
                        Calendar calendario = Calendar.getInstance();

                        // Obtener el año, mes, día, hora, minutos y segundos
                        int año = calendario.get(Calendar.YEAR);
                        int mes = calendario.get(Calendar.MONTH) + 1; // Los meses empiezan en 0, por eso se suma 1
                        int dia = calendario.get(Calendar.DAY_OF_MONTH);
                        int hora = calendario.get(Calendar.HOUR_OF_DAY);
                        int minutos = calendario.get(Calendar.MINUTE);
                        int segundos = calendario.get(Calendar.SECOND);

                        // Combinar los valores en un solo entero (formato AAAAMMDDHHmmss)
                        int dateTimeEntero = año * 100000000 + mes * 1000000 + dia * 10000 + hora * 100 + minutos * 1 + segundos;


                        // Estableiendo los campos y valores del documento a crear
                        Map<String, Object> taskMap = new HashMap<>();

                        taskMap.put("task", task);
                        taskMap.put("due", dueDate);
                        taskMap.put("status", 0);
                        taskMap.put("type", opcionElegida);
                        taskMap.put("reminder", "");
                        taskMap.put("limitDate", limitDate);
                        taskMap.put("idInt", dateTimeEntero);


                        firestore.collection(usuarioId).add(taskMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(context, "Tarea guardada", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                dismiss();
            }
        });

        mSaveBtn.setEnabled(false);
        mSaveBtn.setBackgroundColor(Color.GRAY);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment activity = getParentFragment();

        if (activity instanceof OnDialogCloseListener) {
            ((OnDialogCloseListener)activity).onDialogClose(dialog);
        }
    }
}
