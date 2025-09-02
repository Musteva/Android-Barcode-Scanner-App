package com.example.barcode_scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.barcode_scanner.ui.theme.Barcode_ScannerTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

data class Product(val barcode: String, val name: String, val imageName: String)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Barcode_ScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val products = remember { loadProducts(context) }
    var foundProduct by remember { mutableStateOf<Product?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    if (isScanning) {
        CameraScreen(
            onBarcodeFound = { barcode ->
                foundProduct = products[barcode]
                isScanning = false
            }
        )
    } else {
        ProductResultScreen(
            product = foundProduct,
            onScanAgain = {
                foundProduct = null
                isScanning = true
            }
        )
    }
}

@Composable
fun ProductResultScreen(product: Product?, onScanAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (product != null) {
            Text("Product Found!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            val context = LocalContext.current
            val imageResId = remember(product.imageName) {
                val cleanedImageName = product.imageName
                    .lowercase()
                    .replace("-", "_")
                    .substringBefore(".")
                context.resources.getIdentifier(cleanedImageName, "drawable", context.packageName)
            }

            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Image Found", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Text("Product Not Found in Database", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onScanAgain) {
            Text("Scan Again")
        }
    }
}

@Composable
fun CameraScreen(onBarcodeFound: (String) -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(onBarcodeFound = onBarcodeFound)
        } else {
            Text("Camera permission is required.", modifier = Modifier.align(Alignment.Center))
        }
    }
}


@Composable
fun CameraPreview(onBarcodeFound: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var key by remember { mutableStateOf(Unit) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, BarcodeAnalyzer { barcodeValue ->
                            onBarcodeFound(barcodeValue)
                        })
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize(),
        update = { key = Unit }
    )
}

class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private var hasScanned = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (hasScanned) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        hasScanned = true
                        barcodes.first()?.rawValue?.let {
                            onBarcodeDetected(it)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("BarcodeAnalyzer", "Barcode analysis failed.", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val scanner = BarcodeScanning.getClient(options)
}

fun loadProducts(context: Context): Map<String, Product> {
    val productMap = mutableMapOf<String, Product>()
    try {
        val inputStream = context.resources.openRawResource(R.raw.barcode_list)
        val reader = inputStream.bufferedReader()
        reader.readLine()
        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 4) {
                val product = Product(
                    barcode = tokens[0].trim(),
                    name = tokens[2].trim().replace("\"", ""),
                    imageName = tokens[3].trim().replace("\"", "")
                )
                productMap[product.barcode] = product
            }
        }
        reader.close()
    } catch (e: Exception) {
        Log.e("LoadProducts", "Error reading barcode list", e)
    }
    return productMap
}
