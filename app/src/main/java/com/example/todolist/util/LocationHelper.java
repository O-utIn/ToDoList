package com.example.todolist.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private static final long UPDATE_INTERVAL_MS = 10000;
    private static final long FASTEST_INTERVAL_MS = 5000;

    private static volatile LocationHelper INSTANCE;
    private static boolean playServicesChecked = false;
    private static boolean playServicesAvailable = false;

    private final FusedLocationProviderClient fusedClient;
    private final MutableLiveData<LocationInfo> locationLiveData = new MutableLiveData<>();
    private final ExecutorService geoExec = Executors.newSingleThreadExecutor();
    private LocationCallback locationCallback;
    private volatile boolean isTracking = false;

    private LocationHelper(Context context) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
    }

    public static LocationHelper getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocationHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LocationHelper(context);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<LocationInfo> getLocationLiveData() {
        return locationLiveData;
    }

    /**
     * Check whether Google Play Services is available on this device.
     * Must be called before any location operations.
     */
    public static boolean isPlayServicesAvailable(Context context) {
        if (!playServicesChecked) {
            int result = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context.getApplicationContext());
            playServicesAvailable = (result == ConnectionResult.SUCCESS);
            playServicesChecked = true;
            if (!playServicesAvailable) {
                Log.w(TAG, "Google Play Services not available, code=" + result);
            }
        }
        return playServicesAvailable;
    }

    public static boolean hasPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request a single fresh location update, then automatically stop.
     * Fires a "locating..." status, then the result, then "completed" status.
     * Falls back to last-known location after a 15-second timeout.
     */
    @SuppressLint("MissingPermission")
    public void requestSingleLocation(Context context) {
        if (isTracking) {
            Log.d(TAG, "requestSingleLocation: already in progress, skip");
            return;
        }
        if (!isPlayServicesAvailable(context)) {
            locationLiveData.postValue(LocationInfo.error("Google Play Services 不可用"));
            return;
        }
        if (!hasPermission(context)) {
            locationLiveData.postValue(LocationInfo.error("缺少位置权限"));
            return;
        }

        // Cleanup stale callback
        if (locationCallback != null) {
            try {
                fusedClient.removeLocationUpdates(locationCallback);
            } catch (Exception e) {
                Log.w(TAG, "cleanup stale callback failed", e);
            }
            locationCallback = null;
        }

        isTracking = true;
        locationLiveData.postValue(LocationInfo.locating());

        final Context appCtx = context.getApplicationContext();
        final android.os.Handler timeoutHandler = new android.os.Handler(Looper.getMainLooper());

        // Timeout: fall back to last-known location after 15 seconds
        final Runnable timeoutRunnable = () -> {
            Log.w(TAG, "requestSingleLocation timed out, falling back to last location");
            stopTracking();
            getLastLocation(context);
            locationLiveData.postValue(LocationInfo.status("定位超时，显示上次缓存位置"));
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15_000);

        try {
            LocationRequest request = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 0)
                    .setMinUpdateIntervalMillis(0)
                    .setMaxUpdateDelayMillis(0)
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    try {
                        List<Location> locations = result.getLocations();
                        if (locations.isEmpty()) return;
                        Location loc = locations.get(locations.size() - 1);
                        if (loc == null) return;

                        // Got a fresh location — cancel timeout and auto-stop
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        stopTracking();

                        LocationInfo basic = LocationInfo.fromLocation(loc);
                        locationLiveData.postValue(basic);

                        // Run geocoding on background thread
                        geoExec.execute(() -> {
                            String addr = reverseGeocode(appCtx, loc.getLatitude(), loc.getLongitude());
                            locationLiveData.postValue(basic.withAddress(addr));
                        });

                        locationLiveData.postValue(LocationInfo.status("定位完成"));
                    } catch (Exception e) {
                        Log.e(TAG, "onLocationResult error", e);
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        stopTracking();
                    }
                }
            };

            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Single location request started");
        } catch (SecurityException e) {
            Log.e(TAG, "requestSingleLocation: security exception", e);
            timeoutHandler.removeCallbacks(timeoutRunnable);
            isTracking = false;
            locationLiveData.postValue(LocationInfo.error("缺少位置权限"));
        } catch (Exception e) {
            Log.e(TAG, "requestSingleLocation failed", e);
            timeoutHandler.removeCallbacks(timeoutRunnable);
            isTracking = false;
            locationLiveData.postValue(LocationInfo.error("启动定位失败: " + e.getMessage()));
        }
    }

    public void stopTracking() {
        if (locationCallback != null) {
            try {
                fusedClient.removeLocationUpdates(locationCallback);
                Log.d(TAG, "removeLocationUpdates succeeded");
            } catch (Exception e) {
                Log.w(TAG, "removeLocationUpdates failed", e);
            }
            locationCallback = null;
        }
        isTracking = false;
        Log.d(TAG, "Location tracking stopped, isTracking=" + isTracking);
    }

    public boolean isTracking() {
        return isTracking;
    }

    /**
     * Get the last known location once.
     */
    @SuppressLint("MissingPermission")
    public void getLastLocation(Context context) {
        if (!isPlayServicesAvailable(context)) {
            locationLiveData.postValue(LocationInfo.error("Google Play Services 不可用"));
            return;
        }
        if (!hasPermission(context)) {
            locationLiveData.postValue(LocationInfo.error("缺少位置权限"));
            return;
        }
        final Context appCtx = context.getApplicationContext();
        try {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            LocationInfo basic = LocationInfo.fromLocation(loc);
                            geoExec.execute(() -> {
                                String addr = reverseGeocode(appCtx, loc.getLatitude(), loc.getLongitude());
                                locationLiveData.postValue(basic.withAddress(addr));
                            });
                        } else {
                            locationLiveData.postValue(LocationInfo.empty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getLastLocation failed", e);
                        locationLiveData.postValue(LocationInfo.error("获取位置失败: " + e.getMessage()));
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "getLastLocation: security exception", e);
            locationLiveData.postValue(LocationInfo.error("缺少位置权限"));
        } catch (Exception e) {
            Log.e(TAG, "getLastLocation error", e);
            locationLiveData.postValue(LocationInfo.error("定位服务异常"));
        }
    }

    // ──────────────────────────────────────────────
    //  Geocoding
    // ──────────────────────────────────────────────

    private static String reverseGeocode(Context appCtx, double lat, double lng) {
        try {
            if (!Geocoder.isPresent()) {
                return String.format(Locale.US, "%.5f, %.5f", lat, lng);
            }
            Geocoder geocoder = new Geocoder(appCtx, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (a.getLocality() != null) sb.append(a.getLocality());
                if (a.getSubLocality() != null) {
                    if (sb.length() > 0) sb.append(" · ");
                    sb.append(a.getSubLocality());
                }
                if (a.getAddressLine(0) != null && sb.length() == 0) {
                    sb.append(a.getAddressLine(0));
                }
                return sb.length() > 0 ? sb.toString()
                        : String.format(Locale.US, "%.5f, %.5f", lat, lng);
            }
        } catch (IOException e) {
            Log.w(TAG, "Geocoder failed", e);
        } catch (Exception e) {
            Log.w(TAG, "Geocoder unexpected error", e);
        }
        return String.format(Locale.US, "%.5f, %.5f", lat, lng);
    }

    /**
     * Reset the singleton (useful for testing or full cleanup).
     */
    public static void resetInstance() {
        synchronized (LocationHelper.class) {
            if (INSTANCE != null && INSTANCE.isTracking) {
                INSTANCE.stopTracking();
            }
            INSTANCE = null;
            playServicesChecked = false;
            playServicesAvailable = false;
        }
    }

    // ──────────────────────────────────────────────
    //  LocationInfo data class
    // ──────────────────────────────────────────────

    public static class LocationInfo {
        public final double latitude;
        public final double longitude;
        public final float accuracy;
        public final String addressLine;
        public final long timestamp;
        public final boolean isEmpty;
        public final boolean isError;
        public final String errorMessage;

        private LocationInfo(double lat, double lng, float acc, String addr,
                             long ts, boolean empty, boolean isError, String errorMessage) {
            this.latitude = lat;
            this.longitude = lng;
            this.accuracy = acc;
            this.addressLine = addr;
            this.timestamp = ts;
            this.isEmpty = empty;
            this.isError = isError;
            this.errorMessage = errorMessage;
        }

        static LocationInfo fromLocation(Location loc) {
            return new LocationInfo(
                    loc.getLatitude(), loc.getLongitude(),
                    loc.getAccuracy(),
                    String.format(Locale.US, "%.5f, %.5f", loc.getLatitude(), loc.getLongitude()),
                    System.currentTimeMillis(),
                    false, false, null);
        }

        LocationInfo withAddress(String addr) {
            return new LocationInfo(latitude, longitude, accuracy, addr,
                    timestamp, false, false, null);
        }

        static LocationInfo empty() {
            return new LocationInfo(0, 0, 0, "暂无位置信息", 0, true, false, null);
        }

        static LocationInfo error(String msg) {
            return new LocationInfo(0, 0, 0, msg, System.currentTimeMillis(),
                    false, true, msg);
        }

        static LocationInfo locating() {
            return new LocationInfo(0, 0, 0, "正在定位...", System.currentTimeMillis(),
                    true, false, null);
        }

        static LocationInfo status(String msg) {
            return new LocationInfo(0, 0, 0, msg, System.currentTimeMillis(),
                    true, false, null);
        }
    }
}
