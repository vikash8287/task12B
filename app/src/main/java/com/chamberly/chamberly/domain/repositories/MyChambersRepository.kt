package com.chamberly.chamberly.domain.repositories

import com.chamberly.chamberly.models.ChamberPreview
import com.chamberly.chamberly.utils.Resource
import kotlinx.coroutines.flow.Flow

interface MyChambersRepository {
    fun getMyChambers(uid: String): Flow<Resource<List<ChamberPreview>>>
    fun addChamber(uid: String, chamber: ChamberPreview)
}