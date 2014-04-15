package com.example.bluetooth2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
 
import com.example.bluetooth2.R;
 
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
 
public class MainActivity extends Activity {
  
  private static final String TAG = "bluetooth2";
   
  Button btnOn, btnOff;
  TextView txtArduino;
  Handler h;
  
  final int RECIEVE_MESSAGE = 1;		// Status  for Handler
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder sb = new StringBuilder();
  
  private ConnectedThread mConnectedThread;
   
  // SPP UUID service
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
  // MAC-address of Bluetooth module (you must edit this line)
  private static String address = "00:06:66:4F:B8:12";
   

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
 
    setContentView(R.layout.activity_main);
 
    btnOn = (Button) findViewById(R.id.btnOn);					
    btnOff = (Button) findViewById(R.id.btnOff);				
    txtArduino = (TextView) findViewById(R.id.txtArduino);		
    
    h = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
            case RECIEVE_MESSAGE:													// if receive massage
            	
            	byte[] readBuf = (byte[]) msg.obj;
            	String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
            	
            	sb.append(strIncom);	
            	// append string
            	int endOfLineIndex = sb.indexOf("\r\n");							
            	if (endOfLineIndex > 0) { 											
            		String sbprint = sb.substring(0, endOfLineIndex);				
                    sb.delete(0, sb.length());										
                	txtArduino.setText("Data from sensor: " + sbprint); 	       
                	btnOff.setEnabled(true);
                	btnOn.setEnabled(true); 
                }
            	
            	break;
    		}
        };
	};
     
    btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
    checkBTState();
 
    btnOn.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
    	btnOn.setEnabled(false);
    	mConnectedThread.write("1");	// Send "1" via Bluetooth

      }
    });
 
    btnOff.setOnClickListener(new OnClickListener() {
      
     public void onClick(View v) {
    	 
    	btnOff.setEnabled(false);  
    	mConnectedThread.write("0");	// Send "0" via Bluetooth

     }
     
    });
    
  }
  
  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      
	  if(Build.VERSION.SDK_INT >= 10){
          try {
              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
              return (BluetoothSocket) m.invoke(device, MY_UUID);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
      
  }
   
  @Override
  public void onResume() {
    
	super.onResume();
 
   
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
   
	try {
		btSocket = createBluetoothSocket(device);
	} catch (IOException e) {
		errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
	}
    
    /*try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }*/
   

    btAdapter.cancelDiscovery();
   
    Toast.makeText(getBaseContext(), "...Connecting...", Toast.LENGTH_SHORT).show();
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
      Toast.makeText(getBaseContext(), "...Connection ok..", Toast.LENGTH_SHORT).show();
      Log.d(TAG, "....Connection ok...");
    } catch (IOException e) {
      
    	try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
     

    Log.d(TAG, "...Create Socket...");
   
    mConnectedThread = new ConnectedThread(btSocket);
    mConnectedThread.start();
    
  }
 
  @Override
  public void onPause() {
    super.onPause();
 
    Log.d(TAG, "...In onPause()...");
  
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
    
  }
   
  private void checkBTState() 
  {

    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
        
      }
    }
  }
 
  private void errorExit(String title, String message){
    
	Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
    
  }
 
  private class ConnectedThread extends Thread {
	    
	  	private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        
	    	InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        try {
	            
	        	tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	            
	        }catch(IOException e){ 
	        	
	        }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run(){
	        
	    	byte[] buffer = new byte[256];  
	        int bytes; 

	        while(true){
	        	
	        	try{
	            	        		
	                bytes = mmInStream.read(buffer);	
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();	
	            
	        	}catch (IOException e) {
	                break;
	            }
	        	
	        }
	    }
	 
	    public void write(String message) {
	    	Log.d(TAG, "...Data to send: " + message + "...");
	    	byte[] msgBuffer = message.getBytes();
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
	          }
	    }
	    
	}
}