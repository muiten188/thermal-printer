package com.mybigday.rmxprinter;

import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import java.util.Set;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
//
import net.xprinter.utils.XPrinterDev;
import net.xprinter.utils.XPrinterDev.*;
import net.xprinter.utils.DataForSendToPrinterXp80;

public class RNXprinter extends ReactContextBaseJavaModule {
  private String LOG_TAG = "RNXprinter";
  private ReactApplicationContext context;

  private byte[] mBuffer = new byte[0];

  // Bluetooth
  private Set<BluetoothDevice> mPairedDevices;
  private XPrinterDev mBluetoothPrinter = null;

  public RNXprinter(ReactApplicationContext reactContext) {
    super(reactContext);

    this.context = reactContext;

    Log.v(LOG_TAG, "RNXprinter alloc");
  }

  @Override
  public String getName() {
    return "RNXprinter";
  }

  @ReactMethod
  public void getDeviceList(Promise promise){
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      promise.reject("-100", "Not bluetooth adapter");
    }
    else if (bluetoothAdapter.isEnabled()) {
      mPairedDevices = bluetoothAdapter.getBondedDevices();
      WritableArray pairedDeviceList = Arguments.createArray();
      for (BluetoothDevice device : mPairedDevices) {
        WritableMap deviceMap = Arguments.createMap();
        deviceMap.putString("name", device.getName());
        deviceMap.putString("address", device.getAddress());
        pairedDeviceList.pushMap(deviceMap);
      }
      promise.resolve(pairedDeviceList);
    }
    else {
      promise.reject("-103", "BluetoothAdapter not open...");
    }
  }

  @ReactMethod
  public void selectDevice(String address, Promise promise){
    if(mPairedDevices==null){
      promise.reject("-505", "bluetooth off");
    }
    else{
      for (BluetoothDevice device : mPairedDevices) {
        Log.d(LOG_TAG, "Checking:" + device.getAddress() + " : " + address);
        if(device.getAddress().equals(address)){
          mBluetoothPrinter = new XPrinterDev(PortType.Bluetooth, address);
          promise.resolve(true);
          return;
        }
      }
      promise.reject("-105", "Device address not exist.");
    }
  }

  @ReactMethod
  public void pushText(String text, Integer size,Integer alignText, Integer boldText){
    if(size < 0 || size > 7) {
      size = 0;
    }
    Log.d(LOG_TAG, "Bold text" + boldText);
    pushByteToBuffer(DataForSendToPrinterXp80.selectOrCancelBoldModel(boldText));
    Log.d(LOG_TAG, "Set AlignMent:" + alignText);
    pushByteToBuffer(DataForSendToPrinterXp80.selectAlignment(alignText));
    Log.d(LOG_TAG, "Set Font Size:" + size);
    pushByteToBuffer(DataForSendToPrinterXp80.selectCharacterSize(size * 17));
    Log.d(LOG_TAG, "Print String:" + text);
    pushByteToBuffer(DataForSendToPrinterXp80.strTobytes(text + "\n"));
  }

  @ReactMethod
  public void pushFlashImage(Integer index){
    Log.d(LOG_TAG, "Print FLASH Image:" + index);
    pushByteToBuffer(DataForSendToPrinterXp80.printBmpInFLASH(index, 0));
  }

  @ReactMethod
  public void pushCutPaper(){
    Log.d(LOG_TAG, "Cut Paper");
    pushByteToBuffer(DataForSendToPrinterXp80.selectCutPagerModerAndCutPager(66, 0));
  }

  @ReactMethod
  public void print(Promise promise){
    if(mBluetoothPrinter == null){
      promise.reject("-107", "Must select printer first.");
      return;
    }
    if(mBuffer.length == 0){
      promise.reject("-109", "Buffer is empty");
      return;
    }
    ReturnMessage returnMessage = mBluetoothPrinter.Open();
    if(returnMessage.GetErrorCode()!= ErrorCode.OpenPortSuccess){
      promise.reject("connectfail",returnMessage.GetErrorStrings());
    }
    else{
      Log.d(LOG_TAG, "Open device:" + returnMessage.GetErrorStrings());
      returnMessage = mBluetoothPrinter.Write(mBuffer);
      Log.d(LOG_TAG, "Write data:" + returnMessage.GetErrorStrings());
      clearBuffer();
      returnMessage = mBluetoothPrinter.Close();
      Log.d(LOG_TAG, "Close device:" + returnMessage.GetErrorStrings());
      promise.resolve(true);
    }

  }

  @ReactMethod
  public void clearPrintBuffer(){
    clearBuffer();
  }

  @ReactMethod
  public void printDemoPage(Promise promise){
    clearBuffer();
    pushFlashImage(1);
    pushText("Xprinter TEST\n", 1,1,3);
    pushText(" May in cua ban da san sang\n\n", 1,1,3);
    pushText("Powered by SDC Team", 0,1,3);
    pushCutPaper();
    print(promise);
  }

  private void pushByteToBuffer(byte[] input){
    byte[] newByte = new byte[mBuffer.length + input.length];
    System.arraycopy(mBuffer, 0, newByte, 0, mBuffer.length);
    System.arraycopy(input, 0, newByte, mBuffer.length, input.length);
    mBuffer = newByte;
    Log.d(LOG_TAG, "Push buffer:" + mBuffer.length);
  }
  private void clearBuffer(){
    mBuffer = new byte[0];
    Log.d(LOG_TAG, "Clear buffer:" + mBuffer.length);
  }
}