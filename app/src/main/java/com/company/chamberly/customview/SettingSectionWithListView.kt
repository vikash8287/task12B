package com.company.chamberly.customview

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.company.chamberly.R

class SettingSectionWithListAndSwitchButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {
private  var dataList:ArrayList<ListWithSwitchItem>? = null
    private var  listView: ListView? = null
    init {
init(attrs)
    }

    private fun init(attrs: AttributeSet?){
        View.inflate(context, R.layout.setting_section_with_list_view_toggle_button,this)
        val title = findViewById<TextView>(R.id.title)
    listView  = findViewById<ListView>(R.id.list)

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
          listView!!.adapter = dataList?.let { ListWithSwitchAdapter( list = it, activity = context as Activity) }
    }

    data class  ListWithSwitchItem(val label:String, val subItem:String ="", val state:Boolean=false, val switchStateListener:OnCheckedChangeListener?= null)
    class ListWithSwitchAdapter(val list: ArrayList<ListWithSwitchItem>, val activity: Activity):
        BaseAdapter(){
        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Any {
            return  list[position]
        }

        override fun getItemId(position: Int): Long {
            return  list.size.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            val view:View?
            val vh:ViewHolder?
            if (convertView==null){
                view=activity.layoutInflater.inflate(R.layout.setting_list_item_layout_with_toggle,parent,false)
                vh = ViewHolder(view)
                view.tag = vh
            }else{
                view = convertView
                vh = view.tag as ViewHolder

            }
            vh.label.text = list[position].label
            val subItemText =list[position].subItem
            if(subItemText.isBlank()){
                vh.subItem.visibility = GONE
            }else{
                vh.subItem.text = subItemText

            }
            if(count==1){
                vh.rootLayout.background = AppCompatResources.getDrawable(activity,R.drawable.setting_single_list_item_background)

            }
            else if (position==0){
                vh.rootLayout.background = AppCompatResources.getDrawable(activity,R.drawable.setting_top_list_item_background)
            }else if(position == (list.size-1)){
                vh.rootLayout.background = AppCompatResources.getDrawable(activity,R.drawable.setting_bottom_list_item_background)

            }
            vh.switch.isChecked = list[position].state
            if(list[position].switchStateListener != null) vh.switch.setOnCheckedChangeListener(list[position].switchStateListener)
            return  view
        }
        class ViewHolder(view:View){
            val label:TextView
            val rootLayout:ConstraintLayout
            val subItem :TextView
            val switch:Switch
            init{
                label = view.findViewById<TextView>(R.id.label)
                rootLayout = view.findViewById<ConstraintLayout>(R.id.root_layout)
                subItem = view.findViewById<TextView>(R.id.subItem)
                switch = view.findViewById<Switch>(R.id.state)
            }
        }
    }



    }
