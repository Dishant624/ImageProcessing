package com.livebird.imageprocessing

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.File


const val GALLERY_REQUEST_CODE = 100

const val CAMERA_REQUEST_CODE_1 = 201
const val CAMERA_REQUEST_CODE_2 = 202
const val CAMERA_REQUEST_CODE_3 = 203
const val CAMERA_REQUEST_CODE_4 = 204
const val CAMERA_REQUEST_CODE_5 = 205
const val CAMERA_REQUEST_CODE_6 = 206

const val PERMISSION_REQUEST_CODE = 300

const val CAPTURE_COMBO_REQUEST_CODE = 400
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var cameraUri: Uri? = null
    private var isButtonsVisible = false
    private var imageFilePath: String? = null


    private val captureImageCombo = View.OnClickListener {
        group.visibility = View.GONE
        startActivityForResult(getPickImageChooserIntent(), CAPTURE_COMBO_REQUEST_CODE)
    }

    private val galleryClick = View.OnClickListener {
        group.visibility = View.GONE
        intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        )
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    /*The Camera application, when triggered
      via an intent, does not return the full-size image back to the calling activity. In general,
      doing so would require quite a bit of memory, and the mobile device is generally
      constrained in this respect. Instead the Camera application returns a small thumbnail
      image in the returned intent
      */
    private val cameraClick_1 = View.OnClickListener {
        group.visibility = View.GONE
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE_1)
    }

    private val cameraClick_2 = View.OnClickListener {
        group.visibility = View.GONE
        cameraUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues()
        )!!
        Log.d(TAG, "onCreate: $cameraUri")
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        startActivityForResult(intent, CAMERA_REQUEST_CODE_2)
    }
    
    /*Refer this link about uri used by other apps
    https://stackoverflow.com/q/38200282/7265720
    * Capture larger image */
    private val cameraClick_3 = View.OnClickListener {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            imageFilePath = "${getExternalFilesDir(null)?.absolutePath}/myPic.jpg"
        } else {
            imageFilePath = "${Environment.getExternalStorageDirectory().absolutePath}/myPic.jpg"
        }
        val imageFile = File(imageFilePath)
        // not working in android  7.0 and above
        // val imageFileUri = Uri.fromFile(imageFile)
        val imageFileUri = FileProvider.getUriForFile(
            applicationContext,
            "com.livebird.imageprocessing", imageFile
        )


        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri)
        startActivityForResult(intent, CAMERA_REQUEST_CODE_3)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE
        )

        fab_main.setOnClickListener(View.OnClickListener {
            if (isButtonsVisible) {
                isButtonsVisible = false
                group.visibility = View.GONE
            } else {
                isButtonsVisible = true
                group.visibility = View.VISIBLE
            }
        })


        fab_gallery.setOnClickListener(galleryClick)
        card_gallery.setOnClickListener(galleryClick)

        fab_camera.setOnClickListener(cameraClick_3)
        card_camera.setOnClickListener(cameraClick_3)

        capture_btn.setOnClickListener(captureImageCombo)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {

            when (requestCode) {

                GALLERY_REQUEST_CODE -> {
                    val uri = data?.data
                    Log.d(TAG, "onActivityResult: $uri")

                    val bitmap = BitmapFactory.decodeStream(
                        contentResolver.openInputStream(uri!!),
                        null, null
                    )

                    image.setImageBitmap(bitmap)
                }

                CAMERA_REQUEST_CODE_1 -> {
                    val bitmap: Bitmap? = data?.extras?.get("data") as Bitmap?
                    Log.d(TAG, "onActivityResult: $bitmap")

                    bitmap?.let {
                        image.setImageBitmap(it)
                    }
                }

                CAMERA_REQUEST_CODE_2 -> {
                    cameraUri?.let {
                        val bitmap = BitmapFactory.decodeStream(
                            contentResolver
                                .openInputStream(it),
                            null, null
                        )

                        image.setImageBitmap(bitmap)
                    }
                }

                //https://stackoverflow.com/a/11623693/7265720
                CAMERA_REQUEST_CODE_3 -> {

                    val size = Point()
                    val realSize = Point()
                    windowManager.defaultDisplay.getSize(size)
                    windowManager.defaultDisplay.getRealSize(realSize)
                    val displayWidth = size.x //1080
                    val displayHeight = size.y //2030

                    val displayRealWidht = realSize.x // 1080
                    val displayRealHeight = realSize.y//2160

                    val density = resources.displayMetrics.densityDpi //440 xhdpi
                    val densityDpi = resources.displayMetrics.density // 2.75

                    val bitmapFactoryOptions = BitmapFactory.Options()
                    bitmapFactoryOptions.inJustDecodeBounds = true
                    var bitmap = BitmapFactory.decodeFile(imageFilePath, bitmapFactoryOptions)

                    val heightRatio =
                        Math.ceil((bitmapFactoryOptions.outHeight / displayRealHeight).toDouble())
                            .toInt()
                    val widthRatio =
                        Math.ceil((bitmapFactoryOptions.outWidth / displayRealWidht).toDouble())
                            .toInt()

                    if (heightRatio > 1 && widthRatio > 1) {

                        if (heightRatio > widthRatio) {
                            bitmapFactoryOptions.inSampleSize = heightRatio
                        } else {
                            bitmapFactoryOptions.inSampleSize = widthRatio
                        }
                    }

                    bitmapFactoryOptions.inJustDecodeBounds = false
                    bitmap = BitmapFactory.decodeFile(imageFilePath, bitmapFactoryOptions)

                    val rightBitmap = rotatedBitmapIfRequired(imageFilePath, bitmap)
                    image.setImageBitmap(rightBitmap)

                }

                CAPTURE_COMBO_REQUEST_CODE -> {
                    val isFormCamera = data == null || data.extras?.get("data") == null

                    if (isFormCamera) {
                        getCaptureImageOutputUri()?.let {
                            val bitmap = BitmapFactory.decodeStream(
                                contentResolver
                                    .openInputStream(it), null, null)

                            image.setImageBitmap(bitmap)
                        }
                    } else {
                        val filePath = getFilePathFromUri(data)
                        val bitmap = BitmapFactory.decodeFile(filePath)
                        val rightBitmap = rotatedBitmapIfRequired(filePath, bitmap)
                        image.setImageBitmap(rightBitmap)
                    }


                }


            }

        }
    }

    private fun getFilePathFromUri(data: Intent?): String {
        val proj =
            arrayOf(MediaStore.Audio.Media.DATA)
        val cursor: Cursor? =
            data?.data?.let { contentResolver.query(it, proj, null, null, null) }
        val column_index: Int =
            cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    /*
    * https://stackoverflow.com/a/14066265/7265720*/
    private fun rotatedBitmapIfRequired(
        imageFilePath: String?,
        bitmap: Bitmap
    ): Bitmap {

        val exifInterface = ExifInterface(imageFilePath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 90)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 90)
            }

            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    fun getPickImageChooserIntent(): Intent? {

        val imageUri = getCaptureImageOutputUri()

        val allIntent = ArrayList<Intent>()

        val packageManager = packageManager

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val listOfCamera = packageManager.queryIntentActivities(captureIntent, 0)

        listOfCamera.forEach {
            val intent = Intent(captureIntent)
            intent.component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
            intent.`package` = it.activityInfo.packageName
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            allIntent.add(intent)
        }

        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            allIntent.add(intent)
        }

        var mainIntent: Intent? = null
        allIntent.forEach {
            if (it.component?.packageName.equals("com.android.documentsui")) {
                mainIntent = it
                return@forEach
            }
        }

        allIntent.remove(mainIntent)

        val chooserIntent = Intent.createChooser(Intent(), "Select source")
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            allIntent.toArray(arrayOfNulls<Parcelable>(allIntent.size))
        )

        return chooserIntent

    }

    private fun getCaptureImageOutputUri(): Uri? {
        val imageFile =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path, "profile.png")
        return FileProvider.getUriForFile(
            applicationContext,
            "com.livebird.imageprocessing", imageFile
        )
    }
}