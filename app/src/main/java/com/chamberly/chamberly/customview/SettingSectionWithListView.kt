package com.chamberly.chamberly.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.chamberly.chamberly.R

class SettingSectionWithListAndSwitchButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {
private  var dataList:ArrayList<ListWithSwitchItem>? = null
 //   private var  listView: ListView? = null
    private  lateinit var linearLayout:LinearLayout
    init {
init(attrs)
    }

    private fun init(attrs: AttributeSet?){
        View.inflate(context, R.layout.setting_section_with_list_view_toggle_button,this)
        val title = findViewById<TextView>(R.id.title)
   // listView  = findViewById<ListView>(R.id.list)
linearLayout = findViewById<LinearLayout>(R.id.list)
        val ta = context.obtainStyledAttributes(attrs,R.styleable.SettingSectionWithListAndSwitchButton)
        try{
val text = ta.getText(R.styleable.SettingSectionWithListAndSwitchButton_text)
      title.text = text

        }finally {

            ta.recycle()
        }

    }
      fun  setData(list:ArrayList<ListWithSwitchItem>){
        this.dataList = list
        //  listView!!.adapter = dataList?.let { ListWithSwitchAdapter( list = it, activity = context as Activity) }
updateList()
      }

    fun updateList(){
    val inflater = LayoutInflater.from(context)
        val size = this.dataList!!.size
        for((position,item) in this.dataList!!.withIndex()){
            val view = inflater.inflate(R.layout.setting_list_item_layout_with_toggle,linearLayout,false)
            val  label = view.findViewById<TextView>(R.id.label)
            val  rootLayout = view.findViewById<ConstraintLayout>(R.id.root_layout)
            val  subItem = view.findViewById<TextView>(R.id.subItem)
            val  switch = view.findViewById<Switch>(R.id.state)
            getView(position,linearLayout, item = item,size,label=label,rootLayout=rootLayout,subItem=subItem,switch=switch)

            linearLayout.addView(view)
        }

    }


    data class  ListWithSwitchItem(val label:String, val subItem:String ="", val state:Boolean=false, val switchStateListener:OnCheckedChangeListener?= null)
//    class ListWithSwitchAdapter(val list: ArrayList<ListWithSwitchItem>, val activity: Activity):
//        BaseAdapter(){
//        override fun getCount(): Int {
//            return list.size
//        }
//
//        override fun getItem(position: Int): Any {
//            return  list[position]
//        }
//
//        override fun getItemId(position: Int): Long {
//            return  list.size.toLong()
//        }

       private  fun getView(
           position: Int,
           view: View,
           item: ListWithSwitchItem,
           size: Int,
           label: TextView,
           rootLayout: ConstraintLayout,
           subItem: TextView,
           switch: Switch
       ): View {




            label.text = item.label
            val subItemText =item.subItem
            if(subItemText.isBlank()){
                subItem.visibility = GONE
            }else{
                subItem.text = subItemText

            }
            if(position==0 && size ==1){
                rootLayout.background = AppCompatResources.getDrawable(context,R.drawable.setting_single_list_item_background)

            }
            else if (position==0){
                rootLayout.background = AppCompatResources.getDrawable(context,R.drawable.setting_top_list_item_background)
            }else if(position == (size-1)){
                rootLayout.background = AppCompatResources.getDrawable(context,R.drawable.setting_bottom_list_item_background)

            }
            switch.setOnCheckedChangeListener(null);
            switch.isChecked = item.state
            if(item.switchStateListener != null) switch.setOnCheckedChangeListener(item.switchStateListener)
            return  view
        }

    }



