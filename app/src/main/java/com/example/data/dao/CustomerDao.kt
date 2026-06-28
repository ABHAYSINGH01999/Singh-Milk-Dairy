package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Int): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Int)
}
