package com.ephemeral.artify

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.ads.*
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.nio.charset.Charset
import java.security.MessageDigest


class MainActivity : AppCompatActivity(),StyleFragment.OnListFragmentInteractionListener {

    var content_img: CardView? = null
    var tick_content: LottieAnimationView? = null
    var click_content: LottieAnimationView? = null
    var style_img: CardView? = null
    var tick_style: LottieAnimationView? = null
    var click_style:LottieAnimationView? = null

    lateinit var card_content : ImageView
    lateinit var card_style : ImageView

    lateinit var gpu_switch : Switch
    lateinit var card_gpu: ImageView

    lateinit var forward_anim : LottieAnimationView

    private val stylesFragment: StyleFragment = StyleFragment()
    private var selectedStyle: String = ""

    lateinit var textView: TextView
    var passing_content: Uri? = null
    lateinit var passing_contentstr : String
    lateinit var passStyle : String

    lateinit var bmp_w_h : Bitmap


    var trigger_content : Boolean = false
    var trigger_style : Boolean = false

    private var useGPU = false

    private lateinit var mInterstitialAd: InterstitialAd

    private lateinit var linear_lay : LinearLayout
    private lateinit var loading_anim: LottieAnimationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        //hooks
        content_img = findViewById(R.id.content_img)
        tick_content = findViewById(R.id.tick_content)
        click_content = findViewById(R.id.click_content)
        card_content = findViewById(R.id.card_content)

        style_img = findViewById(R.id.style_img)
        tick_style = findViewById(R.id.tick_style)
        click_style = findViewById(R.id.click_style)
        card_style = findViewById(R.id.card_style)

        gpu_switch = findViewById(R.id.gpu_switch)
        card_gpu = findViewById(R.id.card_gpu)

        forward_anim = findViewById(R.id.forward_anim)
        forward_anim.isClickable = false

        textView = findViewById(R.id.textView)

        trigger_content = false
        trigger_style = false

        linear_lay = findViewById(R.id.linear_lay)
        loading_anim = findViewById(R.id.loading_anim)

        MobileAds.initialize(this) {}

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-9480100348722027/1129472043"



        val filter = card_gpu.colorFilter
        gpu_switch.setOnCheckedChangeListener{ _, isChecked ->
            if (isChecked){
                card_gpu.setColorFilter(Color.argb(255, 47, 84, 77))
                useGPU = true
            }
            else{
                card_gpu.setColorFilter(filter)
                useGPU = false
            }
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            content_img?.clipToOutline = true
        }


        //content image listener
        content_img?.setOnClickListener(View.OnClickListener {
            //if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            pickImageFromGallery()
            //}
        })

        //style image listener
        style_img?.setOnClickListener {
            stylesFragment.show(supportFragmentManager, "StylesFragment")
        }

    }


    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }







    private fun xx(){
        forward_anim.setOnClickListener(View.OnClickListener {
            if (isConnected(this)) {
                val animat = AnimationUtils.loadAnimation(this, R.anim.tick_out)
                forward_anim.animation = animat
                Handler(Looper.getMainLooper()).postDelayed({
                    forward_anim.visibility = View.INVISIBLE
                    mInterstitialAd.loadAd(AdRequest.Builder().build())
                }, 250)
                Handler(Looper.getMainLooper()).postDelayed({
                }, 100)
                loading_anim.visibility = View.VISIBLE
                loading_anim.playAnimation()
                loading_anim.repeatCount = LottieDrawable.INFINITE
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.show()
                } else {
                    mInterstitialAd.loadAd(AdRequest.Builder().build())
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                }


                val finalIntent = Intent(this, FinalActivity::class.java)


                mInterstitialAd.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        mInterstitialAd.show()

                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        // Code to be executed when an ad request fails.
                        Log.d("ee",adError.toString())
                        forward_anim.visibility = View.VISIBLE
                        loading_anim.visibility = View.INVISIBLE
                        finalIntent.putExtra("passContent", passing_contentstr)
                        finalIntent.putExtra("passStyle", passStyle)
                        finalIntent.putExtra("gpuStatus", useGPU)
                        finalIntent.putExtra("width", bmp_w_h.width)
                        finalIntent.putExtra("height", bmp_w_h.height)
                        startActivity(finalIntent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }

                    override fun onAdOpened() {
                        // Code to be executed when the ad is displayed.

                    }

                    override fun onAdClicked() {
                        // Code to be executed when the user clicks on an ad.
                    }

                    override fun onAdLeftApplication() {
                        // Code to be executed when the user has left the app.
                    }

                    override fun onAdClosed() {
                        // Code to be executed when the interstitial ad is closed.
                        forward_anim.visibility = View.VISIBLE
                        loading_anim.visibility = View.INVISIBLE
                        finalIntent.putExtra("passContent", passing_contentstr)
                        finalIntent.putExtra("passStyle", passStyle)
                        finalIntent.putExtra("gpuStatus", useGPU)
                        finalIntent.putExtra("width", bmp_w_h.width)
                        finalIntent.putExtra("height", bmp_w_h.height)
                        startActivity(finalIntent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }

            } else {
                var snackbar = Snackbar.make(
                    linear_lay,
                    "Check your internet connection",
                    Snackbar.LENGTH_LONG
                )
                snackbar.show()
            }
        })

    }

    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1000)
    }
    private fun pickStyleFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1001)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == 1000){
            //card_content.setImageURI(data?.data)
            /*bmp_w_h = MediaStore.Images.Media.getBitmap(this.contentResolver, data?.data)
            val bitmap_content = modifyOrientation(bmp_w_h, getImagePath(data?.data))
            bmp_w_h = bitmap_content!!
            card_content.setImageBitmap(bitmap_content)*/


            Glide.with(this)
                .asBitmap()
                .load(data?.data)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        card_content.setColorFilter(Color.argb(0, 255, 255, 255))
                        card_content.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        card_content.setImageBitmap(resource)
                        bmp_w_h = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })

            passing_contentstr = (data?.data).toString()
            passing_content = data?.data
            tick_content?.playAnimation()
            click_content?.visibility = View.INVISIBLE
            trigger_content = true
            if(trigger_style){
                forward_anim.visibility = View.VISIBLE
                xx()
                forward_anim.playAnimation()
            }
        }
        if(resultCode == Activity.RESULT_OK && requestCode == 1001){
            card_style.setColorFilter(Color.argb(0, 255, 255, 255))
            card_style.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            /*val bmp_style = MediaStore.Images.Media.getBitmap(this.contentResolver, data?.data)
            val bitmap_content = modifyOrientation(bmp_style, getImagePath(data?.data))
            card_style.setImageBitmap(bitmap_content)
            //card_style.setImageURI(data?.data)*/

            Glide.with(baseContext)
                .load(data?.data)
                .into(card_style)

            tick_style?.playAnimation()
            click_style?.visibility = View.INVISIBLE
            passStyle = (data?.data).toString()
            trigger_style = true
            if(trigger_content){
                forward_anim.visibility = View.VISIBLE
                xx()
                forward_anim.playAnimation()
            }
        }
    }

    private fun setImageView(imageView: ImageView, imagePath: String) {
        Glide.with(baseContext)
            .asBitmap()
            .load(imagePath)
            .override(512, 512)
            .apply(RequestOptions().transform(MainActivity.CropTop()))
            .into(imageView)
        card_style.setColorFilter(Color.argb(0, 255, 255, 255))
        card_style.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    private fun getUriFromAssetThumb(thumb: String): String {
        return "file:///android_asset/thumbnails/$thumb"
    }

    override fun onListFragmentInteraction(item: String) {
        selectedStyle = item
        stylesFragment.dismiss()
        if(item.equals("galleryf.jpg")){
            //if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            pickStyleFromGallery()
            //}
        }
        else{
            setImageView(card_style, getUriFromAssetThumb(selectedStyle))
            click_style?.visibility = View.INVISIBLE
            tick_style?.playAnimation()
            passStyle = item
            trigger_style = true
            if(trigger_content){
                forward_anim.visibility = View.VISIBLE
                xx()
                forward_anim.playAnimation()
            }
        }
    }


    fun getImagePath(uri: Uri?): String? {
        var cursor: Cursor? = contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToFirst()
        var document_id: String = cursor.getString(0)
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
        cursor.close()
        cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            MediaStore.Images.Media._ID + " = ? ",
            arrayOf(
                document_id
            ),
            null
        )
        cursor!!.moveToFirst()
        val path: String = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor!!.close()
        return path
    }

    @Throws(IOException::class)
    fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String?): Bitmap? {
        val ei = ExifInterface(image_absolute_path!!)
        val orientation: Int =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate(bitmap, 270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(bitmap, true, false)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(bitmap, false, true)
            else -> bitmap
        }
    }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap? {
        val matrix = Matrix()
        matrix.preScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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


    val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123

    fun checkPermissionREAD_EXTERNAL_STORAGE(context: Context?): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        return if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    showDialog(
                        "External storage", context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                } else {
                    ActivityCompat
                        .requestPermissions(
                            context as Activity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                        )
                }
                false
            } else {
                true
            }
        } else {
            true
        }
    }


    fun showDialog(msg: String, context: Context?, permission: String){
        val alertBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("$msg permission is necessary")
        alertBuilder.setPositiveButton(android.R.string.yes,
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    ActivityCompat.requestPermissions(
                        (context as Activity?)!!, arrayOf(permission),
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                    )
                }
            })
        val alert: AlertDialog = alertBuilder.create()
        alert.show()
    }






}