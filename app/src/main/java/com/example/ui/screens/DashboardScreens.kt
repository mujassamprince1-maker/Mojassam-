package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ColorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardContainer(viewModel: ChatViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Chats, 1: Stories, 2: Calls, 3: Settings, 4: Admin
    val activeTheme by viewModel.theme.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    // Edge to edge safe layout
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AbyssBlack,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                val tabs = listOf(
                    Triple("Chats", Icons.Default.Chat, 0),
                    Triple("Status", Icons.Default.CameraAlt, 1),
                    Triple("Calls", Icons.Default.Call, 2),
                    Triple("Settings", Icons.Default.Security, 3),
                    Triple("Mod Panel", Icons.Default.AdminPanelSettings, 4)
                )

                tabs.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.reportUserInteraction()
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else TextGray
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else TextGray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.reportUserInteraction()
                        viewModel.navigateTo(AppScreen.CONTACTS)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = CircleShape,
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact Thread")
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
            // Main Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AbyssBlack)
                    .border(width = 0.5.dp, color = SteelSlate, shape = RectangleShape)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRIVORA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 4.sp,
                        color = TextCrisp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Metadata Invisibility Layer",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                // Header interactive status symbols
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val statusColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = "SECURE_CONN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Screen Content injection based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ChatListTab(viewModel)
                    1 -> StoriesTab(viewModel)
                    2 -> CallsTab(viewModel)
                    3 -> SettingsTab(viewModel)
                    4 -> AdminTab(viewModel)
                }
            }
        }
    }
}

// --- TAB 1: SEARCHABLE CHATS LIST ---
@Composable
fun ChatListTab(viewModel: ChatViewModel) {
    val chatsList by viewModel.chats.collectAsState(initial = emptyList())
    val typingMap by viewModel.typingStatus.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val filteredChats = chatsList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Strip
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search encrypted tunnels...", color = TextGray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = TextGray) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextGray,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
            } else null,
            singleLine = true,
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

        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "No Chats",
                        tint = TextDim,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No private sessions active",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tap + to verify a new contact handshake",
                        color = TextDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredChats) { chat ->
                    val userTyping = typingMap[chat.id]
                    ChatListItem(chat, userTyping) {
                        viewModel.openChat(chat.id)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, typingStatus: String?, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = formatter.format(Date(chat.lastMessageTime))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aesthetic User Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(chat.avatarColor).copy(alpha = 0.2f))
                .border(1.5.dp, Color(chat.avatarColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (chat.isGroup) Icons.Default.Group else if (chat.isSecret) Icons.Default.Lock else Icons.Default.Person,
                contentDescription = "Avatar",
                tint = Color(chat.avatarColor)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Message Preview Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextCrisp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isSecret) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Secret Chat Indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 4.dp)
                    )
                }

                if (typingStatus != null) {
                    Text(
                        text = typingStatus,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 13.sp,
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Action Status Flags: Badge, Pin
        if (chat.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.unreadCount.toString(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}


// --- TAB 2: STORIES / STATUS INCompose ---
@Composable
fun StoriesTab(viewModel: ChatViewModel) {
    val storiesList by viewModel.stories.collectAsState(initial = emptyList())
    var showStoryCreator by remember { mutableStateOf(false) }
    var storyText by remember { mutableStateOf("") }
    var selectBgIdx by remember { mutableStateOf(0) }
    var viewingStory by remember { mutableStateOf<Story?>(null) }

    val bgList = listOf(0xFF0F1116.toInt(), 0xFF14002C.toInt(), 0xFF001E1D.toInt(), 0xFF2A000A.toInt())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Status Feed (SecShots)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextCrisp
        )
        Text(
            text = "Secure daily snap uploads. Auto-wiped after 24 hrs.",
            fontSize = 11.sp,
            color = TextGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Upload My Status Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showStoryCreator = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Post Snap", tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = "My Digital Presence", fontWeight = FontWeight.Bold, color = TextCrisp, fontSize = 14.sp)
                Text(text = "Broadcast encrypted snippet to mesh circle", color = TextGray, fontSize = 12.sp)
            }
        }

        Divider(color = SteelSlate, modifier = Modifier.padding(vertical = 12.dp))

        // Stories Lists
        if (storiesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No visual broadcasts compiled currently.", color = TextDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(storiesList) { story ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingStory = story }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(story.bgHex))
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                story.textContent.take(3),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = TextCrisp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(text = story.name, fontWeight = FontWeight.Bold, color = TextCrisp, fontSize = 14.sp)
                            val f = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
                            Text(text = f.format(Date(story.timestamp)), color = TextGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Story Creation dialog prompt
    if (showStoryCreator) {
        AlertDialog(
            onDismissRequest = { showStoryCreator = false },
            title = { Text(text = "Compose Broadcast Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        placeholder = { Text("What is happening in your network?") },
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = SteelSlate,
                            focusedTextColor = TextCrisp,
                            unfocusedTextColor = TextCrisp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Aesthetic Matrix Color:", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow {
                        items((bgList.indices).toList()) { idx ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(bgList[idx]))
                                    .border(
                                        width = if (selectBgIdx == idx) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { selectBgIdx = idx }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (storyText.isNotEmpty()) {
                            viewModel.addStory(storyText, bgList[selectBgIdx])
                            storyText = ""
                            showStoryCreator = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                ) {
                    Text("BROADCAST")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStoryCreator = false }) { Text("CANCEL") }
            }
        )
    }

    // Fullscreen Story Viewer
    val currStory = viewingStory
    if (currStory != null) {
        var progress by remember { mutableStateOf(0f) }

        LaunchedEffect(currStory) {
            progress = 0f
            // Progress tracker animation
            repeat(100) {
                delay(40) // total 4 seconds
                progress += 0.01f
            }
            viewingStory = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(currStory.bgHex))
                .clickable { viewingStory = null },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .safeDrawingPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Line Bars
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = TextDim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currStory.name, fontWeight = FontWeight.Bold, color = TextCrisp)
                        Icon(Icons.Default.Close, "Dismiss viewer", tint = TextCrisp, modifier = Modifier.clickable { viewingStory = null })
                    }
                }

                // Core Story Text Content
                Text(
                    text = currStory.textContent,
                    color = TextCrisp,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Bottom security footer stamp
                Text(
                    text = "End-to-End Cryptographic Packet verified.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}


// --- TAB 3: CALL LOGS ---
@Composable
fun CallsTab(viewModel: ChatViewModel) {
    val callList by viewModel.callRecords.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "SIP Secure Calls Logs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextCrisp)
                Text(text = "Bypassing mainstream telecommunications securely.", fontSize = 11.sp, color = TextGray)
            }

            if (callList.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear logs list",
                    tint = TextGray,
                    modifier = Modifier.clickable {
                        // Triggers cleanup
                        viewModel.logAdminAction("SysEngine: Cleared calling history ledger.")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (callList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No audio or video call transmissions.", color = TextDim, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(callList) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Hot callback
                                viewModel.makeCall(record.name, record.phone, record.avatarColor, record.isVideo)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(record.avatarColor).copy(alpha = 0.2f))
                                    .border(1.dp, Color(record.avatarColor), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (record.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Icon",
                                    tint = Color(record.avatarColor)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(text = record.name, fontWeight = FontWeight.Bold, color = TextCrisp, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (record.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                                        contentDescription = "Dir",
                                        tint = if (record.isIncoming) CyberGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val formattedTime = SimpleDateFormat("HH:mm - h a, d MMM", Locale.getDefault())
                                    Text(text = "${formattedTime.format(Date(record.timestamp))} (${record.duration}s)", color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }

                        Icon(
                            imageVector = if (record.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Redial Call Button",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


// --- TAB 4: ADVANCED SECURITY SETTINGS ---
@Composable
fun SettingsTab(viewModel: ChatViewModel) {
    val currentTheme by viewModel.theme.collectAsState()
    val isBioLocked by viewModel.isBiometricLocked.collectAsState()
    val isPassEnabled by viewModel.isPasscodeEnabled.collectAsState()
    val currentPasscode by viewModel.passcode.collectAsState()
    val screenShotEnabled by viewModel.screenshotProtection.collectAsState()
    val twoFaEnabled by viewModel.twoFactorEnabled.collectAsState()
    val currentTimeout by viewModel.inactivityLockSeconds.collectAsState()
    val activePhone by viewModel.userPhone.collectAsState()
    val activeUsername by viewModel.userName.collectAsState()
    val activeDownload by viewModel.activeDownload.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinValueInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Download Status Progress Block
        if (activeDownload != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "DOWNLOADING NODE CLIENT...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Source: secure.privora.io/node/$activeDownload",
                        fontSize = 11.sp,
                        color = TextCrisp
                    )
                    Text(
                        text = "Establishing cross-platform handoff link & verifying digital signature hashes with SHA-256 integrity check.",
                        fontSize = 9.sp,
                        color = TextGray
                    )
                }
            }
        }

        // Profile Summary
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VerifiedUser, "Profile Status Icon", tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = activeUsername, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextCrisp)
                    Text(text = activePhone, color = TextGray, fontSize = 12.sp)
                    Text(
                        text = "RSA-4096 Secure Active Protocol",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Sub-segment: Visual Styling Accents
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("NEON ACCENT THEMING", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple("CYAN", ColorTheme.CYAN_NEON, CyanPrimary),
                        Triple("EMERALD", ColorTheme.EMERALD_MATRIX, EmeraldPrimary),
                        Triple("CRIMSON", ColorTheme.CRIMSON_AURA, CrimsonPrimary),
                        Triple("AMETHYST", ColorTheme.AMETHYST_CYBER, AmethystPrimary)
                    ).forEach { (label, enumType, rawColor) ->
                        val isSel = currentTheme == enumType
                        Box(
                            modifier = Modifier
                                .clickable {
                                    viewModel.reportUserInteraction()
                                    viewModel.setTheme(enumType)
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) rawColor.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) rawColor else SteelSlate, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) rawColor else TextGray
                            )
                        }
                    }
                }
            }
        }

        // Sub-segment: Advanced Cryptographic Biometric Lock
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("PRIVACY & BIOMETRIC GATEWAYS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

                // 1. Biometric Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fingerprint & Face Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                        Text("Prompts authentication biometric scan on entrance", fontSize = 11.sp, color = TextGray)
                    }
                    Switch(
                        checked = isBioLocked,
                        onCheckedChange = { viewModel.toggleBiometric(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Divider(color = SteelSlate)

                // 2. Passcode Switch and settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Numeric PIN Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                        Text("Configure supplementary access login PIN ($currentPasscode)", fontSize = 11.sp, color = TextGray)
                    }
                    Switch(
                        checked = isPassEnabled,
                        onCheckedChange = {
                            if (it) {
                                showPinDialog = true
                            } else {
                                viewModel.togglePasscode(false)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Divider(color = SteelSlate)

                // 3. App Activity Lock Timer Limit
                Column {
                    Text("Auto Lock on Inactivity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                    Text("Time threshold before active view is covered", fontSize = 11.sp, color = TextGray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 15, 30, 60).forEach { sec ->
                            val sActive = currentTimeout == sec
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        viewModel.reportUserInteraction()
                                        viewModel.setInactivityTimeout(sec)
                                    }
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else SteelSlate.copy(alpha = 0.2f))
                                    .border(1.dp, if (sActive) MaterialTheme.colorScheme.primary else SteelSlate, RoundedCornerShape(6.dp))
                                    .width(62.dp)
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sActive) MaterialTheme.colorScheme.primary else TextGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-segment: Extreme Chat Privacy Options
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("HARSH SECURITY CONSTRICTION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

                // 1. Screenshot protection toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Screenshot Inhibition", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                        Text("Secures chat windows against system screenshot grabs", fontSize = 11.sp, color = TextGray)
                    }
                    Switch(
                        checked = screenShotEnabled,
                        onCheckedChange = { viewModel.toggleScreenshotProtection(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Divider(color = SteelSlate)

                // 2. 2FA Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Two-Factor Cryptology (2FA)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                        Text("Mandates temporary auxiliary keys upon device changes", fontSize = 11.sp, color = TextGray)
                    }
                    Switch(
                        checked = twoFaEnabled,
                        onCheckedChange = { viewModel.toggle2FA(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Divider(color = SteelSlate)

                // 3. Device verification details display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Network Cloud Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                        Text("Securely backups chat nodes securely to GCS", fontSize = 11.sp, color = TextGray)
                    }
                    Button(
                        onClick = { viewModel.logAdminAction("SysEngine: Synced database backups to decentral cloud node gracefully.") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Text("SYNC NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub-segment: Cross-Platform Device Deployment & Downloads
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "CROSS-PLATFORM DEPLOYMENT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Extend your secured chat tunnel network across multiple environments. Download corresponding standalone installations below.",
                    fontSize = 11.sp,
                    color = TextGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mobile Node Card Option
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AbyssBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, SteelSlate, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.reportUserInteraction()
                                viewModel.simulateNodeDownload("Mobile", "privora_mobile_v2.4.1.apk")
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Mobile Download Link Option",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MOBILE NODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCrisp
                        )
                        Text(
                            text = "Android APK / iOS app",
                            fontSize = 9.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OBTAIN APK",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Computer Node Card Option
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AbyssBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, SteelSlate, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.reportUserInteraction()
                                viewModel.simulateNodeDownload("Computer", "privora_desktop_v1.9.8.dmg")
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = "Computer Download Link Option",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "COMPUTER CLIENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextCrisp
                        )
                        Text(
                            text = "Mac DMG / Win EXE / Lin",
                            fontSize = 9.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OBTAIN DMG/EXE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Node Verification Key Setup Download option
                Divider(color = SteelSlate)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export pairing configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextCrisp)
                        Text("Generate an identity credentials key file to bind computer & secondary mobile node sessions instantly.", fontSize = 10.sp, color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.reportUserInteraction()
                            viewModel.simulateNodeDownload("Credentials Profile", "privora_pairing_package.json")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Text("EXPORT PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub-segment: Logout block
        item {
            Box(
                modifier = Modifier
                    .fillPaddingAndWidth(1f)
                    .clickable { viewModel.logOut() }
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "TERMINATE SECURE SESSION", color = CyberRed, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinValueInput = ""
            },
            title = { Text(text = "Define Locked PIN") },
            text = {
                OutlinedTextField(
                    value = pinValueInput,
                    onValueChange = { if (it.length <= 4) pinValueInput = it },
                    placeholder = { Text("e.g. 1337") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = SteelSlate,
                        focusedTextColor = TextCrisp,
                        unfocusedTextColor = TextCrisp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValueInput.length == 4) {
                            viewModel.togglePasscode(true, pinValueInput)
                            showPinDialog = false
                            pinValueInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinValueInput = ""
                }) { Text("CANCEL") }
            }
        )
    }
}

private fun Modifier.fillPaddingAndWidth(weight: Float): Modifier = this.fillMaxWidth()


// --- TAB 5: ADMIN AND FIREWALL MODERATION PANEL ---
@Composable
fun AdminTab(viewModel: ChatViewModel) {
    val logs by viewModel.adminLogs.collectAsState()
    val spamFilterEnabled by viewModel.spamProtectionEnabled.collectAsState()
    val contactsList by viewModel.contacts.collectAsState(initial = emptyList())
    val spamBots = contactsList.filter { it.isSpam }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Firewall Statistics block
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("INTEGRATED MODERATOR FIREWALL", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Encrypted Streams", fontSize = 12.sp, color = TextGray)
                        Text("4 Nodes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextCrisp)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(40.dp).background(SteelSlate))
                    Column {
                        Text("Interfere Fraud Blocks", fontSize = 12.sp, color = TextGray)
                        Text("1 Blocked", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberRed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Integrity Shielding", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrisp)
                        Text("Automatic warning/removal of suspected spambots", fontSize = 11.sp, color = TextGray)
                    }
                    Switch(
                        checked = spamFilterEnabled,
                        onCheckedChange = { viewModel.toggleSpamProtection(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Section: Active Threat Spambots list
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("CAPTURED SPAM FRAUD BOT METADATA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))

                if (spamBots.isEmpty()) {
                    Text("Secure. Zero spam network vectors open.", color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                } else {
                    spamBots.forEach { bot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(bot.avatarColor).copy(alpha = 0.2f))
                                        .border(1.dp, Color(bot.avatarColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Dangerous, "Danger Spambot", tint = Color(bot.avatarColor), modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(bot.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextCrisp)
                                    Text(bot.phone, fontSize = 11.sp, color = TextGray)
                                }
                            }

                            Button(
                                onClick = { viewModel.banishContactAndFakeUser(bot.phone) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.2f), contentColor = CyberRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.border(1.dp, CyberRed, RoundedCornerShape(8.dp))
                            ) {
                                Text("SHRED NODE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: System Security Handshake Ledger
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassLayer, RoundedCornerShape(12.dp))
                    .border(1.dp, SteelSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("SECURITY SYSTEM EVENT LEDGER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text("No audit files compiled currently. Initiate user interaction.", color = TextDim, fontSize = 11.sp)
                    } else {
                        logs.forEach { logItem ->
                            Text(
                                text = logItem,
                                color = if (logItem.contains("Warning") || logItem.contains("Spam")) CyberRed else if (logItem.contains("Biometric") || logItem.contains("Crypt")) CyberGreen else TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
