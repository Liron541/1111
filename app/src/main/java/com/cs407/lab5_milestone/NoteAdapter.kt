package com.cs407.lab5_milestone

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cs407.lab5_milestone.data.Note
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(
    private var notes: MutableList<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        Log.d("Pagination", "$position, noteId: ${note.noteId}, noteTitle: ${note.noteTitle}, noteAbstract: ${note.noteAbstract}")
        holder.bind(note)
        holder.itemView.setOnClickListener { onNoteClick(note) }
        holder.itemView.setOnLongClickListener {
            onNoteLongClick(note)
            true
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteTitleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val noteAbstractTextView: TextView = itemView.findViewById(R.id.abstractTextView)
        private val noteDateTextView: TextView = itemView.findViewById(R.id.dateTextView)

        fun bind(note: Note) {
            noteTitleTextView.text = note.noteTitle
            noteAbstractTextView.text = note.noteAbstract
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = note.lastEdited.let { dateFormatter.format(it) }

            // Set the formatted date in the TextView
            noteDateTextView.text = formattedDate
        }
    }
}
