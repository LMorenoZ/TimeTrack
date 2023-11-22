package sv.edu.catolica.timetrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private HomeFragment frag;
    private TextView correoUsuario;
    private FirebaseAuth mAuth;
    private ImageView profilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_main);

        // obteniendo el id del usuario activo para identificar su db
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Colocando el correo del usuario activo y la fecha
        try {
            // Correo
            View headerLayout = navigationView.getHeaderView(0);
            correoUsuario = headerLayout.findViewById(R.id.tvBienvenida);

            mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();
            correoUsuario.setText(currentUser.getEmail());

            // Imagen de perfil
            profilePic = headerLayout.findViewById(R.id.userPic);
            String photoUrl = currentUser.getPhotoUrl().toString();

            if (photoUrl != null) {

                // Ajustando el tamanio de la imagen y pasarsela al imageView por medio de la Biblioteca Glide.
                Glide.with(this)
                        .load(photoUrl)
                        .override(175, 175)
                        .transform(new CircleCrop())
                        .into(profilePic);
            }

            // Fecha
            String fechaActual = obtenerFechaActual();
            TextView textViewFecha = headerLayout.findViewById(R.id.tvCorreo);
            textViewFecha.setText(fechaActual);

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            frag = new HomeFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, frag).commit();

            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private String obtenerFechaActual() {
        // Obtiene la fecha actual
        Calendar calendar = Calendar.getInstance();

        // Define el formato deseado ("EEEE d 'de' MMMM 'de' yyyy")
        SimpleDateFormat formatoFecha;
        String idioma = Locale.getDefault().getLanguage();
        switch (idioma) {
            case "es":
                formatoFecha = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale.getDefault()); // formato para español
                break;
            case "en":
                formatoFecha = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()); // formato para ingles
                break;
            case "pt":
                formatoFecha = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault()); // formato para portugues
                break;
            default:
                formatoFecha = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale.getDefault()); // formato para otros idiomas
                break;
        }

        // Formatea la fecha según el patrón
        String fechaEnString = formatoFecha.format(calendar.getTime());
        fechaEnString = Character.toUpperCase(fechaEnString.charAt(0)) + fechaEnString.substring(1);
        return fechaEnString;
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
         int itemId = item.getItemId();

        try {
            if (itemId == R.id.nav_home) {
                if (frag != null) {
                    // recupera el fragment del viewpager previamente seleccionado, ya sea en "Pendientes" o "Completadas"
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, frag).commit();
                } else {
                    // crea de nuevo el fragment del viewpager y lo asigna al fragment "pendiente"
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                }
            } else if (itemId == R.id.nav_settings) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new RecordatoriosFragment()).commit();
            } else if (itemId == R.id.nav_share) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SesionFragment()).commit();
            } else if (itemId == R.id.nav_about) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AboutFragment()).commit();
            } else if (itemId == R.id.nav_logout) {
                // Dialogo de confirmacion
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.cerrar_sesi_n);
                builder.setMessage(R.string.est_seguro_que_desea_salir);

                builder.setPositiveButton(R.string.confirmar, (dialog, which) -> {
                    logout();
                });

                builder.setNegativeButton(R.string.cancelar_logout, (dialog, which) -> { });

                AlertDialog dialog = builder.create();
                dialog.show();

            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            irLogin();
        }
    }

    private void logout() {
        mAuth.signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut();

        irLogin();
    }

    private void irLogin() {
        Intent intent = new Intent(MainActivity.this, SigninActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}