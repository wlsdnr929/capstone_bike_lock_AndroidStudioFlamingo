package com.example.wifiwithkotlin

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlin.math.round


data class myLatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0
)

data class myDistance(
    val distance: Double = 0.0,
    val altitude: Double = 0.0
)

private val graphList = mutableListOf<myDistance>()

class RouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var chart: LineChart? = null
    //private lateinit var binding: ActivityMapsBinding

    var selectedtime = ""
    var totaldistance = 0.0f
    var cal = 0.0f

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        chart = findViewById(R.id.linechart);
        graphList.clear()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.mapview2) as SupportMapFragment
        mapFragment.getMapAsync(this)

        selectedtime = intent.getStringExtra("selectedData").toString()

        setupChart()

        val routefinish:Button = findViewById(R.id.btnrouteFinish)
        routefinish.setOnClickListener{
            graphList.clear()
            mMap.clear()
            totaldistance = 0.0f
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val database = FirebaseDatabase.getInstance()
        val parentRef: DatabaseReference = database.reference.child("$user_name")

        val textView: TextView = findViewById(R.id.textview2)
        // val distance = round(user_distance * 1000) / 1000.0f
        // textView.text = "${user_name}의 이동 거리: $distance km"

        parentRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val currentTime = dataSnapshot.key
                if (currentTime == selectedtime) {
                    val pathRef: DatabaseReference = parentRef.child(currentTime.toString())
                    val coordinates: MutableList<myLatLng> = mutableListOf()

                    pathRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (childSnapshot in dataSnapshot.children) {
                                val coordinate: myLatLng? = childSnapshot.getValue(object :
                                    GenericTypeIndicator<myLatLng>() {})
                                coordinate?.let { coordinates.add(it) }
                            }
                            // coordinates 배열을 이용하여 필요한 처리 로직을 수행합니다.
                            calcdistance(coordinates)
                            drawPolyline(coordinates)
                            textView.text = "${user_name}의 이동 거리: ${totaldistance}km\n예상 소모 칼로리: ${cal}kcal/kg"
                            setData()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            println("데이터 읽기 중 오류 발생: $databaseError")
                        }
                    })
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // 경로 데이터가 변경되었을 때의 처리 로직을 작성합니다.
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // 경로 데이터가 제거되었을 때의 처리 로직을 작성합니다.
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // 경로 데이터가 이동되었을 때의 처리 로직을 작성합니다.
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("데이터 읽기 중 오류 발생: $databaseError")
            }
        })

//        val valueEventListener = object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    val arrayData = mutableListOf<myLatLng>()
//                    for (childSnapshot in dataSnapshot.children) {
//                        val latLngData = childSnapshot.getValue(myLatLng::class.java)
//                        latLngData?.let { arrayData.add(it) }
//                    }
//                    // 배열 데이터 사용
//                    drawPolyline(arrayData)
//                }
//                else {
//                    // 데이터베이스에 해당 경로에 값이 없는 경우 처리
//                    Toast.makeText(applicationContext, "저장된 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
//                    finish()
//                }
//            }
//            override fun onCancelled(databaseError: DatabaseError) {
//                // 읽기 작업이 취소된 경우 처리
//            }
//        }
//        childRef.addValueEventListener(valueEventListener)



    }

    private fun calcdistance(array: MutableList<myLatLng>) {
        // 시작점 표시
        val graphinit = myDistance(totaldistance.toDouble(), array[0].altitude)
        graphList.add(graphinit)

        for (i in 0 until array.size - 1) {
            val gps1 = GPS(array[i].latitude, array[i].longitude)
            val gps2 = GPS(array[i + 1].latitude, array[i + 1].longitude)

            val loc1 = Location("")
            loc1.latitude = gps1.latitude
            loc1.longitude = gps1.longitude

            val loc2 = Location("")
            loc2.latitude = gps2.latitude
            loc2.longitude = gps2.longitude

            // 거리 계산
            val distance = loc1.distanceTo(loc2)
            totaldistance += distance

            // 고도 변화 계산
            val height1 = array[i].altitude
            val height2 = array[i + 1].altitude

            // 칼로리 계산
            val D = distance / 1000.0f
            val H = height2 - height1
            val T = 3
            val weight = user_weight
            cal = (((D * 35) + (H * 0.125) + (T * 0.14)) / (weight * 1000)).toFloat()
            cal = round(cal * 1000) / 1000.0f

            // 이동 거리 저장 -> 그래프 표시 해줘야 함
            val graphval = myDistance((round(totaldistance) / 1000.0f).toDouble(), array[i + 1].altitude)
            graphList.add(graphval)

        }
        totaldistance = round(totaldistance) / 1000.0f
    }

    fun drawPolyline(latLngs: List<myLatLng>) {
        val polylineOptions = PolylineOptions()
            .addAll(latLngs.map { LatLng(it.latitude, it.longitude) })

        mMap.addPolyline(polylineOptions)

        // 카메라 위치 변경
        val lastLatLng = latLngs.lastOrNull()
        if (lastLatLng != null) {
            // 카메라를 마지막 LatLng 위치로 이동합니다.
            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(lastLatLng.latitude, lastLatLng.longitude))
                .zoom(16f) // 줌 레벨 설정
                .build()
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun setupChart() {
        chart!!.setDrawBorders(true)
        val desc = Description()
        desc.text = "Line Chart Example"
        chart!!.description = desc
    }

    private fun setData() {
        val entries = arrayListOf<Entry>()
        for (i in 0 until graphList.size) {
            entries.add(Entry(graphList[i].distance.toFloat(), graphList[i].altitude.toFloat()))
        }

//        entries.add(Entry(0f, 10f))

        val dataSet = LineDataSet(entries, "Data")
        dataSet.color = Color.RED
        dataSet.valueTextColor = Color.BLACK

        val lineData = LineData(dataSet)
        chart!!.data = lineData
        chart!!.invalidate()
    }

}
