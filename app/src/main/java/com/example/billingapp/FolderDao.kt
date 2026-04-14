package com.example.billingapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): LiveData<List<Folder>>

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFoldersSync(): List<Folder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder)

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)
}
