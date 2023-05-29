package com.example.wifiwithkotlin

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifiwithkotlin.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.*
import java.net.URL
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

private val latLngList = mutableListOf<LatLng>()
private var movedistance = 0.0f // 누적 이동 거리

// 날씨 api Interface 및 data class
var TO_GRID = 0
var TO_GPS = 1
var user_name:String  = ""
var user_distance:Float = 0.0f
var user_weightname:String = ""
var user_weight:Float = 0.0f

data class GPS(val latitude: Double, val longitude: Double)

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var mConnectionStatus: TextView? = null
    private var mInputEditText: EditText? = null
    private var mConversationArrayAdapter: ArrayAdapter<String>? = null
    private var TAG: String? = null
    private var isConnected = false
    private var mServerIP: String? = null
    private var mSocket: Socket? = null
    private var mOut: PrintWriter? = null
    private var mIn: BufferedReader? = null
    private var mReceiverThread: Thread? = null
    var isLock:Boolean = true // 1: 잠김, 0: 열림
    var flag:Boolean = false

    var isRide:Boolean = false

    //var user_distance:Float = 0.0f
    var prev_dis:Float =0.0f
    var user_distanc_with_int:Int = 0

    lateinit var firebaseDatabase:FirebaseDatabase
    lateinit var databaseReference : DatabaseReference


    // 지도 관련 변수
    // private lateinit var mMap: GoogleMap
     private lateinit var fusedLocationClient: FusedLocationProviderClient
    // private val LOCATION_PERMISSION_REQUEST_CODE = 1
    // private val UPDATE_INTERVAL = 2000L // 업데이트 주기 단위 1ms
    // private val FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2
    // private var locationUpdateHandler = Handler(Looper.getMainLooper())

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mConnectionStatus = findViewById<View>(R.id.connection_status_textview) as TextView


        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("user_name")

        val activityLauncher= openActivityResultLauncher()



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
        //mapFragment.getMapAsync(this)

        // fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

//        binding.btnCheckSafety.setOnClickListener{
//            if(flag){
//                binding.btnCheckSafety.setBackgroundColor(resources.getColor(R.color.black))
//                //binding.btnCheckSafety.setBackgroundColor(Color.parseColor("#ff3030"))
//                flag = false;
//            }else{
//                binding.btnCheckSafety.setBackgroundColor(Color.parseColor("#ff3030"))
//                flag=true;
//            }
//        }

        //mInputEditText = findViewById<View>(R.id.input_string_edittext) as EditText
        TAG = "TcpClient"
        //val mMessageListview = findViewById<View>(R.id.message_listview) as ListView
//        val sendButton = findViewById<View>(R.id.send_button) as Button
//        sendButton.setOnClickListener {
//            //val sendMessage = mInputEditText!!.text.toString()
//            val sendMessage = flagLock.toString()
//            if (sendMessage.length > 0) {
//                if (!isConnected) showErrorDialog("서버로 접속된후 다시 해보세요.") else {
//                    Thread(SenderThread(sendMessage)).start()
//                    mInputEditText!!.setText(" ")
//                }
//            }
//        }
        mConversationArrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item
        )
        //mMessageListview.adapter = mConversationArrayAdapter
        // 집
        //Thread(ConnectThread("192.168.0.17", 8090)).start()
        // 학교
        //Thread(ConnectThread("192.168.0.54", 8090)).start()
        // 원찬 핫스팟
        //Thread(ConnectThread("192.168.215.192", 8090)).start()
        Thread(ConnectThread("192.168.146.192", 8090)).start()

        binding.imageButton.setOnClickListener {
            if(isLock){
                binding.imageButton.setImageResource(R.drawable.unlock)
                isLock=false
            }
            else{
                binding.imageButton.setImageResource(R.drawable.lock)
                isLock = true
            }
//            var sendMessage = "5"
//            if(isLock == true) sendMessage = "1"
//            else sendMessage = "0"
            Thread(SenderThread("5")).start()
        }

        binding.checkMissing.setOnClickListener {
            Thread(SenderThread("9")).start()
        }

//        binding.btnCctv.setOnClickListener {
//            var intent = Intent(this,CCTVActivity::class.java)
//            startActivity(intent)
//        }

        inpuName()

        binding.btnCctv.setOnClickListener {
            val intent =Intent(this,CCTVActivity::class.java)
            startActivity(intent)
        }

        binding.btnmap.setOnClickListener {
            val intent =Intent(this,MapsActivity::class.java)
            activityLauncher.launch(intent)
        }


        binding.btnDialog.setOnClickListener{
            val v1 = layoutInflater.inflate(R.layout.alertdaiolog_edittext,null)
            val builder = AlertDialog.Builder(this)
            builder.setView(v1)
            var listener =DialogInterface.OnClickListener {
                dialog,i->
                var alert = dialog as AlertDialog
                val et = alert.findViewById<EditText>(R.id.editText)
                val et2 = alert.findViewById<EditText>(R.id.editText2)
                if(et?.text.toString() != user_name){
                    user_distance=0.0f
                }
                user_name = et?.text.toString()

                if (et2?.text.toString() != user_weight.toString()) {
                    val weighttext = et2?.text.toString()
                    user_weight = weighttext.toFloatOrNull()!!
                }

                Log.e("1 input_user_name",user_name.toString())
                //initDatabase()

                //checkData()

                databaseReference.child(user_name).get()
                    .addOnSuccessListener {
                        Log.e("확인 값","${it.value}")
                        if(it.value !=null){
                            prev_dis = it.value.toString().toFloat()
                        }
                        else{
                            prev_dis=0.0f
                        }
                    }


//                databaseReference.addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        for(shot in snapshot.children) {
//                            val name = shot.key
//                            val dis:Float = shot.value.toString().toFloat()
//                            Log.e("user_name",user_name)
//                            Log.e("name",name.toString())
//                            Log.e("dis",dis.toString())
////                    val p_name = data
////                    val p_phone = data2
////                            val res:Boolean = (name.toString() == user_name)
////                            Log.e("답은?",res.toString())
//
//                            //Log.e("name ->",name::class.simpleName.toString())
//                            //Log.e("user_name ->?",user_name::class.simpleName.toString())
//
//                           if(name==user_name) {
//                               Log.e("여기", "같음")
//                               prev_dis = dis
//                               break
//                           }
//                        }
//                        user_distance+=prev_dis
//                        Log.e("여기 prev_dis",prev_dis.toString())
//                        Log.e("user_distance",user_distance.toString())
//                    }
//                    override fun onCancelled(error: DatabaseError) {
//                        TODO("Not yet implemented")
//                    }
//
//
//                })

                //val databaseReference1 = firebaseDatabase.getReference("user_name")
//                databaseReference.child(user_name!!).setValue(user_distance)
//                Log.e("3","3")



                val toast = Toast.makeText(this@MainActivity,"입력한 이름: $user_name, 체중: $user_weight",Toast.LENGTH_SHORT)
                toast.setGravity(
                    Gravity.BOTTOM,0,0)
                toast.show()

                Log.e("4 user_name",user_name.toString())


            }

            builder.setTitle("이름 입력")
                .setPositiveButton("확인",listener)
            builder.show()
            Log.e("5","5")
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 정보에 따라 실행
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val lat = it.latitude
                    val lon = it.longitude
                    lifecycleScope.launch {
                        val response = withContext(Dispatchers.IO) {
                            getWeather(lat, lon)
                        }
                        // 응답 처리 코드 작성
                        parseXML(response)
                    }
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        }


        binding.btnRanking.setOnClickListener {
            val intent = Intent(this@MainActivity,RankingActivity::class.java)
            startActivity(intent)
        }

        binding.goRiding.setOnClickListener {
            if(isRide){
                binding.goRiding.setText("주행 시작")
                isRide= false
                Log.e("is_ride",isRide.toString())
            }
            else{
                binding.goRiding.setText("주행 중")
                isRide = true
                Log.e("is_ride",isRide.toString())
            }

            Thread(SenderThread("10")).start()
        }

    }

    private fun openActivityResultLauncher(): ActivityResultLauncher<Intent> {
        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "수신 성공", Toast.LENGTH_SHORT).show()
                //binding.tvComeback.text = result.data?.getStringExtra("comeback")
                user_distance = result.data!!.getFloatExtra("now_dis",0.0f)
                Log.e("받은 거리",user_distance.toString())

                user_distance +=prev_dis
                Log.e("파베로 보내는 거리",user_distance.toString())
                databaseReference.child(user_name!!).setValue(user_distance)
                //user_distance=0.0f

            }
            else {
                Toast.makeText(this, "수신 실패", Toast.LENGTH_SHORT).show()
            }
        }
        return resultLauncher
    }

    fun inpuName() {
        val v1 = layoutInflater.inflate(R.layout.alertdaiolog_edittext, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(v1)
        var listener = DialogInterface.OnClickListener { dialog, i ->
            var alert = dialog as AlertDialog
            val et = alert.findViewById<EditText>(R.id.editText)
            if (et?.text.toString() != user_name) {
                user_distance = 0.0f
            }
            user_name = et?.text.toString()


            Log.e("1 input_user_name", user_name.toString())
            //initDatabase()

            //checkData()

            databaseReference.child(user_name).get()
                .addOnSuccessListener {
                    Log.e("확인 값", "${it.value}")
                    if (it.value != null) {
                        prev_dis = it.value.toString().toFloat()
                    } else {
                        prev_dis = 0.0f
                    }
                }


//                databaseReference.addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        for(shot in snapshot.children) {
//                            val name = shot.key
//                            val dis:Float = shot.value.toString().toFloat()
//                            Log.e("user_name",user_name)
//                            Log.e("name",name.toString())
//                            Log.e("dis",dis.toString())
////                    val p_name = data
////                    val p_phone = data2
////                            val res:Boolean = (name.toString() == user_name)
////                            Log.e("답은?",res.toString())
//
//                            //Log.e("name ->",name::class.simpleName.toString())
//                            //Log.e("user_name ->?",user_name::class.simpleName.toString())
//
//                           if(name==user_name) {
//                               Log.e("여기", "같음")
//                               prev_dis = dis
//                               break
//                           }
//                        }
//                        user_distance+=prev_dis
//                        Log.e("여기 prev_dis",prev_dis.toString())
//                        Log.e("user_distance",user_distance.toString())
//                    }
//                    override fun onCancelled(error: DatabaseError) {
//                        TODO("Not yet implemented")
//                    }
//
//
//                })

            //val databaseReference1 = firebaseDatabase.getReference("user_name")
//                databaseReference.child(user_name!!).setValue(user_distance)
//                Log.e("3","3")


            val toast = Toast.makeText(this@MainActivity, "입력한 이름: $user_name", Toast.LENGTH_SHORT)
            toast.setGravity(
                Gravity.BOTTOM, 0, 0
            )
            toast.show()

            Log.e("4 user_name", user_name.toString())


        }
    }

    fun checkData(){
        databaseReference.child("user_name").child(user_name).get().addOnSuccessListener {
            Log.e("2 있어!","${it.value}")

        }.addOnFailureListener {
            Log.e("2 없어!","없어!")
        }
    }
    fun initDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("user_name")
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
    }

    override fun onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            Log.e(TAG, "onBackPressed:")
            isConnected = false
            finish()
        } else {
            Toast.makeText(baseContext, "한번 더 뒤로가기를 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            back_pressed = System.currentTimeMillis()
        }
    }

    private inner class ConnectThread internal constructor(
        private val serverIP: String,
        private val serverPort: Int
    ) :
        Runnable {
        override fun run() {
            try {
                mSocket = Socket(serverIP, serverPort)
                //ReceiverThread: java.net.SocketTimeoutException: Read timed out 때문에 주석처리
                //mSocket.setSoTimeout(3000);
                mServerIP = mSocket!!.remoteSocketAddress.toString()
            } catch (e: UnknownHostException) {
                Log.e(TAG, "ConnectThread: can't find host")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "ConnectThread: timeout")
            } catch (e: Exception) {
                Log.e(TAG, "ConnectThread:" + e.message)
            }
            if (mSocket != null) {
                try {
                    mOut = PrintWriter(
                        BufferedWriter(
                            OutputStreamWriter(
                                mSocket!!.getOutputStream(), "UTF-8"
                            )
                        ), true
                    )
                    mIn = BufferedReader(InputStreamReader(mSocket!!.getInputStream(), "UTF-8"))
                    isConnected = true
                } catch (e: IOException) {
                    Log.e(TAG, "ConnectThread:" + e.message)
                }
            }
            runOnUiThread {
                if (isConnected) {
                    Log.e(TAG, "connected to $serverIP")
                    mConnectionStatus!!.text = "$serverIP 연결 성공"
                    mReceiverThread = Thread(ReceiverThread())
                    mReceiverThread!!.start()
                } else {
                    Log.e(
                        TAG,
                        "failed to connect to server $serverIP"
                    )
                    mConnectionStatus!!.text = "$serverIP 연결 실패"
                }
            }
        }

        init {
            mConnectionStatus!!.text = "connecting to $serverIP......."
        }
    }

    private inner class SenderThread internal constructor(private val msg: String) : Runnable {
        override fun run() {
            mOut!!.println(msg)
            mOut!!.flush()
            runOnUiThread {
                Log.e(TAG, "send message: $msg")
                //mConversationArrayAdapter!!.insert("Me - $msg", 0)

                //mConversationArrayAdapter!!.insert("<조이름: 매트릭스> 아두이노로 보내는 거: $msg", 0)
            }
        }
    }

    private inner class ReceiverThread : Runnable {
        override fun run() {
            try {
                while (isConnected) {
                    if (mIn == null) {
                        Log.e(TAG, "ReceiverThread: mIn is null")
                        break
                    }
                    val recvMessage = mIn!!.readLine()
                    if (recvMessage != null) {
                        runOnUiThread {

                            Log.e(TAG,"recv message: $recvMessage")
                            // 자물쇠 털림
                            if(recvMessage == "4"){
                                binding.btnCheckSafety.setBackgroundColor(resources.getColor(R.color.red))
                                //binding.btnCheckSafety.setBackgroundColor(ContextCompat.getColor(context,R.color.red)
                                binding.btnCheckSafety.setText("털림")
                            }
                            // 자물쇠 붙어있음
                            else if(recvMessage=="7"){
                                binding.btnCheckSafety.setBackgroundColor(resources.getColor(R.color.green))
                                binding.btnCheckSafety.setText("잘 잠겨있음")
                            }

                            // mConversationArrayAdapter!!.insert("$mServerIP - $recvMessage", 0)

                            //mConversationArrayAdapter!!.insert("<조이름: 매트릭스> 아두이노에서 받은 거:(바로 윗줄)", 0)
                            //mConversationArrayAdapter!!.insert("$recvMessage", 0)
                        }
                    }
                }
                Log.e(TAG, "ReceiverThread: thread has exited")
                if (mOut != null) {
                    mOut!!.flush()
                    mOut!!.close()
                }
                mIn = null
                mOut = null
                if (mSocket != null) {
                    try {
                        mSocket!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "ReceiverThread: $e")
            }
        }
    }

    fun showErrorDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setCancelable(false)
        builder.setMessage(message)
        builder.setPositiveButton(
            "OK"
        ) { dialog, which ->
            dialog.dismiss()
            finish()
        }
        // builder.create().notify();
    }

    companion object {
        private const val TAG = "TcpClient"
        private var back_pressed: Long = 0
    }

    /*========================== 날씨 관련 작동 파트 ========================================*/

    // 날씨 정보를 얻어온 데이터를 파싱하는 함수
    fun parseXML(xml: String) {
        val WeatherMap = mutableMapOf<String, MutableMap<String, String>>()

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse(xml.byteInputStream())

        doc.documentElement.normalize()

        val items: NodeList = doc.getElementsByTagName("item")

        for (i in 0 until items.length) {
            val item: Element = items.item(i) as Element
            val category: String = item.getElementsByTagName("category").item(0).textContent
            val fcsttime: String = item.getElementsByTagName("fcstTime").item(0).textContent
            val fcstValue: String = item.getElementsByTagName("fcstValue").item(0).textContent

            // Weather Map 저장해서, 원하는 순서로 출력하게 해줌
            WeatherMap.getOrPut("$fcsttime") {mutableMapOf()}["$category"] = "$fcstValue"
            //Log.e("카테고리",category)
        }

        //val weathertext = findViewById<TextView>(R.id.weatherView)
        val sb = StringBuilder()
        sb.append("\n")
        // 저장된 Weather Map을 이용하여, 텍스트 출력

        var is_rain = false
        var rain_time:String? = null

        for ((key, valueMap) in WeatherMap) {
            var subkey = key.slice(0..1)    // 몇시인지만 표시
            sb.append("${subkey}시 ")
            for ((category, fcstValue) in valueMap) {
                when (category) {
                    "SKY" -> {
                        val weather = when (fcstValue) {
                            "1" -> "맑음"
                            "2" -> {
                                when (WeatherMap[key]?.get("PTY")) {
                                    "1" -> "비"
                                    "2" -> "진눈깨비"
                                    "3" -> "눈"
                                    "4" -> "소나기"
                                    else ->  "구름 조금"
                                }
                            }
                            "3" -> {
                                when (WeatherMap[key]?.get("PTY")) {
                                    "1" -> "비"
                                    "2" -> "진눈깨비"
                                    "3" -> "눈"
                                    "4" -> "소나기"
                                    else -> "구름 많음"
                                }
                            }
                            "4" -> {
                                when (WeatherMap[key]?.get("PTY")) {
                                    "1" -> "비"
                                    "2" -> "진눈깨비"
                                    "3" -> "눈"
                                    "4" -> "소나기"
                                    else -> "흐림"
                                }
                            }
                            else -> "알 수 없음"
                        }
                        sb.append("날씨: $weather, ")
                        if(weather == "비" || weather == "소나기" || weather=="눈"){
                            if(is_rain==false){
                                is_rain = true
                                rain_time=subkey
                            }

                        }
                    }
                    "T1H" -> {
                        sb.append("온도: $fcstValue C°, ")
                    }
                    "REH" -> {
                        sb.append("습도: $fcstValue %\n")
                    }
                }
            }
            //Log.e("sb",sb.toString())
        }
        var weatherstr = sb.toString()
        //weathertext.text = "$weatherstr"
        Log.e("날씨 정보",weatherstr)

        if(is_rain) {
            Log.e("비오는 시간", rain_time.toString())
            binding.weatherImg.setImageResource(R.drawable.rain)
            binding.weatherStatus.text = "${rain_time.toString()}시 비 예보 있음"
        }
        else{
            binding.weatherImg.setImageResource(R.drawable.sun)
            binding.weatherStatus.text = "계속 맑음"
        }

    }

    // 날씨 정보 얻어오는 함수
    @SuppressLint("SimpleDateFormat")
    private suspend fun getWeather(lat: Double, lon: Double): String {
        var str = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst?serviceKey="
        val serviceKey = "Q8%2BLawDhUZ40LuRydl43E9K%2BfQQE%2F3gl1pYPvOZscTMef5tp6QbDLVofj5%2BwDYH7YKesf5GRZY9WIzzaWkzj2g%3D%3D"
        str += serviceKey
        val pair: LatXLngY? = trans(TO_GRID, lat, lon)
        val nx = pair?.x?.toInt().toString()
        val ny = pair?.y?.toInt().toString()

        //var baseTime = SimpleDateFormat("HHmm").format(Date())
        //var baseDate = SimpleDateFormat("yyyyMMdd").format(Date())

        var pair2 = settime()
        var baseTime = pair2.first
        var baseDate = pair2.second

        str = "$str&pageNo=1&numOfRows=100&_type=JSON&base_date=$baseDate&base_time=$baseTime&nx=$nx&ny=$ny"
        val url = URL(str)
        val connection = url.openConnection()
        connection.connect()

        val response = connection.getInputStream().bufferedReader().use { it.readText() }

        return response
    }

    // GPS 위도 및 경도 -> 기상청 제공 좌표 변환
    private fun trans(mode: Int, lat_X: Double, lng_Y: Double): LatXLngY? {
        val RE = 6371.00877 // 지구 반경(km)
        val GRID = 5.0 // 격자 간격(km)
        val SLAT1 = 30.0 // 투영 위도1(degree)
        val SLAT2 = 60.0 // 투영 위도2(degree)
        val OLON = 126.0 // 기준점 경도(degree)
        val OLAT = 38.0 // 기준점 위도(degree)
        val XO = 43.0 // 기준점 X좌표(GRID)
        val YO = 136.0 // 기1준점 Y좌표(GRID)

        //
        // LCC DFS 좌표변환 ( code : "TO_GRID" (위경도->좌표, lat_X:위도,  lng_Y:경도), "TO_GPS"(좌표->위경도,  lat_X:x, lng_Y:y) )
        //
        val DEGRAD = Math.PI / 180.0
        val RADDEG = 180.0 / Math.PI
        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD
        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / Math.pow(ro, sn)
        val rs = LatXLngY()
        if (mode == TO_GRID) {
            rs.lat = lat_X
            rs.lng = lng_Y
            var ra = Math.tan(Math.PI * 0.25 + lat_X * DEGRAD * 0.5)
            ra = re * sf / Math.pow(ra, sn)
            var theta = lng_Y * DEGRAD - olon
            if (theta > Math.PI) theta -= 2.0 * Math.PI
            if (theta < -Math.PI) theta += 2.0 * Math.PI
            theta *= sn
            rs.x = Math.floor(ra * Math.sin(theta) + XO + 0.5)
            rs.y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5)
        } else {
            rs.x = lat_X
            rs.y = lng_Y
            val xn = lat_X - XO
            val yn = ro - lng_Y + YO
            var ra = Math.sqrt(xn * xn + yn * yn)
            if (sn < 0.0) {
                ra = -ra
            }
            var alat = Math.pow(re * sf / ra, 1.0 / sn)
            alat = 2.0 * Math.atan(alat) - Math.PI * 0.5
            var theta = 0.0
            if (Math.abs(xn) <= 0.0) {
                theta = 0.0
            } else {
                if (Math.abs(yn) <= 0.0) {
                    theta = Math.PI * 0.5
                    if (xn < 0.0) {
                        theta = -theta
                    }
                } else theta = Math.atan2(xn, yn)
            }
            val alon = theta / sn + olon
            rs.lat = alat * RADDEG
            rs.lng = alon * RADDEG
        }
        return rs
    }
    internal class LatXLngY {
        var lat = 0.0
        var lng = 0.0
        var x = 0.0
        var y = 0.0
    }

    // API 초단기예보 업데이트 제공 시간이 6시간이기에, 현재 시간 + 앞으로 5시간 표시를 위해 1시간 감소
    private fun settime(): Pair<String, String> {
        val now = Date()
        val cal = Calendar.getInstance()
        cal.time = now

        // 기준 시간 구하기
        var baseTime = ""
        var baseDate = ""
        cal.add(Calendar.HOUR, -1)

        // 시간과 날짜 형식 지정
        val timeFormat = SimpleDateFormat("HH")
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        // 기준 시간과 날짜 설정
        baseTime = timeFormat.format(cal.time)
        baseDate = dateFormat.format(cal.time)
        // 00시일 경우 전날 23시로 돌리기
        if (baseTime == "00") {
            cal.add(Calendar.DATE, -1)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            baseTime = timeFormat.format(cal.time)
            baseDate = dateFormat.format(cal.time)
        }
        return Pair(baseTime + "30", baseDate)
    }

    /*=================================================== 지도 작동 파트 =======================================================*/

    /*
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // 카메라 초기 위치, 확대 크기 설정
        val cameraPosition = CameraPosition.Builder().target(LatLng(37.375191, 126.632868)).zoom(16.5f).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        // 위치 권한 요청
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // 권한이 허용되면 현재 위치를 가져와서 GoogleMap 객체에 표시
        updateLocation()
    }

    private var lastMarker: Marker? = null // 직전 마커 저장
    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun updateLocation() {
        val textView: TextView = findViewById(R.id.textview)

        startLocationUpdates()  // 현재 위치 업데이트해주기
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                latLngList.add(currentLatLng) // 현재 마커 위치 추가

                // 마커 연결
                val polylineOptions = PolylineOptions().addAll(latLngList)
                mMap.addPolyline(polylineOptions)

                // 직전 위치 마커 지우기
                lastMarker?.remove()
                if (latLngList.size > 1) {
                    // 마커 data 지우기 전 속력 계산
                    val gps1 = GPS(latLngList[0].latitude, latLngList[0].longitude)
                    val gps2 = GPS(latLngList[1].latitude, latLngList[1].longitude)

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

                    // 속도 출력
                    textView.text = "$printspeed km/h\n이동 거리: $printdistance km"

                    // 속도 출력 (app 로그에서 확인)
                    // Log.d("Speed", "Speed: $speedInKmh km/h")
                    // Log.d("Distance", "Distance: $printdistance km")


                    latLngList.removeAt(0)
                }

                // 버튼이 눌리면, 이동경로 및 거리 초기화
                binding.button.setOnClickListener{
                    mMap.clear()
                    movedistance = 0.0f
                    textView.text = "0.0 km/h\n이동 거리: 0.0 km"
                }

                // 현재 위치 마커 표시
                lastMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("현재 위치"))

                // 카메라 위치 설정
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(latLngList[0].latitude, latLngList[0].longitude))
                    .zoom(mMap.cameraPosition.zoom)
                    .build()

                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

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
     */
}