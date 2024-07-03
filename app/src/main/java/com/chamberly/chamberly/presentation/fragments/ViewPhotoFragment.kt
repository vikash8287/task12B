package com.chamberly.chamberly.presentation.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chamberly.chamberly.R


class ViewPhotoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                          savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_view_photo, container, false)

        val imagePreview: ImageView = view.findViewById<ImageView>(R.id.image_preview)
 val backButton:ImageButton  = view.findViewById<ImageButton>(R.id.back_button)
val image_url:String = arguments?.getString("image_url")?:""
        // TODO : use cache to reduce the request send
        if(image_url == ""){
            // TODO : show some error message
        Toast.makeText(requireContext(),"Something went wrong",Toast.LENGTH_SHORT).show()

        }else{
Glide.with(this).load(image_url)
    .diskCacheStrategy(DiskCacheStrategy.DATA)
    .into(imagePreview)
        }

    backButton.setOnClickListener{
        val navController = requireActivity().findNavController(R.id.navHostFragment)
navController.popBackStack()
        Log.i("back","true")
    }
    return view
    }

}