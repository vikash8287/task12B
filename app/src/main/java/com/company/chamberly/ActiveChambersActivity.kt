package com.company.chamberly
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
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
        val addchamber = findViewById<ImageButton>(R.id.btnAddChamber)


        val recyclerView = findViewById<RecyclerView>(R.id.rvChambers)
        val adapter = ChambersRecyclerViewAdapter { chamber ->
            // Handle click, navigate to ChatActivity
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("groupChatId", chamber.groupChatId)
                putExtra("groupTitle", chamber.groupTitle)
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

        addchamber.setOnClickListener{
            goToCreateActivity()
        }


        val btnFindChamber = findViewById<Button>(R.id.btnFindChamber)
        val btnCreateChamber = findViewById<Button>(R.id.btnCreateChamber)
        btnFindChamber.setOnClickListener {
            goToSearchActivity()
        }

        btnCreateChamber.setOnClickListener {
            goToCreateActivity()
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
                lastMessages.forEachIndexed { index, dataSnapshot ->
                    val lastMessage = dataSnapshot.children.firstOrNull()?.getValue(Message::class.java)
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
        val options = arrayOf("Delete Account", "Show Privacy Policy")
        val builder = AlertDialog.Builder(this)
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> deleteAccount() // Delete account option
                1 -> showPrivacyPolicy() // Show privacy policy option
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
                    val sharedPreferences = getSharedPreferences("userDetails", Context.MODE_PRIVATE)
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

    private fun goToCreateActivity() {
        val intent = Intent(this, CreateActivity::class.java)
        startActivity(intent)
    }


}