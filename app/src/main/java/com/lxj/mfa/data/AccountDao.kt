package com.lxj.mfa.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY issuer, label")
    fun observe(): LiveData<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY issuer, label")
    suspend fun getAllList(): List<Account>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(a: Account)

    @Delete
    suspend fun delete(a: Account)
}
