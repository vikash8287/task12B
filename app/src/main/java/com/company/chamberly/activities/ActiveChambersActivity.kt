package com.company.chamberly.activities
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.company.chamberly.ChambersRecyclerViewAdapter
import com.company.chamberly.models.Chamber
import com.company.chamberly.models.Message
import com.company.chamberly.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActiveChambersActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val database = Firebase.database        // realtime database
    private lateinit var sharedPreferences: SharedPreferences

    // Using View Binding to reference the views
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activechambers)

        sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val homeButton = findViewById<ImageButton>(R.id.homeButton)

        val emptyStateView = findViewById<RelativeLayout>(R.id.emptyStateView)
        val addChamber = findViewById<ImageButton>(R.id.btnAddChamber)
        val layoutManager = LinearLayoutManager(this)


        val recyclerView = findViewById<RecyclerView>(R.id.rvChambers)
        val adapter = ChambersRecyclerViewAdapter { chamber ->
            // Handle click, navigate to ChatActivity
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("GroupChatId", chamber.groupChatId)
                putExtra("GroupTitle", chamber.groupTitle)
                // Add other necessary data
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(this, layoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)
        // Fetch chambers and update RecyclerView
        fetchChambers { chambers ->
            if (chambers.isNotEmpty()) {
                adapter.updateChambers(chambers)
                recyclerView.visibility = View.VISIBLE
                emptyStateView.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                emptyStateView.visibility = View.VISIBLE
            }
        }


        homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val profilePicButton = findViewById<ImageButton>(R.id.btnProfilePic)
        profilePicButton.setOnClickListener {buttonView ->
            showProfileOptionsPopup(buttonView)
        }

        addChamber.setOnClickListener{
            goToCreateChamberActivity()
        }

    }

    private fun fetchChambers(callback: (List<Chamber>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("GroupChatIds")
                .whereArrayContains("members", userId)
                .limit(20)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val chambers = querySnapshot.documents.mapNotNull { it.toObject(Chamber::class.java) }
                    fetchLastMessagesForChambers(chambers, callback)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching chambers: ${exception.message}", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
        } else {
            callback(emptyList()) // No user logged in
        }
    }


    private fun fetchLastMessagesForChambers(chambers: List<Chamber>, callback: (List<Chamber>) -> Unit) {
        val lastMessageTasks = chambers.map { chamber ->
            fetchLastMessageForChamber(chamber)
        }

        Tasks.whenAllSuccess<DataSnapshot>(lastMessageTasks)
            .addOnSuccessListener { lastMessages ->
                lastMessages.forEachIndexed { index, dataSnapshot ->
                    val lastMessage = try { dataSnapshot.children.firstOrNull()?.getValue(Message::class.java) } catch(_: Exception) { Message(message_content = "No messages") }
                    chambers[index].lastMessage = getLastMessage(lastMessage)
                }
                callback(chambers)
            }
    }

    private fun getLastMessage(lastMessage: Message?): String {
        return if(lastMessage == null) {
            "No messages"
        } else if(lastMessage.message_type == "text" || lastMessage.message_type == "system") {
            lastMessage.message_content
        } else {
            lastMessage.message_type
        }
    }

    private fun fetchLastMessageForChamber(chamber: Chamber): Task<DataSnapshot> {
        return database.reference.child(chamber.groupChatId)
            .child("messages").orderByKey().limitToLast(1).get()
    }

    private fun showProfileOptionsPopup(buttonView: View) {

        val profileOptionsPopUp = Dialog(this, R.style.Dialog)
        profileOptionsPopUp.setContentView(R.layout.popup_profile_options)

        val deleteAccountButton = profileOptionsPopUp.findViewById<TextView>(R.id.delete_account)
        val showPrivacyPolicyButton = profileOptionsPopUp.findViewById<TextView>(R.id.show_privacy_policy)
        val submitFeedbackButton = profileOptionsPopUp.findViewById<TextView>(R.id.submit_feedback)

        val roleSelectorButton = profileOptionsPopUp.findViewById<RadioGroup>(R.id.role_selector_group)
        val confirmRoleChangeView = profileOptionsPopUp.findViewById<LinearLayout>(R.id.confirm_role_change_view)
        val confirmRoleChangeButton = profileOptionsPopUp.findViewById<TextView>(R.id.confirm_role_change_button)
        val ventorButton = profileOptionsPopUp.findViewById<RadioButton>(R.id.role_ventor)
        val listenerButton = profileOptionsPopUp.findViewById<RadioButton>(R.id.role_listener)

        var isListener = sharedPreferences.getBoolean("isListener", false)

        roleSelectorButton.setOnCheckedChangeListener { _, selectedButton ->
            listenerButton.setTextColor(if(selectedButton == R.id.role_listener) Color.BLACK else Color.WHITE)
            ventorButton.setTextColor(if(selectedButton == R.id.role_ventor) Color.BLACK else Color.WHITE)
            if(isListener && selectedButton == R.id.role_ventor) {
                confirmRoleChangeView.visibility = View.VISIBLE
                confirmRoleChangeButton.setOnClickListener {
                    isListener = false
                    setRole("ventor")
                    confirmRoleChangeView.visibility = View.GONE
                }
            } else if(!isListener && selectedButton == R.id.role_listener) {
                confirmRoleChangeView.visibility = View.VISIBLE
                confirmRoleChangeButton.setOnClickListener {
                    isListener = true
                    setRole("listener")
                    confirmRoleChangeView.visibility = View.GONE
                }
            } else {
                confirmRoleChangeView.visibility = View.GONE
            }
        }
        roleSelectorButton.check(if(isListener) R.id.role_listener else R.id.role_ventor)


        deleteAccountButton.setOnClickListener { deleteAccount() }
        showPrivacyPolicyButton.setOnClickListener { showPrivacyPolicy() }
        submitFeedbackButton.setOnClickListener { submitFeedback(profileOptionsPopUp) }

        val params = WindowManager.LayoutParams()
        params.copyFrom(profileOptionsPopUp.window?.attributes)
        params.gravity = Gravity.TOP
        params.y = buttonView.bottom
        profileOptionsPopUp.window?.attributes = params
        profileOptionsPopUp.show()
    }

    private fun setRole(role: String) {
        val editor = sharedPreferences.edit()
        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid) ?: ""
        val currentUserRef = firestore.collection("Accounts").document(uid)
        currentUserRef.update("selectedRole", role)
        stopProcrastination()
        editor.putBoolean("isListener", role == "listener")
        editor.apply()
    }

    private fun showPrivacyPolicy() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            // Optional: Delete user's associated data from Firestore
            auth.signOut()
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        clear()
                        putBoolean("isNewUser", false)
                        apply()
                    }
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    redirectToWelcomeActivity()
                } else {
                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitFeedback(dialog: Dialog) {
        dialog.setContentView(R.layout.dialog_feedback)

        val submitButton = dialog.findViewById<Button>(R.id.submitFeedbackButton)
        val dismissButton = dialog.findViewById<Button>(R.id.dismissFeedbackDialogButton)
        val editText = dialog.findViewById<EditText>(R.id.feedback_text)
        val feedbackSuccessText = dialog.findViewById<TextView>(R.id.feedback_success_text)

        dismissButton.setOnClickListener { dialog.hide() }

        submitButton.setOnClickListener {
            val feedbackText = "Android: ${editText.text}"
            val feedbackRef = firestore.collection("Feedback").document()
            val uid = sharedPreferences.getString("uid", "") ?: auth.currentUser!!.uid
            val displayName = sharedPreferences.getString("displayName", "") ?: ""
            feedbackRef.set(mapOf(
                "byName" to displayName,
                "byUID" to uid,
                "feedbackData" to feedbackText,
                "timestamp" to FieldValue.serverTimestamp()
            )).addOnSuccessListener {
                editText.visibility = View.GONE
                feedbackSuccessText.visibility = View.VISIBLE
                submitButton.visibility = View.GONE
            }
        }
    }

    private fun redirectToWelcomeActivity() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    private fun goToCreateChamberActivity() {
        val intent = Intent(this, CreateChamberActivity::class.java)
        startActivity(intent)
    }

    private fun stopProcrastination() {
        val editor = sharedPreferences.edit()
        val uid = sharedPreferences.getString("uid", auth.currentUser!!.uid)
        val topicsList = sharedPreferences.getString("topics", "")!!.split(",") as MutableList<String>
        for(topic in topicsList) {
            if(topic.isNotBlank()) {
                database
                    .reference
                    .child(topic)
                    .child("users")
                    .child("members")
                    .child(uid!!)
                    .removeValue()
            }
        }
        editor.remove("topics")
        editor.apply()
    }
}
