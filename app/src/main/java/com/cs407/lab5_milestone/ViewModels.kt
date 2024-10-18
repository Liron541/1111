package com.cs407.lab5_milestone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cs407.lab5_milestone.data.NoteDao
import com.cs407.lab5_milestone.data.NoteSummary
import kotlinx.coroutines.flow.Flow

data class UserState(/* Delete `void` and Add what you want */val void: String = "")

/* Use UserViewModel to cache user state (userId, userName, etc.)
    Don't forget to clean the state when log out/delete account
 */
/* Ref: https://developer.android.com/topic/libraries/architecture/viewmodel */
class NoteViewModel(private val noteDao: NoteDao, private val userId: Int) : ViewModel() {

    val notePagingFlow: Flow<PagingData<NoteSummary>> = Pager(
        config = PagingConfig(
            pageSize = 20,       // Number of items to load per page
            prefetchDistance = 10 // Fetch the next 10 items before they're needed
        ),
        pagingSourceFactory = { noteDao.getNoteListsByUserIdPaged(userId) }
    ).flow
        .cachedIn(viewModelScope)
}