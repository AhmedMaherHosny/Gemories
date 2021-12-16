package com.example.gemories

import android.Manifest
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat
import com.example.gemories.database.LocationDatabase
import com.example.gemories.database.LocationsTable
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import kotlin.system.exitProcess

class MainActivity : BaseActivity() {
    private var sendOnce = true
    lateinit var currentLocation : LatLng
    lateinit var mark : MarkerOptions
    lateinit var markFixed : MarkerOptions
    var currentMarker : Marker? = null
    private lateinit var googleMap: GoogleMap
    private lateinit var addBtn:MaterialButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapFragment : SupportMapFragment
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var manager : LocationManager
    private lateinit var locationCallBack : LocationCallback
    private lateinit var locationDB : LatLng
    private lateinit var data: List<LocationsTable>
    private var res = FloatArray(1)
    private lateinit var channel : NotificationChannel
    private lateinit var notificationManger : NotificationManager
    private lateinit var notificationMangerCompat : NotificationManagerCompat


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationUpdate()
        manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        addBtn = findViewById(R.id.add)
        notificationMangerCompat = NotificationManagerCompat.from(applicationContext)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        loadMarkers()
        trackGPS()

        addBtn.setOnClickListener(View.OnClickListener {
            saveLocation()
            loadMarkers()
        })
    }

    private fun pushNotification() {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            channel = NotificationChannel("Gemories1", "Gemories", IMPORTANCE_DEFAULT)
            notificationManger = getSystemService(NotificationManager::class.java)
            notificationManger.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = NotificationCompat.Builder(baseContext, "Gemories1")
        notification.setSmallIcon(R.drawable.noti)
                .setContentIntent(pendingIntent)
                .setContentTitle("Attention!").setContentText("You have a memory over there check it out!")
                .priority = NotificationCompat.PRIORITY_HIGH
        notificationManger.notify(10, notification.build())
    }
    private fun loadMarkers() {
        if (LocationDatabase.getInstance(this).locationDAO().checkIfEmpty()!=0){
            data = LocationDatabase.getInstance(this)
                    .locationDAO().getAllLocations()
            mapFragment.getMapAsync(OnMapReadyCallback {
                for (location:LocationsTable in data){
                    markFixed = MarkerOptions().position(LatLng(location.location.latitude, location.location.longitude)).title("flag")
                    it.addMarker(markFixed)
                    Log.e("load : ", "success")
                }
            })
        }
    }
    private fun trackGPS(){
        locationCallBack = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                for (location:Location in p0.locations){
//                    lat=location.latitude
//                    lon=location.longitude
                    currentLocation = LatLng(location.latitude, location.longitude)

                    //comparing
                    for (marker:LocationsTable in data){
                        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, marker.location.latitude, marker.location.longitude, res)
                        if (res[0] <= 10 && sendOnce){
                            pushNotification()
                            sendOnce = false
                            break
                            //Log.e("memory : ", "True")
                        }else{
                            sendOnce = true
                        }
                    }

                    mapFragment.getMapAsync(OnMapReadyCallback {
                        googleMap = it
                        googleMap.isMyLocationEnabled = true
//                        Log.e("onLocationResult : ", ""+currentLocation)
                        //googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17.5f))
                    })
                }
            }
        }

    }
    private fun saveLocation(){
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            askToEnableGps()
        }else{
            locationDB = currentLocation
//              Log.e("locationDB", "" + locationDB)
            if (LocationDatabase.getInstance(this).locationDAO().count(locationDB.latitude, locationDB.longitude)!=1){
                val locDB = LocationsTable(location = locationDB)
                LocationDatabase.getInstance(this)
                        .locationDAO().addLocation(locDB)
                Log.e("add : ", "success")
            }else{
                Toast.makeText(this,"Sorry we can't add this location cuz this location is exist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
           // getLastLocation()
            checkSettingsAndStartLocationUpdate()
        }else{
            if (isGoogleServices()){
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    askToEnableGps()
                }else {
                    requestPermission()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        startLocationUpdate()
    }
    override fun onStop() {
        super.onStop()
        stopLocationUpdate()
    }

    private fun getLastLocation(){
        fusedLocationClient.lastLocation.addOnSuccessListener {
            Log.e("toString", "" + it.toString())
            Log.e("latlit", "" + it.latitude)
            Log.e("longlit", "" + it.longitude)
        }
        fusedLocationClient.lastLocation.addOnFailureListener {
            Log.e("onFailure", ""+it.localizedMessage)
        }
    }

    private fun checkSettingsAndStartLocationUpdate(){
        val request = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build()
        val client = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(request).addOnSuccessListener {
            startLocationUpdate()
        }
        client.checkLocationSettings(request).addOnFailureListener{
            if (it is ResolvableApiException){
                val apiException = it
                try {
                    apiException.startResolutionForResult(this, 1)
                }catch (e:IntentSender.SendIntentException){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startLocationUpdate(){
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper())
    }
    private fun getLocationUpdate(){
        locationRequest = com.google.android.gms.location.LocationRequest.create()
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private fun stopLocationUpdate(){
        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }

    private val requestPermissionLauncher =
            registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
//                    Toast.makeText(this, "permission grant from val", Toast.LENGTH_SHORT).show()
//                    getLastLocation()

                } else {
                    //Toast.makeText(this, "permission not grant from val", Toast.LENGTH_SHORT).show()
                }
            }

    private fun askToEnableGps() {
            createDialog("You must turn on the GPS or close the app!", "TURN ON",
                    { p0, p1 ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    "CLOSE THE APP",
                    { p0, p1 ->
                        p0.dismiss()
                        finish()
                        exitProcess(0)
                    }
            )
    }

    private fun isGoogleServices():Boolean{
        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        when {
            available == ConnectionResult.SUCCESS -> {
                Log.e("services is :", "ok")
                return true
            }
            GoogleApiAvailability.getInstance().isUserResolvableError(available) -> {
                Log.e("services is :", "not ok but we can")
                val dialog: Dialog? = GoogleApiAvailability.getInstance().getErrorDialog(this, available, 2)
                dialog?.show()
            }
            else -> {
                Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
//                Toast.makeText(this, "permission grant from func", Toast.LENGTH_SHORT).show()
//                getLastLocation()
                checkSettingsAndStartLocationUpdate()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                val msg = "Location Access Is Required!"
                createDialog(
                        msg, "OK",
                        { p0, p1 -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        "NO", { p0, p1 -> p0.dismiss() }
                )
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

}