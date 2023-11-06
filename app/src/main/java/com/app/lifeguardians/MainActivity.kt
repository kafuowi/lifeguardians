package com.app.lifeguardians

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.app.lifeguardians.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapOptions
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
data class MarkerItem(
    val uploader: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = ""
)
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    var currentUser: FirebaseUser? = mAuth.getCurrentUser()

    private val PERMISSIONS = arrayOf<String>(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION
    )
    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener




    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = FirebaseFirestore.getInstance()//firestore initialization

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }
        binding.fabAdd.setOnClickListener {//마커추가
            //Toast.makeText(this, "add button clicked", Toast.LENGTH_LONG).show()
            getLastLocation()
        }


        binding.fabLogout.setOnClickListener{//sign out
            mAuth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }


        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance(NaverMapOptions().locationButtonEnabled(true)).also {
                supportFragmentManager.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync(this)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager



        locationListener = LocationListener { location ->
            val latitude = location.latitude
            val longitude = location.longitude
            Toast.makeText(this, "Latitude: $latitude, Longitude: $longitude", Toast.LENGTH_LONG).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if(currentUser==null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }


        //마커정보 가져오기



    }
    fun uploadMarker(markerItem: MarkerItem) {
        // Add a new document with a generated ID to the "markers" collection
        val db = FirebaseFirestore.getInstance()
        db.collection("markers")
            .add(markerItem)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_LOCATION)
            return
        }

        val task: Task<Location> = fusedLocationClient.lastLocation
        task.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Toast.makeText(this, "Latitude: $latitude, Longitude: $longitude", Toast.LENGTH_SHORT).show()

                val user = mAuth.currentUser

                user?.let {//파이어베이스 업로드
                    // The user is signed in
                    val email = user.email // This is the email used for sign-in
                    val markerItem = email?.let { it1 ->
                        MarkerItem(
                            uploader = it1,
                            latitude = latitude, 
                            longitude = longitude,  
                            description = "Description of the marker"
                        )
                    }

                    if (markerItem != null) {
                        uploadMarker(markerItem)
                    }
                } ?: run {
                    // No user is signed in
                }

            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions,
                grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMapReady(naverMap: NaverMap) {
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow
        val firestore = FirebaseFirestore.getInstance()
        val markersCollection = firestore.collection("markers")


        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        markersCollection.get().addOnSuccessListener { documents ->
            val markerList = documents.mapNotNull { snapshot ->
                snapshot.toObject(MarkerItem::class.java)
            }
            markerList.forEach { marker ->
                // Same body as in the for-loop
                val uploader = marker.uploader
                val latitude = marker.latitude
                val longitude = marker.longitude
                val description = marker.description
                val thismarker = Marker()
                thismarker.position = LatLng(latitude, longitude)
                thismarker.map = naverMap
                // Do something with the data
            }
            // Now markerList contains all markers from Firestore
            // Use this list to update your UI or handle markers on map
        }.addOnFailureListener { exception ->
            // Handle error
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val REQUEST_LOCATION = 1
    }

}