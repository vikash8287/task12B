package com.company.chamberly.presentation.fragments

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat.getColor
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.R
import com.company.chamberly.presentation.adapters.MessageAdapter
import com.company.chamberly.models.Message
import com.company.chamberly.models.toMap
import com.company.chamberly.presentation.viewmodels.ChamberViewModel
import com.company.chamberly.presentation.viewmodels.UserViewModel
import com.google.firebase.firestore.FieldValue

class ChatFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val chamberViewModel: ChamberViewModel by activityViewModels()

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emojiPickerView: EmojiPickerView
    private lateinit var cancelReplyButton: ImageButton
    private lateinit var replyContentView: TextView

    private val replyingTo = MutableLiveData("")
    private val reactionEmojis: List<String> = listOf("👍", "💗", "😂", "😯", "😥", "😔", "+")
    private val chamberLeavingOptions: Map<String, String> = mapOf(
        "DONE VENTING" to "\"I am done venting, thank you so much 💗\"",
        "WRONG MATCH" to "\"Wrong match, sorry 😢\"",
        "IN A HURRY" to "\"Sorry, I am in a hurry 😰\"",
        "JUST CHECKING THE APP OUT" to "\"Just checking the app 🥰\"",
        "NO ACTIVITY" to "\"There is no activity 😔\""
    )
    private val reportReasons: List<String> = listOf(
        "Harassment",
        "Inappropriate Behavior",
        "Unsupportive Behaviour",
        "Spamming",
        "Annoying"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chamberViewModel.setChamber(
            userViewModel.chamberID.value ?: "",
            userViewModel.userState.value!!.UID
        )
    }

//    override fun on
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        val groupTitle = view.findViewById<TextView>(R.id.groupTitle)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val exitChamberButton = view.findViewById<ImageButton>(R.id.exitChatButton)
        recyclerView = view.findViewById(R.id.recyclerViewMessages)
        val sendButton = view.findViewById<Button>(R.id.buttonSend)
        val messageBox = view.findViewById<EditText>(R.id.editTextMessage)
        val replyView = view.findViewById<LinearLayout>(R.id.replyingToView)
        emojiPickerView = view.findViewById(R.id.reaction_emoji_picker)
        replyContentView = view.findViewById(R.id.replyContentView)
        cancelReplyButton = view.findViewById(R.id.cancelReplyButton)

        replyingTo.observe(viewLifecycleOwner) {
            replyContentView.text = replyingTo.value
            if(it.isNullOrBlank()) {
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
            object: MessageAdapter.OnMessageLongClickListener {
                override fun onMessageLongClick(message: Message) {
                    showMessageDialog(message)
                }

                override fun onSelfLongClick(message: Message) {
                    showSelfMessageDialog(message)
                }
            }
        )

        chamberViewModel.messages.observe(viewLifecycleOwner) { messages ->
            if(messageAdapter.itemCount == 0) {
                messages?.let {
                    messageAdapter.setMessages(it)
                    recyclerView.scrollToPosition(it.size - 1)
                }
            } else {
                val newSize = messages.size
                val oldSize = messageAdapter.itemCount

                if (newSize == oldSize) {
                    if(newSize == 40 && messageAdapter.messageAt(0).message_id != messages[0].message_id) {
                        // More than 40 messages & a new message received
                        messageAdapter.messageRemoved(messageAdapter.messageAt(0))
                        messageAdapter.addMessage(messages[newSize - 1])
                        recyclerView.smoothScrollToPosition(39)
                    } else {
                        // A message was modified, following cases are possible
                        // A reaction was added/modified/removed
                        // The message was edited
                        // Since there are only 40 message, we iterate over all and find the one
                        // that was modified.
                        for(index in 0 until newSize) {
                            if (messages[index] != messageAdapter.messageAt(index)) {
                                messageAdapter.messageChanged(
                                    messages[index],
                                    messages[index].message_id
                                )
                                recyclerView.smoothScrollBy(0, 20)
                            }
                        }
                    }
                } else if(newSize > oldSize) {
                    // A new message was received
                    messageAdapter.addMessage(messages[newSize - 1])
                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                } else {
                    // A message was removed
                    // Will be handled later since there is no front-end functionality to delete
                    // messages
                }
            }

        }

        sendButton.setOnClickListener {
            val text = messageBox.text.toString()
            if(text.isBlank()) {
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
                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
            )
        }

        chamberViewModel.chamberState.observe(viewLifecycleOwner) {
            groupTitle.text = it.chamberTitle
        }

        backButton.setOnClickListener {
            chamberViewModel.clear()
            userViewModel.closeChamber()
        }

        val backPressHandler = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                chamberViewModel.clear()
                userViewModel.closeChamber()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)
        exitChamberButton.setOnClickListener {
            showChamberExitDialog()
        }
        return view
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
                if(emoji != "+") {
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
            blockDialogTitle.text = getString(R.string.block_user_dialog_title, message.sender_name)
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
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", messageContent)
        clipboard.setPrimaryClip(clip)
        Log.d("Holder", "Message copied: $messageContent")
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
        dialog.setContentView(R.layout.dialog_info)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val chamberExitOptionsLayout = dialog.findViewById<LinearLayout>(R.id.leave_chamber_options)
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
                chamberViewModel
                    .sendExitMessage(
                        message = message,
                        callback = {
                            showRatingDialog(
                                dialog = dialog,
                                userToRateUID = uid,
                                userToRateName = displayName,
                                callback = {
                                    dialog.dismiss()
                                    exitChamber()
                                    userViewModel.closeChamber()
                                }
                            )
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

        heading.text = getString(R.string.report_user_title_text, userToRateName)

        cancelButton.setOnClickListener {
            callback()
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            val stars = ratingBar.rating
            rateUser(userToRateUID, stars.toDouble())
            if(stars == 5.0f) {
                askForPlayStoreReview(dialog, callback = callback)
            } else if(stars <= 3.0f) {
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
        callback: () -> Unit = {}
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
            optionButton.setPaddingRelative(16, 16, 16, 16)
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
        val uid = userViewModel.userState.value!!.UID
        val report = hashMapOf(
            "against" to (against ?: uid),
            "by" to uid,
            "groupChatId" to chamberViewModel.chamberState.value!!.chamberID,
            "realHost" to "",
            "messages" to chamberViewModel.messages.value!!.map {
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

    private fun blockUser(
        uid: String
    ) {
        userViewModel.blockUser(uid)
    }

    private fun exitChamber() {
        val uid = userViewModel.userState.value!!.UID

        chamberViewModel.exitChamber(uid)
    }

    private fun askForPlayStoreReview(dialog: Dialog, callback: () -> Unit = {}) {
        dialog.setContentView(R.layout.dialog_ask_rating_playstore)

        val confirmButton = dialog.findViewById<TextView>(R.id.getPlayStoreReviewButton)
        val dismissButton = dialog.findViewById<TextView>(R.id.dismissDialogButton)

        dismissButton.setOnClickListener { callback() }
        confirmButton.setOnClickListener {
            callback()
            val appId = "com.google.android.youtube"
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
    }
}