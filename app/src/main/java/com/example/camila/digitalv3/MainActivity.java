package com.example.camila.digitalv3;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //widgets pantalla
    Button btnPaired;
    ListView devicelist;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;//punto de entrada de toda interaccion de bluetooth
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "EXTRA";//Dirección del dispositivo

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EXTRA_ADDRESS = BluetoothAdapter.getDefaultAdapter().getAddress().toString();

        //asignar botones a variables
        btnPaired = (Button)findViewById(R.id.button);
        devicelist = (ListView)findViewById(R.id.listView);

        //asignamos adaptador bluetooth para poder usarlo
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null)
        {
            Toast.makeText(getApplicationContext(), "El dispositivo Bluetooth no esta disponible.", Toast.LENGTH_LONG).show();
            //si no hay adaptador bluetooth se cierra la app

            finish();
        }
        else if(!myBluetooth.isEnabled())
        {
            //si hay bluetooth pero no esta activado
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //Intent i = new Intent(MainActivity.this, CameraActivity.class);
                //Change the activity.
                //i.putExtra(EXTRA_ADDRESS, address); //esto se recibe en la actividad camera activity
                //startActivity(i);
                pairedDevicesList();
            }
        });

    }


    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();

        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No hay dispositivos vinculados.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener);

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Intent para empezar otra actividad
            Intent i = new Intent(MainActivity.this, CameraActivity.class);
            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //esto se recibe en la actividad camera activity
            startActivity(i);
        }
    };
}