package com.ahandyapp.airnavx.ui.grid

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.ahandyapp.airnavx.R
import java.util.ArrayList

internal class GridViewAdapter(
    private val context: Context,
//    private val numbersInWords: Array<String>,
//    private val numberImage: IntArray
    private val numbersInWords: ArrayList<String>,
    private val gridBitmapArray: ArrayList<Bitmap>
) :
    BaseAdapter() {
    private var layoutInflater: LayoutInflater? = null
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView

    override fun getCount(): Int {
        return numbersInWords.size
    }
    override fun getItem(position: Int): Any? {
        return null
    }
    override fun getItemId(position: Int): Long {
        return 0
    }
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        if (layoutInflater == null) {
            layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }
        if (convertView == null) {
            convertView = layoutInflater!!.inflate(R.layout.grid_row_item, null)
        }
        imageView = convertView!!.findViewById(R.id.imageView)
        textView = convertView.findViewById(R.id.textView)
//        imageView.setImageResource(numberImage[position])
        imageView.setImageBitmap(gridBitmapArray[position])
        textView.text = numbersInWords[position]
        return convertView
    }
}
