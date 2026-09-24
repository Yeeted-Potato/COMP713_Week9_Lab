package nz.ac.aut.comp713.week9lab;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {
    // Activity 2 uses this executor for network work.
    private final ExecutorService networkExecutor =
            Executors.newSingleThreadExecutor();

    // Activity 4: request code and location service.
    private static final int LOCATION_REQUEST = 7;
    private LocationManager locationManager;

    private EditText taskIdInput;
    private Button loadTaskButton;
    private Button locationButton;
    private TextView networkOutput;
    private TextView locationOutput;
    private TextView combinedOutput;

    // Activities 2 and 4 set these values after successful operations.
    private String latestTaskTitle;
    private Location latestLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskIdInput = findViewById(R.id.taskIdInput);
        loadTaskButton = findViewById(R.id.loadTaskButton);
        locationButton = findViewById(R.id.locationButton);
        networkOutput = findViewById(R.id.networkOutput);
        locationOutput = findViewById(R.id.locationOutput);
        combinedOutput = findViewById(R.id.combinedOutput);

        // Activity 2
        loadTaskButton.setOnClickListener(v -> loadTask());

        // Activity 4
        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);
        locationButton.setOnClickListener(v -> showLocation());

        updateCombinedSummary();
    }

    // Activity 2 and 3: HTTPS GET on a worker thread

    private void loadTask() {
        // Main thread: validate input and show Loading.
        String entered = taskIdInput.getText().toString().trim();
        final int taskId;
        try {
            taskId = Integer.parseInt(entered);
            if (taskId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            taskIdInput.setError("Enter a positive task ID");
            return;
        }

        networkOutput.setText("Loading...");
        loadTaskButton.setEnabled(false);

        // Worker thread: the slow part. It never touches a view directly.
        networkExecutor.execute(() -> {
            try {
                String title = fetchTask(taskId);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    latestTaskTitle = title;
                    networkOutput.setText(
                            "Task " + taskId + ": " + title);
                    loadTaskButton.setEnabled(true);
                    updateCombinedSummary();
                });
            } catch (Exception e) {
                Log.e("Week9Lab", "GET failed", e);
                String message = messageFor(e);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    if (latestTaskTitle == null) {
                        networkOutput.setText(message);
                    } else {
                        networkOutput.setText(message
                                + "\nPrevious result: " + latestTaskTitle);
                    }
                    loadTaskButton.setEnabled(true);
                });
            }
        });
    }

    private String fetchTask(int taskId) throws Exception {
        URL url = new URL(
                "https://jsonplaceholder.typicode.com/todos/"
                        + taskId);
        HttpsURLConnection connection =
                (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");

        try {
            // Check the status BEFORE reading the body: for a 404,
            // getInputStream() would throw FileNotFoundException instead.
            int status = connection.getResponseCode();
            if (status != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                return new JSONObject(body.toString())
                        .getString("title");
            }
        } finally {
            connection.disconnect();
        }
    }

    private String messageFor(Exception error) {
        if (error instanceof SocketTimeoutException) {
            return "The request timed out. Retry.";
        }
        if (error instanceof JSONException) {
            return "The service returned unexpected data.";
        }
        String detail = error.getMessage();
        if (detail != null && detail.startsWith("HTTP ")) {
            return "The service returned " + detail + ".";
        }
        return "Could not reach the service. Check the connection.";
    }

    // Activity 4: runtime permission and one current fix

    private void showLocation() {
        if (checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_REQUEST);
            return;
        }
        fetchCurrentLocation();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
        if (requestCode != LOCATION_REQUEST) return;

        if (grantResults.length > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationOutput.setText("Location permission denied.");
        }
    }

    private void fetchCurrentLocation() {
        if (!locationManager.isLocationEnabled()) {
            locationOutput.setText(
                    "Turn on device Location and retry.");
            return;
        }

        locationOutput.setText("Finding location...");
        try {
            // If FUSED_PROVIDER keeps returning null on the emulator,
            // try LocationManager.GPS_PROVIDER instead.
            locationManager.getCurrentLocation(
                    LocationManager.FUSED_PROVIDER,
                    null,
                    getMainExecutor(),
                    fix -> {
                        if (fix == null) {
                            locationOutput.setText(
                                    "No fix yet. Retry.");
                            return;
                        }
                        latestLocation = fix;
                        locationOutput.setText(String.format(
                                Locale.US,
                                "Lat %.5f\nLon %.5f\nAccuracy +/- %.0f m",
                                fix.getLatitude(),
                                fix.getLongitude(),
                                fix.getAccuracy()));
                        updateCombinedSummary();
                    });
        } catch (SecurityException e) {
            locationOutput.setText(
                    "Permission changed. Tap again.");
        }
    }

    // Activity 5: 

    private void updateCombinedSummary() {
        if (latestTaskTitle == null || latestLocation == null) {
            combinedOutput.setText(R.string.summary_not_ready);
            return;
        }

        combinedOutput.setText(String.format(Locale.US,
                "%s\nNear %.5f, %.5f (accuracy +/- %.0f m)",
                latestTaskTitle,
                latestLocation.getLatitude(),
                latestLocation.getLongitude(),
                latestLocation.getAccuracy()));
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
    }
}

 