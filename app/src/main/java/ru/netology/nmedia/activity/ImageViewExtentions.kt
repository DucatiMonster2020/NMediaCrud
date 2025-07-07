package ru.netology.nmedia.activity

import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import ru.netology.nmedia.R

fun ImageView.load(url: String) {
    Glide.with(this)
        .load(url.toUri())
        .circleCrop()
        .error(R.drawable.ic_error_100dp)
        .placeholder(R.drawable.ic_loading_100dp)
        .timeout(10_000)
        .into(this)
}