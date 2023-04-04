package org.lm.senstick;

import static org.lm.senstick.Utils.DEVELOPER_VERSION_DEBUG;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_ADC_POWER_ON;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_ENABLE_ADC2;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_ENABLE_ADC3;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_ENABLE_VOC;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_FW_VERSION;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_HW_VERSION;
import static org.lm.senstick.Utils.KEY_SP_SENSTICK_REGION;
import static org.lm.senstick.Utils.KEY_SP_STRING_TAG;
import static org.lm.senstick.Utils.KEY_SP_STRING_TAG_SHARE;
import static org.lm.senstick.Utils.Log;
import static org.lm.senstick.Utils.TAG;
import static org.lm.senstick.Utils.readStringParmSharedPref;
import static org.lm.senstick.Utils.saveStringParmSharedPref;
import static org.lm.senstick.Utils.stringToBoolean;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private NfcAdapter mNfcAdapter;
    Context context;
    Spinner spinnerDataRate;
    int counterLogoTap = 0;
    Boolean flagEnableTapLogo = false;

    EditText editTextAppEui, editTextAppKey, editTextSendPeriod, editTextNoJoinRetries, editTextConfirmPacketNo;
    EditText editTextJoinPeriodForRetries, editTextJoinPeriodAfterRetries, editTextMovementThreshold;
    TextView editTextGpsLatitude, editTextGpsLongitude, editTextGpsAltitude;
    EditText editTextConfirmPacket, editTextMovementSendDelay;
    TextView editTextHwVersion, editTextFwVersion, textViewDataRate;
    CheckBox checkBoxEnableAdr, checkBoxEnableMovementDetection, checkBoxEnableTempHum, checkBoxEnableAirPressure;

    String valueStringTag = "";
    String valueDeviceEui = "";
    String valueAppEui = "";
    String valueAppKey = "";
    String valueAppKeyHidden = "";
    String valueSendPeriod = "";
    String valueNoJoinRetries = "";
    String valueJoinPeriodForRetries = "";
    String valueJoinPeriodAfterRetries = "";
    String valueConfirmPacket = "";
    String valueFwVersion = "";
    String valueHwVersion = "";
    String valueEnableAdr = "";
    String valueDataRate = "";
    String valueEnableMovementDetection = "";
    String valueMovementSendDelay = "";
    String valueMovementThreshold = "";
    String valueEnableTempHum = "";
    String valueEnableAirPressure = "";
    String valueEnableVoc = "";
    String valueEnableAdc2 = "";
    String valueEnableAdc3 = "";
    String valueRegion = "";
    String valueAdcPowerOn = "";
    String valueGpsLatitude = "";
    String valueGpsLongitude = "";
    String valueGpsAltitude = "";
    String valueHybridEnable = "";
    String valueMask0 = "";
    String valueMask1 = "";
    String valueAS923OFFSET = "";
    String valueMask2 = "";
    String valueMask3 = "";
    String valueMask4 = "";
    String valueMask5 = "";
    String valueNFCChanges = "";
    String valueDeviceStatus = "";
    String valueReservedBytes = "";
    String valueFamilyID = "";
    String valueProductID = "";

    int APP_KEY_LEN_32 = 32;
    int APP_EUI_LEN_16 = 16;

    static String VIEW_OLD = "ViewOld";
    static String VIEW_H30 = "ViewH30";
    static String VIEW_H30_V3 = "ViewH30_V3";
    static String VIEW_INIT = "ViewInit";
    static String currentView = VIEW_INIT;

    TextView textViewDeviceEui;
    TextView textViewDeviceId;

    LocationManager locationManager;
    LocationListener locationListener;
    ArrayAdapter<String> spinnerArrayAdapter;

    Button buttonSendToEmail;
    Button buttonWrite;
    Tag tag;
    boolean flagWrite = false;
    boolean flagHw30 = false;
    boolean flagHw30_V3 = false;
    int HW_30 = 30;

    int minTime = 20 * 1000;
    int minDist = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentView = VIEW_INIT;
        setContentView(R.layout.activity_main_init);

        context = this;

        initNFC();

        initPermissions();

        locationStart();


    }

    void initView() {
        if (currentView.equals(VIEW_OLD))
            return;

        currentView = VIEW_OLD;
        setContentView(R.layout.activity_main);
        initFields();
        initFieldsEnable();
    }

    void initViewHw30() {
        if (currentView.equals(VIEW_H30))
            return;

        currentView = VIEW_H30;
        setContentView(R.layout.activity_main_30v2);
        initFields30();
        initFieldsEnableHw30();
    }
    void initViewHw30_V3() {
        if (currentView.equals(VIEW_H30_V3))
            return;

        currentView = VIEW_H30_V3;
        setContentView(R.layout.activity_main_30);
        initFields30_V3();
        initFieldsEnableHw30();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        locationManager.removeUpdates(locationListener);

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

        counterLogoTap = 0;


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    private void initNFC() {

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!flagWrite) {


                if (intent != null) {
                    String data = readFromNfc(intent);

                    // Test
//                    data = TEST_VALUE_30;

                    // Check for 30 version
                  //  int valueHw30 = 0;
                  //  try {
                  //      valueHw30 = Integer.decode("0x" + data.substring(74, 76));
                  //  } catch (Exception e) {
                  //      setContentView(R.layout.activity_main_init);
                  //     return;
                  // }
                  //int valueHw30_V3 = 0;
                  //try {
                  //     valueHw30_V3 = Integer.decode("0x" + data.substring(4, 6));
                  //} catch (Exception e) {
                  //    setContentView(R.layout.activity_main_init);
                  //    return;
                  //}

                  //if (valueHw30 == HW_30)
                  //    flagHw30 = true;
                  //else
                  //    flagHw30 = false;

                  //if (valueHw30_V3 == HW_30)
                  //    flagHw30_V3 = true;
                  //else
                  //    flagHw30_V3 = false;

                    if (flagHw30) {
                        initViewHw30();

                        parseDataHw30(data);
                    }

                    else if (flagHw30_V3) {
                        initViewHw30_V3();

                        parseDataHw30_V3(data);
                    }
                   else {
                       initView();

                       parseData(data);
                   }

                    if (DEVELOPER_VERSION_DEBUG) {
                        editTextAppEui = findViewById(R.id.editTextApp_Eui);
                        editTextAppEui.setBackgroundColor(Color.BLUE);
                    }

                    if (flagHw30) {
                        initDataRate30();
                    }
                    else if (flagHw30_V3) {
                        initDataRate30();
                    }
                    else{
                        initDataRate();
                    }


                }


            } else {

                if (flagHw30)
                    writeTag(tag, prepareDataForWriteHw30());
                else if (flagHw30_V3)
                    writeTag_V3(tag, prepareDataForWriteHw30_V3());
                else
                    writeTag(tag, prepareDataForWrite());

            }

        }

    }

    void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] PERMISSIONS = {Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NFC};

            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

  // private String readFirstByte(Intent intent){
  //
  // }
    private String readFromNfc(Intent intent) {

        String result = "";
        int shift = 0;

        try {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NfcV nfcvTag = NfcV.get(tag);

            StringBuilder stringbuilder = new StringBuilder();
            ByteBuffer buffer = ByteBuffer.allocate(160);

            byte[] id = tag.getId();
            byte[] readCmd = new byte[3 + id.length];
            // set "address" flag (only send command to this tag)
            readCmd[0] = 0x20;
            // ISO 15693 Single Block Read command byte
            readCmd[1] = 0x20;

            System.arraycopy(id, 0, readCmd, 2, id.length);
            nfcvTag.connect();

            int padding = -1;

            for (int i = 0; i < 20; i++) {
                // 1 byte payload: block address
                readCmd[2 + id.length] = (byte) (i + shift);
                byte[] data;
                data = nfcvTag.transceive(readCmd);
//                Log(TAG, String.valueOf(i));


                for (byte aData1 : data) {
                    if (i == padding) {
//                        Log(TAG, String.format("%02X ", aData1));
//                        Log(TAG, String.format("%02X ", aData1)+ " : " + String.valueOf(i));
                        stringbuilder.append(String.format("%02X", aData1));
                        buffer.put(aData1);
                    } else {
                        padding = i;
                    }
//
                }
            }
//
            result = stringbuilder.toString().trim();

//            for(int i=0; i< result.length() ; i++) {
//                Log(TAG, String.format("%02X ", result[i]));
//            }

            nfcvTag.close();

            Toast.makeText(context, getResources().getString(R.string.toast_tag_read_ok),
                    Toast.LENGTH_SHORT).show();


        } catch (Exception e) {

            try {
                Toast toast = Toast.makeText(context, getResources().getString(R.string.toast_tag_read_nok), Toast.LENGTH_SHORT);
                TextView v = toast.getView().findViewById(android.R.id.message);
                v.setTextColor(Color.RED);
                toast.show();
            } catch (Exception e2) {
            }

        }

        if (Integer.parseInt(result.substring(1,2)) != 0) {
            flagHw30_V3 = true;
            flagHw30 = false;
        }
        else{
            try {
                result = result.substring(52, result.length());
                Log(TAG, shift + " : " + result);
                flagHw30_V3 = false;
                flagHw30 = true;
            }
            catch (Exception e) {
                Log(TAG, e.toString());
            }

            Log("Write READ", result);

        }
        return result;

    }

    void initFields() {

        textViewDeviceEui = findViewById(R.id.editTextDeviceId);
        textViewDeviceId = findViewById(R.id.textViewDeviceId);
        editTextAppEui = findViewById(R.id.editTextApp_Eui);
        editTextAppKey = findViewById(R.id.editTextApp_Key);
        editTextSendPeriod = findViewById(R.id.editTextSendPeriod);
        editTextNoJoinRetries = findViewById(R.id.editTextNoJoinRetries);
        editTextJoinPeriodForRetries = findViewById(R.id.editTextJoinPeriodForRetries);
        editTextJoinPeriodAfterRetries = findViewById(R.id.editTextJoinPeriodAfterRetries);
        editTextMovementThreshold = findViewById(R.id.editTextMovementThreshold);
        editTextConfirmPacketNo = findViewById(R.id.editTextConfirmPacketNo);

        textViewDataRate = findViewById(R.id.textViewDataRate);
        checkBoxEnableAdr = findViewById(R.id.checkBoxEnableAdr);
        checkBoxEnableMovementDetection = findViewById(R.id.checkBoxEnableMovementDetection);
        checkBoxEnableTempHum = findViewById(R.id.checkBoxTempHum);
        checkBoxEnableAirPressure = findViewById(R.id.checkBoxEnableAirPressure);

        editTextGpsLatitude = findViewById(R.id.editTextGpsLatitude);
        editTextGpsLongitude = findViewById(R.id.editTextGpsLongitude);
        editTextGpsAltitude = findViewById(R.id.editTextGpsAltitude);

        buttonSendToEmail = findViewById(R.id.buttonSend);
        buttonWrite = findViewById(R.id.buttonSave);

        List<String> myArrayList = Arrays.asList(getResources().getStringArray(R.array.data_rate));
        spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        myArrayList);

        spinnerDataRate = findViewById(R.id.spinnerDataRate);


        editTextAppEui.setEnabled(false);
        editTextAppKey.setEnabled(false);

        editTextSendPeriod.setEnabled(false);
        editTextNoJoinRetries.setEnabled(false);
        editTextJoinPeriodForRetries.setEnabled(false);
        editTextJoinPeriodAfterRetries.setEnabled(false);
        editTextConfirmPacketNo.setEnabled(false);
        editTextMovementThreshold.setEnabled(false);
        editTextGpsLatitude.setEnabled(false);
        editTextGpsLongitude.setEnabled(false);
        editTextGpsAltitude.setEnabled(false);
        checkBoxEnableAdr.setEnabled(false);
        checkBoxEnableMovementDetection.setEnabled(false);
        checkBoxEnableTempHum.setEnabled(false);
        checkBoxEnableAirPressure.setEnabled(false);

        buttonSendToEmail.setEnabled(false);
        buttonWrite.setEnabled(false);
    }

    void initFields30() {

        textViewDeviceEui = findViewById(R.id.editTextDeviceId);
        textViewDeviceId = findViewById(R.id.textViewDeviceId);
        editTextAppEui = findViewById(R.id.editTextApp_Eui);
        editTextAppKey = findViewById(R.id.editTextApp_Key);

        List<String> myArrayList = Arrays.asList(getResources().getStringArray(R.array.data_rate));
        spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        myArrayList);

        textViewDataRate = findViewById(R.id.textViewDataRate);
        checkBoxEnableAdr = findViewById(R.id.checkBoxEnableAdr);
        spinnerDataRate = findViewById(R.id.spinnerDataRate);
        editTextConfirmPacket = findViewById(R.id.editTextConfirmPacket);
        editTextSendPeriod = findViewById(R.id.editTextSendPeriod);
        //editTextMovementSendDelay = findViewById(R.id.editTextMovementSendDelay);
        editTextMovementThreshold = findViewById(R.id.editTextMovementThreshold);

        editTextHwVersion = findViewById(R.id.editTextHwVersion);
        editTextFwVersion = findViewById(R.id.editTextFwVersion);

        editTextGpsLatitude = findViewById(R.id.editTextGpsLatitude);
        editTextGpsLongitude = findViewById(R.id.editTextGpsLongitude);
        editTextGpsAltitude = findViewById(R.id.editTextGpsAltitude);

        buttonSendToEmail = findViewById(R.id.buttonSend);
        buttonWrite = findViewById(R.id.buttonSave);

        editTextAppEui.setEnabled(false);
        editTextAppKey.setEnabled(false);

        editTextSendPeriod.setEnabled(false);
        editTextConfirmPacket.setEnabled(false);
        //editTextMovementSendDelay.setEnabled(false);
        editTextMovementThreshold.setEnabled(false);
        editTextGpsLatitude.setEnabled(false);
        editTextGpsLongitude.setEnabled(false);
        editTextGpsAltitude.setEnabled(false);
//        checkBoxEnableAdr.setEnabled(false);

        buttonSendToEmail.setEnabled(false);
        buttonWrite.setEnabled(false);
    }
    void initFields30_V3() {

        textViewDeviceEui = findViewById(R.id.editTextDeviceId);
        textViewDeviceId = findViewById(R.id.textViewDeviceId);
        editTextAppEui = findViewById(R.id.editTextApp_Eui);
        editTextAppKey = findViewById(R.id.editTextApp_Key);

        List<String> myArrayList = Arrays.asList(getResources().getStringArray(R.array.data_rate));
        spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        myArrayList);

        textViewDataRate = findViewById(R.id.textViewDataRate);
        checkBoxEnableAdr = findViewById(R.id.checkBoxEnableAdr);
        spinnerDataRate = findViewById(R.id.spinnerDataRate);
        editTextConfirmPacket = findViewById(R.id.editTextConfirmPacket);
        editTextSendPeriod = findViewById(R.id.editTextSendPeriod);
        //editTextMovementSendDelay = findViewById(R.id.editTextMovementSendDelay);
        editTextMovementThreshold = findViewById(R.id.editTextMovementThreshold);

        editTextHwVersion = findViewById(R.id.editTextHwVersion);
        editTextFwVersion = findViewById(R.id.editTextFwVersion);

        editTextGpsLatitude = findViewById(R.id.editTextGpsLatitude);
        editTextGpsLongitude = findViewById(R.id.editTextGpsLongitude);
        editTextGpsAltitude = findViewById(R.id.editTextGpsAltitude);
        checkBoxEnableTempHum = findViewById(R.id.checkBoxTempHum);
        checkBoxEnableAirPressure = findViewById(R.id.checkBoxEnableAirPressure);

        buttonSendToEmail = findViewById(R.id.buttonSend);
        buttonWrite = findViewById(R.id.buttonSave);

        editTextAppEui.setEnabled(false);
        editTextAppKey.setEnabled(false);

        editTextSendPeriod.setEnabled(false);
        editTextConfirmPacket.setEnabled(false);
        //editTextMovementSendDelay.setEnabled(false);
        editTextMovementThreshold.setEnabled(false);
        editTextGpsLatitude.setEnabled(false);
        editTextGpsLongitude.setEnabled(false);
        editTextGpsAltitude.setEnabled(false);
//        checkBoxEnableAdr.setEnabled(false);


        buttonSendToEmail.setEnabled(false);
        buttonWrite.setEnabled(false);
    }

    void initDataRate() {

        if ((checkBoxEnableAdr != null) && (spinnerDataRate != null)) {
            if (!checkBoxEnableAdr.isChecked()) {
                spinnerDataRate.setEnabled(true);
                spinnerDataRate.setAlpha(1.0f);
            } else {
                spinnerDataRate.setEnabled(false);
                spinnerDataRate.setAlpha(0.5f);
            }

            hideDataRate(checkBoxEnableAdr.isChecked());
        }

    }

    void initDataRate30() {

        if (valueDataRate.length() == 0)
            return;

        if ((checkBoxEnableAdr != null) && (spinnerDataRate != null)) {
//            spinnerDataRate.setEnabled(true);
//            checkBoxEnableAdr.setEnabled(true);

            int ADR = Integer.decode("0x" + valueDataRate);

            if (ADR >= 128) {
//                spinnerDataRate.setAlpha(0.5f);
//                spinnerDataRate.setEnabled(false);

                checkBoxEnableAdr.setChecked(true);
                spinnerDataRate.setSelection(ADR - 128);

            } else {
//                spinnerDataRate.setAlpha(1.0f);
//                spinnerDataRate.setEnabled(true);

                checkBoxEnableAdr.setChecked(false);
                spinnerDataRate.setSelection(ADR);
            }

//            hideDataRate(checkBoxEnableAdr.isChecked());
        }

    }

    void hideDataRate(Boolean hide) {

        if (hide) {
            spinnerDataRate.setVisibility(View.GONE);
            textViewDataRate.setVisibility(View.GONE);
        } else {
            spinnerDataRate.setVisibility(View.VISIBLE);
            textViewDataRate.setVisibility(View.VISIBLE);
        }

    }

    void initFieldsEnable() {
        spinnerDataRate.setAdapter(spinnerArrayAdapter);
        spinnerDataRate.setOnItemSelectedListener(this);

//        spinnerDataRate.setEnabled(true);

        textViewDeviceEui.setEnabled(true);
        textViewDeviceEui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("device_eui", textViewDeviceEui.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(context, getResources().getString(R.string.toast_device_eui_copy),
                        Toast.LENGTH_SHORT).show();
            }
        });

        editTextAppEui.setEnabled(true);
        editTextAppEui.setTransformationMethod(null);
        editTextAppEui.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable textAppEui) {

                if (editTextAppEui.getText().length() > 0 && editTextAppEui.getText().length() < APP_EUI_LEN_16) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                            Toast.LENGTH_SHORT).show();
                    editTextAppEui.setTextColor(Color.RED);

                    // Check for INVALID
                    if (!textAppEui.toString().matches("^[0-9a-fA-F]+$")) {
                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_invalid),
                                Toast.LENGTH_SHORT).show();
                        editTextAppEui.setTextColor(Color.RED);
                    }

                } else if (editTextAppEui.getText().length() > 0 && editTextAppEui.getText().length() == APP_EUI_LEN_16) {
                    if (textAppEui.toString().matches("^[0-9a-fA-F]+$"))
                        editTextAppEui.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextAppEui.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TAP OUTSIDE
                if (!hasFocus) {
                    // ORIGINAL VALUES, HIDE CHAR
                    if (editTextAppEui.getText().length() != 0) {
                        editTextAppEui.setText(editTextAppEui.getText().toString().replaceAll("[ ()-]", ""));
                        valueAppEui = editTextAppEui.getText().toString();
                    }

                    // CHECK LENGTH
                    if (editTextAppEui.getText().length() > 0 && editTextAppEui.getText().length() < APP_EUI_LEN_16) {
                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                                Toast.LENGTH_SHORT).show();
                        editTextAppEui.setTextColor(Color.RED);
                    }

                }
            }
        });

        editTextAppKey.setEnabled(true);
        editTextAppKey.setTransformationMethod(null);
//        editTextAppKey.setTransformationMethod(new PasswordTransformationMethod());
        editTextAppKey.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable text) {

                if (editTextAppKey.getText().length() > 0 && editTextAppKey.getText().length() < APP_KEY_LEN_32) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                            Toast.LENGTH_SHORT).show();
                    editTextAppKey.setTextColor(Color.RED);

                    // Check for INVALID
                    if (!text.toString().matches("^[0-9a-fA-F]+$")) {

                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_invalid),
                                Toast.LENGTH_SHORT).show();
                        editTextAppKey.setTextColor(Color.RED);
                    }

                } else if (editTextAppKey.getText().length() > 0 && editTextAppKey.getText().length() == APP_KEY_LEN_32) {
                    if (text.toString().matches("^[0-9a-fA-F]+$"))
                        editTextAppKey.setTextColor(getResources().getColor(R.color.colorText));

                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });


        editTextAppKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TAP INSIDE
                if (hasFocus) {
                    if (editTextAppKey.getText().length() == APP_KEY_LEN_32)
                        editTextAppKey.setText("");
                }
                // TAP OUTSIDE
                else {
                    // ORIGINAL VALUES, HIDE CHAR
                    if (editTextAppKey.getText().length() == 0) {
                        editTextAppKey.setText(valueAppKey);

                    }
                    // NEW VALUES, STORE
                    else {
                        editTextAppKey.setText(editTextAppKey.getText().toString().replaceAll("[ ()-]", ""));
                        valueAppKey = editTextAppKey.getText().toString();
                    }

                    // CHECK LENGTH
                    if (editTextAppKey.getText().length() > 0 && editTextAppKey.getText().length() < APP_KEY_LEN_32) {
                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                                Toast.LENGTH_SHORT).show();
                        editTextAppKey.setTextColor(Color.RED);
                    }

                }

            }
        });


        editTextSendPeriod.setEnabled(true);
        editTextSendPeriod.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (editTextSendPeriod.getText().length() > 0 && Integer.valueOf(editTextSendPeriod.getText().toString()) > 65535) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextSendPeriod.setText("");
                    editTextSendPeriod.setTextColor(Color.RED);
                } else if (editTextSendPeriod.getText().length() > 0 && Integer.valueOf(editTextSendPeriod.getText().toString()) < 600) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_low),
                            Toast.LENGTH_SHORT).show();
                    editTextSendPeriod.setTextColor(Color.RED);
                } else {
                    editTextSendPeriod.setTextColor(getResources().getColor(R.color.colorText));
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextNoJoinRetries.setEnabled(true);
        editTextNoJoinRetries.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (editTextNoJoinRetries.getText().length() > 0 && Integer.valueOf(editTextNoJoinRetries.getText().toString()) > 65535) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextNoJoinRetries.setText("");
                    editTextNoJoinRetries.setTextColor(Color.RED);
                } else if (editTextNoJoinRetries.getText().length() > 0 && Integer.valueOf(editTextNoJoinRetries.getText().toString()) > 30) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_low),
                            Toast.LENGTH_SHORT).show();
                    editTextNoJoinRetries.setTextColor(Color.RED);
                } else {
                    editTextNoJoinRetries.setTextColor(getResources().getColor(R.color.colorText));
                }
            }


            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextJoinPeriodForRetries.setEnabled(true);
        editTextJoinPeriodForRetries.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (editTextJoinPeriodForRetries.getText().length() > 0 && Integer.valueOf(editTextJoinPeriodForRetries.getText().toString()) > 65535) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextJoinPeriodForRetries.setText("");
                    editTextJoinPeriodForRetries.setTextColor(Color.RED);
                } else if (editTextJoinPeriodForRetries.getText().length() > 0 && Integer.valueOf(editTextJoinPeriodForRetries.getText().toString()) < 30) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_low),
                            Toast.LENGTH_SHORT).show();
                    editTextJoinPeriodForRetries.setTextColor(Color.RED);
                } else {
                    editTextJoinPeriodForRetries.setTextColor(getResources().getColor(R.color.colorText));
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextJoinPeriodAfterRetries.setEnabled(true);
        editTextJoinPeriodAfterRetries.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (editTextJoinPeriodAfterRetries.getText().length() > 0 && Integer.valueOf(editTextJoinPeriodAfterRetries.getText().toString()) > 65535) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextJoinPeriodAfterRetries.setText("");
                    editTextJoinPeriodAfterRetries.setTextColor(Color.RED);
                } else if (editTextJoinPeriodAfterRetries.getText().length() > 0 && Integer.valueOf(editTextJoinPeriodAfterRetries.getText().toString()) < 3600) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_low),
                            Toast.LENGTH_SHORT).show();
                    editTextJoinPeriodAfterRetries.setTextColor(Color.RED);
                } else {
                    editTextJoinPeriodAfterRetries.setTextColor(getResources().getColor(R.color.colorText));
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextConfirmPacketNo.setEnabled(true);
        editTextConfirmPacketNo.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if (editTextConfirmPacketNo.getText().length() > 0 && Integer.valueOf(editTextConfirmPacketNo.getText().toString()) > 255) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextConfirmPacketNo.setText("");
                    editTextConfirmPacketNo.setTextColor(Color.RED);
                } else {
                    editTextConfirmPacketNo.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextMovementThreshold.setEnabled(false);
        editTextMovementThreshold.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if (editTextMovementThreshold.getText().length() > 0 && Integer.valueOf(editTextMovementThreshold.getText().toString()) > 100) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextMovementThreshold.setText("");
                    editTextMovementThreshold.setTextColor(Color.RED);
                } else {
                    editTextMovementThreshold.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextGpsLatitude.setEnabled(true);
        editTextGpsLongitude.setEnabled(true);
        editTextGpsAltitude.setEnabled(true);

        checkBoxEnableAdr.setEnabled(true);
        checkBoxEnableAdr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                         @Override
                                                         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                                                             if (!isChecked) {
//                                                                 spinnerDataRate.setEnabled(true);
//                                                                 spinnerDataRate.setAlpha(1.0f);
//                                                             } else {
//                                                                 spinnerDataRate.setEnabled(false);
//                                                                 spinnerDataRate.setAlpha(0.5f);
//                                                             }
//                                                             hideDataRate(isChecked);
                                                         }
                                                     }
        );

        checkBoxEnableMovementDetection.setEnabled(true);
        checkBoxEnableMovementDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                                       @Override
                                                                       public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                                           if (isChecked) {
                                                                               editTextMovementThreshold.setEnabled(true);
                                                                               editTextMovementThreshold.setAlpha(1.0f);
                                                                           } else {
                                                                               editTextMovementThreshold.setEnabled(false);
                                                                               editTextMovementThreshold.setAlpha(0.5f);
                                                                           }
                                                                       }
                                                                   }
        );

        checkBoxEnableTempHum.setEnabled(true);
        checkBoxEnableAirPressure.setEnabled(true);

        buttonSendToEmail.setEnabled(true);
        buttonWrite.setEnabled(true);

        flagEnableTapLogo = true;
    }


    void initFieldsEnableHw30() {
        flagEnableTapLogo = true;

        spinnerDataRate.setAdapter(spinnerArrayAdapter);
        spinnerDataRate.setOnItemSelectedListener(this);

        textViewDeviceEui.setEnabled(true);
        textViewDeviceEui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("device_eui", textViewDeviceEui.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(context, getResources().getString(R.string.toast_device_eui_copy),
                        Toast.LENGTH_SHORT).show();
            }
        });

        editTextAppEui.setEnabled(true);
        editTextAppEui.setTransformationMethod(null);
        editTextAppEui.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable textAppEui) {

                if (editTextAppEui.getText().length() > 0 && editTextAppEui.getText().length() < APP_EUI_LEN_16) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                            Toast.LENGTH_SHORT).show();
                    editTextAppEui.setTextColor(Color.RED);

                    // Check for INVALID
                    if (!textAppEui.toString().matches("^[0-9a-fA-F]+$")) {
                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_invalid),
                                Toast.LENGTH_SHORT).show();
                        editTextAppEui.setTextColor(Color.RED);
                    }

                } else if (editTextAppEui.getText().length() > 0 && editTextAppEui.getText().length() == APP_EUI_LEN_16) {
                    if (textAppEui.toString().matches("^[0-9a-fA-F]+$"))
                        editTextAppEui.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextAppEui.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TAP OUTSIDE
                if (!hasFocus) {
                    // ORIGINAL VALUES, HIDE CHAR
                    if (editTextAppEui.getText().length() != 0) {
                        editTextAppEui.setText(editTextAppEui.getText().toString().replaceAll("[ ()-]", ""));
                        valueAppEui = editTextAppEui.getText().toString();
                    }

                    // CHECK LENGTH
                    if (editTextAppEui.getText().length() > 0 && editTextAppEui.getText().length() < APP_EUI_LEN_16) {
                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                                Toast.LENGTH_SHORT).show();
                        editTextAppEui.setTextColor(Color.RED);
                    }

                }
            }
        });

        editTextAppKey.setEnabled(true);
        editTextAppKey.setTransformationMethod(null);
//        editTextAppKey.setTransformationMethod(new PasswordTransformationMethod());
        editTextAppKey.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable text) {

                if (editTextAppKey.getText().length() > 0 && editTextAppKey.getText().length() < APP_KEY_LEN_32) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                            Toast.LENGTH_SHORT).show();
                    editTextAppKey.setTextColor(Color.RED);

                    // Check for INVALID
                    if (!text.toString().matches("^[0-9a-fA-F]+$")) {

                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_invalid),
                                Toast.LENGTH_SHORT).show();
                        editTextAppKey.setTextColor(Color.RED);
                    }

                } else if (editTextAppKey.getText().length() > 0 && editTextAppKey.getText().length() == APP_KEY_LEN_32) {
                    if (text.toString().matches("^[0-9a-fA-F]+$"))
                        editTextAppKey.setTextColor(getResources().getColor(R.color.colorText));

                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        editTextAppKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TAP INSIDE
                if (hasFocus) {
                    if (editTextAppKey.getText().length() == APP_KEY_LEN_32)
                        editTextAppKey.setText("");
                }
                // TAP OUTSIDE
                else {
                    // ORIGINAL VALUES, HIDE CHAR
                    if (editTextAppKey.getText().length() == 0) {
                        editTextAppKey.setText(valueAppKey);

                    }
                    // NEW VALUES, STORE
                    else {
                        editTextAppKey.setText(editTextAppKey.getText().toString().replaceAll("[ ()-]", ""));
                        valueAppKey = editTextAppKey.getText().toString();
                    }

                    // CHECK LENGTH
                    if (editTextAppKey.getText().length() > 0 && editTextAppKey.getText().length() < APP_KEY_LEN_32) {
                        Toast.makeText(context, getResources().getString(R.string.toast_value_to_short),
                                Toast.LENGTH_SHORT).show();
                        editTextAppKey.setTextColor(Color.RED);
                    }

                }

            }
        });


        editTextSendPeriod.setEnabled(true);
        editTextSendPeriod.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if (editTextSendPeriod.getText().length() > 1 && Integer.valueOf(editTextSendPeriod.getText().toString()) > 255) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextSendPeriod.setText("");
                    editTextSendPeriod.setTextColor(Color.RED);
                } else {
                    editTextSendPeriod.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextConfirmPacket.setEnabled(true);
        editTextConfirmPacket.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if (editTextConfirmPacket.getText().length() > 0 && Integer.valueOf(editTextConfirmPacket.getText().toString()) > 255) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextConfirmPacket.setText("");
                    editTextConfirmPacket.setTextColor(Color.RED);
                } else {
                    editTextConfirmPacket.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        editTextMovementThreshold.setEnabled(true);
        editTextMovementThreshold.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if (editTextMovementThreshold.getText().length() > 0 && Integer.valueOf(editTextMovementThreshold.getText().toString()) > 127) {
                    Toast.makeText(context, getResources().getString(R.string.toast_value_to_big),
                            Toast.LENGTH_SHORT).show();
                    editTextMovementThreshold.setText("");
                    editTextMovementThreshold.setTextColor(Color.RED);
                } else {
                    editTextMovementThreshold.setTextColor(getResources().getColor(R.color.colorText));
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextGpsLatitude.setEnabled(true);
        editTextGpsLongitude.setEnabled(true);
        editTextGpsAltitude.setEnabled(true);

        checkBoxEnableAdr.setEnabled(true);
        checkBoxEnableAdr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                         @Override
                                                         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                     if (!isChecked) {
//                         spinnerDataRate.setEnabled(true);
//                         spinnerDataRate.setAlpha(1.0f);
//                     } else {
//                         spinnerDataRate.setEnabled(false);
//                         spinnerDataRate.setAlpha(0.5f);
//                     }
//                     hideDataRate(isChecked);
                                                         }
                                                     }
        );

        buttonSendToEmail.setEnabled(true);
        buttonWrite.setEnabled(true);
    }

    void setEditTextAppKeyTwoLines() {
        if (editTextAppKey.getText().length() == APP_KEY_LEN_32 / 2) {
            String line1 = editTextAppKey.getText().toString().substring(0, APP_KEY_LEN_32 / 2);
            String line2 = editTextAppKey.getText().toString().substring(APP_KEY_LEN_32 / 2, editTextAppKey.getText().length());

            editTextAppKey.setText(line1 + "\n" + line2);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void parseData(String result) {

        // SAVE TAG
        valueStringTag = result;
        Log.e("valueStringTag", valueStringTag);

        saveStringParmSharedPref(valueStringTag, KEY_SP_STRING_TAG, this);

        try {

            // 8B Device EUI
            valueDeviceEui = result.substring(0, 16);
            textViewDeviceEui.setText(valueDeviceEui);

            // 8B Application  EUI
            valueAppEui = result.substring(16, 32);
            editTextAppEui.setText(valueAppEui);

            // 1B APP Key
            valueAppKey = result.substring(32, 64);
            valueAppKeyHidden = valueAppKey;
            editTextAppKey.setText(valueAppKey);

            // 1B Frequency Region
            valueRegion = result.substring(64, 66);

            // 1B Data Rate
            valueDataRate = result.substring(66, 68);

            int spinnerSelection = Integer.decode("0x" + valueDataRate);
            if (spinnerSelection < spinnerArrayAdapter.getCount())
                spinnerDataRate.setSelection(spinnerSelection);

            // 2B Send Period
            valueSendPeriod = result.substring(68, 72);
            editTextSendPeriod.setText(String.valueOf(Integer.decode("0x" + valueSendPeriod)));

            // 2B Join Retries
            valueNoJoinRetries = result.substring(72, 76);
            editTextNoJoinRetries.setText(String.valueOf(Integer.decode("0x" + valueNoJoinRetries)));

            // 2B Join Retry Period 1
            valueJoinPeriodForRetries = result.substring(76, 80);
            editTextJoinPeriodForRetries.setText(String.valueOf(Integer.decode("0x" + valueJoinPeriodForRetries)));

            // 2B Join Retry Period 2
            valueJoinPeriodAfterRetries = result.substring(80, 84);
            editTextJoinPeriodAfterRetries.setText(String.valueOf(Integer.decode("0x" + valueJoinPeriodAfterRetries)));

            // 1B Accelerometer Threshold
            valueMovementThreshold = result.substring(84, 86);
            editTextMovementThreshold.setText(String.valueOf(Integer.decode("0x" + valueMovementThreshold)));

            // Other, 1 bits values
            String substr = result.substring(86, 88);
            String binary = Integer.toBinaryString(Integer.decode("0x" + substr));

//            binary.substring(0, 1) - Secure Join

            valueEnableAdr = binary.substring(1, 2);
            checkBoxEnableAdr.setChecked(stringToBoolean(valueEnableAdr));

            valueEnableMovementDetection = binary.substring(2, 3);
            checkBoxEnableMovementDetection.setChecked(stringToBoolean(valueEnableMovementDetection));

            valueEnableTempHum = binary.substring(3, 4);
            checkBoxEnableTempHum.setChecked(stringToBoolean(valueEnableTempHum));

            valueEnableAirPressure = binary.substring(4, 5);
            checkBoxEnableAirPressure.setChecked(stringToBoolean(valueEnableAirPressure));

            valueEnableVoc = binary.substring(5, 6);

            valueEnableAdc2 = binary.substring(6, 7);

            valueEnableAdc3 = binary.substring(7, 8);

            // 2B ADC Power On
            valueAdcPowerOn = result.substring(88, 92);

            // 1B Confirm Packet No
            valueConfirmPacket = result.substring(92, 94);
            editTextConfirmPacketNo.setText(String.valueOf(Integer.decode("0x" + valueConfirmPacket)));

            // 1B Fw Version
            valueFwVersion = result.substring(94, 96);

            Utils.saveStringParmSharedPref(valueEnableVoc, KEY_SP_SENSTICK_ENABLE_VOC, this);
            Utils.saveStringParmSharedPref(valueEnableAdc2, KEY_SP_SENSTICK_ENABLE_ADC2, this);
            Utils.saveStringParmSharedPref(valueEnableAdc3, KEY_SP_SENSTICK_ENABLE_ADC3, this);
            Utils.saveStringParmSharedPref(valueAdcPowerOn, KEY_SP_SENSTICK_ADC_POWER_ON, this);
            Utils.saveStringParmSharedPref(valueRegion, KEY_SP_SENSTICK_REGION, this);
            Utils.saveStringParmSharedPref(valueFwVersion, KEY_SP_SENSTICK_FW_VERSION, this);

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            valueGpsLatitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude());
            valueGpsLongitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());
            valueGpsAltitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getAltitude());
            editTextGpsLatitude.setText(valueGpsLatitude);
            editTextGpsLongitude.setText(valueGpsLongitude);
            editTextGpsAltitude.setText(valueGpsAltitude);

        } catch (Exception e) {
            Log(TAG, e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void parseDataHw30(String result) {

        // SAVE TAG
        valueStringTag = result;
        Log.d(TAG, "Parse: " + result);
        saveStringParmSharedPref(valueStringTag, KEY_SP_STRING_TAG, this);

        try {

            // 8B Device EUI
            valueDeviceEui = result.substring(0, 16);
            textViewDeviceEui.setText(valueDeviceEui);

            // 8B Application  EUI
            valueAppEui = result.substring(16, 32);
            editTextAppEui.setText(valueAppEui);

            // 16B APP Key
            valueAppKey = result.substring(32, 64);
            editTextAppKey.setText(valueAppKey);

            // 1B ADC3, ADC4, 1 bits values
            String substr = result.substring(64, 66);
            String binary = Integer.toBinaryString(Integer.decode("0x" + substr));
            binary = ("00000000" + binary).substring(binary.length());
            if (binary.length() > 2) {
                valueEnableAdc2 = binary.substring(binary.length() - 1, binary.length());
                valueEnableAdc3 = binary.substring(binary.length() - 2, binary.length() - 1);
            }

            // 1B Send Period
            valueSendPeriod = result.substring(66, 68);
            editTextSendPeriod.setText(String.valueOf(Integer.decode("0x" + valueSendPeriod)));

            // 1B MovementThreshold
            valueMovementThreshold = result.substring(68, 70);
            editTextMovementThreshold.setText(String.valueOf(Integer.decode("0x" + valueMovementThreshold)));

            // 1B Confirm Packet
            valueConfirmPacket = result.substring(70, 72);
            editTextConfirmPacket.setText(String.valueOf(Integer.decode("0x" + valueConfirmPacket)));

            // 1B Data Rate
            valueDataRate = result.substring(72, 74);
//            int spinnerSelection = Integer.decode("0x" + valueDataRate);
//            if (spinnerSelection < spinnerArrayAdapter.getCount())
//                spinnerDataRate.setSelection(spinnerSelection);
            Log.d(TAG, valueDataRate);

            // 1B Hw Version
            valueHwVersion = result.substring(74, 76);
            editTextHwVersion.setText(String.valueOf((float) Integer.decode("0x" + valueHwVersion) / 10));

            // 1B Fw Version
            valueFwVersion = result.substring(76, 78);
            editTextFwVersion.setText(String.valueOf((float) Integer.decode("0x" + valueFwVersion) / 10));

            // 2B ADC Power On
            valueAdcPowerOn = result.substring(78, 82);

            Utils.saveStringParmSharedPref(valueEnableAdc2, KEY_SP_SENSTICK_ENABLE_ADC2, this);
            Utils.saveStringParmSharedPref(valueEnableAdc3, KEY_SP_SENSTICK_ENABLE_ADC3, this);
            Utils.saveStringParmSharedPref(valueAdcPowerOn, KEY_SP_SENSTICK_ADC_POWER_ON, this);

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            valueGpsLatitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude());
            valueGpsLongitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());
            valueGpsAltitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getAltitude());
            editTextGpsLatitude.setText(valueGpsLatitude);
            editTextGpsLongitude.setText(valueGpsLongitude);
            editTextGpsAltitude.setText(valueGpsAltitude);


        } catch (Exception e) {
            Log(TAG, e.toString());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void actionButtonSend(View v) {

        if (flagHw30)
            composeMailHw30();
        else if(flagHw30_V3){
            composeMailHw30();
        }
        else
            composeMail();

    }

    void composeMail() {
        StringBuilder body = new StringBuilder("");

        body.append(getResources().getString(R.string.label_send_period) + ": " + Integer.decode("0x" + valueSendPeriod) + "\n");
        body.append(getResources().getString(R.string.label_join_retries) + ": " + Integer.decode("0x" + valueNoJoinRetries) + "\n");
        body.append(getResources().getString(R.string.label_join_period_for_retries) + ": " + Integer.decode("0x" + valueJoinPeriodForRetries) + "\n");
        body.append(getResources().getString(R.string.label_join_period_after_retries) + ": " + Integer.decode("0x" + valueJoinPeriodAfterRetries) + "\n");
        body.append(getResources().getString(R.string.label_packet_no) + ": " + Integer.decode("0x" + valueConfirmPacket) + "\n");

        body.append(getResources().getString(R.string.label_enable_adr) + ": " + stringToBoolean(valueEnableAdr) + "\n");
        body.append(getResources().getString(R.string.label_data_rate) + ": " + spinnerDataRate.getSelectedItem().toString() + "\n");

        body.append(getResources().getString(R.string.label_enable_movement_detection) + ": " + stringToBoolean(valueEnableMovementDetection) + "\n");
        body.append(getResources().getString(R.string.label_movement_threshold) + ": " + Integer.decode("0x" + valueMovementThreshold) + "\n");

        body.append(getResources().getString(R.string.label_enable_temp_hum) + ": " + stringToBoolean(valueEnableTempHum) + "\n");
        body.append(getResources().getString(R.string.label_enable_air_pressure) + ": " + stringToBoolean(valueEnableAirPressure) + "\n");

        body.append(getResources().getString(R.string.label_gps_lat) + ": " + valueGpsLatitude + "\n");
        body.append(getResources().getString(R.string.label_gps_lon) + ": " + valueGpsLongitude + "\n");
        body.append(getResources().getString(R.string.label_gps_alt) + ": " + valueGpsAltitude + "\n");


        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Senstick " + valueDeviceEui);
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());

        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    void composeMailHw30() {

        StringBuilder body = new StringBuilder("");

        body.append(getResources().getString(R.string.label_send_period) + ": " + Integer.decode("0x" + valueSendPeriod) + "\n");
        body.append(getResources().getString(R.string.label_movement_threshold) + ": " + Integer.decode("0x" + valueMovementThreshold) + "\n");
        body.append(getResources().getString(R.string.label_packet_no) + ": " + Integer.decode("0x" + valueConfirmPacket) + "\n");

        if (checkBoxEnableAdr.isChecked()) {
            body.append(getResources().getString(R.string.label_enable_adr) + ": " + stringToBoolean(valueEnableAdr) + "\n");
        } else {
            body.append(getResources().getString(R.string.label_data_rate) + ": " + spinnerDataRate.getSelectedItem().toString() + "\n");
        }

        body.append(getResources().getString(R.string.label_enable_adc1) + ": " + stringToBoolean(valueEnableAdc2) + "\n");
        body.append(getResources().getString(R.string.label_enable_adc2) + ": " + stringToBoolean(valueEnableAdc3) + "\n");
        body.append(getResources().getString(R.string.label_adc_power_on) + ": " + Integer.decode("0x" + valueAdcPowerOn) + "\n");

        body.append(getResources().getString(R.string.label_hw_version) + ": " + Integer.decode("0x" + valueHwVersion) / 10 + "\n");
        body.append(getResources().getString(R.string.label_fw_version) + ": " + Integer.decode("0x" + valueFwVersion) / 10 + "\n");


        body.append(getResources().getString(R.string.label_gps_lat) + ": " + valueGpsLatitude + "\n");
        body.append(getResources().getString(R.string.label_gps_lon) + ": " + valueGpsLongitude + "\n");
        body.append(getResources().getString(R.string.label_gps_alt) + ": " + valueGpsAltitude + "\n");
        body.append("Device EUI"+ ": " + valueDeviceEui + "\n");
        body.append("App EUI"+ ": " + valueAppEui + "\n");
        body.append("App key"+ ": " + valueAppKey + "\n");


        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_SUBJECT, "WaterSense " + valueDeviceEui);
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());

        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    public void actionButtonSave(View v) {

        Toast.makeText(context, getResources().getString(R.string.toast_tag_write),
                Toast.LENGTH_SHORT).show();
        flagWrite = true;
    }

    public void logoTapped(View view) {

        if (!flagEnableTapLogo)
            return;

        counterLogoTap++;

        if (counterLogoTap > 5 || DEVELOPER_VERSION_DEBUG) {
            // Start new Activity
            Intent intent;
            if (flagHw30)
                intent = new Intent(getApplicationContext(), HiddenActivity30.class);
            else if (flagHw30_V3)
                intent = new Intent(getApplicationContext(), HiddenActivity.class);
            else
                intent = new Intent(getApplicationContext(), HiddenActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(KEY_SP_STRING_TAG_SHARE, valueStringTag);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }

    }

    void locationStart() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };

        if (Build.VERSION.SDK_INT < 23) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, locationListener);
            //LocationManager.NETWORK_PROVIDER

//            Log(MOL_TAG,"Location Manager Started.");

        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {

                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDist, locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, locationListener);

                Location locationLastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
    }

    public void writeTag(Tag tag, String data) {

        if (!flagHw30) {
            data = getRandomString(4) + data;
            data = data + getRandomString(108 - data.length());
        }
        //if (flagHw30_V3){
        //    data = getRandomString(108 - data.length());
        //}

        flagWrite = false;

//        Log.e("TAG_W", data);

        NfcV myTag = NfcV.get(tag);
        try {
            myTag.connect();
            if (myTag.isConnected()) {
//                byte[] info = data.getBytes();
                byte[] info = hexStringToByteArray(data);
                int dataLength = info.length;
                if (data.length() / 4 <= 64) {
                    byte[] args = new byte[15];
                    // Write flags
                    args[0] = 0x20;
                    args[1] = 0x21;
                    // Get ID and write news ID in block
                    byte[] id = tag.getId();
                    for (int o = 0; o < 8; o++)
                        args[o + 2] = id[o];
//                    // RANDOM DATA, 64 blocks of 4 Bytes
                    /*
                    for (int i = 0; i < 64; i++) {
                        // OFFSET
                        args[10] = (byte) i;
                        // 4 Bytes of DATA
                        args[11] = getRandomByte();
                        args[12] = getRandomByte();
                        args[13] = getRandomByte();
                        args[14] = getRandomByte();
                        byte[] out = myTag.transceive(args);
//                        String out2 = bytesToHex(out);
                    }
                    */


                    for (int i = 0; i < dataLength / 4; i++) {
//                    for (int i = 0; i<64; i++) {
                        args[10] = (byte) (i + 6);
                        args[11] = info[(i * 4) + 0];
                        args[12] = info[(i * 4) + 1];
                        args[13] = info[(i * 4) + 2];
                        args[14] = info[(i * 4) + 3];
                        byte[] out = myTag.transceive(args);
//                        String out2 = bytesToHex(out);
//                        Log("Write2",out2);
//                        Log("Write2",String.valueOf(args));
                    }

                    Toast.makeText(context, getResources().getString(R.string.toast_tag_write_ok),
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {

            Toast toast = Toast.makeText(context, getResources().getString(R.string.toast_tag_write_nok), Toast.LENGTH_SHORT);
            //TextView v = toast.getView().findViewById(android.R.id.message);
            //v.setTextColor(Color.RED);
            toast.show();

            flagWrite = true;
        } finally {
            if (myTag != null) {
                try {
                    myTag.close();
                } catch (Exception e) {

                }
            }
        }

    }

    public void writeTag_V3(Tag tag, String data) {
        flagWrite = false;

//        Log.e("TAG_W", data);

        NfcV myTag = NfcV.get(tag);
        try {
            myTag.connect();
            if (myTag.isConnected()) {
//                byte[] info = data.getBytes();
                byte[] info = hexStringToByteArray(data);
                int dataLength = info.length;
                if (data.length() / 4 <= 64) {
                    byte[] args = new byte[15];
                    // Write flags
                    args[0] = 0x20;
                    args[1] = 0x21;
                    // Get ID and write news ID in block
                    byte[] id = tag.getId();
                    for (int o = 0; o < 8; o++)
                        args[o + 2] = id[o];
//                    // RANDOM DATA, 64 blocks of 4 Bytes
                    /*
                    for (int i = 0; i < 64; i++) {
                        // OFFSET
                        args[10] = (byte) i;
                        // 4 Bytes of DATA
                        args[11] = getRandomByte();
                        args[12] = getRandomByte();
                        args[13] = getRandomByte();
                        args[14] = getRandomByte();
                        byte[] out = myTag.transceive(args);
//                        String out2 = bytesToHex(out);
                    }
                    */


                    for (int i = 0; i < dataLength / 4; i++) {
//                    for (int i = 0; i<64; i++) {
                        args[10] = (byte) (i + 6);
                        args[11] = info[(i * 4) + 0];
                        args[12] = info[(i * 4) + 1];
                        args[13] = info[(i * 4) + 2];
                        args[14] = info[(i * 4) + 3];
                        byte[] out = myTag.transceive(args);
//                        String out2 = bytesToHex(out);
//                        Log("Write2",out2);
//                        Log("Write2",String.valueOf(args));
                    }

                    Toast.makeText(context, getResources().getString(R.string.toast_tag_write_ok),
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {

            Toast toast = Toast.makeText(context, getResources().getString(R.string.toast_tag_write_nok), Toast.LENGTH_SHORT);
            //TextView v = toast.getView().findViewById(android.R.id.message);
            //v.setTextColor(Color.RED);
            toast.show();

            flagWrite = true;
        } finally {
            if (myTag != null) {
                try {
                    myTag.close();
                } catch (Exception e) {

                }
            }
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    String getRandomString(int sizeOfRandomString) {
        String ALLOWED_CHARACTERS = "0123456789ABCDEF";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    String getRandomBitsString(int sizeOfRandomString) {
        String ALLOWED_CHARACTERS = "01";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    String prepareDataForWrite() {
        StringBuilder writeValue = new StringBuilder("");

        try {

            // 8B Device EUI
            writeValue.append(hexaDataLimitSize(8, valueDeviceEui));

            // 8B Application  EUI
            valueAppEui = String.valueOf(editTextAppEui.getText());
            writeValue.append(hexaDataLimitSize(8, valueAppEui));

            // 16B APP Key
            valueAppKey = String.valueOf(editTextAppKey.getText());
            writeValue.append(hexaDataLimitSize(16, valueAppKey));

            // 1B FREQ Region, HiddenActivity
            valueRegion = readStringParmSharedPref(KEY_SP_SENSTICK_REGION, this);
            writeValue.append(hexaDataLimitSize(1, valueRegion));

            // 1B Data Rate
            valueDataRate = String.valueOf(spinnerDataRate.getSelectedItemPosition());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueDataRate))));


            // 2B Period
            valueSendPeriod = String.valueOf(editTextSendPeriod.getText());
            writeValue.append(hexaDataLimitSize(2, Integer.toHexString(Integer.valueOf(valueSendPeriod))));

            // 2B Join Retries
            valueNoJoinRetries = String.valueOf(editTextNoJoinRetries.getText());
            writeValue.append(hexaDataLimitSize(2, Integer.toHexString(Integer.valueOf(valueNoJoinRetries))));

            // 2B Join Retry Period 1
            valueJoinPeriodForRetries = String.valueOf(editTextJoinPeriodForRetries.getText());
            writeValue.append(hexaDataLimitSize(2, Integer.toHexString(Integer.valueOf(valueJoinPeriodForRetries))));

            // 2B Join Retry Period 2
            valueJoinPeriodAfterRetries = String.valueOf(editTextJoinPeriodAfterRetries.getText());
            writeValue.append(hexaDataLimitSize(2, Integer.toHexString(Integer.valueOf(valueJoinPeriodAfterRetries))));

            // 1B Accelerometer Threshold
            valueMovementThreshold = String.valueOf(editTextMovementThreshold.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueMovementThreshold))));

            // 1 bit values
            // #1 Secure Join = TRUE
            StringBuilder otherBits = new StringBuilder("");
            otherBits.append("1");
            // #2 ADR
            valueEnableAdr = String.valueOf(checkBoxEnableAdr.isChecked() ? 1 : 0);
            otherBits.append(valueEnableAdr);
            // #3 Accelerometer
            valueEnableMovementDetection = String.valueOf(checkBoxEnableMovementDetection.isChecked() ? 1 : 0);
            otherBits.append(valueEnableMovementDetection);
            // #4 TempHum
            valueEnableTempHum = String.valueOf(checkBoxEnableTempHum.isChecked() ? 1 : 0);
            otherBits.append(valueEnableTempHum);
            // #5 AIR
            valueEnableAirPressure = String.valueOf(checkBoxEnableAirPressure.isChecked() ? 1 : 0);
            otherBits.append(valueEnableAirPressure);
            // #6 VOC, HiddenActivity
            valueEnableVoc = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_VOC, this);
            otherBits.append(valueEnableVoc);
            // #7 ADC1, HiddenActivity
            valueEnableAdc2 = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_ADC2, this);
            otherBits.append(valueEnableAdc2);
            // #8 ADC2, HiddenActivity
            valueEnableAdc3 = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_ADC3, this);
            otherBits.append(valueEnableAdc3);

            writeValue.append(hexaDataLimitSize(1, binaryToHex(otherBits.toString())));

            // 2B ADC Power On
            valueAdcPowerOn = readStringParmSharedPref(KEY_SP_SENSTICK_ADC_POWER_ON, this);
            writeValue.append(valueAdcPowerOn);
            Log.d(TAG, "prepareDataForWrite: " + valueDataRate);
            // 1B Confirm Packet On
            valueConfirmPacket = String.valueOf(editTextConfirmPacketNo.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueConfirmPacket))));

            // 1B Fw Version
            valueFwVersion = readStringParmSharedPref(KEY_SP_SENSTICK_FW_VERSION, this);
            writeValue.append(valueFwVersion);

            String str = new String(writeValue.toString().getBytes(), "UTF-8");
        } catch (Exception e) {

            Toast toast = Toast.makeText(context, getResources().getString(R.string.toast_tag_write_nok), Toast.LENGTH_SHORT);
            TextView v = toast.getView().findViewById(android.R.id.message);
            v.setTextColor(Color.RED);
            toast.show();
        }

        return writeValue.toString();
    }

    String prepareDataForWriteHw30() {
        StringBuilder writeValue = new StringBuilder("");

        try {
            // 2B Start Bytes
            writeValue.append("0000");

            // 8B Device EUI
            writeValue.append(hexaDataLimitSize(8, valueDeviceEui));

            // 8B Application  EUI
            valueAppEui = String.valueOf(editTextAppEui.getText());
            writeValue.append(hexaDataLimitSize(8, valueAppEui));

            // 16B APP Key
            valueAppKey = String.valueOf(editTextAppKey.getText());
            writeValue.append(hexaDataLimitSize(16, valueAppKey));

            // 1 bit values
            StringBuilder otherBits = new StringBuilder("");
            // #1 ADC3
            valueEnableAdc2 = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_ADC2, this);
            // #2 ADC4
            valueEnableAdc3 = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_ADC3, this);
            // #3 - #8
            otherBits.append("000000");
            otherBits.append(valueEnableAdc3);
            otherBits.append(valueEnableAdc2); // LSB
            writeValue.append(hexaDataLimitSize(1, binaryToHex(otherBits.toString())));

            // 1B Send Period
            valueSendPeriod = String.valueOf(editTextSendPeriod.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueSendPeriod))));
            Log.d(TAG, valueSendPeriod);

            // 1B MovementThreshold
            valueMovementThreshold = String.valueOf(editTextMovementThreshold.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueMovementThreshold))));

            // 1B Confirm Packet
            valueConfirmPacket = String.valueOf(editTextConfirmPacket.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueConfirmPacket))));

            // 1B Data Rate
            if (checkBoxEnableAdr.isChecked()) {
                valueDataRate = String.valueOf(128 + spinnerDataRate.getSelectedItemPosition());
            } else {
                valueDataRate = String.valueOf(spinnerDataRate.getSelectedItemPosition());
            }
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueDataRate))));
            Log.d(TAG, valueDataRate);

            // 1B Hw Version
            valueHwVersion = String.valueOf(editTextHwVersion.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Math.round(Float.valueOf(valueHwVersion) * 10))));

            // 1B Fw Version
            valueFwVersion = String.valueOf(editTextFwVersion.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Math.round(Float.valueOf(valueFwVersion) * 10))));

            // 2B ADC Power On
            valueAdcPowerOn = readStringParmSharedPref(KEY_SP_SENSTICK_ADC_POWER_ON, this);
            writeValue.append(valueAdcPowerOn);

            // 1B Stop Byte
            writeValue.append("FF");

            // 1B Frequency region
            writeValue.append(hexaDataLimitSize(1, valueRegion));
            Log.d(TAG, "Write: " + writeValue);
        } catch (Exception e) {
            Toast toast = Toast.makeText(context, getResources().getString(R.string.toast_tag_write_nok), Toast.LENGTH_SHORT);
            TextView v = toast.getView().findViewById(android.R.id.message);
            v.setTextColor(Color.RED);
            toast.show();
        }

        return writeValue.toString();
    }

    String hexaDataLimitSize(int bytes, String input) {
        String limit = String.valueOf(bytes * 2);

        String output = String.format("%" + limit + "s", input).replace(' ', '0');

        return output;
    }

    String binaryToHex(String bin) {
        return String.format("%2X", Long.parseLong(bin, 2));
    }


    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    void parseDataHw30_V3(String result) {

        // SAVE TAG
        valueStringTag = result;
        Log.d(TAG, "parse: " + result);
        Log.d(TAG, String.valueOf(result.length()));
        saveStringParmSharedPref(valueStringTag, KEY_SP_STRING_TAG, this);
        try {

            // 1B family ID
            valueFamilyID = result.substring(0, 2);
            Log.d(TAG, valueFamilyID );

            // 1B product ID
            valueProductID = result.substring(2, 4);
            Log.d(TAG, valueProductID);

            //try {
            // 1B HW version
            valueHwVersion = result.substring(4, 6);
            editTextHwVersion.setText(String.valueOf((float)Integer.decode("0x" + valueHwVersion) / 10));
            Log.d(TAG, valueHwVersion);
        //} catch (NumberFormatException e) {
        //    Log.e(TAG, "Error parsing HW version", e);
        //} catch (Exception e) {
        //    Log.e(TAG, "Unknown error occurred", e);
        //}

            // 1B FW version
            valueFwVersion = result.substring(6,8);
            editTextFwVersion.setText(String.valueOf((float) Integer.decode("0x" + valueFwVersion) / 10));
            Log.d(TAG, valueFwVersion);

            // 4B reserved bytes
            valueReservedBytes = result.substring(8,16);
            Log.d(TAG, valueReservedBytes);

            // 8B Device EUI
            valueDeviceEui = result.substring(16, 32);
            textViewDeviceEui.setText(valueDeviceEui);
            Log.d(TAG, valueDeviceEui);

            // 8B Application  EUI
            valueAppEui = result.substring(32, 48);
            editTextAppEui.setText(valueAppEui);
            Log.d(TAG, valueAppEui);

            // 16B APP Key
            valueAppKey = result.substring(48, 80);
            editTextAppKey.setText(valueAppKey);
            Log.d(TAG, valueAppKey);

            // 1B Data rate
            valueDataRate = result.substring(80,82);
            int spinnerSelection = Integer.decode("0x" + valueDataRate);
            if (spinnerSelection < spinnerArrayAdapter.getCount()){
                spinnerDataRate.setSelection(spinnerSelection);
            }
            Log.d(TAG, valueDataRate);

            // 1B Frequency region
            valueRegion = result.substring(82, 84);
            Log.d(TAG, valueRegion);

            // 1B HYBRID ENABLE + AS923 OFFSET + MASK 0-1
            String substrHybrid = result.substring(84, 86);
            String binaryHybrid = Integer.toBinaryString(Integer.decode("0x" + substrHybrid));
            binaryHybrid = ("00000000" + binaryHybrid).substring(binaryHybrid.length());
            if (binaryHybrid.length() > 2) {
                valueHybridEnable = binaryHybrid.substring(binaryHybrid.length() - 8, binaryHybrid.length() - 7);
                valueAS923OFFSET = binaryHybrid.substring(binaryHybrid.length() - 7, binaryHybrid.length() - 4);
                valueMask1 = binaryHybrid.substring(binaryHybrid.length() - 4, binaryHybrid.length() - 2);
                valueMask0 = binaryHybrid.substring(binaryHybrid.length() - 2, binaryHybrid.length());
            }
            Log.d(TAG, substrHybrid);

            // 1B Hybrid mask 2-5
            String substrHybridMask = result.substring(86, 88);
            String binaryHybridMask = Integer.toBinaryString(Integer.decode("0x" + substrHybridMask));
            binaryHybridMask = ("00000000" + binaryHybridMask).substring(binaryHybridMask.length());
            if (binaryHybridMask.length() > 2) {
                valueMask5 = binaryHybridMask.substring(binaryHybridMask.length() - 8, binaryHybridMask.length() - 6);
                valueMask4 = binaryHybridMask.substring(binaryHybridMask.length() - 6, binaryHybridMask.length() - 4);
                valueMask3 = binaryHybridMask.substring(binaryHybridMask.length() - 4, binaryHybridMask.length() - 2);
                valueMask2 = binaryHybridMask.substring(binaryHybridMask.length() - 2, binaryHybridMask.length());
            }
            Log.d(TAG, substrHybridMask);

            // 1B Send Period
            valueSendPeriod = result.substring(88, 90);
            editTextSendPeriod.setText(String.valueOf(Integer.decode("0x" + valueSendPeriod)));
            Log.d(TAG, valueSendPeriod);

            // 1B Confirm Packet
            valueConfirmPacket = result.substring(90, 92);
            editTextConfirmPacket.setText(String.valueOf(Integer.decode("0x" + valueConfirmPacket)));
            Log.d(TAG, valueConfirmPacket);

            // 1B MovementThreshold
            valueMovementThreshold = result.substring(92, 94);
            editTextMovementThreshold.setText(String.valueOf((int)Integer.decode("0x" + valueMovementThreshold)));
            Log.d(TAG, valueMovementThreshold);

            // 1B ADC3, ADC4, 1 bits values
            String substr = result.substring(94, 96);
            String binary = Integer.toBinaryString(Integer.decode("0x" + substr));
            binary = ("00000000" + binary).substring(binary.length());
            if (binary.length() > 2) {
                valueEnableAdc2 = binary.substring(binary.length() - 1, binary.length());
                valueEnableAdc3 = binary.substring(binary.length() - 2, binary.length() - 1);
            }
            Log.d(TAG, substr);

            // 2B ADC Power On
            valueAdcPowerOn = result.substring(96, 100);
            Log.d(TAG, valueAdcPowerOn);

            Utils.saveStringParmSharedPref(valueEnableAdc2, KEY_SP_SENSTICK_ENABLE_ADC2, this);
            Utils.saveStringParmSharedPref(valueEnableAdc3, KEY_SP_SENSTICK_ENABLE_ADC3, this);
            Utils.saveStringParmSharedPref(valueAdcPowerOn, KEY_SP_SENSTICK_ADC_POWER_ON, this);
            Utils.saveStringParmSharedPref(valueFwVersion, KEY_SP_SENSTICK_FW_VERSION, this);
            Utils.saveStringParmSharedPref(valueHwVersion, KEY_SP_SENSTICK_HW_VERSION, this);

            // 1B 0xFF
            valueNFCChanges = result.substring(100, 102);
            Log.d(TAG, valueNFCChanges);

            // 1B device status
            valueDeviceStatus = result.substring(102, 104);
            Log.d(TAG, valueDeviceStatus);

            // 2B temperature + humidity
            valueEnableTempHum = result.substring(104, 108);
            checkBoxEnableTempHum.setChecked(stringToBoolean(valueEnableTempHum));
            Log.d(TAG, valueEnableTempHum);

            // 2B air pressure MSB + air pressure LSB
            valueEnableAirPressure = result.substring(108, 112);
            checkBoxEnableAirPressure.setChecked(stringToBoolean(valueEnableAirPressure));
            Log.d(TAG, " test"+ valueEnableAirPressure);


            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            valueGpsLatitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude());
            valueGpsLongitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());
            valueGpsAltitude = String.format(Locale.US, "%.2f", locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getAltitude());
            editTextGpsLatitude.setText(valueGpsLatitude);
            editTextGpsLongitude.setText(valueGpsLongitude);
            editTextGpsAltitude.setText(valueGpsAltitude);


        } catch (Exception e) {
            Log(TAG, "error" + e);
        }
    }
    String prepareDataForWriteHw30_V3() {
        StringBuilder writeValue = new StringBuilder("");
        Log.d(TAG, "write: ");
        try {

           //// 1B family ID
           //writeValue.append(hexaDataLimitSize(1, valueFamilyID));
           //Log.d(TAG, valueFamilyID);

           //// 1B product ID
           //writeValue.append(hexaDataLimitSize(1, valueProductID));
           //Log.d(TAG, valueProductID);

           //// 1B Hw Version
           //valueHwVersion = readStringParmSharedPref(KEY_SP_SENSTICK_HW_VERSION, this);
           //writeValue.append(hexaDataLimitSize(1, valueHwVersion));
           //Log.d(TAG, valueHwVersion);
//
           //// 1B Fw Version
           //valueFwVersion = readStringParmSharedPref(KEY_SP_SENSTICK_FW_VERSION, this);
           //writeValue.append(hexaDataLimitSize(1, valueFwVersion));
           //Log.d(TAG, valueFwVersion);
//
           //// 4B reserved
           ////writeValue.append("00000000");
           //writeValue.append(hexaDataLimitSize(4, valueReservedBytes));
           //Log.d(TAG, valueReservedBytes);

            // 8B Device EUI
            writeValue.append(hexaDataLimitSize(8, valueDeviceEui));
            Log.d(TAG, valueDeviceEui);

            // 8B Application  EUI
            valueAppEui = String.valueOf(editTextAppEui.getText());
            writeValue.append(hexaDataLimitSize(8, valueAppEui));
            Log.d(TAG, valueAppEui);

            // 16B APP Key
            valueAppKey = String.valueOf(editTextAppKey.getText());
            writeValue.append(hexaDataLimitSize(16, valueAppKey));
            Log.d(TAG, valueAppKey);

            // 1B Data Rate
            if (checkBoxEnableAdr.isChecked()) {
                valueDataRate = String.valueOf(128 + spinnerDataRate.getSelectedItemPosition());
            } else {
                valueDataRate = String.valueOf(spinnerDataRate.getSelectedItemPosition());
            }

            int intValue = Integer.parseInt(valueDataRate);
            String hexValue = Integer.toHexString(intValue);
            writeValue.append(hexaDataLimitSize(1, hexValue));

            Log.d(TAG, valueDataRate);

            //1B frequency region
            writeValue.append(hexaDataLimitSize(1, valueRegion));
            Log.d(TAG, valueRegion);

            // 1B hybrid enable + AS923 offset + mask 0-1
            StringBuilder hybridBits = new StringBuilder("");
            hybridBits.append(valueAS923OFFSET);
            hybridBits.append(valueMask1);
            hybridBits.append(valueMask0);
            writeValue.append(hexaDataLimitSize(1, binaryToHex(hybridBits.toString())));
            Log.d(TAG, binaryToHex(String.valueOf(hybridBits)));

            // 1B Hybrid mask 2-5
            StringBuilder maskBits = new StringBuilder("");
            maskBits.append(valueMask5);
            maskBits.append(valueMask4);
            maskBits.append(valueMask3);
            maskBits.append(valueMask2);
            writeValue.append(hexaDataLimitSize(1, binaryToHex(maskBits.toString())));
            Log.d(TAG, binaryToHex(String.valueOf(maskBits)));

            // 1B Send Period
            valueSendPeriod = String.valueOf(editTextSendPeriod.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueSendPeriod))));
            Log.d(TAG, valueSendPeriod);

            // 1B Confirm Packet
            valueConfirmPacket = String.valueOf(editTextConfirmPacket.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueConfirmPacket))));
            Log.d(TAG, valueConfirmPacket);

            // 1B Movement Threshold
            valueMovementThreshold = String.valueOf(editTextMovementThreshold.getText());
            writeValue.append(hexaDataLimitSize(1, Integer.toHexString(Integer.valueOf(valueMovementThreshold))));
            Log.d(TAG, valueMovementThreshold);

            // 1B ADC enable
            StringBuilder otherBits = new StringBuilder("");
            // #1 ADC2
            valueEnableAdc2 = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_ADC2, this);
            // #2 ADC3
            valueEnableAdc3 = readStringParmSharedPref(KEY_SP_SENSTICK_ENABLE_ADC3, this);
            // #3 - #8
            otherBits.append("000000");
            otherBits.append(valueEnableAdc3); // MSB
            otherBits.append(valueEnableAdc2); // LSB
            writeValue.append(hexaDataLimitSize(1, binaryToHex(otherBits.toString())));
            Log.d(TAG, binaryToHex(String.valueOf(otherBits)));

            // 2B ADC Power On
            valueAdcPowerOn = readStringParmSharedPref(KEY_SP_SENSTICK_ADC_POWER_ON, this);
            writeValue.append(hexaDataLimitSize(2, valueAdcPowerOn));
            Log.d(TAG, valueAdcPowerOn);

            // 1B 0xFF
            writeValue.append("FF");

            // 1B device status
            //writeValue.append(hexaDataLimitSize(1, valueDeviceStatus));
            //Log.d(TAG, valueDeviceStatus);
//
            //// 2B temperature + humidity
            //writeValue.append(hexaDataLimitSize(1, valueEnableTempHum));
            //Log.d(TAG, valueEnableTempHum);
//
            //// 2B air pressure MSB + air pressure LSB
            //writeValue.append(hexaDataLimitSize(1, valueEnableAirPressure));
            //Log.d(TAG, valueEnableAirPressure);
            Log.d(TAG, "write: " + writeValue);

        } catch (Exception e) {
            Toast toast = Toast.makeText(context, getResources().getString(R.string.toast_tag_write_nok), Toast.LENGTH_SHORT);
            TextView v = toast.getView().findViewById(android.R.id.message);
            v.setTextColor(Color.RED);
            toast.show();
        }
        return writeValue.toString();
    }
}