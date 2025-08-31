package com.craftflowtechnologies.guidelens.clerk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SignInOrUpView(
    isDarkTheme: Boolean = false
) {
    var isSignUp by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        if (isSignUp) {
            SignUpView(isDarkTheme = isDarkTheme)
        } else {
            SignInView(isDarkTheme = isDarkTheme)
        }

        OutlinedButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
            )
        ) {
            Text(
                text = if (isSignUp) {
                    "Already have an account? Sign in"
                } else {
                    "Don't have an account? Sign up"
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}