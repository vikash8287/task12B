package com.chamberly.chamberly.presentation.fragments

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getColor
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chamberly.chamberly.R
import com.chamberly.chamberly.presentation.adapters.MessageAdapter
import com.chamberly.chamberly.models.ActiveChatInfoModel
import com.chamberly.chamberly.models.Message
import com.chamberly.chamberly.models.toMap
import com.chamberly.chamberly.presentation.viewmodels.ChamberViewModel
import com.chamberly.chamberly.presentation.viewmodels.UserViewModel
import com.chamberly.chamberly.notification.ReminderNotification

import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val chamberViewModel: ChamberViewModel by activityViewModels()

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emojiPickerView: EmojiPickerView
    private lateinit var cancelReplyButton: ImageButton
    private lateinit var replyContentView: TextView
    private lateinit var addImageButton: Button
    private lateinit var messagesCountView: TextView

    private val pickMedia = registerActivityResultLauncher()
    private val replyingTo = MutableLiveData("")

    private val reactionEmojis: List<String> = listOf("👍", "💗", "😂", "😯", "😥", "😔", "+")
    private val chamberLeavingOptions: Map<String, String> = mapOf(
        "Done Venting" to "\"I am done venting, thank you so much 💗\"",
        "Wrong Match" to "\"Wrong match, sorry 😢\"",
        "In a Hurry" to "\"Sorry, I am in a hurry 😰\"",
        "Just checking the app out" to "\"Just checking the app 🥰\"",
        "No activity" to "\"There is no activity 😔\""
    )
    private val reportReasons: List<String> = listOf(
        "Harassment",
        "Sexual Behavior",
        "Unsupportive Behaviour",
        "Spamming",
        "Annoying"
    )

    override fun onResume() {
        super.onResume()
        chamberViewModel.removeNotificationKey(userViewModel.userState.value!!.UID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userViewModel.chamberID.value.isNullOrBlank()) {
            userViewModel.closeChamber()
        }
        chamberViewModel.clear(
            userViewModel.chamberID.value!!,
            userViewModel.userState.value!!.UID
        )
        cancelNotificationAlarm(requireContext())
        chamberViewModel.setChamber(
            userViewModel.chamberID.value!!,
            userViewModel.userState.value!!.UID
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        val groupTitle = view.findViewById<TextView>(R.id.groupTitle)
        messagesCountView = view.findViewById(R.id.messagesCount)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val exitChamberButton = view.findViewById<ImageButton>(R.id.exitChatButton)
        recyclerView = view.findViewById(R.id.recyclerViewMessages)
        val sendButton = view.findViewById<Button>(R.id.buttonSend)
        val messageBox = view.findViewById<EditText>(R.id.editTextMessage)
        val replyView = view.findViewById<LinearLayout>(R.id.replyingToView)
        addImageButton = view.findViewById(R.id.buttonAddImage)
        emojiPickerView = view.findViewById(R.id.reaction_emoji_picker)
        replyContentView = view.findViewById(R.id.replyContentView)
        cancelReplyButton = view.findViewById(R.id.cancelReplyButton)

        replyingTo.observe(viewLifecycleOwner) {
            replyContentView.text = replyingTo.value
            if (it.isNullOrBlank()) {
                replyView.visibility = View.GONE
            } else {
                replyView.visibility = View.VISIBLE
            }
        }

        messageAdapter = MessageAdapter(userViewModel.userState.value!!.UID)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        val recyclerViewLayoutChangeListener =
            View.OnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                val bottomDifference = oldBottom - bottom
                if (bottomDifference > 0) {
                    recyclerView.smoothScrollBy(0, bottomDifference)
                }
            }

        recyclerView.addOnLayoutChangeListener(recyclerViewLayoutChangeListener)

        messageAdapter.setOnMessageLongClickListener(
            object : MessageAdapter.OnMessageLongClickListener {
                override fun onMessageLongClick(message: Message) {
                    showMessageDialog(message)
                }

                override fun onSelfLongClick(message: Message) {
                    showSelfMessageDialog(message)
                }
            }
        )

        chamberViewModel.messages.observe(viewLifecycleOwner) { messages ->
            val chamberID = chamberViewModel.chamberState.value!!.chamberID

            messagesCountView.text =
                "${messages[chamberID]?.size ?: 0}/${chamberViewModel.messageLimitCount}"

            if (chamberID.isBlank() || messages[chamberID] == null) {
                return@observe
            }

            if (messageAdapter.itemCount == 0) {
                messages?.let {
                    messageAdapter.setMessages(it[chamberID]!!)
                    recyclerView.scrollToPosition(it[chamberID]!!.size - 1)
                }
            } else {
                val newSize = messages[chamberID]!!.size
                val oldSize = messageAdapter.itemCount
                if (newSize == oldSize) {
                    if (
                        newSize == 40 &&
                        messageAdapter.messageAt(0).message_id != messages[chamberID]!![0].message_id
                    ) {
                        // More than 40 messages & a new message received
                        messageAdapter.messageRemoved(messageAdapter.messageAt(0))
                        messageAdapter.addMessage(messages[chamberID]!![newSize - 1])
                        recyclerView.smoothScrollToPosition(39)
                    } else {
                        // A message was modified, following cases are possible
                        // A reaction was added/modified/removed
                        // The message was edited
                        // Since there are only 40 message, we iterate over all and find the one
                        // that was modified.
                        for (index in 0 until newSize) {
                            if (messages[chamberID]!![index] != messageAdapter.messageAt(index)) {
                                messageAdapter.messageChanged(
                                    messages[chamberID]!![index],
                                    messages[chamberID]!![index].message_id
                                )
                                recyclerView.smoothScrollBy(0, 20)
                            }
                        }
                    }
                } else if (newSize > oldSize) {
                    // A new message was received
                    messageAdapter.addMessage(messages[chamberID]!![newSize - 1])
                    recyclerView.smoothScrollToPosition(newSize - 1)
                } else {
                    // A message was removed
                    // Will be handled later since there is no front-end functionality to delete
                    // messages
                }
            }
        }

        sendButton.setOnClickListener {
            val chamberID = chamberViewModel.chamberState.value!!.chamberID
            // TODO: This check is to be done using messagesCount in RT-DB
            if ((chamberViewModel.messages.value!![chamberID]?.size?.toLong()
                    ?: 0L) >= chamberViewModel.messageLimitCount
            ) {
                showTooManyMessagesDialog()
                return@setOnClickListener
            }
            val text = messageBox.text.toString()
            if (text.isBlank()) {
                return@setOnClickListener
            }
            val message = Message(
                UID = userViewModel.userState.value!!.UID,
                sender_name = userViewModel.userState.value!!.displayName,
                message_content = text,
                message_type = "text",
                replyingTo = replyingTo.value ?: ""
            )
            chamberViewModel.sendMessage(
                message,
                successCallback = {
                    replyingTo.postValue("")
                    messageBox.setText("")
//                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
            )
        }

        chamberViewModel.chamberState.observe(viewLifecycleOwner) {
            groupTitle.text = it.chamberTitle
        }

        backButton.setOnClickListener {
            chamberViewModel.clear(
                userViewModel.userState.value!!.UID,
                userViewModel.userState.value!!.notificationKey
            )
            userViewModel.closeChamber()
        }

        val backPressHandler = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                chamberViewModel.clear(
                    userViewModel.userState.value!!.UID,
                    userViewModel.userState.value!!.notificationKey
                )
                userViewModel.closeChamber()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)
        exitChamberButton.setOnClickListener {
            showChamberExitDialog()
        }
        //val postImage = PostImage(activity= requireActivity(),storage = FirebaseStorage.getInstance(),auth = FirebaseAuth.getInstance(), database = FirebaseDatabase.getInstance(),groupChatId = groupChatId,senderName=senderName,messageAdapter = messageAdapter, recyclerView = recyclerView,messages = chamberViewModel.messages)

        addImageButton.setOnClickListener {
            launchPhotoPicker()
        }
//        groupTitle.setOnClickListener {
//            chamberViewModel.getChamberMetadata {
//                if (it.isNotEmpty()) {
//                    val chamberInfo = ActiveChatInfoModel(
//                        groupChatID = chamberViewModel.chamberState.value!!.chamberID,
//                        groupChatName = groupTitle.text as String,
//                        activeChatMemberLimit = 2,
//                        memberInfoList = it
//                    )
//                    val args = Bundle()
//                    args.putSerializable("chamberInfo", chamberInfo)
//                    val navController = requireActivity().findNavController(R.id.navHostFragment)
//                    Log.i("calledBefore", "is bbeing caLLED")
//
//                    navController.navigate(
//                        R.id.chamber_info_fragment,
//                        args = args,
//                        navOptions {
//                            anim {
//                                enter = R.anim.slide_in
//                                exit = R.anim.slide_out
//                                popEnter = R.anim.slide_in
//                                popExit = R.anim.slide_out
//                            }
//                        }
//
//                    )
//                    Log.i("called", "is bbeing caLLED")
//                } else {
//                    Toast.makeText(context, "Chamber info not available", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun showTooManyMessagesDialog() {
        val dialog = Dialog(requireContext(), R.style.Dialog)
        dialog.setContentView(R.layout.dialog_too_many_messages)

        val getChamberlyPlusButton = dialog.findViewById<Button>(R.id.getChamberlyPlusButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        getChamberlyPlusButton.setOnClickListener {
            dialog.dismiss()
            openSubscriptionScreen()
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openSubscriptionScreen() {
        findNavController()
            .navigate(
                R.id.subscriptionFragment,
                null,
                navOptions {
                    anim {
                        enter = R.anim.slide_in
                        exit = R.anim.slide_out
                        popEnter = R.anim.slide_in
                        popExit = R.anim.slide_out
                    }
                }
            )
    }

    private fun showMessageDialog(message: Message) {
        val dialog = Dialog(requireContext(), R.style.Dialog)
        dialog.setContentView(R.layout.popup_message_options_others)

        val dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)
        val replyButton = dialog.findViewById<Button>(R.id.buttonReply)
        val reportButton = dialog.findViewById<Button>(R.id.buttonReport)
        val blockButton = dialog.findViewById<Button>(R.id.buttonBlock)
        val rateUserButton = dialog.findViewById<Button>(R.id.buttonRate)
        val reactionEmojisView = dialog.findViewById<LinearLayout>(R.id.reactionEmojis)

        dialogTitle.text = message.sender_name
        dialogMessage.text = message.message_content

        for (emoji in reactionEmojis) {
            val emojiButton = TextView(requireContext())
            emojiButton.text = emoji
            emojiButton.setTextColor(Color.BLACK)
            emojiButton.textSize = 24.0f
            emojiButton.setPadding(8, 8, 8, 8)
            emojiButton.setOnClickListener {
                if (emoji != "+") {
                    react(message, emoji)
                } else {
                    emojiPickerView.visibility = View.VISIBLE
                    emojiPickerView.setOnEmojiPickedListener {
                        dialog.dismiss()
                        emojiPickerView.visibility = View.GONE
                        react(message, it.emoji)
                    }
                }
                recyclerView.smoothScrollBy(0, 20)
                dialog.dismiss()
            }
            reactionEmojisView.addView(emojiButton)
        }

        copyButton.setOnClickListener {
            copyMessage(message.message_content)
            dialog.dismiss()
        }

        replyButton.setOnClickListener {
            replyingTo.value = message.message_content
            cancelReplyButton.setOnClickListener {
                replyingTo.value = ""
            }
            dialog.dismiss()
        }

        rateUserButton.setOnClickListener {
            showRatingDialog(dialog, message.UID, message.sender_name)
        }

        reportButton.setOnClickListener {
            showReportDialog(
                dialog,
                against = message.UID,
                againstName = message.sender_name
            )
        }

        blockButton.setOnClickListener {
            dialog.setContentView(R.layout.confirm_block)
            val blockDialogTitle = dialog.findViewById<TextView>(R.id.blockDialogTitle)
            blockDialogTitle.text =
                getString(R.string.block_user_dialog_title, message.sender_name)
            val confirmButton = dialog.findViewById<Button>(R.id.buttonConfirmBlock)
            val cancelButton = dialog.findViewById<Button>(R.id.buttonCancelBlock)

            confirmButton.setOnClickListener {
                blockUser(message.UID)
                dialog.dismiss()
                showChamberExitDialog()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
                showMessageDialog(message)
            }
        }

//        val window = dialog.window
//        val layoutParams = WindowManager.LayoutParams()
//        layoutParams.copyFrom(window?.attributes)
//        layoutParams.width = min(layoutParams.width, 400)
//        layoutParams.gravity = Gravity.CENTER
//        window?.attributes = layoutParams

        dialog.show()
    }

    private fun react(message: Message, reaction: String) {
        chamberViewModel.react(message.message_id, reaction)
    }

    private fun copyMessage(messageContent: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", messageContent)
        clipboard.setPrimaryClip(clip)
    }

    private fun showSelfMessageDialog(message: Message) {
        val dialog = Dialog(requireContext(), R.style.Dialog)
        dialog.setContentView(R.layout.popup_message_options_self)

        val dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)

        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        window?.attributes = layoutParams

        dialogTitle.text = message.sender_name
        dialogMessage.text = message.message_content

        copyButton.setOnClickListener {
            copyMessage(message.message_content)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChamberExitDialog() {
        val dialog = Dialog(requireContext(), R.style.Dialog)
        dialog.setContentView(R.layout.dialog_leave_chamber)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val chamberExitOptionsLayout =
            dialog.findViewById<LinearLayout>(R.id.leave_chamber_options)
        val uid = userViewModel.userState.value!!.UID
        val displayName = userViewModel.userState.value!!.displayName
        var message = Message(
            UID = uid,
            message_content = "$displayName left the chat. Reason: ",
            message_type = "custom",
            sender_name = displayName
        )

        for ((heading, reason) in chamberLeavingOptions) {
            val optionButton = TextView(requireContext())
            optionButton.text = heading
            optionButton.setTextColor(getColor(requireContext(), R.color.primary))
            optionButton.textSize = 18.0f
            optionButton.setPaddingRelative(16, 16, 16, 16)
            optionButton.gravity = Gravity.CENTER_HORIZONTAL
            optionButton.setOnClickListener {
                message = message.copy(
                    message_content = message.message_content + reason
                )
                val chamberMembers = chamberViewModel.chamberState.value!!.members
                val userToRateUID =
                    try {
                        if (chamberMembers[0] != userViewModel.userState.value!!.UID) chamberMembers[0]
                        else chamberMembers[1]
                    } catch (_: Exception) {
                        // The user is currently alone in the chamber so no need to show
                        // the reporting dialog
                        ""
                    }
                val userToRateName = chamberViewModel.memberNames[userToRateUID] ?: ""
                chamberViewModel
                    .sendExitMessage(
                        message = message,
                        callback = {
                            if (userToRateUID.isNotBlank()) {
                                showRatingDialog(
                                    dialog = dialog,
                                    userToRateUID = userToRateUID,
                                    userToRateName = userToRateName,
                                    callback = {
                                        dialog.dismiss()
                                        exitChamber()
                                        chamberViewModel.clear(
                                            userViewModel.userState.value!!.UID,
                                            ""
                                        )
                                    }
                                )
                            } else {
                                dialog.dismiss()
                                exitChamber()
                                chamberViewModel.clear(
                                    userViewModel.userState.value!!.UID,
                                    ""
                                )
                            }
                        }
                    )
            }
            chamberExitOptionsLayout.addView(optionButton)
        }

        val cancelButton = TextView(requireContext())
        cancelButton.text = "CANCEL"
        cancelButton.setTextColor(Color.BLACK)
        cancelButton.textSize = 18.0f
        cancelButton.setPadding(16, 16, 16, 16)
        cancelButton.gravity = Gravity.CENTER_HORIZONTAL
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        chamberExitOptionsLayout.addView(cancelButton)
        dialog.show()
    }

    private fun showRatingDialog(
        dialog: Dialog,
        userToRateUID: String,
        userToRateName: String,
        callback: () -> Unit = {
            dialog.dismiss()
        }
    ) {
        dialog.setContentView(R.layout.dialog_rate_user)

        val heading = dialog.findViewById<TextView>(R.id.title_rate_user)
        val ratingBar = dialog.findViewById<RatingBar>(R.id.user_review_bar)
        val cancelButton = dialog.findViewById<Button>(R.id.button_rating_cancel)
        val confirmButton = dialog.findViewById<Button>(R.id.button_rating_confirm)

        heading.text = getString(R.string.rate_user, userToRateName)

        cancelButton.setOnClickListener {
            dialog.dismiss()
            callback()
        }

        confirmButton.setOnClickListener {
            val stars = ratingBar.rating
            rateUser(userToRateUID, stars.toDouble())
            if (stars == 5.0f) {
                askForPlayStoreReview(dialog, callback = callback)
            } else if (stars <= 3.0f) {
                blockUser(uid = userToRateUID)
                showReportDialog(
                    dialog = dialog,
                    against = userToRateUID,
                    againstName = userToRateName,
                    callback = callback
                )
            } else {
                callback()
            }
        }
    }

    private fun showReportDialog(
        dialog: Dialog,
        against: String,
        againstName: String,
        callback: () -> Unit = { dialog.dismiss() }
    ) {
        dialog.setContentView(R.layout.dialog_report_options)

        val titleTextView = dialog.findViewById<TextView>(R.id.textReportTitle)
        val optionsLayout = dialog.findViewById<LinearLayout>(R.id.reportReasonsLayout)

        titleTextView.text = getString(R.string.report_user_title_text, againstName)

        for (reason in reportReasons) {
            val optionButton = TextView(requireContext())
            optionButton.text = reason
            optionButton.setTextColor(getColor(requireContext(), R.color.red))
            optionButton.textSize = 18.0f
            optionButton.setPaddingRelative(16, 32, 16, 32)
            optionButton.gravity = Gravity.CENTER_HORIZONTAL
            optionButton.setOnClickListener {
                reportUser(against = against, reason = reason)
                callback()
            }
            optionsLayout.addView(optionButton)
        }
        val cancelButton = TextView(requireContext())
        cancelButton.text = getString(R.string.cancel)
        cancelButton.setTextColor(Color.BLACK)
        cancelButton.textSize = 18.0f
        cancelButton.setPaddingRelative(16, 16, 16, 16)
        cancelButton.gravity = Gravity.CENTER_HORIZONTAL
        cancelButton.setOnClickListener {
            callback()
        }
        optionsLayout.addView(cancelButton)
    }

    private fun rateUser(userToRate: String, starRating: Double) {
        chamberViewModel
            .rateUser(
                userToRate = userToRate,
                starRating = starRating,
                UID = userViewModel.userState.value!!.UID
            )
    }

    private fun reportUser(
        against: String? = null,
        reason: String,
        selfReport: Boolean = true
    ) {
        val members = chamberViewModel.chamberState.value!!.members
        val uid =
            if (members.size == 1) ""
            else if (members[0] == userViewModel.userState.value!!.UID) members[1]
            else members[0]
        val report = hashMapOf(
            "against" to (against ?: uid),
            "by" to userViewModel.userState.value!!.UID,
            "groupChatId" to chamberViewModel.chamberState.value!!.chamberID,
            "realHost" to "",
            "messages" to chamberViewModel.messages.value!![chamberViewModel.chamberState.value!!.chamberID]!!.map {
                it.toMap()
            },
            "reason" to reason,
            "ticketTaken" to false,
            "selfReport" to selfReport,
            "title" to "",
            "description" to "",
            "reportDate" to FieldValue.serverTimestamp()
        )

        chamberViewModel.reportUser(report = report)
    }

    private fun blockUser(uid: String) {
        userViewModel.blockUser(uid)
    }

    private fun exitChamber() {
        val uid = userViewModel.userState.value!!.UID
        userViewModel.closeChamber()
        chamberViewModel.exitChamber(uid)
    }

    private fun askForPlayStoreReview(dialog: Dialog, callback: () -> Unit = {}) {
        dialog.setContentView(R.layout.dialog_playstore_rating)

        val confirmButton = dialog.findViewById<TextView>(R.id.getPlayStoreReviewButton)
        val dismissButton = dialog.findViewById<TextView>(R.id.dismissDialogButton)

        dismissButton.setOnClickListener { callback() }
        confirmButton.setOnClickListener {
            callback()
            val appId = requireContext().packageName
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appId")
                    )
                )
            } catch (e: Exception) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appId")
                    )
                )
            }
        }
    }


    private fun registerActivityResultLauncher(): ActivityResultLauncher<PickVisualMediaRequest> {
        return registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                showImageConfirmationDialogBox(imageUri = uri) {
                    if (it) {
                        chamberViewModel.postImage(
                            uri = uri,
                            UID = userViewModel.userState.value!!.UID,
                            senderName = userViewModel.userState.value!!.displayName
                        )
                    } else {
                        Log.d("ImagePickedConfirmation", "Failed")
                    }
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
    }

    private fun launchPhotoPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showImageConfirmationDialogBox(
        imageUri: Uri,
        result: (v: Boolean) -> Unit
    ) {
        val dialog = Dialog(requireActivity(), R.style.Dialog)
        dialog.setContentView(R.layout.dialog_box_upload_button)
        dialog.setCancelable(true)
        val confirm_button = dialog.findViewById<Button>(R.id.confirm_button)
        val cancel_button = dialog.findViewById<Button>(R.id.cancel_button)
        val previewImage = dialog.findViewById<ImageView>(R.id.previewImage)
        confirm_button.setOnClickListener {
            result(true)
            dialog.dismiss()
        }
        cancel_button.setOnClickListener {
            result(false)
            dialog.dismiss()
        }

        CoroutineScope(Dispatchers.Main).launch {
            chamberViewModel.compressThumbnail(imageUri, previewImage) {
                dialog.show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chamberViewModel.addNotificationKey(
            userViewModel.userState.value!!.UID,
            userViewModel.userState.value!!.notificationKey
        )
    }

    override fun onPause() {
        super.onPause()
        chamberViewModel.addNotificationKey(
            userViewModel.userState.value!!.UID,
            userViewModel.userState.value!!.notificationKey
        )
        scheduleNotification(requireContext())

    }

    private fun scheduleNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderNotification::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )

        val triggerAtMillis = System.currentTimeMillis() + 16 * 60 * 60 * 1000L
        if (hasScheduleExactAlarmPermission(context)) {
            Log.d("AlarmManagerPermission", "Success")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )

        } else {
            Log.d("AlarmManagerPermission", "Denied")
        }
    }

    private fun hasScheduleExactAlarmPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun cancelNotificationAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderNotification::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
    }
}


