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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import sv.edu.catolica.timetrack.R;
import sv.edu.catolica.timetrack.Interfaces.OnDialogCloseListener;

public class AddNewTask extends BottomSheetDialogFragment {
    public static final String TAG = "AddNewTask";

    private TextView setDueDate;
    private EditText mTaskEdit;
    private Button mSaveBtn;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String usuarioId;
    private Context context;
    private String dueDate = "";
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
        mSaveBtn = view.findViewById(R.id.save_btn);

        firestore = FirebaseFirestore.getInstance();

        // para identificar si se pretende actualizar elementos en firestore
        boolean isUpdate = false;
        final Bundle bundle = getArguments();
        if (bundle != null) {
            isUpdate = true;
            String task = bundle.getString("task");
            id = bundle.getString("id");
            dueDateUpdate = bundle.getString("due");

            mTaskEdit.setText(task);
            setDueDate.setText(dueDateUpdate);

            if (task.length() > 0) {  // validacion para no actualizar el mismo texto
                mSaveBtn.setEnabled(false);
                mSaveBtn.setBackgroundColor(Color.GRAY);
            }
        }

        // Comprueba que no se intente crear una tarea con el titulo vacio
        mTaskEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence c, int i, int i1, int i2) {
                if(c.toString().trim().equals("")) {
                    mSaveBtn.setEnabled(false);
                    mSaveBtn.setBackgroundColor(Color.GRAY);
                } else {
                    mSaveBtn.setEnabled(true);
                    mSaveBtn.setBackgroundColor(getResources().getColor(R.color.lavender));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

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
                    }
                }, YEAR, MONTH, DATE);

                datePickerDialog.show();
            }
        });

        boolean finalIsUpdate = isUpdate;
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
                            "due", dueDate
                    );
                    Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                } else {  // para crear la tarea
                    if (task.isEmpty()) {
                        Toast.makeText(context, "No se permiten actividades en blanco", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> taskMap = new HashMap<>();

                        taskMap.put("task", task);
                        taskMap.put("due", dueDate);
                        taskMap.put("status", 0);
                        taskMap.put("time", FieldValue.serverTimestamp());

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
