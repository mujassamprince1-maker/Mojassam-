package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(viewModel: ChatViewModel) {
    val scale = remember { Animatable(0f) }
    var showSubtitle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        showSubtitle = true
        delay(2500)
        if (viewModel.isAuthenticated.value) {
            viewModel.navigateTo(AppScreen.DASHBOARD)
        } else {
            viewModel.navigateTo(AppScreen.ONBOARDING)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background nodes
        Box(
            modifier = Modifier
                .size(300.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1100F0FF), Color.Transparent),
                            radius = size.width / 2
                        )
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Lock Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PRIVORA",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 8.sp,
                color = TextCrisp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn() + expandVertically()
            ) {
                Text(
                    text = "QUANTUM KEY CHAT ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: ChatViewModel) {
    var step by remember { mutableStateOf(1) }
    val maxSteps = 3

    val title = when (step) {
        1 -> "Zero-Metadata Chat"
        2 -> "Biometric Vault"
        else -> "Smart AI Engine"
    }

    val desc = when (step) {
        1 -> "Every message stream is encrypted symmetrically using Ephemeral Key handshakes. Rest assured, your metadata remains completely unreachable."
        2 -> "Lock and shield active message feeds within seconds. Seamless face & fingerprint unlock protection guards against physical snoopers."
        else -> "Translate chat streams natively in one touch, auto-generate replies with Gemini AI, and execute timed self-destruct routines."
    }

    val icon = when (step) {
        1 -> Icons.Default.VpnKey
        2 -> Icons.Default.Fingerprint
        else -> Icons.Default.Psychology
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "SKIP",
                    color = TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(AppScreen.LOGIN) }
                        .padding(12.dp)
                )
            }

            // Visual Center Illustration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(4.dp, CircleShape)
                        .background(GlassLayer, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Illustration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextCrisp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = desc,
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Bottom control navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Dot indicators
                Row {
                    repeat(maxSteps) { i ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (step == i + 1) 24.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (step == i + 1) MaterialTheme.colorScheme.primary
                                    else TextDim
                                )
                        )
                    }
                }

                // Next Buttons
                Button(
                    onClick = {
                        if (step < maxSteps) {
                            step++
                        } else {
                            viewModel.navigateTo(AppScreen.LOGIN)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (step == maxSteps) "AUTHORIZE" else "NEXT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: ChatViewModel) {
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = "Open Lock Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Secure Network Entry",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextCrisp
            )

            Text(
                text = "Initiate secure terminal connection",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .background(GlassLayer, RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phonelink,
                    contentDescription = "Multiplatform Compatibility",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Computer & Mobile nodes active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Custom Text Fields
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Display Name") },
                placeholder = { Text("e.g. Neo") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = SteelSlate,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = TextGray,
                    focusedTextColor = TextCrisp,
                    unfocusedTextColor = TextCrisp
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Mobile Matrix Number") },
                placeholder = { Text("+1 (555) 555-5555") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = SteelSlate,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = TextGray,
                    focusedTextColor = TextCrisp,
                    unfocusedTextColor = TextCrisp
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (phone.isNotEmpty() && username.isNotEmpty()) {
                        focusManager.clearFocus()
                        viewModel.login(phone, username)
                    }
                },
                enabled = phone.isNotEmpty() && username.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = SteelSlate,
                    contentColor = Color.Black,
                    disabledContentColor = TextDim
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "REQUEST SECURE OTP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun OtpVerificationScreen(viewModel: ChatViewModel) {
    var code by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Message lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Enter Handshake OTP",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextCrisp
            )

            Text(
                text = "Enter verification OTP code sent to ${viewModel.userPhone.collectAsState().value}",
                fontSize = 13.sp,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp)
            )

            // OTP Box Representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val maxChars = 4
                repeat(maxChars) { i ->
                    val char = if (code.length > i) code[i].toString() else ""
                    val isCurrent = code.length == i

                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(56.dp)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (showError) CyberRed else if (isCurrent) MaterialTheme.colorScheme.primary else SteelSlate,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(GlassLayer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showError) CyberRed else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showError) {
                Text(
                    text = "Security payload mismatch. Access denied.",
                    color = CyberRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulated Numeric Grid Pad
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BACK", "0", "OK")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SteelSlate.copy(alpha = 0.5f))
                                    .border(1.dp, SteelSlate, CircleShape)
                                    .clickable {
                                        showError = false
                                        when (digit) {
                                            "BACK" -> if (code.isNotEmpty()) code = code.dropLast(1)
                                            "OK" -> {
                                                val verified = viewModel.verifyOtp(code)
                                                if (!verified) {
                                                    code = ""
                                                    showError = true
                                                }
                                            }
                                            else -> if (code.length < 4) code += digit
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = if (digit.length > 1) 12.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (digit == "OK") MaterialTheme.colorScheme.primary else TextCrisp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Auto-simulate valid bypass code: '7777'",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.clickable { code = "7777" }
            )
        }
    }
}

@Composable
fun BiometricLockOverlay(viewModel: ChatViewModel) {
    var enteredPin by remember { mutableStateOf("") }
    var loginErr by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { viewModel.reportUserInteraction() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield Locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PRIVORA SECURE LOCK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = TextCrisp
            )

            Text(
                text = "Awaiting secure handshake authorization",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pulsing fingerprint button
            var pulse by remember { mutableStateOf(true) }
            val scaleAnim by animateFloatAsState(
                targetValue = if (pulse) 1.1f else 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Pulse"
            )

            LaunchedEffect(Unit) { pulse = false }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .background(GlassLayer, CircleShape)
                    .clickable {
                        // Simulating successful biometric bypass click
                        viewModel.unlockApp()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint Sensor",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tap fingerprint sensor for biometric bypass",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                val expectedPin = viewModel.passcode.collectAsState().value
                repeat(expectedPin.length) { idx ->
                    val dotActive = enteredPin.length > idx
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (loginErr) CyberRed
                                else if (dotActive) MaterialTheme.colorScheme.primary
                                else SteelSlate
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Pin Pad fallback
            Column(
                modifier = Modifier.padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pinKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                pinKeys.forEach { prow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        prow.forEach { ky ->
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(SteelSlate.copy(alpha = 0.3f))
                                    .clickable {
                                        viewModel.reportUserInteraction()
                                        loginErr = false
                                        when (ky) {
                                            "C" -> enteredPin = ""
                                            "OK" -> {
                                                val saved = viewModel.passcode.value
                                                if (enteredPin == saved) {
                                                    viewModel.unlockApp()
                                                } else {
                                                    enteredPin = ""
                                                    loginErr = true
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += ky
                                                    if (enteredPin.length == 4) {
                                                        // Auto verify PIN
                                                        val saved = viewModel.passcode.value
                                                        if (enteredPin == saved) {
                                                            viewModel.unlockApp()
                                                        } else {
                                                            enteredPin = ""
                                                            loginErr = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ky,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ky == "OK") MaterialTheme.colorScheme.primary else TextCrisp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
