package com.example.artify

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.net.toUri
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.example.artify.ImageUtils
import java.net.URI.create
import java.nio.charset.Charset
import java.security.MessageDigest

class StyleSelection : AppCompatActivity(),StyleFragment.OnListFragmentInteractionListener {

    var passed: String? = null
    lateinit var passStyle : String
    var tick: LottieAnimationView? = null
    var click_content: LottieAnimationView? = null
    private var selectedStyle: String = ""
    private lateinit var styleImageView: ImageView
    private val stylesFragment: StyleFragment = StyleFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_style_selection)


        //hooks
        styleImageView = findViewById(R.id.style_imageview)
        tick = findViewById(R.id.tick_content)
        click_content = findViewById(R.id.click_content)

        passed = intent.getStringExtra("content")


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            styleImageView.clipToOutline = true
        }


        styleImageView.setOnClickListener {
            stylesFragment.show(supportFragmentManager, "StylesFragment")
        }

        tick?.setOnClickListener(View.OnClickListener {
            val thirdIntent = Intent(this, FinalActivity::class.java)
            thirdIntent.putExtra("passContent",passed)
            thirdIntent.putExtra("passStyle",passStyle)
            startActivity(thirdIntent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        })





    }







    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right)
    }

    override fun onListFragmentInteraction(item: String) {
        selectedStyle = item
        stylesFragment.dismiss()
        if(item.equals("pp.jpg")){
            pickImageFromGallery()
        }
        else{
            setImageView(styleImageView, getUriFromAssetThumb(selectedStyle))
            tick?.playAnimation()
            click_content?.visibility = View.INVISIBLE
            Log.d("yol",item.toString())
            passStyle = item
        }
    }

    private fun getUriFromAssetThumb(thumb: String): String {
        return "file:///android_asset/thumbnails/$thumb"
    }

    private fun setImageView(imageView: ImageView, imagePath: String) {
        Glide.with(baseContext)
            .asBitmap()
            .load(imagePath)
            .override(512, 512)
            .apply(RequestOptions().transform(CropTop()))
            .into(imageView)
    }

    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,1000)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == 1000){
            styleImageView?.setImageURI(data?.data)
            tick?.playAnimation()
            click_content?.visibility = View.INVISIBLE
            passStyle = (data?.data).toString()
        }
    }

















    class CropTop : BitmapTransformation() {
        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            return if (toTransform.width == outWidth && toTransform.height == outHeight) {
                toTransform
            } else ImageUtils.scaleBitmapAndKeepRatio(toTransform, outWidth, outHeight)
        }

        override fun equals(other: Any?): Boolean {
            return other is CropTop
        }

        override fun hashCode(): Int {
            return ID.hashCode()
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(ID_BYTES)
        }

        companion object {
            private const val ID = "org.tensorflow.lite.examples.styletransfer.CropTop"
            private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
        }
    }


}