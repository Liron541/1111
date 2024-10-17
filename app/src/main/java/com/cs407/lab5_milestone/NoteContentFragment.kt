package com.cs407.lab5_milestone

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class NoteContentFragment : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private val args: NoteContentFragmentArgs by navArgs()
    private var noteId: Int = 0
    private var userId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_content, container, false)

        titleEditText = view.findViewById(R.id.titleEditText)
        contentEditText = view.findViewById(R.id.contentEditText)
        saveButton = view.findViewById(R.id.saveButton)
        backButton = view.findViewById(R.id.backButton)

        noteId = args.noteId
        userId = args.userId

        // If editing an existing note, load its details
        if (noteId != 0) {
            loadNoteDetails(noteId)
        }

        // Set up the save button to save the note
        saveButton.setOnClickListener {
            saveNote()
        }
        
        return view
    }

    private fun loadNoteDetails(noteId: Int) {
        // Use coroutine to fetch note details from the database
        lifecycleScope.launch(Dispatchers.IO) {
            val noteDao = NoteDatabase.getDatabase(requireContext()).noteDao()
            val note = noteDao.getNoteById(noteId)

            withContext(Dispatchers.Main) {
                // If the note exists, populate the UI with its details
                note?.let {
                    titleEditText.setText(it.noteTitle)
                    contentEditText.setText(it.noteDetail)
                }
            }
        }
    }

    private fun saveNote() {
        // Get the content of the title and detail fields
        val noteTitle = titleEditText.text.toString().trim()
        val noteDetail = contentEditText.text.toString().trim()
        val noteAbstract = noteDetail.split("\n").firstOrNull()?.take(20) ?: ""

        // Create a new Note object to insert or update
        val note = Note(
            noteId = if (noteId == 0) 0 else noteId,
            userId = userId,
            noteTitle = noteTitle,
            noteDetail = noteDetail,
            noteAbstract = noteAbstract,
            notePath = null,
            lastEdited = Calendar.getInstance().time
        )

        // Use coroutine to save or update the note in the database
        lifecycleScope.launch(Dispatchers.IO) {
            val noteDao = NoteDatabase.getDatabase(requireContext()).noteDao()
            noteDao.upsert(note)

            // Return to the NoteListFragment after saving
            withContext(Dispatchers.Main) {
                findNavController().popBackStack()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle the back (cancel) button
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Handle the save button
        saveButton.setOnClickListener {
            saveNote()
        }
    }
}
