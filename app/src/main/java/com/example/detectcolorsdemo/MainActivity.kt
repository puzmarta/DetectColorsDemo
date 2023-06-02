package com.example.detectcolorsdemo

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.opencv.android.OpenCVLoader
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR2HSV
import org.opencv.imgproc.Imgproc.cvtColor

class MainActivity : AppCompatActivity() {


        lateinit var bitmap: Bitmap
        lateinit var imageView: ImageView
        lateinit var cameraDevice: CameraDevice
        lateinit var handler: Handler
        lateinit var cameraManager: CameraManager
        lateinit var textureView: TextureView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            //checking if opencv is working
            Log.d("OPENCV", "OPenCV Loading Status ${OpenCVLoader.initDebug()}")

            get_permission()

            val handlerThread = HandlerThread("videoThread")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            textureView = findViewById(R.id.textureView)
            textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{


                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    Log.d("CAMERA", "Camera opened for texture available")
                    openCamera()

                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    Log.d("BITMAP", "Surface update")

                    bitmap = textureView.bitmap!!

                     if (bitmap != null) {
                         Log.d("BITMAP", "Bitmap not null")
                         val colorResult = detectColors(bitmap)
                     }
                    else{
                         Log.d("BITMAP", "Bitmap is null")
                    }


                }
            }

            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        }


        @SuppressLint("MissingPermission")
        fun openCamera(){
            Log.d("CAMERA", "Camera opened")
            cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
                override fun onOpened(camera: CameraDevice) {
                    Log.d("CAMERA", "Camera opened")
                    cameraDevice = camera

                    var surfaceTexture = textureView.surfaceTexture
                    var surface = Surface(surfaceTexture)

                    var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){

                        override fun onConfigured(session: CameraCaptureSession) {
                            session.setRepeatingRequest(captureRequest.build(), null, null)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                        }
                    }, handler)
                }

                override fun onDisconnected(camera: CameraDevice) {

                }

                override fun onError(camera: CameraDevice, error: Int) {

                }
            }, handler)

        }

        fun get_permission(){

            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
            }
        }


        private fun detectColors(bitmap: Bitmap): List<String>{

            val colors = mutableListOf<String>()

            //bitmap to openCV mat object
            val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC3)
            Utils.bitmapToMat(bitmap,mat)

            //image to HSV color space
            val hsvMat = Mat(mat.rows(), mat.cols(), CvType.CV_8UC3)
            cvtColor(mat, hsvMat, COLOR_BGR2HSV)

            val lowerThreshold = Scalar(0.0,100.0,100.0)
            val upperThreshold = Scalar(10.0,255.0, 255.0)

            val mask = Mat(mat.rows(), mat.cols(), CV_8UC1)
            Core.inRange(hsvMat, lowerThreshold, upperThreshold, mask)

            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()

            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            for (contour in contours){

                val contourArea = Imgproc.contourArea(contour)

                if (contourArea > 1000){

                    val epsilon = 0.02 * contourArea
                    val approxCurve = MatOfPoint2f()
                    Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approxCurve, epsilon, true)

                    val boundingRect = Imgproc.boundingRect(MatOfPoint(*approxCurve.toArray()))

                    Imgproc.rectangle(mat, boundingRect.tl(),boundingRect.br(), Scalar(0.0, 255.0, 0.0), 3)


                    colors.add("RED")
                }

            }
            Utils.matToBitmap(mat, bitmap)

            return colors
        }


    //
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }


    }