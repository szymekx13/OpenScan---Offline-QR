package com.openscan.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import androidx.compose.ui.graphics.Color

@Composable
fun QRScanScreen(onResult: (String) -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                hasCameraPermission = true
            }
            else -> {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    if (hasCameraPermission) {
        CameraPreview(onResult = onResult)
    }
}

@Composable
fun CameraPreview(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val previewView = remember { PreviewView(context) }

    Box(modifier = Modifier.fillMaxSize()) {

        // kamera jako pierwsza warstwa
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize()) {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            QrCodeAnalyzer(onResult)
                        )
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            }, ContextCompat.getMainExecutor(context))
        }

        // overlay na kamerę
        ScannerOverlay()
    }
}

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {

            // przyciemnienie tła
            drawRect(
                color = Color.Black.copy(alpha = 0.5f)
            )

            // wycięty środek
            val size = size.minDimension * 0.6f
            val left = (this.size.width - size) / 2
            val top = (this.size.height - size) / 2

            drawRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(size, size),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // biały border
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(size, size),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 6f
                )
            )
        }
    }
}

private class QrCodeAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader: MultiFormatReader = MultiFormatReader()

    init {
        val hints = mutableMapOf<DecodeHintType, Any>()
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
        reader.setHints(hints)
    }
    private var scanned = false;
    override fun analyze(imageProxy: ImageProxy) {

        if (imageProxy.format == ImageFormat.YUV_420_888 || imageProxy.format == ImageFormat.YUV_422_888 || imageProxy.format == ImageFormat.YUV_444_888) {
            if(scanned){
                imageProxy.close();
                return;
            }
            val yBuffer = imageProxy.planes[0].buffer // Y
            val ySize = yBuffer.remaining()
            val yBytes = ByteArray(ySize)
            yBuffer.get(yBytes)

            val source = PlanarYUVLuminanceSource(
                yBytes,
                imageProxy.width,
                imageProxy.height,
                0,
                0,
                imageProxy.width,
                imageProxy.height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result = reader.decode(binaryBitmap)
                scanned = true;
                onResult(result.text)
                Log.d("QrCodeAnalyzer", "QR Code found: ${result.text}")
            } catch (e: NotFoundException) {
                // No QR code found
            } finally {
                reader.reset()
            }
        }
        imageProxy.close()
    }
}