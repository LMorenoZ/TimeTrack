package sv.edu.catolica.timetrack;

import android.app.Dialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SesionFragment extends Fragment {
    private int timeSelected = 0;
    private CountDownTimer timeCountDown = null;
    private int timeProgress = 0;
    private long pauseOffSet = 0;
    private boolean isStart = true;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Configurando los botones de la interfaz
        TextView tvTimeLeft = getView().findViewById(R.id.tvTimeLeft);
        tvTimeLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTimeFunction();
            }
        });

        Button startBtn = getView().findViewById(R.id.btnPlayPause);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTimerSetup();
            }
        });

        Button resetBtn = getView().findViewById(R.id.btnReset);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTime();
            }
        });

        TextView addTimeTv = getView().findViewById(R.id.tvAddTime);
        addTimeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addExtraTime();
            }
        });
    }

    // metodos
    private void addExtraTime() {
        ProgressBar progressBar = getView().findViewById(R.id.pbTimer);

        if (timeSelected != 0) {
            timeSelected += 60;
            progressBar.setMax(timeSelected);

            timePause();
            startTimer(pauseOffSet);

            Toast.makeText(getContext(), R.string._1_minuto_a_adido, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetTime() {
        try {
            if (timeCountDown != null) {
                timeCountDown.cancel();
                timeProgress = 0;
                timeSelected = 0;
                pauseOffSet = 0;
                timeCountDown = null;

                Button startBtn = getView().findViewById(R.id.btnPlayPause);
                startBtn.setText(R.string.comenzar);
                isStart = true;

                ProgressBar progressBar = getView().findViewById(R.id.pbTimer);
                progressBar.setProgress(0);

                TextView timeLeftTv = getView().findViewById(R.id.tvTimeLeft);
                timeLeftTv.setText("00:00");

                TextView addTimeTv = getView().findViewById(R.id.tvAddTime);
                addTimeTv.setEnabled(false);
                addTimeTv.setTextColor(requireActivity().getResources().getColor(R.color.lavender));
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void timePause() {
        if (timeCountDown != null) {
            TextView addTimeTv = getView().findViewById(R.id.tvAddTime);
            addTimeTv.setEnabled(false);
            addTimeTv.setTextColor(Color.GRAY);

            timeCountDown.cancel();
        } else {
            TextView addTimeTv = getView().findViewById(R.id.tvAddTime);
            addTimeTv.setEnabled(false);
            addTimeTv.setTextColor(requireActivity().getResources().getColor(R.color.lavender));
        }
    }

    private void startTimerSetup() {
        Button startBtn = getView().findViewById(R.id.btnPlayPause);

        if (timeSelected > timeProgress) {
            if (isStart) {
                startBtn.setText(R.string.pausar);
                startTimer(pauseOffSet);
                isStart = false;
            } else {
                isStart = true;
                startBtn.setText(R.string.reanudar);
                timePause();
            }
        } else {
            Toast.makeText(getContext(), R.string.ingrese_un_tiempo, Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer(long pauseOffSetL) {
        ProgressBar progressBar = getView().findViewById(R.id.pbTimer);
        progressBar.setProgress(timeProgress);

            timeCountDown = new CountDownTimer( ((long) (timeSelected * 1000)) - pauseOffSetL * 1000, 1000 ) {
            @Override
            public void onTick(long p0) {
                timeProgress++;
                pauseOffSet = ((long) timeSelected) - p0/1000;
                int minutos = (int) (p0 / 1000) / 60;
                int segundos = (int) (p0 / 1000) % 60;
                progressBar.setProgress(timeSelected - timeProgress);

                TextView timeLeftTv = getView().findViewById(R.id.tvTimeLeft);
                timeLeftTv.setText(String.valueOf(String.format("%02d:%02d", minutos, segundos)));  // texto mostrando tiempo restante
            }

            @Override
            public void onFinish() {
                resetTime();
                Toast.makeText(getContext(), R.string.finaliz, Toast.LENGTH_SHORT).show();
                MediaPlayer sonido = MediaPlayer.create(getContext(), R.raw.alarm);

                if (sonido.isPlaying()) {
                    sonido.stop();
                } else {
                    try {
                        sonido.start();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        timeCountDown.start();
    }

    private void setTimeFunction() {
        Dialog timeDialog  = new Dialog(getContext());
        timeDialog.setContentView(R.layout.add_dialog);
        EditText timeSet = timeDialog.findViewById(R.id.etGetTime);
        TextView timeLeftTv = getView().findViewById(R.id.tvTimeLeft);
        Button btnStart = getView().findViewById(R.id.btnPlayPause);
        ProgressBar progressBar = getView().findViewById(R.id.pbTimer);

        timeDialog.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timeSet.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), R.string.txt_ingrese_un_tiempo, Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        resetTime();
                        int minutos = Integer.parseInt(timeSet.getText().toString());
                        timeLeftTv.setText(String.format("%d:%02d", minutos, 0));
                        btnStart.setText(R.string.txt_comenzar);
                        timeSelected = Integer.parseInt(timeSet.getText().toString()) * 60;
                        progressBar.setMax(timeSelected);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                timeDialog.dismiss();
            }
        });

        timeDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (timeCountDown != null) {
            timeCountDown.cancel();
            timeProgress = 0;
        }
    }

}