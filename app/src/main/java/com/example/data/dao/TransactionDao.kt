package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0")
    fun getAllTransactions(): Flow<List<TransactionEntry>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0")
    suspend fun getAllTransactionsSync(): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY dateMillis DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<TransactionEntry>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntry)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntry)

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntry)
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTransactions(): Flow<List<TransactionEntry>>

    @Query("DELETE FROM transactions WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()
}
