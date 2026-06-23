package com.example.todolist.fragments;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.util.LocationHelper;
import com.example.todolist.util.LocationHelper.LocationInfo;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationFragment extends Fragment {

    private TextView tvAddress, tvCoords, tvAccuracy, tvStatus;
    private Button btnLocate;
    private RecyclerView recyclerHistory;
    private LocationHistoryAdapter historyAdapter;
    private final List<LocationInfo> historyList = new ArrayList<>();
    private LocationHelper locationHelper;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                requestLocation();
            } else {
                Context ctx = getContext();
                if (ctx != null) {
                    Toast.makeText(ctx, "位置权限被拒绝，无法获取位置", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_location, container, false);

        tvAddress = v.findViewById(R.id.tv_location_address);
        tvCoords = v.findViewById(R.id.tv_location_coords);
        tvAccuracy = v.findViewById(R.id.tv_location_accuracy);
        tvStatus = v.findViewById(R.id.tv_tracking_status);
        btnLocate = v.findViewById(R.id.btn_locate);
        recyclerHistory = v.findViewById(R.id.recycler_location_history);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new LocationHistoryAdapter();
        recyclerHistory.setAdapter(historyAdapter);

        locationHelper = LocationHelper.getInstance(requireContext());

        // Check Play Services
        if (!LocationHelper.isPlayServicesAvailable(requireContext())) {
            tvAddress.setText("Google Play Services 不可用");
            tvCoords.setText("位置功能需要 Google Play Services");
            btnLocate.setEnabled(false);
            return v;
        }

        // Observe location updates
        locationHelper.getLocationLiveData().observe(getViewLifecycleOwner(), info -> {
            if (info == null) return;
            if (info.isError) {
                tvAddress.setText(info.errorMessage);
                tvCoords.setText("");
                tvAccuracy.setText("");
                btnLocate.setEnabled(true);
                return;
            }
            updateLocationDisplay(info);
            addToHistory(info);
        });

        btnLocate.setOnClickListener(view -> {
            Context ctx = getContext();
            if (ctx == null) return;
            if (!LocationHelper.hasPermission(ctx)) {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                requestLocation();
            }
        });

        return v;
    }

    private void requestLocation() {
        btnLocate.setEnabled(false);
        tvStatus.setText("● 正在获取位置...");
        locationHelper.requestSingleLocation(requireContext());
    }

    private void updateLocationDisplay(LocationInfo info) {
        if (info.isEmpty) {
            // Status-only message (locating, completed, etc.)
            tvStatus.setText(info.addressLine);
            // Enable button when not actively locating
            if (!info.addressLine.contains("定位")) {
                btnLocate.setEnabled(true);
            }
            return;
        }
        tvAddress.setText(info.addressLine);
        tvCoords.setText(String.format(Locale.US, "纬度: %.6f  经度: %.6f", info.latitude, info.longitude));
        tvAccuracy.setText(String.format(Locale.US, "精度: %.0f 米", info.accuracy));
        btnLocate.setEnabled(true);
    }

    private void addToHistory(LocationInfo info) {
        if (info.isEmpty || info.isError) return;
        // Avoid consecutive duplicate entries
        if (!historyList.isEmpty()) {
            LocationInfo last = historyList.get(0);
            if (last.latitude == info.latitude && last.longitude == info.longitude) return;
        }
        historyList.add(0, info);
        if (historyList.size() > 50) {
            historyList.remove(historyList.size() - 1);
        }
        historyAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationHelper != null && locationHelper.isTracking()) {
            locationHelper.stopTracking();
        }
    }

    // --- Inner adapter ---
    private class LocationHistoryAdapter extends RecyclerView.Adapter<LocationHistoryAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_location_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LocationInfo info = historyList.get(position);
            holder.tvAddr.setText(info.addressLine);
            holder.tvCoords.setText(String.format(Locale.US, "%.5f, %.5f", info.latitude, info.longitude));
            holder.tvTime.setText(timeFormat.format(new Date(info.timestamp)));
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAddr, tvCoords, tvTime;
            VH(@NonNull View itemView) {
                super(itemView);
                tvAddr = itemView.findViewById(R.id.tv_history_addr);
                tvCoords = itemView.findViewById(R.id.tv_history_coords);
                tvTime = itemView.findViewById(R.id.tv_history_time);
            }
        }
    }
}
