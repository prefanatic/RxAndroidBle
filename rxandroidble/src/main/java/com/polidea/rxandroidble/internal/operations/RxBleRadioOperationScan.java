package com.polidea.rxandroidble.internal.operations;

import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleInternalScanResult> {

    /*private final UUID[] filterServiceUUIDs;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final UUIDUtil uuidUtil;*/
    private final BluetoothLeScannerCompat scanner;
    private final List<ScanFilter> filters;
    private final ScanSettings settings;
    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;

   /* private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {

        if (!hasDefinedFilter() || hasDefinedFilter() && containsDesiredServiceIds(scanRecord)) {
            onNext(new RxBleInternalScanResult(device, rssi, scanRecord));
        }
    };*/

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            onNext(new RxBleInternalScanResult(result.getDevice(), result.getRssi(),
                    result.getScanRecord().getBytes()));
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(0, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            onError(new BleScanException(errorCode));
        }
    };

    public RxBleRadioOperationScan(List<ScanFilter> filters, ScanSettings settings) {
        this.scanner = BluetoothLeScannerCompat.getScanner();
        this.filters = filters;
        this.settings = settings;
    }

    @Override
    protected void protectedRun() {

        try {
            scanner.startScan(filters, settings, scanCallback);

            /*if (!startLeScanStatus) {
                onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            } else {*/
            synchronized (this) { // synchronization added for stopping the scan
                isStarted = true;
                if (isStopped) {
                    stop();
                }
            }
            //}
        } catch (Throwable throwable) {
            isStarted = true;
            RxBleLog.e(throwable, "Error while calling BluetoothAdapter.startLeScan()");
            onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
        } finally {
            releaseRadio();
        }
    }

    // synchronized keyword added to be sure that operation will be stopped no matter which thread will call it
    public synchronized void stop() {
        isStopped = true;
        if (isStarted) {
            // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
            scanner.stopScan(scanCallback);
        }
    }

 /*   private boolean containsDesiredServiceIds(byte[] scanRecord) {
        List<UUID> advertisedUUIDs = uuidUtil.extractUUIDs(scanRecord);

        for (UUID desiredUUID : filterServiceUUIDs) {

            if (!advertisedUUIDs.contains(desiredUUID)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasDefinedFilter() {
        return filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
    }*/
}
