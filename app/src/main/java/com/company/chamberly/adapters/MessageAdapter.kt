package com.company.chamberly.adapters


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.company.chamberly.models.Message
import com.company.chamberly.R
import com.company.chamberly.activities.ViewPhotoActivity
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.CropSquareTransformation

class MessageAdapter(private val uid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages: MutableList<Message> = mutableListOf()
    private var onMessageLongClickListener: OnMessageLongClickListener? = null

    fun setOnMessageLongClickListener(listener: OnMessageLongClickListener) {
        onMessageLongClickListener = listener
    }

    companion object {
        private const val VIEW_TYPE_ME = 1
        private const val VIEW_TYPE_SYSTEM = 2
        private const val VIEW_TYPE_OTHER = 3
        private  const val VIEW_TYPE_PHOTO_ME = 4
        private  const val  VIEW_TYPE_PHOTO_OTHER =5

    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSender: TextView = itemView.findViewById(R.id.text_gchat_user_other)
        val textMessage: TextView = itemView.findViewById(R.id.text_gchat_message_other)
        val reactedWithHolder: TextView = itemView.findViewById(R.id.reactedWith)
        val replyingToHolder: TextView = itemView.findViewById(R.id.replyingToHolder)
        init {
            // set on long click listener
            itemView.setOnLongClickListener {
                val message = messages[bindingAdapterPosition]
                onMessageLongClickListener!!.onMessageLongClick(message)
                onMessageLongClickListener!!.onSelfLongClick(message)
                true
            }

        }
    }

    inner class MessageViewHolderMe(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.text_gchat_message_me)
        val reactedWithHolder: TextView = itemView.findViewById(R.id.reactedWith)
        val replyingToHolder: TextView = itemView.findViewById(R.id.replyingToHolder)
    }
inner class MessagePhotoViewHolderSystemMe(itemView: View):RecyclerView.ViewHolder(itemView){
    val imageView:ImageView = itemView.findViewById(R.id.image_preview)
    val progressView :ProgressBar = itemView.findViewById(R.id.progressBar)

}
    inner class MessagePhotoViewHolderSystemOther(itemView: View):RecyclerView.ViewHolder(itemView){
        val imageView:ImageView = itemView.findViewById(R.id.image_preview)
      val progressView :ProgressBar = itemView.findViewById(R.id.progressBar)


    }
    inner class MessageViewHolderSystem(itemView: View): RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.text_system_message)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        Log.d("ViewType",viewType.toString())
        return when (viewType) {
            VIEW_TYPE_ME -> {
                val itemView = inflater.inflate(R.layout.item_chat_me, parent, false)
                MessageViewHolderMe(itemView)
            }
            VIEW_TYPE_PHOTO_ME ->{
                val itemView = inflater.inflate(R.layout.item_message_photo_me,parent,false)
               MessagePhotoViewHolderSystemMe(itemView)
            }
            VIEW_TYPE_PHOTO_OTHER ->{
               val itemView = inflater.inflate(R.layout.item_message_photo_other,parent,false)
                MessagePhotoViewHolderSystemOther(itemView)
            }
            VIEW_TYPE_SYSTEM -> {
                val itemView = inflater.inflate(R.layout.item_system_message, parent, false)
                MessageViewHolderSystem(itemView)
            }

            VIEW_TYPE_OTHER -> {
                val itemView = inflater.inflate(R.layout.item_chat_other, parent, false)
                MessageViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is MessageViewHolderMe -> {
                holder.textMessage.text = message.message_content
                holder.itemView.setOnLongClickListener {
                    onMessageLongClickListener?.onSelfLongClick(message)
                    true
                }
                holder.reactedWithHolder.text = message.reactedWith.ifBlank { "" }
                holder.reactedWithHolder.visibility = if(message.reactedWith.isNotBlank()) View.VISIBLE else View.GONE
                holder.replyingToHolder.text = if(message.replyingTo.isNotBlank()) "Replying to: ${message.replyingTo}" else ""
                holder.replyingToHolder.visibility = if(message.replyingTo.isNotBlank()) View.VISIBLE else View.GONE
            }
            is MessageViewHolder -> {
                if(position != 0 && message.UID == messages[position - 1].UID && messages[position - 1].message_type == "text") {
                    holder.textSender.visibility = View.GONE
                } else {
                    holder.textSender.visibility = View.VISIBLE
                    holder.textSender.text = message.sender_name
                }
                holder.itemView.setOnLongClickListener {
                    onMessageLongClickListener?.onMessageLongClick(message)
                    true
                }
                holder.textMessage.text = message.message_content
                holder.reactedWithHolder.text = message.reactedWith.ifBlank { "" }
                holder.reactedWithHolder.visibility = if(message.reactedWith.isNotBlank()) View.VISIBLE else View.GONE
                holder.replyingToHolder.text = if(message.replyingTo.isNotBlank()) "Replying to: ${message.replyingTo}" else ""
                holder.replyingToHolder.visibility = if(message.replyingTo.isNotBlank()) View.VISIBLE else View.GONE
            }
            is MessagePhotoViewHolderSystemMe->{
                val multi = MultiTransformation<Bitmap>(
                    BlurTransformation(150),
                    CropSquareTransformation()
                )
val image_view = holder.imageView
                val Context = holder.imageView.context
                val progressBar = holder.progressView
                progressBar.visibility = View.VISIBLE
                image_view.isClickable =false

// Todo: Add Exception
                Glide
                    .with(Context)

                    .load(message.message_content)
                    .listener(
                        object:RequestListener<Drawable>{
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                image_view.isClickable =false
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressBar.visibility = View.GONE
                                image_view.isClickable =true

                                return  false
                            }

                        }
                    )
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .apply(RequestOptions.bitmapTransform(multi))

                    .into(image_view)
                image_view.setOnClickListener{
val intent  = Intent(Context,ViewPhotoActivity::class.java)
                    intent.putExtra("image_url",message.message_content)
                    Context.startActivity(intent)

            }
            }
            is MessagePhotoViewHolderSystemOther->{
                val multi = MultiTransformation<Bitmap>(
                    BlurTransformation(150),
                    CropSquareTransformation()
                )
                val image_view = holder.imageView
                val Context = holder.imageView.context
                val progressBar = holder.progressView
                progressBar.visibility = View.VISIBLE
                image_view.isClickable =false

// Todo: Add Exception
                Glide
                    .with(Context)

                    .load(message.message_content)
                    .listener(
                        object:RequestListener<Drawable>{
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
image_view.isClickable = false
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressBar.visibility = View.GONE
                                image_view.isClickable =true
                                return  false
                            }

                        }
                    )
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .apply(RequestOptions.bitmapTransform(multi))
                    .into(image_view)
                image_view.setOnClickListener{
                    val intent  = Intent(Context,ViewPhotoActivity::class.java)
                    intent.putExtra("image_url",message.message_content)
                    Context.startActivity(intent)

                }
            }
            is MessageViewHolderSystem -> {
                holder.message.text = message.message_content
            }
        }
    }

    fun setMessages(messages: List<Message>) {
        this.messages.clear()
        this.messages.addAll(messages)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun addMessage(message: Message, position: Int = -1) {
        if(position != -1) {
            messages.add(position, message)
            notifyItemInserted(position)
        } else {
            if(messages.contains(message)) {
                return
            }
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
    }

    fun messageChanged(message: Message, messageId: String) {
        for(i in 0 until messages.size) {
            if (messages[i].message_id == messageId) {
                messages[i] = message
                notifyItemChanged(i)
                return
            }
        }
    }

    fun messageRemoved(message: Message) {
        for(i in 0 until messages.size) {
            if (messages[i].message_id == message.message_id) {
                messages.removeAt(i)
                notifyItemRemoved(i)
                return
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.UID == uid && message.message_type!="photo") {
            VIEW_TYPE_ME


        }
        else if (message.message_type == "photo" && message.UID == uid){
            VIEW_TYPE_PHOTO_ME
        }
        else if (message.message_type == "photo" ){
            VIEW_TYPE_PHOTO_OTHER
        }

        else if (message.message_type == "custom" || message.message_type == "system" ) {
            VIEW_TYPE_SYSTEM
        } else {
            VIEW_TYPE_OTHER
        }
    }

    interface OnMessageLongClickListener {
        fun onMessageLongClick(message: Message)
        fun onSelfLongClick(message: Message)
    }
}