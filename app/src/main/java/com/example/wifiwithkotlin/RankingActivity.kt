package com.example.wifiwithkotlin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.wifiwithkotlin.databinding.ActivityRankingBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson

class RankingActivity : AppCompatActivity() {

    private lateinit var binding:ActivityRankingBinding
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var databaseReference : DatabaseReference

    var mm = mutableMapOf<String,Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDatabase()

        getData()

        binding.btnGraph.setOnClickListener {
            val jsonString = Gson().toJson(mm)
            val intent = Intent(this, GraphActivity::class.java)
            intent.putExtra("mapJson", jsonString)
            startActivity(intent)
        }

        binding.btnRoute.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            //intent.putExtra("distance", user_distance)
            startActivity(intent)
        }

    }


    fun initDatabase(){
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("user_name")
    }

    fun getData(){

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(shot in snapshot.children) {
                    val name = shot.key
                    val dis = shot.value.toString()
//                    val p_name = data
//                    val p_phone = data2
//                    val p = DistanceData(name, dis.toFloat())


                    mm.put(name!!,dis.toFloat())

                }

                mm = mm.toList().sortedByDescending { it.second }.toMap() as MutableMap<String, Float>

                var cnt=0
                for(i in mm){
                    Log.e("목록","${i.key} : ${i.value}")
                    if(cnt==0){
                        binding.TV1.text = i.key.toString()
                    }
                    if(cnt==1){
                        binding.TV2.text = i.key.toString()
                    }
                    if(cnt==2){
                        binding.TV3.text = i.key.toString()
                    }
                    cnt++
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}