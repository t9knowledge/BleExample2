package jp.co.ctc_g.bleexample;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import jp.co.ctc_g.common.BleUtil;
import jp.co.ctc_g.common.BleUuid;
import knowledgedatabase.info.bleexample.R;

import static jp.co.ctc_g.bleexample.BleExampleApplication.TAG;

public class MainActivity extends BaseActivity {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private AdvertiseCallback mAdvertiseCallback;
    @Bind(R.id.statusTextView)
    TextView statusTextView;
    @Bind(R.id.advertiseSwitch)
    Switch advertiseSwitch;
    private byte[] mAlertLevel = new byte[] { (byte) 0x00 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        if (!BleUtil.isBLESupported(getApplicationContext())) {
            showText(getString(R.string.ble_not_supported));
            finish();
        }

        initialize();

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                ButterKnife.bind(MainActivity.this);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "#onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "#onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "#onDestroy");
        ButterKnife.unbind(this);
    }

    private void initialize() {
        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mAdvertiseCallback = new AdvertiseCallback() {

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                switch (errorCode) {
                    case ADVERTISE_FAILED_ALREADY_STARTED:
                        Log.e(TAG, "already started!");
                        break;
                    case ADVERTISE_FAILED_DATA_TOO_LARGE:
                        Log.e(TAG, "data is too large!");
                        break;
                    case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        Log.e(TAG, "feature unsupported!");
                        break;
                    case ADVERTISE_FAILED_INTERNAL_ERROR:
                        Log.e(TAG, "internal error!");
                        break;
                    case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        Log.e(TAG, "too many advertisers!");
                        break;
                }
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.i(TAG, "onStartSuccess: " + settingsInEffect.toString());
            }
        };
    }

    private void startAdvertise() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(
                getApplicationContext(),
                new BluetoothGattServerCallback() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onServiceAdded(int status, BluetoothGattService service) {
                        switch (status) {
                            case BluetoothGatt.GATT_SUCCESS:
                                Log.i(TAG, "onServiceAdded " + service.getUuid().toString());
                                break;

                            default:
                                Log.i(TAG, "onServiceAdded status is not GATT_SUCCESS");
                                break;
                        }
                    }

                    @SuppressLint("LongLogTag")
                    @Override
                    public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status, int newState) {
                        Log.d(TAG, "onConnectionStateChange [" + status + "->" + newState + "]");
                    }

                    @SuppressLint("LongLogTag")
                    @Override
                    public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device,
                                                            int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);
                        if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_ALERT_LEVEL))) {
                            Log.d(TAG, "CHAR_MANUFACTURER_NAME_STRING");
                            //characteristic.setValue("Name:Hoge");
                            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                        }
                    }

                    @Override
                    public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device,
                                                             int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                                                             boolean responseNeeded, int offset, byte[] value) {
                        Log.d(TAG, "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                                + Boolean.toString(preparedWrite) + " responseNeeded="
                                + Boolean.toString(responseNeeded) + " offset=" + offset);
                        if (characteristic.getUuid().equals(
                                UUID.fromString(BleUuid.CHAR_ALERT_LEVEL))) {
                            Log.d(TAG, "CHAR_ALERT_LEVEL");
                            if (value != null && value.length > 0) {
                                Log.d(TAG, "value.length=" + value.length);
                                mAlertLevel[0] = value[0];
                                characteristic.setValue(mAlertLevel);
                                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                vibrator.vibrate(10);
                            } else {
                                Log.d(TAG, "invalid value written");
                            }
                            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
                        }
                    }
                });
        setupUuid();
        mBluetoothLeAdvertiser.startAdvertising(createSettings(), createAdvertiseData(), mAdvertiseCallback);
    }

    private void stopAdvertise() {
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }

        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    @OnCheckedChanged(R.id.advertiseSwitch)
    void onCheckedChanged(Switch advertiseSwitch, boolean isChecked) {
        Log.i(TAG, "#onCheckedChanged [" + isChecked + "]");
        if (isChecked) {
            startAdvertise();
            statusTextView.setText(getString(R.string.switch_on));
       } else {
            stopAdvertise();
            statusTextView.setText(getString(R.string.switch_off));
        }
    }

    private void setupUuid() {
        BluetoothGattService ias = new BluetoothGattService(
                UUID.fromString(BleUuid.SERVICE_IMMEDIATE_ALERT),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic alc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_ALERT_LEVEL),
                BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        alc.setValue(mAlertLevel);
        ias.addCharacteristic(alc);
        mBluetoothGattServer.addService(ias);
    }

    private AdvertiseSettings createSettings() {
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW);
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingBuilder.setConnectable(true);
        return settingBuilder.build();
    }

    private AdvertiseData createAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        ParcelUuid uuid = new ParcelUuid(UUID.fromString(BleUuid.SERVICE_IMMEDIATE_ALERT));
        dataBuilder.addServiceUuid(uuid);
        AdvertiseData advertiseData = dataBuilder.build();
        return advertiseData;
    }

}
