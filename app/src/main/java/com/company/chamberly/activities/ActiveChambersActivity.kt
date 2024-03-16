package com.company.chamberly.activities
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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


    // Using View Binding to reference the views
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activechambers)
        val homeButton = findViewById<ImageButton>(R.id.homeButton)

        val emptyStateView = findViewById<RelativeLayout>(R.id.emptyStateView)
        val addChamber = findViewById<ImageButton>(R.id.btnAddChamber)


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
        recyclerView.layoutManager = LinearLayoutManager(this)
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
        }

        val profilePicButton = findViewById<ImageButton>(R.id.btnProfilePic)
        profilePicButton.setOnClickListener {
            showProfileOptionsPopup()
        }

        addChamber.setOnClickListener{
            goToCreateChamberActivity()
        }


        val btnFindChamber = findViewById<Button>(R.id.btnFindChamber)
        val btnCreateChamber = findViewById<Button>(R.id.btnCreateChamber)
        val btnCreateTopic = findViewById<Button>(R.id.btnCreateTopic)
        btnFindChamber.setOnClickListener {
            goToSearchActivity()
        }

        btnCreateChamber.setOnClickListener {
            goToCreateChamberActivity()
        }
        btnCreateTopic.setOnClickListener {
            goToCreateTopicActivity()
        }

    }

    private fun fetchChambers(callback: (List<Chamber>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("GroupChatIds")
                .whereArrayContains("members", userId)
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
                Log.d("MESSAGES", lastMessages.size.toString())
                lastMessages.forEachIndexed { index, dataSnapshot ->
                    val lastMessage = try { dataSnapshot.children.firstOrNull()?.getValue(Message::class.java) } catch(_: Exception) { Message(message_content = "No messages") }
                    chambers[index].lastMessage = lastMessage?.message_content ?: "No messages"
                }
                callback(chambers)
            }
    }

    private fun fetchLastMessageForChamber(chamber: Chamber): Task<DataSnapshot> {
        return database.reference.child(chamber.groupChatId)
            .child("messages").orderByKey().limitToLast(1).get()
    }

    private fun showProfileOptionsPopup() {
        val options = arrayOf("Delete Account", "Show Privacy Policy", "Submit feedback")
        val builder = AlertDialog.Builder(this)
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> deleteAccount() // Delete account option
                1 -> showPrivacyPolicy() // Show privacy policy option
                2 -> submitFeedback(dialog) // Show feedback dialog
            }
        }
        builder.show()
    }

    private fun showPrivacyPolicy() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chamberly.net/privacy-policy"))
        startActivity(browserIntent)
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            // Optional: Delete user's associated data from Firestore

            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        clear()
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

    private fun submitFeedback(dialog: DialogInterface) {
        dialog.dismiss()
        val feedbackDialog = Dialog(this, R.style.Dialog)
        feedbackDialog.setContentView(R.layout.dialog_feedback)
        feedbackDialog.show()
        val submitButton = feedbackDialog.findViewById<Button>(R.id.submitFeedbackButton)
        val dismissButton = feedbackDialog.findViewById<Button>(R.id.dismissFeedbackDialogButton)
        val editText = feedbackDialog.findViewById<EditText>(R.id.feedback_text)
        val feedbackSuccessText = feedbackDialog.findViewById<TextView>(R.id.feedback_success_text)
        val buttonsLayout = feedbackDialog.findViewById<TableRow>(R.id.buttons_layout)
        dismissButton?.setOnClickListener {
            feedbackDialog.hide()
        }

        submitButton?.setOnClickListener {
            Log.d("FEEDBACK", "SUBMIT PRESSED")
            val feedbackText = "Android: ${editText.text}"
            val feedbackRef = firestore.collection("Feedback").document()
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
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


    private fun goToSearchActivity() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun goToCreateChamberActivity() {
        val intent = Intent(this, CreateChamberActivity::class.java)
        startActivity(intent)
    }

    private fun goToCreateTopicActivity() {
        val intent = Intent(this, CreateTopicActivity::class.java)
        startActivity(intent)
    }


}