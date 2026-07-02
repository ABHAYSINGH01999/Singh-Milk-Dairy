package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    fun getCustomerById(id: Int): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdSync(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Query("UPDATE customers SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteCustomer(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Int)
    
    @Query("SELECT * FROM customers WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedCustomers(): Flow<List<Customer>>

    @Query("DELETE FROM customers WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()
}
