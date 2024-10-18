package com.cs407.lab5_milestone

import android.os.Bundle
import android.util.Log
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
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class NoteContentFragment : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private val args: NoteContentFragmentArgs by navArgs()
    private var noteId: Int = 0
    private var userId: Int = 0
    private var notePath: String? = null

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

                    // Load content from the file if notePath is present
                    val noteContent = if (!it.notePath.isNullOrEmpty()) {
                        notePath = it.notePath
                        readContentFromFile(it.notePath)
                    } else {
                        it.noteDetail ?: ""
                    }

                    contentEditText.setText(noteContent)
                }
            }
        }
    }

    // Helper function to read content from a file
    private fun readContentFromFile(path: String): String {
        val file = File(path)
        return if (file.exists()) {
            file.readText()
        } else {
            ""
        }
    }

    private fun saveNote() {
        // Get the content of the title and detail fields
        val noteTitle = titleEditText.text.toString().trim()
        val noteDetail = contentEditText.text.toString().trim()
        val noteAbstract = if (noteDetail.isNotEmpty()) {
            noteDetail.split("\n").firstOrNull()?.take(20) ?: ""
        } else {
            ""
        }
        Log.d("NoteSave", "noteDetail length before saving: ${noteDetail.length}")


        val newNotePath: String? = if (noteDetail.length > 1024) {
            saveContentToFile(noteDetail, existingFilePath = notePath)
        } else {
            null
        }
        Log.d("NoteSave", "Saving note: noteId=$noteId, notePath=$newNotePath")

        // Create a new Note object to insert or update
        val note = Note(
            noteId = if (noteId == 0) 0 else noteId,
            userId = userId,
            noteTitle = noteTitle,
            noteDetail = if (newNotePath == null) noteDetail else null,
            noteAbstract = noteAbstract,
            notePath = newNotePath,
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

    // Helper function
    private fun saveContentToFile(content: String, existingFilePath: String?): String {
        val file: File = if (!existingFilePath.isNullOrEmpty()) {
            File(existingFilePath)
        } else {
            // Create a new file if no existing path
            val fileName = "note-$userId-${if (noteId == 0) "new" else noteId}-${System.currentTimeMillis()}.txt"
            File(requireContext().filesDir, fileName)
        }

        FileOutputStream(file).use { output ->
            output.write(content.toByteArray())
        }
        Log.d("NoteFile", "File saved at: ${file.absolutePath}, Content size: ${content.length}")
        return file.absolutePath
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
