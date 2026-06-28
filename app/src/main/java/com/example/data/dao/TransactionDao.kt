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
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<TransactionEntry>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY dateMillis DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<TransactionEntry>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntry)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntry)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntry)
}
