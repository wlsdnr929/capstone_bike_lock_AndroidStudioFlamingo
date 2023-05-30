package com.example.wifiwithkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wifiwithkotlin.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Date
import java.util.Locale
import kotlin.math.round
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager

private val latLngList = mutableListOf<LatLng>()
private val uploadList = mutableListOf<myLatLng>()
private var movedistance = 0.0f // 누적 이동 거리 (단위: m)

//data class GPS(val latitude: Double, val longitude: Double)

@Suppress("DEPRECATION")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val UPDATE_INTERVAL = 3000L // 업데이트 주기 단위 1ms
    private val FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2

    private var locationUpdateHandler = Handler(Looper.getMainLooper())

    //val user_dis_float:Float = 0.0f
    var user_dis_int:Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.button.setOnClickListener{
            latLngList.clear()
            uploadList.clear()
            mMap.clear()
            movedistance = 0.0f
            val textView: TextView = findViewById(R.id.textview)
            textView.text = "0.0 km/h\n이동 거리: 0.0 km"
            finish()
        }

        val finish_button2:Button = findViewById(R.id.btnmapFinish)
        finish_button2.setOnClickListener {

            val intent = Intent()
            user_dis_int = movedistance / 1000.0f   // 단위 m -> km
            intent.putExtra("now_dis", user_dis_int)
            Log.e("보내는 거리", "보내는 거리: $user_dis_int")

            val arrayData = uploadList
            val database: FirebaseDatabase = FirebaseDatabase.getInstance()
            val parentRef: DatabaseReference = database.reference.child("$user_name")

            val currentTime = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault()).format(Date())

            val childRef: DatabaseReference = parentRef.child("$currentTime")
            // 경로 데이터를 저장할 고유한 키 생성
            // val pathKey = childRef.child(currentTime).push().key

            // 경로 데이터를 저장
            childRef.setValue(arrayData)
                .addOnSuccessListener {
                    // childRef.child("이동거리").setValue(user_dis_int) // 해당 시간에 이동한 경로의 이동거리 표시
                    Log.e("업로드 성공", "업로드 성공")
                }
                .addOnFailureListener { error ->
                    Log.e("업로드 실패", "업로드 실패")
                }

            setResult(RESULT_OK, intent)

//            if (user_name != "") {
//                val intent = Intent()
//
//                user_dis_int = movedistance / 1000.0f   // 단위 m -> km
//                intent.putExtra("now_dis",user_dis_int)
//                Log.e("보내는 거리","보내는 거리: $user_dis_int")
//
//                val arrayData = latLngList
//                val database: FirebaseDatabase = FirebaseDatabase.getInstance()
//                val parentRef: DatabaseReference = database.reference.child("name")
//                val childRef: DatabaseReference = parentRef.child("$user_name")
//
//                childRef.setValue(arrayData)
//                    .addOnSuccessListener {
//                        println("데이터가 성공적으로 업로드되었습니다.")
//                    }
//                    .addOnFailureListener { error ->
//                        println("데이터 업로드 중 오류 발생: $error")
//                    }
//
//                setResult(RESULT_OK, intent)
//            }

            latLngList.clear()
            uploadList.clear()
            mMap.clear()
            movedistance = 0.0f
            val textView: TextView = findViewById(R.id.textview)
            textView.text = "0.0 km/h\n이동 거리: 0.0 km"
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // 카메라 초기 위치, 확대 크기 설정
        val cameraPosition = CameraPosition.Builder().target(LatLng(37.375191, 126.632868)).zoom(16f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        // 위치 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // 권한이 허용되면 현재 위치를 가져와서 GoogleMap 객체에 표시
        updateLocation()
    }

    private var lastMarker: Marker? = null // 직전 마커 저장
    @SuppressLint("MissingPermission")
    private fun updateLocation() {
        val textView: TextView = findViewById(R.id.textview)
        val textlocation: TextView = findViewById(R.id.textlocation)

        startLocationUpdates()  // 현재 위치 업데이트해주기
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // 위치 업로드
                val uploadLatLng = myLatLng(location.latitude, location.longitude, location.altitude)
                uploadList.add(uploadLatLng)

                // 현재 위치 추가
                val currentLatLng = LatLng(location.latitude, location.longitude)
                latLngList.add(currentLatLng)

                // 마커 연결
                val polylineOptions = PolylineOptions().addAll(latLngList)
                mMap.addPolyline(polylineOptions)

                // 직전 위치 마커 지우기
                lastMarker?.remove()
                if (latLngList.size > 1) {
                    // 마커 data 지우기 전 속력 계산
                    val size = latLngList.size
                    val gps1 = GPS(latLngList[size - 2].latitude, latLngList[size - 2].longitude)
                    val gps2 = GPS(latLngList[size - 1].latitude, latLngList[size - 1].longitude)

                    val loc1 = Location("")
                    loc1.latitude = gps1.latitude
                    loc1.longitude = gps1.longitude

                    val loc2 = Location("")
                    loc2.latitude = gps2.latitude
                    loc2.longitude = gps2.longitude

                    // 거리 계산
                    val distance = loc1.distanceTo(loc2)
                    movedistance = movedistance + distance
                    val printdistance = round(movedistance) / 1000.0f

                    // 시간 계산 (초)
                    val timeInSeconds = UPDATE_INTERVAL / 1000

                    // 속력 계산 (m/s)
                    val speedInMps = distance / timeInSeconds

                    // km/h로 변환
                    val speedInKmh = speedInMps * 3.6f
                    val printspeed = round(speedInKmh*10) / 10.0f

                    // 고도 표시
                    val altitude = location.altitude
                    val printaltitude = round(altitude * 10) / 10.0f

                    // 출력
                    textView.text = "$printspeed km/h\n이동 거리: $printdistance km"

                    // 속도 출력 (app 로그에서 확인)
                    //Log.e("Speed", "Speed: $speedInKmh km/h")
                    //Log.e("Distance", "Distance: $printdistance km")

                    // 현재 위치 표시
                    lastMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("현재 위치"))
                    val mylocation = getAddress(latLngList[size - 1].latitude, latLngList[size - 1].longitude)
                    textlocation.text = "$mylocation"

                    // 카메라 위치 설정
                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(latLngList[size - 1].latitude, latLngList[size - 1].longitude))
                        .zoom(mMap.cameraPosition.zoom)
                        .build()

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                }

                // 버튼이 눌리면, 이동경로 및 거리 초기화
                //binding.button.setOnClickListener{
                //    latLngList.clear()
                //    mMap.clear()
                //    movedistance = 0.0f
                //    textView.text = "0.0 km/h\n이동 거리: 0.0 km"
                //}



            }
            locationUpdateHandler.postDelayed({ updateLocation() }, UPDATE_INTERVAL)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 지도에 현재 위치를 표시
                updateLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationRequest = LocationRequest.create().apply {
        interval = UPDATE_INTERVAL
        fastestInterval = FASTEST_UPDATE_INTERVAL
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            // Handle location updates
        }
    }

    private fun getAddress(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        var addressText = ""
        val thread = Thread {
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    addressText = address.getAddressLine(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        return addressText
    }

}

