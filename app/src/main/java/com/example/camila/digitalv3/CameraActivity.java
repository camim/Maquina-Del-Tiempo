package com.example.camila.digitalv3;import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ZoomControls;


public class CameraActivity extends Activity implements PictureCallback, SurfaceHolder.Callback, SensorEventListener {

    public static final String EXTRA_CAMERA_DATA = "camera_data";

    private static final String KEY_IS_CAPTURING = "is_capturing";
    //manejo camara

    private SurfaceView mCameraPreview;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Button mCaptureImageButton;
    private byte[] mCameraData;
    private boolean mIsCapturing;
    private Bitmap mCameraBitmap;
    private File imageFile;

    //manejo conexion bluetooth
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //manejo para peticiones bluetooth
    final int handlerState = 0;
    private ConnectedThread mConnectedThread;
    Handler bluetoothIn;
    boolean processingImage = false;
    public static String PHOTOPATH = "PATH";

    //manejo sensores
    private SensorManager managerProx;
    private Sensor proximidad;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!isBtConnected) {//Si no esta conectado entonces
            Intent newint = getIntent();
            address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS); //recibimos los datos del intent de main activity

            //Se crea un handle para manejar los mensajes.
            bluetoothIn = new Handler() {
                public void handleMessage(android.os.Message msg) {

                    if(!processingImage) {//Si la foto no esta siendo procesada entonces
                        String readMessage = (String) msg.obj;  //lee mensajes del hilo que escucha al bluetooth
                        processingImage = true;
                        captureImage();
                    }

                }
            };

          new ConnectBT().execute();
        }

        managerProx = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximidad = managerProx.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        mCameraPreview = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = mCameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mCaptureImageButton = (Button) findViewById(R.id.capture_image_button);
        mCaptureImageButton.setOnClickListener(mCaptureImageButtonClickListener);

        final Button doneButton = (Button) findViewById(R.id.done_button);
        doneButton.setOnClickListener(mDoneButtonClickListener);

        mIsCapturing = true;
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    /*Al crearse el objeto de la clase AsyncTask se llama, para empezar, a su primer  onPreExecute()
    que se ejecuta sobre el hilo principal. Al terminar este AsyncTask crea un hilo secundario y ejecuta
    su trabajo dentro de doInBackground(). Durante la ejecuciÃ³n en segundo plano podemos realizar llamadas
    al hilos principal (por ejemplo, para incrementar la barra de carga a medida que se ejecuta el hilo en
    segundo plano) desde doInBackground(). Al terminar de ejecutarse
    doInBackground() se llama de inmediato a onPostExecute(), y se da por acabado a este hilo en segundo plano.*/
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(CameraActivity.this, "Conectando", "Por favor, espere.");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //se muestra el progress dialog en la pantalla y mientras se hace esto
        {
            try {
                //btSocket es de clae BluetoothSocket
                if (btSocket == null || !isBtConnected) {//Si no hay hilo o no esta conectado entonces
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//obtener dispositivo bluetooth
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//conectar y verificar disponibilidad
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();

                    //empzamos hilo, de clase Connected Thread, que escucha peticiones
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //despues de terminar doInBackground hace esto
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Conexion fallida. Intente nuevamente.");
                finish();
            } else {
                msg("Conectado!");
                isBtConnected = true;
            }
            progress.dismiss(); // sacamos el progress dialog
        }

    }

    //para escuchar constantemente request de la placa
    private class ConnectedThread extends Thread {
        BufferedReader reader;

        public ConnectedThread(BluetoothSocket socket) {

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
            }

        }

        public void run() {

            while (true) { //habria que cambiarlo por una variable asi corta cuando la actividad esta pausada
                try {
                   if(!processingImage) {
                        String readMessage = reader.readLine();
                        // enviamos mensaje recibido por bluetooth al handler
                        bluetoothIn.obtainMessage(handlerState, readMessage).sendToTarget();
                   }

                } catch (IOException e) {
                    break;
                }
            }
        }
    }


    private OnClickListener mCaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            captureImage();
        }
    };

    private OnClickListener mRecaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setupImageCapture();
        }
    };

    private OnClickListener mDoneButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            disconnect();
        }
    };

    private void disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish();

    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(KEY_IS_CAPTURING, mIsCapturing);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mIsCapturing = savedInstanceState.getBoolean(KEY_IS_CAPTURING, mCameraData == null);
        if (mCameraData == null)
            setupImageCapture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        processingImage=false;


        managerProx.registerListener(this, proximidad, SensorManager.SENSOR_DELAY_UI);

        if (camera == null) {
            try {
                camera = Camera.open();
                camera.setPreviewDisplay(mCameraPreview.getHolder());
                if (mIsCapturing) {
                    camera.startPreview();
                }
            } catch (Exception e) {
                Toast.makeText(CameraActivity.this, "No se pudo abrir la camara.", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        managerProx.unregisterListener(this);

        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {

            try {
                camera.setPreviewDisplay(holder);
                /*Arreglo de la orientacion de la camara*/
                Camera.Parameters parameters = camera.getParameters();
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    camera.setDisplayOrientation(90);
                }
                camera.setParameters(parameters);
                /*fin de arreglo*/

                //AUTO FOCO Y ZOOM//
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode("continuous-picture");
                camera.setParameters(params);
                params.setZoom((int) ((int)params.getMaxZoom() / 2.1));
                camera.setParameters(params);
                //FIN DE AUTOFOCO Y ZOOM//


                if (mIsCapturing) {
                    camera.startPreview();
                }
            } catch (IOException e) {
                Toast.makeText(CameraActivity.this, "Vista previa no disponible. Reinicie la conexion..", Toast.LENGTH_LONG).show();
            }


        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private void captureImage() {
        camera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        mCameraData = data;
        new SaveImageTask().execute();
        setupImageCapture();
    }

    private class SaveImageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(CameraActivity.this, "Guardando foto", "Por favor. Paciencia!");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            Date date = new Date();
            String stamp = new Timestamp(date.getTime()).toString();

            try {
                // convertir datos de la camara a un bitmap
                Bitmap loadedImage = null;
                Bitmap rotatedBitmap = null;
                loadedImage = BitmapFactory.decodeByteArray(mCameraData, 0,
                        mCameraData.length);

                // rotamos imagen (probar)
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90);//ACA
                rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                        loadedImage.getWidth(), loadedImage.getHeight(),
                        rotateMatrix, false);

                rotatedBitmap = ChangeBitmapMutability.convertToMutable(rotatedBitmap);

                //convertimos la imagen a positivo (o negativo si saque una foto en positivo)
                rotatedBitmap = convertImage(rotatedBitmap);

                //obtenemos info del almacenamiento y de la carpeta donde se guarda la foto
                String state = Environment.getExternalStorageState();
                File folder = null;
                if (state.contains(Environment.MEDIA_MOUNTED)) {
                    folder = new File(Environment
                            .getExternalStorageDirectory() + "/Digitalizador");
                } else {
                    folder = new File(Environment
                            .getExternalStorageDirectory() + "/Digitalizador");
                }

                boolean success = true;
                if (!folder.exists()) {
                    success = folder.mkdirs();
                }
                if (success) {

                    imageFile = new File(folder.getAbsolutePath()
                            + File.separator
                            + stamp
                            + "Imagen.jpg");

                    imageFile.createNewFile(); //crea archivo pero no tiene nada

                } else {
                    Toast.makeText(getBaseContext(), "Imagen no guardada",
                            Toast.LENGTH_SHORT).show();
                    return null;
                }

                ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                // guardamos imagen en la galeria en el archivo imageFile antes creado
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);

                FileOutputStream fout = new FileOutputStream(imageFile);
                fout.write(ostream.toByteArray());
                fout.close();
                ContentValues values = new ContentValues();

                values.put(MediaStore.Images.Media.DATE_TAKEN,
                        System.currentTimeMillis());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.DATA,
                        imageFile.getAbsolutePath());

                CameraActivity.this.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


                processingImage = false;

                /*PARA MOSTRAR LA FOTO*/
                PHOTOPATH = imageFile.getAbsolutePath();
                Intent w = new Intent(CameraActivity.this, Preview.class);
                w.putExtra(PHOTOPATH, PHOTOPATH);//esto se recibe en la actividad preview
                startActivity(w);
                /********************/

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        public Bitmap convertImage(Bitmap rotatedBitmap)
        {
            for (int x = 0; x < rotatedBitmap.getWidth(); x++) {
                for (int y = 0; y < rotatedBitmap.getHeight(); y++) {
                    int pixel = rotatedBitmap.getPixel(x, y);
                    int alpha = Color.alpha(pixel);
                    int rojo = 255 - Color.red(pixel);
                    int azul = 255 - Color.blue(pixel);
                    int verde = 255 - Color.green(pixel);
                    rotatedBitmap.setPixel(x, y, Color.argb(alpha, rojo, verde, azul));
                }
            }
            return rotatedBitmap;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progress.dismiss();
        }

    }


    private void setupImageCapture() {
        mCameraPreview.setVisibility(View.VISIBLE);
        camera.startPreview();
        mCaptureImageButton.setText(R.string.capture_image);
        mCaptureImageButton.setOnClickListener(mCaptureImageButtonClickListener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY && event.values[0] >= -4.0 && event.values[0] <= 4.0) {
            Intent dialogIntent = new Intent(CameraActivity.this, AlertDialogActivity.class);
            startActivity(dialogIntent);

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(2000);
        }

    }

    @Override
    public void onBackPressed() {
        //Primero se descontecta, al apretar atras y vuelve a la actividad anterior
        disconnect();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
