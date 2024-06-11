package com.company.chamberly.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.company.chamberly.R

class CustomStarRatingView @JvmOverloads constructor(
    context: Context,attrs:AttributeSet?=null,defStyleAttr:Int=0
):View(context,attrs,defStyleAttr){
    private  var rating:Float=0f
    private var starSize:Int=0

    private lateinit var emptyStar: Drawable
    private lateinit var halfStar: Drawable
    private lateinit var fullStar: Drawable

    init {
        if(attrs!=null){
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomStarRatingView)
            rating = typedArray.getFloat(R.styleable.CustomStarRatingView_rating,0f)
            starSize = typedArray.getDimensionPixelSize(R.styleable.CustomStarRatingView_starSize,50)
            emptyStar = ContextCompat.getDrawable(context,R.drawable.star_empty)!!
            halfStar = ContextCompat.getDrawable(context,R.drawable.star_half)!!
            fullStar = ContextCompat.getDrawable(context,R.drawable.star_full)!!
            typedArray.recycle()
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(20, 0, 20, 0)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in 0 until 5) {
            val left = i * starSize
            val drawable = when {
                i + 1 <= rating -> fullStar
                i < rating && i + 1 > rating -> halfStar
                else -> emptyStar
            }
            drawable.setBounds(left, 0, left + starSize, starSize)
            drawable.draw(canvas)
        }
    }

    fun setRating(newRating: Float) {
        rating = newRating
        invalidate()
    }
}