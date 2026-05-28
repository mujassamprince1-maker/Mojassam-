package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.ActiveCall
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- SCREEN 1: PRIVATE CHAT ROOM ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrivateChatScreen(viewModel: ChatViewModel) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val activeChat = viewModel.chats.collectAsState(initial = emptyList()).value.find { it.id == activeChatId }
    val messagesList by viewModel.activeMessages.collectAsState(initial = emptyList())
    val typingStatusMap by viewModel.typingStatus.collectAsState()
    val smartSuggestions by viewModel.smartReplies.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showActionTray by remember { mutableStateOf(false) }
    var selectedMsgForOption by remember { mutableStateOf<Message?>(null) }
    var voiceRecordingState by remember { mutableStateOf(false) }
    var showSelfDestructConfig by remember { mutableStateOf(false) }
    var showScheduleConfig by remember { mutableStateOf(false) }
    var scheduleSecondsInput by remember { mutableStateOf("10") }
    var selfDestructSecondsInput by remember { mutableStateOf("5") }

    if (activeChat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val userTyping = typingStatusMap[activeChat.id]

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AbyssBlack)
                    .statusBarsPadding()
                    .border(0.5.dp, SteelSlate, RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Leave Thread",
                        tint = TextCrisp,
                        modifier = Modifier
                            .clickable { viewModel.closeChat() }
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Mini Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(activeChat.avatarColor).copy(alpha = 0.2f))
                            .border(1.dp, Color(activeChat.avatarColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeChat.isSecret) Icons.Default.Lock else Icons.Default.Person,
                            contentDescription = "Active chat profile avatar",
                            tint = Color(activeChat.avatarColor),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = activeChat.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextCrisp
                        )
                        Text(
                            text = userTyping ?: if (activeChatId == 4L) "AI Assistant Active" else "Online status hidden",
                            fontSize = 11.sp,
                            color = if (userTyping != null) MaterialTheme.colorScheme.primary else TextGray
                        )
                    }
                }

                // Header Calling actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Trigger Audio Connection",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                viewModel.makeCall(activeChat.name, "+123456", activeChat.avatarColor, isVideo = false)
                            }
                            .padding(8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Trigger Video Link",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                viewModel.makeCall(activeChat.name, "+123456", activeChat.avatarColor, isVideo = true)
                            }
                            .padding(8.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDark)
                .padding(innerPadding)
        ) {
            // Secret mode warning box
            if (activeChat.isSecret) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, "Shielding active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SCREENSHOT PROTECTION STRICTLY ENGAGED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Message Scroll Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messagesList) { msg ->
                    ChatBubble(
                        message = msg,
                        isMine = msg.senderPhone.isEmpty(),
                        onLongClick = {
                            viewModel.reportUserInteraction()
                            selectedMsgForOption = msg
                        }
                    )
                }
            }

            // Horizontally Scrollable AI Smart Replies recommendation row
            if (smartSuggestions.isNotEmpty() && !voiceRecordingState) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AbyssBlack)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(smartSuggestions) { suggestionText ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    viewModel.reportUserInteraction()
                                    textInput = suggestionText
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = suggestionText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bottom interactive Chat Input panel
            Column(modifier = Modifier.background(AbyssBlack)) {
                // Recording animation preview
                if (voiceRecordingState) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CyberRed)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("RECORDING SECURE AUDIO FLOW...", color = TextCrisp, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Text(
                            text = "CANCEL",
                            color = CyberRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { voiceRecordingState = false }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment trigger '+'
                    IconButton(onClick = {
                        viewModel.reportUserInteraction()
                        showActionTray = !showActionTray
                    }) {
                        Icon(
                            imageVector = if (showActionTray) Icons.Default.Close else Icons.Default.AddCircleOutline,
                            contentDescription = "Attachment Action Menu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Input Text Area
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            viewModel.reportUserInteraction()
                            textInput = it
                        },
                        placeholder = { Text("Encrypted message...", color = TextGray, fontSize = 13.sp) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = SteelSlate,
                            focusedTextColor = TextCrisp,
                            unfocusedTextColor = TextCrisp
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    // Send or Mic button
                    if (textInput.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Symmetric Send Action",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        // Mic Button
                        IconButton(onClick = {
                            if (voiceRecordingState) {
                                // Close and simulate message insertion
                                viewModel.sendMessage("Voice Note transmitted 🎤 (0:04)", type = "VOICE")
                                voiceRecordingState = false
                            } else {
                                voiceRecordingState = true
                            }
                        }) {
                            Icon(
                                imageVector = if (voiceRecordingState) Icons.Default.StopCircle else Icons.Default.Mic,
                                contentDescription = "Voice Memo Record Trigger",
                                tint = if (voiceRecordingState) CyberRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Attachment Action Options overlay Tray
    if (showActionTray) {
        AlertDialog(
            onDismissRequest = { showActionTray = false },
            title = { Text(text = "Secure Cryptographic Cargo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple("Share Secure Photo Node", Icons.Default.CameraAlt) {
                            viewModel.sendMessage("Image packet shared 📷", type = "IMAGE", mediaUri = "simulated_photo")
                            showActionTray = false
                        },
                        Triple("Share Sealed Archive File", Icons.Default.AttachFile) {
                            viewModel.sendMessage("Sealed document shared 📁", type = "FILE", mediaUri = "simulated_doc")
                            showActionTray = false
                        },
                        Triple("Delayed Message Delivery", Icons.Default.Schedule) {
                            showScheduleConfig = true
                            showActionTray = false
                        },
                        Triple("Configure Message Self-Destruct", Icons.Default.AvTimer) {
                            showSelfDestructConfig = true
                            showActionTray = false
                        }
                    ).forEach { (label, ic, act) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { act() }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = ic, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = label, fontWeight = FontWeight.SemiBold, color = TextCrisp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionTray = false }) { Text("CANCEL", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    // Message context options sheet (Reaction, pin, translation, delete)
    if (selectedMsgForOption != null) {
        val activeMsg = selectedMsgForOption!!
        AlertDialog(
            onDismissRequest = { selectedMsgForOption = null },
            title = { Text(text = "Packet Options Ledger") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Reactions Strip Selection
                    Text("Attach Reaction Handshake:", fontSize = 11.sp, color = TextGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("👍", "❤️", "😂", "🔥", "😮").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.reactToMessage(activeMsg.id, emoji)
                                        selectedMsgForOption = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Translate button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.translateChatMessage(activeMsg.id, "Spanish")
                                selectedMsgForOption = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Translate, "Translate", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Translate to Spanish (Gemini)", fontWeight = FontWeight.SemiBold, color = TextCrisp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.pinMessage(activeMsg.id)
                                selectedMsgForOption = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PushPin, "Pin", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (activeMsg.isPinned) "Unpin packet" else "Pin packet securely", fontWeight = FontWeight.SemiBold, color = TextCrisp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.handleSelfDestruct(activeMsg.id, 5)
                                selectedMsgForOption = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, "Self Destruct", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Shred in 5 seconds", fontWeight = FontWeight.SemiBold, color = TextCrisp)
                    }

                    // Delete button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.deleteMessage(activeMsg.id)
                                selectedMsgForOption = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteForever, "Destroy", tint = CyberRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Shred package forever", fontWeight = FontWeight.Bold, color = CyberRed)
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Schedule configure popup
    if (showScheduleConfig) {
        val sController = LocalSoftwareKeyboardController.current
        AlertDialog(
            onDismissRequest = { showScheduleConfig = false },
            title = { Text(text = "Configure Delivery Delay") },
            text = {
                Column {
                    Text("Enter delay before message is automatically injected into the stream (in seconds):", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = scheduleSecondsInput,
                        onValueChange = { scheduleSecondsInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextCrisp, unfocusedTextColor = TextCrisp, focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val secs = scheduleSecondsInput.toIntOrNull() ?: 10
                        viewModel.sendMessage("SCHEDULED TASK: Transmitting delayed data packet.", scheduledDelaySeconds = secs)
                        sController?.hide()
                        showScheduleConfig = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                ) {
                    Text("ENGAGE SCHEDULER")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleConfig = false }) { Text("CANCEL") }
            }
        )
    }

    // Self destruct configure popup
    if (showSelfDestructConfig) {
        AlertDialog(
            onDismissRequest = { showSelfDestructConfig = false },
            title = { Text(text = "Self-Destruct Threshold") },
            text = {
                Column {
                    Text("Enter destruction countdown limit (seconds) for target outgoing packets:", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = selfDestructSecondsInput,
                        onValueChange = { selfDestructSecondsInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextCrisp, unfocusedTextColor = TextCrisp, focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val secs = selfDestructSecondsInput.toIntOrNull() ?: 5
                        viewModel.sendMessage("SELF-DESTRUCT ALERT: This message package triggers shredding automatically on delivery.", scheduledDelaySeconds = 0)
                        // Note: Self destruct will be configured on long tap of sent item
                        showSelfDestructConfig = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                ) {
                    Text("ENGAGE DESTRUCTION")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelfDestructConfig = false }) { Text("CANCEL") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: Message, isMine: Boolean, onLongClick: () -> Unit) {
    val alignEnd = isMine
    val bubbleBg = if (alignEnd) MaterialTheme.colorScheme.secondaryContainer else SteelSlate.copy(alpha = 0.5f)
    val textAlignment = if (alignEnd) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = textAlignment
    ) {
        // Pinned label tracker
        if (message.isPinned) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(Icons.Default.PushPin, "Pinned Message Indicator", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SECURELY PINNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextGray)
            }
        }

        // Active bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (alignEnd) 16.dp else 4.dp,
                        bottomEnd = if (alignEnd) 4.dp else 16.dp
                    )
                )
                .background(bubbleBg)
                .combinedClickable(
                    onLongClick = { onLongClick() },
                    onClick = { /* Interactive action hooks */ }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (!alignEnd && message.senderPhone.isNotEmpty()) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Render media placeholders if specified
                if (message.type == "IMAGE") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianDark)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, "Inline Image file", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (message.type == "VOICE") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, "Voice recording wave play", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        // Draw custom wave
                        Box(modifier = Modifier.height(16.dp).weight(1f).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Body text
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = TextCrisp
                )

                // Translation Display If translated in session
                if (message.translatedText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = TextDim, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.translatedText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom strip details: Time, single/double blue receipts
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                    Text(
                        text = formatted,
                        fontSize = 9.sp,
                        color = TextGray
                    )

                    if (alignEnd) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = when (message.status) {
                                "SENT" -> Icons.Default.Check
                                "DELIVERED" -> Icons.Default.DoneAll
                                else -> Icons.Default.DoneAll // Blue tick represented by color below
                            },
                            contentDescription = "Read receipts ticks",
                            tint = if (message.status == "READ" || message.status == "DELIVERED" && message.id % 2 == 0L) MaterialTheme.colorScheme.primary else TextGray,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        // Reactions floating badge under bubble
        if (message.reaction != null) {
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SteelSlate)
                    .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = message.reaction, fontSize = 11.sp)
            }
        }

        // Self-destruct warning label
        if (message.isSelfDestruct) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(Icons.Default.AvTimer, "Ticking Timer", tint = CyberRed, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SELF-SHRED SEQUENCE ACTIVE", fontSize = 9.sp, color = CyberRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}


// --- SCREEN 2: GROUP CHAT ROOM ---
@Composable
fun GroupChatScreen(viewModel: ChatViewModel) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val activeChat = viewModel.chats.collectAsState(initial = emptyList()).value.find { it.id == activeChatId }
    val messagesList by viewModel.activeMessages.collectAsState(initial = emptyList())
    var textInput by remember { mutableStateOf("") }

    if (activeChat == null) return

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AbyssBlack)
                    .statusBarsPadding()
                    .border(0.5.dp, SteelSlate, RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go Back",
                    tint = TextCrisp,
                    modifier = Modifier
                        .clickable { viewModel.closeChat() }
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(activeChat.avatarColor).copy(alpha = 0.2f))
                        .border(1.dp, Color(activeChat.avatarColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Group, "Group Icon Avatar", tint = Color(activeChat.avatarColor), modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = activeChat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                    Text(text = "7 members • Cryptographic Channel", fontSize = 11.sp, color = TextGray)
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDark)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messagesList) { msg ->
                    ChatBubble(message = msg, isMine = msg.senderPhone.isEmpty(), onLongClick = {})
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AbyssBlack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Group message...", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = SteelSlate,
                        focusedTextColor = TextCrisp,
                        unfocusedTextColor = TextCrisp
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = {
                    if (textInput.isNotEmpty()) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                    }
                }) {
                    Icon(Icons.Default.Send, "Send Group Msg", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


// --- SCREEN 3: CONTACTS & QR HANDSHAKES ---
@Composable
fun ContactsScreen(viewModel: ChatViewModel) {
    val contactsList by viewModel.contacts.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showQrProfile by remember { mutableStateOf(false) }

    val filteredContacts = contactsList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AbyssBlack)
                    .statusBarsPadding()
                    .border(0.5.dp, SteelSlate, RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = TextCrisp,
                        modifier = Modifier
                            .clickable { viewModel.navigateBack() }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Handshake Contacts Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextCrisp)
                }

                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Share handshakes QR scanner",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showQrProfile = true }
                        .padding(8.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDark)
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contact nodes...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = SteelSlate,
                    focusedTextColor = TextCrisp,
                    unfocusedTextColor = TextCrisp
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Direct lookup chat or simulated start
                                viewModel.openChat(1L) // Always direct fallback for test prototype
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(contact.avatarColor).copy(alpha = 0.2f))
                                .border(1.dp, Color(contact.avatarColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(contact.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Color(contact.avatarColor))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(text = contact.name, fontWeight = FontWeight.Bold, color = TextCrisp, fontSize = 14.sp)
                            Text(text = "${contact.username} • ${contact.statusText}", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showQrProfile) {
        AlertDialog(
            onDismissRequest = { showQrProfile = false },
            title = { Text(text = "Secure Mesh Identity QR") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Peer contact network scan code:", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Draw custom simulated QR pattern using Canvas
                    Canvas(modifier = Modifier.size(160.dp)) {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        drawRoundRect(
                            color = CyanPrimary,
                            style = Stroke(width = 3.dp.toPx(), pathEffect = pathEffect)
                        )

                        // Draw QR mock square blocks
                        drawRect(color = Color.White, size = size / 3f)
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width * (2f/3f), 0f), size = size / 3f)
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * (2f/3f)), size = size / 3f)
                        drawCircle(color = CyanPrimary, radius = size.width / 8f, center = center)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = viewModel.userName.collectAsState().value,
                        fontWeight = FontWeight.Bold,
                        color = TextCrisp
                    )
                    Text(
                        text = viewModel.userPhone.collectAsState().value,
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showQrProfile = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)) {
                    Text("CLOSE")
                }
            }
        )
    }
}


// --- SCREEN 4: VOICE TELEPHONY CONNECT SCREEN ---
@Composable
fun AudioCallScreen(viewModel: ChatViewModel) {
    val callDetail by viewModel.activeCall.collectAsState()
    if (callDetail == null) return

    val detail = callDetail!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header warning indicators
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SIP ENCRYPTED LINK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SRTP Handshake active",
                    fontSize = 10.sp,
                    color = TextGray
                )
            }

            // Core Profile and animated waves
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Expanding wave rings
                var scaleEffect by remember { mutableStateOf(true) }
                val animScale by animateFloatAsState(
                    targetValue = if (scaleEffect) 1.4f else 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "Wave"
                )
                LaunchedEffect(Unit) { scaleEffect = false }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(scaleX = animScale, scaleY = animScale)
                        .border(1.dp, Color(detail.avatarColor).copy(alpha = 0.5f), CircleShape)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color(detail.avatarColor).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(detail.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = detail.name.take(2).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = detail.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextCrisp
                )

                Text(
                    text = if (detail.status == "RINGING") "ENCRYPTING TUNNEL..." else "SECURE: " + formatDuration(detail.durationSeconds),
                    fontSize = 13.sp,
                    color = if (detail.status == "RINGING") MaterialTheme.colorScheme.primary else CyberGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Call Actions Control row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SteelSlate.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.MicNone, "Mute Mic Input", tint = TextCrisp)
                }

                // HANG UP Red Button
                IconButton(
                    onClick = { viewModel.endCall() },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(CyberRed)
                ) {
                    Icon(Icons.Default.CallEnd, "Terminate Call link", tint = TextCrisp, modifier = Modifier.size(32.dp))
                }

                // Volume handsfree
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SteelSlate.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.VolumeUp, "Speaker toggle action", tint = TextCrisp)
                }
            }
        }
    }
}


// --- SCREEN 5: VIDEO ENCRYPTED CELL SCREEN ---
@Composable
fun VideoCallScreen(viewModel: ChatViewModel) {
    val callDetail by viewModel.activeCall.collectAsState()
    if (callDetail == null) return

    val detail = callDetail!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Large Simulated Feed view utilizing Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw grid line matrix overlay representing a digital feed interface
            val interval = 40.dp.toPx()
            for (x in 0..size.width.toInt() step interval.toInt()) {
                drawLine(color = Color(0x0C00F0FF), start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height))
            }
            for (y in 0..size.height.toInt() step interval.toInt()) {
                drawLine(color = Color(0x0C00F0FF), start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()))
            }
        }

        // Overlay central display content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FlipCameraAndroid, "Flip Camera Feed", tint = TextCrisp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SECURE TELECONFERENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (detail.status == "RINGING") "TUNNELLING..." else "SECURE: " + formatDuration(detail.durationSeconds),
                        color = if (detail.status == "RINGING") CyberGold else CyberGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(CyberGreen)
                )
            }

            // Visual central caller frame if not ringing, otherwise avatar placeholder
            if (detail.status == "RINGING") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(detail.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            detail.name.take(2).uppercase(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Ringing crypt-connection...", color = TextGray, fontSize = 14.sp)
                }
            } else {
                // Large frame layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassLayer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Videocam, "Target Videofeed active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "${detail.name}'s feed", fontWeight = FontWeight.Bold, color = TextCrisp, fontSize = 14.sp)
                        Text(text = "1085p 60fps • GCM-GCM Encryption key verified", color = TextGray, fontSize = 11.sp)
                    }
                }
            }

            // Floating mini local video preview block on right side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp, 120.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(AbyssBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Face, "Own Video feedback window preview", tint = MaterialTheme.colorScheme.primary)
                        Text("You", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action Hangup Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(SteelSlate.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.VideocamOff, "Drop Camera Video input", tint = TextCrisp)
                }

                // Termination
                IconButton(
                    onClick = { viewModel.endCall() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CyberRed)
                ) {
                    Icon(Icons.Default.CallEnd, "Hangup", tint = TextCrisp, modifier = Modifier.size(30.dp))
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(SteelSlate.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.VolumeMute, "Speaker mute", tint = TextCrisp)
                }
            }
        }
    }
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
