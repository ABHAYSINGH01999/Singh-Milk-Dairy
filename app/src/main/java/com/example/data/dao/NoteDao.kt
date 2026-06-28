package com.example.data.dao

import androidx.room.*
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAtMillis DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<Note>

    @Query("SELECT * FROM notes WHERE customerId = :customerId ORDER BY createdAtMillis DESC")
    fun getNotesForCustomer(customerId: Int): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}
