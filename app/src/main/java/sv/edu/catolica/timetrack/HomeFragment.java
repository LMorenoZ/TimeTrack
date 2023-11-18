package sv.edu.catolica.timetrack;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import sv.edu.catolica.timetrack.Adapter.VPAdapter;

public class HomeFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tablayout);
        viewPager = view.findViewById(R.id.viewpager);

        tabLayout.setupWithViewPager(viewPager);

        VPAdapter vpAdapter = new VPAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpAdapter.addFragment(new PendientesFragment(), "Pendientes");
        vpAdapter.addFragment(new CompletadasFragment(), "Completadas");
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(vpAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

//        View view = inflater.inflate(R.layout.fragment_home, container, false);
//
//        tabLayout = view.findViewById(R.id.tablayout);
//        viewPager = view.findViewById(R.id.viewpager);
//
//        tabLayout.setupWithViewPager(viewPager);
//
//        VPAdapter vpAdapter = new VPAdapter(getParentFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
//        vpAdapter.addFragment(new PendientesFragment(), "Pendientes");
//        vpAdapter.addFragment(new CompletadasFragment(), "Completadas");
//        viewPager.setAdapter(vpAdapter);
//        return view;

        return inflater.inflate(R.layout.fragment_home, container, false);
    }
}