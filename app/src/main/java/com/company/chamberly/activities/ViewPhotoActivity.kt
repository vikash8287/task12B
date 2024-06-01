package com.company.chamberly.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.company.chamberly.R

class ViewPhotoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_photo)
val imagePreview: ImageView = findViewById<ImageView>(R.id.image_preview)
 val backButton:ImageButton  = findViewById<ImageButton>(R.id.back_button)
val image_url:String = intent.getStringExtra("image_url")?:""
        // TODO : use cache to reduce the request send
        if(image_url.equals("")){
            // TODO : show some error message
Toast.makeText(this,"Some error happened",Toast.LENGTH_SHORT).show()
        }else{
Glide.with(this).load(image_url)
    .diskCacheStrategy(DiskCacheStrategy.DATA)
    .into(imagePreview)
        }

    backButton.setOnClickListener{
        finish()
    }
    }
}