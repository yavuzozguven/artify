package com.example.artify

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.*
import java.util.*
import java.util.concurrent.Executors

class FinalActivity : AppCompatActivity() {

    var passed_content : String? = null
    var passed_style : String? = null
    private lateinit var resultImageView: ImageView
    lateinit var input : InputStream
    lateinit var input2 : InputStream
    lateinit var input_style : InputStream
    lateinit var exif : ExifInterface
    lateinit var bitmap: Bitmap
    lateinit var bitmap_style: Bitmap

    private lateinit var save_img : CardView

    private lateinit var back_arrow : LottieAnimationView


    private lateinit var tick_save : LottieAnimationView
    private lateinit var click_save : LottieAnimationView



    private lateinit var viewModel: MLExecutionViewModel
    private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainScope = MainScope()

    private var useGPU = false

    lateinit var res : ModelExecutionResult

    var width:Int = 0
    var height:Int = 0







    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_final)



        viewModel = ViewModelProvider.AndroidViewModelFactory(application).create(
            MLExecutionViewModel::class.java
        )
        resultImageView = findViewById(R.id.resultImageView)
        save_img = findViewById(R.id.save_img)
        back_arrow = findViewById(R.id.back_arrow)
        tick_save = findViewById(R.id.tick_save)
        click_save = findViewById(R.id.click_save)

        back_arrow.playAnimation()

        back_arrow.setOnClickListener(View.OnClickListener {
            this.finish()
        })



        passed_content = intent.getStringExtra("passContent")
        passed_style = intent.getStringExtra("passStyle")
        useGPU = intent.getBooleanExtra("gpuStatus", false)
        width = intent.getIntExtra("width", 0)
        height = intent.getIntExtra("height", 0)


        styleTransferModelExecutor = StyleTransferModelExecutor(this@FinalActivity, useGPU = useGPU)



        save_img.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(
                    baseContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                if(resultImageView.drawable != null)
                saveImage(res.styledImage)
            } else {
                ActivityCompat.requestPermissions(
                    this@FinalActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100
                )
            }
        })



        input = contentResolver.openInputStream(Uri.parse(passed_content))!!
        exif = ExifInterface(input)
        input.close()
        input2 = contentResolver.openInputStream(Uri.parse(passed_content))!!
        if(passed_style!!.contains("external")){
            input_style = contentResolver.openInputStream(Uri.parse(passed_style))!!
        }
        try{
            bitmap = BitmapFactory.decodeStream(input2)
            bitmap_style = BitmapFactory.decodeStream(input_style)
        }catch (e: Exception){
            Log.d("except", e.toString())
        }
        if(passed_style!!.contains("external")){
            startRunningModel(true)
        }
        else{
            startRunningModel()
        }


        viewModel.styledBitmap.observe(
            this,
            Observer { resultImage ->
                if (resultImage != null) {
                    updateUIWithResults(resultImage)
                    res = resultImage
                }
            }
        )



    }



    private fun startRunningModel(trigger: Boolean) {
        //if (!isRunningModel && lastSavedFile.isNotEmpty() && selectedStyle.isNotEmpty()) {
        // val chooseStyleLabel: TextView = findViewById(R.id.choose_style_text_view)
        //chooseStyleLabel.visibility = View.GONE
        //enableControls(false)
        //setImageView(styleImageView, getUriFromAssetThumb(selectedStyle))
        //resultImageView.visibility = View.INVISIBLE
        //progressBar.visibility = View.VISIBLE
        viewModel.onApplyStyle(
            baseContext,
            passed_content.toString(),
            passed_style.toString(),
            styleTransferModelExecutor,
            inferenceThread,
            exif,
            bitmap,
            bitmap_style
        )
        //} else {
        //   Toast.makeText(this, "Previous Model still running", Toast.LENGTH_SHORT).show()
        //}

    }


    private fun startRunningModel() {
        //if (!isRunningModel && lastSavedFile.isNotEmpty() && selectedStyle.isNotEmpty()) {
        // val chooseStyleLabel: TextView = findViewById(R.id.choose_style_text_view)
        //chooseStyleLabel.visibility = View.GONE
        //enableControls(false)
        //setImageView(styleImageView, getUriFromAssetThumb(selectedStyle))
        //resultImageView.visibility = View.INVISIBLE
        //progressBar.visibility = View.VISIBLE
        viewModel.onApplyStyle(
            baseContext,
            passed_content.toString(),
            passed_style.toString(),
            styleTransferModelExecutor,
            inferenceThread,
            exif,
            bitmap,
            null
        )
        //} else {
        //   Toast.makeText(this, "Previous Model still running", Toast.LENGTH_SHORT).show()
        //}

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            this@FinalActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) ===
                                PackageManager.PERMISSION_GRANTED)
                    ) {
                        saveImage(res.styledImage)
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }


    private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
        resultImageView.visibility = View.VISIBLE
        setImageView(resultImageView, modelExecutionResult.styledImage)
    }


    private fun setImageView(imageView: ImageView, image: Bitmap) {
        Glide.with(baseContext)
            .load(image)
            .override(width, height)
            .fitCenter()
            .apply(RequestOptions().transform(RoundedCorners(30)))
            .into(imageView)
    }



    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun saveImage(bmp: Bitmap){
        try{
            val ratio:Double = (width).toDouble()/(height).toDouble()
            val resized = Bitmap.createScaledBitmap(bmp, width, height, true)
            Log.d("width", resized.width.toString())
            Log.d("height", resized.height.toString())
            Log.d("width", bmp.width.toString())
            Log.d("height", bmp.height.toString())
            Log.d("width", width.toString())
            Log.d("height", height.toString())


            var title = random()
            var myDir = File(Environment.getExternalStorageDirectory().path + "/Pictures" + "/Artify"+title+".jpg")
            val list = myDir.list()
            if(myDir.exists()){
                title = random()
                myDir = File(Environment.getExternalStorageDirectory().path + "/Pictures" + "/Artify"+title+".jpg")
            }
            val path = Environment.getExternalStorageDirectory().toString()
            var fOut : OutputStream
            val file = File(path, "/Pictures/Artify" + title + ".jpg")
            fOut = FileOutputStream(file)
            resized.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
            fOut.flush()
            fOut.close()

            Log.d("aa", path)
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "Artify")
            values.put(MediaStore.Images.Media.DESCRIPTION, title)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.MediaColumns.DATA, file.absolutePath)
            baseContext.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            /*val photoUriStr = MediaStore.Images.Media.insertImage(
                contentResolver,
                file.absolutePath,
                file.name,
                file.name
            )
            val photoUri = Uri.parse(photoUriStr)

            val now = System.currentTimeMillis() / 1000
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATE_ADDED, now)
            values.put(MediaStore.Images.Media.DATE_MODIFIED, now)
            values.put(MediaStore.Images.Media.DATE_TAKEN, now)

            contentResolver.update(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
                MediaStore.Images.Media._ID + "=?",
                arrayOf(ContentUris.parseId(photoUri).toString())
            )*/

            click_save.visibility = View.INVISIBLE
            tick_save.visibility = View.VISIBLE
            tick_save.playAnimation()



        }
        catch (e: Exception){
            Log.d("aa2", e.toString())
        }
    }

    fun random(): String? {
        val generator = Random()
        val randomStringBuilder = StringBuilder()
        val randomLength: Int = generator.nextInt(100)
        var tempChar: Char
        for (i in 0 until randomLength) {
            tempChar = (generator.nextInt(96) + 32).toChar()
            randomStringBuilder.append(tempChar)
        }
        return randomStringBuilder.toString()
    }




    fun getImagePath(uri: Uri?): String? {
        var cursor: Cursor? = contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToFirst()
        var document_id: String = cursor.getString(0)
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
        cursor.close()
        cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, MediaStore.Images.Media._ID + " = ? ", arrayOf(document_id), null
        )
        cursor!!.moveToFirst()
        val path: String = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor!!.close()
        return path
    }

    @Throws(IOException::class)
    fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String?): Bitmap? {
        val ei = androidx.exifinterface.media.ExifInterface(image_absolute_path!!)
        val orientation: Int =
            ei.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        return when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotate(bitmap, 90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotate(
                bitmap,
                180f
            )
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotate(
                bitmap,
                270f
            )
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(
                bitmap,
                true,
                false
            )
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(
                bitmap,
                false,
                true
            )
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


}