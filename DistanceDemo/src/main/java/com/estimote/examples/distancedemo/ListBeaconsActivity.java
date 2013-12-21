package com.estimote.examples.distancedemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.Collections;
import java.util.List;

/**
 * Displays list of found beacons sorted by RSSI.
 *
 * @author wiktorgworek@google.com (Wiktor Gworek)
 */
public class ListBeaconsActivity extends Activity {

  private static final String TAG = ListBeaconsActivity.class.getSimpleName();

  private static final int REQUEST_ENABLE_BT = 1234;
  private static final String ESTIMOTE_PROXIMITY_UUID = "b9407f30-f5f8-466e-aff9-25556b57fe6d";
  //private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
  private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region(ESTIMOTE_PROXIMITY_UUID, null, null);

  private BeaconManager beaconManager;
  private LeDeviceListAdapter adapter;

  private TextView statusTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // Initialize status views.
    statusTextView = (TextView) findViewById(R.id.status);

    // Configure device list.
    adapter = new LeDeviceListAdapter(this);
    ListView list = (ListView) findViewById(R.id.device_list);
    list.setAdapter(adapter);
    list.setOnItemClickListener(createOnItemClickListener());

    // Configure BeaconManager.
    beaconManager = new BeaconManager(this);
    beaconManager.setRangingListener(new BeaconManager.RangingListener() {
      @Override
      public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
        // Note that results are not delivered on UI thread.
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            statusTextView.setText("Found beacons: " + beacons.size());
            adapter.replaceWith(beacons);
          }
        });
      }
    });
  }

  @Override
  protected void onDestroy() {
    beaconManager.disconnect();

    super.onDestroy();
  }

  @Override
  protected void onStart() {
    super.onStart();

    // Check if device supports Bluetooth Low Energy.
    if (!beaconManager.hasBluetooth()) {
      Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
      return;
    }

    // If Bluetooth is not enabled, let user enable it.
    if (!beaconManager.isBluetoothEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    } else {
      connectToService();
    }
  }

  @Override
  protected void onStop() {
    try {
      beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
    } catch (RemoteException e) {
      Log.d(TAG, "Error while stopping ranging", e);
    }

    super.onStop();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == Activity.RESULT_OK) {
        connectToService();
      } else {
        Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
        statusTextView.setText("Bluetooth not enabled");
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void connectToService() {
    statusTextView.setText("Scanning...");
    adapter.replaceWith(Collections.<Beacon>emptyList());
    beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
      @Override
      public void onServiceReady() {
        try {
          beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
          Toast.makeText(ListBeaconsActivity.this, "Cannot start ranging, something terrible happened",
              Toast.LENGTH_LONG).show();
          Log.e(TAG, "Cannot start ranging", e);
        }
      }
    });
  }

  private AdapterView.OnItemClickListener createOnItemClickListener() {
    return new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(DistanceBeaconActivity.createIntent(ListBeaconsActivity.this, adapter.getItem(position)));
      }
    };
  }

}
