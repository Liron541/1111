package com.cs407.lab5_milestone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.cs407.lab5_milestone.data.NoteDatabase
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Date

data class Note(
    val noteTitle: String,
    val noteAbstract: String,
    val noteDetail: String,
    val notePath: String?,
    val lastEdited: Date
)

// Fake function, you need to use your own @Dao function
fun upsertNote(note: Note, userId: Int) {}

class NoteListFragment : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var sharedPrefFile: String
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manually create 1000 notes for "large" user (only valid for bonus part)
        lifecycleScope.launch {
            val countNote = 0; // The total number of notes in the table
            val userName = "large";
            val userId = 0;
            if (countNote == 0 && userName == "large") {
                for (i in 1..1000) {
                    upsertNote(
                        Note(
                            noteTitle = "Note $i",
                            noteAbstract = "This is Note $i",
                            noteDetail = "Welcome to Note $i",
                            notePath = null,
                            lastEdited = Calendar.getInstance().time
                        ), userId
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)

        greetingTextView = view.findViewById(R.id.greetingTextView)
        noteRecyclerView = view.findViewById(R.id.noteRecyclerView)
        sharedPrefFile = getString(R.string.userPasswdKV)

        val username = arguments?.getString("username") ?: "User"
        userId = arguments?.getInt("userId") ?: 0

        greetingTextView.text = getString(R.string.greeting_text, username)

        setupRecyclerView(userId)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //remove back button when back to Note List
        if (activity is AppCompatActivity) {
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        val fab = view.findViewById<FloatingActionButton>(R.id.fab)


        // Set the OnClickListener for the FAB to add a new note
        fab.setOnClickListener {
            // Navigate to the NoteContentFragment to create a new note
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(
                noteId = 0,
                userId = userId
            )
            findNavController().navigate(action)
        }
        setupMenu()

    }

    private fun setupRecyclerView(userId: Int) {
        val noteDao = NoteDatabase.getDatabase(requireContext()).noteDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val notes = noteDao.getNotesForUser(userId)

            withContext(Dispatchers.Main) {
                val adapter = NoteAdapter(
                    notes.toMutableList(),
                    onNoteClick = { note ->
                        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(
                            noteId = note.noteId,
                            userId = userId
                        )
                        findNavController().navigate(action)
                    },
                    onNoteLongClick = { note ->
                        showDeleteDialog(note)
                    }
                )
                noteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                noteRecyclerView.adapter = adapter
            }
        }
    }


    private fun showDeleteDialog(note: com.cs407.lab5_milestone.data.Note) {
        // Create a BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        // Inflate the provided layout for the bottom sheet
        val view = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
        bottomSheetDialog.setContentView(view)

        // Get references to the buttons in the layout
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)

        // Set up the delete button click listener
        deleteButton.setOnClickListener {
            deleteNoteFromDatabase(note)
            bottomSheetDialog.dismiss()
        }

        // Set up the cancel button click listener
        cancelButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Show the dialog
        bottomSheetDialog.show()
    }


    private fun deleteNoteFromDatabase(note: com.cs407.lab5_milestone.data.Note) {
        lifecycleScope.launch(Dispatchers.IO) {
            val noteDao = NoteDatabase.getDatabase(requireContext()).noteDao()
            noteDao.deleteNoteById(note.noteId)

            // Delete the associated file if it exists
            note.notePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }

            // Fetch updated notes and refresh the RecyclerView
            val updatedNotes = noteDao.getNotesForUser(userId)
            withContext(Dispatchers.Main) {
                (noteRecyclerView.adapter as? NoteAdapter)?.updateNotes(updatedNotes)
            }
        }
    }

    private fun setupMenu() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.note_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        logout()
                        true
                    }
                    R.id.action_delete_account -> {
                        handleAccountDeletion()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleAccountDeletion() {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("current_user", "") ?: ""

        if (username.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val userDao = NoteDatabase.getDatabase(requireContext()).userDao()
                userDao.deleteUserByUsername(username)

                // Clear user information from SharedPreferences
                with(sharedPreferences.edit()) {
                    remove("current_user")
                    apply()
                }

                // Navigate back to the login screen on the main thread
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
                }
            }
        } else {
            Toast.makeText(requireContext(), "No user found to delete", Toast.LENGTH_SHORT).show()
        }
    }


    private fun logout() {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("current_user")
            apply()
        }
        navigateToLoginScreen()
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}