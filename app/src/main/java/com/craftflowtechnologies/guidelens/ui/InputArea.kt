package com.craftflowtechnologies.guidelens.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.craftflowtechnologies.guidelens.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class SelectedImage(
    val base64: String,
    val bitmap: Bitmap? = null
)

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun EnhancedInputArea(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isConnected: Boolean,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    isThinking: Boolean,
    onImageSelected: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Enhanced state management with animations
    var selectedImages by remember { mutableStateOf<List<SelectedImage>>(emptyList()) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    var isHovering by remember { mutableStateOf(false) }

    // Smooth animation states
    val containerAnimation = animateFloatAsState(
        targetValue = if (isInputFocused || selectedImages.isNotEmpty()) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "container"
    )

    // Image picker launchers
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        scope.launch {
            val images = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readBytes()
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            SelectedImage(
                                base64 = bytes.encodeToBase64(),
                                bitmap = bitmap
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            selectedImages = images
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val byteArray = ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
                output.toByteArray()
            }
            selectedImages = selectedImages + SelectedImage(
                base64 = byteArray.encodeToBase64(),
                bitmap = bitmap
            )
        }
    }

    // Main container with glassmorphism
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // Glassmorphism background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color(0xFF1A1A2E).copy(alpha = 0.6f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.7f),
                                Color(0xFFF8FAFC).copy(alpha = 0.9f)
                            )
                        }
                    )
                )
                .blur(radius = 20.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            // Enhanced image preview section
            AnimatedVisibility(
                visible = selectedImages.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                EnhancedImagePreviewSection(
                    images = selectedImages,
                    onRemoveImage = { index ->
                        selectedImages = selectedImages.filterIndexed { i, _ -> i != index }
                    },
                    isDarkTheme = isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(if (selectedImages.isNotEmpty()) 2.dp else 0.dp))

            // Main input container with premium glassmorphism
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(containerAnimation.value),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Premium image upload button
                        PremiumImageUploadButton(
                            isThinking = isThinking,
                            selectedAgent = selectedAgent,
                            isDarkTheme = isDarkTheme,
                            hasImages = selectedImages.isNotEmpty(),
                            onClick = { showImagePickerDialog = true }
                        )

                        // Enhanced text input
                        PremiumTextField(
                            value = userInput,
                            onValueChange = onInputChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = if (selectedImages.isNotEmpty()) "Add a caption..." else "Message ${selectedAgent.name}...",
                            selectedAgent = selectedAgent,
                            isDarkTheme = isDarkTheme,
                            isThinking = isThinking,
                            onFocusChanged = { isInputFocused = it },
                            onSend = {
                                if (userInput.isNotBlank() || selectedImages.isNotEmpty()) {
                                    onImageSelected(selectedImages.map { it.base64 })
                                    onSendMessage()
                                    selectedImages = emptyList()
                                }
                            }
                        )

                        // Premium send button
                        PremiumSendButton(
                            isEnabled = (userInput.isNotBlank() || selectedImages.isNotEmpty()) && !isThinking,
                            isThinking = isThinking,
                            selectedAgent = selectedAgent,
                            isDarkTheme = isDarkTheme,
                            onSend = {
                                if (userInput.isNotBlank() || selectedImages.isNotEmpty()) {
                                    onImageSelected(selectedImages.map { it.base64 })
                                    onSendMessage()
                                    selectedImages = emptyList()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Premium image picker dialog
        if (showImagePickerDialog) {
            PremiumImagePickerDialog(
                onCameraClick = {
                    cameraLauncher.launch()
                    showImagePickerDialog = false
                },
                onGalleryClick = {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showImagePickerDialog = false
                },
                onDismiss = { showImagePickerDialog = false },
                isDarkTheme = isDarkTheme
            )
        }
    }

    // Auto-focus when images are selected
    LaunchedEffect(selectedImages) {
        if (selectedImages.isNotEmpty()) {
            delay(300)
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    isThinking: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onSend: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderAnimation = animateColorAsState(
        targetValue = if (isFocused) {
            selectedAgent.primaryColor.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "border"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = 44.dp, max = 120.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.5f)
                    } else {
                        Color.Black.copy(alpha = 0.4f)
                    }
                )
            )
        },
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
            color = if (isDarkTheme) Color.White else Color.Black
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = borderAnimation.value,
            cursorColor = selectedAgent.primaryColor,
            focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
            unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(22.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = { onSend() }
        ),
        maxLines = 4,
        enabled = !isThinking
    )

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }
}

@Composable
private fun PremiumSendButton(
    isEnabled: Boolean,
    isThinking: Boolean,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onSend: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isEnabled && !isThinking) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val rotationAnimation by animateFloatAsState(
        targetValue = if (isThinking) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        onClick = onSend,
        enabled = isEnabled,
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .scale(scale),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isEnabled) {
                            listOf(
                                selectedAgent.primaryColor,
                                selectedAgent.primaryColor.copy(alpha = 0.8f)
                            )
                        } else {
                            listOf(
                                Color.Gray.copy(alpha = 0.3f),
                                Color.Gray.copy(alpha = 0.2f)
                            )
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isThinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Send message",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumImageUploadButton(
    isThinking: Boolean,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    hasImages: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (hasImages) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        enabled = !isThinking,
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .scale(scale),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = if (hasImages) {
                            listOf(
                                selectedAgent.primaryColor.copy(alpha = 0.8f),
                                selectedAgent.primaryColor.copy(alpha = 0.4f)
                            )
                        } else {
                            listOf(
                                if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasImages) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = "Upload Image",
                tint = if (hasImages) Color.White else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EnhancedImagePreviewSection(
    images: List<SelectedImage>,
    onRemoveImage: (Int) -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.7f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "${images.size} image${if (images.size > 1) "s" else ""} selected",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            Color.Black.copy(alpha = 0.6f)
                        }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(images.size) { index ->
                        val image = images[index]
                        EnhancedImagePreviewItem(
                            image = image,
                            onRemove = { onRemoveImage(index) },
                            isDarkTheme = isDarkTheme,
                            index = index
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedImagePreviewItem(
    image: SelectedImage,
    onRemove: () -> Unit,
    isDarkTheme: Boolean,
    index: Int
) {
    val enterAnimation = remember {
        slideInHorizontally(
            initialOffsetX = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn()
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .animateContentSize()
    ) {
        // Image with glassmorphism overlay
        image.bitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Glassmorphism overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        // Premium remove button
        Surface(
            onClick = onRemove,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove image",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumImagePickerDialog(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Add Images",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumOptionButton(
                    icon = Icons.Rounded.CameraAlt,
                    text = "Take Photo",
                    onClick = onCameraClick,
                    isDarkTheme = isDarkTheme
                )

                PremiumOptionButton(
                    icon = Icons.Rounded.PhotoLibrary,
                    text = "Choose from Gallery",
                    onClick = onGalleryClick,
                    isDarkTheme = isDarkTheme
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
                    )
                )
            }
        },
        containerColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White
    )
}

@Composable
private fun PremiumOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        } else {
                            listOf(
                                Color.Black.copy(alpha = 0.04f),
                                Color.Black.copy(alpha = 0.02f)
                            )
                        }
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                )
            }
        }
    }
}

// Utility function to convert byte array to base64
@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.encodeToBase64(): String {
    return Base64.Default.encode(this)
}