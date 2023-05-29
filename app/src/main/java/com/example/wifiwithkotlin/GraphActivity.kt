package com.example.wifiwithkotlin

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.wifiwithkotlin.databinding.ActivityGraphBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Math.round
import java.util.Arrays

class GraphActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGraphBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding  = ActivityGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonString = intent.getStringExtra("mapJson")
        val mapType = object : TypeToken<MutableMap<String, Float>>() {}.type
        val mm = Gson().fromJson<MutableMap<String, Float>>(jsonString, mapType)

        setChartView(mm)
    }


    private fun setChartView(mm: MutableMap<String, Float>) {
        var chartWeek = binding.chartGraph
        setWeek(chartWeek, mm)
    }

    private fun initBarDataSet(barDataSet: BarDataSet) {
        //Changing the color of the bar
        barDataSet.color = Color.parseColor("#304567")
        //Setting the size of the form in the legend
        barDataSet.formSize = 15f
        //showing the value of the bar, default true if not set
        barDataSet.setDrawValues(false)
        //setting the text size of the value of the bar
        barDataSet.valueTextSize = 12f
    }

    private fun setWeek(barChart: BarChart, mm: MutableMap<String, Float>) {
        initBarChart(barChart)

        barChart.setScaleEnabled(false) //Zoom In/Out

        var userList = mm.keys.toTypedArray()
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(userList)
        val valueList = mm.values.toTypedArray()
        val entries: ArrayList<BarEntry> = ArrayList()
        val title = "이동 거리 (단위: Km)"

        //fit the data into a bar
        for (i in 0 until valueList.size) {
            // if (i >= 10) break  // 10등까지만 표시하도록 함
            val barEntry = BarEntry(i.toFloat(), round(valueList[i] * 100000)/100000.0f) // 센티미터까지 표시
            entries.add(barEntry)
        }

        val barDataSet = BarDataSet(entries, title)
        val data = BarData(barDataSet)
        barChart.data = data
        barChart.invalidate()


    }

    private fun initBarChart(barChart: BarChart) {
        //hiding the grey background of the chart, default false if not set
        barChart.setDrawGridBackground(false)
        //remove the bar shadow, default false if not set
        barChart.setDrawBarShadow(false)
        //remove border of the chart, default false if not set
        barChart.setDrawBorders(false)

        //remove the description label text located at the lower right corner
        val description = Description()
        description.setEnabled(false)
        barChart.setDescription(description)

        //X, Y 바의 애니메이션 효과
        barChart.animateY(1000)
        barChart.animateX(1000)


        //바텀 좌표 값
        val xAxis: XAxis = barChart.getXAxis()
        //change the position of x-axis to the bottom
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        //set the horizontal distance of the grid line
        xAxis.granularity = 1f
        xAxis.textColor = Color.MAGENTA
        //hiding the x-axis line, default true if not set
        xAxis.setDrawAxisLine(false)
        //hiding the vertical grid lines, default true if not set
        xAxis.setDrawGridLines(false)


        //좌측 값 hiding the left y-axis line, default true if not set
        val leftAxis: YAxis = barChart.getAxisLeft()
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = Color.RED


        //우측 값 hiding the right y-axis line, default true if not set
        val rightAxis: YAxis = barChart.getAxisRight()
        rightAxis.isEnabled = false
        rightAxis.setDrawAxisLine(false)
        rightAxis.textColor = Color.RED


        //바차트의 타이틀
        val legend: Legend = barChart.getLegend()
        //setting the shape of the legend form to line, default square shape
        legend.form = Legend.LegendForm.LINE
        //setting the text size of the legend
        legend.textSize = 11f
        legend.textColor = Color.GREEN
        //setting the alignment of legend toward the chart
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        //setting the stacking direction of legend
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        //setting the location of legend outside the chart, default false if not set
        legend.setDrawInside(false)
    }


}