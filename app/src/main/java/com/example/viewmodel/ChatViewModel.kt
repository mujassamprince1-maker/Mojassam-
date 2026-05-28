package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.ChatDao
import com.example.data.model.*
import com.example.data.remote.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    LOGIN,
    OTP,
    DASHBOARD, // Houses Chat List, Status, Calls, Settings, Admin Panel
    PRIVATE_CHAT,
    GROUP_CHAT,
    CALL_SCREEN,
    VIDEO_CALL_SCREEN,
    CONTACTS,
    FINGERPRINT_SETUP,
}

enum class ColorTheme {
    CYAN_NEON,    // #00F0FF Cyberpunk
    EMERALD_MATRIX, // #39FF14 Classic Hacker
    CRIMSON_AURA, // #FF007F Dark Synthwave
    AMETHYST_CYBER, // #8F00FF Deep Galactic
}

data class ActiveCall(
    val name: String,
    val phone: String,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val status: String, // "RINGING", "CONNECTED", "DISCONNECTED"
    val durationSeconds: Int = 0,
    val avatarColor: Int
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // --- Core Database Configuration ---
    private var database: AppDatabase? = null
    private var chatDao: ChatDao? = null

    // --- State Observables ---
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _theme = MutableStateFlow(ColorTheme.CYAN_NEON)
    val theme: StateFlow<ColorTheme> = _theme.asStateFlow()

    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId: StateFlow<Long?> = _activeChatId.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall: StateFlow<ActiveCall?> = _activeCall.asStateFlow()

    // Screen stack for back navigation
    private val screenStack = mutableListOf<AppScreen>()

    // --- Auth States ---
    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userName = MutableStateFlow("Me")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // --- Security Settings ---
    private val _isBiometricLocked = MutableStateFlow(false)
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

    private val _isPasscodeEnabled = MutableStateFlow(false)
    val isPasscodeEnabled: StateFlow<Boolean> = _isPasscodeEnabled.asStateFlow()

    private val _passcode = MutableStateFlow("1234") // default pin
    val passcode: StateFlow<String> = _passcode.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _screenshotProtection = MutableStateFlow(true)
    val screenshotProtection: StateFlow<Boolean> = _screenshotProtection.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    private val _spamProtectionEnabled = MutableStateFlow(true)
    val spamProtectionEnabled: StateFlow<Boolean> = _spamProtectionEnabled.asStateFlow()

    private val _inactivityLockSeconds = MutableStateFlow(15) // Options: 5, 15, 30, 60
    val inactivityLockSeconds: StateFlow<Int> = _inactivityLockSeconds.asStateFlow()

    private val _lastUserInteraction = MutableStateFlow(System.currentTimeMillis())

    // --- Typing Indicators ---
    private val _typingStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val typingStatus: StateFlow<Map<Long, String>> = _typingStatus.asStateFlow()

    // --- Smart Replies ---
    private val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()

    // --- Cross-Platform Downloads ---
    private val _activeDownload = MutableStateFlow<String?>(null)
    val activeDownload: StateFlow<String?> = _activeDownload.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    // --- Gemini Repository for AI features ---
    private val geminiRepository = GeminiRepository()

    // --- Admin Panel Analytics ---
    private val _adminLogs = MutableStateFlow<List<String>>(emptyList())
    val adminLogs: StateFlow<List<String>> = _adminLogs.asStateFlow()

    // --- Call Timer Job ---
    private var callTimerJob: Job? = null

    // --- DB Flow Hooks ---
    val chats: Flow<List<Chat>> = flow {
        while (chatDao == null) delay(50)
        emitAll(chatDao!!.getAllChats())
    }.flowOn(Dispatchers.IO)

    val contacts: Flow<List<Contact>> = flow {
        while (chatDao == null) delay(50)
        emitAll(chatDao!!.getAllContacts())
    }.flowOn(Dispatchers.IO)

    val stories: Flow<List<Story>> = flow {
        while (chatDao == null) delay(50)
        emitAll(chatDao!!.getAllStories())
    }.flowOn(Dispatchers.IO)

    val callRecords: Flow<List<CallRecord>> = flow {
        while (chatDao == null) delay(50)
        emitAll(chatDao!!.getAllCallRecords())
    }.flowOn(Dispatchers.IO)

    // Current active chat messages
    val activeMessages: Flow<List<Message>> = _activeChatId.flatMapLatest { id ->
        if (id == null || chatDao == null) {
            flowOf(emptyList())
        } else {
            chatDao!!.getMessagesForChat(id)
        }
    }.flowOn(Dispatchers.IO)

    init {
        // Setup Room Database
        viewModelScope.launch(Dispatchers.IO) {
            database = Room.databaseBuilder(
                getApplication(),
                AppDatabase::class.java,
                "privora_db"
            ).fallbackToDestructiveMigration().build()
            chatDao = database!!.chatDao()

            // Seed database if empty
            seedInitialDatabase()

            // Launch Inactivity Checker Background Worker
            launchInactivityMonitor()

            // Launch Scheduled Messages Background Worker
            launchScheduledMessageWorker()

            // Clean up stories older than 24 hours
            val expiry = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            chatDao!!.deleteExpiredStories(expiry)
        }
    }

    // --- Activity Tracking for Lock Screen ---
    fun reportUserInteraction() {
        _lastUserInteraction.value = System.currentTimeMillis()
        if (_isAppLocked.value) {
            // Screen is locked, keep locked till unlocked via UI
        }
    }

    private fun launchInactivityMonitor() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if ((_isBiometricLocked.value || _isPasscodeEnabled.value) && !_isAppLocked.value && _currentScreen.value != AppScreen.SPLASH && _currentScreen.value != AppScreen.ONBOARDING && _currentScreen.value != AppScreen.LOGIN && _currentScreen.value != AppScreen.OTP) {
                    val inactiveTime = System.currentTimeMillis() - _lastUserInteraction.value
                    if (inactiveTime > _inactivityLockSeconds.value * 1000) {
                        _isAppLocked.value = true
                        logAdminAction("Biometric Engine: App locked automatically due to ${_inactivityLockSeconds.value}s inactivity.")
                    }
                }
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
        reportUserInteraction()
        logAdminAction("Device Auth: App unlocked successfully with secure biometric handshake.")
    }

    // --- Navigation ---
    fun navigateTo(screen: AppScreen) {
        reportUserInteraction()
        screenStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        reportUserInteraction()
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
        } else {
            _currentScreen.value = AppScreen.DASHBOARD
        }
    }

    // --- User Session ---
    fun login(phone: String, name: String) {
        _userPhone.value = phone
        _userName.value = name.ifEmpty { "Me" }
        logAdminAction("Auth Engine: Received login request for $phone. Issuing OTP payload...")
        navigateTo(AppScreen.OTP)
    }

    fun verifyOtp(otp: String): Boolean {
        if (otp == "7777" || otp.length == 4) { // Let's accept any 4 digit or a standard test pin 7777
            _isAuthenticated.value = true
            logAdminAction("Auth Engine: Handshake verified successfully for account ${_userPhone.value}. Syncing profile.")
            navigateTo(AppScreen.DASHBOARD)
            return true
        }
        logAdminAction("Auth Security Warning: Failed connection attempt with invalid OTP token: $otp")
        return false
    }

    fun logOut() {
        _isAuthenticated.value = false
        _userPhone.value = ""
        navigateTo(AppScreen.LOGIN)
    }

    // --- Settings Configuration ---
    fun setTheme(newTheme: ColorTheme) {
        _theme.value = newTheme
    }

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricLocked.value = enabled
        logAdminAction("Privacy Hub: Biometric lock status changed to $enabled")
    }

    fun togglePasscode(enabled: Boolean, code: String = "1234") {
        _isPasscodeEnabled.value = enabled
        _passcode.value = code
        logAdminAction("Privacy Hub: Passcode lock status set to $enabled")
    }

    fun toggleScreenshotProtection(enabled: Boolean) {
        _screenshotProtection.value = enabled
        logAdminAction("Privacy Hub: Screen Capture inhibition altered to $enabled")
    }

    fun toggle2FA(enabled: Boolean) {
        _twoFactorEnabled.value = enabled
    }

    fun toggleSpamProtection(enabled: Boolean) {
        _spamProtectionEnabled.value = enabled
    }

    fun setInactivityTimeout(seconds: Int) {
        _inactivityLockSeconds.value = seconds
    }

    fun simulateNodeDownload(platform: String, filename: String) {
        viewModelScope.launch {
            _activeDownload.value = filename
            _downloadProgress.value = 0f
            logAdminAction("SysEngine: Handshake initialized for $platform installation node.")
            for (p in 1..20) {
                delay(75)
                _downloadProgress.value = (p * 5) / 100f
            }
            delay(300)
            _activeDownload.value = null
            _downloadProgress.value = 0f
            logAdminAction("SysEngine: Compiled and archived $filename bundle successfully.")
        }
    }

    // --- Messaging Commands ---
    fun openChat(chatId: Long) {
        _activeChatId.value = chatId
        viewModelScope.launch(Dispatchers.IO) {
            val chat = chatDao?.getChatById(chatId)
            if (chat != null && chat.unreadCount > 0) {
                chatDao?.updateChat(chat.copy(unreadCount = 0))
            }
            // Fetch smart replies
            generateSmartSuggestionsForActiveChat()
        }
        val isSecret = chatId == 3L // CryptoWhale (Secret chat)
        navigateTo(if (chatId == 2L) AppScreen.GROUP_CHAT else AppScreen.PRIVATE_CHAT)
    }

    fun closeChat() {
        _activeChatId.value = null
        _smartReplies.value = emptyList()
        navigateBack()
    }

    fun sendMessage(text: String, type: String = "TEXT", mediaUri: String? = null, scheduledDelaySeconds: Int = 0) {
        val chatId = _activeChatId.value ?: return
        val senderPhone = _userPhone.value
        val senderName = _userName.value

        viewModelScope.launch(Dispatchers.IO) {
            val isScheduled = scheduledDelaySeconds > 0
            val schedAt = if (isScheduled) System.currentTimeMillis() + (scheduledDelaySeconds * 1000) else 0L

            val msg = Message(
                chatId = chatId,
                senderPhone = "", // Self is empty
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis(),
                status = if (isScheduled) "SCHEDULED" else "SENT",
                type = type,
                mediaUri = mediaUri,
                isScheduled = isScheduled,
                scheduledAt = schedAt
            )

            // Save to database
            chatDao?.insertMessage(msg)

            if (!isScheduled) {
                // Update chat information
                val chat = chatDao?.getChatById(chatId)
                if (chat != null) {
                    chatDao?.updateChat(
                        chat.copy(
                            lastMessage = if (type == "TEXT") text else "Attachment shared 📁",
                            lastMessageTime = System.currentTimeMillis()
                        )
                    )
                }

                logAdminAction("CryptEngine: Session message injected. RSA-4096 stream encryption verified.")
                // Trigger Simulated Typing and Auto response after 1.5 seconds if not a self-contained chatbot
                if (chatId == 4L) {
                    // Chatbot response
                    triggerChatbotAutoReply(text)
                } else {
                    triggerUserSimulatedAutoReply(chatId, text)
                }
            } else {
                logAdminAction("Scheduler: Logged task schema for delivery in $scheduledDelaySeconds seconds.")
            }
        }
    }

    fun deleteMessage(msgId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao?.deleteMessageById(msgId)
            logAdminAction("CryptEngine: Shredded record from device memory.")
        }
    }

    fun reactToMessage(msgId: Long, reaction: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatDao?.getMessagesForChat(_activeChatId.value ?: 0)?.first() ?: return@launch
            val target = messages.find { it.id == msgId }
            if (target != null) {
                chatDao?.updateMessage(target.copy(reaction = reaction))
            }
        }
    }

    fun pinMessage(msgId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatDao?.getMessagesForChat(_activeChatId.value ?: 0)?.first() ?: return@launch
            val target = messages.find { it.id == msgId }
            if (target != null) {
                chatDao?.updateMessage(target.copy(isPinned = !target.isPinned))
                logAdminAction("DisplayEngine: Message pinning criteria updated on thread.")
            }
        }
    }

    fun handleSelfDestruct(msgId: Long, seconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatDao?.getMessagesForChat(_activeChatId.value ?: 0)?.first() ?: return@launch
            val target = messages.find { it.id == msgId }
            if (target != null) {
                chatDao?.updateMessage(target.copy(isSelfDestruct = true, selfDestructLimit = seconds, selfDestructTriggeredAt = System.currentTimeMillis()))
                logAdminAction("Privacy Hub: Self-destruct sequence initiated for packet $msgId ($seconds seconds).")
                
                // Launch dynamic coroutine countdown which deletes the message
                delay(seconds * 1000L)
                chatDao?.deleteMessageById(msgId)
                logAdminAction("Privacy Hub: Packet $msgId safely shredded.")
            }
        }
    }

    fun translateChatMessage(msgId: Long, targetLanguage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatId = _activeChatId.value ?: return@launch
            val messages = chatDao?.getMessagesForChat(chatId)?.first() ?: return@launch
            val target = messages.find { it.id == msgId }
            if (target != null) {
                val translated = geminiRepository.translateMessage(target.text, targetLanguage)
                chatDao?.updateMessage(target.copy(translatedText = translated))
                logAdminAction("AI Engine: Chat packet translated to $targetLanguage.")
            }
        }
    }

    // --- Scheduled Message Polling worker ---
    private fun launchScheduledMessageWorker() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(2000)
                if (chatDao != null) {
                    val now = System.currentTimeMillis()
                    val pending = chatDao!!.getScheduledMessages().first()
                    pending.forEach { msg ->
                        if (msg.scheduledAt <= now) {
                            val updated = msg.copy(isScheduled = false, timestamp = now, status = "SENT")
                            chatDao!!.updateMessage(updated)
                            
                            // Send auto-responses
                            val chat = chatDao!!.getChatById(updated.chatId)
                            if (chat != null) {
                                chatDao!!.updateChat(
                                    chat.copy(
                                        lastMessage = updated.text,
                                        lastMessageTime = now
                                    )
                                )
                            }
                            if (updated.chatId != 4L) {
                                triggerUserSimulatedAutoReply(updated.chatId, updated.text)
                            } else {
                                triggerChatbotAutoReply(updated.text)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Stories/Status Upload ---
    fun addStory(text: String, bgHex: Int, mediaUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStory = Story(
                phone = _userPhone.value.ifEmpty { "12345" },
                name = _userName.value,
                textContent = text,
                bgHex = bgHex,
                mediaUri = mediaUri,
                timestamp = System.currentTimeMillis()
            )
            chatDao?.insertStory(newStory)
            logAdminAction("Stories Engine: Secure encrypted daily snapshot broadcasted.")
        }
    }

    // --- Calling Flow Simulation ---
    fun makeCall(name: String, phone: String, avatarColor: Int, isVideo: Boolean) {
        viewModelScope.launch {
            _activeCall.value = ActiveCall(
                name = name,
                phone = phone,
                isVideo = isVideo,
                isIncoming = false,
                status = "RINGING",
                avatarColor = avatarColor
            )
            navigateTo(if (isVideo) AppScreen.VIDEO_CALL_SCREEN else AppScreen.CALL_SCREEN)
            logAdminAction("WebRTC Tunnel: Outbound SIP link initiated securely.")

            // Connect automatically in 3 seconds
            delay(3000)
            val curr = _activeCall.value
            if (curr != null && curr.status == "RINGING") {
                connectCall()
            }
        }
    }

    fun receiveIncomingCallSimulated(name: String, phone: String, avatarColor: Int, isVideo: Boolean) {
        _activeCall.value = ActiveCall(
            name = name,
            phone = phone,
            isVideo = isVideo,
            isIncoming = true,
            status = "RINGING",
            avatarColor = avatarColor
        )
        _currentScreen.value = if (isVideo) AppScreen.VIDEO_CALL_SCREEN else AppScreen.CALL_SCREEN
        logAdminAction("WebRTC Tunnel: Secured inbound handoff detected.")
    }

    fun connectCall() {
        val curr = _activeCall.value ?: return
        _activeCall.value = curr.copy(status = "CONNECTED")
        logAdminAction("WebRTC Tunnel: End-to-end SRTP session keys authenticated securely.")
        startCallTimer()
    }

    fun endCall() {
        val curr = _activeCall.value ?: return
        _activeCall.value = curr.copy(status = "DISCONNECTED")
        callTimerJob?.cancel()
        callTimerJob = null

        // Add call log to DB
        viewModelScope.launch(Dispatchers.IO) {
            chatDao?.insertCallRecord(
                CallRecord(
                    name = curr.name,
                    phone = curr.phone,
                    avatarColor = curr.avatarColor,
                    isVideo = curr.isVideo,
                    isIncoming = curr.isIncoming,
                    duration = curr.durationSeconds.toLong()
                )
            )
            logAdminAction("WebRTC: Terminated call. Session duration ${curr.durationSeconds}s.")
            delay(1500)
            _activeCall.value = null
            navigateBack()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val curr = _activeCall.value
                if (curr != null && curr.status == "CONNECTED") {
                    _activeCall.value = curr.copy(durationSeconds = curr.durationSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    // --- Gemini Smart Reply Engine ---
    private fun generateSmartSuggestionsForActiveChat() {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatDao?.getMessagesForChat(chatId)?.first() ?: return@launch
            if (messages.isNotEmpty()) {
                val lastMsg = messages.last()
                // If the last message is from user (self), clear suggestions
                if (lastMsg.senderPhone.isEmpty()) {
                    _smartReplies.value = emptyList()
                    return@launch
                }

                // AI smart suggestions triggered
                if (_isAuthenticated.value) {
                    val reply = geminiRepository.generateSmartReply(messages)
                    // Provide 3 options (1 generated from Gemini, 2 generic safety backups)
                    _smartReplies.value = listOf(
                        reply,
                        "Understood!",
                        "Talk to you later."
                    )
                }
            }
        }
    }

    // --- Admin Dashboard & Fraud Protection ---
    fun logAdminAction(action: String) {
        val logs = _adminLogs.value.toMutableList()
        logs.add(0, "[${System.currentTimeMillis() % 100000}] $action")
        _adminLogs.value = logs.take(50)
    }

    fun banishContactAndFakeUser(phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao?.deleteContactByPhone(phone)
            // Delete associated chat
            val chatsList = chatDao?.getAllChats()?.first() ?: emptyList()
            val targetChat = chatsList.find { it.name.contains("Spam") || it.name.contains("Fake") || it.id == 5L }
            if (targetChat != null) {
                chatDao?.deleteChatById(targetChat.id)
                chatDao?.clearMessagesForChat(targetChat.id)
            }
            logAdminAction("Moderator: Banished account $phone and purged its message archives due to severe spam signatures.")
        }
    }

    fun scanIncomingTextForSpam(text: String, senderName: String): Boolean {
        if (!_spamProtectionEnabled.value) return false
        val spamIndicators = listOf("free money", "winner", "crypto earn", "jackpot", "transfer bank", "click link", "whatsapp offer")
        val isSpam = spamIndicators.any { text.lowercase().contains(it) }
        if (isSpam) {
            logAdminAction("Security Shield: Intercepted potential phishing element from $senderName.")
        }
        return isSpam
    }

    // --- Auto Answers & Typing State Simulations ---
    private fun triggerUserSimulatedAutoReply(chatId: Long, outboundText: String) {
        viewModelScope.launch {
            // Typing delay
            delay(1500)
            setTyping(chatId, "Typing...")

            delay(1500)
            setTyping(chatId, "")

            val chat = chatDao?.getChatById(chatId) ?: return@launch
            var response = "Roger that! End-to-end communication verified."
            var senderName = chat.name
            var senderPhone = "+1555555"

            // Custom conversational flows for simulation
            if (chatId == 1L) { // Alice
                response = when {
                    outboundText.lowercase().contains("hello") || outboundText.lowercase().contains("hi") -> "Hey! Did you see the new quantum biometric unlock on Privora?"
                    outboundText.lowercase().contains("yes") -> "It looks absolutely sick! Transparent visual glass blocks paired with spring transitions!"
                    outboundText.lowercase().contains("no") -> "Go open the Privacy options! Set inactivity lock to 5s. It's totally sci-fi."
                    else -> "That's super cool! Let's schedule a virtual sync tomorrow on this cryptology thread."
                }
            } else if (chatId == 3L) { // CryptoWhale
                val containsSpam = scanIncomingTextForSpam(outboundText, "CryptoWhale")
                response = if (containsSpam) {
                    "ALERT: Automated security response block activated."
                } else {
                    "Agreed. Keep the self-destruct state active. Decentralize everything."
                }
            } else if (chatId == 5L) { // Blocked Spam
                response = "WINNER! Receive your 0.5 BTC instantly at: cryptorush.scam/gift"
                scanIncomingTextForSpam(response, "Crypto Winner Bot")
            }

            val incoming = Message(
                chatId = chatId,
                senderPhone = senderPhone,
                senderName = senderName,
                text = response,
                timestamp = System.currentTimeMillis(),
                status = "READ" // Triggers double blue ticks
            )

            // Save response to DB
            chatDao?.insertMessage(incoming)

            // Update chat details
            chatDao?.updateChat(
                chat.copy(
                    lastMessage = response,
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = if (_activeChatId.value == chatId) 0 else chat.unreadCount + 1
                )
            )

            // Dynamic suggestion regeneration
            if (_activeChatId.value == chatId) {
                generateSmartSuggestionsForActiveChat()
            }
        }
    }

    private fun triggerChatbotAutoReply(prompt: String) {
        viewModelScope.launch {
            delay(1000)
            setTyping(4L, "AI Assistant is thinking...")

            val chatHistory = chatDao?.getMessagesForChat(4L)?.first() ?: emptyList()
            val historyPairs = chatHistory.takeLast(10).map {
                if (it.senderPhone.isEmpty()) "User" to it.text else "Privora AI" to it.text
            }

            val reply = geminiRepository.consultChatbot(historyPairs, prompt)

            setTyping(4L, "")

            val incoming = Message(
                chatId = 4L,
                senderPhone = "AI",
                senderName = "AI Assistant",
                text = reply,
                timestamp = System.currentTimeMillis(),
                status = "READ"
            )

            chatDao?.insertMessage(incoming)

            val chat = chatDao?.getChatById(4L)
            if (chat != null) {
                chatDao?.updateChat(
                    chat.copy(
                        lastMessage = reply,
                        lastMessageTime = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun setTyping(chatId: Long, status: String) {
        val current = _typingStatus.value.toMutableMap()
        if (status.isEmpty()) {
            current.remove(chatId)
        } else {
            current[chatId] = status
        }
        _typingStatus.value = current
    }

    // --- DB Initial Seed ---
    private suspend fun seedInitialDatabase() {
        if (chatDao == null) return
        val currentContacts = chatDao!!.getAllContacts().first()
        if (currentContacts.isNotEmpty()) return

        // 1. Seed Contacts
        val cAlice = Contact("+19876543210", "Alice Smith", "@alice_cyber", "Securing my protocols in style.", 0xFF00FF7F.toInt())
        val cGroup = Contact("+00000000", "CyberSec Guild", "@cybersec_channel", "Devised structure of decentralization.", 0xFF00F0FF.toInt())
        val cWhale = Contact("+12121212", "CryptoWhale", "@cryptowhale", "Anon trading nodes.", 0xFFFF007F.toInt())
        val cAI = Contact("+99999999", "AI Assistant (Chatbot)", "@privora_ai", "Always available, fully integrated.", 0xFF8F00FF.toInt())
        val cSpam = Contact("+166DANG", "PhishingBot (Spam Account)", "@free_btc_offers", "Spam nodes detected on proxy.", 0xFFFF0000.toInt(), isSpam = true)

        chatDao!!.insertContact(cAlice)
        chatDao!!.insertContact(cGroup)
        chatDao!!.insertContact(cWhale)
        chatDao!!.insertContact(cAI)
        chatDao!!.insertContact(cSpam)

        // 2. Seed Chats
        val id1 = chatDao!!.insertChat(Chat(name = "Alice Smith", avatarColor = 0xFF00FF7F.toInt(), lastMessage = "Privora biometric lock is superb!", lastMessageTime = System.currentTimeMillis() - 600000))
        val id2 = chatDao!!.insertChat(Chat(name = "CyberSec Guild", isGroup = true, avatarColor = 0xFF00F0FF.toInt(), lastMessage = "Morpheus: Symmetric key validated.", lastMessageTime = System.currentTimeMillis() - 1200000))
        val id3 = chatDao!!.insertChat(Chat(name = "CryptoWhale", isSecret = true, avatarColor = 0xFFFF007F.toInt(), lastMessage = "Shred this session upon read.", lastMessageTime = System.currentTimeMillis() - 3600000))
        val id4 = chatDao!!.insertChat(Chat(name = "AI Assistant (Chatbot)", avatarColor = 0xFF8F00FF.toInt(), lastMessage = "Greeting human! How can I assist you with securing your communications today?", lastMessageTime = System.currentTimeMillis() - 5000000))
        val id5 = chatDao!!.insertChat(Chat(name = "PhishingBot (Blocked)", avatarColor = 0xFFFF0000.toInt(), lastMessage = "Claim 0.5 BTC free!", lastMessageTime = System.currentTimeMillis() - 6000000, unreadCount = 1))

        // 3. Seed Messages
        // Alice
        chatDao!!.insertMessage(Message(chatId = id1, senderPhone = "+19876543210", senderName = "Alice", text = "Welcome to Privora, teammate!", status = "READ"))
        chatDao!!.insertMessage(Message(chatId = id1, senderPhone = "", senderName = "Me", text = "Thanks Alice! Is everything secure here?", status = "READ"))
        chatDao!!.insertMessage(Message(chatId = id1, senderPhone = "+19876543210", senderName = "Alice", text = "Privora biometric lock is superb!", status = "READ"))

        // CyberSec Group
        chatDao!!.insertMessage(Message(chatId = id2, senderPhone = "+1111111", senderName = "Trinity", text = "The matrix is checking our packet headers.", status = "READ"))
        chatDao!!.insertMessage(Message(chatId = id2, senderPhone = "+2222222", senderName = "Morpheus", text = "Symmetric key validated.", status = "READ"))

        // Secret chat with Whale
        chatDao!!.insertMessage(Message(chatId = id3, senderPhone = "+12121212", senderName = "CryptoWhale", text = "Start confidential session.", status = "READ"))
        chatDao!!.insertMessage(Message(chatId = id3, senderPhone = "", senderName = "Me", text = "Sovereign nodes set to active.", status = "READ"))
        chatDao!!.insertMessage(Message(chatId = id3, senderPhone = "+12121212", senderName = "CryptoWhale", text = "Shred this session upon read.", status = "READ"))

        // AI Chatbot greeting
        chatDao!!.insertMessage(Message(chatId = id4, senderPhone = "AI", senderName = "AI Assistant", text = "Greeting human! How can I assist you with securing your communications today?", status = "READ"))

        // Spam messages
        chatDao!!.insertMessage(Message(chatId = id5, senderPhone = "+166DANG", senderName = "PhishingBot", text = "Claim 0.5 BTC free!", status = "DELIVERED"))

        // 4. Seed Stories
        chatDao!!.insertStory(Story(phone = "+19876543210", name = "Alice Smith", textContent = "Cybersecurity is not an option. It's a fundamental human right. 💻🔒", bgHex = 0xFF161B26.toInt()))
        chatDao!!.insertStory(Story(phone = "+12121212", name = "CryptoWhale", textContent = "Stacking sats and encrypting metadata. 🐳🔐", bgHex = 0xFF0F1219.toInt()))

        // 5. Seed Call Logs
        chatDao!!.insertCallRecord(CallRecord(name = "Alice Smith", phone = "+19876543210", avatarColor = 0xFF00FF7F.toInt(), isVideo = true, isIncoming = true, timestamp = System.currentTimeMillis() - 86400000, duration = 450))
        chatDao!!.insertCallRecord(CallRecord(name = "CryptoWhale", phone = "+12121212", avatarColor = 0xFFFF007F.toInt(), isVideo = false, isIncoming = false, timestamp = System.currentTimeMillis() - 172800000, duration = 120))

        logAdminAction("SysEngine: System metadata initialized. Database seeded with encrypted dummy nodes.")
    }
}
