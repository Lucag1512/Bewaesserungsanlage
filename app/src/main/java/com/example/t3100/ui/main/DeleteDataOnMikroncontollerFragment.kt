package com.example.t3100.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t3100.MainActivity
import com.example.t3100.R
import com.example.t3100.adapter.BluetoothDevicesAdapter
import com.example.t3100.data.ParsedDelete
import com.example.t3100.databinding.FragmentDeletedataonmikrocontrollerBinding
import com.example.t3100.viewmodel.BluetoothViewModel
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class DeleteDataOnMikroncontollerFragment : Fragment() {

    //Companion object verwendung der Variablen in Klasse
    companion object {
        fun newInstance() = DeleteDataOnMikroncontollerFragment()


        //Deklaration welche Permissions bei welcher SDK benötigt werden
        private val REQUIRED_PERMISSIONS =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN)
            }else{
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }

        /* Aktuell nicht verwendet
        // Defines several constants used when transmitting messages between the
        // service and the UI.
        val MESSAGE_READ: Int = 0
        val MESSAGE_WRITE: Int = 1
        val MESSAGE_TOAST: Int = 2
        */

    }

    private lateinit var viewModel: BluetoothViewModel
    private lateinit var binding: FragmentDeletedataonmikrocontrollerBinding

    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var adapter: BluetoothDevicesAdapter

    //Permissions für Appfunktionalität anfordern
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all{
            it.value == true
        }
        //Sind alle Permissions gegeben wird setupBT gestartet
        if(granted){
            setupBluetooth()
        } else{
            //TODO: Schleife falls Nutzer Permissions ablehnt
        }
    }

    //Prüfung ist BT eingeschaltet worden
    private val bluetoothRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "All permissions set", Toast.LENGTH_LONG).show()
            } else {
                //TODO: BT wird für APP benötigt
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BluetoothViewModel::class.java)
    }

    //Binding für einfacheren Zugriff auf Buttons, TV aus xml File
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.title = "Daten auf Mikrocontroller löschen"
        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_deletedataonmikrocontroller, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Prüfung sind alle Permissions gegeben wenn nicht User auffordern
        if(checkPermissionsGranted()){
            setupBluetooth()
        }else{
            permissionRequest.launch(REQUIRED_PERMISSIONS)
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel


        adapter = BluetoothDevicesAdapter(viewModel.bluetoothDevices, object :
            BluetoothDevicesAdapter.ItemClickListener {
            override fun onItemClick(device: BluetoothDevice){
                stopSearchDevices()

                val delete = ParsedDelete(true)

                val gson = Gson()
                val manualWateringElementsJson = gson.toJson(delete)
                ConnectThread(device).connectAndSend(manualWateringElementsJson)

                val alertDialog = AlertDialog.Builder(requireContext()).create()
                alertDialog.setTitle("Information")
                alertDialog.setMessage("Daten erfolgreich gelöscht")
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "OK",
                    object: DialogInterface.OnClickListener{
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            p0?.dismiss()
                            findNavController().popBackStack()
                        }
                    })
                alertDialog.show()


            }
        })

        binding.rvBluetoothDevices.adapter = adapter
        binding.rvBluetoothDevices.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            (binding.rvBluetoothDevices.layoutManager as LinearLayoutManager).getOrientation()
        )
        binding.rvBluetoothDevices.addItemDecoration(dividerItemDecoration)

        //Verfügbare BT Geräte in der Nähe suchen
        binding.btnStartSearch.setOnClickListener {
            startSearchDevices()
        }

        binding.btnEndSearch.setOnClickListener {
            stopSearchDevices()
        }

    }

    override fun onResume() {
        super.onResume()

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)
        //LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receiver, filter)
    }

    //BT reciever deaktivieren bei Schließen der App
    override fun onPause() {
        super.onPause()
        // Don't forget to unregister the ACTION_FOUND receiver.
        //LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receiver)
        requireActivity().unregisterReceiver(receiver)
    }

    //Prüfung sind alle Permissions gegeben
    private fun checkPermissionsGranted() = REQUIRED_PERMISSIONS.all {permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupBluetooth(){
        //Adapter anlegen, entspricht BT Adapter des lokalen Gerätes
        val bluetoothManager: BluetoothManager? = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        if(bluetoothAdapter == null){
            Toast.makeText(requireContext(), "Your Device doens't support BT", Toast.LENGTH_LONG).show()
        }

        //Prüfung ist BT auf dem Gerät eingeschaltet, wenn nicht über Intent anfordern
        if (bluetoothAdapter?.isEnabled == true) {
            Toast.makeText(requireContext(), "All permissions set", Toast.LENGTH_LONG).show()
        } else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothRequest.launch(enableBtIntent)
            //TODO: Schleife falls Nutzer BT nicht einschaltet
        }
    }

    //Suche nach Geräte starten
    @SuppressLint("MissingPermission")
    private fun startSearchDevices(){
        binding.btnStartSearch.visibility = View.INVISIBLE
        binding.loadingBarSearch.visibility = View.VISIBLE
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun stopSearchDevices(){
        binding.btnStartSearch.visibility = View.VISIBLE
        binding.loadingBarSearch.visibility = View.GONE
        bluetoothAdapter?.cancelDiscovery()
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    // Nach Geräten suchen und Name sowie MAC-Adresse in Variablen speichern
    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address

                    if(deviceName?.contains("ESP") == true && !viewModel.bluetoothDevices.contains(device)){
                        viewModel.bluetoothDevices.add(device)
                        adapter.notifyItemInserted(viewModel.bluetoothDevices.size - 1)

                    }

                }
            }
        }
    }

    //Verbindung zu einem BT Gerät aufbauen und Klasse zum Daten senden aufrufen
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {


        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
        }

        fun connectAndSend(message: String){
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    ConnectedThread(socket).write(message.toByteArray())

                }catch (e: IOException){
                    Toast.makeText(requireContext(),"Verbindung nicht erfolgreich", Toast.LENGTH_LONG).show()
                    binding.main.visibility = View.VISIBLE
                    binding.loadingBarSearch.visibility = View.GONE
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun closeBluetoothSocket() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("geu", "Could not close the client socket", e)
            }
        }
    }

    //Klasse um Daten an ein verbundenes Gerät zu senden oder Daten zu empfangen
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d("geu", "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
               /* val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()*/
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                TimeUnit.SECONDS.sleep(1L)
                mmOutStream.write(bytes)

            } catch (e: IOException) {
                Log.e("geu", "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                /*val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)*/
                Toast.makeText(requireContext(), "Couldn't send data to the other device", Toast.LENGTH_LONG).show()
                binding.main.visibility = View.VISIBLE
                binding.loadingBarSearch.visibility = View.GONE
                return
            }

            closeBluetoothSocket()

           /* // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()*/
        }

        // Call this method from the main activity to shut down the connection.
        fun closeBluetoothSocket() {
            try {
                mmSocket.close()
                binding.main.visibility = View.VISIBLE
                binding.loadingBarSearch.visibility = View.GONE
            } catch (e: IOException) {
                Log.e("geu", "Could not close the connect socket", e)
                binding.main.visibility = View.VISIBLE
                binding.loadingBarSearch.visibility = View.GONE
            }
        }
    }


}