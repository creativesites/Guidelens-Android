package com.craftflowtechnologies.guidelens.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * A composable that displays images from both URL and base64 formats
 * Handles XAI's URL-based images by downloading and caching them
 */
@Composable
fun URLImageDisplay(
    imageSource: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null
) {
    var imageState by remember { mutableStateOf<ImageState>(ImageState.Loading) }
    
    LaunchedEffect(imageSource) {
        imageState = ImageState.Loading
        try {
            val bitmap = when {
                imageSource.startsWith("http://") || imageSource.startsWith("https://") -> {
                    // Handle URL-based images (XAI format)
                    downloadImageFromUrl(imageSource)
                }
                imageSource.startsWith("data:image/") -> {
                    // Handle base64 images
                    decodeBase64Image(imageSource)
                }
                else -> {
                    // Assume it's a base64 string without data URI prefix
                    decodeBase64Image("data:image/png;base64,$imageSource")
                }
            }
            
            if (bitmap != null) {
                imageState = ImageState.Success(bitmap)
            } else {
                imageState = ImageState.Error("Failed to load image")
            }
        } catch (e: Exception) {
            imageState = ImageState.Error(e.message ?: "Unknown error")
        }
    }
    
    Box(modifier = modifier) {
        when (val state = imageState) {
            is ImageState.Loading -> {
                placeholder?.invoke() ?: DefaultImagePlaceholder()
            }
            is ImageState.Success -> {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            is ImageState.Error -> {
                error?.invoke() ?: DefaultImageError(state.message)
            }
        }
    }
}

/**
 * Optimized image display component specifically for stage images in cooking sessions
 */
@Composable
fun StageImageDisplay(
    stageImage: com.craftflowtechnologies.guidelens.storage.StageImage?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showPlaceholder: Boolean = true
) {
    if (stageImage?.image?.url != null) {
        URLImageDisplay(
            imageSource = stageImage.image.url,
            contentDescription = stageImage.description,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = contentScale,
            placeholder = if (showPlaceholder) { { StageImagePlaceholder(stageImage.stageNumber) } } else null,
            error = { StageImageError(stageImage.stageNumber) }
        )
    } else if (showPlaceholder) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            StageImagePlaceholder(stageImage?.stageNumber ?: 0)
        }
    }
}

/**
 * Enhanced image display for generated images with metadata
 */
@Composable
fun GeneratedImageDisplay(
    generatedImage: com.craftflowtechnologies.guidelens.storage.GeneratedImage?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showMetadata: Boolean = false
) {
    Column(modifier = modifier) {
        if (generatedImage?.url != null) {
            URLImageDisplay(
                imageSource = generatedImage.url,
                contentDescription = "Generated image: ${generatedImage.prompt}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = contentScale
            )
            
            if (showMetadata) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Generated with ${generatedImage.model}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        if (generatedImage.prompt.isNotEmpty()) {
                            Text(
                                text = "Prompt: ${generatedImage.prompt.take(100)}${if (generatedImage.prompt.length > 100) "..." else ""}",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        } else {
            DefaultImageError("No image available")
        }
    }
}

// Internal state management
private sealed class ImageState {
    object Loading : ImageState()
    data class Success(val bitmap: Bitmap) : ImageState()
    data class Error(val message: String) : ImageState()
}

// Image downloading and processing functions
private suspend fun downloadImageFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.connect()
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun decodeBase64Image(base64String: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val base64Data = if (base64String.startsWith("data:image/")) {
            // Remove data URI prefix (e.g., "data:image/png;base64,")
            base64String.substringAfter(",")
        } else {
            base64String
        }
        
        val decodedBytes = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getDecoder().decode(base64Data)
        } else {
            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        }
        
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

// Default UI components
@Composable
private fun DefaultImagePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading image...",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun DefaultImageError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Error loading image",
                fontSize = 10.sp,
                color = Color.Red,
                fontWeight = FontWeight.Medium
            )
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    fontSize = 8.sp,
                    color = Color.Red.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StageImagePlaceholder(stageNumber: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Step $stageNumber",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Loading...",
            fontSize = 10.sp,
            color = Color.Gray.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StageImageError(stageNumber: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Step $stageNumber",
            fontSize = 10.sp,
            color = Color.Red,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Image failed to load",
            fontSize = 8.sp,
            color = Color.Red.copy(alpha = 0.7f)
        )
    }
}