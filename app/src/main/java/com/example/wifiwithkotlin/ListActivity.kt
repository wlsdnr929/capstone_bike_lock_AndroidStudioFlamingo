package com.example.wifiwithkotlin

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ListActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private var selectedData: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById(R.id.listView)

        // Firebase 데이터베이스 초기화
        database = FirebaseDatabase.getInstance()
        myRef = database.getReference("$user_name") // 'your_reference'는 데이터베이스에서 가져올 위치를 나타냅니다.

        // 데이터베이스에서 데이터 읽기
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val dataList = ArrayList<String>()

                // 데이터를 가져와서 dataList에 추가
                for (snapshot in dataSnapshot.children) {
                    val time = snapshot.key // 시간 부분을 가져옴
                    time?.let {
                        dataList.add(time)
                    }
                }

                // ListView에 데이터 설정
                val adapter = ArrayAdapter(this@ListActivity, android.R.layout.simple_list_item_1, dataList)
                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read value.", error.toException())
            }
        })

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            selectedData = parent.getItemAtPosition(position) as String
            Toast.makeText(this@ListActivity, "선택된 데이터: $selectedData", Toast.LENGTH_SHORT).show()

            // RouteActivity로 넘기기
            val intent = Intent(this,RouteActivity::class.java)
            intent.putExtra("selectedData", selectedData) // 데이터 전달
            startActivity(intent)
        }
    }

}
